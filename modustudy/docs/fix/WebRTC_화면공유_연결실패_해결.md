# WebRTC 화면공유/캠 연결 실패 해결

## 증상
- 미팅 화면에서 화면공유 또는 캠을 켜도 상대방에게 보이지 않음
- 브라우저 콘솔: `[sfu] send transport state connecting` → `[sfu] send transport state failed`
- SFU 서버 로그: `transport created` 만 찍히고 `transport connected`, `produce`, `consume` 등 후속 로그 없음

## 원인 분석 (총 5가지 이슈)

### 1. SFU_ANNOUNCED_IP에 도메인 사용 (치명적)
- **문제**: `SFU_ANNOUNCED_IP=i14d106.p.ssafy.io` (도메인명)
- **원인**: mediasoup의 `announcedIp`는 반드시 IP 주소여야 함. 도메인명을 넣으면 ICE candidate가 유효하지 않음
- **해결**: `SFU_ANNOUNCED_IP=REMOVED_DB_HOST` (공인 IP)로 변경

### 2. SFU baseUrl 경로 불일치
- **문제**: 백엔드 `app.sfu.base-url=wss://i14d106.p.ssafy.io/sfu`
- **원인**: 프론트엔드 Socket.io가 `wss://host/sfu`에 연결하면 실제 요청은 `/sfu/socket.io/`로 감. SFU 서버는 `/socket.io/`만 인식
- **해결**: `APP_SFU_BASE_URL=wss://i14d106.p.ssafy.io` (경로 없이)로 변경. Nginx가 `/socket.io/` 경로를 SFU로 프록시하므로 별도 경로 불필요

### 3. config.js listenIp 바인딩 버그
- **문제**: 컨테이너 안에서 이전 버전의 config.js가 `listenIp: process.env.ANNOUNCED_IP || process.env.SFU_ANNOUNCED_IP || ...` 순서로 참조
- **원인**: `SFU_ANNOUNCED_IP=REMOVED_DB_HOST`가 설정되어 있어서 `listenIp`가 공인 IP로 설정됨. Docker 컨테이너 내부에서 외부 IP에 바인딩 불가
- **해결**: 소스 config.js는 이미 `process.env.LISTEN_IP`를 먼저 참조하도록 수정되어 있었음. 컨테이너 재빌드로 해결

### 4. SSAFY 인프라 포트 차단 (핵심 원인)
- **문제**: WebRTC 미디어 포트(20000-21000 TCP/UDP)가 외부에서 접근 불가
- **원인**: SSAFY 제공 서버의 상위 네트워크(AWS 보안그룹 또는 SSAFY 네트워크 정책)에서 **80, 443 포트만 외부 허용**. ufw에서 포트를 열어도 상위에서 차단
- **확인 방법**: 로컬 PC PowerShell에서 `Test-NetConnection -ComputerName REMOVED_DB_HOST -Port 20000` → `TCP connect failed`

### 5. Docker 포트 매핑에 TCP 누락
- **문제**: docker-compose에 `"20000-21000:20000-21000/udp"`만 있고 TCP 매핑 없음
- **해결**: `"20000-21000:20000-21000/tcp"` 추가

## 최종 해결: TURN 서버 (coturn) 도입

SSAFY 인프라에서 80/443 외 포트를 차단하므로, 클라이언트가 SFU 미디어 포트에 직접 접근 불가. **TURN 서버를 릴레이로 사용**하여 443 포트를 통해 미디어 전달.

### 구성
```
클라이언트 → TURN (UDP 443) → SFU (내부 네트워크)
```

### coturn 설정 (`turnserver.conf`)
```conf
listening-port=443
listening-ip=0.0.0.0
external-ip=REMOVED_DB_HOST
relay-ip=0.0.0.0
min-port=49152
max-port=49200
fingerprint
lt-cred-mech
user=squiz:squiz2025turn
realm=i14d106.p.ssafy.io
no-tcp
no-tls
no-dtls
no-tcp-relay
log-file=stdout
```

- **443 UDP 사용**: Nginx가 443 TCP를 점유하고 있으므로 UDP와 충돌 없음
- `no-tcp`: TCP 리스닝 비활성화 (443 TCP는 Nginx가 사용)
- `network_mode: host`: Docker에서 호스트 네트워크 사용 (포트 매핑 불필요)

### coturn Docker 실행
```bash
sudo docker run -d --name squiz-coturn --network=host --restart=unless-stopped \
  -v /home/ubuntu/squiz/turnserver.conf:/etc/coturn/turnserver.conf \
  coturn/coturn:latest
```

### SFU 서버 수정

**config.js** - TURN 환경변수 추가:
```js
turnUrl: process.env.TURN_URL || '',
turnUsername: process.env.TURN_USERNAME || '',
turnCredential: process.env.TURN_CREDENTIAL || '',
```

**index.js** - `createWebRtcTransport` 응답에 iceServers 포함:
```js
const iceServers = [];
if (config.turnUrl) {
  iceServers.push({
    urls: config.turnUrl,
    username: config.turnUsername,
    credential: config.turnCredential
  });
}

callback({
  params: {
    id: transport.id,
    iceParameters: transport.iceParameters,
    iceCandidates: transport.iceCandidates,
    dtlsParameters: transport.dtlsParameters,
    iceServers  // TURN 정보 포함
  }
});
```

### docker-compose.blue.yml SFU 환경변수 추가
```yaml
- TURN_URL=turn:REMOVED_DB_HOST:443?transport=udp
- TURN_USERNAME=squiz
- TURN_CREDENTIAL=squiz2025turn
```

## 수정된 파일 목록

### 서버 설정 (운영 서버 직접 수정)
- `/home/ubuntu/squiz/docker-compose.blue.yml` - SFU 환경변수, 포트 매핑, coturn 설정
- `/home/ubuntu/squiz/docker-compose.green.yml` - 동일 변경
- `/home/ubuntu/squiz/turnserver.conf` - coturn 설정 파일 (신규)

### SFU 소스코드
- `sfu-server/src/config.js` - TURN 환경변수 설정 추가
- `sfu-server/src/index.js` - iceServers를 transport 응답에 포함

### 프론트엔드
- `frontend/src/features/meeting/services/sfuClient.ts` - transport 파라미터 디버그 로그 추가

## 핵심 교훈
1. **announcedIp는 반드시 IP 주소** (도메인 X)
2. **제한된 네트워크 환경에서는 TURN 서버 필수** - 80/443 외 포트 차단 시 직접 연결 불가
3. **Docker 컨테이너 내 listenIp는 0.0.0.0** - 외부 IP 바인딩 불가
4. **Docker 포트 매핑에 TCP/UDP 모두 명시** - 기본값은 TCP만
5. **coturn 443 UDP는 Nginx 443 TCP와 공존 가능** - 프로토콜이 다르면 같은 포트 사용 가능
