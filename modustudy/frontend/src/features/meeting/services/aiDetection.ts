import type { ObjectDetection } from '@tensorflow-models/coco-ssd';

class AIDetectionService {
    private model: ObjectDetection | null = null;
    private isLoading = false;

    async loadModel() {
        if (this.model) return this.model;
        if (this.isLoading) {
            await new Promise<void>((resolve) => {
                const checkInterval = window.setInterval(() => {
                    if (this.model) {
                        clearInterval(checkInterval);
                        resolve();
                    }
                }, 100);
            });
            return this.model;
        }

        try {
            this.isLoading = true;
            // TensorFlow.js를 동적으로 로드 (미팅 진입 시에만 다운로드)
            await import('@tensorflow/tfjs');
            const cocoSsd = await import('@tensorflow-models/coco-ssd');
            this.model = await cocoSsd.load();
            this.isLoading = false;
            return this.model;
        } catch (error) {
            this.isLoading = false;
            throw error;
        }
    }

    async detectPerson(videoElement: HTMLVideoElement | null): Promise<boolean | null> {
        try {
            if (!this.model) {
                await this.loadModel();
            }
            if (!videoElement || videoElement.readyState < 2) {
                console.log('[ai] detect skipped: video not ready', {
                    hasVideo: Boolean(videoElement),
                    readyState: videoElement?.readyState,
                    width: videoElement?.videoWidth,
                    height: videoElement?.videoHeight,
                });
                return null;
            }
            const predictions = await this.model!.detect(videoElement);
            return predictions.some((prediction) => prediction.class === 'person' && prediction.score > 0.5);
        } catch (error) {
            console.warn('[ai] detect error', error);
            return null;
        }
    }

    startDetection(
        videoElement: HTMLVideoElement | null,
        callback: (isPresent: boolean) => void,
        interval = 2000
    ) {
        console.log('[ai] startDetection', {
            interval,
            hasVideo: Boolean(videoElement),
        });
        const detectionLoop = async () => {
            const isPresent = await this.detectPerson(videoElement);
            if (isPresent === null) {
                return;
            }
            console.log('[ai] detect result', { isPresent });
            callback(isPresent);
        };
        detectionLoop();
        const intervalId = window.setInterval(detectionLoop, interval);
        return () => {
            console.log('[ai] stopDetection');
            clearInterval(intervalId);
        };
    }
}

export default new AIDetectionService();
