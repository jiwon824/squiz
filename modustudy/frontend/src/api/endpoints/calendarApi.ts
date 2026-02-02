import api from '../axios';
import { ScheduleStatus } from '@/features/calendar/types';

/**
 * 개인 일정 API 응답 타입
 */
interface PersonalScheduleDTO {
    id: number;
    userId: number;
    title: string;
    description?: string;
    startDate: string; // YYYY-MM-DD
    startTime?: string; // HH:mm
    endDate?: string;
    endTime?: string;
    location?: string;
    isOnline: boolean;
    color?: string;
    googleEventId?: string;
    isSyncedWithGoogle?: boolean;
    lastSyncedAt?: string;
    createdAt: string;
    updatedAt: string;
}

/**
 * 스터디 세션 API 응답 타입 (백엔드 기존 타입)
 */
interface StudySessionDTO {
    id: number;
    studyId: number;
    sessionNumber: number;
    title: string;
    description?: string;
    scheduledAt: string; // ISO DateTime
    durationMinutes: number;
    location?: string;
    isOnline: boolean;
    status: ScheduleStatus;
    completedAt?: string;
    createdAt: string;
}

/**
 * Google Calendar 이벤트 응답 타입
 */
interface GoogleCalendarEventDTO {
    id: string;
    summary: string;
    description?: string;
    start: {
        date?: string; // YYYY-MM-DD (종일 이벤트)
        dateTime?: string; // ISO DateTime (시간 지정 이벤트)
    };
    end: {
        date?: string;
        dateTime?: string;
    };
    location?: string;
}

/**
 * 개인 일정 생성 요청
 */
interface CreatePersonalScheduleRequest {
    title: string;
    description?: string;
    startDate: string;
    startTime?: string;
    endDate?: string;
    endTime?: string;
    location?: string;
    isOnline?: boolean;
    color?: string;
    syncToGoogle?: boolean; // Google Calendar 동기화 여부 (생성/수정 시)
}

/**
 * 개인 일정 수정 요청
 */
interface UpdatePersonalScheduleRequest extends Partial<CreatePersonalScheduleRequest> { }

/**
 * 캘린더 API
 */
export const calendarApi = {
    // ==================== 개인 일정 ====================

    /**
     * 개인 일정 목록 조회
     * GET /api/v1/users/me/schedules?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD
     */
    getPersonalSchedules: async (startDate: string, endDate: string) => {
        const response = await api.get<{ success: boolean; data: PersonalScheduleDTO[] }>(
            '/api/v1/users/me/schedules',
            {
                params: { startDate, endDate },
                headers: {
                    'User-Id': localStorage.getItem('userId') || ''
                }
            }
        );
        return response.data.data;
    },

    /**
     * 개인 일정 단건 조회
     * GET /api/v1/users/me/schedules/{scheduleId}
     */
    getPersonalSchedule: async (scheduleId: number) => {
        const response = await api.get<{ success: boolean; data: PersonalScheduleDTO }>(
            `/api/v1/users/me/schedules/${scheduleId}`,
            {
                headers: {
                    'User-Id': localStorage.getItem('userId') || ''
                }
            }
        );
        return response.data.data;
    },

    /**
     * 개인 일정 생성
     * POST /api/v1/users/me/schedules
     */
    createPersonalSchedule: async (request: CreatePersonalScheduleRequest) => {
        const response = await api.post<{ success: boolean; data: PersonalScheduleDTO }>(
            '/api/v1/users/me/schedules',
            request,
            {
                headers: {
                    'User-Id': localStorage.getItem('userId') || ''
                }
            }
        );
        return response.data.data;
    },

    /**
     * 개인 일정 수정
     * PUT /api/v1/users/me/schedules/{scheduleId}
     */
    updatePersonalSchedule: async (scheduleId: number, request: UpdatePersonalScheduleRequest) => {
        const response = await api.put<{ success: boolean; data: PersonalScheduleDTO }>(
            `/api/v1/users/me/schedules/${scheduleId}`,
            request,
            {
                headers: {
                    'User-Id': localStorage.getItem('userId') || ''
                }
            }
        );
        return response.data.data;
    },

    /**
     * 개인 일정 삭제
     * DELETE /api/v1/users/me/schedules/{scheduleId}
     */
    deletePersonalSchedule: async (scheduleId: number) => {
        await api.delete(`/api/v1/users/me/schedules/${scheduleId}`, {
            headers: {
                'User-Id': localStorage.getItem('userId') || ''
            }
        });
    },

    // ==================== 스터디 세션 ====================

    /**
     * 내가 속한 스터디들의 세션 목록 조회
     * GET /api/v1/users/me/study-sessions?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD
     */
    getMyStudySessions: async (startDate: string, endDate: string) => {
        const response = await api.get<{ success?: boolean; data?: StudySessionDTO[] } | StudySessionDTO[]>(
            '/api/v1/users/me/study-sessions',
            {
                params: { startDate, endDate },
                headers: {
                    'User-Id': localStorage.getItem('userId') || ''
                }
            }
        );
        const data = response.data as any;
        return Array.isArray(data) ? data : (data?.data ?? []);
    },

    /**
     * 특정 스터디의 세션 목록 조회
     * GET /api/v1/studies/{studyId}/sessions?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD
     */
    getStudySessions: async (studyId: number, startDate?: string, endDate?: string) => {
        const response = await api.get<{ success?: boolean; data?: StudySessionDTO[] } | StudySessionDTO[]>(
            `/api/v1/studies/${studyId}/sessions`,
            {
                params: { startDate, endDate }
            }
        );
        const data = response.data as any;
        return Array.isArray(data) ? data : (data?.data ?? []);
    },

    // ==================== Google Calendar ====================

    /**
     * Google Calendar 연동 상태 확인
     * GET /api/v1/calendar/status
     */
    getGoogleCalendarStatus: async () => {
        const response = await api.get<{
            success: boolean;
            data: {
                connected: boolean;
                email?: string;
                hasValidToken?: boolean;
                calendarId?: string;
                tokenExpiresAt?: string;
            }
        }>(
            '/api/v1/calendar/status',
            {
                headers: {
                    'User-Id': localStorage.getItem('userId') || ''
                }
            }
        );
        return response.data.data;
    },

    /**
     * Google Calendar 연동 시작 (OAuth URL 반환)
     * GET /api/v1/calendar/google/auth-url
     */
    getGoogleAuthUrl: async () => {
        const response = await api.get<{ success: boolean; data: { authUrl: string } }>(
            '/api/v1/calendar/google/auth-url',
            {
                headers: {
                    'User-Id': localStorage.getItem('userId') || ''
                }
            }
        );
        return response.data.data.authUrl;
    },

    /**
     * Google Calendar 이벤트 조회
     * GET /api/v1/calendar/events?startTime=ISO&endTime=ISO
     */
    getGoogleCalendarEvents: async (startTime: string, endTime: string) => {
        const response = await api.get<{
            success: boolean;
            data: GoogleCalendarEventDTO[];
        }>(
            '/api/v1/calendar/events',
            {
                params: { startTime, endTime },
                headers: {
                    'User-Id': localStorage.getItem('userId') || ''
                }
            }
        );
        return response.data.data;
    },

    /**
     * Google Calendar 동기화 트리거 (Watch 등록)
     * POST /api/v1/calendar/sync
     */
    syncGoogleCalendar: async () => {
        const response = await api.post<{
            success: boolean;
            data: null;
            message?: string;
        }>(
            '/api/v1/calendar/sync',
            null,
            {
                headers: {
                    'User-Id': localStorage.getItem('userId') || ''
                }
            }
        );
        return response.data;
    },

    /**
     * Google Calendar 연동 해제
     * POST /api/v1/calendar/disconnect
     */
    disconnectGoogleCalendar: async () => {
        await api.post('/api/v1/calendar/disconnect', null, {
            headers: {
                'User-Id': localStorage.getItem('userId') || ''
            }
        });
    },

    // ==================== 통합 조회 ====================

    /**
     * 모든 일정 통합 조회 (개인 + 스터디 + Google)
     * GET /api/v1/calendar/all?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD
     */
    getAllSchedules: async (startDate: string, endDate: string) => {
        const response = await api.get<{
            success: boolean;
            data: {
                personal: PersonalScheduleDTO[];
                studySessions: StudySessionDTO[];
                googleEvents: GoogleCalendarEventDTO[];
            }
        }>(
            '/api/v1/calendar/all',
            {
                params: { startDate, endDate },
                headers: {
                    'User-Id': localStorage.getItem('userId') || ''
                }
            }
        );
        return response.data.data;
    }
};

export type {
    PersonalScheduleDTO,
    StudySessionDTO,
    GoogleCalendarEventDTO,
    CreatePersonalScheduleRequest,
    UpdatePersonalScheduleRequest
};
