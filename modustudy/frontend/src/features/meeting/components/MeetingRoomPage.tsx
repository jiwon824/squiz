import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import axios from 'axios';
import { UserLayoutV2 } from '@/layouts/UserLayoutV2';
import { useAuthStore } from '@/store/authStore';
import { useUIStore } from '@/store/uiStore';
import MeetingRoomHeader from './MeetingRoomHeader';
import MeetingRoomContent from './MeetingRoomContent';
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
import { captureFrame, formatDuration, formatPlannedDuration, stopTracks } from './meetingRoomUtils';

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
    const { showToast } = useUIStore();

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
    const micAudioContextRef = useRef<AudioContext | null>(null);
    const micGainNodeRef = useRef<GainNode | null>(null);
    const micProcessedTrackRef = useRef<MediaStreamTrack | null>(null);
    const micDestinationRef = useRef<MediaStreamAudioDestinationNode | null>(null);
    const micSourceNodeRef = useRef<MediaStreamAudioSourceNode | null>(null);
    const micConstantSourceRef = useRef<ConstantSourceNode | null>(null);
    const sfuBaseUrlRef = useRef<string | null>(null);
    const devicePermissionRequestedRef = useRef(false);
    const autoEndTriggeredRef = useRef(false);

    const [meetingTitle, setMeetingTitle] = useState('');
    const [meetingStartedAt, setMeetingStartedAt] = useState<string | null>(null);
    const [plannedDurationSeconds, setPlannedDurationSeconds] = useState<number | null>(null);
    const [elapsedSeconds, setElapsedSeconds] = useState(0);
    const [timeWarning, setTimeWarning] = useState<string | null>(null);
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
    const [isEnding, setIsEnding] = useState(false);

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
    const MAX_PLANNED_DURATION_SECONDS = 3 * 60 * 60;
    const warningThresholds = useMemo(() => [10 * 60, 5 * 60], []);
    const warningFlagsRef = useRef<Record<number, boolean>>({});

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

    useEffect(() => {
        warningFlagsRef.current = {};
        setTimeWarning(null);
    }, [plannedDurationSeconds, meetingStartedAt]);

    useEffect(() => {
        if (!plannedDurationSeconds) {
            setTimeWarning(null);
            return;
        }
        const remaining = plannedDurationSeconds - elapsedSeconds;
        if (remaining <= 0) {
            setTimeWarning(null);
            return;
        }
        if (remaining <= 10 * 60) {
            const minutesLeft = Math.max(1, Math.ceil(remaining / 60));
            setTimeWarning(`미팅 종료까지 ${minutesLeft}분 남았습니다.`);
        } else {
            setTimeWarning(null);
        }
        if (remaining <= 5 * 60) {
            const threshold = warningThresholds[1];
            if (!warningFlagsRef.current[threshold]) {
                warningFlagsRef.current[threshold] = true;
                showToast('미팅 종료까지 5분 남았습니다.', 'warning');
            }
        } else if (remaining <= 10 * 60) {
            const threshold = warningThresholds[0];
            if (!warningFlagsRef.current[threshold]) {
                warningFlagsRef.current[threshold] = true;
                showToast('미팅 종료까지 10분 남았습니다.', 'warning');
            }
        }
    }, [elapsedSeconds, plannedDurationSeconds, warningThresholds, showToast]);

    useEffect(() => {
        autoEndTriggeredRef.current = false;
    }, [meetingStartedAt, plannedDurationSeconds]);

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
        if (!nextTrack || nextTrack.readyState !== 'live') return;
        if (publishedAudioTrackIdRef.current === nextTrack.id) return;
        await sfuClientRef.current.produceTrack('audio', nextTrack);
        publishedAudioTrackIdRef.current = nextTrack.id;
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

    const stopMicProcessedTrack = useCallback(() => {
        if (micProcessedTrackRef.current) {
            try {
                micProcessedTrackRef.current.stop();
            } catch {
                // ignore
            }
        }
        micProcessedTrackRef.current = null;
        micGainNodeRef.current = null;
        micSourceNodeRef.current = null;
        if (micConstantSourceRef.current) {
            try {
                micConstantSourceRef.current.stop();
            } catch {
                // ignore
            }
        }
        micConstantSourceRef.current = null;
        micDestinationRef.current = null;
        if (micAudioContextRef.current) {
            micAudioContextRef.current.close().catch(() => {});
            micAudioContextRef.current = null;
        }
    }, []);

    const ensureMicProcessedTrack = useCallback(() => {
        if (micProcessedTrackRef.current && micProcessedTrackRef.current.readyState === 'live') {
            return micProcessedTrackRef.current;
        }
        stopMicProcessedTrack();
        const context = new AudioContext();
        const destination = context.createMediaStreamDestination();
        const constantSource = context.createConstantSource();
        constantSource.offset.value = 0;
        constantSource.connect(destination);
        constantSource.start();
        context.resume().catch(() => {});
        micAudioContextRef.current = context;
        micDestinationRef.current = destination;
        micConstantSourceRef.current = constantSource;
        micProcessedTrackRef.current = destination.stream.getAudioTracks()[0] ?? null;
        return micProcessedTrackRef.current;
    }, [stopMicProcessedTrack]);

    const resumeMicContext = useCallback(async () => {
        const context = micAudioContextRef.current;
        if (!context) return;
        if (context.state === 'suspended') {
            try {
                await context.resume();
            } catch {
                // ignore resume errors
            }
        }
        console.log('[mic] context state', context.state);
    }, []);

    const attachMicStreamToMixer = useCallback((stream: MediaStream) => {
        if (!micAudioContextRef.current || !micDestinationRef.current) return;
        micAudioContextRef.current.resume().catch(() => {});
        if (micSourceNodeRef.current) {
            try {
                micSourceNodeRef.current.disconnect();
            } catch {
                // ignore
            }
            micSourceNodeRef.current = null;
        }
        const source = micAudioContextRef.current.createMediaStreamSource(stream);
        const gainNode = micAudioContextRef.current.createGain();
        gainNode.gain.value = 1;
        source.connect(gainNode);
        gainNode.connect(micDestinationRef.current);
        micSourceNodeRef.current = source;
        micGainNodeRef.current = gainNode;
        const track = stream.getAudioTracks()[0] ?? null;
        console.log('[mic] attach mixer', {
            gain: gainNode.gain.value,
            trackId: track?.id,
            trackEnabled: track?.enabled,
            trackMuted: track?.muted,
            trackReadyState: track?.readyState,
        });
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
            // ✅ FIX 1: 토큰 체크를 더 세밀하게 수행
            const checkToken = () => {
            if (token !== updateTokenRef.current) {
                console.warn('[video] Update cancelled - token mismatch', { token, current: updateTokenRef.current });
                return false;
            }
            return true;
            };

            try {
                if (wantsMixed && hasLiveTracks) {
                    await Promise.all([
                    waitForTrackUnmute(screenTrack, 500),
                    waitForTrackUnmute(cameraTrack, 500)
                    ]);
                    
                    if (!checkToken()) {
                    // 토큰이 변경되었지만 트랙이 살아있으면 fallback 처리
                    if (screenTrack?.readyState === 'live') {
                        nextStream = screenStream;
                    } else if (cameraTrack?.readyState === 'live') {
                        nextStream = effectiveCameraStream;
                    }
                    // 화면을 완전히 끄지 않고 단일 스트림으로 폴백
                    console.log('[video] Fallback to single stream on token mismatch');
                    }
                }
                 // ✅ FIX 3: 트랙 상태 재검증
                const isTrackLive = (track: MediaStreamTrack | null) => 
                    track && track.readyState === 'live';

                
                if (
        effectiveCameraStream &&
        screenStream &&
        isTrackLive(cameraTrack) &&
        isTrackLive(screenTrack)
      ) {
        const composed = await canvasComposer.composeStreams(
          screenStream,
          effectiveCameraStream,
          { pipPosition: nextPosition }
        );

        if (!checkToken()) {
          // 토큰 불일치 시에도 composed가 성공했으면 사용
          if (composed && isTrackLive(composed.getVideoTracks()[0])) {
            console.log('[video] Using composed stream despite token mismatch');
            nextStream = composed;
            composedSuccess = true;
          } else {
            if (composed) stopTracks(composed);
            // 폴백
            nextStream = isTrackLive(screenTrack) ? screenStream : 
                        isTrackLive(cameraTrack) ? effectiveCameraStream : null;
          }
        } else if (composed) {
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

        // ✅ FIX 4: 폴백 우선순위 개선
        if (isTrackLive(screenTrack)) {
          nextStream = screenStream;
        } else if (isTrackLive(cameraTrack)) {
          nextStream = effectiveCameraStream;
        }
      }
    } catch (error) {
      console.error('Failed to update composed stream', error);
      
      // ✅ FIX 5: 에러 발생 시에도 폴백 보장
      canvasComposer.stopComposing();
      if (isTrackLive(screenTrack)) {
        nextStream = screenStream;
      } else if (isTrackLive(cameraTrack)) {
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

            // Audio publishing is handled independently (mic toggle).

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
            pipPosition,
            shareMode,
            waitForTrackUnmute,
        ]
    );

    const VOICE_RECORDER_SLICE_MS = 1000;
    const VOICE_RECORDER_FLUSH_DELAY_MS = 400;

    const startVoiceRecording = useCallback(
        (stream: MediaStream) => {
            if (!numericStudyId || !numericMeetingId) return;
            if (!isLoggedIn) return;
            // 회의 소유주만 녹음
            if (!canEndMeeting) return;
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
            console.log('Voice recording started'); // 디버깅용
        },
        [isLoggedIn, numericMeetingId, numericStudyId, canEndMeeting]
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
    }, [
        canEndMeeting,
        isLoggedIn,
        numericMeetingId,
        numericStudyId,
        stopVoiceRecording,
    ]);

    const getVoiceRecordingTrack = useCallback(() => {
        const tracks: MediaStreamTrack[] = [];
        const sourceIds: string[] = [];
        const micTrack = localMicStreamRef.current?.getAudioTracks()?.[0] ?? null;

        // 내 마이크 오디오만 포함 (마이크가 켜져있을 때만)
        if (micEnabledRef.current && micTrack && micTrack.readyState === 'live') {
            tracks.push(micTrack);
            sourceIds.push(`mic:${micTrack.id}`);
        }

        // 모든 원격 참가자의 오디오 (항상 포함)
        // 이것도 마이크 오디오만 포함됨 (화면 공유 오디오는 별도 트랙이므로 자동으로 제외됨)
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
            // 회의 소유주가 아니면 녹음하지 않음
            if (!canEndMeeting) {
                if (voiceRecorderRef.current) {
                    await stopVoiceRecording();
                }
                voiceRecordingSourceIdRef.current = null;
                return;
            }

            const canRecord = isLoggedIn && numericStudyId && numericMeetingId;

            if (!canRecord) {
                if (voiceRecorderRef.current) {
                    await stopVoiceRecording();
                }
                voiceRecordingSourceIdRef.current = null;
                return;
            }

            const selection = getVoiceRecordingTrack();
            if (!selection) {
                if (voiceRecorderRef.current) {
                    await stopVoiceRecording();
                }
                voiceRecordingSourceIdRef.current = null;
                return;
            }
            
            const nextSourceId = selection.sourceId;
            if (voiceRecorderRef.current && voiceRecordingSourceIdRef.current === nextSourceId) {
                return;
            }
            
            if (voiceRecorderRef.current) {
                await stopVoiceRecording();
            }
            
            const stream = new MediaStream([selection.track]);
            startVoiceRecording(stream);
            voiceRecordingSourceIdRef.current = nextSourceId;
        });
    }, [
        canEndMeeting,
        getVoiceRecordingTrack,
        isLoggedIn,
        numericMeetingId,
        numericStudyId,
        startVoiceRecording,
        stopVoiceRecording,
    ]);

    const ensureMicrophoneStream = useCallback(
        async (silentMode: boolean) => {
            const effectiveSilentMode = silentMode && !micEnabledRef.current;
            const processedTrack = ensureMicProcessedTrack();
            await resumeMicContext();
            if (processedTrack) {
                await updateOutgoingAudio(processedTrack);
            }
            if (!navigator.mediaDevices?.getUserMedia) {
                if (!effectiveSilentMode) {
                    setMicEnabled(false);
                }
                return;
            }
            if (localMicStreamRef.current) {
                const existingTrack = localMicStreamRef.current.getAudioTracks()[0] ?? null;
                if (!existingTrack || existingTrack.readyState !== 'live') {
                    stopTracks(localMicStreamRef.current);
                    localMicStreamRef.current = null;
                }
            }
            if (localMicStreamRef.current) {
                await resumeMicContext();
                const track = localMicStreamRef.current.getAudioTracks()[0];
                if (track) {
                    track.enabled = true;
                }
                if (!effectiveSilentMode) {
                    setMicEnabled(true);
                }
                attachMicStreamToMixer(localMicStreamRef.current);
                if (micGainNodeRef.current) {
                    micGainNodeRef.current.gain.value = effectiveSilentMode ? 0 : 1;
                    console.log('[mic] set gain (existing stream)', {
                        silentMode: effectiveSilentMode,
                        gain: micGainNodeRef.current.gain.value,
                        micEnabled: micEnabledRef.current,
                        trackEnabled: track?.enabled,
                        trackMuted: track?.muted,
                        trackReadyState: track?.readyState,
                    });
                }
                if (!effectiveSilentMode && !audioDetectionActiveRef.current) {
                    audioDetectionActiveRef.current = Boolean(
                        await audioDetection.startDetection(localMicStreamRef.current, (isSpeaking) => {
                            if (speakingRef.current === isSpeaking) return;
                            speakingRef.current = isSpeaking;
                            updateSelfParticipant({ isSpeaking });
                            if (wsClientRef.current && roomIdRef.current) {
                                wsClientRef.current.setSpeaking(roomIdRef.current, { speaking: isSpeaking });
                            }
                        })
                    );
                }
                updateVoiceRecordingSource();
                return;
            }
            try {
                const stream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
                localMicStreamRef.current = stream;
                const track = stream.getAudioTracks()[0] ?? null;
                if (track) {
                    track.addEventListener('ended', () => {
                        console.log('[mic] track ended', { trackId: track.id });
                    });
                    track.addEventListener('mute', () => {
                        console.log('[mic] track muted', { trackId: track.id });
                    });
                    track.addEventListener('unmute', () => {
                        console.log('[mic] track unmuted', { trackId: track.id });
                    });
                }
                await resumeMicContext();
                if (!effectiveSilentMode) {
                    setMicEnabled(true);
                }
                attachMicStreamToMixer(stream);
                if (micGainNodeRef.current) {
                    micGainNodeRef.current.gain.value = effectiveSilentMode ? 0 : 1;
                    console.log('[mic] set gain (new stream)', {
                        silentMode: effectiveSilentMode,
                        gain: micGainNodeRef.current.gain.value,
                        micEnabled: micEnabledRef.current,
                    });
                }
                updateVoiceRecordingSource();
                if (!effectiveSilentMode) {
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
                }
            } catch (error) {
                console.error('Failed to access microphone', error);
                if (!effectiveSilentMode) {
                    setMicEnabled(false);
                }
            }
        },
        [
            attachMicStreamToMixer,
            ensureMicProcessedTrack,
            resumeMicContext,
            updateOutgoingAudio,
            updateSelfParticipant,
            updateVoiceRecordingSource,
        ]
    );

    const refreshOutgoingAudio = useCallback(async () => {
        const processedTrack = ensureMicProcessedTrack();
        if (processedTrack && processedTrack.readyState === 'live') {
            await updateOutgoingAudio(processedTrack);
        }
    }, [ensureMicProcessedTrack, updateOutgoingAudio]);

    const requestCameraPermission = useCallback(async () => {
        if (!navigator.mediaDevices?.getUserMedia) return;
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
            stream.getTracks().forEach((track) => track.stop());
        } catch (error) {
            console.error('Failed to access camera permission', error);
        }
    }, []);

    const requestDevicePermissions = useCallback(async () => {
        if (devicePermissionRequestedRef.current) return;
        devicePermissionRequestedRef.current = true;
        await ensureMicrophoneStream(true);
        await requestCameraPermission();
    }, [ensureMicrophoneStream, requestCameraPermission]);

    const startMicrophone = useCallback(async () => {
        console.log('[mic] start requested', { micEnabled: micEnabledRef.current });
        await ensureMicrophoneStream(false);
    }, [ensureMicrophoneStream]);

    const stopMicrophone = useCallback(async () => {
        console.log('[mic] stop requested', { micEnabled: micEnabledRef.current });
        audioDetection.stopDetection();
        audioDetectionActiveRef.current = false;
        speakingRef.current = false;
        updateSelfParticipant({ isSpeaking: false });
        if (wsClientRef.current && roomIdRef.current) {
            wsClientRef.current.setSpeaking(roomIdRef.current, { speaking: false });
        }
        const track = localMicStreamRef.current?.getAudioTracks()?.[0] ?? null;
        if (track) {
            track.enabled = true;
        }
        if (micGainNodeRef.current) {
            micGainNodeRef.current.gain.value = 0;
        } else if (!track) {
            stopTracks(localMicStreamRef.current);
            localMicStreamRef.current = null;
        }
        setMicEnabled(false);
        updateVoiceRecordingSource();
    }, [ensureMicrophoneStream, updateSelfParticipant, updateVoiceRecordingSource]);

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
                        if (isPresenterRef.current) {
                            void refreshOutgoingAudio();
                        }
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
        [refreshOutgoingAudio, updateOutgoingVideo, updateVoiceRecordingSource]
    );

    const stopScreenShare = useCallback(async () => {
        stopTracks(localScreenStreamRef.current);
        localScreenStreamRef.current = null;
        setScreenSharing(false);
        screenSharingRef.current = false;
        clearMixedRetry();
        if (isPresenterRef.current) {
            await refreshOutgoingAudio();
        }
        await updateOutgoingVideo({ nextScreenStream: null });
        updateVoiceRecordingSource();
    }, [clearMixedRetry, refreshOutgoingAudio, updateOutgoingVideo, updateVoiceRecordingSource]);

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
            updateOutgoingVideo({ publish: false, cameraEnabledOverride: false, nextScreenStream: null });
            void refreshOutgoingAudio();
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
    }, [isPresenter, refreshOutgoingAudio, stopMixedAudioTrack, updateOutgoingVideo]);

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
        if (isPresenter || wasPresenter) {
            console.log('[mic] presenter change', {
                isPresenter,
                micEnabled: micEnabledRef.current,
                gain: micGainNodeRef.current?.gain?.value,
                micTrack: localMicStreamRef.current?.getAudioTracks()?.[0]?.readyState,
            });
            if (micEnabledRef.current) {
                void ensureMicrophoneStream(false);
            }
        }
    }, [cameraEnabled, ensureMicrophoneStream, isPresenter, stopCameraPublish, stopScreenShare]);

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

    const handleCapture = useCallback(async () => {
        if (!isLoggedIn) {
            showToast('로그인 후 캡처할 수 있습니다.', 'warning');
            return;
        }
        if (!numericStudyId || !numericMeetingId || isCapturing) return;
        const video = videoStageRef.current?.querySelector('video');
        if (!video) {
            showToast('캡처할 화면이 없습니다.', 'warning');
            return;
        }
        setIsCapturing(true);
        try {
            const blob = await captureFrame(video);
            if (!blob) {
                showToast('캡처할 화면이 없습니다.', 'warning');
                return;
            }
            const file = new File([blob], 'meeting-capture.png', { type: 'image/png' });
            await meetingApi.addPhoto(numericStudyId, numericMeetingId, file);
        } catch (error) {
            console.error('Failed to capture meeting screen', error);
        } finally {
            setIsCapturing(false);
        }
    }, [isCapturing, isLoggedIn, numericMeetingId, numericStudyId, showToast]);

    const handleExtendMeeting = useCallback(async () => {
        if (!numericStudyId || !numericMeetingId || !canEndMeeting) return;
        const currentSeconds = plannedDurationSeconds ?? 3600;
        if (currentSeconds >= MAX_PLANNED_DURATION_SECONDS) {
            return;
        }
        const nextSeconds = Math.min(currentSeconds + 1800, MAX_PLANNED_DURATION_SECONDS);
        try {
            const detail = await meetingApi.updatePlannedDuration(numericStudyId, numericMeetingId, nextSeconds);
            setPlannedDurationSeconds(detail.plannedDurationSeconds ?? nextSeconds);
        } catch (error) {
            console.error('Failed to extend meeting duration', error);
        }
    }, [
        MAX_PLANNED_DURATION_SECONDS,
        canEndMeeting,
        numericMeetingId,
        numericStudyId,
        plannedDurationSeconds,
    ]);

    const presenterLabel = presenterName ? '발표자: ' + presenterName : '발표자';
    const elapsedLabel = formatDuration(elapsedSeconds);
    const plannedLabel = plannedDurationSeconds ? formatPlannedDuration(plannedDurationSeconds) : null;

    const safeFinalizeVoiceRecording = useCallback(async (timeoutMs = 2000) => {
        try {
            await Promise.race([
                finalizeVoiceRecording(),
                new Promise<void>((resolve) => window.setTimeout(resolve, timeoutMs)),
            ]);
        } catch (error) {
            console.warn('Finalize voice recording skipped', error);
        }
    }, [finalizeVoiceRecording]);

    const endMeetingInternal = useCallback(
        async (requireConfirm: boolean) => {
            if (!numericStudyId || !numericMeetingId || !canEndMeeting) return;
            if (requireConfirm) {
                const confirmed = window.confirm('미팅을 종료하시겠습니까?');
                if (!confirmed) return;
            }
            try {
                setIsEnding(true);
                await safeFinalizeVoiceRecording();
                await meetingApi.endMeeting(numericStudyId, numericMeetingId);
            } catch (error) {
                console.error('Failed to end meeting', error);
            } finally {
                stopCameraHardware();
                sessionStorage.setItem(`meeting-end-reload-${numericMeetingId}`, '1');
                navigate(`/study/${numericStudyId}/meetings/${numericMeetingId}`);
            }
        },
        [numericStudyId, numericMeetingId, canEndMeeting, navigate, stopCameraHardware, safeFinalizeVoiceRecording]
    );

    useEffect(() => {
        if (!meetingStartedAt) return;
        if (!plannedDurationSeconds) return;
        if (!canEndMeeting) return;
        if (autoEndTriggeredRef.current) return;
        if (elapsedSeconds >= plannedDurationSeconds) {
            autoEndTriggeredRef.current = true;
            void endMeetingInternal(false);
        }
    }, [elapsedSeconds, plannedDurationSeconds, meetingStartedAt, canEndMeeting, endMeetingInternal]);

    const handleEndMeeting = useCallback(() => {
        void endMeetingInternal(true);
    }, [endMeetingInternal]);

    const handleRoomEvent = useCallback(
        (event: MeetingRoomEvent) => {
            if (event.type === 'MEETING_ENDED') {
                setIsEnding(true);
                void (async () => {
                    await safeFinalizeVoiceRecording();
                    stopCameraHardware();
                    sessionStorage.setItem(`meeting-end-reload-${numericMeetingId}`, '1');
                    navigate(`/study/${numericStudyId}/meetings/${numericMeetingId}`);
                })();
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
            mergeParticipants,
            navigate,
            numericStudyId,
            numericMeetingId,
            presenterName,
            safeFinalizeVoiceRecording,
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
                setPlannedDurationSeconds(detail.plannedDurationSeconds ?? null);
            } catch (error) {
                if (cancelled) return;
                if (axios.isAxiosError(error) && error.response?.status === 404) {
                    setRoomGuardStatus('blocked');
                    setRoomGuardMessage('존재하지 않는 회의입니다.');
                    return;
                }
                setMeetingTitle(`미팅 ${numericMeetingId}`);
                setMeetingStartedAt(new Date().toISOString());
                setPlannedDurationSeconds(null);
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
            sfuBaseUrlRef.current = sfuBaseUrl;

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
                await requestDevicePermissions();
                await refreshOutgoingAudio();
                if (!cancelled) {
                    if (micEnabledRef.current) {
                        await startMicrophone();
                    }
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
            stopMicProcessedTrack();
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
        canEndMeeting, // 의존성 추가
        handleNewConsumer,
        handlePeerLeft,
        handleProducerClosed,
        handleRoomEvent,
        finalizeVoiceRecording,
        ensureMicrophoneStream,
        requestDevicePermissions,
        refreshOutgoingAudio,
        stopMicProcessedTrack,
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
        if (!micEnabled) return;
        if (localMicStreamRef.current) return;
        void startMicrophone();
    }, [micEnabled, startMicrophone]);

    useEffect(() => {
        updateVoiceRecordingSource();
    }, [
        micEnabled, // 마이크 상태 변경 시
        remoteAudioVersion, // 원격 참가자 오디오 변경 시
        updateVoiceRecordingSource
    ]);
    if (roomGuardStatus === 'blocked') {
        return (
            <UserLayoutV2>
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
            </UserLayoutV2>
        );
    }
    if (roomGuardStatus === 'checking') {
        return (
            <UserLayoutV2>
                <div className="meeting-room meeting-room__blocked">
                    <div className="meeting-room__blocked-card">
                        <p>{roomGuardMessage}</p>
                    </div>
                </div>
            </UserLayoutV2>
        );
    }

    return (
        <UserLayoutV2>
            <div className="meeting-room">
                {isEnding && (
                    <div className="meeting-room__ending-overlay">
                        <div className="meeting-room__ending-text">
                            미팅 종료중입니다. 잠시만 기다려주세요.
                        </div>
                    </div>
                )}
                <MeetingRoomHeader
                    meetingTitle={meetingTitle}
                    elapsedLabel={elapsedLabel}
                    plannedLabel={plannedLabel}
                    timeWarning={timeWarning}
                    isPresenter={isPresenter}
                    onGoList={() => navigate(meetingListPath)}
                    micEnabled={micEnabled}
                    micDisabled={false}
                    shareMode={shareMode}
                    onToggleMic={handleToggleMic}
                    onShareModeChange={handleShareModeChange}
                    onTogglePresenter={handleTogglePresenter}
                    onEndMeeting={handleEndMeeting}
                    onCapture={handleCapture}
                    captureDisabled={isCapturing || !isLoggedIn}
                    canEndMeeting={canEndMeeting}
                    canExtendMeeting={canEndMeeting}
                    extendDisabled={
                        plannedDurationSeconds !== null
                            ? plannedDurationSeconds >= MAX_PLANNED_DURATION_SECONDS
                            : true
                    }
                    onExtendMeeting={handleExtendMeeting}
                />

                <MeetingRoomContent
                    localStream={localStream}
                    localLabel={displayNameRef.current}
                    localIsPresenter={isPresenter}
                    videoStageRef={videoStageRef}
                    aiVideoRef={aiVideoRef}
                    remoteVideoStreams={remoteVideoStreams.map((item) => ({
                        id: item.id,
                        stream: item.stream,
                        label: presenterName ? presenterLabel : item.label,
                        isPresenter: Boolean(presenterName),
                    }))}
                    participants={participants}
                    presenterId={presenterId}
                    presenterName={presenterName}
                    chatMessages={chatMessages}
                    onSendChat={handleSendChat}
                    onDeleteChat={handleDeleteChat}
                    currentUserId={user?.id ?? null}
                    currentSender={displayNameRef.current}
                />
            </div>
        </UserLayoutV2>
    );
};

export default MeetingRoomPage;
