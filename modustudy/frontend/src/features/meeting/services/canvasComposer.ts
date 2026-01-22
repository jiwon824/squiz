class CanvasComposerService {
    private canvas: HTMLCanvasElement | null = null;
    private ctx: CanvasRenderingContext2D | null = null;
    private animationFrame: number | null = null;
    private isComposing = false;
    private composeToken = 0;
    private canvasContainer: HTMLDivElement | null = null;
    private activeVideos: HTMLVideoElement[] = [];

    createCanvas(width = 1920, height = 1080) {
        this.canvas = document.createElement('canvas');
        this.canvas.width = width;
        this.canvas.height = height;
        this.ctx = this.canvas.getContext('2d');
        if (!this.canvasContainer) {
            const container = document.createElement('div');
            container.style.position = 'fixed';
            container.style.left = '-9999px';
            container.style.top = '0';
            container.style.width = '1px';
            container.style.height = '1px';
            container.style.opacity = '0';
            container.style.pointerEvents = 'none';
            container.appendChild(this.canvas);
            document.body.appendChild(container);
            this.canvasContainer = container;
        } else if (!this.canvasContainer.contains(this.canvas)) {
            this.canvasContainer.appendChild(this.canvas);
        }
        return this.canvas;
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
                video.removeEventListener('error', onError);
            };
            if (video.readyState >= 2) {
                resolve();
                return;
            }
            video.addEventListener('loadedmetadata', onReady, { once: true });
            video.addEventListener('canplay', onReady, { once: true });
            video.addEventListener('error', onError, { once: true });
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
        if (this.canvasContainer) {
            this.canvasContainer.appendChild(video);
            this.activeVideos.push(video);
        }
        await this.waitForVideoReady(video);
        await this.ensurePlayback(video);
        await this.waitForVideoDimensions(video, 5000);
        await this.waitForFirstFrame(video, 5000);
        return video;
    }

    private async ensurePlayback(video: HTMLVideoElement, timeoutMs = 2000) {
        try {
            await video.play();
        } catch {
            // Ignore autoplay rejections; we'll retry via polling below.
        }
        if (!video.paused && video.readyState >= 2) {
            return;
        }
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

    private waitForVideoDimensions(video: HTMLVideoElement, timeoutMs = 2000) {
        if (video.videoWidth > 0 && video.videoHeight > 0) {
            return Promise.resolve();
        }
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

    private waitForFirstFrame(video: HTMLVideoElement, timeoutMs = 2000) {
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

        if (token !== this.composeToken) {
            return null;
        }

        const screenSettings = screenStream.getVideoTracks()[0]?.getSettings?.() ?? {};
        const width = screenVideo.videoWidth || screenSettings.width || 1920;
        const height = screenVideo.videoHeight || screenSettings.height || 1080;
        if (!this.canvas) {
            this.createCanvas(width, height);
        } else {
            this.canvas.width = width;
            this.canvas.height = height;
        }

        if (!this.ctx) {
            return null;
        }

        this.isComposing = true;

        const getPipPosition = () => {
            const positions = {
                'top-left': { x: pipPadding, y: pipPadding },
                'top-right': { x: this.canvas!.width - pipWidth - pipPadding, y: pipPadding },
                'bottom-left': { x: pipPadding, y: this.canvas!.height - pipHeight - pipPadding },
                'bottom-right': {
                    x: this.canvas!.width - pipWidth - pipPadding,
                    y: this.canvas!.height - pipHeight - pipPadding,
                },
            };
            return positions[pipPosition] || positions['bottom-right'];
        };

        const render = () => {
            if (!this.isComposing || !this.ctx || !this.canvas) return;

            const pipPos = getPipPosition();

            if (screenVideo.readyState >= 2 && screenVideo.videoWidth > 0) {
                this.ctx.drawImage(screenVideo, 0, 0, this.canvas.width, this.canvas.height);
            }

            this.ctx.strokeStyle = borderColor;
            this.ctx.lineWidth = borderWidth;
            this.ctx.strokeRect(
                pipPos.x - borderWidth,
                pipPos.y - borderWidth,
                pipWidth + borderWidth * 2,
                pipHeight + borderWidth * 2
            );

            if (cameraVideo.readyState >= 2 && cameraVideo.videoWidth > 0) {
                this.ctx.drawImage(cameraVideo, pipPos.x, pipPos.y, pipWidth, pipHeight);
            }

            this.animationFrame = requestAnimationFrame(render);
        };

        render();

        const composedStream = this.canvas.captureStream(30);
        const audioTracks = [...screenStream.getAudioTracks(), ...cameraStream.getAudioTracks()];
        audioTracks.forEach((track) => {
            composedStream.addTrack(track);
        });

        return composedStream;
    }

    stopComposing() {
        this.isComposing = false;
        if (this.animationFrame) {
            cancelAnimationFrame(this.animationFrame);
            this.animationFrame = null;
        }
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
