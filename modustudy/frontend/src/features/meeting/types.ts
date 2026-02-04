export type MeetingType = 'DAILY' | 'WEEKLY' | 'FREE' | 'OTHER';
export type MeetingStatus = 'WAITING' | 'IN_PROGRESS' | 'ENDED';
export type RecordingStatus = 'WAITING' | 'RECORDING' | 'READY' | 'UPLOADING' | 'FAILED';
export type SttStatus = 'PENDING' | 'PROCESSING' | 'DONE' | 'FAILED';
export type SummaryStatus = 'PENDING' | 'PROCESSING' | 'DONE' | 'FAILED';
export type ActionItemStatus = 'TODO' | 'DONE';
export type MeetingAudioTrackType = 'MIXED' | 'INDIVIDUAL';
export type MeetingTextTrackType = 'MIXED' | 'INDIVIDUAL';

export interface ApiResponse<T> {
    success: boolean;
    data: T;
    error?: {
        code: string;
        message: string;
    };
}

export interface PageResponse<T> {
    status: number;
    message: string;
    content: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    first: boolean;
    last: boolean;
}

export interface MeetingSessionResponse {
    id: number;
    sessionNumber: number;
    title: string;
}

export interface MeetingWorkspaceResponse {
    id: number;
    name: string;
}

export interface MeetingUserResponse {
    id: number;
    nickname: string;
}

export interface MeetingParticipantResponse {
    userId: number;
    nickname: string;
    joinedAt: string | null;
    leftAt: string | null;
}

export interface MeetingListItemResponse {
    id: number;
    title: string;
    session: MeetingSessionResponse | null;
    meetingType: MeetingType;
    startedAt: string | null;
    endedAt: string | null;
    durationSeconds: number | null;
    participantCount: number;
    hasSummary: boolean;
    hasTranscript: boolean;
    photoCount: number;
}

export interface MeetingResponse {
    id: number;
    title: string;
    roomToken: string | null;
    status: MeetingStatus;
    meetingType: MeetingType;
    recordingStatus: RecordingStatus | null;
    sttStatus: SttStatus | null;
    summaryStatus: SummaryStatus | null;
}

export interface MeetingDetailResponse {
    id: number;
    title: string;
    session: MeetingSessionResponse | null;
    workspace: MeetingWorkspaceResponse | null;
    meetingType: MeetingType;
    startedAt: string | null;
    endedAt: string | null;
    durationSeconds: number | null;
    plannedDurationSeconds: number | null;
    status: MeetingStatus;
    recordingStatus: RecordingStatus | null;
    sttStatus: SttStatus | null;
    summaryStatus: SummaryStatus | null;
    autoShareSummary: boolean | null;
    shareWorkspaceId: number | null;
    participants: MeetingParticipantResponse[];
    keywords: string[];
    summary: MeetingSummaryResponse | null;
}

export interface MeetingJoinResponse {
    roomToken: string;
    iceServers: MeetingIceServerResponse[];
}

export interface MeetingIceServerResponse {
    urls: string;
    username?: string | null;
    credential?: string | null;
}

export interface SfuConfigResponse {
    baseUrl: string;
    iceServers: MeetingIceServerResponse[];
}

export interface MeetingChatMessageResponse {
    id: number;
    userId: number | null;
    senderName: string;
    content: string;
    sentAt: string;
}

export interface MeetingChatMessagePageResponse {
    content: MeetingChatMessageResponse[];
    totalElements: number;
    hasMore: boolean;
}

export interface MeetingSummaryResponse {
    id: number;
    summary: string;
    actionItems: MeetingActionItemResponse[];
    keywords: string[];
    highlights: string[];
    status: SummaryStatus;
    createdAt: string;
}

export interface MeetingActionItemResponse {
    id: number;
    content: string;
    assigneeId: number | null;
    status: ActionItemStatus;
}

export interface MeetingRecordingResponse {
    id: number;
    recordingUrl: string | null;
    format: string | null;
    durationSeconds: number | null;
    startedAt: string | null;
    endedAt: string | null;
    fileSize: number | null;
    status: RecordingStatus | null;
    createdAt: string | null;
}

export interface MeetingAudioRecordingResponse {
    id: number;
    meetingId: number;
    userId: number | null;
    trackType: MeetingAudioTrackType;
    recordingUrl: string | null;
    format: string | null;
    fileSize: number | null;
    createdAt: string | null;
}

export interface MeetingSttFileResponse {
    id: number;
    meetingId: number;
    userId: number | null;
    trackType: MeetingTextTrackType;
    fileUrl: string | null;
    createdAt: string | null;
    updatedAt: string | null;
}

export interface MeetingSttSummaryResponse {
    id: number;
    meetingId: number;
    userId: number | null;
    trackType: MeetingTextTrackType;
    fileUrl: string | null;
    createdAt: string | null;
    updatedAt: string | null;
}

export interface MeetingPhotoResponse {
    id: number;
    imageUrl: string;
    capturedAt: string;
    isSelected: boolean | null;
}

export interface MeetingTranscriptItemResponse {
    id: number;
    user: MeetingUserResponse | null;
    content: string;
    timestampSeconds: number;
    startMs: number | null;
    endMs: number | null;
    createdAt: string;
}

export interface MeetingTranscriptPageResponse {
    content: MeetingTranscriptItemResponse[];
    totalElements: number;
    hasMore: boolean;
}

export interface MeetingRoomParticipant {
    id: number;
    displayName: string;
    active: boolean;
    isSpeaking?: boolean;
    isPresent?: boolean;
    isPresenter?: boolean;
}

export interface MeetingRoomChatMessage {
    id?: number | null;
    userId?: number | null;
    sender: string;
    text: string;
    sentAt: string;
}

export type MeetingRoomEventType =
    | 'JOIN'
    | 'LEAVE'
    | 'CHAT'
    | 'CHAT_HISTORY'
    | 'CHAT_DELETED'
    | 'PRESENTER'
    | 'SPEAKING'
    | 'PRESENCE'
    | 'MEETING_ENDING'
    | 'MEETING_ENDED'
    | 'MEETING_DURATION_UPDATED';

export interface MeetingRoomEvent {
    type: MeetingRoomEventType;
    roomId: string;
    createdAt: string;
    participant?: MeetingRoomParticipant;
    participants?: MeetingRoomParticipant[];
    chat?: MeetingRoomChatMessage;
    chatHistory?: MeetingRoomChatMessage[];
    deletedChatId?: number | null;
    presenterName?: string;
    presenterId?: number | null;
    plannedDurationSeconds?: number | null;
}

export interface MeetingRequestPayload {
    title: string;
    sessionId?: number | null;
    workspaceId?: number | null;
    meetingType: MeetingType;
    autoShareSummary?: boolean;
    shareWorkspaceId?: number | null;
    plannedDurationSeconds?: number | null;
}

export interface MeetingActionItemRequest {
    content: string;
    assigneeId?: number | null;
    status: ActionItemStatus;
}

export interface MeetingSummaryUpdateRequest {
    summary: string;
    actionItems: MeetingActionItemRequest[];
    keywords: string[];
    status: SummaryStatus;
}

// 실시간 발화 자막 타입
export interface SpeechSegment {
    meetingId: number;
    speakerId: string;
    speakerName: string;
    timestamp: number;
    durationMs: number;
    text: string;
}
