const fs = require('fs');
const path = require('path');
const dgram = require('dgram');
const { spawn } = require('child_process');

const RTP_IP = process.env.RECORDING_RTP_IP || '127.0.0.1';
const KEEP_RECORDING_SEGMENTS =
  String(process.env.RECORDING_KEEP_SEGMENTS || '').toLowerCase() === 'true'
  || process.env.RECORDING_KEEP_SEGMENTS === '1';
const RECORDING_OVERLAP_MS = Number(process.env.RECORDING_OVERLAP_MS || 0);
const RECORDING_REFRESH_INTERVAL_MS = Number(process.env.RECORDING_REFRESH_INTERVAL_MS || 0);
const RECORDING_VIDEO_READY_TIMEOUT_MS = Number(process.env.RECORDING_VIDEO_READY_TIMEOUT_MS || 1200);
const RECORDING_VIDEO_READY_POLL_MS = Number(process.env.RECORDING_VIDEO_READY_POLL_MS || 150);
const reservedPorts = new Set();
const RECORDING_RTP_PORT_MIN = Number(process.env.RECORDING_RTP_PORT_MIN || 45000);
const RECORDING_RTP_PORT_MAX = Number(process.env.RECORDING_RTP_PORT_MAX || 47000);
let recordingPortCursor = RECORDING_RTP_PORT_MIN % 2 === 0
  ? RECORDING_RTP_PORT_MIN
  : RECORDING_RTP_PORT_MIN + 1;
const RECORDING_SWITCH_RETRY_MS = Number(process.env.RECORDING_SWITCH_RETRY_MS || 300);
const RECORDING_SWITCH_MAX_RETRIES = Number(process.env.RECORDING_SWITCH_MAX_RETRIES || 8);

const getFreeUdpPort = () =>
  new Promise((resolve, reject) => {
    const socket = dgram.createSocket('udp4');
    socket.on('error', (err) => {
      socket.close();
      reject(err);
    });
    socket.bind(0, () => {
      const { port } = socket.address();
      socket.close(() => resolve(port));
    });
  });

const isPortFree = (port) =>
  new Promise((resolve) => {
    const socket = dgram.createSocket('udp4');
    socket.once('error', () => {
      socket.close();
      resolve(false);
    });
    socket.bind(port, RTP_IP, () => {
      socket.close(() => resolve(true));
    });
  });

const isPortPairFree = async (port) => {
  const first = await isPortFree(port);
  if (!first) return false;
  return isPortFree(port + 1);
};

const getUniqueUdpPort = async (usedPorts) => {
  const rangeSize = Math.max(0, RECORDING_RTP_PORT_MAX - RECORDING_RTP_PORT_MIN + 1);
  const attempts = Math.ceil(rangeSize / 2);
  for (let attempt = 0; attempt < attempts; attempt += 1) {
    const candidate = recordingPortCursor;
    recordingPortCursor = recordingPortCursor + 2 > RECORDING_RTP_PORT_MAX
      ? (RECORDING_RTP_PORT_MIN % 2 === 0 ? RECORDING_RTP_PORT_MIN : RECORDING_RTP_PORT_MIN + 1)
      : recordingPortCursor + 2;
    if (
      usedPorts.has(candidate)
      || usedPorts.has(candidate + 1)
      || reservedPorts.has(candidate)
      || reservedPorts.has(candidate + 1)
    ) {
      continue;
    }
    // Check OS-level availability for RTP/RTCP pair to avoid RTCP conflicts.
    if (await isPortPairFree(candidate)) {
      usedPorts.add(candidate);
      usedPorts.add(candidate + 1);
      reservedPorts.add(candidate);
      reservedPorts.add(candidate + 1);
      return candidate;
    }
  }
  // Fallback to ephemeral port if pool is exhausted.
  let port = await getFreeUdpPort();
  while (
    usedPorts.has(port)
    || usedPorts.has(port + 1)
    || reservedPorts.has(port)
    || reservedPorts.has(port + 1)
  ) {
    port = await getFreeUdpPort();
  }
  usedPorts.add(port);
  usedPorts.add(port + 1);
  reservedPorts.add(port);
  reservedPorts.add(port + 1);
  return port;
};

const releaseUdpPorts = (ports, delayMs = 5000) => {
  if (!ports || ports.length === 0) return;
  setTimeout(() => {
    ports.forEach((port) => reservedPorts.delete(port));
  }, delayMs);
};

const escapeFfmpegPath = (value) => value.replace(/\\/g, '/').replace(/'/g, "'\\''");

const ensureDir = (targetDir) => {
  fs.mkdirSync(targetDir, { recursive: true });
};

const buildFmtpLine = (payloadType, params) => {
  if (!params || Object.keys(params).length === 0) {
    return null;
  }
  const serialized = Object.entries(params)
    .map(([key, value]) => `${key}=${value}`)
    .join(';');
  return `a=fmtp:${payloadType} ${serialized}`;
};

const pickPrimaryCodec = (rtpParameters, kind) => {
  if (!rtpParameters || !Array.isArray(rtpParameters.codecs)) return null;
  const lowerKind = String(kind).toLowerCase();
  return (
    rtpParameters.codecs.find((c) => c.mimeType?.toLowerCase().startsWith(`${lowerKind}/`) && !c.mimeType?.toLowerCase().includes('rtx'))
    || rtpParameters.codecs[0]
    || null
  );
};

const buildMediaSection = ({ kind, port, codec, ssrc, mid }) => {
  const mimeParts = codec.mimeType.split('/');
  const codecName = mimeParts[1].toUpperCase();
  const channels = codec.channels ? `/${codec.channels}` : '';
  const lines = [
    `m=${kind} ${port} RTP/AVPF ${codec.payloadType}`,
    `c=IN IP4 ${RTP_IP}`,
    `a=rtpmap:${codec.payloadType} ${codecName}/${codec.clockRate}${channels}`,
  ];
  const fmtp = buildFmtpLine(codec.payloadType, codec.parameters);
  if (fmtp) lines.push(fmtp);
  if (Array.isArray(codec.rtcpFeedback)) {
    codec.rtcpFeedback.forEach((fb) => {
      if (!fb || !fb.type) return;
      lines.push(`a=rtcp-fb:${codec.payloadType} ${fb.type}${fb.parameter ? ` ${fb.parameter}` : ''}`);
    });
  }
  lines.push(`a=rtcp-mux`);
  lines.push(`a=recvonly`);
  lines.push(`a=mid:${mid}`);
  if (ssrc) {
    lines.push(`a=ssrc:${ssrc} cname:mediasoup-record`);
  }
  return lines.join('\n');
};

const buildSdp = ({ audioConsumers, videoConsumer }) => {
  const lines = [
    'v=0',
    `o=- 0 0 IN IP4 ${RTP_IP}`,
    's=mediasoup-recording',
    `c=IN IP4 ${RTP_IP}`,
    't=0 0',
    'a=msid-semantic: WMS *',
  ];

  const mediaSections = [];
  let midIndex = 0;

  if (videoConsumer) {
    const { port, rtpParameters } = videoConsumer;
    const codec = pickPrimaryCodec(rtpParameters, 'video');
    if (!codec) return lines.join('\n');
    const ssrc = rtpParameters.encodings?.[0]?.ssrc;
    mediaSections.push(buildMediaSection({
      kind: 'video',
      port,
      codec,
      ssrc,
      mid: `v${midIndex++}`,
    }));
  }

  audioConsumers.forEach((consumer) => {
    const { port, rtpParameters } = consumer;
    const codec = pickPrimaryCodec(rtpParameters, 'audio');
    if (!codec) return;
    const ssrc = rtpParameters.encodings?.[0]?.ssrc;
    mediaSections.push(buildMediaSection({
      kind: 'audio',
      port,
      codec,
      ssrc,
      mid: `a${midIndex++}`,
    }));
  });

  return `${lines.join('\n')}\n${mediaSections.join('\n')}\n`;
};

const waitForExit = (child) =>
  new Promise((resolve) => {
    if (!child) {
      resolve();
      return;
    }
    child.once('exit', () => resolve());
  });

const createFallbackSegment = (ffmpegPath, outputPath, width, height, fps, videoBitrateKbps, audioBitrateKbps) =>
  new Promise((resolve) => {
    const args = [
      '-y',
      '-f', 'lavfi', '-i', `color=c=black:s=${width}x${height}:r=${fps}`,
      '-f', 'lavfi', '-i', 'anullsrc=channel_layout=stereo:sample_rate=48000',
      '-t', '1',
      '-c:v', 'libvpx', '-b:v', `${videoBitrateKbps}k`, '-r', String(fps), '-g', String(fps * 2),
      '-c:a', 'libopus', '-b:a', `${audioBitrateKbps}k`,
      '-f', 'webm',
      outputPath,
    ];
    const proc = spawn(ffmpegPath, args, { stdio: ['ignore', 'ignore', 'ignore'] });
    proc.on('exit', () => resolve());
    proc.on('error', () => resolve());
  });

const createGapSegment = (
  ffmpegPath,
  outputPath,
  width,
  height,
  fps,
  durationSeconds,
  videoBitrateKbps,
  audioBitrateKbps
) =>
  new Promise((resolve) => {
    const safeDuration = Math.max(0.1, durationSeconds);
    const args = [
      '-y',
      '-f', 'lavfi', '-i', `color=c=black:s=${width}x${height}:r=${fps}`,
      '-f', 'lavfi', '-i', 'anullsrc=channel_layout=stereo:sample_rate=48000',
      '-t', safeDuration.toFixed(3),
      '-c:v', 'libvpx', '-b:v', `${videoBitrateKbps}k`, '-r', String(fps), '-g', String(fps * 2),
      '-c:a', 'libopus', '-b:a', `${audioBitrateKbps}k`,
      '-f', 'webm',
      outputPath,
    ];
    const proc = spawn(ffmpegPath, args, { stdio: ['ignore', 'ignore', 'ignore'] });
    proc.on('exit', () => resolve());
    proc.on('error', () => resolve());
  });

const createSegmentProcess = async ({
  ffmpegPath,
  outputPath,
  width,
  height,
  fps,
  videoBitrateKbps,
  audioBitrateKbps,
  sdpPath,
  hasVideoRtp,
  hasAudioRtp,
  audioCount,
}) => {
  const args = ['-y', '-loglevel', 'error'];
  let videoInputIndex = null;
  let audioInputBase = null;
  let inputIndex = 0;
  let sdpIndex = null;
  let colorIndex = null;
  let anullIndex = null;

  if (sdpPath) {
    args.push('-protocol_whitelist', 'file,udp,rtp');
    args.push('-f', 'sdp', '-i', sdpPath);
    sdpIndex = inputIndex;
    inputIndex += 1;
  }

  if (!hasVideoRtp) {
    args.push('-f', 'lavfi', '-i', `color=c=black:s=${width}x${height}:r=${fps}`);
    colorIndex = inputIndex;
    inputIndex += 1;
  } else {
    videoInputIndex = sdpIndex;
  }

  if (!hasAudioRtp) {
    args.push('-f', 'lavfi', '-i', 'anullsrc=channel_layout=stereo:sample_rate=48000');
    anullIndex = inputIndex;
    inputIndex += 1;
    audioInputBase = anullIndex;
  } else {
    audioInputBase = sdpIndex;
  }

  if (!hasVideoRtp) {
    videoInputIndex = colorIndex;
  }

  const filterParts = [];
  const mapArgs = [];

  if (videoInputIndex !== null) {
    filterParts.push(
      `[${videoInputIndex}:v:0]scale=${width}:${height}:force_original_aspect_ratio=decrease,` +
      `pad=${width}:${height}:(ow-iw)/2:(oh-ih)/2:black[vout]`
    );
    mapArgs.push('-map', '[vout]');
  }

  if (hasAudioRtp) {
    const amixInputs = [];
    for (let idx = 0; idx < audioCount; idx += 1) {
      amixInputs.push(`[${audioInputBase}:a:${idx}]`);
    }
    if (amixInputs.length > 1) {
      filterParts.push(`${amixInputs.join('')}amix=inputs=${amixInputs.length}:normalize=0[aout]`);
      mapArgs.push('-map', '[aout]');
    } else if (amixInputs.length === 1) {
      mapArgs.push('-map', `${audioInputBase}:a:0`);
    }
  } else {
    mapArgs.push('-map', `${audioInputBase}:a:0`);
  }

  if (filterParts.length > 0) {
    args.push('-filter_complex', filterParts.join(';'));
  }

  args.push(...mapArgs);
  args.push('-c:v', 'libvpx', '-b:v', `${videoBitrateKbps}k`, '-r', String(fps), '-g', String(fps * 2));
  args.push('-c:a', 'libopus', '-b:a', `${audioBitrateKbps}k`);
  args.push('-f', 'webm', outputPath);

  const proc = spawn(ffmpegPath, args, { stdio: ['ignore', 'ignore', 'pipe'] });
  const stderrChunks = [];
  let exitInfo = null;
  proc.on('error', (err) => {
    // eslint-disable-next-line no-console
    console.error('[recording] ffmpeg spawn error', err);
  });
  proc.stderr.on('data', (data) => {
    const text = data.toString();
    stderrChunks.push(text);
    // eslint-disable-next-line no-console
    console.error('[recording] ffmpeg stderr', text.trim());
  });
  proc.on('exit', (code, signal) => {
    exitInfo = { code, signal };
    // eslint-disable-next-line no-console
    console.log('[recording] ffmpeg exit', { code, signal, outputPath });
  });
  return {
    proc,
    getExitInfo: () => exitInfo,
    getStderr: () => stderrChunks.join(''),
  };
};

const requestKeyFrame = (consumer) => {
  if (!consumer || consumer.closed || typeof consumer.requestKeyFrame !== 'function') return;
  Promise.resolve(consumer.requestKeyFrame()).catch(() => {
    // ignore keyframe errors
  });
};

const logProducerStats = async (label, producer, roomId) => {
  if (!producer || producer.closed) return;
  try {
    const stats = await producer.getStats();
    const report = Array.from(stats.values())[0] || {};
    const packetsSent = report.packetsSent ?? report.packetCount ?? report.packets;
    const bytesSent = report.bytesSent ?? report.byteCount ?? report.bytes;
    const packetsReceived = report.packetsReceived ?? report.packets;
    const bytesReceived = report.bytesReceived ?? report.bytes;
    // eslint-disable-next-line no-console
    console.log('[recording] producer stats', {
      roomId,
      producerId: producer.id,
      label,
      kind: producer.kind,
      type: report.type,
      ssrc: report.ssrc,
      packetsSent,
      bytesSent,
      packetsReceived,
      bytesReceived,
    });
  } catch {
    // ignore stats failures
  }
};

const logConsumerStats = async (label, consumer, transport, roomId, producerId) => {
  if (!consumer || consumer.closed) return;
  try {
    const stats = await consumer.getStats();
    const report = Array.from(stats.values())[0] || {};
    const packetsReceived = report.packetsReceived ?? report.packetCount ?? report.packets;
    const bytesReceived = report.bytesReceived ?? report.byteCount ?? report.bytes;
    const packetsSent = report.packetsSent ?? report.packetCount ?? report.packets;
    const bytesSent = report.bytesSent ?? report.byteCount ?? report.bytes;
    // eslint-disable-next-line no-console
    console.log('[recording] consumer stats', {
      roomId,
      producerId,
      label,
      kind: consumer.kind,
      paused: consumer.paused,
      producerPaused: consumer.producerPaused,
      type: report.type,
      ssrc: report.ssrc,
      packetsReceived,
      bytesReceived,
      packetsSent,
      bytesSent,
    });
  } catch {
    // ignore stats failures
  }
  if (transport && typeof transport.getStats === 'function') {
    try {
      const stats = await transport.getStats();
      const report = Array.from(stats.values())[0] || {};
      // eslint-disable-next-line no-console
      console.log('[recording] transport stats', {
        roomId,
        label,
        bytesSent: report.bytesSent,
        packetsSent: report.packetsSent,
      });
    } catch {
      // ignore
    }
  }
  if (transport && transport.tuple) {
    // eslint-disable-next-line no-console
    console.log('[recording] transport tuple', {
      roomId,
      label,
      tuple: transport.tuple,
      rtcpTuple: transport.rtcpTuple,
    });
  }
};

const waitForConsumerPackets = async (consumer, timeoutMs, pollMs) => {
  if (!consumer || consumer.closed) return false;
  const deadline = Date.now() + timeoutMs;
  const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
  while (Date.now() < deadline) {
    try {
      const stats = await consumer.getStats();
      const report = Array.from(stats.values())[0] || {};
      const packets =
        report.packetsReceived
        ?? report.packetCount
        ?? report.packets
        ?? report.packetsSent
        ?? 0;
      if (Number(packets) > 0) {
        return true;
      }
    } catch {
      // ignore stats failures
    }
    await delay(pollMs);
  }
  return false;
};

const createRecordingManager = ({ getOrCreateRoom, rooms, config }) => {
  const recordings = new Map();

  const enqueue = (roomId, task) => {
    const state = recordings.get(roomId);
    if (!state) return Promise.resolve();
    state.queue = (state.queue || Promise.resolve()).then(task).catch((err) => {
      // eslint-disable-next-line no-console
      console.error('[recording] task failed', err);
    });
    return state.queue;
  };

  const pickPresenterVideo = (room) => {
    let selected = null;
    let selectedMixed = null;
    room.peers.forEach((peer) => {
      peer.producers.forEach((entry) => {
        if (entry.kind !== 'video') return;
        if (!selected || entry.createdAt > selected.createdAt) {
          selected = entry;
        }
        if (entry.appData && entry.appData.source === 'mixed') {
          if (!selectedMixed || entry.createdAt > selectedMixed.createdAt) {
            selectedMixed = entry;
          }
        }
      });
    });
    const chosen = selectedMixed || selected;
    return chosen ? chosen.producer.id : null;
  };

  const listAudioProducers = (room) => {
    const ids = [];
    room.peers.forEach((peer) => {
      peer.producers.forEach((entry) => {
        if (entry.kind === 'audio') {
          ids.push(entry.producer.id);
        }
      });
    });
    return ids.sort();
  };

  const findProducer = (room, producerId) => {
    let found = null;
    room.peers.forEach((peer) => {
      peer.producers.forEach((entry) => {
        if (entry.producer.id === producerId) {
          found = entry.producer;
        }
      });
    });
    return found;
  };

  const stopSegment = async (state, segmentOverride = null) => {
    const segment = segmentOverride || state.currentSegment;
    if (!segment) return;
    if (segment.stopping) return;
    segment.stopping = true;
    if (!segmentOverride || segment === state.currentSegment) {
      state.currentSegment = null;
    }
    if (segment.ffmpeg) {
      segment.ffmpeg.kill('SIGINT');
      await waitForExit(segment.ffmpeg);
    }
    if (segment.segmentPath) {
      try {
        const exists = fs.existsSync(segment.segmentPath);
        const size = exists ? fs.statSync(segment.segmentPath).size : 0;
        if (!exists || size === 0) {
          await createFallbackSegment(
            config.ffmpegPath,
            segment.segmentPath,
            config.recordingWidth,
            config.recordingHeight,
            config.recordingFps,
            config.recordingVideoBitrate,
            config.recordingAudioBitrate
          );
        }
      } catch {
        // ignore fallback failures
      }
    }
    releaseUdpPorts(segment.ports);
    segment.consumers.forEach((consumer) => {
      try {
        consumer.close();
      } catch {
        // ignore
      }
    });
    segment.transports.forEach((transport) => {
      try {
        transport.close();
      } catch {
        // ignore
      }
    });
  };

  const startSegment = async (roomId, videoProducerId, audioProducerIds) => {
    const state = recordings.get(roomId);
    if (!state) return;

    const room = rooms.get(roomId);
    if (!room) return;
    if (!videoProducerId) return;

    const usedPorts = new Set();
    const segmentIndex = state.segments.length;
    const segmentsDir = state.segmentsDir;
    ensureDir(segmentsDir);
    const segmentPath = path.join(segmentsDir, `segment-${segmentIndex}.webm`);
    const sdpPath = path.join(segmentsDir, `segment-${segmentIndex}.sdp`);

    const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
    const maxAttempts = 3;
    for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
      const consumers = [];
      const transports = [];
      let videoConsumerInfo = null;
      let videoConsumerRef = null;
      const audioConsumerInfos = [];

      if (videoProducerId) {
        const producer = findProducer(room, videoProducerId);
        if (producer) {
          const transport = await room.router.createPlainTransport({
            listenIp: RTP_IP,
            rtcpMux: true,
            comedia: false,
          });
          const port = await getUniqueUdpPort(usedPorts);
          try {
            await transport.connect({ ip: RTP_IP, port });
            const consumer = await transport.consume({
              producerId: producer.id,
              rtpCapabilities: room.router.rtpCapabilities,
              paused: false,
              enableRtx: false,
            });
            await consumer.resume();
            requestKeyFrame(consumer);
            setTimeout(() => requestKeyFrame(consumer), 300);
            setTimeout(() => requestKeyFrame(consumer), 900);
            consumers.push(consumer);
            videoConsumerRef = consumer;
            transports.push(transport);
            videoConsumerInfo = { port, rtpParameters: consumer.rtpParameters, producerId: producer.id };
            // eslint-disable-next-line no-console
            console.log('[recording] video transport connected', {
              roomId,
              producerId: producer.id,
              port,
              tuple: transport.tuple,
              rtcpTuple: transport.rtcpTuple,
            });
          } catch (err) {
            // eslint-disable-next-line no-console
            console.error('[recording] video consume failed', { roomId, producerId: producer.id, err: String(err) });
            try {
              transport.close();
            } catch {
              // ignore
            }
          }
        } else {
          // eslint-disable-next-line no-console
          console.warn('[recording] video producer missing', { roomId, videoProducerId });
        }
      }

      for (const producerId of audioProducerIds) {
        const producer = findProducer(room, producerId);
        if (!producer) continue;
        const transport = await room.router.createPlainTransport({
          listenIp: RTP_IP,
          rtcpMux: true,
          comedia: false,
        });
        const port = await getUniqueUdpPort(usedPorts);
        try {
          await transport.connect({ ip: RTP_IP, port });
          const consumer = await transport.consume({
            producerId: producer.id,
            rtpCapabilities: room.router.rtpCapabilities,
            paused: false,
          });
          await consumer.resume();
          consumers.push(consumer);
          transports.push(transport);
          audioConsumerInfos.push({ port, rtpParameters: consumer.rtpParameters, producerId: producer.id });
          // eslint-disable-next-line no-console
          console.log('[recording] audio transport connected', {
            roomId,
            producerId: producer.id,
            port,
            tuple: transport.tuple,
            rtcpTuple: transport.rtcpTuple,
          });
        } catch (err) {
          // eslint-disable-next-line no-console
          console.error('[recording] audio consume failed', { roomId, producerId: producer.id, err: String(err) });
          try {
            transport.close();
          } catch {
            // ignore
          }
        }
      }

      let sdpContent = null;
      if (videoConsumerInfo || audioConsumerInfos.length > 0) {
        sdpContent = buildSdp({
          audioConsumers: audioConsumerInfos,
          videoConsumer: videoConsumerInfo,
        });
        fs.writeFileSync(sdpPath, sdpContent);
      }

        const { proc, getExitInfo, getStderr } = await createSegmentProcess({
          ffmpegPath: config.ffmpegPath,
          outputPath: segmentPath,
          width: config.recordingWidth,
          height: config.recordingHeight,
          fps: config.recordingFps,
          videoBitrateKbps: config.recordingVideoBitrate,
          audioBitrateKbps: config.recordingAudioBitrate,
          sdpPath: sdpContent ? sdpPath : null,
          hasVideoRtp: Boolean(videoConsumerInfo),
          hasAudioRtp: audioConsumerInfos.length > 0,
          audioCount: audioConsumerInfos.length,
        });

        if (videoConsumerInfo) {
          const videoConsumer = consumers.find((c) => c.kind === 'video');
          if (videoConsumer) {
            setTimeout(() => logConsumerStats('t+1s', videoConsumer, transports[0], roomId, videoConsumerInfo.producerId), 1000);
            setTimeout(() => logConsumerStats('t+3s', videoConsumer, transports[0], roomId, videoConsumerInfo.producerId), 3000);
          }
          const videoProducer = findProducer(room, videoConsumerInfo.producerId);
          if (videoProducer) {
            setTimeout(() => logProducerStats('t+1s', videoProducer, roomId), 1000);
            setTimeout(() => logProducerStats('t+3s', videoProducer, roomId), 3000);
          }
        }
        if (audioConsumerInfos.length > 0) {
          const audioConsumer = consumers.find((c) => c.kind === 'audio');
          if (audioConsumer) {
            setTimeout(() => logConsumerStats('t+1s', audioConsumer, transports.find((t) => t !== transports[0]), roomId, audioConsumerInfos[0].producerId), 1000);
          }
          const audioProducer = findProducer(room, audioConsumerInfos[0].producerId);
          if (audioProducer) {
            setTimeout(() => logProducerStats('t+1s', audioProducer, roomId), 1000);
          }
        }

        const exitedEarly = await Promise.race([
          waitForExit(proc).then(() => true),
          delay(400).then(() => false),
        ]);

        if (!exitedEarly) {
          if (videoConsumerInfo && videoConsumerRef) {
            const ready = await waitForConsumerPackets(
              videoConsumerRef,
              RECORDING_VIDEO_READY_TIMEOUT_MS,
              RECORDING_VIDEO_READY_POLL_MS
            );
            if (!ready) {
              try {
                proc.kill('SIGINT');
              } catch {
                // ignore
              }
              await waitForExit(proc);
              consumers.forEach((consumer) => {
                try {
                  consumer.close();
                } catch {
                  // ignore
                }
              });
              transports.forEach((transport) => {
                try {
                  transport.close();
                } catch {
                  // ignore
                }
              });
              releaseUdpPorts(Array.from(usedPorts));
              if (!KEEP_RECORDING_SEGMENTS) {
                try {
                  if (sdpContent) fs.rmSync(sdpPath, { force: true });
                  fs.rmSync(segmentPath, { force: true });
                } catch {
                  // ignore
                }
              }
              return null;
            }
          }
          if (!state.firstVideoAt && videoConsumerInfo) {
            state.firstVideoAt = Date.now();
          }
        const nextSegment = {
          ffmpeg: proc,
          consumers,
          transports,
          segmentPath,
          sdpPath: sdpContent ? sdpPath : null,
          ports: Array.from(usedPorts),
          videoProducerId,
          startedAt: Date.now(),
          key: `${videoProducerId || 'blank'}|${audioProducerIds.join(',')}`,
          stopping: false,
        };
        state.currentSegment = nextSegment;
        state.segments.push(segmentPath);
        return nextSegment;
      }

      const stderrText = getStderr();
      const exitInfo = getExitInfo();
      // eslint-disable-next-line no-console
      console.warn('[recording] ffmpeg exited early', { roomId, attempt, exitInfo });
      consumers.forEach((consumer) => {
        try {
          consumer.close();
        } catch {
          // ignore
        }
      });
      transports.forEach((transport) => {
        try {
          transport.close();
        } catch {
          // ignore
        }
      });
      releaseUdpPorts(Array.from(usedPorts));
      if (!KEEP_RECORDING_SEGMENTS) {
        try {
          if (sdpContent) fs.rmSync(sdpPath, { force: true });
          fs.rmSync(segmentPath, { force: true });
        } catch {
          // ignore
        }
      }

      if (!stderrText.includes('bind failed') || attempt === maxAttempts) {
        try {
          await createFallbackSegment(
            config.ffmpegPath,
            segmentPath,
            config.recordingWidth,
            config.recordingHeight,
            config.recordingFps,
            config.recordingVideoBitrate,
            config.recordingAudioBitrate
          );
          state.segments.push(segmentPath);
        } catch {
          // ignore
        }
        return null;
      }
      await delay(150);
    }
  };

  const refreshRecording = async (roomId) => {
      const state = recordings.get(roomId);
      if (!state) return;
      const room = rooms.get(roomId);
      if (!room) return;

      const nextVideo = pickPresenterVideo(room);
      const nextAudio = listAudioProducers(room);
      const nextKey = `${nextVideo || 'blank'}|${nextAudio.join(',')}`;
      const scheduleRetry = () => {
        if (!state.switchRetry) {
          state.switchRetry = { key: nextKey, attempts: 0, timer: null };
        }
        if (state.switchRetry.key !== nextKey) {
          state.switchRetry.key = nextKey;
          state.switchRetry.attempts = 0;
        }
        if (state.switchRetry.timer) return;
        if (state.switchRetry.attempts >= RECORDING_SWITCH_MAX_RETRIES) return;
        state.switchRetry.attempts += 1;
        state.switchRetry.timer = setTimeout(() => {
          state.switchRetry.timer = null;
          enqueue(roomId, () => refreshRecording(roomId));
        }, RECORDING_SWITCH_RETRY_MS);
      };

      if (!state.currentSegment) {
        const started = await startSegment(roomId, nextVideo, nextAudio);
        if (!started) {
          scheduleRetry();
        } else if (state.switchRetry) {
          state.switchRetry.key = null;
          state.switchRetry.attempts = 0;
        }
        state.pendingGapStartedAt = null;
        return;
      }
      if (state.currentSegment.key !== nextKey) {
        await stopSegment(state);
        if (!state.pendingGapStartedAt) {
          state.pendingGapStartedAt = Date.now();
        }
        const nextSegment = await startSegment(roomId, nextVideo, nextAudio);
        if (!nextSegment) {
          scheduleRetry();
          return;
        }
        if (state.switchRetry) {
          state.switchRetry.key = null;
          state.switchRetry.attempts = 0;
        }
        if (state.pendingGapStartedAt) {
          const gapMs = Math.max(0, Date.now() - state.pendingGapStartedAt);
          state.pendingGapStartedAt = null;
          if (gapMs >= 120) {
            const gapIndex = state.segments.lastIndexOf(nextSegment.segmentPath);
            const gapPath = path.join(
              state.segmentsDir,
              `gap-${Date.now()}-${Math.random().toString(36).slice(2, 6)}.webm`
            );
            try {
              await createGapSegment(
                config.ffmpegPath,
                gapPath,
                config.recordingWidth,
                config.recordingHeight,
                config.recordingFps,
                gapMs / 1000,
                config.recordingVideoBitrate,
                config.recordingAudioBitrate
              );
              if (gapIndex === -1) {
                state.segments.push(gapPath);
              } else {
                state.segments.splice(gapIndex, 0, gapPath);
              }
            } catch {
              // ignore gap failures
            }
          }
        }
      }
    };

  const startRecording = async ({ roomId, meetingId }) => {
    if (recordings.has(roomId)) {
      return { status: 'already-recording' };
    }
    const room = await getOrCreateRoom(roomId);
    if (!room) {
      throw new Error('ROOM_NOT_FOUND');
    }
    const outputDir = path.join(
      config.recordingsBasePath,
      'meetings',
      String(meetingId),
      'recordings',
      'video'
    );
    const segmentsDir = path.join(outputDir, 'segments');
    ensureDir(segmentsDir);
    const state = {
      roomId,
      meetingId,
      outputDir,
      segmentsDir,
      segments: [],
      currentSegment: null,
      stopping: false,
      startedAt: Date.now(),
      firstVideoAt: null,
      queue: Promise.resolve(),
      switchRetry: { key: null, attempts: 0, timer: null },
      refreshTimer: null,
      pendingGapStartedAt: null,
    };
    recordings.set(roomId, state);
    await refreshRecording(roomId);
    if (RECORDING_REFRESH_INTERVAL_MS > 0) {
      state.refreshTimer = setInterval(() => {
        enqueue(roomId, () => refreshRecording(roomId));
      }, RECORDING_REFRESH_INTERVAL_MS);
    }
    return { status: 'started' };
  };

  const concatSegments = async (state) => {
    if (!state || state.segments.length === 0) {
      return null;
    }
    const existingSegments = state.segments.filter((segmentPath) => {
      try {
        return fs.existsSync(segmentPath) && fs.statSync(segmentPath).size > 0;
      } catch {
        return false;
      }
    });
    if (existingSegments.length === 0) {
      // eslint-disable-next-line no-console
      console.warn('[recording] concat skipped: no segments exist', { roomId: state.roomId });
      return null;
    }
    const concatFile = path.join(state.outputDir, 'segments.txt');
    const contents = existingSegments
      .map((segmentPath) => `file '${escapeFfmpegPath(segmentPath)}'`)
      .join('\n');
    fs.writeFileSync(concatFile, contents);
    const outputPath = path.join(state.outputDir, 'meeting.webm');
    await new Promise((resolve, reject) => {
      const args = ['-y', '-f', 'concat', '-safe', '0', '-i', concatFile, '-c', 'copy', outputPath];
      // eslint-disable-next-line no-console
      console.log('[recording] concat start', { roomId: state.roomId, segments: existingSegments.length });
      const proc = spawn(config.ffmpegPath, args, { stdio: ['ignore', 'ignore', 'pipe'] });
      proc.on('error', (err) => {
        reject(err);
      });
      proc.stderr.on('data', (data) => {
        // eslint-disable-next-line no-console
        console.error('[recording] concat stderr', data.toString().trim());
      });
      proc.on('exit', (code) => {
        if (code === 0) resolve();
        else reject(new Error(`concat failed (${code})`));
      });
    });
    // eslint-disable-next-line no-console
    console.log('[recording] concat done', { roomId: state.roomId, outputPath });
    if (!KEEP_RECORDING_SEGMENTS) {
      try {
        fs.rmSync(state.segmentsDir, { recursive: true, force: true });
        fs.rmSync(concatFile, { force: true });
      } catch {
        // ignore cleanup errors
      }
    }
    return outputPath;
  };

  const stopRecording = async ({ roomId }) => {
    const state = recordings.get(roomId);
    if (!state) {
      return { status: 'not-recording' };
    }
    if (state.stopping) {
      return { status: 'stopping' };
    }
    state.stopping = true;
    // eslint-disable-next-line no-console
    console.log('[recording] stop start', { roomId });
    await enqueue(roomId, async () => {
      await stopSegment(state);
    });
    try {
      if (state.refreshTimer) {
        clearInterval(state.refreshTimer);
        state.refreshTimer = null;
      }
      if (state.switchRetry?.timer) {
        clearTimeout(state.switchRetry.timer);
        state.switchRetry.timer = null;
      }
      const outputPath = await concatSegments(state);
      recordings.delete(roomId);
      if (!outputPath) {
        return { status: 'stopped', outputPath: null };
      }
      const fileSize = fs.existsSync(outputPath) ? fs.statSync(outputPath).size : null;
      const videoStartOffsetMs =
        state.firstVideoAt && state.startedAt ? Math.max(0, state.firstVideoAt - state.startedAt) : null;
      return { status: 'stopped', outputPath, fileSize, videoStartOffsetMs };
    } catch (err) {
      // eslint-disable-next-line no-console
      console.error('[recording] concat failed', err);
      recordings.delete(roomId);
      return { status: 'failed', outputPath: null };
    }
  };

  const onProducersChanged = (roomId) => {
    if (!recordings.has(roomId)) return;
    enqueue(roomId, () => refreshRecording(roomId));
  };

  return {
    startRecording,
    stopRecording,
    onProducersChanged,
    recordings,
  };
};

module.exports = { createRecordingManager };
