const path = require('path');

const config = {
  port: Number(process.env.PORT || 4000),
  listenIp: process.env.LISTEN_IP || '127.0.0.1',
  announcedIp: process.env.ANNOUNCED_IP
    || process.env.SFU_ANNOUNCED_IP
    || process.env.LISTEN_IP
    || '127.0.0.1',
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
  ffmpegPath: process.env.FFMPEG_PATH || 'ffmpeg',
  recordingWidth: Number(process.env.RECORDING_WIDTH || 1280),
  recordingHeight: Number(process.env.RECORDING_HEIGHT || 720),
  recordingFps: Number(process.env.RECORDING_FPS || 30),
  recordingVideoBitrate: Number(process.env.RECORDING_VIDEO_BITRATE || 1000), // kbps
  recordingAudioBitrate: Number(process.env.RECORDING_AUDIO_BITRATE || 64), // kbps
  rtcEnableUdp: String(process.env.RTC_ENABLE_UDP ?? 'true').toLowerCase() !== 'false',
  rtcEnableTcp: String(process.env.RTC_ENABLE_TCP ?? 'true').toLowerCase() !== 'false',
  rtcPreferUdp: String(process.env.RTC_PREFER_UDP ?? 'true').toLowerCase() !== 'false',
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

module.exports = config;
