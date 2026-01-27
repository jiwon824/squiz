import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import axios from 'axios';
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
type ShareMode = 'camera' | 'screen' | 'mixed';

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
    const meetingListPath =
        Number.isFinite(numericStudyId) && numericStudyId > 0 ? `/study/${numericStudyId}/meetings` : '/study';
    const { user, isLoggedIn } = useAuthStore();

    const getGuestName = useCallback(() => {
        const key = 'meeting-guest-name';
        const stored = localStorage.getItem(key);
        if (stored) return stored;
        const generated = `guest-${Math.random().toString(36).slice(2, 6)}`;
        localStorage.setItem(key, generated);
        return generated;
    }, []);

    const getDisplayName = useCallback(() => {
        if (!isLoggedIn) return getGuestName();
        return user?.nickname || user?.name || getGuestName();
    }, [getGuestName, isLoggedIn, user?.nickname, user?.name]);

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
    const remoteAudioTracksRef = useRef<Map<string, MediaStreamTrack>>(new Map());
    const voiceRecorderRef = useRef<MediaRecorder | null>(null);
    const voiceStopResolverRef = useRef<(() => void) | null>(null);
    const voiceUploadChainRef = useRef<Promise<void>>(Promise.resolve());
    const voiceFinalizeRequestedRef = useRef(false);
    const voiceRecordingSourceIdRef = useRef<string | null>(null);
    const voiceSourceUpdateChainRef = useRef<Promise<void>>(Promise.resolve());
    const mixedAudioContextRef = useRef<AudioContext | null>(null);
    const mixedAudioTrackRef = useRef<MediaStreamTrack | null>(null);
    const mixedAudioKeyRef = useRef<string | null>(null);
    const recordingAudioContextRef = useRef<AudioContext | null>(null);
    const recordingAudioTrackRef = useRef<MediaStreamTrack | null>(null);
    const recordingAudioKeyRef = useRef<string | null>(null);

    const [meetingTitle, setMeetingTitle] = useState('');
    const [meetingStartedAt, setMeetingStartedAt] = useState<string | null>(null);
    const [elapsedSeconds, setElapsedSeconds] = useState(0);
    const [participants, setParticipants] = useState<MeetingRoomParticipant[]>([]);
    const [chatMessages, setChatMessages] = useState<MeetingRoomChatMessage[]>([]);
    const [presenterName, setPresenterName] = useState<string | null>(null);
    const [presenterId, setPresenterId] = useState<number | null>(null);
    const [micEnabled, setMicEnabled] = useState(false);
    const [cameraEnabled, setCameraEnabled] = useState(false);
    const [screenSharing, setScreenSharing] = useState(false);
    const [shareMode, setShareMode] = useState<ShareMode | null>(null);
    const [pipPosition] = useState<PipPosition>('bottom-right');
    const [localStream, setLocalStream] = useState<MediaStream | null>(null);
    const [remoteVideoStreams, setRemoteVideoStreams] = useState<RemoteVideoStream[]>([]);
    const [remoteAudioVersion, setRemoteAudioVersion] = useState(0);
    const [isCapturing, setIsCapturing] = useState(false);
    const [roomGuardStatus, setRoomGuardStatus] = useState<'checking' | 'ok' | 'blocked'>('checking');
    const [roomGuardMessage, setRoomGuardMessage] = useState('회의 정보를 확인 중입니다.');
    const [sfuReady, setSfuReady] = useState(false);

    const aiVideoRef = useRef<HTMLVideoElement | null>(null);
    const videoStageRef = useRef<HTMLDivElement | null>(null);
    const ownerKey = user?.id ?? user?.nickname ?? user?.name ?? 'guest';
    const micEnabledRef = useRef(micEnabled);
    const speakingRef = useRef(false);
    const presenceRef = useRef(false);
    const prevPresenterRef = useRef(false);
    const publishedVideoTrackIdRef = useRef<string | null>(null);
    const publishedAudioTrackIdRef = useRef<string | null>(null);
    const shareModeRef = useRef<ShareMode | null>(shareMode);
    const screenSharingRef = useRef(screenSharing);
    const mixedRetryRef = useRef<{ attempts: number; timer: number | null }>({ attempts: 0, timer: null });
    const isPresenterRef = useRef(false);

    useEffect(() => {
        displayNameRef.current = getDisplayName();
    }, [getDisplayName]);

    useEffect(() => {
        micEnabledRef.current = micEnabled;
    }, [micEnabled]);

    useEffect(() => {
        shareModeRef.current = shareMode;
    }, [shareMode]);

    useEffect(() => {
        screenSharingRef.current = screenSharing;
    }, [screenSharing]);

    useEffect(() => {
        if (!meetingStartedAt) return;
        const startedAtMs = new Date(meetingStartedAt).getTime();
        if (Number.isNaN(startedAtMs)) return;
        const updateElapsed = () => {
            const nextSeconds = Math.max(0, Math.floor((Date.now() - startedAtMs) / 1000));
            setElapsedSeconds(nextSeconds);
        };
        updateElapsed();
        const timerId = window.setInterval(updateElapsed, 1000);
        return () => window.clearInterval(timerId);
    }, [meetingStartedAt]);

    const canEndMeeting = useMemo(() => {
        if (!numericMeetingId) return false;
        return localStorage.getItem(`meeting-owner-${numericMeetingId}`) === String(ownerKey);
    }, [numericMeetingId, ownerKey]);

    const isPresenter = useMemo(() => {
        if (presenterId !== null && selfParticipantIdRef.current !== null) {
            return presenterId === selfParticipantIdRef.current;
        }
        if (presenterName) {
            return presenterName === displayNameRef.current;
        }
        return false;
    }, [presenterId, presenterName]);

    useEffect(() => {
        isPresenterRef.current = isPresenter;
    }, [isPresenter]);

    const formatDuration = (totalSeconds: number) => {
        const safeSeconds = Math.max(0, Math.floor(totalSeconds));
        const hours = Math.floor(safeSeconds / 3600);
        const minutes = Math.floor((safeSeconds % 3600) / 60);
        const seconds = safeSeconds % 60;
        const padded = (value: number) => String(value).padStart(2, '0');
        return `${padded(hours)}:${padded(minutes)}:${padded(seconds)}`;
    };

    const stopTracks = (stream: MediaStream | null) => {
        if (!stream) return;
        stream.getTracks().forEach((track) => track.stop());
    };

    const waitForTrackUnmute = useCallback((track: MediaStreamTrack | null, timeoutMs = 1200) => {
        if (!track) return Promise.resolve();
        if (track.readyState !== 'live') return Promise.resolve();
        if (track.muted === false) return Promise.resolve();
        return new Promise<void>((resolve) => {
            const onUnmute = () => {
                cleanup();
                resolve();
            };
            const cleanup = () => {
                track.removeEventListener('unmute', onUnmute);
                window.clearTimeout(timerId);
            };
            const timerId = window.setTimeout(() => {
                cleanup();
                resolve();
            }, timeoutMs);
            track.addEventListener('unmute', onUnmute, { once: true });
        });
    }, []);

    const clearMixedRetry = useCallback(() => {
        if (mixedRetryRef.current.timer) {
            window.clearTimeout(mixedRetryRef.current.timer);
        }
        mixedRetryRef.current = { attempts: 0, timer: null };
    }, []);

    const updateOutgoingAudio = useCallback(async (nextTrack: MediaStreamTrack | null) => {
        if (!sfuClientRef.current) return;
        if (nextTrack && nextTrack.readyState === 'live') {
            if (publishedAudioTrackIdRef.current === nextTrack.id) return;
            await sfuClientRef.current.produceTrack('audio', nextTrack);
            publishedAudioTrackIdRef.current = nextTrack.id;
        } else {
            await sfuClientRef.current.closeProducer('audio');
            publishedAudioTrackIdRef.current = null;
        }
    }, []);

    const stopMixedAudioTrack = useCallback(() => {
        if (mixedAudioTrackRef.current) {
            try {
                mixedAudioTrackRef.current.stop();
            } catch {
                // ignore
            }
        }
        mixedAudioTrackRef.current = null;
        mixedAudioKeyRef.current = null;
        if (mixedAudioContextRef.current) {
            mixedAudioContextRef.current.close().catch(() => {});
            mixedAudioContextRef.current = null;
        }
    }, []);

    const stopRecordingAudioTrack = useCallback(() => {
        if (recordingAudioTrackRef.current) {
            try {
                recordingAudioTrackRef.current.stop();
            } catch {
                // ignore
            }
        }
        recordingAudioTrackRef.current = null;
        recordingAudioKeyRef.current = null;
        if (recordingAudioContextRef.current) {
            recordingAudioContextRef.current.close().catch(() => {});
            recordingAudioContextRef.current = null;
        }
    }, []);

    const ensureMixedAudioTrack = useCallback(
        (tracks: MediaStreamTrack[]) => {
            if (tracks.length <= 1) {
                stopMixedAudioTrack();
                return tracks[0] ?? null;
            }
            const ids = tracks.map((track) => track.id).sort();
            const key = ids.join('|');
            if (mixedAudioKeyRef.current === key && mixedAudioTrackRef.current) {
                if (mixedAudioTrackRef.current.readyState === 'live') {
                    return mixedAudioTrackRef.current;
                }
            }
            stopMixedAudioTrack();
            const context = new AudioContext();
            const destination = context.createMediaStreamDestination();
            tracks.forEach((track) => {
                const sourceStream = new MediaStream([track]);
                const source = context.createMediaStreamSource(sourceStream);
                source.connect(destination);
            });
            context.resume().catch(() => {});
            const outputTrack = destination.stream.getAudioTracks()[0] ?? null;
            mixedAudioContextRef.current = context;
            mixedAudioTrackRef.current = outputTrack;
            mixedAudioKeyRef.current = key;
            return outputTrack;
        },
        [stopMixedAudioTrack]
    );

    const ensureRecordingAudioTrack = useCallback(
        (tracks: MediaStreamTrack[]) => {
            if (tracks.length <= 1) {
                stopRecordingAudioTrack();
                return tracks[0] ?? null;
            }
            const ids = tracks.map((track) => track.id).sort();
            const key = ids.join('|');
            if (recordingAudioKeyRef.current === key && recordingAudioTrackRef.current) {
                if (recordingAudioTrackRef.current.readyState === 'live') {
                    return recordingAudioTrackRef.current;
                }
            }
            stopRecordingAudioTrack();
            const context = new AudioContext();
            const destination = context.createMediaStreamDestination();
            tracks.forEach((track) => {
                const sourceStream = new MediaStream([track]);
                const source = context.createMediaStreamSource(sourceStream);
                source.connect(destination);
            });
            context.resume().catch(() => {});
            const outputTrack = destination.stream.getAudioTracks()[0] ?? null;
            recordingAudioContextRef.current = context;
            recordingAudioTrackRef.current = outputTrack;
            recordingAudioKeyRef.current = key;
            return outputTrack;
        },
        [stopRecordingAudioTrack]
    );

    const getPresenterAudioSelection = useCallback(() => {
        const micTrack = localMicStreamRef.current?.getAudioTracks()?.[0] ?? null;
        const screenTrack = localScreenStreamRef.current?.getAudioTracks()?.[0] ?? null;
        const shareModeValue = shareModeRef.current;
        const tracks: MediaStreamTrack[] = [];
        const sourceIds: string[] = [];
        const micActive = Boolean(micEnabledRef.current && micTrack && micTrack.readyState === 'live');

        if (micActive && micTrack) {
            tracks.push(micTrack);
            sourceIds.push(`mic:${micTrack.id}`);
        }

        if (shareModeValue === 'screen' || shareModeValue === 'mixed') {
            if (screenTrack && screenTrack.readyState === 'live') {
                tracks.push(screenTrack);
                sourceIds.push(`screen:${screenTrack.id}`);
            }
        }

        if (tracks.length === 0) {
            stopMixedAudioTrack();
            return null;
        }

        const track = ensureMixedAudioTrack(tracks);
        const sourceId = tracks.length > 1 ? `mix:${sourceIds.join('|')}` : sourceIds[0];
        return { track, sourceId };
    }, [ensureMixedAudioTrack, stopMixedAudioTrack]);

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

    const ensureAiDetection = useCallback(() => {
        const stream = localCameraStreamRef.current;
        if (!stream || !aiVideoRef.current) return;
        const track = stream.getVideoTracks()?.[0];
        if (!track || track.readyState !== 'live') return;
        if (aiVideoRef.current.srcObject !== stream) {
            aiVideoRef.current.srcObject = stream;
        }
        aiVideoRef.current.muted = true;
        aiVideoRef.current.playsInline = true;
        aiVideoRef.current.play().catch(() => {});
        if (!aiDetectionCleanupRef.current) {
            aiDetectionCleanupRef.current = aiDetection.startDetection(aiVideoRef.current, (isPresent) => {
                if (presenceRef.current === isPresent) return;
                presenceRef.current = isPresent;
                updateSelfParticipant({ isPresent });
                if (wsClientRef.current && roomIdRef.current) {
                    wsClientRef.current.setPresence(roomIdRef.current, { present: isPresent });
                }
            });
        }
    }, [updateSelfParticipant]);

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

    const handleDeleteChat = useCallback((target: MeetingRoomChatMessage) => {
        setChatMessages((prev) =>
            prev.filter(
                (message) =>
                    !(
                        message.sentAt === target.sentAt &&
                        message.sender === target.sender &&
                        message.text === target.text
                    )
            )
        );
    }, []);

        const updateOutgoingVideo = useCallback(
        async (options?: {
            nextCameraStream?: MediaStream | null;
            nextScreenStream?: MediaStream | null;
            nextPipPosition?: PipPosition;
            publish?: boolean;
            cameraEnabledOverride?: boolean;
        }) => {
            const token = ++updateTokenRef.current;
            const cameraStream =
                options?.nextCameraStream !== undefined ? options.nextCameraStream : localCameraStreamRef.current;
            const screenStream =
                options?.nextScreenStream !== undefined ? options.nextScreenStream : localScreenStreamRef.current;
            const nextPosition = options?.nextPipPosition ?? pipPosition;
            const publish = options?.publish ?? true;
            const cameraEnabledValue = options?.cameraEnabledOverride ?? cameraEnabled;
            const effectiveCameraStream = cameraEnabledValue ? cameraStream : null;
            let nextStream: MediaStream | null = null;
            const cameraTrack = effectiveCameraStream?.getVideoTracks()?.[0] ?? null;
            const screenTrack = screenStream?.getVideoTracks()?.[0] ?? null;
            const wantsMixed = shareModeRef.current === 'mixed';
            const hasLiveTracks =
                cameraTrack &&
                screenTrack &&
                cameraTrack.readyState === 'live' &&
                screenTrack.readyState === 'live';
            let composedSuccess = false;

            if (
                options?.nextPipPosition &&
                !options?.nextCameraStream &&
                !options?.nextScreenStream &&
                shareMode === 'mixed' &&
                cameraTrack &&
                screenTrack &&
                canvasComposer.isComposingWith(screenTrack.id, cameraTrack.id)
            ) {
                canvasComposer.updatePipPosition(nextPosition);
                return;
            }

            try {
                if (wantsMixed && hasLiveTracks) {
                    await Promise.all([waitForTrackUnmute(screenTrack), waitForTrackUnmute(cameraTrack)]);
                    if (token !== updateTokenRef.current) return;
                }
                if (
                    effectiveCameraStream &&
                    screenStream &&
                    cameraTrack &&
                    screenTrack &&
                    cameraTrack.readyState === 'live' &&
                    screenTrack.readyState === 'live'
                ) {
                    const composed = await canvasComposer.composeStreams(screenStream, effectiveCameraStream, {
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
                        composedSuccess = true;
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
                    } else if (effectiveCameraStream && effectiveCameraStream.getVideoTracks().length > 0) {
                        nextStream = effectiveCameraStream;
                    }
                }
            } catch (error) {
                console.error('Failed to update composed stream', error);
                canvasComposer.stopComposing();
                if (screenTrack && screenTrack.readyState === 'live') {
                    nextStream = screenStream;
                } else if (cameraTrack && cameraTrack.readyState === 'live') {
                    nextStream = effectiveCameraStream;
                }
            }

            if (token !== updateTokenRef.current) return;
            if (!wantsMixed) {
                clearMixedRetry();
            } else if (composedSuccess) {
                clearMixedRetry();
            } else if (wantsMixed && hasLiveTracks && mixedRetryRef.current.attempts < 5) {
                mixedRetryRef.current.attempts += 1;
                if (!mixedRetryRef.current.timer) {
                    mixedRetryRef.current.timer = window.setTimeout(() => {
                        mixedRetryRef.current.timer = null;
                        updateOutgoingVideo({
                            nextCameraStream: localCameraStreamRef.current,
                            nextScreenStream: localScreenStreamRef.current,
                            publish: true,
                            cameraEnabledOverride: true,
                        });
                    }, 300);
                }
            }

            if (!publish) {
                await updateOutgoingAudio(null);
            } else if (isPresenter && shareModeRef.current) {
                const selection = getPresenterAudioSelection();
                await updateOutgoingAudio(selection?.track ?? null);
            }

            setLocalStream(publish ? nextStream : null);

            if (!publish) {
                if (sfuClientRef.current) {
                    await sfuClientRef.current.closeProducer('video');
                }
                publishedVideoTrackIdRef.current = null;
                return;
            }

            const videoTrack = nextStream?.getVideoTracks()[0] ?? null;
            const nextVideoSource = (() => {
                if (shareModeRef.current === 'mixed' && composedSuccess) {
                    return 'mixed';
                }
                if (nextStream && screenStream && nextStream === screenStream) {
                    return 'screen';
                }
                if (nextStream && effectiveCameraStream && nextStream === effectiveCameraStream) {
                    return 'camera';
                }
                if (shareModeRef.current === 'screen') {
                    return 'screen';
                }
                if (shareModeRef.current === 'camera') {
                    return 'camera';
                }
                return undefined;
            })();
            if (videoTrack) {
                await waitForTrackUnmute(videoTrack);
                if (token !== updateTokenRef.current) return;
            }
            if (sfuClientRef.current) {
                if (videoTrack && videoTrack.readyState === 'live') {
                    if (publishedVideoTrackIdRef.current && publishedVideoTrackIdRef.current !== videoTrack.id) {
                        await sfuClientRef.current.closeProducer('video');
                    }
                    await sfuClientRef.current.produceTrack(
                        'video',
                        videoTrack,
                        nextVideoSource ? { source: nextVideoSource } : undefined
                    );
                    publishedVideoTrackIdRef.current = videoTrack.id;
                } else {
                    await sfuClientRef.current.closeProducer('video');
                    publishedVideoTrackIdRef.current = null;
                }
            }
        },
        [
            cameraEnabled,
            clearMixedRetry,
            getPresenterAudioSelection,
            isPresenter,
            pipPosition,
            shareMode,
            updateOutgoingAudio,
            waitForTrackUnmute,
        ]
    );

    const VOICE_RECORDER_SLICE_MS = 1000;
    const VOICE_RECORDER_FLUSH_DELAY_MS = 400;

    const startVoiceRecording = useCallback(
        (stream: MediaStream) => {
            if (!numericStudyId || !numericMeetingId) return;
            if (!isLoggedIn) return;
            if (voiceRecorderRef.current) return;
            if (typeof MediaRecorder === 'undefined') return;
            const supportedType = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
                ? 'audio/webm;codecs=opus'
                : 'audio/webm';
            const recorder = new MediaRecorder(stream, { mimeType: supportedType });
            const chunks: BlobPart[] = [];
            recorder.ondataavailable = (event) => {
                if (event.data && event.data.size > 0) {
                    chunks.push(event.data);
                }
            };
            recorder.onstop = () => {
                const blob = new Blob(chunks, { type: supportedType });
                if (blob.size > 0) {
                    voiceUploadChainRef.current = voiceUploadChainRef.current
                        .then(() => meetingApi.uploadRecordingAudioSegment(numericStudyId, numericMeetingId, blob))
                        .catch((error) => {
                            console.error('Failed to upload voice segment', error);
                        });
                }
                if (voiceStopResolverRef.current) {
                    voiceStopResolverRef.current();
                    voiceStopResolverRef.current = null;
                }
            };
            recorder.start(VOICE_RECORDER_SLICE_MS);
            voiceRecorderRef.current = recorder;
        },
        [isLoggedIn, numericMeetingId, numericStudyId]
    );

    const stopVoiceRecording = useCallback(async () => {
        const recorder = voiceRecorderRef.current;
        if (!recorder) return;
        if (recorder.state === 'inactive') {
            voiceRecorderRef.current = null;
            return;
        }
        const stopPromise = new Promise<void>((resolve) => {
            voiceStopResolverRef.current = resolve;
        });
        try {
            recorder.requestData();
        } catch {
            // ignore
        }
        await new Promise((resolve) => window.setTimeout(resolve, VOICE_RECORDER_FLUSH_DELAY_MS));
        recorder.stop();
        voiceRecorderRef.current = null;
        await stopPromise;
        await voiceUploadChainRef.current;
    }, []);

    const finalizeVoiceRecording = useCallback(async () => {
        if (!numericStudyId || !numericMeetingId) return;
        if (!isLoggedIn) return;
        if (!canEndMeeting) return;
        if (voiceFinalizeRequestedRef.current) return;
        voiceFinalizeRequestedRef.current = true;
        try {
            await stopVoiceRecording();
            await voiceUploadChainRef.current;
            await meetingApi.concatRecordingAudio(numericStudyId, numericMeetingId);
        } catch (error) {
            console.error('Failed to finalize voice recording', error);
        }
    }, [canEndMeeting, isLoggedIn, numericMeetingId, numericStudyId, stopVoiceRecording]);

    const getVoiceRecordingTrack = useCallback(() => {
        const tracks: MediaStreamTrack[] = [];
        const sourceIds: string[] = [];
        const micTrack = localMicStreamRef.current?.getAudioTracks()?.[0] ?? null;
        const screenTrack = localScreenStreamRef.current?.getAudioTracks()?.[0] ?? null;

        if (micEnabledRef.current && micTrack && micTrack.readyState === 'live') {
            tracks.push(micTrack);
            sourceIds.push(`mic:${micTrack.id}`);
        }

        if (screenSharingRef.current && screenTrack && screenTrack.readyState === 'live') {
            tracks.push(screenTrack);
            sourceIds.push(`screen:${screenTrack.id}`);
        }

        remoteAudioTracksRef.current.forEach((track, producerId) => {
            if (track.readyState === 'live') {
                tracks.push(track);
                sourceIds.push(`remote:${producerId}:${track.id}`);
            }
        });

        if (tracks.length === 0) {
            stopRecordingAudioTrack();
            return null;
        }

        const mixedTrack = ensureRecordingAudioTrack(tracks);
        const track = tracks.length > 1 ? mixedTrack : tracks[0];
        if (!track) return null;
        const sourceId = tracks.length > 1 ? `mix:${sourceIds.join('|')}` : sourceIds[0];
        return { track, sourceId };
    }, [ensureRecordingAudioTrack, stopRecordingAudioTrack]);

    const updateVoiceRecordingSource = useCallback(() => {
        voiceSourceUpdateChainRef.current = voiceSourceUpdateChainRef.current.then(async () => {
            const shouldPublishPresenterAudio = isPresenterRef.current && shareModeRef.current;
            const presenterSelection = shouldPublishPresenterAudio ? getPresenterAudioSelection() : null;
            const canRecord = isLoggedIn && canEndMeeting && numericStudyId && numericMeetingId;

            if (!canRecord) {
                if (voiceRecorderRef.current) {
                    await stopVoiceRecording();
                }
                voiceRecordingSourceIdRef.current = null;
                if (shouldPublishPresenterAudio) {
                    await updateOutgoingAudio(presenterSelection?.track ?? null);
                }
                return;
            }

            const selection = getVoiceRecordingTrack();
            if (!selection) {
                if (voiceRecorderRef.current) {
                    await stopVoiceRecording();
                }
                voiceRecordingSourceIdRef.current = null;
                if (shouldPublishPresenterAudio) {
                    await updateOutgoingAudio(presenterSelection?.track ?? null);
                }
                return;
            }
            const nextSourceId = selection.sourceId;
            if (voiceRecorderRef.current && voiceRecordingSourceIdRef.current === nextSourceId) {
                if (shouldPublishPresenterAudio) {
                    await updateOutgoingAudio(presenterSelection?.track ?? null);
                }
                return;
            }
            if (voiceRecorderRef.current) {
                await stopVoiceRecording();
            }
            const stream = new MediaStream([selection.track]);
            startVoiceRecording(stream);
            voiceRecordingSourceIdRef.current = nextSourceId;
            if (shouldPublishPresenterAudio) {
                await updateOutgoingAudio(presenterSelection?.track ?? null);
            }
        });
    }, [
        canEndMeeting,
        getPresenterAudioSelection,
        getVoiceRecordingTrack,
        isLoggedIn,
        numericMeetingId,
        numericStudyId,
        startVoiceRecording,
        stopVoiceRecording,
        updateOutgoingAudio,
    ]);

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
            updateVoiceRecordingSource();
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
    }, [updateSelfParticipant, updateVoiceRecordingSource]);

    const stopMicrophone = useCallback(async () => {
        await stopVoiceRecording();
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
        updateVoiceRecordingSource();
    }, [stopVoiceRecording, updateSelfParticipant, updateVoiceRecordingSource]);

    const ensureCameraStream = useCallback(async (publishCamera?: boolean) => {
        if (!navigator.mediaDevices?.getUserMedia) {
            setCameraEnabled(false);
            return;
        }
        const needsAudio = false;
        if (localCameraStreamRef.current) {
            const track = localCameraStreamRef.current.getVideoTracks()?.[0];
            const hasAudio = localCameraStreamRef.current.getAudioTracks().length > 0;
            if (!track || track.readyState !== 'live' || (needsAudio && !hasAudio)) {
                stopTracks(localCameraStreamRef.current);
                localCameraStreamRef.current = null;
            }
        }
        if (localCameraStreamRef.current) {
            if (publishCamera && isPresenter) {
                await updateOutgoingVideo({ publish: true, cameraEnabledOverride: true });
            }
            return;
        }
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: needsAudio });
            localCameraStreamRef.current = stream;
            const shouldPublish = publishCamera ?? (isPresenter && cameraEnabled);
            if (isPresenter && shouldPublish) {
                await updateOutgoingVideo({
                    nextCameraStream: stream,
                    publish: true,
                    cameraEnabledOverride: true,
                });
            } else if (isPresenter) {
                await updateOutgoingVideo({
                    nextCameraStream: stream,
                    publish: true,
                    cameraEnabledOverride: false,
                });
            } else {
                await updateOutgoingVideo({
                    nextCameraStream: stream,
                    publish: false,
                    cameraEnabledOverride: false,
                });
            }
            updateVoiceRecordingSource();
            if (aiVideoRef.current) {
                aiVideoRef.current.srcObject = stream;
                aiVideoRef.current.play().catch(() => {});
                if (aiDetectionCleanupRef.current) {
                    aiDetectionCleanupRef.current();
                }
                aiDetectionCleanupRef.current = aiDetection.startDetection(aiVideoRef.current, (isPresent) => {
                    if (presenceRef.current === isPresent) return;
                    presenceRef.current = isPresent;
                    updateSelfParticipant({ isPresent });
                    if (wsClientRef.current && roomIdRef.current) {
                        wsClientRef.current.setPresence(roomIdRef.current, { present: isPresent });
                    }
                });
            }
        } catch (error) {
            console.error('Failed to access camera', error);
            setCameraEnabled(false);
        }
    }, [cameraEnabled, isPresenter, updateOutgoingVideo, updateSelfParticipant, updateVoiceRecordingSource]);

    const stopCameraPublish = useCallback(async () => {
        setCameraEnabled(false);
        await updateOutgoingVideo({ publish: isPresenter, cameraEnabledOverride: false });
        ensureAiDetection();
    }, [ensureAiDetection, isPresenter, updateOutgoingVideo]);

    const stopCameraHardware = useCallback(() => {
        if (aiDetectionCleanupRef.current) {
            aiDetectionCleanupRef.current();
            aiDetectionCleanupRef.current = null;
        }
        presenceRef.current = false;
        updateSelfParticipant({ isPresent: false });
        if (wsClientRef.current && roomIdRef.current) {
            wsClientRef.current.setPresence(roomIdRef.current, { present: false });
        }
        stopTracks(localCameraStreamRef.current);
        localCameraStreamRef.current = null;
    }, [updateSelfParticipant]);

    const startScreenShare = useCallback(
        async (cameraEnabledOverride?: boolean, publishOnStart = true) => {
            const needsAudio = shareModeRef.current === 'screen' || shareModeRef.current === 'mixed';
            if (localScreenStreamRef.current) {
                const videoTrack = localScreenStreamRef.current.getVideoTracks()[0];
                const hasAudio = localScreenStreamRef.current.getAudioTracks().length > 0;
                if (!videoTrack || videoTrack.readyState !== 'live' || (needsAudio && !hasAudio)) {
                    stopTracks(localScreenStreamRef.current);
                    localScreenStreamRef.current = null;
                } else {
                    setScreenSharing(true);
                    screenSharingRef.current = true;
                    return;
                }
            }
            try {
                const stream = await navigator.mediaDevices.getDisplayMedia({ video: true, audio: needsAudio });
                localScreenStreamRef.current = stream;
                setScreenSharing(true);
                screenSharingRef.current = true;
                updateVoiceRecordingSource();
                const [track] = stream.getVideoTracks();
                if (track) {
                    track.onended = () => {
                        setScreenSharing(false);
                        screenSharingRef.current = false;
                        localScreenStreamRef.current = null;
                        if (shareModeRef.current === 'mixed' || shareModeRef.current === 'screen') {
                            setShareMode('camera');
                            shareModeRef.current = 'camera';
                            setCameraEnabled(true);
                        }
                        void updateOutgoingAudio(null);
                        updateOutgoingVideo({ nextScreenStream: null });
                        updateVoiceRecordingSource();
                    };
                }
                if (publishOnStart) {
                    await updateOutgoingVideo({
                        nextScreenStream: stream,
                        cameraEnabledOverride,
                    });
                }
            } catch (error) {
                console.error('Failed to start screen share', error);
                setScreenSharing(false);
            }
        },
        [updateOutgoingAudio, updateOutgoingVideo, updateVoiceRecordingSource]
    );

    const stopScreenShare = useCallback(async () => {
        stopTracks(localScreenStreamRef.current);
        localScreenStreamRef.current = null;
        setScreenSharing(false);
        screenSharingRef.current = false;
        clearMixedRetry();
        await updateOutgoingAudio(null);
        await updateOutgoingVideo({ nextScreenStream: null });
        updateVoiceRecordingSource();
    }, [clearMixedRetry, updateOutgoingAudio, updateOutgoingVideo, updateVoiceRecordingSource]);

    const handleToggleMic = useCallback(() => {
        if (micEnabled) {
            void stopMicrophone();
        } else {
            void startMicrophone();
        }
    }, [micEnabled, startMicrophone, stopMicrophone]);

    const handleShareModeChange = useCallback(
        async (mode: ShareMode) => {
            if (!isPresenterRef.current) return;
            clearMixedRetry();
            const prevMode = shareModeRef.current;
            canvasComposer.stopComposing();
            if (composedStreamRef.current) {
                composedStreamRef.current = null;
                await sfuClientRef.current?.closeProducer('video');
                publishedVideoTrackIdRef.current = null;
            }
            setShareMode(mode);
            shareModeRef.current = mode;
            if (mode === 'camera') {
                if (screenSharing || localScreenStreamRef.current) {
                    await stopScreenShare();
                    stopTracks(localScreenStreamRef.current);
                    localScreenStreamRef.current = null;
                    setScreenSharing(false);
                    screenSharingRef.current = false;
                }
                setShareMode('camera');
                shareModeRef.current = 'camera';
                setCameraEnabled(true);
                await ensureCameraStream(true);
                await updateOutgoingVideo({
                    publish: true,
                    cameraEnabledOverride: true,
                    nextCameraStream: localCameraStreamRef.current,
                    nextScreenStream: null,
                });
                updateVoiceRecordingSource();
                return;
            }
            if (mode === 'screen') {
                setShareMode('screen');
                shareModeRef.current = 'screen';
                setCameraEnabled(false);
                stopTracks(localCameraStreamRef.current);
                localCameraStreamRef.current = null;
                await ensureCameraStream(false);
                if (!screenSharing) {
                    await startScreenShare(false, true);
                    await updateOutgoingVideo({
                        publish: true,
                        cameraEnabledOverride: false,
                        nextScreenStream: localScreenStreamRef.current,
                    });
                } else {
                    await updateOutgoingVideo({
                        publish: true,
                        cameraEnabledOverride: false,
                        nextScreenStream: localScreenStreamRef.current,
                    });
                }
                updateVoiceRecordingSource();
                return;
            }
            // mixed
            setShareMode('mixed');
            shareModeRef.current = 'mixed';
            setCameraEnabled(true);
            if (prevMode !== 'mixed') {
                stopTracks(localScreenStreamRef.current);
                localScreenStreamRef.current = null;
                setScreenSharing(false);
                screenSharingRef.current = false;
                stopTracks(localCameraStreamRef.current);
                localCameraStreamRef.current = null;
            }
            await ensureCameraStream(true);
            await startScreenShare(true, false);
            await updateOutgoingVideo({
                publish: true,
                cameraEnabledOverride: true,
                nextCameraStream: localCameraStreamRef.current,
                nextScreenStream: localScreenStreamRef.current,
            });
            window.setTimeout(() => {
                if (shareModeRef.current === 'mixed' && screenSharingRef.current) {
                    updateOutgoingVideo({
                        publish: true,
                        cameraEnabledOverride: true,
                        nextScreenStream: localScreenStreamRef.current,
                    });
                }
            }, 500);
            updateVoiceRecordingSource();
        },
        [
            clearMixedRetry,
            ensureCameraStream,
            isPresenter,
            screenSharing,
            shareMode,
            startScreenShare,
            stopScreenShare,
            updateOutgoingVideo,
            updateVoiceRecordingSource,
        ]
    );

    const handleTogglePresenter = useCallback(() => {
        if (!roomIdRef.current || !wsClientRef.current) return;
        if (isPresenter) {
            isPresenterRef.current = false;
            wsClientRef.current.setPresenter(roomIdRef.current, {
                displayName: displayNameRef.current,
                action: 'release',
            });
            setPresenterName(null);
            setPresenterId(null);
            setShareMode(null);
            setScreenSharing(false);
            setCameraEnabled(false);
            canvasComposer.stopComposing();
            if (composedStreamRef.current) {
                stopTracks(composedStreamRef.current);
                composedStreamRef.current = null;
            }
            stopMixedAudioTrack();
            void updateOutgoingAudio(null);
            updateOutgoingVideo({ publish: false, cameraEnabledOverride: false, nextScreenStream: null });
            return;
        }
        const confirmed = window.confirm('발표자가 되시겠습니까? 현재 발표자는 권한을 내려야 합니다.');
        if (!confirmed) return;
        isPresenterRef.current = true;
        wsClientRef.current.setPresenter(roomIdRef.current, {
            displayName: displayNameRef.current,
            action: 'claim',
        });
        setPresenterName(displayNameRef.current);
        if (selfParticipantIdRef.current !== null) {
            setPresenterId(selfParticipantIdRef.current);
        }
    }, [isPresenter, stopMixedAudioTrack, updateOutgoingAudio, updateOutgoingVideo]);

    useEffect(() => {
        if (!isPresenter) return;
        if (shareModeRef.current !== null) return;
        void handleShareModeChange('camera');
    }, [handleShareModeChange, isPresenter]);


    useEffect(() => {
        const wasPresenter = prevPresenterRef.current;
        prevPresenterRef.current = isPresenter;
        if (wasPresenter && !isPresenter) {
            void stopScreenShare();
            if (cameraEnabled) {
                void stopCameraPublish();
            }
        }
    }, [cameraEnabled, isPresenter, stopCameraPublish, stopScreenShare]);

    useEffect(() => {
        if (!sfuReady || !isPresenter) return;
        if (shareModeRef.current === null) {
            void handleShareModeChange('camera');
            return;
        }
        void updateOutgoingVideo({ publish: true });
    }, [handleShareModeChange, isPresenter, sfuReady, updateOutgoingVideo]);

    useEffect(() => {
        void ensureCameraStream(false);
    }, [ensureCameraStream]);

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

    const captureFrame = (video: HTMLVideoElement) =>
        new Promise<Blob | null>((resolve) => {
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
            canvas.toBlob(resolve, 'image/png');
        });

    const handleCapture = useCallback(async () => {
        if (!numericStudyId || !numericMeetingId || isCapturing) return;
        const video = videoStageRef.current?.querySelector('video');
        if (!video) {
            window.alert('캡처할 화면이 없습니다.');
            return;
        }
        setIsCapturing(true);
        try {
            const blob = await captureFrame(video);
            if (!blob) {
                window.alert('캡처할 화면이 없습니다.');
                return;
            }
            const file = new File([blob], 'meeting-capture.png', { type: 'image/png' });
            await meetingApi.addPhoto(numericStudyId, numericMeetingId, file);
        } catch (error) {
            console.error('Failed to capture meeting screen', error);
        } finally {
            setIsCapturing(false);
        }
    }, [isCapturing, numericMeetingId, numericStudyId]);

    const presenterLabel = presenterName ? '발표자: ' + presenterName : '발표자';

    const handleEndMeeting = useCallback(async () => {
        if (!numericStudyId || !numericMeetingId || !canEndMeeting) return;
        const confirmed = window.confirm('미팅을 종료하시겠습니까?');
        if (!confirmed) return;
        try {
            await finalizeVoiceRecording();
            await meetingApi.endMeeting(numericStudyId, numericMeetingId);
        } catch (error) {
            console.error('Failed to end meeting', error);
        } finally {
            stopCameraHardware();
            navigate(`/study/${numericStudyId}/meetings/${numericMeetingId}`);
        }
    }, [numericStudyId, numericMeetingId, canEndMeeting, navigate, stopCameraHardware, finalizeVoiceRecording]);

    const handleRoomEvent = useCallback(
        (event: MeetingRoomEvent) => {
            if (event.type === 'MEETING_ENDED') {
                void finalizeVoiceRecording();
                stopCameraHardware();
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
            const presenterNameFromEvent = event.presenterName ?? presenterName;
            if (
                event.presenterId &&
                presenterNameFromEvent &&
                presenterNameFromEvent === displayNameRef.current &&
                selfParticipantIdRef.current === null
            ) {
                selfParticipantIdRef.current = event.presenterId;
            }
        },
        [
            appendChatMessage,
            finalizeVoiceRecording,
            mergeParticipants,
            navigate,
            numericStudyId,
            numericMeetingId,
            presenterName,
            stopCameraHardware,
        ]
    );

    const handleNewConsumer = useCallback(
        (payload: SfuConsumerPayload) => {
            if (payload.kind === 'audio') {
                const audio = new Audio();
                audio.srcObject = payload.stream;
                audio.autoplay = true;
                audio.play().catch(() => {});
                remoteAudioElementsRef.current.set(payload.producerId, audio);
                const track = payload.stream.getAudioTracks()?.[0] ?? null;
                if (track) {
                    remoteAudioTracksRef.current.set(payload.producerId, track);
                    setRemoteAudioVersion((prev) => prev + 1);
                    updateVoiceRecordingSource();
                }
                return;
            }
            setRemoteVideoStreams((prev) => {
                if (prev.some((item) => item.id === payload.consumerId)) {
                    return prev;
                }
                const withoutSamePeer = payload.peerId
                    ? prev.filter((item) => item.peerId !== payload.peerId)
                    : [];
                const label = payload.peerId ? `참가자(${payload.peerId.slice(0, 6)})` : '참가자';
                return [
                    ...withoutSamePeer,
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
        [setRemoteVideoStreams, updateVoiceRecordingSource]
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
        if (remoteAudioTracksRef.current.delete(payload.producerId)) {
            setRemoteAudioVersion((prev) => prev + 1);
            updateVoiceRecordingSource();
        }
    }, [updateVoiceRecordingSource]);

    useEffect(() => {
        let cancelled = false;
        const setup = async () => {
            setRoomGuardStatus('checking');
            setRoomGuardMessage('회의 정보를 확인 중입니다.');
            if (!numericStudyId || !numericMeetingId || Number.isNaN(numericStudyId) || Number.isNaN(numericMeetingId)) {
                setRoomGuardStatus('blocked');
                setRoomGuardMessage('존재하지 않는 회의입니다.');
                return;
            }
            try {
                const detail = await meetingApi.getMeetingDetail(numericStudyId, numericMeetingId);
                if (cancelled) return;
                if (detail.status === 'ENDED' || detail.endedAt) {
                    setRoomGuardStatus('blocked');
                    setRoomGuardMessage('종료된 회의입니다.');
                    return;
                }
                setMeetingTitle(detail.title || `미팅 ${numericMeetingId}`);
                setMeetingStartedAt(detail.startedAt ?? new Date().toISOString());
            } catch (error) {
                if (cancelled) return;
                if (axios.isAxiosError(error) && error.response?.status === 404) {
                    setRoomGuardStatus('blocked');
                    setRoomGuardMessage('존재하지 않는 회의입니다.');
                    return;
                }
                setMeetingTitle(`미팅 ${numericMeetingId}`);
                setMeetingStartedAt(new Date().toISOString());
            }

            let joinData: MeetingJoinResponse | null = null;
            const fallbackRoomId = `meeting-${numericMeetingId}`;
            setRoomGuardStatus('ok');
            try {
                joinData = await meetingApi.joinMeeting(numericStudyId, numericMeetingId);
                joinSuccessRef.current = true;
            } catch (error) {
                console.error('Failed to join meeting', error);
                joinSuccessRef.current = false;
                if (!cancelled && axios.isAxiosError(error) && error.response?.status === 404) {
                    setRoomGuardStatus('blocked');
                    setRoomGuardMessage('존재하지 않는 회의입니다.');
                    return;
                }
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
                setSfuReady(true);
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
            void finalizeVoiceRecording();
            wsUnsubscribeRef.current.forEach((unsubscribe) => unsubscribe());
            wsUnsubscribeRef.current = [];
            if (wsClientRef.current) {
                wsClientRef.current.disconnect();
            }
            if (sfuClientRef.current) {
                sfuClientRef.current.close();
            }
            setSfuReady(false);
            if (aiDetectionCleanupRef.current) {
                aiDetectionCleanupRef.current();
                aiDetectionCleanupRef.current = null;
            }
            audioDetection.stopDetection();
            stopTracks(localMicStreamRef.current);
            stopCameraHardware();
            stopTracks(localScreenStreamRef.current);
            stopMixedAudioTrack();
            stopRecordingAudioTrack();
            remoteAudioElementsRef.current.forEach((audio) => {
                try {
                    audio.pause();
                } catch {
                    // ignore
                }
                audio.srcObject = null;
            });
            remoteAudioElementsRef.current.clear();
            remoteAudioTracksRef.current.clear();
            canvasComposer.stopComposing();
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
        finalizeVoiceRecording,
        stopMixedAudioTrack,
        stopRecordingAudioTrack,
        startMicrophone,
    ]);

    useEffect(() => {
        if (!micEnabled && audioDetectionActiveRef.current) {
            updateSelfParticipant({ isSpeaking: false });
        }
    }, [micEnabled, updateSelfParticipant]);

    useEffect(() => {
        updateVoiceRecordingSource();
    }, [cameraEnabled, isPresenter, micEnabled, remoteAudioVersion, screenSharing, shareMode, updateVoiceRecordingSource]);

    if (roomGuardStatus === 'blocked') {
        return (
            <MainLayout>
                <div className="meeting-room meeting-room__blocked">
                    <div className="meeting-room__blocked-card">
                        <h1>회의 입장이 제한되었습니다</h1>
                        <p>{roomGuardMessage}</p>
                        <div className="meeting-room__blocked-actions">
                            <button
                                className="meeting-btn primary"
                                onClick={() => navigate(meetingListPath)}
                            >
                                회의 목록으로
                            </button>
                            <button className="meeting-btn ghost" onClick={() => navigate(-1)}>
                                이전 화면
                            </button>
                        </div>
                    </div>
                </div>
            </MainLayout>
        );
    }
    if (roomGuardStatus === 'checking') {
        return (
            <MainLayout>
                <div className="meeting-room meeting-room__blocked">
                    <div className="meeting-room__blocked-card">
                        <p>{roomGuardMessage}</p>
                    </div>
                </div>
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <div className="meeting-room">
                <div className="meeting-room__meta">
                    <div className="meeting-room__meta-row">
                        <div className="meeting-room__meta-left">
                            <div className="meeting-room__meta-title">
                                <h1>{meetingTitle || '미팅 룸'}</h1>
                                <div className="meeting-room__timer">진행 시간: {formatDuration(elapsedSeconds)}</div>
                            </div>
                        </div>
                        <div className="meeting-room__meta-right">
                            <div className="meeting-room__status">
                                <span>{isPresenter ? '발표자 모드' : '참가자 모드'}</span>
                            </div>
                            <button className="meeting-btn ghost" onClick={() => navigate(meetingListPath)}>
                                목록으로
                            </button>
                        </div>
                    </div>

                    <MeetingControls
                        isPresenter={isPresenter}
                        micEnabled={micEnabled}
                        micDisabled={false}
                        shareMode={shareMode}
                        onToggleMic={handleToggleMic}
                        onShareModeChange={handleShareModeChange}
                        onTogglePresenter={handleTogglePresenter}
                        onEndMeeting={handleEndMeeting}
                        canEndMeeting={canEndMeeting}
                        captureDisabled={isCapturing}
                        onCapture={handleCapture}
                    />
                </div>

                <div className="meeting-room__content">
                    <div className="meeting-room__stage">
                        <MeetingVideoStage
                            localStream={localStream}
                            localLabel={displayNameRef.current}
                            localIsPresenter={isPresenter}
                            containerRef={videoStageRef}
                            remoteVideoStreams={remoteVideoStreams.map((item) => ({
                                id: item.id,
                                stream: item.stream,
                                label: presenterName ? presenterLabel : item.label,
                                isPresenter: Boolean(presenterName),
                            }))}
                        />
                        <video ref={aiVideoRef} className="meeting-room__hidden-video" muted playsInline />
                    </div>
                    <div className="meeting-room__side">
                        <MeetingParticipants
                            participants={participants}
                            presenterId={presenterId}
                            presenterName={presenterName}
                        />
                        <MeetingChatPanel
                            messages={chatMessages}
                            onSend={handleSendChat}
                            onDelete={handleDeleteChat}
                            currentUserId={user?.id ?? null}
                            currentSender={displayNameRef.current}
                        />
                    </div>
                </div>
            </div>
        </MainLayout>
    );
};

export default MeetingRoomPage;












