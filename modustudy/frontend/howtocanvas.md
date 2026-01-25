# WebRTC Canvas 기반 화면공유 + 캠 합성 전송 방식 정리

## 1. 개요

본 문서는 **화면공유 영상 위에 캠 영상을 작게 합성하여 하나의 비디오 트랙으로 전송**하는 WebRTC 구조를 설명한다.

이 방식은 `Canvas`를 활용하여 두 개의 영상 소스를 하나로 합성한 뒤, `canvas.captureStream()`을 통해 **단일 비디오 스트림**으로 전송하는 것이 핵심이다.

---

## 2. 도입 배경

### 기존 방식

* 캠 스트림 1개
* 화면공유 스트림 1개
* 총 **비디오 트랙 2개 전송**

➡️ SFU/서버/네트워크 트래픽 증가

### 개선 방식 (Canvas 합성)

* 화면공유 + 캠을 Canvas에서 합성
* **비디오 트랙 1개만 전송**

➡️ 서버 트래픽 감소 및 구조 단순화

---

## 3. 전체 구조 흐름

```
getDisplayMedia()  (화면공유)
getUserMedia()     (캠)
        ↓
      Canvas (합성)
        ↓
 canvas.captureStream()
        ↓
   WebRTC 전송 (Video 1 Track)
```

※ 오디오는 Canvas에 포함하지 않고, **캠 오디오 트랙을 그대로 사용**한다.

---

## 4. 구현 예시 코드

### 4.1 미디어 스트림 획득

```javascript
const screenStream = await navigator.mediaDevices.getDisplayMedia({
  video: true,
  audio: false
});

const camStream = await navigator.mediaDevices.getUserMedia({
  video: true,
  audio: true
});
```

---

### 4.2 비디오 엘리먼트 준비

```javascript
const screenVideo = document.createElement("video");
screenVideo.srcObject = screenStream;
screenVideo.play();

const camVideo = document.createElement("video");
camVideo.srcObject = camStream;
camVideo.play();
```

---

### 4.3 Canvas 합성 처리

```javascript
const canvas = document.createElement("canvas");
canvas.width = 1280;
canvas.height = 720;

const ctx = canvas.getContext("2d");

function draw() {
  // 화면공유 전체 렌더링
  ctx.drawImage(screenVideo, 0, 0, canvas.width, canvas.height);

  // 캠 화면 (우측 하단)
  const camWidth = 320;
  const camHeight = 180;

  ctx.drawImage(
    camVideo,
    canvas.width - camWidth - 20,
    canvas.height - camHeight - 20,
    camWidth,
    camHeight
  );

  requestAnimationFrame(draw);
}

draw();
```

---

### 4.4 Canvas → WebRTC 트랙 전송

```javascript
const mixedStream = canvas.captureStream(30); // 30fps

const videoTrack = mixedStream.getVideoTracks()[0];
const audioTrack = camStream.getAudioTracks()[0];

peerConnection.addTrack(videoTrack, mixedStream);
peerConnection.addTrack(audioTrack, camStream);
```

---

## 5. 장점 요약

* ✅ 비디오 트랙 수 감소 (2 → 1)
* ✅ SFU 서버 트래픽 절감
* ✅ 레이아웃 자유도 높음
* ✅ 발표/회의/녹화 구조에 적합
* ✅ 스트리밍 및 녹화 파이프라인 통합 가능

---

## 6. 주의사항

1. **Canvas 해상도 = 최종 화질**

   * 권장: 1280×720 또는 1920×1080

2. **CPU 사용량 관리**

   * 저사양 환경에서는 FPS 조절 필요

3. **오디오 처리**

   * Canvas에 오디오 합성 금지
   * 항상 원본 오디오 트랙 사용

4. **비율 유지**

   * 캠/화면 비율 깨짐 방지 필요

---

## 7. 결론

Canvas 기반 영상 합성 방식은 **WebRTC 화상회의 및 발표 시스템에서 실무적으로 검증된 구조**이며,
서버 트래픽과 시스템 복잡도를 동시에 줄일 수 있는 효율적인 설계이다.

특히 SFU 기반 다자간 화상회의 환경에서 큰 장점을 가진다.
