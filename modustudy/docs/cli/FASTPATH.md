# Codex Fast Path (modustudy)

목적: 초기 탐색 시간을 줄이기 위해 주요 위치/파일을 빠르게 안내합니다.

## 공통 위치
- 프론트엔드: `frontend/`
- 백엔드: `backend/`
- 문서: `docs/`
- 배포/인프라: `docker-compose.yml`, `nginx/`

## 프론트엔드 경로
- 라우팅: `frontend/src/routes/index.tsx`
- 레이아웃: `frontend/src/layouts/`
- 공통 API: `frontend/src/api/`
- 회의: `frontend/src/features/meeting/`
  - 룸: `frontend/src/features/meeting/components/MeetingRoomPage.tsx`
  - 상세: `frontend/src/features/meeting/components/MeetingDetailPage.tsx`
  - API: `frontend/src/features/meeting/services/meetingApi.ts`
  - 타입: `frontend/src/features/meeting/types.ts`
- 스터디: `frontend/src/features/study/`
- 퀴즈: `frontend/src/features/quiz/`
- DM: `frontend/src/features/dm/`
- 친구: `frontend/src/features/friend/`
- 인증/프로필: `frontend/src/features/auth/`, `frontend/src/features/profile/`
- 채팅/보드/알림: `frontend/src/features/chat/`, `frontend/src/features/board/`, `frontend/src/features/notification/`

## 백엔드 경로
- 패키지 루트: `backend/src/main/java/com/ssafy/domain/`
- 회의: `backend/src/main/java/com/ssafy/domain/meeting/`
  - 컨트롤러: `backend/src/main/java/com/ssafy/domain/meeting/controller/MeetingController.java`
  - 서비스: `backend/src/main/java/com/ssafy/domain/meeting/service/MeetingService.java`
  - 엔티티/리포지토리: `backend/src/main/java/com/ssafy/domain/meeting/entity/`, `backend/src/main/java/com/ssafy/domain/meeting/repository/`
- 스터디: `backend/src/main/java/com/ssafy/domain/study/`
- 퀴즈: `backend/src/main/java/com/ssafy/domain/quiz/`
- DM/친구: `backend/src/main/java/com/ssafy/domain/dm/`, `backend/src/main/java/com/ssafy/domain/friend/`
- 파일 스토리지: `backend/src/main/java/com/ssafy/common/storage/LocalFileStorageService.java`
- 웹소켓: `backend/src/main/java/com/ssafy/common/websocket/`

## 테스트 경로
- 미팅 API: `backend/src/test/java/com/ssafy/domain/meeting/MeetingApiTest.java`
- 웹소켓: `backend/src/test/java/com/ssafy/domain/websocket/`
- 스터디: `backend/src/test/java/com/ssafy/domain/study/`
- 퀴즈: `backend/src/test/java/com/ssafy/domain/quiz/`
- DM/친구: `backend/src/test/java/com/ssafy/domain/dm/`, `backend/src/test/java/com/ssafy/domain/friend/`

## 빠른 검색 예시
- 회의 프론트: `rg -n "MeetingRoom|MeetingDetail|meetingApi" frontend/src/features/meeting -S`
- 회의 백엔드: `rg -n "MeetingController|MeetingService" backend/src/main/java/com/ssafy/domain/meeting -S`
- 스터디: `rg -n "StudyController|StudyService" backend/src/main/java/com/ssafy/domain/study -S`
- 퀴즈: `rg -n "QuizController|QuizService" backend/src/main/java/com/ssafy/domain/quiz -S`
