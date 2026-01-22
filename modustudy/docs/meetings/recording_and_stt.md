# Recording + STT Design (Local Test + AWS Expansion)

## Summary

- Current code has no recording/storage logic (SFU and Spring do not store media).
- WebRTC sends audio/video as separate tracks.
- For scalability, use a dedicated recorder service.

## 1) Recorder Service Design

### Choice: Dedicated Recorder Service (Recommended)

- SFU focuses on real-time routing.
- Recorder receives media and writes files.
- Better isolation and scaling.

### Components

- SFU: mediasoup (routing)
- Recorder: Node.js + mediasoup-client or GStreamer/FFmpeg
- Storage: local disk (test) -> S3 (prod)
- Spring: start/stop control + metadata storage

### Flow

1) Client requests recording start.
2) Spring tells recorder to start.
3) Recorder joins SFU and consumes tracks.
4) Write video/audio files (MP4 + WAV suggested).
5) On stop, upload and callback.
6) Spring updates DB.

### Recorder API (Example)

- POST /recordings/start
  - request: { "roomId": "...", "recordingId": "...", "target": "s3|local" }
- POST /recordings/stop
  - request: { "recordingId": "..." }
- POST /recordings/complete
  - request: { "recordingId": "...", "videoUrl": "...", "audioUrl": "..." }

### Implementation Options

- Option A: mediasoup-client + FFmpeg (common, mid complexity)
- Option B: GStreamer (stable but more setup)

## 2) STT/Summary Stack and Flow

### STT: AWS Transcribe

- Good for long audio.
- Provides timestamped JSON.

### Summary: AWS Bedrock (Claude or Titan)

- Summarize transcript into key points.
- Async processing recommended.

### Async Pipeline

1) Upload complete.
2) S3 event -> SNS/SQS.
3) Worker triggers Transcribe job.
4) On completion, store transcript.
5) Worker calls Bedrock for summary.
6) Update DB.

### Worker Design

- ECS/Fargate or Lambda.
- Store job status in DB for retries.
- Use SQS DLQ.

## 3) Spring API + DTO/Entity Design

### API Spec

- POST /api/recordings/start
  - request: { "roomId": "room-1" }
  - response: { "recordingId": 10, "status": "RECORDING" }

- POST /api/recordings/stop
  - request: { "recordingId": 10 }
  - response: { "status": "UPLOADING" }

- POST /api/recordings/complete
  - request: { "recordingId": 10, "videoUrl": "...", "audioUrl": "..." }
  - response: { "status": "READY" }

- POST /api/recordings/transcripts
  - request: { "recordingId": 10, "fullText": "...", "language": "ko", "sttProvider": "mock" }

- POST /api/recordings/summaries
  - request: { "recordingId": 10, "summaryText": "...", "model": "mock" }

- GET /api/recordings/room/{roomId}
  - response: [ { "recordingId": 10, "status": "READY", "durationSec": 360 } ]

- GET /api/recordings/{recordingId}
  - response: { "recordingId": 10, "videoUrl": "...", "audioUrl": "...", "transcript": "...", "summary": "..." }

### DTOs

- RecordingStartRequest { roomId }
- RecordingStartResponse { recordingId, status }
- RecordingStopRequest { recordingId }
- RecordingCompleteRequest { recordingId, videoUrl, audioUrl }
- TranscriptUpsertRequest { recordingId, fullText, language, sttProvider }
- SummaryUpsertRequest { recordingId, summaryText, model }
- RecordingDetailResponse { recordingId, roomId, status, videoUrl, audioUrl, transcript, summary }

### Entity Relations

- Recording 1:N Transcript (usually 1)
- Recording 1:N Summary (usually 1)
- ProcessingJob tracks async status

## DB Schema + DDL (MySQL)

```sql
CREATE TABLE recordings (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  room_id VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL, -- READY, RECORDING, UPLOADING, FAILED
  s3_video_url TEXT,
  s3_audio_url TEXT,
  started_at DATETIME,
  ended_at DATETIME,
  duration_sec INT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_recordings_room_id (room_id)
);

CREATE TABLE transcripts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  recording_id BIGINT NOT NULL,
  full_text LONGTEXT NOT NULL,
  language VARCHAR(16),
  stt_provider VARCHAR(32),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_transcripts_recording
    FOREIGN KEY (recording_id) REFERENCES recordings(id)
    ON DELETE CASCADE
);

CREATE TABLE summaries (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  recording_id BIGINT NOT NULL,
  summary_text TEXT NOT NULL,
  model VARCHAR(64),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_summaries_recording
    FOREIGN KEY (recording_id) REFERENCES recordings(id)
    ON DELETE CASCADE
);

CREATE TABLE processing_jobs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  recording_id BIGINT NOT NULL,
  job_type VARCHAR(32) NOT NULL, -- STT, SUMMARY
  status VARCHAR(32) NOT NULL, -- PENDING, RUNNING, SUCCESS, FAILED
  attempt_count INT NOT NULL DEFAULT 0,
  last_error TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_jobs_recording
    FOREIGN KEY (recording_id) REFERENCES recordings(id)
    ON DELETE CASCADE
);
```

## Local Test Notes

- Recorder writes to local disk.
- Spring stores file path only.
- STT/summary can be mocked.

## AWS Checklist

- HTTPS/WSS
- Restrict CORS
- S3 presigned URLs
- Async pipeline (SNS/SQS + worker)

## Project Artifacts Added

- Recorder skeleton: `recorder-server`
- Spring recording API + entities: `video-conference-server/src/main/java/com/ssafy/conference/recording`
- Mock processing support: `app.processing.mock` in `video-conference-server/src/main/resources/application.yml`

## How to Run (Local, Mock Mode)

1) Start Spring
2) Start recorder
   - `cd recorder-server`
   - `npm install`
   - `npm start`
3) Call Spring API
   - `POST /api/recordings/start` with roomId
   - `POST /api/recordings/stop` with recordingId
