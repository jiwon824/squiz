# RTC Mockup 개념 및 AWS 배포 가이드

이 문서는 현재 구현에 사용된 핵심 개념과 AWS 배포를 위한 백엔드/인프라 연동 사항을 정리한다.

## 1. 현재 구현 개념 정리

### 1) WebRTC 기본
- 미디어 캡처: `getUserMedia`(캠/마이크), `getDisplayMedia`(화면 공유)
- 전송 보안: DTLS
- NAT 통과: ICE 후보 + STUN/TURN
- 트랙 상태: `readyState`, `mute/unmute`, `ended` 이벤트

### 2) SFU (Selective Forwarding Unit)
- SFU는 송신자 1개의 Producer 스트림을 여러 수신자 Consumer로 전달
- mediasoup 기반: Transport 생성 → Producer 생성 → Consumer 생성/Resume
- keyframe 요청이 없으면 수신자가 검은 화면을 볼 수 있음
- `announcedIp`가 잘못되면 ICE가 붙어도 영상이 흐르지 않음

### 3) 시그널링 (STOMP)
- STOMP(WebSocket)으로 룸 이벤트/채팅/발표자 변경 전달
- 발표자 권한은 서버가 단일 소유자로 관리하고 브로드캐스트
- 채팅 히스토리는 서버 메모리에 저장 후 재입장자에게 전달

### 4) Canvas 합성
- 화면 공유 + 캠 동시 켜짐 → 캔버스에서 합성 후 단일 스트림 송출
- 캔버스는 DOM에 붙여야 `captureStream()` 안정적으로 동작

### 5) 얼굴 인식/자리 상태
- TensorFlow coco-ssd로 사람(person) 감지
- 감지 결과로 참가자 목록 “자리에 있음/없음” 표시

### 6) 발화 감지
- Web Audio Analyser로 음성 레벨 감지
- 참가자 목록에서 발화 중 효과 표시

---

## 2. 현재 코드 구조 요약

### 프론트엔드 (React)
- 프로젝트: `video-conference-client`
- STOMP: `src/services/stompClient.js`
- SFU: `src/services/sfuClient.js`
- Canvas 합성: `src/services/canvasComposer.js`
- 얼굴 인식: `src/services/aiDetection.js`

### 백엔드 (Spring Boot)
- 프로젝트: `video-conference-server`
- STOMP 엔드포인트
  - `/app/rooms/{roomId}/join`
  - `/app/rooms/{roomId}/chat`
  - `/app/rooms/{roomId}/presenter`
- 이벤트 토픽
  - `/topic/rooms/{roomId}/events`
- 채팅 히스토리
  - `/user/queue/rooms/{roomId}/history`

### SFU (Node + mediasoup)
- 프로젝트: `sfu-server`
- Socket.IO 기반 media transport 생성
- Producer/Consumer 생성 및 keyframe 요청 처리

---

## 3. AWS 배포 구체 가이드

### 3.1 전체 아키텍처

1) **프론트엔드**
   - S3 정적 호스팅 + CloudFront
2) **백엔드(API/WS)**
   - ECS Fargate 또는 EC2
   - ALB 앞단에 배치 (HTTPS/WSS)
3) **SFU (mediasoup)**
   - EC2 권장 (UDP 포트 제어 필요)
   - Elastic IP 부여
4) **TURN 서버**
   - coturn을 별도 EC2에 구축
   - TLS 인증서 적용

---

### 3.2 네트워크 및 보안 그룹

#### SFU 서버
- TCP 4000 (Socket.IO signaling)
- UDP 20000-20100 (mediasoup RTP)
- 보안 그룹 예시
  - 인바운드 TCP: 4000 (0.0.0.0/0)
  - 인바운드 UDP: 20000-20100 (0.0.0.0/0)

#### TURN 서버
- TCP/UDP 3478 (STUN/TURN)
- TCP 5349 (TURN TLS)

#### 백엔드(Spring Boot)
- TCP 8080 (내부)
- 외부는 ALB 443(HTTPS/WSS)

---

### 3.3 환경변수 및 설정

#### 프론트엔드
- `REACT_APP_API_BASE_URL=https://api.yourdomain.com`
- `REACT_APP_SFU_BASE_URL=https://sfu.yourdomain.com` (필요 시 override)

#### Spring Boot (`application.yml`)
- `app.cors.allowed-origins`: 실제 프론트 도메인
- `app.sfu.base-url`: SFU 도메인

#### SFU 서버 (`sfu-server/src/config.js`)
- `ANNOUNCED_IP`: **SFU Elastic IP**
- `RTC_MIN_PORT`, `RTC_MAX_PORT`: UDP 범위

---

### 3.4 TURN 서버 구축

1) coturn 설치
2) `/etc/turnserver.conf` 설정
   - `listening-port=3478`
   - `tls-listening-port=5349`
   - `realm=yourdomain.com`
   - `user=turnuser:turnpassword`
3) SSL 인증서 적용 (ACM 불가, 직접 파일 필요)
4) 클라이언트에서 STUN/TURN 서버 리스트 설정 필요

---

### 3.5 도메인 & TLS

- **HTTPS/WSS 필수** (브라우저 보안 정책)
- ACM으로 인증서 발급
- CloudFront, ALB, NLB에 인증서 연결

---

## 4. 문제 발생 시 체크리스트

### 검은 화면 문제
1) SFU `ANNOUNCED_IP` 설정 확인
2) UDP 포트 열림 확인
3) Consumer resume / keyframe 요청 확인
4) Producer stats 증가 여부 확인

### 발표자 권한 문제
- 서버 로그에서 `[PRESENTER]` 이벤트 확인
- 클라이언트 콘솔에서 `[ROOM EVENT] PRESENTER` 확인

---

## 5. 추가 개선 포인트

- Redis로 채팅 히스토리 영속화
- 발표자 변경 기록 저장
- TURN 인증을 동적 발급 API로 교체
- SFU 확장 (룸별 분산, Redis 기반 상태 공유)

---

## 6. 배포 절차 요약

1) Frontend 빌드 → S3 업로드 → CloudFront 연결
2) Backend Spring Boot → ECS/EC2 배포 → ALB 연결
3) SFU Node → EC2 배포 → Elastic IP 할당 → `ANNOUNCED_IP` 설정
4) TURN 서버 구성
5) 전체 도메인 HTTPS/WSS 확인

---

## 7. 참고 실행 명령

### Spring Boot (Eclipse 실행)
- Run As → Spring Boot App

### SFU 서버
```
cd C:\SSAFY\rtc_mockup\sfu-server
npm start
```

### React 클라이언트
```
cd C:\SSAFY\rtc_mockup\video-conference-client
npm start
```

• 맞춤화하려면 아래 정보를 알려줘.

  1. 배포 도메인 구조

  - 예: app.example.com, api.example.com, sfu.example.com,
    turn.example.com

  2. 배포 환경

  - EC2 / ECS Fargate / EKS 중 어떤 조합인지

  3. SFU 배치 방식

  - EC2 단독인지, NLB 뒤인지
  - UDP 포트 범위

  4. TURN 서버 유무

  - 사용/미사용
  - 있으면 도메인/포트

  5. 인증서/HTTPS 구성

  - ACM 사용 여부
  - CloudFront/ALB 연결 방식

  이 값 주면 문서에 그대로 반영해서 구체화