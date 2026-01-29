# 출석 기능 사용 가이드

이 문서는 출석 기능(ble/자동/셀프/수동/소명/캘린더) API를 어떻게 호출해서 쓰는지에 대한 사용 중심 가이드입니다.

## 공통 안내

- 기본 경로: `/api/v1/studies/{studyId}`
- 인증: 대부분 엔드포인트는 로그인 사용자 기준으로 동작합니다. 컨트롤러는 `@AuthenticationPrincipal SsafyUserDetails`를 사용합니다.
- 권한:
  - 스터디장만 가능한 기능: BLE 출석 시작, 출석 수동 변경, 실시간 출석 현황 조회, 소명 승인/거절
  - 스터디원 가능: BLE 출석 체크, 셀프 출석, 온라인 자동 출석, 소명 제출, 월별 캘린더 조회
- 상태 값(enum)
  - 출석 체크 방식: `BLE | SELF | AUTO`
  - 출석 상태: `PRESENT | LATE | ABSENT | EXCUSED`
  - 소명 상태: `PENDING | APPROVED | REJECTED`

---

## 1) BLE 출석

### 1-1. BLE 출석 시작 (스터디장)
- **의도**: 스터디장이 BLE 출석을 시작합니다. 세션 참석자 출석 row를 초기화합니다.
- **요청**
  - `POST /api/v1/studies/{studyId}/sessions/{sessionId}/attendance/ble/start`
- **응답**
  - `201 Created`
  - `ApiResponse<MessageResponse>`

### 1-2. BLE 출석 체크 (스터디원)
- **의도**: 스터디원이 BLE 비콘을 감지했을 때 출석 체크를 요청합니다.
- **요청**
  - `POST /api/v1/studies/{studyId}/sessions/{sessionId}/attendance/ble/check`
- **동작**
  - 출석 상태는 세션 시작 기준 10분 이후면 `LATE`, 그 이전이면 `PRESENT`로 기록됩니다.
- **응답**
  - `200 OK`
  - `ApiResponse<AttendanceResponse>`

---

## 2) 셀프 출석

### 셀프 출석 체크 (스터디원)
- **의도**: 스터디장 미접속 상황에서 셀프 출석을 허용합니다.
- **요청**
  - `POST /api/v1/studies/{studyId}/sessions/{sessionId}/attendance/self`
- **제한 조건**
  - 세션 시작 시각 + 15분 이후부터 가능
  - 스터디장이 이미 출석 상태(ABSENT 이외)라면 불가
- **응답**
  - `200 OK`
  - `ApiResponse<AttendanceResponse>`

---

## 3) 온라인 자동 출석

### 온라인 자동 출석 체크 (스터디원)
- **의도**: 온라인 미팅 자동 시작 후 10분 내 입장 시 자동 출석 체크에 사용합니다.
- **요청**
  - `POST /api/v1/studies/{studyId}/sessions/{sessionId}/attendance/online/auto`
- **제한 조건**
  - 해당 세션이 온라인(`StudySession.isOnline=true`)
  - 세션 시작 시각 ~ 10분 이내에만 가능
- **응답**
  - `200 OK`
  - `ApiResponse<AttendanceResponse>`

---

## 3-2) 온라인 미팅 개설 시간 제한 (±30분)

### 제한 규칙
- **의도**: 스터디 시작 시각 기준 **30분 전 ~ 30분 후** 사이에만 미팅 생성 가능
- **조건**: 스터디원 누구나 생성 가능 (스터디장만 제한하지 않음)

### 적용 위치 (백엔드)
- `MeetingController`의 `POST /api/v1/studies/{studyId}/meetings` 흐름에서  
  `MeetingService.startMeeting(...)` 내부에 시간 검증 로직을 추가하는 방식이 가장 안전합니다.

### 구현 가이드 (예시 로직)
1. `sessionId` 또는 `studyId`에 해당하는 스터디 세션을 조회합니다.
2. `session.scheduledAt`을 기준으로 아래 시간 창을 계산합니다.
   - `openAt = scheduledAt.minusMinutes(30)`
   - `closeAt = scheduledAt.plusMinutes(30)`
3. 현재 시간이 `openAt`~`closeAt` 바깥이면 미팅 생성 거절(예외 발생)합니다.

### 연동 팁
- 프론트에서 미팅 생성 버튼을 숨기기 전에 **서버 검증을 반드시 수행**하세요.
- 서버 검증이 우선이며, 프론트는 UX 보조로만 사용합니다.

---

## 4) 출석 상태 수동 변경 (스터디장)

### 출석 상태 변경
- **의도**: BLE 인식 오류 등으로 스터디장이 출석 상태를 직접 변경합니다.
- **요청**
  - `PATCH /api/v1/studies/{studyId}/sessions/{sessionId}/attendance/{targetUserId}`
  - Body:
    ```json
    {
      "status": "PRESENT",
      "reason": "BLE 인식 오류"
    }
    ```
- **응답**
  - `200 OK`
  - `ApiResponse<AttendanceResponse>`

---

## 5) 실시간 출석 현황 조회 (스터디장)

### 세션별 출석 현황
- **의도**: 스터디장이 현재 세션의 출석 현황을 조회합니다.
- **요청**
  - `GET /api/v1/studies/{studyId}/sessions/{sessionId}/attendance`
- **응답**
  - `200 OK`
  - `ApiResponse<List<AttendanceResponse>>`

---

## 6) 월별 출석 캘린더 조회 (스터디원)

### 월별 캘린더
- **의도**: 사용자가 월별 출석 기록을 캘린더 형태로 조회합니다.
- **요청**
  - `GET /api/v1/studies/{studyId}/attendance/calendar?year=2026&month=1`
- **응답**
  - `200 OK`
  - `ApiResponse<AttendanceCalendarResponse>`

---

## 7) 결석 소명

### 7-1. 소명 제출 (스터디원)
- **의도**: 결석 사유를 소명으로 제출합니다.
- **요청**
  - `POST /api/v1/studies/{studyId}/sessions/{sessionId}/attendance/excuse`
  - Body:
    ```json
    {
      "reason": "지각 버스 지연"
    }
    ```
- **응답**
  - `200 OK`
  - `ApiResponse<AttendanceResponse>`

### 7-2. 소명 승인/거절 (스터디장)
- **의도**: 스터디장이 소명을 승인 또는 거절합니다.
- **요청**
  - `PATCH /api/v1/studies/{studyId}/sessions/{sessionId}/attendance/{targetUserId}/excuse`
  - Body:
    ```json
    {
      "status": "APPROVED"
    }
    ```
- **응답**
  - `200 OK`
  - `ApiResponse<AttendanceResponse>`

---

## 8) 배치/스케줄러 동작 (자동 처리)

### 8-1. 출석 row 초기화 배치
- **동작 주기**: 1분 간격
- **조건**: 세션 시작 시각 기준 ±5분 범위 세션을 대상으로 출석 row 생성
- **관련 클래스**: `AttendanceScheduler.initializeAttendanceRows`

### 8-2. 출석 마감 배치
- **동작 주기**: 2분 간격
- **조건**: 세션 종료 + 10분 경과한 세션은 미체크 인원을 `ABSENT` 처리
- **관련 클래스**: `AttendanceScheduler.finalizeAttendanceRows`

---

## 9) 동작 흐름 예시

### 오프라인 스터디 (BLE)
1. 스터디장: BLE 출석 시작
2. 스터디원: BLE 감지 시 BLE 출석 체크 호출
3. 스터디장: 실시간 출석 현황 조회로 확인
4. 오류 시: 수동 변경 API 호출

### 온라인 스터디
1. 세션 시작 시각 기준 10분 내 입장 시 자동 출석 체크 API 호출
2. 출석 캘린더/실시간 조회로 확인

### 셀프 출석
1. 세션 시작 + 15분 이후에도 스터디장 미출석일 때
2. 스터디원이 셀프 출석 API 호출

---

## 참고: 주요 파일 위치
- 컨트롤러: `backend/src/main/java/com/ssafy/domain/attendance/controller/AttendanceController.java`
- 서비스: `backend/src/main/java/com/ssafy/domain/attendance/service/AttendanceService.java`
- 스케줄러: `backend/src/main/java/com/ssafy/domain/attendance/scheduler/AttendanceScheduler.java`
- 엔티티/ENUM: `backend/src/main/java/com/ssafy/domain/attendance/entity/*`
- 레포지토리: `backend/src/main/java/com/ssafy/domain/attendance/repository/AttendanceRepository.java`

