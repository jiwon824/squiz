export const formatDuration = (totalSeconds: number) => {
    const safeSeconds = Math.max(0, Math.floor(totalSeconds));
    const hours = Math.floor(safeSeconds / 3600);
    const minutes = Math.floor((safeSeconds % 3600) / 60);
    const seconds = safeSeconds % 60;
    const padded = (value: number) => String(value).padStart(2, '0');
    return `${padded(hours)}:${padded(minutes)}:${padded(seconds)}`;
};

export const formatPlannedDuration = (totalSeconds: number) => {
    const safeSeconds = Math.max(0, Math.floor(totalSeconds));
    const hours = Math.floor(safeSeconds / 3600);
    const minutes = Math.floor((safeSeconds % 3600) / 60);
    if (hours <= 0) {
        return `${minutes}분`;
    }
    if (minutes === 0) {
        return `${hours}시간`;
    }
    return `${hours}시간 ${minutes}분`;
};

export const stopTracks = (stream: MediaStream | null) => {
    if (!stream) return;
    stream.getTracks().forEach((track) => track.stop());
};

export const captureFrame = (video: HTMLVideoElement) =>
    new Promise<Blob | null>((resolve) => {
        try {
            if (video.videoWidth === 0 || video.videoHeight === 0) {
                resolve(null);
                return;
            }
            const canvas = document.createElement('canvas');
            canvas.width = video.videoWidth;
            canvas.height = video.videoHeight;
            const ctx = canvas.getContext('2d');
            if (!ctx) {
                resolve(null);
                return;
            }
            ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
            canvas.toBlob((blob) => resolve(blob), 'image/png');
        } catch {
            resolve(null);
        }
    });
