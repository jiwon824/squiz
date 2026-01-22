class CanvasComposerService {
    private canvas: HTMLCanvasElement | null = null;
    private ctx: CanvasRenderingContext2D | null = null;
    private animationFrame: number | null = null;
    private isComposing = false;
    private composeToken = 0;
    private canvasContainer: HTMLDivElement | null = null;
    private activeVideos: HTMLVideoElement[] = [];

    private screenTrackId: string | null = null;
    private cameraTrackId: string | null = null;

    private pipPosition: 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right' = 'bottom-right';
    private pipWidth = 320;
    private pipHeight = 240;
    private pipPadding = 20;
    private borderColor = '#3b82f6';
    private borderWidth = 3;
    private screenBuffer: HTMLCanvasElement | null = null;
    private screenBufferCtx: CanvasRenderingContext2D | null = null;
    private cameraBuffer: HTMLCanvasElement | null = null;
    private cameraBufferCtx: CanvasRenderingContext2D | null = null;
    private hasScreenFrame = false;
    private hasCameraFrame = false;
    private readyPromise: Promise<void> | null = null;
    private resolveReady: (() => void) | null = null;
    private isReady = false;

    createCanvas(width = 1920, height = 1080) {
        this.canvas = document.createElement('canvas');
        this.canvas.width = width;
        this.canvas.height = height;
        this.ctx = this.canvas.getContext('2d');
        if (this.ctx) {
            this.ctx.imageSmoothingEnabled = true;
        }
        if (!this.canvasContainer) {
            const container = document.createElement('div');
            container.style.position = 'fixed';
            container.style.left = '-9999px';
            container.style.top = '0';
            container.style.width = '640px';
            container.style.height = '360px';
            container.style.opacity = '1';
            container.style.pointerEvents = 'none';
            container.appendChild(this.canvas);
            document.body.appendChild(container);
            this.canvasContainer = container;
        } else if (this.canvas && !this.canvasContainer.contains(this.canvas)) {
            this.canvasContainer.appendChild(this.canvas);
        }
        return this.canvas;
    }

    private async ensurePlayback(video: HTMLVideoElement, timeoutMs = 3000) {
        try {
            await video.play();
        } catch {
            // ignore autoplay rejection
        }
        if (!video.paused && video.readyState >= 2) return;
        const start = performance.now();
        return new Promise<void>((resolve) => {
            const tick = () => {
                if (!video.paused && video.readyState >= 2) {
                    resolve();
                    return;
                }
                if (performance.now() - start > timeoutMs) {
                    resolve();
                    return;
                }
                requestAnimationFrame(tick);
            };
            tick();
        });
    }

    private waitForVideoReady(video: HTMLVideoElement) {
        return new Promise<void>((resolve, reject) => {
            const onReady = () => {
                cleanup();
                resolve();
            };
            const onError = (err: Event) => {
                cleanup();
                reject(err);
            };
            const cleanup = () => {
                video.removeEventListener('loadedmetadata', onReady);
                video.removeEventListener('canplay', onReady);
                video.removeEventListener('playing', onReady);
                video.removeEventListener('error', onError);
            };
            if (video.readyState >= 2) {
                resolve();
                return;
            }
            video.addEventListener('loadedmetadata', onReady, { once: true });
            video.addEventListener('canplay', onReady, { once: true });
            video.addEventListener('playing', onReady, { once: true });
            video.addEventListener('error', onError, { once: true });
        });
    }

    private waitForDimensions(video: HTMLVideoElement, timeoutMs = 4000) {
        if (video.videoWidth > 0 && video.videoHeight > 0) return Promise.resolve();
        return new Promise<void>((resolve) => {
            const start = performance.now();
            const tick = () => {
                if (video.videoWidth > 0 && video.videoHeight > 0) {
                    resolve();
                    return;
                }
                if (performance.now() - start > timeoutMs) {
                    resolve();
                    return;
                }
                requestAnimationFrame(tick);
            };
            tick();
        });
    }

    private waitForFirstFrame(video: HTMLVideoElement, timeoutMs = 4000) {
        if ('requestVideoFrameCallback' in video) {
            return new Promise<void>((resolve) => {
                let resolved = false;
                const timeoutId = window.setTimeout(() => {
                    if (!resolved) resolve();
                }, timeoutMs);
                (video as HTMLVideoElement & { requestVideoFrameCallback: (cb: () => void) => void })
                    .requestVideoFrameCallback(() => {
                        resolved = true;
                        clearTimeout(timeoutId);
                        resolve();
                    });
            });
        }
        return new Promise<void>((resolve) => {
            const onData = () => {
                cleanup();
                resolve();
            };
            const timeoutId = window.setTimeout(() => {
                cleanup();
                resolve();
            }, timeoutMs);
            const cleanup = () => {
                video.removeEventListener('loadeddata', onData);
                clearTimeout(timeoutId);
            };
            video.addEventListener('loadeddata', onData, { once: true });
        });
    }

    private async prepareVideo(stream: MediaStream) {
        const video = document.createElement('video');
        video.srcObject = stream;
        video.muted = true;
        video.playsInline = true;
        video.autoplay = true;
        video.setAttribute('muted', 'true');
        video.setAttribute('playsinline', 'true');
        video.style.width = '640px';
        video.style.height = '360px';
        video.style.position = 'fixed';
        video.style.left = '-9999px';
        video.style.top = '0';
        video.style.opacity = '1';
        video.style.pointerEvents = 'none';
        if (this.canvasContainer) {
            this.canvasContainer.appendChild(video);
            this.activeVideos.push(video);
        }
        await this.ensurePlayback(video, 3000);
        await this.waitForVideoReady(video);
        await this.waitForDimensions(video, 4000);
        await this.waitForFirstFrame(video, 4000);
        return video;
    }

    async composeStreams(
        screenStream: MediaStream,
        cameraStream: MediaStream,
        options: {
            pipPosition?: 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right';
            pipWidth?: number;
            pipHeight?: number;
            pipPadding?: number;
            borderColor?: string;
            borderWidth?: number;
        } = {}
    ) {
        if (screenStream.getVideoTracks().length === 0 || cameraStream.getVideoTracks().length === 0) {
            return null;
        }
        if (!this.canvas) {
            this.createCanvas(1920, 1080);
        }

        const {
            pipPosition = 'bottom-right',
            pipWidth = 320,
            pipHeight = 240,
            pipPadding = 20,
            borderColor = '#3b82f6',
            borderWidth = 3,
        } = options;

        this.stopComposing();
        const token = ++this.composeToken;

        const screenVideo = await this.prepareVideo(screenStream);
        const cameraVideo = await this.prepareVideo(cameraStream);

        if (token !== this.composeToken) return null;

        const screenSettings = screenStream.getVideoTracks()[0]?.getSettings?.() ?? {};
        const width = screenVideo.videoWidth || screenSettings.width || 1920;
        const height = screenVideo.videoHeight || screenSettings.height || 1080;
        if (!this.canvas) {
            this.createCanvas(width, height);
        } else {
            this.canvas.width = width;
            this.canvas.height = height;
        }

        if (!this.ctx) return null;

        this.screenBuffer = document.createElement('canvas');
        this.screenBuffer.width = this.canvas.width;
        this.screenBuffer.height = this.canvas.height;
        this.screenBufferCtx = this.screenBuffer.getContext('2d');

        this.cameraBuffer = document.createElement('canvas');
        this.cameraBuffer.width = this.pipWidth;
        this.cameraBuffer.height = this.pipHeight;
        this.cameraBufferCtx = this.cameraBuffer.getContext('2d');
        this.hasScreenFrame = false;
        this.hasCameraFrame = false;
        this.isReady = false;
        this.readyPromise = new Promise<void>((resolve) => {
            this.resolveReady = resolve;
        });

        this.isComposing = true;
        this.screenTrackId = screenStream.getVideoTracks()[0]?.id ?? null;
        this.cameraTrackId = cameraStream.getVideoTracks()[0]?.id ?? null;
        this.pipPosition = pipPosition;
        this.pipWidth = pipWidth;
        this.pipHeight = pipHeight;
        this.pipPadding = pipPadding;
        this.borderColor = borderColor;
        this.borderWidth = borderWidth;

        const getPipPosition = () => {
            const positions = {
                'top-left': { x: this.pipPadding, y: this.pipPadding },
                'top-right': { x: this.canvas!.width - this.pipWidth - this.pipPadding, y: this.pipPadding },
                'bottom-left': { x: this.pipPadding, y: this.canvas!.height - this.pipHeight - this.pipPadding },
                'bottom-right': {
                    x: this.canvas!.width - this.pipWidth - this.pipPadding,
                    y: this.canvas!.height - this.pipHeight - this.pipPadding,
                },
            };
            return positions[this.pipPosition] || positions['bottom-right'];
        };

        const renderOnce = () => {
            if (!this.isComposing || !this.ctx || !this.canvas) return;

            const pipPos = getPipPosition();

            this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
            this.ctx.fillStyle = '#000';
            this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);

            if (
                screenVideo.readyState >= 2 &&
                screenVideo.videoWidth > 0 &&
                this.screenBufferCtx &&
                this.screenBuffer
            ) {
                try {
                    this.screenBufferCtx.drawImage(
                        screenVideo,
                        0,
                        0,
                        this.screenBuffer.width,
                        this.screenBuffer.height
                    );
                    this.hasScreenFrame = true;
                } catch {
                    // ignore
                }
            }

            if (this.screenBuffer && this.hasScreenFrame) {
                this.ctx.drawImage(this.screenBuffer, 0, 0, this.canvas.width, this.canvas.height);
            }

            if (
                cameraVideo.readyState >= 2 &&
                cameraVideo.videoWidth > 0 &&
                this.cameraBufferCtx &&
                this.cameraBuffer
            ) {
                try {
                    this.cameraBufferCtx.clearRect(0, 0, this.cameraBuffer.width, this.cameraBuffer.height);
                    this.cameraBufferCtx.drawImage(
                        cameraVideo,
                        0,
                        0,
                        this.cameraBuffer.width,
                        this.cameraBuffer.height
                    );
                    this.hasCameraFrame = true;
                } catch {
                    // ignore
                }
            }

            if (this.cameraBuffer && this.hasCameraFrame) {
                this.ctx.strokeStyle = this.borderColor;
                this.ctx.lineWidth = this.borderWidth;
                this.ctx.strokeRect(
                    pipPos.x - this.borderWidth,
                    pipPos.y - this.borderWidth,
                    this.pipWidth + this.borderWidth * 2,
                    this.pipHeight + this.borderWidth * 2
                );
                this.ctx.drawImage(this.cameraBuffer, pipPos.x, pipPos.y, this.pipWidth, this.pipHeight);
            }

            if (!this.isReady && this.hasScreenFrame && this.hasCameraFrame) {
                this.isReady = true;
                if (this.resolveReady) {
                    this.resolveReady();
                    this.resolveReady = null;
                }
            }
        };

        const hasFrameCallback =
            'requestVideoFrameCallback' in screenVideo && 'requestVideoFrameCallback' in cameraVideo;
        if (hasFrameCallback) {
            const scheduleScreen = () => {
                if (!this.isComposing || !this.screenBufferCtx || !this.screenBuffer) return;
                (screenVideo as HTMLVideoElement & { requestVideoFrameCallback: (cb: () => void) => void })
                    .requestVideoFrameCallback(() => {
                        try {
                            this.screenBufferCtx!.drawImage(
                                screenVideo,
                                0,
                                0,
                                this.screenBuffer!.width,
                                this.screenBuffer!.height
                            );
                            this.hasScreenFrame = true;
                        } catch {
                            // ignore
                        }
                        scheduleScreen();
                    });
            };
            const scheduleCamera = () => {
                if (!this.isComposing || !this.cameraBufferCtx || !this.cameraBuffer) return;
                (cameraVideo as HTMLVideoElement & { requestVideoFrameCallback: (cb: () => void) => void })
                    .requestVideoFrameCallback(() => {
                        try {
                            this.cameraBufferCtx!.clearRect(0, 0, this.cameraBuffer!.width, this.cameraBuffer!.height);
                            this.cameraBufferCtx!.drawImage(
                                cameraVideo,
                                0,
                                0,
                                this.cameraBuffer!.width,
                                this.cameraBuffer!.height
                            );
                            this.hasCameraFrame = true;
                        } catch {
                            // ignore
                        }
                        scheduleCamera();
                    });
            };
            scheduleScreen();
            scheduleCamera();
        }

        const loop = () => {
            if (!this.isComposing) return;
            renderOnce();
            this.animationFrame = requestAnimationFrame(loop);
        };
        loop();

        if (this.readyPromise) {
            await this.readyPromise;
        }

        const composedStream = this.canvas.captureStream(30);
        const audioTracks = [...screenStream.getAudioTracks(), ...cameraStream.getAudioTracks()];
        audioTracks.forEach((track) => {
            composedStream.addTrack(track);
        });

        return composedStream;
    }

    updatePipPosition(position: 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right') {
        this.pipPosition = position;
    }

    isComposingWith(screenTrackId?: string | null, cameraTrackId?: string | null) {
        return (
            this.isComposing &&
            this.screenTrackId !== null &&
            this.cameraTrackId !== null &&
            this.screenTrackId === screenTrackId &&
            this.cameraTrackId === cameraTrackId
        );
    }

    stopComposing() {
        this.isComposing = false;
        this.screenTrackId = null;
        this.cameraTrackId = null;
        this.isReady = false;
        this.readyPromise = null;
        this.resolveReady = null;
        if (this.animationFrame) {
            cancelAnimationFrame(this.animationFrame);
            this.animationFrame = null;
        }
        this.screenBuffer = null;
        this.screenBufferCtx = null;
        this.cameraBuffer = null;
        this.cameraBufferCtx = null;
        this.hasScreenFrame = false;
        this.hasCameraFrame = false;
        this.activeVideos.forEach((video) => {
            try {
                video.pause();
            } catch {
                // ignore
            }
            video.srcObject = null;
            video.remove();
        });
        this.activeVideos = [];
    }

    cleanup() {
        this.stopComposing();
        if (this.canvasContainer) {
            this.canvasContainer.remove();
            this.canvasContainer = null;
        }
        this.canvas = null;
        this.ctx = null;
    }
}

export default new CanvasComposerService();
