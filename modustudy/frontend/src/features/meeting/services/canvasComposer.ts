// 대역폭 절약을 위한 Canvas 해상도/프레임 제한 상수
const MAX_WIDTH = 1920;
const MAX_HEIGHT = 1080;
const CAPTURE_FPS = 15;
const PIP_WIDTH = 160;
const PIP_HEIGHT = 120;

class CanvasComposerService {
  private canvas!: HTMLCanvasElement;
  private ctx!: CanvasRenderingContext2D;
  private animationFrame: number | null = null;
  private frameRequestId: number | null = null;
  private frameTimerId: number | null = null;
  private lastFrameTs = 0;
  private isComposing = false;
  private lastCameraFrameTs = 0;
  private CAMERA_FRAME_INTERVAL = 100; // ms (10fps)
  private lastScreenFrameTs = 0;
  private SCREEN_FRAME_INTERVAL = 66; // ms (≈15fps)
  private needsRedraw = false;

  private screenVideo!: HTMLVideoElement;
  private cameraVideo!: HTMLVideoElement;
  private screenTrackId: string | null = null;
  private cameraTrackId: string | null = null;

  private composedStream: MediaStream | null = null;

  private pipPosition: 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right' = 'bottom-right';

  private pipWidth = PIP_WIDTH;
  private pipHeight = PIP_HEIGHT;
  private pipPadding = 20;

  private activeVideos: HTMLVideoElement[] = [];

  // 🔥 백그라운드 탭 대응을 위한 Web Worker
  private frameWorker: Worker | null = null;
  private offscreenCanvas: OffscreenCanvas | null = null;
  private useOffscreen = false;

  /* =========================
      Canvas 초기화
  ========================= */
  private ensureCanvas(width: number, height: number) {
    if (!this.canvas) {
      this.canvas = document.createElement('canvas');
      const container = document.createElement('div');
      container.style.position = 'fixed';
      container.style.left = '0';
      container.style.top = '0';
      container.style.width = '1px';
      container.style.height = '1px';
      container.style.opacity = '0';
      container.style.pointerEvents = 'none';
      container.appendChild(this.canvas);
      document.body.appendChild(container);
    }

    this.canvas.width = width;
    this.canvas.height = height;
    this.ctx = this.canvas.getContext('2d')!;
    this.ctx.imageSmoothingEnabled = true;

    // 🔥 OffscreenCanvas 지원 여부 확인 및 초기화
    if (typeof OffscreenCanvas !== 'undefined' && this.canvas.transferControlToOffscreen) {
      try {
        this.useOffscreen = true;
        this.offscreenCanvas = this.canvas.transferControlToOffscreen();
        this.initFrameWorker();
      } catch (e) {
        console.warn('OffscreenCanvas not available, using fallback', e);
        this.useOffscreen = false;
      }
    }
  }

  private resetComposedStream() {
    if (this.composedStream) {
      this.composedStream.getTracks().forEach((t) => t.stop());
    }
    
    // 🔥 백그라운드에서도 안정적인 프레임 레이트 유지
    const fps = CAPTURE_FPS;
    if (this.useOffscreen && this.offscreenCanvas) {
      this.composedStream = this.offscreenCanvas.captureStream(fps);
    } else {
      this.composedStream = this.canvas.captureStream(fps);
    }
  }

  private async prepareVideo(stream: MediaStream) {
    const video = document.createElement('video');
    video.srcObject = stream;
    video.muted = true;
    video.autoplay = true;
    video.playsInline = true;
    video.style.position = 'fixed';
    video.style.width = '1px';
    video.style.height = '1px';
    video.style.opacity = '0';

    document.body.appendChild(video);
    this.activeVideos.push(video);

    await video.play().catch(() => {});
    await new Promise<void>((resolve) => {
      if (video.readyState >= 2) resolve();
      video.onloadeddata = () => resolve();
    });

    return video;
  }

  // 🔥 Web Worker 초기화 (백그라운드 탭에서도 동작)
  private initFrameWorker() {
    if (!this.useOffscreen || !this.offscreenCanvas) return;

    const workerCode = `
      let canvas = null;
      let ctx = null;
      let screenBitmap = null;
      let cameraBitmap = null;
      let isRunning = false;
      let pipPosition = 'bottom-right';
      let pipWidth = ${PIP_WIDTH};
      let pipHeight = ${PIP_HEIGHT};
      let pipPadding = 20;
      let canvasWidth = 0;
      let canvasHeight = 0;

      self.onmessage = async (e) => {
        const { type, data } = e.data;

        if (type === 'init') {
          canvas = data.canvas;
          ctx = canvas.getContext('2d');
          ctx.imageSmoothingEnabled = true;
          canvasWidth = data.width;
          canvasHeight = data.height;
          pipPosition = data.pipPosition || 'bottom-right';
        }

        if (type === 'updatePipPosition') {
          pipPosition = data.position;
        }

        if (type === 'screenFrame') {
          if (screenBitmap) screenBitmap.close();
          screenBitmap = data.bitmap;
        }

        if (type === 'cameraFrame') {
          if (cameraBitmap) cameraBitmap.close();
          cameraBitmap = data.bitmap;
        }

        if (type === 'draw') {
          if (!ctx) return;
          
          ctx.clearRect(0, 0, canvasWidth, canvasHeight);

          if (screenBitmap) {
            ctx.drawImage(screenBitmap, 0, 0, canvasWidth, canvasHeight);
          }

          if (cameraBitmap) {
            const x = pipPosition.includes('right')
              ? canvasWidth - pipWidth - pipPadding
              : pipPadding;

            const y = pipPosition.includes('bottom')
              ? canvasHeight - pipHeight - pipPadding
              : pipPadding;

            ctx.drawImage(cameraBitmap, x, y, pipWidth, pipHeight);
          }
        }

        if (type === 'start') {
          isRunning = true;
        }

        if (type === 'stop') {
          isRunning = false;
          if (screenBitmap) {
            screenBitmap.close();
            screenBitmap = null;
          }
          if (cameraBitmap) {
            cameraBitmap.close();
            cameraBitmap = null;
          }
        }
      };
    `;

    const blob = new Blob([workerCode], { type: 'application/javascript' });
    const workerUrl = URL.createObjectURL(blob);
    this.frameWorker = new Worker(workerUrl);

    this.frameWorker.postMessage({
      type: 'init',
      data: {
        canvas: this.offscreenCanvas,
        width: this.canvas.width,
        height: this.canvas.height,
        pipPosition: this.pipPosition,
      },
    }, [this.offscreenCanvas]);
  }

  /* =========================
      합성 시작
  ========================= */
  async composeStreams(
    screenStream: MediaStream,
    cameraStream: MediaStream,
    options?: { pipPosition?: typeof this.pipPosition }
  ): Promise<MediaStream> {
    if (options?.pipPosition) {
      this.pipPosition = options.pipPosition;
      if (this.useOffscreen && this.frameWorker) {
        this.frameWorker.postMessage({
          type: 'updatePipPosition',
          data: { position: this.pipPosition },
        });
      }
    }

    const nextScreenTrackId = screenStream.getVideoTracks()[0]?.id ?? null;
    const nextCameraTrackId = cameraStream.getVideoTracks()[0]?.id ?? null;

    const shouldReset =
      !this.isComposing ||
      this.screenTrackId !== nextScreenTrackId ||
      this.cameraTrackId !== nextCameraTrackId;

    if (shouldReset) {
      this.stopComposing();

      this.screenTrackId = nextScreenTrackId;
      this.cameraTrackId = nextCameraTrackId;

      this.screenVideo = await this.prepareVideo(screenStream);
      this.cameraVideo = await this.prepareVideo(cameraStream);

      const srcWidth = this.screenVideo.videoWidth || MAX_WIDTH;
      const srcHeight = this.screenVideo.videoHeight || MAX_HEIGHT;
      const scale = Math.min(1, MAX_WIDTH / srcWidth, MAX_HEIGHT / srcHeight);
      const width = Math.round(srcWidth * scale);
      const height = Math.round(srcHeight * scale);

      this.ensureCanvas(width, height);
      this.resetComposedStream();

      this.isComposing = true;
      this.startFrameLoop();
    }

    return this.composedStream!;
  }

  updatePipPosition(pos: typeof this.pipPosition) {
    this.pipPosition = pos;
    if (this.useOffscreen && this.frameWorker) {
      this.frameWorker.postMessage({
        type: 'updatePipPosition',
        data: { position: pos },
      });
    }
  }

  // 🔥 Worker 기반 또는 일반 루프
  private startFrameLoop() {
    this.stopFrameLoop();
    this.lastFrameTs = 0;

    if (this.useOffscreen && this.frameWorker) {
      this.frameWorker.postMessage({ type: 'start' });
      this.startWorkerFrameLoop();
    } else {
      this.startRegularFrameLoop();
    }
  }

  private startWorkerFrameLoop() {
    if (!this.screenVideo || !this.frameWorker) return;

    const sendScreenFrame = async (now: number) => {
      if (!this.isComposing || !this.frameWorker) return;
      if (now - this.lastScreenFrameTs < this.SCREEN_FRAME_INTERVAL) return;

      this.lastScreenFrameTs = now;

      try {
        if (this.screenVideo.readyState >= 2) {
          const screenBitmap = await createImageBitmap(this.screenVideo);
          this.frameWorker.postMessage(
            { type: 'screenFrame', data: { bitmap: screenBitmap } },
            [screenBitmap]
          );
        this.needsRedraw = true; // ✅ 추가
        }
      } catch (e) {
        console.warn('screen frame error', e);
      }
    };


    const sendCameraFrame = async (now: number) => {
      if (!this.cameraVideo || this.cameraVideo.readyState < 2) return;
      if (now - this.lastCameraFrameTs < this.CAMERA_FRAME_INTERVAL) return;

      this.lastCameraFrameTs = now;

      try {
        const cameraBitmap = await createImageBitmap(this.cameraVideo);
        this.frameWorker.postMessage(
          { type: 'cameraFrame', data: { bitmap: cameraBitmap } },
          [cameraBitmap]
        );
      } catch (e) {
        console.warn('camera frame error', e);
      }
    };

  const loop = (now: number) => {
    if (!this.isComposing) return;

    sendScreenFrame(now);
    sendCameraFrame(now);
    if (this.needsRedraw) {
      this.frameWorker?.postMessage({ type: 'draw' });
      this.needsRedraw = false;
    }

    this.animationFrame = requestAnimationFrame(loop);
  };

  this.animationFrame = requestAnimationFrame(loop);
}


  // 기존 프레임 루프 (fallback)
  private startRegularFrameLoop() {
    const intervalMs = Math.max(1000 / CAPTURE_FPS, 16);
    this.frameTimerId = window.setInterval(() => {
      if (!this.isComposing) return;
      this.drawFrame();
    }, intervalMs);
  }

  private drawFrame = () => {
    if (!this.isComposing) return;

    this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

    if (this.screenVideo?.readyState >= 2) {
      this.ctx.drawImage(this.screenVideo, 0, 0, this.canvas.width, this.canvas.height);
    }

    const x =
      this.pipPosition.includes('right')
        ? this.canvas.width - this.pipWidth - this.pipPadding
        : this.pipPadding;

    const y =
      this.pipPosition.includes('bottom')
        ? this.canvas.height - this.pipHeight - this.pipPadding
        : this.pipPadding;

    if (this.cameraVideo?.readyState >= 2) {
      this.ctx.drawImage(this.cameraVideo, x, y, this.pipWidth, this.pipHeight);
    }
  };

  private stopFrameLoop() {
    if (this.frameTimerId !== null) {
      window.clearInterval(this.frameTimerId);
      this.frameTimerId = null;
    }
    if (this.animationFrame) {
      cancelAnimationFrame(this.animationFrame);
      this.animationFrame = null;
    }
    if (this.frameWorker) {
      this.frameWorker.postMessage({ type: 'stop' });
    }
  }

  stopComposing() {
    this.isComposing = false;
    this.screenTrackId = null;
    this.cameraTrackId = null;

    this.stopFrameLoop();

    if (this.composedStream) {
      this.composedStream.getTracks().forEach((t) => t.stop());
      this.composedStream = null;
    }

    this.activeVideos.forEach((v) => {
      v.pause();
      v.srcObject = null;
      v.remove();
    });
    this.activeVideos = [];

    if (this.frameWorker) {
      this.frameWorker.terminate();
      this.frameWorker = null;
    }

    this.useOffscreen = false;
    this.offscreenCanvas = null;
  }

  // 🔥 디버깅: 현재 합성 상태 확인
  isComposingWith(screenTrackId: string, cameraTrackId: string): boolean {
    return (
      this.isComposing &&
      this.screenTrackId === screenTrackId &&
      this.cameraTrackId === cameraTrackId
    );
  }
}

export default new CanvasComposerService();