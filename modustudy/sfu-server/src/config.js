const path = require('path');
const os = require('os');

// ffmpeg-static 패키지에서 ffmpeg 바이너리 경로 가져오기
let ffmpegStaticPath = null;
try {
  ffmpegStaticPath = require('ffmpeg-static');
  console.log('[config] ffmpeg-static loaded:', ffmpegStaticPath);
} catch (err) {
  console.warn('[config] ffmpeg-static not available, using system ffmpeg:', err.message);
}

// Docker 컨테이너 내부 IP 자동 감지
function getContainerIp() {
  const interfaces = os.networkInterfaces();
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) {
        return iface.address;
      }
    }
  }
  return '127.0.0.1';
}

const config = {
  // 멀티 Worker 설정: CPU 코어 수 또는 환경변수로 지정
  numWorkers: Number(process.env.NUM_WORKERS || Math.max(1, os.cpus().length)),
  port: Number(process.env.PORT || 4000),
  listenIp: process.env.LISTEN_IP || '0.0.0.0',
  announcedIp: process.env.ANNOUNCED_IP
    || process.env.SFU_ANNOUNCED_IP
    || getContainerIp(),
  corsOrigins: (process.env.CORS_ORIGINS || process.env.SFU_CORS_ORIGINS || '*')
    .split(',')
    .map((origin) => origin.trim())
    .filter((origin) => origin.length > 0),
  sslKeyPath: process.env.SFU_SSL_KEY_PATH
    || path.resolve(__dirname, '..', 'certs', 'key.pem'),
  sslCertPath: process.env.SFU_SSL_CERT_PATH
    || path.resolve(__dirname, '..', 'certs', 'cert.pem'),
  // Wider UDP port range to support more concurrent transports locally.
  rtcMinPort: Number(process.env.RTC_MIN_PORT || 20000),
  rtcMaxPort: Number(process.env.RTC_MAX_PORT || 22000),
  recordingsBasePath: process.env.RECORDINGS_BASE_PATH
    || path.resolve(__dirname, '..', '..', 'backend', 'uploads'),
  ffmpegPath: process.env.FFMPEG_PATH || ffmpegStaticPath || 'ffmpeg',
  recordingWidth: Number(process.env.RECORDING_WIDTH || 1280),
  recordingHeight: Number(process.env.RECORDING_HEIGHT || 720),
  recordingFps: Number(process.env.RECORDING_FPS || 30),
  recordingVideoBitrate: Number(process.env.RECORDING_VIDEO_BITRATE || 1000), // kbps
  recordingAudioBitrate: Number(process.env.RECORDING_AUDIO_BITRATE || 64), // kbps
  rtcEnableUdp: String(process.env.RTC_ENABLE_UDP ?? 'true').toLowerCase() !== 'false',
  rtcEnableTcp: String(process.env.RTC_ENABLE_TCP ?? 'true').toLowerCase() !== 'false',
  rtcPreferUdp: String(process.env.RTC_PREFER_UDP ?? 'true').toLowerCase() !== 'false',
  // TURN 서버 설정 (환경변수로 주입)
  turnUrl: process.env.TURN_URL || '',
  turnUsername: process.env.TURN_USERNAME || '',
  turnCredential: process.env.TURN_CREDENTIAL || '',
  // AI 서버 URL (녹음 파일 업로드용)
  aiServerUrl: process.env.AI_SERVER_URL || 'http://localhost:8000',
  mediaCodecs: [
    {
      kind: 'audio',
      mimeType: 'audio/opus',
      clockRate: 48000,
      channels: 2
    },
    {
      kind: 'video',
      mimeType: 'video/VP8',
      clockRate: 90000,
      parameters: {
        'x-google-start-bitrate': 1000
      }
    }
  ]
};

console.log('[config] ffmpegPath:', config.ffmpegPath);

module.exports = config;
