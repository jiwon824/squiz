class AudioDetectionService {
    private audioContext: AudioContext | null = null;
    private analyser: AnalyserNode | null = null;
    private microphone: MediaStreamAudioSourceNode | null = null;
    private detectionInterval: number | null = null;

    async startDetection(
        stream: MediaStream,
        callback: (isSpeaking: boolean, average: number) => void,
        threshold = 30
    ) {
        try {
            this.stopDetection();
            this.audioContext = new (window.AudioContext || (window as typeof window & { webkitAudioContext: typeof AudioContext }).webkitAudioContext)();
            this.analyser = this.audioContext.createAnalyser();
            this.analyser.fftSize = 256;

            this.microphone = this.audioContext.createMediaStreamSource(stream);
            this.microphone.connect(this.analyser);

            const bufferLength = this.analyser.frequencyBinCount;
            const dataArray = new Uint8Array(bufferLength);

            this.detectionInterval = window.setInterval(() => {
                this.analyser?.getByteFrequencyData(dataArray);
                const average = dataArray.reduce((sum, value) => sum + value, 0) / bufferLength;
                const isSpeaking = average > threshold;
                callback(isSpeaking, average);
            }, 100);

            return true;
        } catch {
            return false;
        }
    }

    stopDetection() {
        if (this.detectionInterval) {
            clearInterval(this.detectionInterval);
            this.detectionInterval = null;
        }
        if (this.microphone) {
            this.microphone.disconnect();
            this.microphone = null;
        }
        if (this.audioContext) {
            this.audioContext.close();
            this.audioContext = null;
        }
        this.analyser = null;
    }
}

export default new AudioDetectionService();
