# OpenVidu vs 내 구조(mediasoup + Canvas) 성능 비교 & 기술 흐름 정리

---

## 1. 전체 구조 한 줄 요약

**내 구조**
> 클라이언트에서 MediaStream을 Canvas로 합성 → 단일 video track 생성 → mediasoup SFU로 중계

**OpenVidu 구조**
> 클라이언트에서 개별 트랙 전송 → 서버(OpenVidu)가 믹싱/레이아웃 처리 → 재전송

---

## 2. OpenVidu vs 내 구조 성능 비교표

| 구분 | OpenVidu (기본 설정) | 내 구조 (mediasoup + Canvas) |
|----|----|----|
| 영상 합성 위치 | 서버(MCU 또는 Hybrid) | **클라이언트(Canvas)** |
| 서버 CPU 사용량 | 매우 높음 (믹싱/인코딩) | **매우 낮음 (중계만)** |
| 서버 트래픽 | 참가자 수 × 해상도 | **발표자 1스트림 기준** |
| 확장성 | 서버 증설 필수 | **SFU 수평 확장 용이** |
| PIP/레이아웃 | 제한적 | **완전 자유(Canvas)** |
| 발표자 전환 | 서버 로직 필요 | **클라이언트 제어** |
| 지연(Latency) | 중간~높음 | **낮음** |
| 클라 CPU 부담 | 낮음 | **발표자만 높음** |
| 커스터마이징 | 낮음 | **매우 높음** |

📌 결론
> **발표 중심 미팅, 교육, 스터디 서비스 → 내 구조가 압도적으로 유리**

---

## 3. 내 시스템 전체 기술 흐름 (실제 코드 기준)

### 3-1. 시그널링
- REST / WebSocket
- 방 입장, 발표자 지정, 상태 공유
- 미디어 데이터 ❌

### 3-2. mediasoup 연결
1. Router RTP Capabilities 수신
2. Send / Recv Transport 생성
3. Producer/Consumer 준비

### 3-3. 로컬 MediaStream 생성
- camera: getUserMedia()
- screen: getDisplayMedia()

### 3-4. Canvas 합성 (핵심)
- screen + camera → canvas draw
- canvas.captureStream(30)
- **단일 video track 생성**

### 3-5. SFU 전송
- sfuClient.produceTrack('video', track)
- SFU는 영상 내용 관여 ❌

### 3-6. 수신자
- consumer.stream → <video>
- 합성 여부 인지 ❌

---

## 4. 발생했던 문제: 캠+화면 → 다시 캠+화면 시 검정 화면

### 4-1. 현상
- 최초 캠+화면 정상
- 모드 전환 후 다시 캠+화면
- **검정 화면 출력**

### 4-2. 근본 원인

1. canvas.captureStream()은 **같은 canvas 기준 단일 stream**
2. stopComposing() 시:
   - video element 제거
   - animationFrame cancel
3. 하지만 기존 composedStream track은:
   - 이미 ended 상태
   - mediasoup producer는 살아있음

➡️ **죽은 track을 다시 publish**

---

## 5. 해결 핵심 원칙

### ✅ 핵심 규칙 (가장 중요)

> **Canvas 기반 video track은 반드시 일회용**

---

## 6. 실제 적용한 해결 방법 요약

### 6-1. CanvasComposer 개선 포인트

- captureStream은 canvas 생성 시 1회만
- stopComposing에서는:
  - track stop ❌
  - video element + RAF만 정리

### 6-2. 모드 전환 시 처리 순서

1. producer close
2. canvas stopComposing()
3. composedStreamRef = null
4. 새 composeStreams()
5. 새 video track produce

---

## 7. 안정적인 전환을 위한 안전 패턴

```ts
canvasComposer.stopComposing();
composedStreamRef.current = null;
await sfuClient.closeProducer('video');

const composed = await canvasComposer.composeStreams(screen, camera);
const track = composed.getVideoTracks()[0];
await sfuClient.produceTrack('video', track);
```

---

## 8. 이 구조의 기술적 평가

### 👍 장점
- 서버 비용 최소화
- 레이아웃/효과 무제한
- Zoom/Meet 발표 구조와 유사

### ⚠️ 주의점
- 발표자 클라 성능 의존
- Canvas track 생명주기 엄격 관리 필요

---

## 9. 최종 결론

> **내 구조는 OpenVidu보다 한 단계 더 낮은 레벨에서 직접 제어하는 구조**
>
> 잘 설계하면 성능·확장성·자유도 모두 OpenVidu를 능가함

---

필요하면 다음도 바로 확장 가능
- Zoom / Google Meet 구조 비교
- Canvas 없는 replaceTrack 대안
- 모바일 저사양 fallback 전략

