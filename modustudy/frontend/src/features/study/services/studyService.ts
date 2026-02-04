// Study Service - Mock 데이터를 사용한 스터디 관련 비즈니스 로직
import { mockStudies, mockRegions, mockUsers, mockApplicants, mockMembers, Applicant, StudyMember } from '../mockData';

export interface Study {
    id: number;
    leaderId: number;
    name: string;
    description: string;
    topic: string;
    format: string;
    studyType: string;
    meetingType: string;
    status: string;
    isPublic: boolean;
    maxMembers: number;
    currentMembers: number;
    difficulty: string;
    scheduleDays: string;
    scheduleTime?: string;
    regionId?: number;
    locationDetail?: string;
    // 모집 기간
    recruitStartDate?: string;
    recruitEndDate?: string;
    // 스터디 진행 기간
    startDate?: string;
    endDate?: string;
    // 추가 정보
    goal?: string;
    textbook?: string;
    prerequisites?: string;
    processDetail?: string;
    region?: {
        id: number;
        name: string;
    };
    leader: {
        id: number;
        nickname: string;
        profileImage: string | null;
        leaderRating: number | null; // null이면 리뷰가 없는 상태
        leaderReviewCount: number;
    };
    curriculum?: Array<{
        week: number;
        description: string;
    }>;
    isBookmarked: boolean;
    createdAt: string;
}

export interface FilterOptions {
    status?: string[];
    topic?: string[];
    meetingType?: string[];
    difficulty?: string[];
    studyType?: string[];
    regionId?: number[];
    keyword?: string;
}

export interface SortOption {
    field: 'createdAt' | 'currentMembers' | 'recruitEndDate';
    order: 'asc' | 'desc';
}

class StudyService {
    // 전체 스터디 목록 조회
    getAllStudies(): Study[] {
        return [...mockStudies];
    }

    // 스터디 ID로 조회
    getStudyById(id: number): Study | undefined {
        return mockStudies.find((study: Study) => study.id === id);
    }

    // 필터링된 스터디 목록 조회
    getFilteredStudies(filters: FilterOptions): Study[] {
        let filtered = [...mockStudies];

        // 상태 필터
        if (filters.status && filters.status.length > 0) {
            filtered = filtered.filter((study: Study) => filters.status!.includes(study.status));
        }

        // 주제 필터
        if (filters.topic && filters.topic.length > 0) {
            filtered = filtered.filter((study: Study) => filters.topic!.includes(study.topic));
        }

        // 미팅 타입 필터
        if (filters.meetingType && filters.meetingType.length > 0) {
            filtered = filtered.filter((study: Study) =>
                filters.meetingType!.includes(study.meetingType)
            );
        }

        // 난이도 필터
        if (filters.difficulty && filters.difficulty.length > 0) {
            filtered = filtered.filter((study: Study) => filters.difficulty!.includes(study.difficulty));
        }

        // 스터디 타입 필터
        if (filters.studyType && filters.studyType.length > 0) {
            filtered = filtered.filter((study: Study) => filters.studyType!.includes(study.studyType));
        }

        // 지역 필터
        if (filters.regionId && filters.regionId.length > 0) {
            filtered = filtered.filter(
                (study: Study) => study.regionId && filters.regionId!.includes(study.regionId)
            );
        }

        // 키워드 검색
        if (filters.keyword && filters.keyword.trim()) {
            const keyword = filters.keyword.toLowerCase();
            filtered = filtered.filter(
                (study: Study) =>
                    study.name.toLowerCase().includes(keyword) ||
                    study.description.toLowerCase().includes(keyword) ||
                    study.topic.toLowerCase().includes(keyword)
            );
        }

        return filtered;
    }

    // 정렬
    sortStudies(studies: Study[], sortOption: SortOption): Study[] {
        const sorted = [...studies];

        sorted.sort((a: Study, b: Study) => {
            let aValue: any;
            let bValue: any;

            switch (sortOption.field) {
                case 'createdAt':
                    aValue = new Date(a.createdAt).getTime();
                    bValue = new Date(b.createdAt).getTime();
                    break;
                case 'currentMembers':
                    aValue = a.currentMembers;
                    bValue = b.currentMembers;
                    break;
                case 'recruitEndDate':
                    aValue = a.recruitEndDate ? new Date(a.recruitEndDate).getTime() : 0;
                    bValue = b.recruitEndDate ? new Date(b.recruitEndDate).getTime() : 0;
                    break;
                default:
                    return 0;
            }

            if (sortOption.order === 'asc') {
                return aValue - bValue;
            } else {
                return bValue - aValue;
            }
        });

        return sorted;
    }

    // 모집중인 스터디만 조회
    getRecruitingStudies(): Study[] {
        return mockStudies.filter((study: Study) => study.status === 'RECRUITING');
    }

    // 진행중인 스터디만 조회
    getInProgressStudies(): Study[] {
        return mockStudies.filter((study: Study) => study.status === 'IN_PROGRESS');
    }

    // 찜하기 토글 (실제로는 API 호출, 여기서는 로컬 상태만 변경)
    toggleBookmark(studyId: number): boolean {
        const study = mockStudies.find((s: Study) => s.id === studyId);
        if (study) {
            study.isBookmarked = !study.isBookmarked;
            return study.isBookmarked;
        }
        return false;
    }

    // 지역 목록 조회
    getRegions() {
        return [...mockRegions];
    }

    // 사용자 정보 조회
    getUserById(userId: number) {
        return mockUsers.find((user: any) => user.id === userId);
    }

    // 페이지네이션
    paginateStudies(studies: Study[], page: number, pageSize: number) {
        const startIndex = (page - 1) * pageSize;
        const endIndex = startIndex + pageSize;
        const paginatedStudies = studies.slice(startIndex, endIndex);

        return {
            studies: paginatedStudies,
            totalCount: studies.length,
            totalPages: Math.ceil(studies.length / pageSize),
            currentPage: page,
            pageSize,
        };
    }

    // 스터디 신청 (API 시뮬레이션)
    async applyToStudy(_studyId: number, _message: string): Promise<{ success: boolean; message: string }> {
        // 비동기 통신 시뮬레이션 (1.5초 대기)
        return new Promise((resolve) => {
            setTimeout(() => {
                // 시뮬레이션을 위해 10% 확률로 실패 발생
                const isError = Math.random() < 0.1;

                if (isError) {
                    resolve({
                        success: false,
                        message: '서버와의 통신 중 오류가 발생했습니다. 다시 시도해주세요.'
                    });
                } else {
                    resolve({
                        success: true,
                        message: '스터디 신청이 완료되었습니다! 스터디장의 승인을 기다려주세요.'
                    });
                }
            }, 1500);
        });
    }

    // 신청자 목록 조회
    getApplicantsByStudyId(studyId: number): Applicant[] {
        return mockApplicants.filter(app => app.studyId === studyId);
    }

    // 신청 상태 업데이트
    updateApplicantStatus(applicantId: number, status: 'APPROVED' | 'REJECTED'): boolean {
        const applicant = mockApplicants.find(app => app.id === applicantId);
        if (applicant) {
            applicant.status = status;

            // 승인 시 멤버로 자동 추가
            if (status === 'APPROVED') {
                this.addMemberFromApplicant(applicant);
            }
            return true;
        }
        return false;
    }

    // 신청자 정보를 바탕으로 멤버 추가
    private addMemberFromApplicant(applicant: Applicant) {
        // 이미 멤버인지 확인
        const isAlreadyMember = mockMembers.some(
            m => m.studyId === applicant.studyId && m.userId === applicant.userId
        );

        if (!isAlreadyMember) {
            const newMember: StudyMember = {
                id: mockMembers.length > 0 ? Math.max(...mockMembers.map(m => m.id)) + 1 : 1,
                studyId: applicant.studyId,
                userId: applicant.userId,
                nickname: applicant.nickname,
                role: 'MEMBER',
                joinedAt: new Date().toISOString(),
                attendanceRate: 100 // 초기 출석률
            };
            mockMembers.push(newMember);

            // 실제 데이터라면 Study의 currentMembers도 증가시켜야 함
            const study = mockStudies.find(s => s.id === applicant.studyId);
            if (study) {
                study.currentMembers += 1;
            }
        }
    }

    // 멤버 강퇴/탈퇴 처리
    expelMember(studyId: number, userId: number): boolean {
        const index = mockMembers.findIndex(m => m.studyId === studyId && m.userId === userId);
        if (index !== -1) {
            mockMembers.splice(index, 1);
            const study = mockStudies.find(s => s.id === studyId);
            if (study && study.currentMembers > 0) {
                study.currentMembers -= 1;
            }
            return true;
        }
        return false;
    }

    // 멤버 목록 조회
    getMembersByStudyId(studyId: number): StudyMember[] {
        return mockMembers.filter(member => member.studyId === studyId);
    }

    // 유저가 가입한 스터디 목록 조회
    getStudiesByUserId(userId: number): Study[] {
        // 유저가 멤버로 참여 중인 스터디 ID 목록
        const memberStudyIds = mockMembers
            .filter(member => member.userId === userId)
            .map(member => member.studyId);

        // 유저가 리더인 스터디 ID 목록
        const leaderStudyIds = mockStudies
            .filter((study: Study) => study.leaderId === userId)
            .map(study => study.id);

        // 중복 제거 후 스터디 정보 반환
        const allStudyIds = [...new Set([...memberStudyIds, ...leaderStudyIds])];
        return mockStudies.filter((study: Study) => allStudyIds.includes(study.id));
    }
}

// 싱글톤 인스턴스 export
export const studyService = new StudyService();
