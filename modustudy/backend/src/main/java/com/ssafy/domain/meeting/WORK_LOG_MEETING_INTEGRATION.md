# ModuStudy Meeting + RTC Integration 기록

이 문서는 `C:\SSAFY\S14P11D106\modustudy` 기준으로 진행한 미팅/RTC 관련 백엔드 작업 내역을 정리한 것이다.

## 목차
- 1) 미팅 도메인 구현
- 2) API_08_미팅 구현 범위
- 3) STT 실시간 수집 (백엔드)
- 4) 요약 상태 관리
- 5) video-conference-server (WS 서버) 연동 준비
- 6) sfu-server (mediasoup) 연동 준비
- 7) 실행/환경 템플릿
- 8) 미완료/차후 작업

## 1) 미팅 도메인 구현

### ERD 반영 테이블
- `meeting`
- `meeting_participant`
- `meeting_transcript`
- `meeting_summary`
- `meeting_photo`

### 엔티티/상태
- `MeetingStatus`: `WAITING`, `IN_PROGRESS`, `ENDED`
- `SummaryStatus`: `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`

### 주요 동작
- 미팅 시작: `Meeting.start()`로 생성, 상태 `IN_PROGRESS`, 시작 시간 기록
- 미팅 종료: 종료 시간/참여자 수/기간 계산, 상태 `ENDED`
- 참여/퇴장: 참가자 join/leave 시간 갱신, 참여자 수 갱신
- 참가자 음소거: 참가자의 `is_muted` 변경
- 요약/키워드: JSON 문자열로 저장/조회
- 전사: 한 문장 단위 저장, timestamp 기준 조회
- 사진: 이미지 URL과 촬영 시각 저장

### 주요 파일
- `backend/src/main/java/com/ssafy/domain/meeting/entity/Meeting.java`
- `backend/src/main/java/com/ssafy/domain/meeting/entity/MeetingParticipant.java`
- `backend/src/main/java/com/ssafy/domain/meeting/entity/MeetingTranscript.java`
- `backend/src/main/java/com/ssafy/domain/meeting/entity/MeetingSummary.java`
- `backend/src/main/java/com/ssafy/domain/meeting/entity/MeetingPhoto.java`
- `backend/src/main/java/com/ssafy/domain/meeting/entity/MeetingStatus.java`
- `backend/src/main/java/com/ssafy/domain/meeting/entity/SummaryStatus.java`

## 2) API_08_미팅 구현 범위

### REST 엔드포인트
- `GET /api/v1/studies/{studyId}/meetings`
- `GET /api/v1/studies/{studyId}/meetings/{meetingId}`
- `POST /api/v1/studies/{studyId}/meetings`
- `PUT /api/v1/studies/{studyId}/meetings/{meetingId}/end`
- `POST /api/v1/studies/{studyId}/meetings/{meetingId}/join`
- `POST /api/v1/studies/{studyId}/meetings/{meetingId}/leave`
- `GET /api/v1/studies/{studyId}/meetings/{meetingId}/summary`
- `GET /api/v1/studies/{studyId}/meetings/{meetingId}/transcript`
- `POST /api/v1/studies/{studyId}/meetings/{meetingId}/transcript`
- `GET /api/v1/studies/{studyId}/meetings/{meetingId}/photos`
- `POST /api/v1/studies/{studyId}/meetings/{meetingId}/photos`
- `PUT /api/v1/studies/{studyId}/meetings/{meetingId}/keywords`
- `PUT /api/v1/studies/{studyId}/meetings/{meetingId}/participants/{userId}/mute`
- `PUT /api/v1/studies/{studyId}/meetings/{meetingId}/summary`

### 엔드포인트 기능 설명
- 목록 조회: 미팅 기록 목록 + 요약/전사/사진 유무 표시
- 상세 조회: 미팅 정보 + 참가자 + 요약 + 키워드
- 시작: 미팅 생성 + roomToken 발급 (임시 토큰)
- 종료: 종료 처리 + 요약 상태 반환
- 참여/퇴장: 참가자 상태 갱신 + 참여자 수 업데이트
- 요약 조회: 요약 완료 상태만 응답
- 요약 업데이트: AI 결과 저장 (summary/actionItems/keywords/status)
- 전사 조회/저장: STT 저장 및 페이지 조회
- 사진 조회/등록: 스냅샷 메타 저장 및 조회
- 키워드 업데이트: 요약 키워드만 갱신
- 음소거: 특정 참가자 muted 상태 변경

### 컨트롤러
- `backend/src/main/java/com/ssafy/domain/meeting/controller/MeetingController.java`
- Swagger `@Operation`으로 기능 설명 추가 완료

## 3) STT 실시간 수집 (백엔드)

### WebSocket
- `@MessageMapping("/studies/{studyId}/meetings/{meetingId}/stt")`
- 수신 시 전사 저장 후 브로드캐스트
- 브로드캐스트 경로: `/topic/studies/{studyId}/meetings/{meetingId}/stt`

### 처리 흐름
- STT 메시지 수신
- `meeting_transcript` 저장
- 저장 결과를 STT 메시지 형태로 브로드캐스트

### 주요 파일
- `backend/src/main/java/com/ssafy/domain/meeting/controller/MeetingSttController.java`
- `backend/src/main/java/com/ssafy/config/WebSocketConfig.java`
- `backend/src/main/resources/application.properties`

## 4) 요약 상태 관리

### 상태값
- `PENDING` → `PROCESSING` → `COMPLETED` or `FAILED`

### 흐름
- 미팅 종료 시 `PROCESSING`으로 전환
- 요약 조회 시 `COMPLETED`가 아니면 `SUMMARY_NOT_READY`
- 요약 업데이트 API로 결과 저장 가능

### 요약 업데이트 입력
- `summary`: 요약 본문
- `actionItems`: 액션 아이템 목록
- `keywords`: 키워드 목록
- `status`: 요약 상태

## 5) video-conference-server (WS 서버) 연동 준비

### 포트 및 CORS
- `SERVER_PORT` 기본값 8081
- `APP_CORS_ALLOWED_ORIGINS` 환경변수로 관리

### SFU 설정 응답 확장
- `/api/sfu/config` 응답에 `baseUrl`, `iceServers` 포함
- ICE 서버는 `APP_STUN_URL`, `APP_TURN_*`로 주입

### STOMP 토큰 처리
- STOMP CONNECT 헤더에서 토큰 추출 후 세션에 저장
- 헤더 키: `Authorization`, `authorization`, `token`

### 역할
- WebSocket 룸/채팅/이벤트 처리
- 프론트가 사용할 SFU 접속 정보 제공
- STOMP 연결 시 토큰 전달 경로 확보

### 주요 파일
- `video-conference-server/src/main/resources/application.yml`
- `video-conference-server/src/main/java/com/ssafy/conference/controller/RoomController.java`
- `video-conference-server/src/main/java/com/ssafy/conference/config/WebSocketConfig.java`
- `video-conference-server/src/main/java/com/ssafy/conference/config/StompAuthChannelInterceptor.java`
- `video-conference-server/src/main/java/com/ssafy/conference/config/SfuProperties.java`
- `video-conference-server/src/main/java/com/ssafy/conference/dto/SfuConfigResponse.java`
- `video-conference-server/src/main/java/com/ssafy/conference/dto/IceServerResponse.java`

## 6) sfu-server (mediasoup) 연동 준비

### 설정 변경
- CORS origin 환경변수 지원
- socket.io 연결 시 토큰 보관
- 인증서 기본 경로를 `sfu-server/certs` 기준으로 변경

### 역할
- mediasoup 기반 미디어 라우팅
- socket.io 기반 시그널링 처리
- ICE/DTLS transport 생성 및 관리

### 주요 파일
- `sfu-server/src/config.js`
- `sfu-server/src/index.js`

## 7) 실행/환경 템플릿

### 환경변수 예시
- `video-conference-server/env.example`
- `sfu-server/env.example`

### systemd 예시
- `video-conference-server/systemd/video-conference-server.service.example`
- `sfu-server/systemd/sfu-server.service.example`

### 구성 예시
- backend: 8080
- video-conference-server: 8081
- sfu-server: 4000

## 8) 미완료/차후 작업

- 프론트/RTC 클라이언트에서 실제 WS/SFU/HTTP 주소 적용
- STOMP/SFU 토큰 검증 로직(실제 인증 적용)
- EC2 배포 환경에서 도메인/SSL/포트 확정 후 재설정

## 9) API 요청/응답 예시

### 9-1) 미팅 목록 조회
Request
```http
GET /api/v1/studies/1/meetings?page=0&size=20
Authorization: Bearer {accessToken}
```

Response
```json
{
  "status": 200,
  "message": "Success",
  "content": [
    {
      "id": 1,
      "title": "1회차 스터디",
      "session": {
        "id": 1,
        "sessionNumber": 1,
        "title": null
      },
      "meetingType": "WEEKLY",
      "startedAt": "2025-01-15T19:00:00",
      "endedAt": "2025-01-15T20:30:00",
      "durationSeconds": 5400,
      "participantCount": 5,
      "hasSummary": true,
      "hasTranscript": true,
      "photoCount": 3
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 5,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

### 9-2) 미팅 시작
Request
```http
POST /api/v1/studies/1/meetings
Authorization: Bearer {accessToken}
Content-Type: application/json
```
```json
{
  "title": "2회차 스터디",
  "sessionId": 2,
  "workspaceId": 3,
  "meetingType": "DAILY",
  "autoShareSummary": true,
  "shareWorkspaceId": 12
}
```

Response
```json
{
  "status": 201,
  "message": "Created",
  "data": {
    "id": 2,
    "title": "2회차 스터디",
    "roomToken": "meeting-2",
    "status": "IN_PROGRESS",
    "meetingType": "DAILY",
    "recordingStatus": "RECORDING",
    "sttStatus": "PENDING",
    "summaryStatus": "PENDING"
  }
}
```

### 9-3) 미팅 종료
Request
```http
PUT /api/v1/studies/1/meetings/2/end
Authorization: Bearer {accessToken}
```

Response
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "durationSeconds": 5400,
    "participantCount": 5,
    "summaryStatus": "PROCESSING"
  }
}
```

### 9-4) 미팅 참여
Request
```http
POST /api/v1/studies/1/meetings/2/join
Authorization: Bearer {accessToken}
```

Response
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "roomToken": "meeting-2",
    "iceServers": []
  }
}
```

### 9-5) 미팅 요약 조회
Request
```http
GET /api/v1/studies/1/meetings/2/summary
Authorization: Bearer {accessToken}
```

Response
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "id": 10,
    "summary": "이번 미팅에서는 DP와 BFS를 정리했다.",
    "actionItems": [
      {
        "id": 11,
        "content": "백준 1000번 문제 풀기",
        "assigneeId": null,
        "status": "TODO"
      }
    ],
    "keywords": ["DP", "BFS"],
    "status": "DONE",
    "createdAt": "2025-01-15T20:35:00"
  }
}
```

### 9-6) 미팅 전사 저장
Request
```http
POST /api/v1/studies/1/meetings/2/transcript
Authorization: Bearer {accessToken}
Content-Type: application/json
```
```json
{
  "userId": 1,
  "content": "오늘은 DP 개념을 정리합시다.",
  "timestampSeconds": 120,
  "startMs": 120000,
  "endMs": 121500,
  "isFinal": true
}
```

Response
```json
{
  "status": 201,
  "message": "Created",
  "data": {
    "id": 100,
    "user": {
      "id": 1,
      "nickname": null
    },
    "content": "오늘은 DP 개념을 정리합시다.",
    "timestampSeconds": 120,
    "startMs": 120000,
    "endMs": 121500,
    "createdAt": "2025-01-15T19:02:00"
  }
}
```

### 9-7) 미팅 사진 등록
Request
```http
POST /api/v1/studies/1/meetings/2/photos
Authorization: Bearer {accessToken}
Content-Type: multipart/form-data
```
```
image: {file}
```

Response
```json
{
  "status": 201,
  "message": "Created",
  "data": {
    "id": 3,
    "imageUrl": "meeting/2/capture.png",
    "capturedAt": "2025-01-15T20:15:00",
    "isSelected": false
  }
}
```

### 9-8) STT WebSocket 메시지
Send
```
SEND /app/studies/1/meetings/2/stt
{"userId":1,"content":"실시간 STT 메시지","timestampSeconds":135,"startMs":135000,"endMs":136200,"isFinal":false}
```

Broadcast
```
SUBSCRIBE /topic/studies/1/meetings/2/stt
{
  "type": "STT",
  "data": {
    "userId": 1,
    "nickname": null,
    "content": "실시간 STT 메시지",
    "isFinal": false,
    "timestampSeconds": 135,
    "startMs": 135000,
    "endMs": 136200
  }
}
```

## 10) 미팅 기능 확장 (2025-03-08)

### ERD 확장 반영
- `meeting`: 유형/녹음/STT/요약 상태 + 요약 자동공유 옵션 추가
- `meeting_recording`: 녹음 메타데이터 저장
- `meeting_participant_summary`: 참가자별 요약 저장
- `meeting_action_item`: 액션 아이템 저장
- `meeting_transcript`: start/end ms 저장

### 상태값 변경
- `SummaryStatus`: `COMPLETED` → `DONE`
- `RecordingStatus`: `WAITING`, `RECORDING`, `READY`, `FAILED`
- `SttStatus`: `PENDING`, `PROCESSING`, `DONE`, `FAILED`
- `MeetingType`: `DAILY`, `WEEKLY`, `FREE`, `OTHER`
- `ActionItemStatus`: `TODO`, `DONE`

### 신규/확장 API
- 미팅 목록 필터: `GET /api/v1/studies/{studyId}/meetings?meetingType=&startDate=&endDate=`
- 녹음 조회/저장:
  - `GET /api/v1/studies/{studyId}/meetings/{meetingId}/recording`
  - `PUT /api/v1/studies/{studyId}/meetings/{meetingId}/recording`
- 참가자 요약:
  - `GET /api/v1/studies/{studyId}/meetings/{meetingId}/participant-summaries`
  - `PUT /api/v1/studies/{studyId}/meetings/{meetingId}/participant-summaries`
- 액션 아이템:
  - `GET /api/v1/studies/{studyId}/meetings/{meetingId}/action-items`
  - `POST /api/v1/studies/{studyId}/meetings/{meetingId}/action-items`
  - `PUT /api/v1/studies/{studyId}/meetings/{meetingId}/action-items/{actionItemId}`
- 내보내기:
  - `GET /api/v1/studies/{studyId}/meetings/{meetingId}/export?format=MARKDOWN|PDF`

### OpenPDF 적용
- 의존성: `com.github.librepdf:openpdf:1.3.39`
- PDF 렌더링: Markdown 기반 요약 내용을 PDF로 출력
- 폰트 설정: `application.properties`에 `meeting.pdf.font-path` 추가
  - 예: `meeting.pdf.font-path=C:/Windows/Fonts/malgun.ttf`

### 주요 변경 파일
- 엔티티/Enum:
  - `backend/src/main/java/com/ssafy/domain/meeting/entity/Meeting.java`
  - `backend/src/main/java/com/ssafy/domain/meeting/entity/MeetingRecording.java`
  - `backend/src/main/java/com/ssafy/domain/meeting/entity/MeetingParticipantSummary.java`
  - `backend/src/main/java/com/ssafy/domain/meeting/entity/MeetingActionItem.java`
  - `backend/src/main/java/com/ssafy/domain/meeting/entity/MeetingTranscript.java`
  - `backend/src/main/java/com/ssafy/domain/meeting/entity/MeetingType.java`
  - `backend/src/main/java/com/ssafy/domain/meeting/entity/RecordingStatus.java`
  - `backend/src/main/java/com/ssafy/domain/meeting/entity/SttStatus.java`
  - `backend/src/main/java/com/ssafy/domain/meeting/entity/ActionItemStatus.java`
  - `backend/src/main/java/com/ssafy/domain/meeting/entity/SummaryStatus.java`
- Repository:
  - `backend/src/main/java/com/ssafy/domain/meeting/repository/MeetingRepository.java`
  - `backend/src/main/java/com/ssafy/domain/meeting/repository/MeetingRecordingRepository.java`
  - `backend/src/main/java/com/ssafy/domain/meeting/repository/MeetingParticipantSummaryRepository.java`
  - `backend/src/main/java/com/ssafy/domain/meeting/repository/MeetingActionItemRepository.java`
  - `backend/src/main/java/com/ssafy/domain/meeting/repository/MeetingTranscriptRepository.java`
- Service/Controller:
  - `backend/src/main/java/com/ssafy/domain/meeting/service/MeetingService.java`
  - `backend/src/main/java/com/ssafy/domain/meeting/controller/MeetingController.java`
  - `backend/src/main/java/com/ssafy/domain/meeting/controller/MeetingSttController.java`
- 설정:
  - `backend/build.gradle`
  - `backend/src/main/resources/application.properties`

### 참고 문서
- `OpenPDF_GUIDE.md`

## 11) 확장 API 요청/응답 예시

### 11-1) 미팅 녹음 메타 저장
Request
```http
PUT /api/v1/studies/1/meetings/2/recording
Authorization: Bearer {accessToken}
Content-Type: application/json
```
```json
{
  "recordingUrl": "s3://modustudy/meetings/2/audio.wav",
  "format": "wav",
  "durationSeconds": 5400,
  "startedAt": "2025-01-15T19:00:00",
  "endedAt": "2025-01-15T20:30:00",
  "fileSize": 12345678,
  "status": "READY"
}
```

Response
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "id": 1,
    "recordingUrl": "s3://modustudy/meetings/2/audio.wav",
    "format": "wav",
    "durationSeconds": 5400,
    "startedAt": "2025-01-15T19:00:00",
    "endedAt": "2025-01-15T20:30:00",
    "fileSize": 12345678,
    "status": "READY",
    "createdAt": "2025-01-15T20:31:00"
  }
}
```

### 11-1-1) 미팅 녹음 조회
Request
```http
GET /api/v1/studies/1/meetings/2/recording
Authorization: Bearer {accessToken}
```

Response
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "id": 1,
    "recordingUrl": "s3://modustudy/meetings/2/audio.wav",
    "format": "wav",
    "durationSeconds": 5400,
    "startedAt": "2025-01-15T19:00:00",
    "endedAt": "2025-01-15T20:30:00",
    "fileSize": 12345678,
    "status": "READY",
    "createdAt": "2025-01-15T20:31:00"
  }
}
```

### 11-2) 참가자 요약 저장
Request
```http
PUT /api/v1/studies/1/meetings/2/participant-summaries
Authorization: Bearer {accessToken}
Content-Type: application/json
```
```json
[
  {
    "userId": 1,
    "summary": "DP 개념 정리와 예제 풀이를 진행함."
  },
  {
    "userId": 2,
    "summary": "BFS 실습 코드 리뷰를 맡음."
  }
]
```

Response
```json
{
  "status": 200,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "summary": "DP 개념 정리와 예제 풀이를 진행함.",
      "createdAt": "2025-01-15T20:40:00"
    },
    {
      "id": 2,
      "userId": 2,
      "summary": "BFS 실습 코드 리뷰를 맡음.",
      "createdAt": "2025-01-15T20:40:00"
    }
  ]
}
```

### 11-2-1) 참가자 요약 조회
Request
```http
GET /api/v1/studies/1/meetings/2/participant-summaries
Authorization: Bearer {accessToken}
```

Response
```json
{
  "status": 200,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "summary": "DP 개념 정리와 예제 풀이를 진행함.",
      "createdAt": "2025-01-15T20:40:00"
    },
    {
      "id": 2,
      "userId": 2,
      "summary": "BFS 실습 코드 리뷰를 맡음.",
      "createdAt": "2025-01-15T20:40:00"
    }
  ]
}
```

### 11-3) 액션 아이템 생성
Request
```http
POST /api/v1/studies/1/meetings/2/action-items
Authorization: Bearer {accessToken}
Content-Type: application/json
```
```json
{
  "content": "백준 1000번 문제 풀기",
  "assigneeId": 1,
  "status": "TODO"
}
```

Response
```json
{
  "status": 201,
  "message": "Created",
  "data": {
    "id": 11,
    "content": "백준 1000번 문제 풀기",
    "assigneeId": 1,
    "status": "TODO"
  }
}
```

### 11-3-1) 액션 아이템 조회
Request
```http
GET /api/v1/studies/1/meetings/2/action-items
Authorization: Bearer {accessToken}
```

Response
```json
{
  "status": 200,
  "message": "Success",
  "data": [
    {
      "id": 11,
      "content": "백준 1000번 문제 풀기",
      "assigneeId": 1,
      "status": "TODO"
    },
    {
      "id": 12,
      "content": "회의록 정리하기",
      "assigneeId": 2,
      "status": "DONE"
    }
  ]
}
```

### 11-4) 액션 아이템 수정
Request
```http
PUT /api/v1/studies/1/meetings/2/action-items/11
Authorization: Bearer {accessToken}
Content-Type: application/json
```
```json
{
  "assigneeId": 2,
  "status": "DONE"
}
```

Response
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "id": 11,
    "content": "백준 1000번 문제 풀기",
    "assigneeId": 2,
    "status": "DONE"
  }
}
```

### 11-5) 미팅 내보내기 (Markdown)
Request
```http
GET /api/v1/studies/1/meetings/2/export?format=MARKDOWN
Authorization: Bearer {accessToken}
```

Response (파일 다운로드)
```
Content-Type: text/markdown
Content-Disposition: attachment; filename=meeting-2.md
```

### 11-6) 미팅 내보내기 (PDF)
Request
```http
GET /api/v1/studies/1/meetings/2/export?format=PDF
Authorization: Bearer {accessToken}
```

Response (파일 다운로드)
```
Content-Type: application/pdf
Content-Disposition: attachment; filename=meeting-2.pdf
```
