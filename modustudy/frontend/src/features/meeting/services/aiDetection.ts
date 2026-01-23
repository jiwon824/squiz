import * as cocoSsd from '@tensorflow-models/coco-ssd';
import '@tensorflow/tfjs';

class AIDetectionService {
    private model: cocoSsd.ObjectDetection | null = null;
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
                return null;
            }
            const predictions = await this.model!.detect(videoElement);
            return predictions.some((prediction) => prediction.class === 'person' && prediction.score > 0.5);
        } catch {
            return null;
        }
    }

    startDetection(
        videoElement: HTMLVideoElement | null,
        callback: (isPresent: boolean) => void,
        interval = 2000
    ) {
        const detectionLoop = async () => {
            if (document.hidden) {
                return;
            }
            const isPresent = await this.detectPerson(videoElement);
            if (isPresent === null) {
                return;
            }
            callback(isPresent);
        };
        detectionLoop();
        const intervalId = window.setInterval(detectionLoop, interval);
        return () => clearInterval(intervalId);
    }
}

export default new AIDetectionService();
