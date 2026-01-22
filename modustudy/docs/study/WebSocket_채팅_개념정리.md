# WebSocket 채팅 시스템 개념 정리

## 목차
1. [실시간 통신 방식 비교](#1-실시간-통신-방식-비교)
2. [WebSocket이란?](#2-websocket이란)
3. [왜 채팅에 WebSocket을 쓰는가?](#3-왜-채팅에-websocket을-쓰는가)
4. [WebSocket 동작 원리](#4-websocket-동작-원리)
5. [STOMP 프로토콜](#5-stomp-프로토콜)
6. [Spring WebSocket 구현](#6-spring-websocket-구현)
7. [채팅 시나리오별 아키텍처](#7-채팅-시나리오별-아키텍처)
8. [스케일 아웃과 Redis Pub/Sub](#8-스케일-아웃과-redis-pubsub)

---

## 1. 실시간 통신 방식 비교

### 1.1 HTTP Polling

```
클라이언트                          서버
    |                                |
    |------- GET /messages --------->|
    |<------ 응답 (메시지 없음) ------|
    |                                |
    |  (1초 대기)                     |
    |                                |
    |------- GET /messages --------->|
    |<------ 응답 (메시지 없음) ------|
    |                                |
    |  (1초 대기)                     |
    |                                |
    |------- GET /messages --------->|
    |<------ 응답 (새 메시지!) -------|
```

**특징:**
- 클라이언트가 주기적으로 서버에 요청
- 구현이 단순함
- **단점:** 불필요한 요청이 많음, 서버 부하 증가, 실시간성 부족

**부하 계산 (1만 명, 1초 간격):**
```
10,000명 × 60회/분 = 600,000 요청/분
= 10,000 요청/초
```

---

### 1.2 Long Polling

```
클라이언트                          서버
    |                                |
    |------- GET /messages --------->|
    |         (연결 유지, 대기...)    |
    |                    (메시지 발생)|
    |<------ 응답 (새 메시지!) -------|
    |                                |
    |------- GET /messages --------->|  <- 즉시 재연결
    |         (연결 유지, 대기...)    |
```

**특징:**
- 서버가 새 데이터가 있을 때까지 응답을 지연
- Polling보다 효율적
- **단점:** 연결이 자주 끊어졌다 다시 맺어짐, 여전히 HTTP 오버헤드 존재

---

### 1.3 Server-Sent Events (SSE)

```
클라이언트                          서버
    |                                |
    |------- GET /events ----------->|
    |<------ HTTP 200 (연결 유지) ---|
    |                                |
    |<------ data: 메시지1 ----------|
    |                                |
    |<------ data: 메시지2 ----------|
    |                                |
    |<------ data: 메시지3 ----------|
```

**특징:**
- 서버 → 클라이언트 단방향 스트리밍
- HTTP 기반으로 방화벽 친화적
- **단점:** 단방향만 지원 (클라이언트 → 서버는 별도 HTTP 요청 필요)

**적합한 케이스:** 알림, 실시간 피드, 주식 시세

---

### 1.4 WebSocket

```
클라이언트                          서버
    |                                |
    |------ HTTP Upgrade 요청 ------>|
    |<----- 101 Switching Protocols -|
    |                                |
    |========= WebSocket 연결 =======|
    |                                |
    |<-------- 메시지1 --------------|
    |--------- 메시지2 ------------->|
    |<-------- 메시지3 --------------|
    |--------- 메시지4 ------------->|
```

**특징:**
- 양방향(Full-Duplex) 통신
- 한 번 연결 후 지속적으로 유지
- 매우 낮은 오버헤드 (프레임 헤더 2~14바이트)
- **최적의 실시간 통신 방식**

---

### 1.5 방식별 비교표

| 구분 | Polling | Long Polling | SSE | WebSocket |
|------|---------|--------------|-----|-----------|
| 연결 방향 | 단방향 | 단방향 | 단방향 | **양방향** |
| 실시간성 | 낮음 | 중간 | 높음 | **최고** |
| 서버 부하 | 높음 | 중간 | 낮음 | **최저** |
| 오버헤드 | 매우 높음 | 높음 | 낮음 | **최저** |
| 구현 복잡도 | 낮음 | 중간 | 낮음 | 중간 |
| 채팅 적합도 | ❌ | △ | △ | **✅** |

---

## 2. WebSocket이란?

### 2.1 정의

WebSocket은 **단일 TCP 연결을 통해 전이중(Full-Duplex) 양방향 통신**을 제공하는 프로토콜이다.

- **RFC 6455** 표준
- URL 스킴: `ws://` (비암호화), `wss://` (TLS 암호화)
- HTTP와 같은 포트 사용 (80, 443)

### 2.2 HTTP vs WebSocket

```
┌─────────────────────────────────────────────────────────────┐
│                         HTTP                                │
├─────────────────────────────────────────────────────────────┤
│  요청 → 응답 → 연결 종료 → 요청 → 응답 → 연결 종료 ...     │
│                                                             │
│  매 요청마다:                                                │
│  - TCP 3-way handshake                                      │
│  - HTTP 헤더 전송 (~800바이트)                               │
│  - 응답 후 연결 종료                                         │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                       WebSocket                             │
├─────────────────────────────────────────────────────────────┤
│  핸드셰이크(1회) → 연결 유지 → 메시지 ↔ 메시지 ↔ 메시지 ... │
│                                                             │
│  연결 후:                                                    │
│  - TCP 연결 유지                                             │
│  - 프레임 헤더만 전송 (2~14바이트)                           │
│  - 언제든 양방향 전송 가능                                   │
└─────────────────────────────────────────────────────────────┘
```

### 2.3 프레임 구조

WebSocket 메시지는 **프레임(Frame)** 단위로 전송된다.

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-------+-+-------------+-------------------------------+
|F|R|R|R| opcode|M| Payload len |    Extended payload length    |
|I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
|N|V|V|V|       |S|             |   (if payload len==126/127)   |
| |1|2|3|       |K|             |                               |
+-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
|     Extended payload length continued, if payload len == 127  |
+ - - - - - - - - - - - - - - - +-------------------------------+
|                               |Masking-key, if MASK set to 1  |
+-------------------------------+-------------------------------+
| Masking-key (continued)       |          Payload Data         |
+-------------------------------- - - - - - - - - - - - - - - - +
:                     Payload Data continued ...                :
+ - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
|                     Payload Data continued ...                |
+---------------------------------------------------------------+
```

**최소 프레임 크기: 2바이트** (HTTP 헤더 ~800바이트와 비교)

---

## 3. 왜 채팅에 WebSocket을 쓰는가?

### 3.1 효율성 비교

**1만 명이 1분간 채팅하는 상황 (평균 10개 메시지/분/명)**

| 방식 | 요청 수 | 데이터 전송량 | 지연 시간 |
|------|---------|--------------|----------|
| Polling (1초) | 600,000/분 | ~480MB | 최대 1초 |
| WebSocket | 100,000/분 | ~10MB | **즉시** |

**WebSocket이 48배 효율적!**

### 3.2 채팅에 WebSocket이 적합한 이유

```
1. 실시간성
   - 메시지 발생 즉시 전달
   - 지연 시간 < 50ms

2. 양방향 통신
   - 보내기/받기가 동시에 가능
   - "상대방이 입력 중..." 같은 기능 구현 용이

3. 낮은 오버헤드
   - 연결당 메모리: ~10-50KB
   - 메시지당 추가 데이터: 2-14바이트

4. 연결 상태 관리
   - 접속/퇴장 감지 가능
   - 온라인 상태 표시

5. 서버 푸시
   - 서버가 능동적으로 메시지 전송
   - 새 메시지 알림, 읽음 처리 등
```

### 3.3 서버 리소스 비교

**서버 스펙: 16GB RAM, 4 vCPUs**

```
┌────────────────────────────────────────────────────────────┐
│                    HTTP Polling                            │
├────────────────────────────────────────────────────────────┤
│  동시 1만 명 × 1회/초 = 10,000 req/sec                     │
│                                                            │
│  - 매 요청마다 스레드 할당                                  │
│  - HTTP 파싱 오버헤드                                       │
│  - DB 조회 부하                                             │
│                                                            │
│  → CPU 병목, 수천 명이 한계                                 │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│                      WebSocket                             │
├────────────────────────────────────────────────────────────┤
│  동시 1만 연결 유지                                         │
│                                                            │
│  - 연결당 ~50KB = 500MB                                    │
│  - 메시지 있을 때만 처리                                    │
│  - 이벤트 기반 비동기 처리                                  │
│                                                            │
│  → 수만 명 동시 접속 가능                                   │
└────────────────────────────────────────────────────────────┘
```

---

## 4. WebSocket 동작 원리

### 4.1 핸드셰이크 (Handshake)

WebSocket 연결은 HTTP Upgrade 요청으로 시작된다.

**클라이언트 요청:**
```http
GET /chat HTTP/1.1
Host: server.example.com
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
Sec-WebSocket-Version: 13
```

**서버 응답:**
```http
HTTP/1.1 101 Switching Protocols
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
```

### 4.2 연결 수명주기

```
┌──────────────┐                              ┌──────────────┐
│    Client    │                              │    Server    │
└──────┬───────┘                              └──────┬───────┘
       │                                             │
       │  1. HTTP Upgrade Request                    │
       │────────────────────────────────────────────>│
       │                                             │
       │  2. 101 Switching Protocols                 │
       │<────────────────────────────────────────────│
       │                                             │
       │  =========== WebSocket 연결 ===========    │
       │                                             │
       │  3. 메시지 교환 (양방향)                     │
       │<═══════════════════════════════════════════>│
       │                                             │
       │  4. Ping/Pong (연결 유지 확인)               │
       │<────────────────────────────────────────────│
       │────────────────────────────────────────────>│
       │                                             │
       │  5. Close Frame (연결 종료)                  │
       │────────────────────────────────────────────>│
       │<────────────────────────────────────────────│
       │                                             │
```

### 4.3 메시지 타입

| Opcode | 타입 | 설명 |
|--------|------|------|
| 0x1 | Text | UTF-8 텍스트 메시지 |
| 0x2 | Binary | 바이너리 데이터 |
| 0x8 | Close | 연결 종료 |
| 0x9 | Ping | 연결 확인 요청 |
| 0xA | Pong | Ping에 대한 응답 |

---

## 5. STOMP 프로토콜

### 5.1 STOMP란?

**STOMP (Simple Text Oriented Messaging Protocol)**
- WebSocket 위에서 동작하는 메시징 프로토콜
- 메시지 형식과 명령어를 정의
- Pub/Sub 패턴 지원

### 5.2 왜 STOMP를 사용하는가?

**순수 WebSocket의 한계:**
```javascript
// 순수 WebSocket - 메시지 구조가 없음
ws.send("안녕하세요");  // 누구에게? 어떤 채널?
ws.send(JSON.stringify({type: "chat", room: "123", msg: "안녕"}));
// → 직접 파싱/라우팅 로직 구현 필요
```

**STOMP 사용 시:**
```javascript
// STOMP - 명확한 구조와 라우팅
stompClient.subscribe('/topic/chat/room/123', callback);
stompClient.send('/app/chat/room/123', {}, '안녕하세요');
// → 프레임워크가 라우팅 처리
```

### 5.3 STOMP 프레임 구조

```
COMMAND
header1:value1
header2:value2

Body^@
```

**예시 - SEND 프레임:**
```
SEND
destination:/app/chat/room/123
content-type:application/json

{"sender":"user1","content":"안녕하세요"}^@
```

### 5.4 주요 명령어

| 명령어 | 방향 | 설명 |
|--------|------|------|
| CONNECT | C→S | 연결 요청 |
| CONNECTED | S→C | 연결 성공 |
| SUBSCRIBE | C→S | 채널 구독 |
| UNSUBSCRIBE | C→S | 구독 해제 |
| SEND | C→S | 메시지 전송 |
| MESSAGE | S→C | 메시지 수신 |
| DISCONNECT | C→S | 연결 종료 |

### 5.5 Pub/Sub 패턴

```
┌─────────────┐     SEND /app/chat      ┌─────────────┐
│   User A    │ ───────────────────────>│             │
└─────────────┘                         │             │
                                        │   Server    │
┌─────────────┐  SUBSCRIBE /topic/chat  │   (Broker)  │
│   User B    │ <───────────────────────│             │
└─────────────┘                         │             │
                                        │             │
┌─────────────┐  SUBSCRIBE /topic/chat  │             │
│   User C    │ <───────────────────────│             │
└─────────────┘                         └─────────────┘

1. User A가 /app/chat으로 메시지 전송
2. 서버가 처리 후 /topic/chat으로 브로드캐스트
3. /topic/chat 구독자(B, C)에게 메시지 전달
```

---

## 6. Spring WebSocket 구현

### 6.1 의존성

```gradle
// build.gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-websocket'
}
```

### 6.2 WebSocket 설정

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 구독 경로 prefix (서버 → 클라이언트)
        config.enableSimpleBroker("/topic", "/queue");

        // 메시지 전송 경로 prefix (클라이언트 → 서버)
        config.setApplicationDestinationPrefixes("/app");

        // 1:1 메시지용 prefix
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .withSockJS();  // SockJS 폴백 지원
    }
}
```

### 6.3 메시지 컨트롤러

```java
@Controller
public class ChatController {

    @MessageMapping("/chat/room/{roomId}")
    @SendTo("/topic/chat/room/{roomId}")
    public ChatMessage sendMessage(
            @DestinationVariable String roomId,
            ChatMessage message) {

        message.setTimestamp(LocalDateTime.now());
        return message;  // 같은 방 구독자 전체에게 전송
    }

    @MessageMapping("/chat/private")
    @SendToUser("/queue/private")
    public ChatMessage sendPrivateMessage(
            ChatMessage message,
            Principal principal) {

        // 특정 사용자에게만 전송
        return message;
    }
}
```

### 6.4 클라이언트 (JavaScript)

```javascript
// STOMP 클라이언트 연결
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);

    // 채팅방 구독
    stompClient.subscribe('/topic/chat/room/123', function(message) {
        const chatMessage = JSON.parse(message.body);
        displayMessage(chatMessage);
    });
});

// 메시지 전송
function sendMessage(content) {
    stompClient.send('/app/chat/room/123', {}, JSON.stringify({
        sender: username,
        content: content
    }));
}
```

### 6.5 인터셉터 (인증 처리)

```java
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
            StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            // JWT 검증 로직
            UserDetails user = validateToken(token);
            accessor.setUser(new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities()));
        }

        return message;
    }
}
```

---

## 7. 채팅 시나리오별 아키텍처

### 7.1 ModuStudy 채팅 시나리오

| 채팅 유형 | 특성 | 저장 | 권장 방식 |
|-----------|------|------|----------|
| 1:1 DM | 영구 보관, 오프라인 전송 | MySQL | WebSocket + DB |
| 라이브 퀴즈 채팅 | 대규모, 일시적 | Redis | WebSocket + Redis |
| 스터디 워크스페이스 | 영구 보관, 검색 필요 | MySQL | WebSocket + DB |
| 온라인 스터디방 | 세션 기반 | Redis | WebSocket + Redis |

### 7.2 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────────────┐
│                        클라이언트                                │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐            │
│  │ Browser │  │ Browser │  │ Browser │  │ Mobile  │            │
│  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘            │
└───────┼────────────┼────────────┼────────────┼──────────────────┘
        │            │            │            │
        └────────────┴─────┬──────┴────────────┘
                           │ WebSocket (wss://)
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Nginx                                   │
│                    (Reverse Proxy)                              │
│              WebSocket Upgrade 처리                              │
└─────────────────────────────┬───────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Boot Backend                          │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                 WebSocket Handler                         │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐         │  │
│  │  │ DM Handler │  │Quiz Handler│  │Study Handler│        │  │
│  │  └─────┬──────┘  └─────┬──────┘  └─────┬──────┘         │  │
│  └────────┼───────────────┼───────────────┼─────────────────┘  │
│           │               │               │                     │
│           ▼               ▼               ▼                     │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              Message Broker (내장/Redis)                │    │
│  │         /topic/dm/*  /topic/quiz/*  /topic/study/*     │    │
│  └────────────────────────────────────────────────────────┘    │
└───────────────────────┬─────────────────────┬───────────────────┘
                        │                     │
                        ▼                     ▼
              ┌──────────────────┐  ┌──────────────────┐
              │      MySQL       │  │      Redis       │
              │  (영구 메시지)    │  │  (캐시/Pub/Sub)  │
              └──────────────────┘  └──────────────────┘
```

### 7.3 메시지 흐름

**1:1 DM 흐름:**
```
1. User A → WebSocket → /app/dm/send
2. Server: 메시지 DB 저장
3. Server: /topic/dm/{userB} 로 전송
4. User B가 오프라인이면 → 푸시 알림
5. User B 접속 시 → DB에서 안읽은 메시지 로드
```

**라이브 퀴즈 채팅 흐름:**
```
1. User → WebSocket → /app/quiz/{quizId}/chat
2. Server: Redis Pub/Sub으로 브로드캐스트
3. 모든 참가자에게 즉시 전달
4. DB 저장 없음 (또는 비동기 로그만)
```

---

## 8. 스케일 아웃과 Redis Pub/Sub

### 8.1 단일 서버의 한계

```
┌─────────────┐         ┌─────────────┐
│   User A    │────────>│   Server    │<────────│   User B    │
└─────────────┘         └─────────────┘         └─────────────┘

User A와 B가 같은 서버에 연결 → 메시지 전달 OK
```

**문제: 서버가 여러 대일 때**

```
┌─────────────┐         ┌─────────────┐
│   User A    │────────>│  Server 1   │
└─────────────┘         └─────────────┘
                              ✗ 메시지 전달 불가
┌─────────────┐         ┌─────────────┐
│   User B    │────────>│  Server 2   │
└─────────────┘         └─────────────┘
```

### 8.2 Redis Pub/Sub 해결책

```
┌─────────────┐         ┌─────────────┐
│   User A    │────────>│  Server 1   │─────┐
└─────────────┘         └─────────────┘     │ PUBLISH
                                            ▼
                                    ┌─────────────┐
                                    │    Redis    │
                                    │   Pub/Sub   │
                                    └─────────────┘
                                            │ SUBSCRIBE
┌─────────────┐         ┌─────────────┐     │
│   User B    │<────────│  Server 2   │<────┘
└─────────────┘         └─────────────┘
```

### 8.3 Spring에서 Redis Pub/Sub 설정

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Redis를 외부 브로커로 사용
        config.enableStompBrokerRelay("/topic", "/queue")
              .setRelayHost("redis-host")
              .setRelayPort(6379);

        config.setApplicationDestinationPrefixes("/app");
    }
}
```

### 8.4 메시지 흐름 (스케일 아웃)

```
1. User A가 Server 1에 메시지 전송
2. Server 1이 Redis에 PUBLISH
3. Redis가 모든 구독 서버에 메시지 전달
4. Server 2가 메시지 수신
5. Server 2에 연결된 User B에게 전달
```

---

## 요약

### WebSocket을 채팅에 쓰는 이유

1. **실시간성**: 메시지 즉시 전달 (< 50ms)
2. **효율성**: HTTP 대비 48배 이상 효율적
3. **양방향**: 보내기/받기 동시 가능
4. **낮은 오버헤드**: 프레임 헤더 2~14바이트
5. **연결 상태 관리**: 온라인/오프라인 감지

### 기술 스택 권장

```
클라이언트: SockJS + STOMP
서버: Spring WebSocket + STOMP
브로커: 내장 SimpleBroker (소규모) / Redis Pub/Sub (대규모)
저장소: MySQL (영구) + Redis (캐시)
```

### 참고 자료

- [RFC 6455 - The WebSocket Protocol](https://tools.ietf.org/html/rfc6455)
- [STOMP Protocol Specification](https://stomp.github.io/stomp-specification-1.2.html)
- [Spring WebSocket Documentation](https://docs.spring.io/spring-framework/reference/web/websocket.html)
