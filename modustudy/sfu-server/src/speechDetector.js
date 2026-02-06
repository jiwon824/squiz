/**
 * 실시간 발화 감지 모듈
 * mediasoup AudioLevelObserver를 사용하여 발화 시작/종료를 감지
 */
const fs = require('fs');
const path = require('path');
const dgram = require('dgram');
const { spawn } = require('child_process');

const RTP_IP = process.env.RECORDING_RTP_IP || '127.0.0.1';
const AI_SERVER_URL = process.env.AI_SERVER_URL || 'http://localhost:8000';

// 발화 감지 설정
const SPEECH_THRESHOLD = Number(process.env.SPEECH_THRESHOLD || -50);  // dB
const SPEECH_INTERVAL = Number(process.env.SPEECH_INTERVAL || 300);    // ms
const MIN_SPEECH_DURATION = Number(process.env.MIN_SPEECH_DURATION || 500);  // ms
const MAX_SPEECH_DURATION = Number(process.env.MAX_SPEECH_DURATION || 30000); // ms (30초)
const SPEECH_END_DELAY = Number(process.env.SPEECH_END_DELAY || 800);  // 무음 후 발화 종료 판정 지연

// 포트 관리
const speechReservedPorts = new Set();
const SPEECH_RTP_PORT_MIN = Number(process.env.SPEECH_RTP_PORT_MIN || 47000);
const SPEECH_RTP_PORT_MAX = Number(process.env.SPEECH_RTP_PORT_MAX || 48000);
let speechPortCursor = SPEECH_RTP_PORT_MIN % 2 === 0 ? SPEECH_RTP_PORT_MIN : SPEECH_RTP_PORT_MIN + 1;

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

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

const getFreeSpeechPort = async (usedPorts) => {
  const rangeSize = Math.max(0, SPEECH_RTP_PORT_MAX - SPEECH_RTP_PORT_MIN + 1);
  const attempts = Math.ceil(rangeSize / 2);
  for (let attempt = 0; attempt < attempts; attempt += 1) {
    const candidate = speechPortCursor;
    speechPortCursor = speechPortCursor + 2 > SPEECH_RTP_PORT_MAX
      ? (SPEECH_RTP_PORT_MIN % 2 === 0 ? SPEECH_RTP_PORT_MIN : SPEECH_RTP_PORT_MIN + 1)
      : speechPortCursor + 2;
    if (
      usedPorts.has(candidate)
      || speechReservedPorts.has(candidate)
    ) {
      continue;
    }
    if (await isPortFree(candidate)) {
      usedPorts.add(candidate);
      speechReservedPorts.add(candidate);
      return candidate;
    }
  }
  throw new Error('No free port available for speech segment');
};

const releaseSpeechPorts = (ports, delayMs = 3000) => {
  if (!ports || ports.length === 0) return;
  setTimeout(() => {
    ports.forEach((port) => speechReservedPorts.delete(port));
  }, delayMs);
};

const ensureDir = (targetDir) => {
  fs.mkdirSync(targetDir, { recursive: true });
};

const buildAudioSdp = ({ port, rtpParameters }) => {
  if (!rtpParameters || !rtpParameters.codecs || rtpParameters.codecs.length === 0) {
    return null;
  }
  const codec = rtpParameters.codecs.find((c) =>
    c.mimeType?.toLowerCase().startsWith('audio/') && !c.mimeType?.toLowerCase().includes('rtx')
  ) || rtpParameters.codecs[0];

  if (!codec) return null;

  const mimeParts = codec.mimeType.split('/');
  const codecName = mimeParts[1].toUpperCase();
  const channels = codec.channels ? `/${codec.channels}` : '';
  const ssrc = rtpParameters.encodings?.[0]?.ssrc;

  const lines = [
    'v=0',
    `o=- 0 0 IN IP4 ${RTP_IP}`,
    's=speech-segment',
    `c=IN IP4 ${RTP_IP}`,
    't=0 0',
    `m=audio ${port} RTP/AVPF ${codec.payloadType}`,
    `a=rtpmap:${codec.payloadType} ${codecName}/${codec.clockRate}${channels}`,
    'a=rtcp-mux',
    'a=recvonly',
    'a=mid:a0',
  ];

  if (codec.parameters && Object.keys(codec.parameters).length > 0) {
    const fmtp = Object.entries(codec.parameters)
      .map(([key, value]) => `${key}=${value}`)
      .join(';');
    lines.push(`a=fmtp:${codec.payloadType} ${fmtp}`);
  }

  if (ssrc) {
    lines.push(`a=ssrc:${ssrc} cname:speech-segment`);
  }

  return lines.join('\n') + '\n';
};

/**
 * SpeechDetector 클래스
 * AudioLevelObserver를 사용하여 실시간 발화 감지 및 세그먼트 녹음
 */
class SpeechDetector {
  constructor({ router, room, meetingId, config, onSegmentReady }) {
    this.router = router;
    this.room = room;
    this.meetingId = meetingId;
    this.config = config;
    this.onSegmentReady = onSegmentReady;

    this.audioLevelObserver = null;
    this.speakingState = new Map();  // producerId -> { speaking, startTime, userId, segment, endTimer }
    this.producerUserMap = new Map();  // producerId -> userId
    this.segmentIndex = 0;
    this.segmentsDir = null;
    this.stopped = false;
  }

  async initialize() {
    if (!this.router) {
      throw new Error('Router is required');
    }

    // 세그먼트 저장 디렉토리 생성
    this.segmentsDir = path.join(
      this.config.recordingsBasePath,
      'meetings',
      String(this.meetingId),
      'speech-segments'
    );
    ensureDir(this.segmentsDir);

    // AudioLevelObserver 생성
    this.audioLevelObserver = await this.router.createAudioLevelObserver({
      maxEntries: 10,
      threshold: SPEECH_THRESHOLD,
      interval: SPEECH_INTERVAL
    });

    this.setupEventHandlers();
  }

  setupEventHandlers() {
    // 발화 감지 이벤트
    this.audioLevelObserver.on('volumes', (volumes) => {
      if (this.stopped) return;

      const currentSpeakers = new Set(volumes.map(v => v.producer.id));

      // 새로 말하기 시작한 사람 감지
      for (const { producer, volume } of volumes) {
        const state = this.speakingState.get(producer.id);
        if (!state || !state.speaking) {
          this.onSpeechStart(producer.id, volume);
        } else {
          // 이미 말하고 있는 경우, 종료 타이머 취소
          if (state.endTimer) {
            clearTimeout(state.endTimer);
            state.endTimer = null;
          }
        }
      }

      // 말하기 멈춘 사람 감지 (지연 판정)
      for (const [producerId, state] of this.speakingState) {
        if (state.speaking && !currentSpeakers.has(producerId)) {
          // 이미 종료 타이머가 있으면 무시
          if (state.endTimer) continue;

          // 지연 후 발화 종료 판정
          state.endTimer = setTimeout(() => {
            state.endTimer = null;
            const currentState = this.speakingState.get(producerId);
            if (currentState && currentState.speaking) {
              this.onSpeechEnd(producerId);
            }
          }, SPEECH_END_DELAY);
        }
      }
    });

    // 완전 침묵 이벤트
    this.audioLevelObserver.on('silence', () => {
      if (this.stopped) return;

      // 모든 활성 발화 종료 처리 (지연 후)
      for (const [producerId, state] of this.speakingState) {
        if (state.speaking && !state.endTimer) {
          state.endTimer = setTimeout(() => {
            state.endTimer = null;
            const currentState = this.speakingState.get(producerId);
            if (currentState && currentState.speaking) {
              this.onSpeechEnd(producerId);
            }
          }, SPEECH_END_DELAY);
        }
      }
    });
  }

  /**
   * 오디오 Producer 추가
   */
  async addProducer(producer, userId) {
    if (producer.kind !== 'audio') return;

    try {
      await this.audioLevelObserver.addProducer({ producerId: producer.id });
      this.producerUserMap.set(producer.id, userId);
      this.speakingState.set(producer.id, {
        speaking: false,
        startTime: null,
        userId,
        segment: null,
        endTimer: null
      });
    } catch (err) {
    }
  }

  /**
   * 오디오 Producer 제거
   */
  async removeProducer(producerId) {
    const state = this.speakingState.get(producerId);
    if (state) {
      if (state.endTimer) {
        clearTimeout(state.endTimer);
      }
      if (state.speaking) {
        await this.onSpeechEnd(producerId);
      }
    }

    try {
      await this.audioLevelObserver.removeProducer({ producerId });
    } catch (err) {
    }

    this.speakingState.delete(producerId);
    this.producerUserMap.delete(producerId);
  }

  /**
   * 발화 시작 처리
   */
  async onSpeechStart(producerId, volume) {
    const state = this.speakingState.get(producerId);
    if (!state) return;

    const userId = this.producerUserMap.get(producerId);
    state.speaking = true;
    state.startTime = Date.now();


    try {
      const segment = await this.startSegmentRecording(producerId, userId);
      state.segment = segment;

      state.maxDurationTimer = setTimeout(() => {
        this.onSpeechEnd(producerId);
      }, MAX_SPEECH_DURATION);

    } catch (err) {
    }
  }

  /**
   * 발화 종료 처리
   */
  async onSpeechEnd(producerId) {
    const state = this.speakingState.get(producerId);
    if (!state || !state.speaking) return;

    state.speaking = false;
    const duration = Date.now() - state.startTime;
    const userId = this.producerUserMap.get(producerId);

    if (state.maxDurationTimer) {
      clearTimeout(state.maxDurationTimer);
      state.maxDurationTimer = null;
    }

    if (duration < MIN_SPEECH_DURATION) {
      if (state.segment) {
        await this.abortSegmentRecording(state.segment);
        state.segment = null;
      }
      return;
    }

    if (state.segment) {
      await this.finishAndUploadSegment(state.segment, userId, state.startTime, duration);
      state.segment = null;
    }
  }

  async startSegmentRecording(producerId, userId) {
    const producer = this.findProducer(producerId);
    if (!producer) {
      throw new Error('Producer not found');
    }

    const usedPorts = new Set();
    const port = await getFreeSpeechPort(usedPorts);

    // PlainTransport 생성
    const transport = await this.router.createPlainTransport({
      listenIp: RTP_IP,
      rtcpMux: true,
      comedia: false
    });

    await transport.connect({ ip: RTP_IP, port });

    // Consumer 생성
    const consumer = await transport.consume({
      producerId: producer.id,
      rtpCapabilities: this.router.rtpCapabilities,
      paused: false
    });

    await consumer.resume();

    // SDP 파일 생성
    const segmentId = this.segmentIndex++;
    const timestamp = Date.now();
    const segmentPath = path.join(
      this.segmentsDir,
      `user_${userId}_${timestamp}_${segmentId}.webm`
    );
    const sdpPath = path.join(
      this.segmentsDir,
      `user_${userId}_${timestamp}_${segmentId}.sdp`
    );

    const sdpContent = buildAudioSdp({
      port,
      rtpParameters: consumer.rtpParameters
    });

    if (sdpContent) {
      fs.writeFileSync(sdpPath, sdpContent);
    }

    // FFmpeg 프로세스 시작 (오디오만)
    const args = [
      '-y',
      '-loglevel', 'error',
      '-protocol_whitelist', 'file,udp,rtp',
      '-f', 'sdp',
      '-i', sdpPath,
      '-af', 'asetpts=N/SR/TB',
      '-c:a', 'libopus',
      '-b:a', '64k',
      '-f', 'webm',
      segmentPath
    ];

    const ffmpeg = spawn(this.config.ffmpegPath, args, {
      stdio: ['pipe', 'ignore', 'pipe']
    });

    ffmpeg.stderr.on('data', (data) => {
    });

    return {
      producerId,
      userId,
      segmentPath,
      sdpPath,
      ffmpeg,
      consumer,
      transport,
      ports: [port],
      timestamp
    };
  }

  /**
   * 세그먼트 녹음 중단 (짧은 발화)
   */
  async abortSegmentRecording(segment) {
    try {
      // FFmpeg 종료
      if (segment.ffmpeg && !segment.ffmpeg.killed) {
        segment.ffmpeg.kill('SIGKILL');
      }

      // Consumer/Transport 정리
      if (segment.consumer) {
        segment.consumer.close();
      }
      if (segment.transport) {
        segment.transport.close();
      }

      releaseSpeechPorts(segment.ports);

      // 파일 삭제
      if (fs.existsSync(segment.segmentPath)) {
        fs.unlinkSync(segment.segmentPath);
      }
      if (fs.existsSync(segment.sdpPath)) {
        fs.unlinkSync(segment.sdpPath);
      }
    } catch (err) {
    }
  }

  async finishAndUploadSegment(segment, userId, startTime, durationMs) {
    try {
      // FFmpeg 정상 종료
      if (segment.ffmpeg && !segment.ffmpeg.killed) {
        if (segment.ffmpeg.stdin && !segment.ffmpeg.stdin.destroyed) {
          try {
            segment.ffmpeg.stdin.write('q');
            segment.ffmpeg.stdin.end();
          } catch {
            // ignore
          }
        }

        // 종료 대기
        await new Promise((resolve) => {
          const timeout = setTimeout(() => {
            if (!segment.ffmpeg.killed) {
              segment.ffmpeg.kill('SIGKILL');
            }
            resolve();
          }, 2000);

          segment.ffmpeg.once('exit', () => {
            clearTimeout(timeout);
            resolve();
          });
        });
      }

      // Consumer/Transport 정리
      if (segment.consumer) {
        segment.consumer.close();
      }
      if (segment.transport) {
        segment.transport.close();
      }

      releaseSpeechPorts(segment.ports);

      // SDP 파일 삭제
      if (fs.existsSync(segment.sdpPath)) {
        fs.unlinkSync(segment.sdpPath);
      }

      if (!fs.existsSync(segment.segmentPath)) {
        return;
      }

      const fileSize = fs.statSync(segment.segmentPath).size;
      if (fileSize < 100) {
        fs.unlinkSync(segment.segmentPath);
        return;
      }

      this.uploadToAiServer({
        meetingId: this.meetingId,
        userId,
        timestamp: startTime,
        durationMs,
        filePath: segment.segmentPath
      });

      if (this.onSegmentReady) {
        this.onSegmentReady({
          meetingId: this.meetingId,
          userId,
          timestamp: startTime,
          durationMs,
          filePath: segment.segmentPath
        });
      }

    } catch (err) {
    }
  }

  async uploadToAiServer({ meetingId, userId, timestamp, durationMs, filePath }) {
    try {
      const response = await fetch(`${AI_SERVER_URL}/api/process-speech-segment`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          meeting_id: meetingId,
          user_id: userId,
          timestamp,
          duration_ms: durationMs,
          file_path: filePath
        })
      });

      const result = await response.json();

    } catch (err) {
    }
  }

  findProducer(producerId) {
    for (const peer of this.room.peers.values()) {
      for (const entry of peer.producers.values()) {
        if (entry.producer.id === producerId) {
          return entry.producer;
        }
      }
    }
    return null;
  }

  async stop() {
    this.stopped = true;

    for (const [producerId, state] of this.speakingState) {
      if (state.endTimer) {
        clearTimeout(state.endTimer);
      }
      if (state.maxDurationTimer) {
        clearTimeout(state.maxDurationTimer);
      }
      if (state.speaking && state.segment) {
        await this.finishAndUploadSegment(
          state.segment,
          state.userId,
          state.startTime,
          Date.now() - state.startTime
        );
      }
    }

    if (this.audioLevelObserver) {
      this.audioLevelObserver.close();
    }

    this.speakingState.clear();
    this.producerUserMap.clear();
  }
}

const createSpeechDetector = ({ router, room, meetingId, config, onSegmentReady }) => {
  return new SpeechDetector({ router, room, meetingId, config, onSegmentReady });
};

module.exports = { createSpeechDetector, SpeechDetector };
