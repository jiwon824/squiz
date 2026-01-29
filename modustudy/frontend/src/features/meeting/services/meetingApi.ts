import api from '@/api/axios';
import {
    ApiResponse,
    MeetingActionItemRequest,
    MeetingActionItemResponse,
    MeetingAudioRecordingResponse,
    MeetingChatMessagePageResponse,
    MeetingDetailResponse,
    MeetingJoinResponse,
    MeetingListItemResponse,
    MeetingPhotoResponse,
    MeetingRecordingResponse,
    MeetingRequestPayload,
    MeetingResponse,
    MeetingSttFileResponse,
    MeetingSttSummaryResponse,
    MeetingSummaryResponse,
    MeetingSummaryUpdateRequest,
    PageResponse,
    SfuConfigResponse,
} from '../types';

const buildMeetingPath = (studyId: number, meetingId?: number) => {
    if (meetingId) {
        return `/api/v1/studies/${studyId}/meetings/${meetingId}`;
    }
    return `/api/v1/studies/${studyId}/meetings`;
};

export const meetingApi = {
    async listMeetings(
        studyId: number,
        params?: {
            meetingType?: string;
            startDate?: string;
            endDate?: string;
            page?: number;
            size?: number;
        }
    ): Promise<PageResponse<MeetingListItemResponse>> {
        const { data } = await api.get<PageResponse<MeetingListItemResponse>>(buildMeetingPath(studyId), {
            params,
        });
        return data;
    },

    async getMeetingDetail(studyId: number, meetingId: number): Promise<MeetingDetailResponse> {
        const { data } = await api.get<ApiResponse<MeetingDetailResponse>>(buildMeetingPath(studyId, meetingId));
        return data.data;
    },

    async startMeeting(studyId: number, payload: MeetingRequestPayload): Promise<MeetingResponse> {
        const { data } = await api.post<ApiResponse<MeetingResponse>>(buildMeetingPath(studyId), payload);
        return data.data;
    },

    async endMeeting(studyId: number, meetingId: number): Promise<MeetingResponse> {
        const { data } = await api.put<ApiResponse<MeetingResponse>>(
            `${buildMeetingPath(studyId, meetingId)}/end`
        );
        return data.data;
    },

    async updatePlannedDuration(
        studyId: number,
        meetingId: number,
        plannedDurationSeconds: number
    ): Promise<MeetingDetailResponse> {
        const { data } = await api.put<ApiResponse<MeetingDetailResponse>>(
            `${buildMeetingPath(studyId, meetingId)}/duration`,
            { plannedDurationSeconds }
        );
        return data.data;
    },

    async joinMeeting(studyId: number, meetingId: number): Promise<MeetingJoinResponse> {
        const { data } = await api.post<ApiResponse<MeetingJoinResponse>>(
            `${buildMeetingPath(studyId, meetingId)}/join`
        );
        return data.data;
    },

    async leaveMeeting(studyId: number, meetingId: number): Promise<void> {
        await api.post(`${buildMeetingPath(studyId, meetingId)}/leave`);
    },

    async getSummary(studyId: number, meetingId: number): Promise<MeetingSummaryResponse> {
        const { data } = await api.get<ApiResponse<MeetingSummaryResponse>>(
            `${buildMeetingPath(studyId, meetingId)}/summary`
        );
        return data.data;
    },

    async upsertSummary(
        studyId: number,
        meetingId: number,
        payload: MeetingSummaryUpdateRequest
    ): Promise<MeetingSummaryResponse> {
        const { data } = await api.put<ApiResponse<MeetingSummaryResponse>>(
            `${buildMeetingPath(studyId, meetingId)}/summary`,
            payload
        );
        return data.data;
    },

    async getChatHistory(
        studyId: number,
        meetingId: number,
        params?: { page?: number; size?: number }
    ): Promise<MeetingChatMessagePageResponse> {
        const { data } = await api.get<ApiResponse<MeetingChatMessagePageResponse>>(
            `${buildMeetingPath(studyId, meetingId)}/chat`,
            { params }
        );
        return data.data;
    },

    async deleteChatMessage(
        studyId: number,
        meetingId: number,
        messageId: number
    ): Promise<void> {
        await api.delete(`${buildMeetingPath(studyId, meetingId)}/chat/${messageId}`);
    },

    async getRecording(studyId: number, meetingId: number): Promise<MeetingRecordingResponse> {
        const { data } = await api.get<ApiResponse<MeetingRecordingResponse>>(
            `${buildMeetingPath(studyId, meetingId)}/recording`
        );
        return data.data;
    },

    async upsertRecording(
        studyId: number,
        meetingId: number,
        payload: {
            recordingUrl?: string | null;
            format?: string | null;
            durationSeconds?: number | null;
            startedAt?: string | null;
            endedAt?: string | null;
            fileSize?: number | null;
            status?: string | null;
        }
    ): Promise<MeetingRecordingResponse> {
        const { data } = await api.put<ApiResponse<MeetingRecordingResponse>>(
            `${buildMeetingPath(studyId, meetingId)}/recording`,
            payload
        );
        return data.data;
    },

    async uploadRecordingVideo(
        studyId: number,
        meetingId: number,
        file: Blob
    ): Promise<MeetingRecordingResponse> {
        const formData = new FormData();
        formData.append('video', file, 'meeting.webm');
        const { data } = await api.post<ApiResponse<MeetingRecordingResponse>>(
            `${buildMeetingPath(studyId, meetingId)}/recording/video`,
            formData,
            { headers: { 'Content-Type': 'multipart/form-data' } }
        );
        return data.data;
    },

    async uploadRecordingAudio(
        studyId: number,
        meetingId: number,
        payload: {
            trackType: string;
            userId?: number | null;
            file: Blob;
        }
    ): Promise<MeetingAudioRecordingResponse> {
        const formData = new FormData();
        formData.append('trackType', payload.trackType);
        if (payload.userId !== undefined && payload.userId !== null) {
            formData.append('userId', String(payload.userId));
        }
        formData.append('audio', payload.file, 'audio.webm');
        const { data } = await api.post<ApiResponse<MeetingAudioRecordingResponse>>(
            `${buildMeetingPath(studyId, meetingId)}/recording/audio`,
            formData,
            { headers: { 'Content-Type': 'multipart/form-data' } }
        );
        return data.data;
    },

    async uploadRecordingAudioSegment(
        studyId: number,
        meetingId: number,
        file: Blob
    ): Promise<void> {
        const formData = new FormData();
        formData.append('audio', file, 'segment.webm');
        await api.post(`${buildMeetingPath(studyId, meetingId)}/recording/audio/segment`, formData, {
            headers: { 'Content-Type': 'multipart/form-data' },
        });
    },

    async concatRecordingAudio(
        studyId: number,
        meetingId: number
    ): Promise<MeetingAudioRecordingResponse> {
        const { data } = await api.post<ApiResponse<MeetingAudioRecordingResponse>>(
            `${buildMeetingPath(studyId, meetingId)}/recording/audio/concat`
        );
        return data.data;
    },

    async listAudioRecordings(
        studyId: number,
        meetingId: number,
        params?: { trackType?: string; userId?: number }
    ): Promise<MeetingAudioRecordingResponse[]> {
        const { data } = await api.get<ApiResponse<MeetingAudioRecordingResponse[]>>(
            `${buildMeetingPath(studyId, meetingId)}/recording/audio`,
            { params }
        );
        return data.data;
    },

    async uploadSttFile(
        studyId: number,
        meetingId: number,
        payload: {
            trackType: string;
            userId?: number | null;
            file: Blob;
        }
    ): Promise<MeetingSttFileResponse> {
        const formData = new FormData();
        formData.append('trackType', payload.trackType);
        if (payload.userId !== undefined && payload.userId !== null) {
            formData.append('userId', String(payload.userId));
        }
        formData.append('file', payload.file, 'stt.txt');
        const { data } = await api.post<ApiResponse<MeetingSttFileResponse>>(
            `${buildMeetingPath(studyId, meetingId)}/stt/file`,
            formData,
            { headers: { 'Content-Type': 'multipart/form-data' } }
        );
        return data.data;
    },

    async uploadSummaryFile(
        studyId: number,
        meetingId: number,
        payload: {
            trackType: string;
            userId?: number | null;
            file: Blob;
        }
    ): Promise<MeetingSttSummaryResponse> {
        const formData = new FormData();
        formData.append('trackType', payload.trackType);
        if (payload.userId !== undefined && payload.userId !== null) {
            formData.append('userId', String(payload.userId));
        }
        formData.append('file', payload.file, 'summary.txt');
        const { data } = await api.post<ApiResponse<MeetingSttSummaryResponse>>(
            `${buildMeetingPath(studyId, meetingId)}/summary/file`,
            formData,
            { headers: { 'Content-Type': 'multipart/form-data' } }
        );
        return data.data;
    },

    async getSttFile(
        studyId: number,
        meetingId: number,
        params: { trackType: string; userId?: number }
    ): Promise<MeetingSttFileResponse> {
        const { data } = await api.get<ApiResponse<MeetingSttFileResponse>>(
            `${buildMeetingPath(studyId, meetingId)}/stt/file`,
            { params }
        );
        return data.data;
    },

    async getSummaryFile(
        studyId: number,
        meetingId: number,
        params: { trackType: string; userId?: number }
    ): Promise<MeetingSttSummaryResponse> {
        const { data } = await api.get<ApiResponse<MeetingSttSummaryResponse>>(
            `${buildMeetingPath(studyId, meetingId)}/summary/file`,
            { params }
        );
        return data.data;
    },

    async getPhotos(studyId: number, meetingId: number): Promise<MeetingPhotoResponse[]> {
        const { data } = await api.get<ApiResponse<MeetingPhotoResponse[]>>(
            `${buildMeetingPath(studyId, meetingId)}/photos`
        );
        return data.data;
    },

    async addPhoto(studyId: number, meetingId: number, file: Blob): Promise<MeetingPhotoResponse> {
        const formData = new FormData();
        formData.append('image', file, 'meeting-capture.png');
        const { data } = await api.post<ApiResponse<MeetingPhotoResponse>>(
            `${buildMeetingPath(studyId, meetingId)}/photos`,
            formData,
            { headers: { 'Content-Type': 'multipart/form-data' } }
        );
        return data.data;
    },

    async selectPhoto(studyId: number, meetingId: number, photoId: number): Promise<MeetingPhotoResponse> {
        const { data } = await api.put<ApiResponse<MeetingPhotoResponse>>(
            `${buildMeetingPath(studyId, meetingId)}/photos/${photoId}/select`
        );
        return data.data;
    },

    async selectPhotos(
        studyId: number,
        meetingId: number,
        photoIds: number[]
    ): Promise<MeetingPhotoResponse[]> {
        const { data } = await api.put<ApiResponse<MeetingPhotoResponse[]>>(
            `${buildMeetingPath(studyId, meetingId)}/photos/selection`,
            { photoIds }
        );
        return data.data;
    },

    async getActionItems(studyId: number, meetingId: number): Promise<MeetingActionItemResponse[]> {
        const { data } = await api.get<ApiResponse<MeetingActionItemResponse[]>>(
            `${buildMeetingPath(studyId, meetingId)}/action-items`
        );
        return data.data;
    },

    async addActionItem(
        studyId: number,
        meetingId: number,
        payload: MeetingActionItemRequest
    ): Promise<MeetingActionItemResponse> {
        const { data } = await api.post<ApiResponse<MeetingActionItemResponse>>(
            `${buildMeetingPath(studyId, meetingId)}/action-items`,
            payload
        );
        return data.data;
    },

    async updateActionItem(
        studyId: number,
        meetingId: number,
        actionItemId: number,
        payload: MeetingActionItemRequest
    ): Promise<MeetingActionItemResponse> {
        const { data } = await api.put<ApiResponse<MeetingActionItemResponse>>(
            `${buildMeetingPath(studyId, meetingId)}/action-items/${actionItemId}`,
            payload
        );
        return data.data;
    },

    async exportMeeting(
        studyId: number,
        meetingId: number,
        format: 'MARKDOWN' | 'PDF'
    ): Promise<Blob> {
        const { data } = await api.get(`${buildMeetingPath(studyId, meetingId)}/export`, {
            params: { format },
            responseType: 'blob',
        });
        return data;
    },

    async getSfuConfig(): Promise<SfuConfigResponse> {
        const { data } = await api.get<SfuConfigResponse>('/api/v1/sfu/config');
        return data;
    },
};
