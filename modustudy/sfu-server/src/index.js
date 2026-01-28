require('dotenv').config();
const http = require('http');
const https = require('https');
const express = require('express');
const { Server } = require('socket.io');
const mediasoup = require('mediasoup');
const config = require('./config');
const fs = require('fs');
const { createRecordingManager } = require('./recordingManager');

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
let worker;
let recordingManager;

async function createWorker() {
  worker = await mediasoup.createWorker({
    rtcMinPort: config.rtcMinPort,
    rtcMaxPort: config.rtcMaxPort,
    logLevel: 'warn'
  });

  worker.on('died', () => {
    // eslint-disable-next-line no-console
    console.error('mediasoup worker died, exiting');
    process.exit(1);
  });
}

async function getOrCreateRoom(roomId) {
  if (rooms.has(roomId)) {
    return rooms.get(roomId);
  }
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
      producerInfos.push({ producerId: entry.producer.id, peerId, kind: entry.kind });
    });
  });
  return producerInfos;
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
      if (kind === 'video') {
        const existingVideoProducer = Array.from(peer.producers.values()).find((item) => item.kind === 'video');
        if (existingVideoProducer) {
          existingVideoProducer.producer.close();
          peer.producers.delete(existingVideoProducer.producer.id);
          socket.to(roomId).emit('producerClosed', { producerId: existingVideoProducer.producer.id, peerId: socket.id });
        }
      }
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
        if (recordingManager) {
          recordingManager.onProducersChanged(roomId);
        }
      });

      socket.to(roomId).emit('newProducer', { producerId: producer.id, producerPeerId: socket.id, kind });
      if (recordingManager) {
        recordingManager.onProducersChanged(roomId);
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
      entry.producer.close();
      peer.producers.delete(producerId);
      socket.to(roomId).emit('producerClosed', { producerId, peerId: socket.id });
      if (recordingManager) {
        recordingManager.onProducersChanged(roomId);
      }
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
      if (recordingManager) {
        recordingManager.onProducersChanged(roomId);
      }
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
    const result = await recordingManager.stopRecording({ roomId });
    // eslint-disable-next-line no-console
    console.log('[recording] stop response', { roomId, result });
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

createWorker()
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
