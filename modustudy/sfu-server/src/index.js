require('dotenv').config();
const http = require('http');
const https = require('https');
const express = require('express');
const { Server } = require('socket.io');
const mediasoup = require('mediasoup');
const config = require('./config');
const fs = require('fs');
const { createRecordingManager } = require('./recordingManager');
const { createSpeechDetector } = require('./speechDetector');

const app = express();
const corsOrigins = config.corsOrigins.includes('*') ? '*' : config.corsOrigins;
app.use((req, res, next) => {
  const origin = req.headers.origin;
  if (corsOrigins === '*') {
    res.setHeader('Access-Control-Allow-Origin', '*');
  } else if (origin && corsOrigins.includes(origin)) {
    res.setHeader('Access-Control-Allow-Origin', origin);
    res.setHeader('Vary', 'Origin');
  }
  res.setHeader('Access-Control-Allow-Methods', 'GET,POST,PUT,DELETE,OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  if (req.method === 'OPTIONS') {
    res.sendStatus(204);
    return;
  }
  next();
});
app.use(express.json({ limit: '1mb' }));
const useHttps = String(process.env.SFU_USE_HTTPS ?? 'true').toLowerCase() !== 'false';
let server;
if (useHttps) {
  if (!fs.existsSync(config.sslKeyPath) || !fs.existsSync(config.sslCertPath)) {
    // eslint-disable-next-line no-console
    console.error('Missing SSL files for SFU server', {
      key: config.sslKeyPath,
      cert: config.sslCertPath
    });
    process.exit(1);
  }
  // Use HTTPS in production; local dev can opt-out with SFU_USE_HTTPS=false.
  server = https.createServer({
    key: fs.readFileSync(config.sslKeyPath),
    cert: fs.readFileSync(config.sslCertPath)
  }, app);
} else {
  server = http.createServer(app);
}
const io = new Server(server, {
  cors: {
    origin: corsOrigins,
    methods: ['GET', 'POST'],
    credentials: true
  }
});

const rooms = new Map();
const emptyRoomStopTimers = new Map();
const speechDetectors = new Map();  // roomId -> SpeechDetector (실시간 발화 감지)
// 멀티 Worker 배열 및 라운드로빈 인덱스
const workers = [];
let nextWorkerIdx = 0;
let recordingManager;

// 개별 Worker 생성 및 재시작 로직
async function createSingleWorker(index) {
  const worker = await mediasoup.createWorker({
    rtcMinPort: config.rtcMinPort,
    rtcMaxPort: config.rtcMaxPort,
    logLevel: 'warn'
  });

  worker.on('died', async () => {
    // eslint-disable-next-line no-console
    console.error(`mediasoup worker[${index}] died, restarting...`);
    workers[index] = null;
    try {
      workers[index] = await createSingleWorker(index);
      // eslint-disable-next-line no-console
      console.log(`mediasoup worker[${index}] restarted`);
    } catch (err) {
      // eslint-disable-next-line no-console
      console.error(`Failed to restart worker[${index}]`, err);
    }
  });

  return worker;
}

// 멀티 Worker 생성
async function createWorkers() {
  const numWorkers = config.numWorkers;
  // eslint-disable-next-line no-console
  console.log(`Creating ${numWorkers} mediasoup workers...`);
  for (let i = 0; i < numWorkers; i++) {
    const worker = await createSingleWorker(i);
    workers.push(worker);
  }
  // eslint-disable-next-line no-console
  console.log(`${workers.length} mediasoup workers created`);
}

async function getOrCreateRoom(roomId) {
  if (rooms.has(roomId)) {
    return rooms.get(roomId);
  }
  // 라운드로빈으로 Worker 배정
  const worker = workers[nextWorkerIdx % workers.length];
  nextWorkerIdx++;
  const router = await worker.createRouter({ mediaCodecs: config.mediaCodecs });
  const room = { id: roomId, router, peers: new Map() };
  rooms.set(roomId, room);
  return room;
}

function getPeer(room, socketId) {
  return room.peers.get(socketId);
}

function cleanPeer(peer) {
  peer.consumers.forEach((consumer) => consumer.close());
  peer.producers.forEach((entry) => entry.producer.close());
  peer.transports.forEach((transport) => transport.close());
}

function listProducers(room, excludeSocketId) {
  const producerInfos = [];
  room.peers.forEach((peer, peerId) => {
    if (peerId === excludeSocketId) return;
    peer.producers.forEach((entry) => {
      producerInfos.push({ producerId: entry.producer.id, peerId, kind: entry.kind, displayName: peer.displayName });
    });
  });
  return producerInfos;
}

// 실시간 발화 감지기 초기화 (auto-start 지원)
async function ensureSpeechDetector(roomId) {
  if (speechDetectors.has(roomId)) return;

  const room = rooms.get(roomId);
  if (!room) return;

  // roomId에서 meetingId 추출 (meeting-{id} 형식)
  const meetingIdStr = roomId.replace('meeting-', '');
  const meetingId = parseInt(meetingIdStr, 10);
  if (isNaN(meetingId)) return;

  try {
    const speechDetector = createSpeechDetector({
      router: room.router,
      room,
      meetingId,
      config,
      onSegmentReady: (segment) => {
        // eslint-disable-next-line no-console
        console.log('[speech] Segment ready callback', segment);
      }
    });
    await speechDetector.initialize();

    // 기존 오디오 Producer들 등록
    for (const [peerId, peer] of room.peers) {
      for (const [, entry] of peer.producers) {
        if (entry.kind === 'audio') {
          const userId = peer.authToken ? peerId : peer.displayName || peerId;
          await speechDetector.addProducer(entry.producer, userId);
        }
      }
    }

    speechDetectors.set(roomId, speechDetector);
    // eslint-disable-next-line no-console
    console.log('[speech] SpeechDetector started (auto)', { roomId, meetingId });
  } catch (err) {
    // eslint-disable-next-line no-console
    console.error('[speech] SpeechDetector auto-start failed', { roomId, error: err.message });
  }
}

// recordingManager.onProducersChanged 호출 후 SpeechDetector도 시작
async function handleProducersChanged(roomId) {
  if (recordingManager) {
    await recordingManager.onProducersChanged(roomId);
    // 녹음이 시작되었으면 SpeechDetector도 시작
    if (recordingManager.recordings.has(roomId)) {
      await ensureSpeechDetector(roomId);
    }
  }
}

io.on('connection', (socket) => {
  const authToken = socket.handshake.auth?.token || socket.handshake.query?.token;
  if (authToken) {
    socket.data.authToken = authToken;
  }
  socket.on('joinRoom', async ({ roomId, displayName }, callback) => {
    try {
      const pending = emptyRoomStopTimers.get(roomId);
      if (pending) {
        clearTimeout(pending);
        emptyRoomStopTimers.delete(roomId);
      }
      const room = await getOrCreateRoom(roomId);
      room.peers.set(socket.id, {
        id: socket.id,
        displayName,
        authToken: socket.data.authToken || null,
        transports: new Map(),
        producers: new Map(),
        consumers: new Map()
      });
      socket.join(roomId);
      const existingProducers = listProducers(room, socket.id);
      callback({ rtpCapabilities: room.router.rtpCapabilities, existingProducers });
    } catch (err) {
      callback({ error: err.message });
    }
  });

  socket.on('createWebRtcTransport', async ({ roomId }, callback) => {
    try {
      const room = await getOrCreateRoom(roomId);
      const listenIps = [{ ip: config.listenIp }];
      if (config.announcedIp) {
        listenIps[0].announcedIp = config.announcedIp;
      }
      const transport = await room.router.createWebRtcTransport({
        listenIps,
        enableUdp: config.rtcEnableUdp,
        enableTcp: config.rtcEnableTcp,
        preferUdp: config.rtcPreferUdp,
        initialAvailableOutgoingBitrate: 1000000
      });
      // eslint-disable-next-line no-console
      console.log('[webrtc] transport created', { roomId, transportId: transport.id, direction: 'client' });

      transport.on('connectionstatechange', (state) => {
        // eslint-disable-next-line no-console
        console.log('[webrtc] transport state', { roomId, transportId: transport.id, state });
      });

      transport.on('icestatechange', (state) => {
        // eslint-disable-next-line no-console
        console.log('[webrtc] transport ice', { roomId, transportId: transport.id, state });
      });

      const peer = getPeer(room, socket.id);
      peer.transports.set(transport.id, transport);

      transport.on('dtlsstatechange', (dtlsState) => {
        // eslint-disable-next-line no-console
        console.log('[webrtc] transport dtls', { roomId, transportId: transport.id, dtlsState });
        if (dtlsState === 'closed') {
          transport.close();
        }
      });

      // TURN 서버 설정이 있으면 iceServers에 포함
      const iceServers = [];
      if (config.turnUrl) {
        iceServers.push({
          urls: config.turnUrl,
          username: config.turnUsername,
          credential: config.turnCredential
        });
      }

      callback({
        params: {
          id: transport.id,
          iceParameters: transport.iceParameters,
          iceCandidates: transport.iceCandidates,
          dtlsParameters: transport.dtlsParameters,
          iceServers
        }
      });
    } catch (err) {
      callback({ error: err.message });
    }
  });

  socket.on('connectWebRtcTransport', async ({ roomId, transportId, dtlsParameters }, callback) => {
    try {
      const room = await getOrCreateRoom(roomId);
      const peer = getPeer(room, socket.id);
      const transport = peer.transports.get(transportId);
      await transport.connect({ dtlsParameters });
      callback({ connected: true });
    } catch (err) {
      callback({ error: err.message });
    }
  });

  socket.on('produce', async ({ roomId, transportId, kind, rtpParameters, appData }, callback) => {
    try {
      const room = await getOrCreateRoom(roomId);
      const peer = getPeer(room, socket.id);
      const transport = peer.transports.get(transportId);

      // 기존 video Producer 찾기 (교체 시 끊김 최소화를 위해)
      let existingVideoEntry = null;
      if (kind === 'video') {
        existingVideoEntry = Array.from(peer.producers.values()).find((item) => item.kind === 'video');
      }

      // 새 Producer 먼저 생성
      const producer = await transport.produce({ kind, rtpParameters, appData });
      peer.producers.set(producer.id, {
        producer,
        kind,
        createdAt: Date.now(),
        appData: appData || null
      });

      producer.on('transportclose', () => {
        producer.close();
        peer.producers.delete(producer.id);
        handleProducersChanged(roomId);
      });

      // 새 Producer 알림 먼저 전송
      socket.to(roomId).emit('newProducer', { producerId: producer.id, producerPeerId: socket.id, kind, displayName: peer.displayName });
      handleProducersChanged(roomId);

      // 실시간 발화 감지: 오디오 Producer 추가
      if (kind === 'audio') {
        const speechDetector = speechDetectors.get(roomId);
        if (speechDetector) {
          // userId는 peer의 authToken에서 추출하거나 displayName 사용
          const userId = peer.authToken ? peer.id : peer.displayName || socket.id;
          speechDetector.addProducer(producer, userId).catch((err) => {
            // eslint-disable-next-line no-console
            console.error('[speech] addProducer failed', { roomId, producerId: producer.id, error: err.message });
          });
        }
      }

      // 기존 video Producer가 있으면 딜레이 후 정리 (끊김 최소화)
      if (existingVideoEntry) {
        const oldProducerId = existingVideoEntry.producer.id;
        setTimeout(() => {
          if (peer.producers.has(oldProducerId)) {
            existingVideoEntry.producer.close();
            peer.producers.delete(oldProducerId);
            socket.to(roomId).emit('producerClosed', { producerId: oldProducerId, peerId: socket.id });
            handleProducersChanged(roomId);
          }
        }, 300);
      }

      callback({ producerId: producer.id });
    } catch (err) {
      callback({ error: err.message });
    }
  });

  socket.on('closeProducer', ({ roomId, producerId }, callback) => {
    try {
      const room = rooms.get(roomId);
      if (!room) {
        callback({ closed: false });
        return;
      }
      const peer = getPeer(room, socket.id);
      const entry = peer?.producers.get(producerId);
      if (!entry) {
        callback({ closed: false });
        return;
      }

      // 실시간 발화 감지: 오디오 Producer 제거
      if (entry.kind === 'audio') {
        const speechDetector = speechDetectors.get(roomId);
        if (speechDetector) {
          speechDetector.removeProducer(producerId).catch((err) => {
            // eslint-disable-next-line no-console
            console.error('[speech] removeProducer failed', { roomId, producerId, error: err.message });
          });
        }
      }

      entry.producer.close();
      peer.producers.delete(producerId);
      socket.to(roomId).emit('producerClosed', { producerId, peerId: socket.id });
      handleProducersChanged(roomId);
      callback({ closed: true });
    } catch (err) {
      callback({ error: err.message });
    }
  });

  socket.on('consume', async ({ roomId, consumerTransportId, producerId, rtpCapabilities }, callback) => {
    try {
      const room = await getOrCreateRoom(roomId);
      if (!room.router.canConsume({ producerId, rtpCapabilities })) {
        callback({ error: 'cannot consume' });
        return;
      }
      const peer = getPeer(room, socket.id);
      const transport = peer.transports.get(consumerTransportId);
      const consumer = await transport.consume({
        producerId,
        rtpCapabilities,
        paused: false
      });
      peer.consumers.set(consumer.id, consumer);

      consumer.on('transportclose', () => {
        consumer.close();
        peer.consumers.delete(consumer.id);
      });

      callback({
        params: {
          id: consumer.id,
          producerId,
          kind: consumer.kind,
          rtpParameters: consumer.rtpParameters,
          type: consumer.type,
          producerPaused: consumer.producerPaused
        }
      });
    } catch (err) {
      callback({ error: err.message });
    }
  });

  socket.on('resume', async ({ roomId, consumerId }, callback) => {
    try {
      const room = await getOrCreateRoom(roomId);
      const peer = getPeer(room, socket.id);
      const consumer = peer.consumers.get(consumerId);
      if (consumer.paused) {
        await consumer.resume();
      }
      if (consumer.kind === 'video' && typeof consumer.requestKeyFrame === 'function') {
        try {
          await consumer.requestKeyFrame();
        } catch (e) {
          // transport가 아직 연결 중일 때 keyFrame 요청 실패 가능 - 무시
          console.warn('[webrtc] requestKeyFrame failed (transport may not be connected yet)', e.message);
        }
      }
      callback({ resumed: true });
    } catch (err) {
      callback({ error: err.message });
    }
  });

  socket.on('requestKeyFrame', async ({ roomId, consumerId }, callback) => {
    try {
      const room = await getOrCreateRoom(roomId);
      const peer = getPeer(room, socket.id);
      const consumer = peer.consumers.get(consumerId);
      if (consumer && consumer.kind === 'video' && typeof consumer.requestKeyFrame === 'function') {
        await consumer.requestKeyFrame();
        callback({ requested: true });
        return;
      }
      callback({ requested: false });
    } catch (err) {
      callback({ error: err.message });
    }
  });

  socket.on('disconnect', () => {
    rooms.forEach((room, roomId) => {
      const peer = room.peers.get(socket.id);
      if (!peer) return;
      cleanPeer(peer);
      room.peers.delete(socket.id);
      socket.to(roomId).emit('peerLeft', { peerId: socket.id });
      handleProducersChanged(roomId);
      if (room.peers.size === 0) {
        if (recordingManager && recordingManager.recordings.has(roomId)) {
          if (emptyRoomStopTimers.has(roomId)) return;
          const timer = setTimeout(() => {
            emptyRoomStopTimers.delete(roomId);
            recordingManager
              .stopRecording({ roomId })
              .catch((err) => {
                // eslint-disable-next-line no-console
                console.error('[recording] auto-stop failed', err);
              })
              .finally(() => {
                if (room.peers.size === 0) {
                  rooms.delete(roomId);
                }
              });
          }, 5000);
          emptyRoomStopTimers.set(roomId, timer);
        } else {
          rooms.delete(roomId);
        }
      }
    });
  });
});

app.post('/recordings/start', async (req, res) => {
  try {
    const { roomId, meetingId } = req.body || {};
    if (!roomId || !meetingId) {
      res.status(400).json({ error: 'roomId and meetingId required' });
      return;
    }
    // eslint-disable-next-line no-console
    console.log('[recording] start request', { roomId, meetingId });
    const result = await recordingManager.startRecording({ roomId, meetingId });
    // eslint-disable-next-line no-console
    console.log('[recording] start response', { roomId, result });

    // 실시간 발화 감지 시작
    if (!speechDetectors.has(roomId)) {
      const room = rooms.get(roomId);
      if (room) {
        const speechDetector = createSpeechDetector({
          router: room.router,
          room,
          meetingId,
          config,
          onSegmentReady: (segment) => {
            // eslint-disable-next-line no-console
            console.log('[speech] Segment ready callback', segment);
          }
        });
        await speechDetector.initialize();

        // 기존 오디오 Producer들 등록
        for (const [peerId, peer] of room.peers) {
          for (const [, entry] of peer.producers) {
            if (entry.kind === 'audio') {
              const userId = peer.authToken ? peerId : peer.displayName || peerId;
              await speechDetector.addProducer(entry.producer, userId);
            }
          }
        }

        speechDetectors.set(roomId, speechDetector);
        // eslint-disable-next-line no-console
        console.log('[speech] SpeechDetector started', { roomId, meetingId });
      }
    }

    res.json(result);
  } catch (err) {
    // eslint-disable-next-line no-console
    console.error('[recording] start failed', err);
    res.status(500).json({ error: err.message });
  }
});

app.post('/recordings/stop', async (req, res) => {
  try {
    const { roomId } = req.body || {};
    if (!roomId) {
      res.status(400).json({ error: 'roomId required' });
      return;
    }
    // eslint-disable-next-line no-console
    console.log('[recording] stop request', { roomId });

    // 실시간 발화 감지 종료
    const speechDetector = speechDetectors.get(roomId);
    if (speechDetector) {
      await speechDetector.stop();
      speechDetectors.delete(roomId);
      // eslint-disable-next-line no-console
      console.log('[speech] SpeechDetector stopped', { roomId });
    }

    const result = await recordingManager.stopRecording({ roomId });
    // eslint-disable-next-line no-console
    console.log('[recording] stop response', { roomId, result });

    // AI 서버로 녹음 파일 업로드 요청 (비동기, 실패해도 응답은 반환)
    if (result.status === 'stopped' && result.outputPath && result.meetingId) {
      const aiServerUrl = config.aiServerUrl;
      // eslint-disable-next-line no-console
      console.log('[recording] uploading to AI server', {
        aiServerUrl,
        meetingId: result.meetingId,
        outputPath: result.outputPath
      });
      // 비동기로 AI 서버에 업로드 요청 (응답 대기하지 않음)
      fetch(`${aiServerUrl}/api/upload-recording`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          meeting_id: result.meetingId,
          file_path: result.outputPath
        })
      })
        .then((uploadRes) => uploadRes.json())
        .then((uploadResult) => {
          // eslint-disable-next-line no-console
          console.log('[recording] AI upload result', { meetingId: result.meetingId, uploadResult });
        })
        .catch((uploadErr) => {
          // eslint-disable-next-line no-console
          console.error('[recording] AI upload failed', { meetingId: result.meetingId, error: uploadErr.message });
        });
    }

    res.json(result);
  } catch (err) {
    // eslint-disable-next-line no-console
    console.error('[recording] stop failed', err);
    res.status(500).json({ error: err.message });
  }
});

app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

createWorkers()
  .then(() => {
    recordingManager = createRecordingManager({ getOrCreateRoom, rooms, config });
    server.listen(config.port, () => {
      // eslint-disable-next-line no-console
      console.log(`SFU server listening on ${config.port}`);
      // eslint-disable-next-line no-console
      console.log(`SFU announcedIp: ${config.announcedIp}, listenIp: ${config.listenIp}`);
    });
  })
  .catch((err) => {
    // eslint-disable-next-line no-console
    console.error('Failed to start SFU server', err);
    process.exit(1);
  });
