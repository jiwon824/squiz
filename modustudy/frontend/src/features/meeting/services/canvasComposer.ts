// 해상도/프레임 제한 상수
const MAX_WIDTH = 1920;
const MAX_HEIGHT = 1080;
const CAPTURE_FPS = 24;
const PIP_WIDTH = 160;
const PIP_HEIGHT = 120;

class CanvasComposerService {
  private canvas!: HTMLCanvasElement;
  private ctx!: CanvasRenderingContext2D;
  private animationFrame: number | null = null;
  private isComposing = false;

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
  }

  private resetComposedStream() {
    if (this.composedStream) {
      this.composedStream.getTracks().forEach((t) => t.stop());
    }
    this.composedStream = this.canvas.captureStream(CAPTURE_FPS);
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

      // 최대 해상도 제한 적용
      const rawWidth = this.screenVideo.videoWidth || 1920;
      const rawHeight = this.screenVideo.videoHeight || 1080;
      const scale = Math.min(1, MAX_WIDTH / rawWidth, MAX_HEIGHT / rawHeight);
      const width = Math.round(rawWidth * scale);
      const height = Math.round(rawHeight * scale);

      this.ensureCanvas(width, height);
      this.resetComposedStream();

      this.isComposing = true;
      this.loop();
    }

    return this.composedStream!;
  }

  updatePipPosition(pos: typeof this.pipPosition) {
    this.pipPosition = pos;
  }

  private loop = () => {
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

    this.animationFrame = requestAnimationFrame(this.loop);
  };

  stopComposing() {
    this.isComposing = false;
    this.screenTrackId = null;
    this.cameraTrackId = null;

    if (this.animationFrame) {
      cancelAnimationFrame(this.animationFrame);
      this.animationFrame = null;
    }

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
  }
}

export default new CanvasComposerService();
