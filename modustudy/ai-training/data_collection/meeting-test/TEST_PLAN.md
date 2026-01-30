# AI 회의록 처리 시스템 테스트 계획서

## 1. 개요

### 1.1 테스트 목적
- `ai-inference` 서버의 STT 및 요약 기능 검증
- 백엔드 `MeetingAiProcessingService`의 AI 처리 로직 검증
- 화자별/전체 음성 파일 기반 회의록 처리 흐름 검증

### 1.2 테스트 범위
| 구성요소 | 테스트 대상 |
|---------|-----------|
| AI Inference Server | `/api/stt`, `/api/summarize`, `/api/process-meeting-full` |
| Backend Service | `MeetingAiProcessingService`, `MeetingSttService` |
| Database | `meeting_audio_recording`, `meeting_transcript`, `meeting_stt_summary` 테이블 |

### 1.3 테스트 데이터
- **회의록 수**: 10개 (meeting_id: 101~110)
- **총 발언 수**: 336개
- **주제**: SQLD, AWS, CS/데이터베이스, Vue, 이력서/자소서, 알고리즘/백준, 딥러닝, 기술면접, TypeScript, Node.js

---

## 2. 테스트 환경 설정

### 2.1 필수 사전 조건
```bash
# 1. AI Inference Server 실행
cd modustudy/ai-inference
python inference_server.py

# 2. Backend 서버 실행
cd modustudy/backend
./gradlew bootRun

# 3. MySQL 데이터베이스 실행
docker-compose up -d mysql
```

### 2.2 테스트 데이터 설정
```bash
# 1. 화자별 음성 파일 생성 (edge-tts 필요)
cd modustudy/ai-training/data_collection/meeting-test
pip install edge-tts
python generate_tts.py

# 2. MIXED 음성 파일 생성 (ffmpeg 필요)
# ffmpeg 설치:
#   - Windows: choco install ffmpeg 또는 https://ffmpeg.org/download.html
#   - Mac: brew install ffmpeg
#   - Linux: apt install ffmpeg
python generate_mixed.py

# 3. 테스트 데이터 DB 삽입
mysql -u root -p modustudy < sql/insert_test_data.sql
mysql -u root -p modustudy < sql/insert_mixed_audio.sql

# 4. 음성 파일을 업로드 경로로 복사
# Windows
xcopy /E /I audio C:\uploads\meetings\

# Linux/Mac
cp -r audio/* /var/uploads/meetings/
```

### 2.3 디렉토리 구조
```
meeting-test/
├── audio/                          # 생성된 음성 파일
│   ├── meeting_101_mixed.mp3       # 미팅 101 전체 음성
│   ├── meeting_101_user_11_order_001.mp3  # 화자별 개별 음성
│   └── ...
├── sql/
│   ├── insert_test_data.sql        # 기본 테스트 데이터
│   └── insert_mixed_audio.sql      # MIXED 트랙 데이터
├── meeting_info.json               # 회의 메타데이터
├── utterances.json                 # 발언 상세 데이터
├── generate_tts.py                 # TTS 생성 스크립트
├── extract_meetings.py             # 회의록 추출 스크립트
└── TEST_PLAN.md                    # 이 문서
```

---

## 3. 테스트 케이스

### 3.1 AI Inference Server 테스트

#### TC-INF-001: STT 동기 처리
**목적**: 음성 파일을 텍스트로 변환하는 기능 검증

```bash
# 테스트 요청
curl -X POST http://localhost:8000/api/stt \
  -F "file=@audio/meeting_101_mixed.mp3"
```

**예상 결과**:
```json
{
  "text": "아 네네, 그럼 오늘 스터디 시작해볼게요...",
  "segments": [
    {"start": 0.0, "end": 5.2, "text": "아 네네, 그럼 오늘 스터디 시작해볼게요"},
    ...
  ],
  "language": "ko",
  "duration": 180.5
}
```

**검증 항목**:
- [ ] HTTP 200 응답
- [ ] text 필드에 한국어 텍스트 포함
- [ ] segments 배열에 시간 정보 포함
- [ ] duration이 0보다 큼

---

#### TC-INF-002: STT 비동기 처리
**목적**: 대용량 음성 파일의 비동기 처리 검증

```bash
# 1. 작업 등록
curl -X POST http://localhost:8000/api/stt/async \
  -F "file=@audio/meeting_101_mixed.mp3"

# 응답: {"job_id": "xxx", "status": "pending"}

# 2. 작업 상태 확인
curl http://localhost:8000/api/jobs/{job_id}
```

**검증 항목**:
- [ ] job_id 반환
- [ ] status가 pending → processing → completed 순서로 변경
- [ ] completed 시 result에 STT 결과 포함

---

#### TC-INF-003: 회의록 요약
**목적**: STT 결과를 요약하는 기능 검증

```bash
curl -X POST http://localhost:8000/api/summarize \
  -H "Content-Type: application/json" \
  -d '{
    "transcript": "배소희: 아 네네, 그럼 오늘 스터디 시작해볼게요. 오늘의 주제는 SQLD 문제 풀이인데요...",
    "max_tokens": 512,
    "temperature": 0.7
  }'
```

**예상 결과**:
```json
{
  "summary": "## 요약\n오늘 스터디에서는 SQLD 문제 풀이를 진행했습니다...",
  "tokens_used": 256
}
```

**검증 항목**:
- [ ] summary에 요약문 포함
- [ ] tokens_used > 0

---

#### TC-INF-004: 전체 회의 처리 (Full Pipeline)
**목적**: STT + 요약 + 키워드 + 액션아이템 통합 처리 검증

```bash
curl -X POST http://localhost:8000/api/process-meeting-full \
  -F "mixed_audio=@audio/meeting_101_mixed.mp3" \
  -F "individual_audios=@audio/meeting_101_user_11_order_001.mp3" \
  -F "individual_audios=@audio/meeting_101_user_12_order_002.mp3" \
  -F "user_ids=11,12" \
  -F "generate_quiz=true"
```

**예상 결과**:
```json
{
  "job_id": "xxx",
  "status": "pending",
  "message": "회의 전체 처리 작업이 등록되었습니다."
}
```

**완료 시 결과**:
```json
{
  "transcript": "전체 회의 텍스트...",
  "summary": "회의 요약문...",
  "keywords": ["SQLD", "SQL", "SELECT", "WHERE"],
  "action_items": [
    {"user_id": 11, "content": "다음 스터디까지 JOIN 문법 복습"},
    {"user_id": 12, "content": "문제집 2장 풀어오기"}
  ],
  "quiz": "{...퀴즈 JSON...}"
}
```

---

### 3.2 Backend Service 테스트

#### TC-BE-001: AI 처리 시작 API
**목적**: `MeetingAiProcessingService.startAiProcessing()` 검증

```bash
# 1. 로그인하여 토큰 획득
TOKEN=$(curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test_user_11@test.com","password":"test123"}' | jq -r '.data.accessToken')

# 2. AI 처리 시작
curl -X POST http://localhost:8080/api/v1/studies/100/meetings/101/ai/start \
  -H "Authorization: Bearer $TOKEN"
```

**검증 항목**:
- [ ] HTTP 200 응답
- [ ] job_id 반환
- [ ] meeting.summary_status가 PROCESSING으로 변경됨

---

#### TC-BE-002: AI 처리 결과 확인 API
**목적**: AI 처리 완료 후 결과 저장 검증

```bash
# AI 처리 결과 확인 및 저장
curl -X GET http://localhost:8080/api/v1/studies/100/meetings/101/ai/status?jobId={job_id} \
  -H "Authorization: Bearer $TOKEN"
```

**검증 항목**:
- [ ] meeting_stt_file 테이블에 STT 파일 경로 저장됨
- [ ] meeting_stt_summary 테이블에 요약/키워드/액션아이템 저장됨
- [ ] meeting.summary_status가 DONE으로 변경됨

---

### 3.3 데이터베이스 테스트

#### TC-DB-001: meeting_audio_recording 테이블 검증
```sql
-- INDIVIDUAL 트랙 확인
SELECT meeting_id, user_id, track_type, recording_url
FROM meeting_audio_recording
WHERE meeting_id = 101 AND track_type = 'INDIVIDUAL';

-- MIXED 트랙 확인
SELECT meeting_id, track_type, recording_url
FROM meeting_audio_recording
WHERE meeting_id = 101 AND track_type = 'MIXED';
```

**검증 항목**:
- [ ] INDIVIDUAL 트랙: user_id별로 레코드 존재
- [ ] MIXED 트랙: user_id NULL, 전체 음성 경로 저장

---

#### TC-DB-002: meeting_transcript 테이블 검증
```sql
-- 발언 내용 확인
SELECT mt.id, mt.user_id, u.nickname, mt.content, mt.timestamp_seconds
FROM meeting_transcript mt
JOIN user u ON mt.user_id = u.id
WHERE mt.meeting_id = 101
ORDER BY mt.timestamp_seconds;
```

**검증 항목**:
- [ ] 발언 수 = 35개 (meeting 101 기준)
- [ ] timestamp_seconds가 시간순 정렬됨
- [ ] user_id가 meeting_participant와 일치

---

## 4. 테스트 시나리오

### 4.1 E2E 시나리오: 완전한 회의록 처리 흐름

```
1. [사전조건] 테스트 데이터 삽입 완료
   ↓
2. [Backend API] POST /api/v1/studies/100/meetings/101/ai/start
   - meeting.summary_status → PROCESSING
   - AI Inference Server에 비동기 요청
   ↓
3. [AI Server] 음성 파일 처리
   - MIXED 오디오 STT 처리
   - 화자별(INDIVIDUAL) 오디오 STT 처리
   - LLM 요약 생성
   - Claude 검증 및 키워드/액션아이템 추출
   ↓
4. [Backend Scheduler] 주기적 상태 확인 (MeetingAiScheduler)
   - job_id로 AI Server 상태 조회
   - completed 시 결과 저장
   ↓
5. [결과 저장]
   - meeting_stt_file: STT 텍스트 파일 경로
   - meeting_stt_summary: 요약문, 키워드, 액션아이템
   - meeting.summary_status → DONE
   ↓
6. [검증] 프론트엔드에서 회의 요약 조회
   GET /api/v1/studies/100/meetings/101/summary
```

---

## 5. 성능 테스트

### 5.1 STT 처리 시간 측정
| 미팅 ID | 발언 수 | 예상 음성 길이 | STT 처리 시간 |
|--------|--------|--------------|-------------|
| 101 | 35 | ~3분 | 측정 필요 |
| 102 | 35 | ~3분 | 측정 필요 |
| ... | ... | ... | ... |

### 5.2 전체 파이프라인 처리 시간
```bash
# 시간 측정 스크립트
time curl -X POST http://localhost:8000/api/process-meeting-full \
  -F "mixed_audio=@audio/meeting_101_mixed.mp3" \
  -F "generate_quiz=true"
```

---

## 6. 문제 해결 가이드

### 6.1 자주 발생하는 오류

#### Whisper 모델 로드 실패
```
[WARNING] Whisper 로드 실패: ...
```
**해결**: `WHISPER_MODEL` 환경변수 확인, 모델 다운로드 필요

#### LLM 모델 파일 없음
```
[WARNING] LLM 모델 파일 없음: ./models/qwen3-8b-summarizer-q4km.gguf
```
**해결**: 모델 파일 다운로드 또는 `MODEL_PATH` 환경변수 설정

#### 음성 파일 찾기 실패
```
MIXED_AUDIO_NOT_FOUND
```
**해결**: 음성 파일 경로 확인, `generate_tts.py` 실행 여부 확인

---

## 7. 테스트 결과 기록

| 테스트 ID | 테스트명 | 결과 | 비고 |
|----------|---------|------|------|
| TC-INF-001 | STT 동기 처리 | ⬜ | |
| TC-INF-002 | STT 비동기 처리 | ⬜ | |
| TC-INF-003 | 회의록 요약 | ⬜ | |
| TC-INF-004 | 전체 회의 처리 | ⬜ | |
| TC-BE-001 | AI 처리 시작 | ⬜ | |
| TC-BE-002 | AI 처리 결과 확인 | ⬜ | |
| TC-DB-001 | audio_recording 검증 | ⬜ | |
| TC-DB-002 | transcript 검증 | ⬜ | |

---

## 8. 부록

### 8.1 테스트 데이터 회의 목록
| Meeting ID | 주제 | 발언 수 | 참가자 수 |
|------------|------|--------|----------|
| 101 | SQLD | 35 | 5 |
| 102 | AWS | 35 | 6 |
| 103 | CS/데이터베이스 | 35 | 4 |
| 104 | Vue | 33 | 5 |
| 105 | 이력서/자소서 | 33 | 4 |
| 106 | 알고리즘/백준 | 33 | 4 |
| 107 | 딥러닝 | 33 | 5 |
| 108 | 기술면접 | 33 | 5 |
| 109 | TypeScript | 33 | 5 |
| 110 | Node.js | 33 | 5 |

### 8.2 관련 파일 경로
- AI Inference Server: `modustudy/ai-inference/inference_server.py`
- Backend AI Service: `modustudy/backend/.../meeting/service/MeetingAiProcessingService.java`
- ERD: `modustudy/docs/sql/erd.sql`
- 테스트 데이터: `modustudy/ai-training/data_collection/meeting-test/`
