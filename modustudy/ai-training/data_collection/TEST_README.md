# Meeting AI 서비스 테스트 가이드

ModuStudy의 Meeting AI 서비스 (STT, 요약, 키워드, 액션아이템) 통합 테스트 가이드입니다.

## 📋 테스트 개요

Meeting AI 서비스는 다음 기능을 제공합니다:

1. **STT (Speech-to-Text)**: 음성을 텍스트로 변환
2. **요약 (Summary)**: 회의록 요약 생성
3. **키워드 추출 (Keywords)**: 핵심 키워드 추출
4. **액션 아이템 (Action Items)**: 화자별 액션 아이템 생성

## 🔧 사전 준비

### 1. AI 서버 확인

```bash
# AI 서버 헬스체크
curl http://3.88.71.92:8000/health
```

**정상 응답 예시:**
```json
{
  "status": "ok",
  "llm_loaded": true,
  "whisper_loaded": true,
  "whisper_model": "large-v3"
}
```

### 2. 테스트 파일 확인

- `test_meeting.mp3`: 테스트용 음성 파일 (이 디렉토리에 위치)

### 3. Python 환경 (Python 테스트 실행 시)

```bash
pip install requests
```

## 🚀 테스트 실행 방법

### 방법 1: Python 스크립트 (권장)

```bash
cd C:\SSAFY\S14P11D106\modustudy\ai-training\data_collection
python test_meeting_ai_service.py
```

**실행 내용:**
1. AI 서버 헬스체크
2. STT 단독 테스트
3. Meeting 전체 처리 테스트 (STT + 요약 + 키워드 + 액션아이템)
4. 요약 단독 테스트

**결과 저장 경로:**
- `test_results/stt_result.json`: STT 전체 결과
- `test_results/stt_transcript.txt`: STT 텍스트
- `test_results/meeting_full_result.json`: Meeting 전체 처리 결과
- `test_results/meeting_transcript.txt`: 회의록 텍스트
- `test_results/meeting_summary.txt`: 요약 텍스트

### 방법 2: Java 통합 테스트

```bash
cd C:\SSAFY\S14P11D106\modustudy\backend

# 특정 테스트 실행
./gradlew test --tests MeetingAiIntegrationTest

# 또는 IDE에서 직접 실행
# src/test/java/com/ssafy/domain/meeting/MeetingAiIntegrationTest.java
```

**테스트 케이스:**
1. `testAiServerHealthCheck()`: AI 서버 상태 확인
2. `testMeetingProcessAsync()`: 미팅 전체 처리 비동기 테스트
3. `testSummarizeOnly()`: 요약 서비스 단독 테스트

### 방법 3: 기존 GPU 테스트 스크립트

```bash
cd C:\SSAFY\S14P11D106\modustudy\ai-training\data_collection
python test_stt_gpu.py
```

이 스크립트는 STT 기능만 테스트합니다.

## 📊 테스트 결과 예시

### 1. STT 결과

```json
{
  "text": "안녕하세요, 오늘 회의를 시작하겠습니다...",
  "language": "ko",
  "language_probability": 0.98,
  "segments": [
    {
      "start": 0.0,
      "end": 3.5,
      "text": "안녕하세요, 오늘 회의를 시작하겠습니다."
    }
  ]
}
```

### 2. Meeting 전체 처리 결과

```json
{
  "status": "completed",
  "result": {
    "transcript": "전체 회의록 텍스트...",
    "summary": "이번 회의에서는 사용자 인증 기능 개선에 대해 논의했습니다...",
    "keywords": ["인증", "JWT", "리프레시토큰", "보안"],
    "action_items": [
      {
        "user_id": 1,
        "content": "JWT 리프레시 토큰 구현"
      }
    ]
  }
}
```

## 🔍 트러블슈팅

### 문제 1: AI 서버 연결 실패

```
❌ 서버 연결 실패: Connection refused
```

**해결방법:**
- AI 서버(http://3.88.71.92:8000)가 실행 중인지 확인
- 네트워크 연결 확인

### 문제 2: Whisper 모델 미로드

```
⚠️  WARNING: Whisper 모델이 로드되지 않았습니다!
```

**해결방법:**
- AI 서버 재시작
- 서버 로그 확인

### 문제 3: 타임아웃

```
⚠️  타임아웃 (10분)
```

**해결방법:**
- 음성 파일이 너무 큰 경우 처리 시간이 오래 걸릴 수 있습니다
- 타임아웃 시간 조정 (스크립트 내 `max_wait` 변수)

## 📝 백엔드 서비스 플로우

### 1. Meeting 종료 시 자동 처리

```
Meeting 종료
  ↓
MeetingAiScheduler.triggerProcessing()  # 즉시 트리거
  ↓
MeetingAiScheduler.processMeetingIfReady()  # 음성 파일 확인
  ↓
MeetingAiProcessingService.startAiProcessing()  # AI 작업 시작
  ↓
AiService.processMeetingAsync()  # AI 서버에 요청
  ↓
MeetingAiScheduler.checkProcessingJobs()  # 15초마다 결과 폴링
  ↓
MeetingAiProcessingService.checkAndSaveAiResult()  # 결과 저장
```

### 2. 주요 컴포넌트

| 컴포넌트 | 역할 |
|---------|------|
| `MeetingAiScheduler` | AI 처리 스케줄링 (자동 트리거 + 폴링) |
| `MeetingAiProcessingService` | AI 처리 시작 및 결과 저장 |
| `AiService` | AI 서버 API 호출 (STT, 요약, 퀴즈 등) |
| `MeetingSttService` | STT 결과 저장 |
| `MeetingActionItemService` | 액션 아이템 저장 |

### 3. 데이터베이스

- `meeting_audio_recording`: 음성 녹음 파일 정보
- `meeting_stt_file`: STT 결과 파일
- `meeting_stt_summary`: 요약 + 키워드
- `meeting_action_item`: 액션 아이템

## 🎯 테스트 체크리스트

- [ ] AI 서버 헬스체크 정상
- [ ] STT 결과 생성 확인
- [ ] 요약 텍스트 품질 확인
- [ ] 키워드 추출 정확도 확인
- [ ] 액션 아이템 생성 확인
- [ ] 처리 시간 확인 (10분 이내)
- [ ] 결과 파일 저장 확인

## 📚 관련 문서

- [AI 서버 API 문서](../README.md)
- [Meeting 도메인 설계](../../docs/meeting-design.md)
- [ERD](../../docs/sql/ERD.sql)

---

**작성일**: 2026-01-30
**작성자**: Claude Code
