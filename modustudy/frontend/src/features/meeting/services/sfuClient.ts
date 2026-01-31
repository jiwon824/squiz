import { io, Socket } from 'socket.io-client';
import * as mediasoupClient from 'mediasoup-client';

export interface SfuConsumerPayload {
    consumerId: string;
    producerId: string;
    stream: MediaStream;
    kind: 'audio' | 'video';
    peerId?: string;
}

interface SfuCallbacks {
    onNewConsumer?: (payload: SfuConsumerPayload) => void;
    onPeerLeft?: (peerId: string) => void;
    onProducerClosed?: (payload: { producerId: string; peerId: string }) => void;
}

export type ProducerKind = 'audio' | 'video' | 'screen-audio';

export const createSfuClient = (baseUrl: string) => {
    let socket: Socket | null = null;
    let device: any = null;
    let roomId: string | null = null;
    let sendTransport: any = null;
    let recvTransport: any = null;
    let creatingSendTransport: Promise<any> | null = null;
    const producers = new Map<string, any>();
    const consumers = new Map<string, any>();

    const request = (event: string, data: Record<string, unknown> = {}) =>
        new Promise<any>((resolve, reject) => {
            if (!socket) {
                reject(new Error('Socket not ready'));
                return;
            }
            socket.emit(event, data, (response: any) => {
                if (response && response.error) {
                    reject(new Error(response.error));
                } else {
                    resolve(response);
                }
            });
        });

    const connect = async ({
        targetRoomId,
        displayName,
        onNewConsumer,
        onPeerLeft,
        onProducerClosed,
    }: { targetRoomId: string; displayName: string } & SfuCallbacks) => {
        roomId = targetRoomId;
        socket = io(baseUrl, { transports: ['websocket'] });

        socket.on('newProducer', async ({ producerId, producerPeerId, kind }) => {
            console.log('[sfu] newProducer event', { producerId, producerPeerId, kind });
            const consumerData = await consume(producerId);
            if (consumerData && onNewConsumer) {
                onNewConsumer({ ...consumerData, peerId: producerPeerId, kind });
            }
        });

        socket.on('peerLeft', ({ peerId }) => {
            if (onPeerLeft) {
                onPeerLeft(peerId);
            }
        });

        socket.on('producerClosed', ({ producerId, peerId }) => {
            if (onProducerClosed) {
                onProducerClosed({ producerId, peerId });
            }
        });

        await new Promise<void>((resolve) => {
            socket?.on('connect', () => resolve());
        });

        const joinData = await request('joinRoom', { roomId, displayName });
        device = new (mediasoupClient as any).Device();
        await device.load({ routerRtpCapabilities: joinData.rtpCapabilities });

        sendTransport = await createSendTransport();
        recvTransport = await createRecvTransport();

        console.log('[sfu] existingProducers', joinData.existingProducers);
        if (joinData.existingProducers) {
            const producers = joinData.existingProducers as Array<{ producerId: string; peerId: string; kind: string }>;
            // 기존 producer를 병렬로 consume하여 초기 로딩 시간 단축
            const results = await Promise.allSettled(
                producers.map(async (info) => {
                    const consumerData = await consume(info.producerId);
                    if (consumerData && onNewConsumer) {
                        onNewConsumer({ ...consumerData, peerId: info.peerId, kind: info.kind as 'audio' | 'video' });
                    }
                })
            );
            const failed = results.filter((r) => r.status === 'rejected');
            if (failed.length > 0) {
                console.warn('[sfu] some consumers failed', failed.length, '/', producers.length);
            }
        }
    };

    const createSendTransport = async () => {
        const { params } = await request('createWebRtcTransport', { roomId });
        console.log('[sfu] send transport params', { iceServers: params.iceServers, iceCandidates: params.iceCandidates?.length });
        const transport = device.createSendTransport(params);
        transport.on('connect', ({ dtlsParameters }, callback, errback) => {
            request('connectWebRtcTransport', { roomId, transportId: transport.id, dtlsParameters })
                .then(() => callback())
                .catch(errback);
        });
        transport.on('connectionstatechange', (state) => {
            console.log('[sfu] send transport state', state);
        });
        transport.on('produce', ({ kind, rtpParameters, appData }, callback, errback) => {
            request('produce', { roomId, transportId: transport.id, kind, rtpParameters, appData })
                .then(({ producerId }) => callback({ id: producerId }))
                .catch(errback);
        });
        return transport;
    };

    const ensureSendTransport = async () => {
        if (sendTransport && !sendTransport.closed) {
            return sendTransport;
        }
        if (creatingSendTransport) {
            return creatingSendTransport;
        }
        creatingSendTransport = (async () => {
            sendTransport = await createSendTransport();
            producers.clear();
            return sendTransport;
        })();
        try {
            return await creatingSendTransport;
        } finally {
            creatingSendTransport = null;
        }
    };

    const createRecvTransport = async () => {
        const { params } = await request('createWebRtcTransport', { roomId });
        console.log('[sfu] recv transport params', { iceServers: params.iceServers, iceCandidates: params.iceCandidates?.length });
        const transport = device.createRecvTransport(params);
        transport.on('connect', ({ dtlsParameters }, callback, errback) => {
            request('connectWebRtcTransport', { roomId, transportId: transport.id, dtlsParameters })
                .then(() => callback())
                .catch(errback);
        });
        transport.on('connectionstatechange', (state) => {
            console.log('[sfu] recv transport state', state);
        });
        return transport;
    };

    const produceTrack = async (
        kind: ProducerKind,
        track: MediaStreamTrack | null,
        appData?: Record<string, unknown>
    ) => {
        // screen-audio는 mediasoup에서 'audio' 타입으로 전송
        const mediasoupKind = kind === 'screen-audio' ? 'audio' : kind;
        console.log('[sfu] produceTrack called', {
            kind,
            mediasoupKind,
            hasTrack: !!track,
            trackId: track?.id,
            trackReadyState: track?.readyState,
            trackEnabled: track?.enabled,
            trackMuted: track?.muted,
            hasExistingProducer: producers.has(kind),
        });
        if (!track) {
            console.log('[sfu] produceTrack: no track provided');
            return null;
        }
        if (track.readyState === 'ended') {
            console.log('[sfu] produceTrack: track already ended');
            return null;
        }
        const transport = await ensureSendTransport();
        if (!transport) {
            console.log('[sfu] produceTrack: no transport');
            return null;
        }
        if (producers.has(kind)) {
            const existing = producers.get(kind);
            if (existing.track && existing.track.id === track.id) {
                console.log('[sfu] produceTrack: same track already producing');
                return existing;
            }
            console.log('[sfu] produceTrack: replacing existing producer', { existingTrackId: existing.track?.id, newTrackId: track.id });
            try {
                if (typeof existing.replaceTrack === 'function') {
                    await existing.replaceTrack({ track });
                    console.log('[sfu] produceTrack: track replaced successfully');
                    return existing;
                }
            } catch (err) {
                console.warn('[sfu] produceTrack: replaceTrack failed, will recreate', err);
                // fallback to recreate
            }
            try {
                await request('closeProducer', { roomId, producerId: existing.id });
                existing.close();
                producers.delete(kind);
            } catch {
                // ignore and recreate
            }
        }
        // 비디오일 경우 Simulcast 인코딩 설정
        let producer;
        if (mediasoupKind === 'video') {
            const isScreenShare = appData?.source === 'screen' || appData?.source === 'mixed';
            // 화면 공유: 고품질 단일 레이어 / 카메라: 낮은 프레임레이트 + Simulcast 3레이어
            const encodings = isScreenShare
                ? [{ maxBitrate: 2500000 }]
                : [
                    { maxBitrate: 500000, scaleResolutionDownBy: 2, maxFramerate: 24 },
                    { maxBitrate: 1500000, maxFramerate: 30 },
                ];
            const codecOptions = isScreenShare
                ? { videoGoogleStartBitrate: 1500 }
                : { videoGoogleStartBitrate: 800 };
            producer = await transport.produce({
                track,
                appData,
                encodings,
                codecOptions,
            });
        } else {
            console.log('[sfu] produceTrack: creating audio producer');
            producer = await transport.produce({ track, appData });
        }
        producers.set(kind, producer);
        console.log('[sfu] produceTrack: producer created successfully', {
            kind,
            producerId: producer.id,
            producerPaused: producer.paused,
            producerClosed: producer.closed,
        });
        return producer;
    };

    const closeProducer = async (kind: ProducerKind) => {
        const producer = producers.get(kind);
        if (producer) {
            await request('closeProducer', { roomId, producerId: producer.id });
            producer.close();
            producers.delete(kind);
        }
    };

    const consume = async (producerId: string) => {
        console.log('[sfu] consume called', { producerId, hasRecvTransport: !!recvTransport, hasDevice: !!device });
        if (!recvTransport || !device || !device.rtpCapabilities) return null;
        try {
            const { params } = await request('consume', {
                roomId,
                consumerTransportId: recvTransport.id,
                producerId,
                rtpCapabilities: device.rtpCapabilities,
            });
            console.log('[sfu] consume server response', { id: params.id, kind: params.kind, producerId: params.producerId });
            const consumer = await recvTransport.consume({
                id: params.id,
                producerId: params.producerId,
                kind: params.kind,
                rtpParameters: params.rtpParameters,
            });
            consumers.set(consumer.id, consumer);
            console.log('[sfu] consumer created', { consumerId: consumer.id, kind: consumer.kind, trackState: consumer.track?.readyState, trackEnabled: consumer.track?.enabled });
            await request('resume', { roomId, consumerId: consumer.id });
            try {
                await consumer.resume();
            } catch {
                // ignore resume errors when transport state changes quickly
            }
            if (consumer.kind === 'video') {
                // 초기 keyframe 요청
                setTimeout(() => {
                    request('requestKeyFrame', { roomId, consumerId: consumer.id }).catch(() => {});
                }, 300);
                // 주기적 keyframe 요청 (화면 공유 멈춤 방지)
                const keyFrameInterval = setInterval(() => {
                    if (consumer.closed) {
                        clearInterval(keyFrameInterval);
                        return;
                    }
                    request('requestKeyFrame', { roomId, consumerId: consumer.id }).catch(() => {});
                }, 3000);
                consumer.on('close', () => clearInterval(keyFrameInterval));
            }
            const stream = new MediaStream([consumer.track]);
            console.log('[sfu] consume success', { consumerId: consumer.id, kind: consumer.kind, streamActive: stream.active, trackCount: stream.getTracks().length });
            return { consumerId: consumer.id, producerId, stream, kind: consumer.kind as 'audio' | 'video' };
        } catch (err) {
            console.error('[sfu] consume failed', { producerId, error: (err as Error).message });
            return null;
        }
    };

    const close = () => {
        producers.forEach((producer) => producer.close());
        consumers.forEach((consumer) => consumer.close());
        if (sendTransport) sendTransport.close();
        if (recvTransport) recvTransport.close();
        if (socket) socket.disconnect();
        producers.clear();
        consumers.clear();
    };

    return {
        connect,
        produceTrack,
        closeProducer,
        close,
    };
};
