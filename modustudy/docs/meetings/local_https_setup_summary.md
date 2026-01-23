# 로컬 테스트 정리 (modustudy.local, HTTPS, RTC 공유)

이 문서는 로컬 네트워크에서 **다른 PC까지 포함해** 카메라/마이크/화면공유와 카카오 로그인까지 동작하게 만들기 위해
수정/설정한 내용을 정리한 것이다.

---

## 1) 왜 modustudy.local로 변경했나?

- 브라우저 보안 정책 때문에 **HTTP + IP 접속에서는 카메라/마이크가 차단**됨.
- 카카오 로그인은 **로컬 IP(192.168.x.x)**를 redirect 도메인으로 허용하지 않는 경우가 많아 `KOE006` 발생.
- 그래서 **로컬 도메인(modustudy.local) + HTTPS**로 테스트하는 방식으로 변경.

---

## 2) 네트워크 접속 방식

같은 네트워크 참가자는 아래 주소로 접속:

```
https://modustudy.local:3000
```

> IP 접속(`http://192.168.100.90:3000`)은 카메라/마이크가 막히므로 권장하지 않음.

---

## 3) hosts 설정 (모든 기기)

각 PC에서 `hosts` 파일에 아래 줄 추가:

```
192.168.100.90  modustudy.local
```

Windows 경로:
```
C:\Windows\System32\drivers\etc\hosts
```

---

## 4) mkcert + 로컬 HTTPS

내 PC에서 인증서 생성:

```powershell
cd C:\SSAFY\S14P11D106\modustudy
mkcert -install
mkcert -cert-file modustudy.local.pem -key-file modustudy.local-key.pem modustudy.local 192.168.100.90 localhost 127.0.0.1
```

각 PC에서도 mkcert 신뢰 등록:

```powershell
mkcert -install
```

---

## 5) 프론트 HTTPS 설정 (Vite)

`frontend/vite.config.ts`

- HTTPS 인증서 경로 설정
- 외부 접속을 위해 `host: 0.0.0.0`

```ts
server: {
  host: '0.0.0.0',
  port: 3000,
  https: {
    cert: modustudy.local.pem,
    key: modustudy.local-key.pem,
  }
}
```

---

## 6) 백엔드 HTTPS 설정

`backend/src/main/resources/application.properties`

```properties
server.ssl.enabled=true
server.ssl.certificate=C:/SSAFY/S14P11D106/modustudy/modustudy.local.pem
server.ssl.certificate-private-key=C:/SSAFY/S14P11D106/modustudy/modustudy.local-key.pem
```

---

## 7) API 접속 주소 (Frontend)

`frontend/.env`

```
VITE_API_URL=https://modustudy.local:8080
```

---

## 8) CORS 설정

`backend/src/main/java/com/ssafy/config/SecurityConfig.java`

허용 도메인에 아래 추가:

```
https://modustudy.local:3000
```

---

## 9) 카카오 로그인 설정

카카오 개발자 콘솔 설정:

**플랫폼 > Web > 사이트 도메인**
```
https://modustudy.local:3000
```

**카카오 로그인 > Redirect URI**
```
https://modustudy.local:3000/login/callback
```

> redirect URI는 정확히 일치해야 함(슬래시/https/포트 포함).

---

## 10) SFU HTTPS 설정

`sfu-server/.env`

```
SFU_USE_HTTPS=true
CORS_ORIGINS=https://modustudy.local:3000,http://localhost:3000,http://localhost:3001
ANNOUNCED_IP=192.168.100.90
SFU_SSL_CERT_PATH=C:/SSAFY/S14P11D106/modustudy/modustudy.local.pem
SFU_SSL_KEY_PATH=C:/SSAFY/S14P11D106/modustudy/modustudy.local-key.pem
```

`backend/src/main/resources/application.properties`

```
app.sfu.base-url=https://modustudy.local:4000
```

---

## 11) 방화벽 인바운드 규칙

- TCP 3000 (프론트)
- TCP 8080 (백엔드)
- TCP 4000 (SFU signaling)
- UDP 20000-22000 (mediasoup RTP)

PowerShell (관리자):

```powershell
New-NetFirewallRule -DisplayName "FE 3000" -Direction Inbound -Protocol TCP -LocalPort 3000 -Action Allow
New-NetFirewallRule -DisplayName "BE 8080" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow
New-NetFirewallRule -DisplayName "SFU TCP 4000" -Direction Inbound -Protocol TCP -LocalPort 4000 -Action Allow
New-NetFirewallRule -DisplayName "SFU UDP 20000-22000" -Direction Inbound -Protocol UDP -LocalPort 20000-22000 -Action Allow
```

---

## 12) 카메라/화면 공유 동작 정책

### 기본 정책
- **모든 참가자 카메라 스트림은 항상 켜짐** (얼굴 인식용)
- 화면에는 **발표자 공유 화면 + 발표자 캠만 표시**
- **참가자 캠은 화면에 띄우지 않음**

### 발표자 송출
- 발표자가 **캠 ON을 누르면** 모두에게 영상 송출됨
- **캠 OFF**는 송출만 중지, 얼굴 인식은 계속 동작

### 얼굴 인식
- 모든 참가자의 얼굴 인식 결과를 WS로 전송
- 참가자 리스트에서 **자리 있음/없음** 표시

### 발화 표시
- 마이크 음성 감지 시 speaking 상태를 WS로 전송
- 참가자 리스트에서 발화 강조 표시

---

## 13) 방송 수신 화면 라벨

- 수신 화면 좌측 하단 라벨:  
  **"발표자: 닉네임"**

---

## 14) 실행 순서 (로컬 테스트)

```powershell
# SFU
cd C:\SSAFY\S14P11D106\modustudy\sfu-server
npm start

# Backend (재시작)

# Frontend
cd C:\SSAFY\S14P11D106\modustudy\frontend
npm run dev
```

접속:
```
https://modustudy.local:3000
```

---

## 15) 참고

- `http://192.168.100.90:3000` 접속은 카메라/마이크가 차단됨.
- EC2 배포 시에는 **공인 도메인 + 공인 인증서**로 운영 (mkcert 불필요).
