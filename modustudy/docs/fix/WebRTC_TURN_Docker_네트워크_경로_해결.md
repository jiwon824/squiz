# WebRTC TURN-SFU Docker 네트워크 경로 해결

> 2026-01-28 | WebRTC 화면공유가 SSAFY 운영 환경에서 동작하지 않는 문제

---

## 1. 증상

- 화면공유 시작 시 브라우저 콘솔에 `recv transport state: connecting`만 출력되고 `connected`로 넘어가지 않음
- `cannot consume`, `Channel request handler not found [method:consumer.requestKeyFrame]` 에러 발생
- coturn(TURN 서버) 로그에 연결 시도가 0건

---

## 2. 배경 지식: WebRTC 연결 구조

### 일반적인 WebRTC 연결 흐름

```
[브라우저 A] ←──── ICE (직접 연결) ────→ [SFU 서버(mediasoup)]
```

브라우저와 SFU 서버는 ICE(Interactive Connectivity Establishment) 프로토콜로 연결합니다.
ICE는 다음 순서로 연결을 시도합니다:

1. **host candidate** - 직접 연결 (같은 네트워크)
2. **srflx candidate** - STUN 서버로 공인 IP 확인 후 연결
3. **relay candidate** - TURN 서버를 경유한 중계 연결

### SFU 아키텍처에서의 데이터 흐름

```
[브라우저] ──produce──→ [SFU(mediasoup)] ──consume──→ [다른 브라우저]
              │                │                          │
         sendTransport    Router(라우팅)           recvTransport
```

- **produce**: 내 화면/음성 트랙을 SFU에 전송
- **consume**: 다른 사람의 트랙을 SFU로부터 수신
- 모든 미디어는 SFU를 거쳐 중계됨 (Mesh 방식과 다름)

---

## 3. SSAFY 인프라 제약

SSAFY 운영 서버(EC2)의 네트워크 제약:

| 포트 | 프로토콜 | 상태 |
|------|----------|------|
| TCP 80 | HTTP | 허용 |
| TCP 443 | HTTPS | 허용 |
| TCP/UDP 20000-21000 | WebRTC 미디어 | **차단** |
| UDP 전체 | - | **차단** |

mediasoup SFU는 WebRTC transport에 20000-21000번 포트를 사용하는데,
SSAFY에서 이 포트들이 전부 차단되어 있어 직접 연결이 불가능합니다.

---

## 4. 해결 전략: TURN TCP over port 443

직접 연결이 불가능하므로 TURN 서버를 경유하는 relay 방식을 사용합니다.
단, UDP도 차단이므로 **TURN over TCP on port 443**을 사용합니다.

### 전체 아키텍처

```
                        ┌─── Nginx stream (port 443) ───┐
                        │                                │
[브라우저] ──TCP 443──→ │  TLS 트래픽 → :8443 (HTTPS)   │
                        │  non-TLS    → coturn:3478      │
                        │  (ssl_preread로 판별)          │
                        └────────────────────────────────┘
```

**Nginx stream 블록**이 TCP 443에서 트래픽을 분류합니다:
- TLS ClientHello로 시작 → HTTPS 서버 (포트 8443)로 전달
- TLS가 아닌 트래픽 → coturn TURN 서버 (포트 3478)로 전달

```nginx
stream {
    map $ssl_preread_protocol $upstream {
        ""      turn_backend;    # non-TLS → coturn
        default https_backend;   # TLS → HTTPS
    }
    upstream turn_backend {
        server squiz-coturn:3478;
    }
    upstream https_backend {
        server 127.0.0.1:8443;
    }
    server {
        listen 443;
        ssl_preread on;
        proxy_pass $upstream;
    }
}
```

---

## 5. 문제의 핵심: Docker 네트워크 경로

### 문제 상황 (수정 전)

```
                    Docker squiz-network (bridge)
                   ┌──────────────────────────────────┐
                   │  squiz-sfu (172.18.0.x)          │
[브라우저]         │    announcedIp: REMOVED_DB_HOST     │
    │              │    transport port: 20000          │
    │              │                                    │
    │              │  squiz-coturn (172.18.0.y)         │
    │              │                                    │
    │              │  squiz-nginx-proxy (172.18.0.z)    │
    │              └──────────────────────────────────┘
    │                        │
    └── TURN TCP 443 ────→ Nginx → coturn
                               │
                               └─→ relay to REMOVED_DB_HOST:20000
                                          │
                                          ╳ SSAFY 방화벽 차단!
```

**경로 분석:**

1. 브라우저가 `turn:REMOVED_DB_HOST:443?transport=tcp`로 TURN 연결
2. TCP 443 → Nginx stream → coturn:3478 (Docker 내부) ✅
3. TURN relay 할당 성공 ✅
4. SFU의 ICE candidate: `REMOVED_DB_HOST:20000` (announcedIp가 외부 IP)
5. coturn이 relay 데이터를 `REMOVED_DB_HOST:20000`으로 전달 시도
6. Docker 컨테이너에서 `REMOVED_DB_HOST`는 **외부 IP** → 패킷이 Docker 밖으로 나감
7. EC2의 실제 IP는 `172.26.15.97` (private), `REMOVED_DB_HOST`는 AWS NAT
8. 외부에서 20000번 포트로 돌아오려 하지만 **SSAFY 방화벽이 차단** ❌

### 추가 문제: coturn의 네트워크 모드

초기에 coturn은 `--network host` 모드로 실행되었습니다.
Nginx proxy는 `squiz-network` (bridge 모드)에 있어서,
`host.docker.internal:3478`로 coturn에 접근하려 했지만 iptables에 의해 차단되었습니다.

```
squiz-nginx-proxy (bridge) ──→ host.docker.internal:3478 ──→ ╳ iptables 차단
```

---

### 해결 (수정 후)

```
                    Docker squiz-network (bridge)
                   ┌──────────────────────────────────┐
                   │  squiz-sfu (172.18.0.x)          │
[브라우저]         │    announcedIp: 172.18.0.x ← 변경│
    │              │    transport port: 20000          │
    │              │         ▲                         │
    │              │         │ Docker 내부 직접 전달   │
    │              │         │                         │
    │              │  squiz-coturn (172.18.0.y)         │
    │              │         ▲                         │
    │              │         │                         │
    │              │  squiz-nginx-proxy (172.18.0.z)    │
    │              └──────────────────────────────────┘
    │                        ▲
    └── TURN TCP 443 ────────┘
```

**수정된 경로:**

1. 브라우저 → `turn:REMOVED_DB_HOST:443?transport=tcp` → Nginx stream → coturn ✅
2. TURN relay 할당 ✅
3. SFU의 ICE candidate: `172.18.0.x:20000` (**Docker 내부 IP**)
4. 브라우저가 직접 `172.18.0.x`에 연결 시도 → 실패 (사설 IP, 도달 불가)
5. ICE fallback → TURN relay 사용
6. coturn이 relay 데이터를 `172.18.0.x:20000`으로 전달
7. **같은 Docker 네트워크** → 직접 전달 성공 ✅

---

## 6. 수정 내용

### 6-1. SFU announcedIp 자동 감지 (`sfu-server/src/config.js`)

```javascript
const os = require('os');

function getContainerIp() {
  const interfaces = os.networkInterfaces();
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) {
        return iface.address; // Docker 내부 IP (172.18.0.x)
      }
    }
  }
  return '127.0.0.1';
}

const config = {
  announcedIp: process.env.SFU_ANNOUNCED_IP || getContainerIp(),
  // ...
};
```

`SFU_ANNOUNCED_IP` 환경변수를 설정하지 않으면, 컨테이너의 네트워크 인터페이스에서
IPv4 주소를 자동으로 감지합니다. Docker bridge 네트워크에서는 `172.18.0.x` 형태의 IP가 반환됩니다.

### 6-2. docker-compose.app.yml

```yaml
sfu:
  environment:
    # SFU_ANNOUNCED_IP 미설정 → 컨테이너 내부 IP 자동 감지
    - LISTEN_IP=0.0.0.0
  expose:
    - "4000"
    - "20000-21000"    # 호스트 포트 매핑 제거, Docker 내부 expose만
```

- `SFU_ANNOUNCED_IP=REMOVED_DB_HOST` 제거
- `ports: 20000-21000` (호스트 매핑) → `expose: 20000-21000` (Docker 내부만)

### 6-3. coturn을 Docker bridge 네트워크로 이동

```yaml
# docker-compose.yml
coturn:
  image: coturn/coturn:latest
  container_name: squiz-coturn
  volumes:
    - ./turnserver.conf:/etc/turnserver.conf:ro
  command: ["-c", "/etc/turnserver.conf"]
  networks:
    - squiz-network    # SFU와 같은 네트워크
```

기존: `docker run --network host` → Nginx에서 접근 불가
변경: `squiz-network` (bridge) → Nginx, SFU와 같은 네트워크에서 직접 통신

### 6-4. Nginx stream upstream 변경

```nginx
upstream turn_backend {
    server squiz-coturn:3478;    # 기존: host.docker.internal:3478
}
```

### 6-5. SFU resume 핸들러 안정화

```javascript
// requestKeyFrame 실패가 consume 전체를 중단시키지 않도록
if (consumer.kind === 'video') {
  try {
    await consumer.requestKeyFrame();
  } catch (e) {
    // transport 연결 중일 때 실패 가능 - 무시
  }
}
```

---

## 7. 핵심 교훈

### Docker 네트워크와 외부 IP의 함정

Docker bridge 네트워크 안에서 **서버의 외부 IP**로 트래픽을 보내면,
패킷이 Docker 네트워크를 벗어나 외부로 나갑니다.
EC2에서는 외부 IP(REMOVED_DB_HOST)가 AWS NAT이므로, 패킷이 인터넷으로 나갔다가
다시 돌아와야 하는데, 방화벽에서 차단될 수 있습니다.

**같은 Docker 네트워크의 컨테이너끼리는 내부 IP로 직접 통신하는 것이 올바른 방법입니다.**

### TURN 서버의 네트워크 위치

TURN 서버는 relay 대상(SFU)에 직접 도달할 수 있어야 합니다.
TURN과 SFU가 같은 Docker 네트워크에 있으면, 내부 IP로 직접 전달 가능합니다.
네트워크가 다르면 (host vs bridge), Docker의 iptables 규칙에 따라 차단될 수 있습니다.

### ICE fallback 메커니즘 활용

SFU의 announcedIp를 Docker 내부 IP로 설정하면:
- 브라우저 → 내부 IP 직접 연결 → **실패** (사설 IP, 외부에서 접근 불가)
- ICE가 자동으로 TURN relay로 **fallback**
- TURN → 내부 IP로 relay → **성공** (같은 네트워크)

이 fallback은 ICE 프로토콜의 정상적인 동작이며, 추가 설정 없이 자동으로 이루어집니다.

---

## 8. 최종 네트워크 흐름도

```
[브라우저]
    │
    │ (1) TURN Allocate
    │     turn:REMOVED_DB_HOST:443?transport=tcp
    │
    ▼
[AWS EC2 - TCP 443]
    │
    ▼
[Nginx stream - ssl_preread]
    │
    ├─ TLS → :8443 (HTTPS 서버)
    │
    └─ non-TLS → squiz-coturn:3478 (TURN)
                      │
                      │ (2) Allocate Response
                      │     relay address 할당
                      │
                      │ (3) ICE Connectivity Check
                      │     browser relay ↔ SFU 172.18.0.x:20000
                      │
                      ▼
                [squiz-sfu:20000]  ← 같은 Docker 네트워크
                      │
                      │ (4) DTLS Handshake
                      │
                      │ (5) SRTP Media Flow
                      │     (화면공유 비디오 스트림)
                      │
                      ▼
                [mediasoup Router]
                      │
                      │ (6) consume → 다른 브라우저로 전달
                      ▼
                [다른 브라우저의 recvTransport]
```
