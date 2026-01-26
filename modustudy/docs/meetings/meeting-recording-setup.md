# 회의 녹화 설정 정리

## FFmpeg 설치
- 서버 녹화를 위해 FFmpeg를 설치했습니다.
- SFU가 FFmpeg를 실행해 회의 스트림을 녹화/인코딩합니다.
- PATH에 FFmpeg가 없으면 `FFMPEG_PATH` 환경변수로 실행 경로를 지정해야 합니다.

## 녹화 저장 위치
- 녹화 결과는 백엔드 업로드 경로에 저장됩니다.
  - `backend/uploads/meetings/{meetingId}/recordings/video/meeting.webm`
- 이 경로는 SFU의 `RECORDINGS_BASE_PATH`에서 파생됩니다.
- 현재 기본값:
  - `RECORDINGS_BASE_PATH=C:/SSAFY/S14P11D106/modustudy/backend/uploads`

## 저장 포맷 및 방식
- 포맷: **WebM** (용량이 가장 작은 편)
- 해상도: 720p 목표 (1280x720)
- 오디오: Opus 저비트레이트
- 녹화 방식:
  - **발표자 화면만** 녹화
  - 발표자 전환/공백 구간에는 **검은 화면 + 무음**으로 계속 녹화해 전체 타임라인을 유지
  - 여러 세그먼트를 생성한 뒤 회의 종료 시 하나의 `meeting.webm` 파일로 합침

## 주요 환경 변수
- SFU
  - `FFMPEG_PATH=ffmpeg`
  - `RECORDINGS_BASE_PATH=C:/SSAFY/S14P11D106/modustudy/backend/uploads`
- Backend (SFU 제어 API 호출)
  - `APP_SFU_CONTROL_URL=https://modustudy.local:4000`

