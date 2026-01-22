require('dotenv').config();
const http = require('http');
const https = require('https');
const express = require('express');
const { Server } = require('socket.io');
const mediasoup = require('mediasoup');
const config = require('./config');
const fs = require('fs');

const app = express();
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
const corsOrigins = config.corsOrigins.includes('*') ? '*' : config.corsOrigins;
const io = new Server(server, {
  cors: {
    origin: corsOrigins,
    methods: ['GET', 'POST'],
    credentials: true
  }
});

const rooms = new Map();
let worker;

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
  peer.producers.forEach((producer) => producer.close());
  peer.transports.forEach((transport) => transport.close());
}

function listProducers(room, excludeSocketId) {
  const producerInfos = [];
  room.peers.forEach((peer, peerId) => {
    if (peerId === excludeSocketId) return;
    peer.producers.forEach((producer) => {
      producerInfos.push({ producerId: producer.id, peerId, kind: producer.kind });
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
        enableUdp: true,
        enableTcp: true,
        preferUdp: true,
        initialAvailableOutgoingBitrate: 1000000
      });

      const peer = getPeer(room, socket.id);
      peer.transports.set(transport.id, transport);

      transport.on('dtlsstatechange', (dtlsState) => {
        if (dtlsState === 'closed') {
          transport.close();
        }
      });

      callback({
        params: {
          id: transport.id,
          iceParameters: transport.iceParameters,
          iceCandidates: transport.iceCandidates,
          dtlsParameters: transport.dtlsParameters
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

  socket.on('produce', async ({ roomId, transportId, kind, rtpParameters }, callback) => {
    try {
      const room = await getOrCreateRoom(roomId);
      const peer = getPeer(room, socket.id);
      const transport = peer.transports.get(transportId);
      const producer = await transport.produce({ kind, rtpParameters });
      peer.producers.set(producer.id, producer);

      producer.on('transportclose', () => {
        producer.close();
        peer.producers.delete(producer.id);
      });

      socket.to(roomId).emit('newProducer', { producerId: producer.id, producerPeerId: socket.id, kind });
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
      const producer = peer?.producers.get(producerId);
      if (!producer) {
        callback({ closed: false });
        return;
      }
      producer.close();
      peer.producers.delete(producerId);
      socket.to(roomId).emit('producerClosed', { producerId, peerId: socket.id });
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
        await consumer.requestKeyFrame();
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
      if (room.peers.size === 0) {
        rooms.delete(roomId);
      }
    });
  });
});

app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

createWorker()
  .then(() => {
    server.listen(config.port, () => {
      // eslint-disable-next-line no-console
      console.log(`SFU server listening on ${config.port}`);
    });
  })
  .catch((err) => {
    // eslint-disable-next-line no-console
    console.error('Failed to start SFU server', err);
    process.exit(1);
  });
