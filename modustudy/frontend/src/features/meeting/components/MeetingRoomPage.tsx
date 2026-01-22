import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { MainLayout } from '@/layouts/MainLayout';
import { useAuthStore } from '@/store/authStore';
import MeetingControls from './MeetingControls';
import MeetingParticipants from './MeetingParticipants';
import MeetingChatPanel from './MeetingChatPanel';
import MeetingVideoStage from './MeetingVideoStage';
import { meetingApi } from '../services/meetingApi';
import { createMeetingWebsocket } from '../services/meetingWebsocket';
import { createSfuClient, SfuConsumerPayload } from '../services/sfuClient';
import audioDetection from '../services/audioDetection';
import aiDetection from '../services/aiDetection';
import canvasComposer from '../services/canvasComposer';
import {
    MeetingJoinResponse,
    MeetingRoomChatMessage,
    MeetingRoomEvent,
    MeetingRoomParticipant,
} from '../types';
import '../styles/MeetingRoom.css';
import '../styles/MeetingShared.css';

type PipPosition = 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right';

interface RemoteVideoStream {
    id: string;
    stream: MediaStream;
    label: string;
    peerId?: string;
    producerId?: string;
}

const MeetingRoomPage: React.FC = () => {
    const { studyId, meetingId } = useParams();
    const numericStudyId = Number(studyId);
    const numericMeetingId = Number(meetingId);
    const navigate = useNavigate();
    const { user, isLoggedIn } = useAuthStore();

    const getDisplayName = useCallback(() => {
        if (!isLoggedIn) return '게스트';
        return user?.nickname || user?.name || '게스트';
    }, [isLoggedIn, user?.nickname, user?.name]);

    const displayNameRef = useRef(getDisplayName());
    const selfParticipantIdRef = useRef<number | null>(null);
    const wsClientRef = useRef<ReturnType<typeof createMeetingWebsocket> | null>(null);
    const sfuClientRef = useRef<ReturnType<typeof createSfuClient> | null>(null);
    const wsUnsubscribeRef = useRef<(() => void)[]>([]);
    const joinSuccessRef = useRef(false);
    const updateTokenRef = useRef(0);
    const roomIdRef = useRef<string>('');

    const localMicStreamRef = useRef<MediaStream | null>(null);
    const localCameraStreamRef = useRef<MediaStream | null>(null);
    const localScreenStreamRef = useRef<MediaStream | null>(null);
    const composedStreamRef = useRef<MediaStream | null>(null);
    const audioDetectionActiveRef = useRef(false);
    const aiDetectionCleanupRef = useRef<(() => void) | null>(null);
    const chatDedupRef = useRef<Set<string>>(new Set());
    const remoteAudioElementsRef = useRef<Map<string, HTMLAudioElement>>(new Map());

    const [meetingTitle, setMeetingTitle] = useState('');
    const [participants, setParticipants] = useState<MeetingRoomParticipant[]>([]);
    const [chatMessages, setChatMessages] = useState<MeetingRoomChatMessage[]>([]);
    const [presenterName, setPresenterName] = useState<string | null>(null);
    const [presenterId, setPresenterId] = useState<number | null>(null);
    const [micEnabled, setMicEnabled] = useState(true);
    const [cameraEnabled, setCameraEnabled] = useState(false);
    const [screenSharing, setScreenSharing] = useState(false);
    const [pipPosition, setPipPosition] = useState<PipPosition>('bottom-right');
    const [localStream, setLocalStream] = useState<MediaStream | null>(null);
    const [remoteVideoStreams, setRemoteVideoStreams] = useState<RemoteVideoStream[]>([]);
    const [isRecording, setIsRecording] = useState(false);

    const aiVideoRef = useRef<HTMLVideoElement | null>(null);
    const ownerKey = user?.id ?? user?.nickname ?? user?.name ?? 'guest';
    const micEnabledRef = useRef(micEnabled);
    const speakingRef = useRef(false);
    const recordingRef = useRef(false);
    const prevPresenterRef = useRef(false);

    useEffect(() => {
        displayNameRef.current = getDisplayName();
    }, [getDisplayName]);

    useEffect(() => {
        micEnabledRef.current = micEnabled;
    }, [micEnabled]);

    const canEndMeeting = useMemo(() => {
        if (!numericMeetingId) return false;
        return localStorage.getItem(`meeting-owner-${numericMeetingId}`) === String(ownerKey);
    }, [numericMeetingId, ownerKey]);

    const isPresenter = useMemo(() => {
        const selfName = displayNameRef.current;
        return Boolean(
            (presenterName && presenterName === selfName) ||
                (presenterId !== null && presenterId === selfParticipantIdRef.current)
        );
    }, [presenterName, presenterId]);

    const stopTracks = (stream: MediaStream | null) => {
        if (!stream) return;
        stream.getTracks().forEach((track) => track.stop());
    };

    const updateSelfParticipant = useCallback(
        (updates: Partial<MeetingRoomParticipant>) => {
            const displayName = displayNameRef.current;
            setParticipants((prev) => {
                const next = new Map(prev.map((participant) => [participant.displayName, participant]));
                const existing = next.get(displayName);
                const merged: MeetingRoomParticipant = {
                    id: existing?.id ?? selfParticipantIdRef.current ?? -1,
                    displayName,
                    active: existing?.active ?? true,
                    ...existing,
                    ...updates,
                };
                next.set(displayName, merged);
                return Array.from(next.values());
            });
        },
        [setParticipants]
    );

    const mergeParticipants = useCallback((incoming: MeetingRoomParticipant[]) => {
        setParticipants((prev) => {
            const next = new Map(prev.map((participant) => [participant.displayName, participant]));
            incoming.forEach((participant) => {
                const existing = next.get(participant.displayName);
                const merged: MeetingRoomParticipant = {
                    ...existing,
                    ...participant,
                };
                if (
                    participant.displayName === displayNameRef.current &&
                    selfParticipantIdRef.current === null &&
                    participant.id
                ) {
                    selfParticipantIdRef.current = participant.id;
                }
                next.set(participant.displayName, merged);
            });
            return Array.from(next.values());
        });
    }, []);

    const appendChatMessage = useCallback((message: MeetingRoomChatMessage) => {
        const key = `${message.sentAt}-${message.sender}-${message.text}`;
        if (chatDedupRef.current.has(key)) {
            return;
        }
        chatDedupRef.current.add(key);
        setChatMessages((prev) => [...prev, message]);
    }, []);

    const updateOutgoingVideo = useCallback(
        async (options?: {
            nextCameraStream?: MediaStream | null;
            nextScreenStream?: MediaStream | null;
            nextPipPosition?: PipPosition;
        }) => {
            const token = ++updateTokenRef.current;
            const cameraStream =
                options?.nextCameraStream !== undefined ? options.nextCameraStream : localCameraStreamRef.current;
            const screenStream =
                options?.nextScreenStream !== undefined ? options.nextScreenStream : localScreenStreamRef.current;
            const nextPosition = options?.nextPipPosition ?? pipPosition;
            let nextStream: MediaStream | null = null;

            try {
                if (
                    cameraStream &&
                    screenStream &&
                    cameraStream.getVideoTracks().length > 0 &&
                    screenStream.getVideoTracks().length > 0
                ) {
                    const composed = await canvasComposer.composeStreams(screenStream, cameraStream, {
                        pipPosition: nextPosition,
                    });
                    if (token !== updateTokenRef.current) {
                        if (composed) stopTracks(composed);
                        return;
                    }
                    if (composed) {
                        if (composedStreamRef.current && composedStreamRef.current !== composed) {
                            stopTracks(composedStreamRef.current);
                        }
                        composedStreamRef.current = composed;
                        nextStream = composed;
                    }
                }

                if (!nextStream) {
                    canvasComposer.stopComposing();
                    if (composedStreamRef.current) {
                        stopTracks(composedStreamRef.current);
                        composedStreamRef.current = null;
                    }
                    if (screenStream && screenStream.getVideoTracks().length > 0) {
                        nextStream = screenStream;
                    } else if (cameraStream && cameraStream.getVideoTracks().length > 0) {
                        nextStream = cameraStream;
                    }
                }
            } catch (error) {
                console.error('Failed to update composed stream', error);
                canvasComposer.stopComposing();
                if (screenStream && screenStream.getVideoTracks().length > 0) {
                    nextStream = screenStream;
                } else if (cameraStream && cameraStream.getVideoTracks().length > 0) {
                    nextStream = cameraStream;
                }
            }

            if (token !== updateTokenRef.current) return;
            setLocalStream(nextStream);

            const videoTrack = nextStream?.getVideoTracks()[0] ?? null;
            if (sfuClientRef.current) {
                if (videoTrack) {
                    await sfuClientRef.current.produceTrack('video', videoTrack);
                } else {
                    await sfuClientRef.current.closeProducer('video');
                }
            }
        },
        [pipPosition]
    );

    const startMicrophone = useCallback(async () => {
        if (!navigator.mediaDevices?.getUserMedia) {
            setMicEnabled(false);
            return;
        }
        if (localMicStreamRef.current) {
            setMicEnabled(true);
            return;
        }
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
            localMicStreamRef.current = stream;
            setMicEnabled(true);
            if (sfuClientRef.current) {
                await sfuClientRef.current.produceTrack('audio', stream.getAudioTracks()[0] ?? null);
            }
            audioDetectionActiveRef.current = Boolean(
                await audioDetection.startDetection(stream, (isSpeaking) => {
                    if (speakingRef.current === isSpeaking) return;
                    speakingRef.current = isSpeaking;
                    updateSelfParticipant({ isSpeaking });
                    if (wsClientRef.current && roomIdRef.current) {
                        wsClientRef.current.setSpeaking(roomIdRef.current, { speaking: isSpeaking });
                    }
                })
            );
        } catch (error) {
            console.error('Failed to access microphone', error);
            setMicEnabled(false);
        }
    }, [updateSelfParticipant]);

    const stopMicrophone = useCallback(async () => {
        audioDetection.stopDetection();
        audioDetectionActiveRef.current = false;
        speakingRef.current = false;
        updateSelfParticipant({ isSpeaking: false });
        if (wsClientRef.current && roomIdRef.current) {
            wsClientRef.current.setSpeaking(roomIdRef.current, { speaking: false });
        }
        stopTracks(localMicStreamRef.current);
        localMicStreamRef.current = null;
        setMicEnabled(false);
        if (sfuClientRef.current) {
            await sfuClientRef.current.closeProducer('audio');
        }
    }, [updateSelfParticipant]);

    const startCamera = useCallback(async () => {
        if (!navigator.mediaDevices?.getUserMedia) {
            setCameraEnabled(false);
            return;
        }
        if (localCameraStreamRef.current) {
            setCameraEnabled(true);
            return;
        }
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
            localCameraStreamRef.current = stream;
            setCameraEnabled(true);
            await updateOutgoingVideo({ nextCameraStream: stream });
            if (aiVideoRef.current) {
                aiVideoRef.current.srcObject = stream;
                aiVideoRef.current.play().catch(() => {});
                if (aiDetectionCleanupRef.current) {
                    aiDetectionCleanupRef.current();
                }
                aiDetectionCleanupRef.current = aiDetection.startDetection(aiVideoRef.current, (isPresent) => {
                    updateSelfParticipant({ isPresent });
                });
            }
        } catch (error) {
            console.error('Failed to access camera', error);
            setCameraEnabled(false);
        }
    }, [updateOutgoingVideo, updateSelfParticipant]);

    const stopCamera = useCallback(async () => {
        if (aiDetectionCleanupRef.current) {
            aiDetectionCleanupRef.current();
            aiDetectionCleanupRef.current = null;
        }
        updateSelfParticipant({ isPresent: false });
        stopTracks(localCameraStreamRef.current);
        localCameraStreamRef.current = null;
        setCameraEnabled(false);
        await updateOutgoingVideo({ nextCameraStream: null });
    }, [updateOutgoingVideo, updateSelfParticipant]);

    const startScreenShare = useCallback(async () => {
        if (localScreenStreamRef.current) {
            setScreenSharing(true);
            return;
        }
        try {
            const stream = await navigator.mediaDevices.getDisplayMedia({ video: true, audio: false });
            localScreenStreamRef.current = stream;
            setScreenSharing(true);
            const [track] = stream.getVideoTracks();
            if (track) {
                track.onended = () => {
                    setScreenSharing(false);
                    localScreenStreamRef.current = null;
                    updateOutgoingVideo({ nextScreenStream: null });
                };
            }
            await updateOutgoingVideo({ nextScreenStream: stream });
        } catch (error) {
            console.error('Failed to start screen share', error);
            setScreenSharing(false);
        }
    }, [updateOutgoingVideo]);

    const stopScreenShare = useCallback(async () => {
        stopTracks(localScreenStreamRef.current);
        localScreenStreamRef.current = null;
        setScreenSharing(false);
        await updateOutgoingVideo({ nextScreenStream: null });
    }, [updateOutgoingVideo]);

    const handleToggleMic = useCallback(() => {
        if (micEnabled) {
            void stopMicrophone();
        } else {
            void startMicrophone();
        }
    }, [micEnabled, startMicrophone, stopMicrophone]);

    const handleToggleCamera = useCallback(() => {
        if (!isPresenter) return;
        if (cameraEnabled) {
            void stopCamera();
        } else {
            void startCamera();
        }
    }, [cameraEnabled, isPresenter, startCamera, stopCamera]);

    const handleToggleScreenShare = useCallback(() => {
        if (!isPresenter) return;
        if (screenSharing) {
            void stopScreenShare();
        } else {
            void startScreenShare();
        }
    }, [isPresenter, screenSharing, startScreenShare, stopScreenShare]);

    const handlePipPositionChange = useCallback(
        (position: PipPosition) => {
            setPipPosition(position);
            updateOutgoingVideo({ nextPipPosition: position });
        },
        [updateOutgoingVideo]
    );

    const handleTogglePresenter = useCallback(() => {
        if (!roomIdRef.current || !wsClientRef.current) return;
        if (isPresenter) {
            wsClientRef.current.setPresenter(roomIdRef.current, {
                displayName: displayNameRef.current,
                action: 'release',
            });
            setPresenterName(null);
            setPresenterId(null);
            return;
        }
        const confirmed = window.confirm('발표자가 되겠습니까? 현재 발표자는 권한이 내려집니다.');
        if (!confirmed) return;
        wsClientRef.current.setPresenter(roomIdRef.current, {
            displayName: displayNameRef.current,
            action: 'claim',
        });
        setPresenterName(displayNameRef.current);
        if (selfParticipantIdRef.current !== null) {
            setPresenterId(selfParticipantIdRef.current);
        }
    }, [isPresenter]);

    useEffect(() => {
        const wasPresenter = prevPresenterRef.current;
        prevPresenterRef.current = isPresenter;
        if (wasPresenter && !isPresenter) {
            void stopScreenShare();
            void stopCamera();
        }
    }, [isPresenter, stopCamera, stopScreenShare]);

    const handleSendChat = useCallback((text: string) => {
        if (!roomIdRef.current || !wsClientRef.current) return;
        const payload: MeetingRoomChatMessage = {
            sender: displayNameRef.current,
            text,
            sentAt: new Date().toISOString(),
            userId: selfParticipantIdRef.current ?? null,
        };
        wsClientRef.current.sendChat(roomIdRef.current, payload);
    }, []);

    const handleEndMeeting = useCallback(async () => {
        if (!numericStudyId || !numericMeetingId || !canEndMeeting) return;
        const confirmed = window.confirm('미팅을 종료하시겠습니까?');
        if (!confirmed) return;
        try {
            await meetingApi.endMeeting(numericStudyId, numericMeetingId);
        } catch (error) {
            console.error('Failed to end meeting', error);
        } finally {
            navigate(`/study/${numericStudyId}/meetings/${numericMeetingId}`);
        }
    }, [numericStudyId, numericMeetingId, canEndMeeting, navigate]);

    const handleRoomEvent = useCallback(
        (event: MeetingRoomEvent) => {
            if (event.type === 'MEETING_ENDED') {
                navigate(`/study/${numericStudyId}/meetings/${numericMeetingId}`);
                return;
            }
            if (event.participants && event.participants.length > 0) {
                mergeParticipants(event.participants);
            }
            if (event.participant) {
                mergeParticipants([event.participant]);
            }
            if (event.type === 'LEAVE' && event.participant) {
                setParticipants((prev) =>
                    prev.map((participant) =>
                        participant.displayName === event.participant?.displayName
                            ? { ...participant, active: false }
                            : participant
                    )
                );
            }
            if (event.type === 'CHAT' && event.chat) {
                appendChatMessage(event.chat);
            }
            if (event.type === 'CHAT_HISTORY' && event.chatHistory) {
                event.chatHistory.forEach((message) => appendChatMessage(message));
            }
            if (event.type === 'PRESENTER') {
                setPresenterName(event.presenterName || null);
                setPresenterId(event.presenterId ?? null);
            } else if (event.type === 'JOIN') {
                if (event.presenterName) {
                    setPresenterName(event.presenterName);
                }
                if (event.presenterId !== undefined && event.presenterId !== null) {
                    setPresenterId(event.presenterId);
                }
            }
        },
        [appendChatMessage, mergeParticipants, navigate, numericStudyId, numericMeetingId]
    );

    const handleNewConsumer = useCallback(
        (payload: SfuConsumerPayload) => {
            if (payload.kind === 'audio') {
                const audio = new Audio();
                audio.srcObject = payload.stream;
                audio.autoplay = true;
                audio.play().catch(() => {});
                remoteAudioElementsRef.current.set(payload.producerId, audio);
                return;
            }
            setRemoteVideoStreams((prev) => {
                if (prev.some((item) => item.id === payload.consumerId)) {
                    return prev;
                }
                const label = payload.peerId ? `참가자 (${payload.peerId.slice(0, 6)})` : '참가자';
                return [
                    ...prev,
                    {
                        id: payload.consumerId,
                        stream: payload.stream,
                        label,
                        peerId: payload.peerId,
                        producerId: payload.producerId,
                    },
                ];
            });
        },
        [setRemoteVideoStreams]
    );

    const handlePeerLeft = useCallback((peerId: string) => {
        setRemoteVideoStreams((prev) => prev.filter((item) => item.peerId !== peerId));
    }, []);

    const handleProducerClosed = useCallback((payload: { producerId: string; peerId: string }) => {
        setRemoteVideoStreams((prev) => prev.filter((item) => item.producerId !== payload.producerId));
        const audio = remoteAudioElementsRef.current.get(payload.producerId);
        if (audio) {
            try {
                audio.pause();
            } catch {
                // ignore
            }
            audio.srcObject = null;
            remoteAudioElementsRef.current.delete(payload.producerId);
        }
    }, []);

    useEffect(() => {
        if (!numericStudyId || !numericMeetingId) return;
        let cancelled = false;
        const setup = async () => {
            try {
                const detail = await meetingApi.getMeetingDetail(numericStudyId, numericMeetingId);
                if (!cancelled) {
                    setMeetingTitle(detail.title || `미팅 ${numericMeetingId}`);
                }
            } catch {
                if (!cancelled) {
                    setMeetingTitle(`미팅 ${numericMeetingId}`);
                }
            }

            let joinData: MeetingJoinResponse | null = null;
            const fallbackRoomId = `meeting-${numericMeetingId}`;
            try {
                joinData = await meetingApi.joinMeeting(numericStudyId, numericMeetingId);
                joinSuccessRef.current = true;
            } catch (error) {
                console.error('Failed to join meeting', error);
                joinSuccessRef.current = false;
            }

            const roomId = joinData?.roomToken || fallbackRoomId;
            roomIdRef.current = roomId;

            setParticipants([
                {
                    id: selfParticipantIdRef.current ?? -1,
                    displayName: displayNameRef.current,
                    active: true,
                    isPresent: false,
                },
            ]);

            const wsBaseUrl = import.meta.env.VITE_API_URL || undefined;
            const wsClient = createMeetingWebsocket(wsBaseUrl);
            wsClientRef.current = wsClient;
            try {
                await wsClient.connect();
                if (cancelled) return;
                wsUnsubscribeRef.current.push(wsClient.subscribeRoomEvents(roomId, handleRoomEvent));
                wsUnsubscribeRef.current.push(wsClient.subscribeChatHistory(roomId, handleRoomEvent));
                wsClient.joinRoom(roomId, { displayName: displayNameRef.current });
            } catch (error) {
                console.error('Failed to connect websocket', error);
            }

            let sfuBaseUrl = import.meta.env.VITE_SFU_URL || 'http://localhost:4000';
            try {
                const config = await meetingApi.getSfuConfig();
                if (config?.baseUrl) {
                    sfuBaseUrl = config.baseUrl;
                }
            } catch {
                // use fallback
            }

            const sfuClient = createSfuClient(sfuBaseUrl);
            sfuClientRef.current = sfuClient;
            try {
                await sfuClient.connect({
                    targetRoomId: roomId,
                    displayName: displayNameRef.current,
                    onNewConsumer: handleNewConsumer,
                    onPeerLeft: handlePeerLeft,
                    onProducerClosed: handleProducerClosed,
                });
                if (!cancelled && micEnabledRef.current) {
                    await startMicrophone();
                }
            } catch (error) {
                console.error('Failed to connect SFU', error);
            }
        };
        setup();

        return () => {
            cancelled = true;
            wsUnsubscribeRef.current.forEach((unsubscribe) => unsubscribe());
            wsUnsubscribeRef.current = [];
            if (wsClientRef.current) {
                wsClientRef.current.disconnect();
            }
            if (sfuClientRef.current) {
                sfuClientRef.current.close();
            }
            if (aiDetectionCleanupRef.current) {
                aiDetectionCleanupRef.current();
                aiDetectionCleanupRef.current = null;
            }
            audioDetection.stopDetection();
            stopTracks(localMicStreamRef.current);
            stopTracks(localCameraStreamRef.current);
            stopTracks(localScreenStreamRef.current);
            remoteAudioElementsRef.current.forEach((audio) => {
                try {
                    audio.pause();
                } catch {
                    // ignore
                }
                audio.srcObject = null;
            });
            remoteAudioElementsRef.current.clear();
            canvasComposer.cleanup();
            if (joinSuccessRef.current) {
                meetingApi.leaveMeeting(numericStudyId, numericMeetingId).catch((error) => {
                    console.error('Failed to leave meeting', error);
                });
            }
        };
    }, [
        numericStudyId,
        numericMeetingId,
        handleNewConsumer,
        handlePeerLeft,
        handleProducerClosed,
        handleRoomEvent,
        startMicrophone,
    ]);

    useEffect(() => {
        if (!cameraEnabled) {
            updateSelfParticipant({ isPresent: false });
        }
    }, [cameraEnabled, updateSelfParticipant]);

    useEffect(() => {
        if (!micEnabled && audioDetectionActiveRef.current) {
            updateSelfParticipant({ isSpeaking: false });
        }
    }, [micEnabled, updateSelfParticipant]);

    useEffect(() => {
        if (!localStream || recordingRef.current) return;
        const audioTracks = localMicStreamRef.current?.getAudioTracks() ?? [];
        const videoTracks = localStream.getVideoTracks();
        const combined = new MediaStream([...audioTracks, ...videoTracks]);
        if (combined.getTracks().length === 0) return;
        let recorder: MediaRecorder | null = null;
        let stopped = false;
        const tryStart = () => {
            const mimeTypes = ['video/webm;codecs=vp9,opus', 'video/webm;codecs=vp8,opus', 'video/webm'];
            const supported = mimeTypes.find((type) => MediaRecorder.isTypeSupported(type));
            try {
                recorder = new MediaRecorder(combined, supported ? { mimeType: supported } : undefined);
                recorder.start();
                recordingRef.current = true;
                setIsRecording(true);
            } catch (error) {
                console.error('Failed to start MediaRecorder', error);
            }
        };
        tryStart();

        return () => {
            if (stopped) return;
            stopped = true;
            if (recorder && recorder.state !== 'inactive') {
                recorder.stop();
            }
            recordingRef.current = false;
            setIsRecording(false);
        };
    }, [localStream]);

    return (
        <MainLayout>
            <div className="meeting-room">
                <div className="meeting-room__header">
                    <div>
                        <h1>{meetingTitle || '미팅 룸'}</h1>
                        <div className="meeting-room__status">
                            <span>{isPresenter ? '발표자 모드' : '참가자 모드'}</span>
                            {isRecording && <span className="meeting-room__recording">녹음 중</span>}
                        </div>
                    </div>
                    <button className="meeting-btn ghost" onClick={() => navigate(`/study/${numericStudyId}/meetings`)}>
                        목록으로
                    </button>
                </div>

                <MeetingControls
                    isPresenter={isPresenter}
                    micEnabled={micEnabled}
                    cameraEnabled={cameraEnabled}
                    screenSharing={screenSharing}
                    pipPosition={pipPosition}
                    onToggleMic={handleToggleMic}
                    onToggleCamera={handleToggleCamera}
                    onToggleScreenShare={handleToggleScreenShare}
                    onTogglePresenter={handleTogglePresenter}
                    onPipPositionChange={handlePipPositionChange}
                    onEndMeeting={handleEndMeeting}
                    canEndMeeting={canEndMeeting}
                />

                <div className="meeting-room__content">
                    <div className="meeting-room__stage">
                        <MeetingVideoStage
                            localStream={localStream}
                            localLabel={displayNameRef.current}
                            localIsPresenter={isPresenter}
                            remoteVideoStreams={remoteVideoStreams.map((item) => ({
                                id: item.id,
                                stream: item.stream,
                                label: item.label,
                                isPresenter: false,
                            }))}
                        />
                        <video ref={aiVideoRef} className="meeting-room__hidden-video" muted playsInline />
                    </div>
                    <div className="meeting-room__side">
                        <MeetingParticipants participants={participants} presenterId={presenterId} />
                        <MeetingChatPanel messages={chatMessages} onSend={handleSendChat} />
                    </div>
                </div>
            </div>
        </MainLayout>
    );
};

export default MeetingRoomPage;
