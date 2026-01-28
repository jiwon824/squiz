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

export const createSfuClient = (baseUrl: string) => {
    let socket: Socket | null = null;
    let device: any = null;
    let roomId: string | null = null;
    let sendTransport: any = null;
    let recvTransport: any = null;
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
        kind: 'audio' | 'video',
        track: MediaStreamTrack | null,
        appData?: Record<string, unknown>
    ) => {
        console.log('[sfu] produceTrack called', { kind, hasTrack: !!track, trackState: track?.readyState, appData });
        if (!sendTransport || !track) {
            console.warn('[sfu] produceTrack abort: no sendTransport or track');
            return null;
        }
        if (track.readyState === 'ended') {
            console.warn('[sfu] produceTrack abort: track ended');
            return null;
        }
        if (producers.has(kind)) {
            const existing = producers.get(kind);
            if (existing.track && existing.track.id === track.id) {
                console.log('[sfu] produceTrack: same track already producing');
                return existing;
            }
            try {
                await request('closeProducer', { roomId, producerId: existing.id });
                existing.close();
                producers.delete(kind);
            } catch {
                // ignore and recreate
            }
        }
        const producer = await sendTransport.produce({ track, appData });
        console.log('[sfu] produceTrack success', { kind, producerId: producer.id, trackId: track.id });
        producers.set(kind, producer);
        return producer;
    };

    const closeProducer = async (kind: 'audio' | 'video') => {
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
                setTimeout(() => {
                    request('requestKeyFrame', { roomId, consumerId: consumer.id }).catch(() => {});
                }, 300);
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
