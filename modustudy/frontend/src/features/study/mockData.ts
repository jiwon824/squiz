// Mock 데이터 - 백엔드 API 연동 전까지 사용
// SQL 테스트 데이터를 JSON으로 변환

export const mockUsers = [
    {
        id: 1,
        email: 'leader1@ssafy.com',
        name: '김싸피',
        nickname: 'ssafy_kim',
        role: 'USER',
        isActive: true
    },
    {
        id: 2,
        email: 'leader2@ssafy.com',
        name: '이싸피',
        nickname: 'ssafy_lee',
        role: 'USER',
        isActive: true
    },
    {
        id: 3,
        email: 'leader3@ssafy.com',
        name: '박싸피',
        nickname: 'ssafy_park',
        role: 'USER',
        isActive: true
    },
    {
        id: 4,
        email: 'leader4@ssafy.com',
        name: '최싸피',
        nickname: 'ssafy_choi',
        role: 'USER',
        isActive: true
    },
    {
        id: 5,
        email: 'user5@ssafy.com',
        name: '정싸피',
        nickname: 'ssafy_jung',
        role: 'USER',
        isActive: true
    },
    {
        id: 6,
        email: 'user6@ssafy.com',
        name: '강싸피',
        nickname: 'ssafy_kang',
        role: 'USER',
        isActive: true
    }
];

export const mockRegions = [
    { id: 1, code: 'SEOUL', name: '서울', sortOrder: 1 },
    { id: 2, code: 'BUSAN', name: '부산', sortOrder: 2 },
    { id: 3, code: 'DAEJEON', name: '대전', sortOrder: 3 },
    { id: 4, code: 'GWANGJU', name: '광주', sortOrder: 4 },
    { id: 5, code: 'GUMI', name: '구미', sortOrder: 5 },
    { id: 6, code: 'DAEGU', name: '대구', sortOrder: 6 },
    { id: 7, code: 'INCHEON', name: '인천', sortOrder: 7 },
    { id: 8, code: 'ULSAN', name: '울산', sortOrder: 8 }
];

export const mockStudies = [
    {
        id: 1,
        leaderId: 1,
        name: '알고리즘 마스터',
        description: '백준 골드 문제 집중 풀이 스터디입니다',
        topic: '알고리즘',
        format: '문제풀이',
        studyType: 'PLANNED',
        meetingType: 'ONLINE',
        status: 'RECRUITING',
        isPublic: true,
        maxMembers: 6,
        currentMembers: 3,
        difficulty: 'INTERMEDIATE',
        scheduleDays: 'MON,WED,FRI',
        scheduleTime: '19:00:00',
        recruitStartDate: '2025-01-15',
        recruitEndDate: '2025-01-31',
        startDate: '2025-02-01',
        endDate: '2025-05-01',
        goal: '골드 티어 달성',
        textbook: '백준 온라인 저지',
        leader: {
            id: 1,
            nickname: 'ssafy_kim',
            profileImage: null,
            leaderRating: 4.5,
            leaderReviewCount: 10
        },
        curriculum: [
            { week: 1, description: '백준 골드 티어 달성을 위한 입문 문제 풀이 (탐색, 그리디)' },
            { week: 2, description: '동적 계획법(DP) 기초 및 핵심 유형 마스터' },
            { week: 3, description: '그래프 탐색 심화 (BFS, DFS 응용 및 최단 경로)' },
            { week: 4, description: '자료구조 활용 (스택, 큐, 트리, 힙)' },
            { week: 5, description: '실전 모의고사 및 코드 리뷰 집중 주간' }
        ],
        isBookmarked: false,
        createdAt: '2025-01-15T10:00:00.000Z'
    },
    {
        id: 2,
        leaderId: 1,
        name: 'CS 기초 탄탄',
        description: '운영체제, 네트워크 완벽 정리',
        topic: 'CS',
        format: '독서',
        studyType: 'PLANNED',
        meetingType: 'OFFLINE',
        regionId: 1,
        locationDetail: '강남역 스터디카페',
        status: 'RECRUITING',
        isPublic: true,
        maxMembers: 4,
        currentMembers: 2,
        difficulty: 'BEGINNER',
        scheduleDays: 'TUE,THU',
        scheduleTime: '20:00:00',
        recruitStartDate: '2025-01-15',
        recruitEndDate: '2025-01-25',
        startDate: '2025-02-01',
        endDate: '2025-04-01',
        goal: 'CS 기본 개념 마스터',
        textbook: '운영체제와 정보기술의 원리',
        region: {
            id: 1,
            name: '서울'
        },
        leader: {
            id: 1,
            nickname: 'ssafy_kim',
            profileImage: null,
            leaderRating: 4.5,
            leaderReviewCount: 10
        },
        isBookmarked: false,
        createdAt: '2025-01-16T14:30:00.000Z'
    },
    {
        id: 3,
        leaderId: 2,
        name: '스프링 부트 실전',
        description: 'Spring Boot 프로젝트 기반 학습',
        topic: '백엔드',
        format: '프로젝트',
        studyType: 'PLANNED',
        meetingType: 'HYBRID',
        regionId: 1,
        locationDetail: '역삼역 근처',
        status: 'IN_PROGRESS',
        isPublic: true,
        maxMembers: 8,
        currentMembers: 6,
        difficulty: 'INTERMEDIATE',
        scheduleDays: 'SAT,SUN',
        scheduleTime: '14:00:00',
        startDate: '2025-01-01',
        endDate: '2025-03-31',
        goal: '실전 프로젝트 완성',
        region: {
            id: 1,
            name: '서울'
        },
        leader: {
            id: 2,
            nickname: 'ssafy_lee',
            profileImage: null,
            leaderRating: 4.8,
            leaderReviewCount: 15
        },
        isBookmarked: false,
        createdAt: '2025-01-17T09:15:00.000Z'
    },
    {
        id: 4,
        leaderId: 2,
        name: '주말 코테 특강',
        description: '이번 주말 집중 코딩테스트 준비',
        topic: '알고리즘',
        format: '문제풀이',
        studyType: 'LIGHTNING',
        meetingType: 'ONLINE',
        status: 'RECRUITING',
        isPublic: true,
        maxMembers: 10,
        currentMembers: 7,
        difficulty: 'ADVANCED',
        scheduleDays: 'SAT',
        recruitStartDate: '2025-01-18',
        recruitEndDate: '2025-01-20',
        startDate: '2025-01-21',
        endDate: '2025-01-21',
        leader: {
            id: 2,
            nickname: 'ssafy_lee',
            profileImage: null,
            leaderRating: 4.8,
            leaderReviewCount: 15
        },
        isBookmarked: false,
        createdAt: '2025-01-18T16:45:00.000Z'
    },
    {
        id: 5,
        leaderId: 3,
        name: 'React 정복하기',
        description: 'React 18 신기능 마스터',
        topic: '프론트엔드',
        format: '강의수강',
        studyType: 'PLANNED',
        meetingType: 'ONLINE',
        status: 'COMPLETED',
        isPublic: true,
        maxMembers: 5,
        currentMembers: 5,
        difficulty: 'INTERMEDIATE',
        scheduleDays: 'MON,WED',
        startDate: '2024-10-01',
        endDate: '2024-12-31',
        goal: 'React 18 완벽 이해',
        leader: {
            id: 3,
            nickname: 'ssafy_park',
            profileImage: null,
            leaderRating: 4.3,
            leaderReviewCount: 8
        },
        isBookmarked: false,
        createdAt: '2024-12-20T11:20:00.000Z'
    },
    {
        id: 6,
        leaderId: 3,
        name: '자바 기초부터',
        description: '자바 기본 문법 완벽 정리',
        topic: '프로그래밍 기초',
        format: '강의수강',
        studyType: 'PLANNED',
        meetingType: 'OFFLINE',
        regionId: 3,
        locationDetail: '유성구 카페',
        status: 'RECRUITING',
        isPublic: true,
        maxMembers: 6,
        currentMembers: 4,
        difficulty: 'BEGINNER',
        scheduleDays: 'MON,WED,FRI',
        scheduleTime: '19:30:00',
        recruitStartDate: '2025-01-18',
        recruitEndDate: '2025-02-05',
        startDate: '2025-02-10',
        endDate: '2025-04-10',
        region: {
            id: 3,
            name: '대전'
        },
        leader: {
            id: 3,
            nickname: 'ssafy_park',
            profileImage: null,
            leaderRating: 4.3,
            leaderReviewCount: 8
        },
        isBookmarked: false,
        createdAt: '2025-01-18T08:00:00.000Z'
    },
    {
        id: 7,
        leaderId: 4,
        name: 'SQL 완벽 정복',
        description: 'MySQL부터 PostgreSQL까지',
        topic: '데이터베이스',
        format: '문제풀이',
        studyType: 'PLANNED',
        meetingType: 'ONLINE',
        status: 'RECRUITING',
        isPublic: true,
        maxMembers: 10,
        currentMembers: 5,
        difficulty: 'INTERMEDIATE',
        scheduleDays: 'TUE,THU',
        scheduleTime: '21:00:00',
        recruitStartDate: '2025-01-18',
        recruitEndDate: '2025-02-01',
        startDate: '2025-02-05',
        endDate: '2025-04-30',
        goal: 'SQL 고급 쿼리 마스터',
        textbook: 'SQL 첫걸음',
        leader: {
            id: 4,
            nickname: 'ssafy_choi',
            profileImage: null,
            leaderRating: 4.6,
            leaderReviewCount: 12
        },
        isBookmarked: false,
        createdAt: '2025-01-18T13:30:00.000Z'
    },
    {
        id: 8,
        leaderId: 4,
        name: '파이썬 데이터 분석',
        description: 'Pandas, NumPy 활용하기',
        topic: '데이터 분석',
        format: '프로젝트',
        studyType: 'PLANNED',
        meetingType: 'ONLINE',
        status: 'IN_PROGRESS',
        isPublic: true,
        maxMembers: 8,
        currentMembers: 6,
        difficulty: 'ELEMENTARY',
        scheduleDays: 'SAT',
        scheduleTime: '10:00:00',
        startDate: '2025-01-10',
        endDate: '2025-03-10',
        goal: '데이터 분석 기초 완성',
        leader: {
            id: 4,
            nickname: 'ssafy_choi',
            profileImage: null,
            leaderRating: 4.6,
            leaderReviewCount: 12
        },
        isBookmarked: false,
        createdAt: '2025-01-10T07:00:00.000Z'
    },
    {
        id: 9,
        leaderId: 1,
        name: '매우매우매우매우매우 길어서 두 줄을 훌쩍 넘어가 버리는 스터디 제목 테스트입니다. 과연 제목은 두 줄에서 잘릴까요? 세 줄이 될까요?',
        description: '이것은 아주아주아주아주 긴 설명글입니다. 한 줄, 두 줄, 세 줄, 네 줄을 넘어서 계속해서 길어지는 텍스트가 카드의 레이아웃을 무너뜨리지 않고 CSS line-clamp 속성에 의해 두 줄에서 우아하게 잘리는지 확인하기 위한 테스트용 목업 데이터입니다. 더 이상 길어질 수 없을 만큼 길게 작성해 보겠습니다.',
        topic: '테스트',
        format: '테스트',
        studyType: 'PLANNED',
        meetingType: 'HYBRID',
        regionId: 1,
        locationDetail: '테스트 서버',
        status: 'RECRUITING',
        isPublic: true,
        maxMembers: 99,
        currentMembers: 1,
        difficulty: 'ADVANCED',
        scheduleDays: 'MON,TUE,WED,THU,FRI,SAT,SUN',
        scheduleTime: '00:00:00',
        recruitStartDate: '2025-01-01',
        recruitEndDate: '2025-12-31',
        startDate: '2025-01-01',
        endDate: '2025-12-31',
        goal: '오버플로우 완벽 차단',
        region: { id: 1, name: '서울' },
        leader: {
            id: 1,
            nickname: 'overflow_tester',
            profileImage: null,
            leaderRating: 5.0,
            leaderReviewCount: 999
        },
        isBookmarked: false,
        createdAt: '2025-01-19T00:00:00.000Z'
    },
    {
        id: 10,
        leaderId: 2,
        name: '자바스크립트 딥다이브',
        description: '자바스크립트의 내부 원리를 파헤치는 스터디입니다',
        topic: '프론트엔드',
        format: '독서',
        studyType: 'PLANNED',
        meetingType: 'ONLINE',
        status: 'RECRUITING',
        isPublic: true,
        maxMembers: 5,
        currentMembers: 3,
        difficulty: 'INTERMEDIATE',
        scheduleDays: 'TUE,THU',
        recruitStartDate: '2025-01-20',
        recruitEndDate: '2025-01-30',
        startDate: '2025-02-01',
        leader: {
            id: 2,
            nickname: 'ssafy_lee',
            profileImage: null,
            leaderRating: 4.8,
            leaderReviewCount: 15
        },
        isBookmarked: false,
        createdAt: '2025-01-20T10:00:00.000Z'
    },
    {
        id: 11,
        leaderId: 3,
        name: '테라폼으로 배우는 IaC',
        description: 'Terraform을 활용한 클라우드 인프라 구축 실습',
        topic: '인프라',
        format: '프로젝트',
        studyType: 'PLANNED',
        meetingType: 'HYBRID',
        regionId: 1,
        status: 'RECRUITING',
        isPublic: true,
        maxMembers: 4,
        currentMembers: 1,
        difficulty: 'ADVANCED',
        scheduleDays: 'SAT',
        recruitStartDate: '2025-01-21',
        recruitEndDate: '2025-02-01',
        startDate: '2025-02-10',
        leader: {
            id: 3,
            nickname: 'ssafy_park',
            profileImage: null,
            leaderRating: 4.3,
            leaderReviewCount: 8
        },
        isBookmarked: false,
        createdAt: '2025-01-21T15:00:00.000Z'
    },
    {
        id: 12,
        leaderId: 4,
        name: 'Vue.js 기초 스터디',
        description: 'Vue 3 Composition API 기반으로 시작하기',
        topic: '프론트엔드',
        format: '강의수강',
        studyType: 'LIGHTNING',
        meetingType: 'ONLINE',
        status: 'RECRUITING',
        isPublic: true,
        maxMembers: 6,
        currentMembers: 2,
        difficulty: 'BEGINNER',
        scheduleDays: 'WED',
        recruitStartDate: '2025-01-22',
        recruitEndDate: '2025-01-28',
        startDate: '2025-02-01',
        leader: {
            id: 4,
            nickname: 'ssafy_choi',
            profileImage: null,
            leaderRating: 4.6,
            leaderReviewCount: 12
        },
        isBookmarked: false,
        createdAt: '2025-01-22T09:00:00.000Z'
    },
    {
        id: 13,
        leaderId: 1,
        name: '코틀린 맛보기',
        description: '자바 개발자를 위한 코틀린 핵심 문법',
        topic: '안드로이드',
        format: '문제풀이',
        studyType: 'PLANNED',
        meetingType: 'OFFLINE',
        regionId: 2,
        status: 'RECRUITING',
        isPublic: true,
        maxMembers: 4,
        currentMembers: 4,
        difficulty: 'INTERMEDIATE',
        scheduleDays: 'MON,FRI',
        recruitStartDate: '2025-01-23',
        recruitEndDate: '2025-02-10',
        startDate: '2025-02-15',
        leader: {
            id: 1,
            nickname: 'ssafy_kim',
            profileImage: null,
            leaderRating: 4.5,
            leaderReviewCount: 10
        },
        isBookmarked: false,
        createdAt: '2025-01-23T14:00:00.000Z'
    },
    {
        id: 14,
        leaderId: 2,
        name: '네트워크 면접 대비',
        description: '신입 개발자 면접을 위한 네트워크 총정리',
        topic: 'CS',
        format: '문제풀이',
        studyType: 'PLANNED',
        meetingType: 'ONLINE',
        status: 'RECRUITING',
        isPublic: true,
        maxMembers: 8,
        currentMembers: 5,
        difficulty: 'ADVANCED',
        scheduleDays: 'WED,SUN',
        recruitStartDate: '2025-01-24',
        recruitEndDate: '2025-02-05',
        startDate: '2025-02-15',
        leader: {
            id: 2,
            nickname: 'ssafy_lee',
            profileImage: null,
            leaderRating: 4.8,
            leaderReviewCount: 15
        },
        isBookmarked: false,
        createdAt: '2025-01-24T11:00:00.000Z'
    },
    {
        id: 15,
        leaderId: 3,
        name: '타입스크립트 시작하기',
        description: '점진화된 프로젝트를 위한 타입스크립트 스터디',
        topic: '프론트엔드',
        format: '프로젝트',
        studyType: 'LIGHTNING',
        meetingType: 'HYBRID',
        status: 'RECRUITING',
        isPublic: true,
        maxMembers: 10,
        currentMembers: 9,
        difficulty: 'BEGINNER',
        scheduleDays: 'THU',
        recruitStartDate: '2026-01-25',
        recruitEndDate: '2026-01-29',
        startDate: '2026-02-01',
        leader: {
            id: 3,
            nickname: 'ssafy_park',
            profileImage: null,
            leaderRating: 4.3,
            leaderReviewCount: 8
        },
        isBookmarked: false,
        createdAt: '2025-01-25T16:00:00.000Z'
    }
];

export interface Applicant {
    id: number;
    studyId: number;
    userId: number;
    nickname: string;
    message: string;
    status: 'PENDING' | 'APPROVED' | 'REJECTED';
    createdAt: string;
}

export interface StudyMember {
    id: number;
    studyId: number;
    userId: number;
    nickname: string;
    role: 'LEADER' | 'SUB_LEADER' | 'MEMBER';
    joinedAt: string;
    attendanceRate: number;
}

export const mockApplicants: Applicant[] = [
    {
        id: 1,
        studyId: 1,
        userId: 2,
        nickname: 'ssafy_lee',
        message: '안녕하세요! 알고리즘에 관심이 많아 신청합니다. 정말 열심히 하겠습니다!',
        status: 'PENDING',
        createdAt: '2025-01-20T10:00:00.000Z'
    },
    {
        id: 2,
        studyId: 1,
        userId: 3,
        nickname: 'ssafy_park',
        message: '알고리즘 골드 티어가 목표입니다. 꼭 뽑아주세요!',
        status: 'PENDING',
        createdAt: '2025-01-21T11:30:00.000Z'
    },
    {
        id: 3,
        studyId: 2,
        userId: 4,
        nickname: 'ssafy_choi',
        message: 'CS 기초를 탄탄히 다지고 싶습니다.',
        status: 'PENDING',
        createdAt: '2025-01-19T09:00:00.000Z'
    },
    {
        id: 4,
        studyId: 1,
        userId: 5,
        nickname: 'ssafy_jung',
        message: '안녕하세요! 파이썬 개발자인데 알고리즘 보완하고 싶어서 신청합니다.',
        status: 'PENDING',
        createdAt: '2025-01-22T14:20:00.000Z'
    },
    {
        id: 5,
        studyId: 1,
        userId: 6,
        nickname: 'ssafy_kang',
        message: '매일 참여 가능합니다! 꼭 같이 공부하고 싶어요.',
        status: 'PENDING',
        createdAt: '2025-01-23T09:15:00.000Z'
    }
];

export const mockMembers: StudyMember[] = [
    {
        id: 1,
        studyId: 1,
        userId: 1,
        nickname: 'ssafy_kim',
        role: 'LEADER',
        joinedAt: '2025-01-15T10:00:00.000Z',
        attendanceRate: 100
    },
    {
        id: 2,
        studyId: 1,
        userId: 4,
        nickname: 'ssafy_choi',
        role: 'SUB_LEADER',
        joinedAt: '2025-01-16T11:00:00.000Z',
        attendanceRate: 90
    }
];

// 유틸리티 함수들
export const getStudyById = (id: number) => {
    return mockStudies.find(study => study.id === id);
};

// 리더 리뷰 인터페이스
export interface LeaderReview {
    id: number;
    leaderId: number;
    reviewerId: number;
    reviewerNickname: string;
    reviewerProfileImage: string | null;
    studyId: number;
    studyName: string;
    rating: number;
    comment: string;
    createdAt: string;
}

// 리더별 리뷰 Mock 데이터
export const mockLeaderReviews: LeaderReview[] = [
    // 리더 1 (ssafy_kim) 리뷰들 - 평균 4.5
    {
        id: 1,
        leaderId: 1,
        reviewerId: 2,
        reviewerNickname: 'ssafy_lee',
        reviewerProfileImage: null,
        studyId: 1,
        studyName: '알고리즘 마스터',
        rating: 5,
        comment: '정말 체계적으로 스터디를 운영해주셔서 실력이 많이 늘었습니다. 질문에도 친절하게 답변해주시고 최고의 리더예요!',
        createdAt: '2025-01-10T10:00:00.000Z'
    },
    {
        id: 2,
        leaderId: 1,
        reviewerId: 3,
        reviewerNickname: 'ssafy_park',
        reviewerProfileImage: null,
        studyId: 1,
        studyName: '알고리즘 마스터',
        rating: 4,
        comment: '꼼꼼하게 피드백해주시고 동기부여도 많이 해주셨습니다. 다만 가끔 일정 조율이 어려웠어요.',
        createdAt: '2025-01-08T14:30:00.000Z'
    },
    {
        id: 3,
        leaderId: 1,
        reviewerId: 4,
        reviewerNickname: 'ssafy_choi',
        reviewerProfileImage: null,
        studyId: 1,
        studyName: '알고리즘 마스터',
        rating: 5,
        comment: '매주 과제 피드백이 정말 도움이 많이 됐어요. 덕분에 골드 티어 달성했습니다!',
        createdAt: '2025-01-05T09:15:00.000Z'
    },
    {
        id: 4,
        leaderId: 1,
        reviewerId: 5,
        reviewerNickname: 'ssafy_jung',
        reviewerProfileImage: null,
        studyId: 1,
        studyName: '알고리즘 마스터',
        rating: 4,
        comment: '전체적으로 만족스러운 스터디였습니다. 리더님이 열정적이세요.',
        createdAt: '2025-01-03T16:45:00.000Z'
    },
    // 리더 2 (ssafy_lee) 리뷰들 - 평균 4.8
    {
        id: 5,
        leaderId: 2,
        reviewerId: 1,
        reviewerNickname: 'ssafy_kim',
        reviewerProfileImage: null,
        studyId: 2,
        studyName: 'CS 기초 완성반',
        rating: 5,
        comment: '운영체제, 네트워크 개념을 정말 쉽게 설명해주셨어요. 면접 준비에 큰 도움이 됐습니다.',
        createdAt: '2025-01-12T11:00:00.000Z'
    },
    {
        id: 6,
        leaderId: 2,
        reviewerId: 3,
        reviewerNickname: 'ssafy_park',
        reviewerProfileImage: null,
        studyId: 2,
        studyName: 'CS 기초 완성반',
        rating: 5,
        comment: '자료도 잘 준비해오시고 질문에도 성심성의껏 답해주셨습니다. 강추!',
        createdAt: '2025-01-11T08:20:00.000Z'
    },
    // 리더 3 (ssafy_park) 리뷰들 - 평균 4.3
    {
        id: 7,
        leaderId: 3,
        reviewerId: 1,
        reviewerNickname: 'ssafy_kim',
        reviewerProfileImage: null,
        studyId: 5,
        studyName: 'React 프로젝트',
        rating: 4,
        comment: '실무에서 쓰이는 패턴들을 많이 배웠습니다. 코드 리뷰가 특히 좋았어요.',
        createdAt: '2025-01-09T13:00:00.000Z'
    },
    {
        id: 8,
        leaderId: 3,
        reviewerId: 2,
        reviewerNickname: 'ssafy_lee',
        reviewerProfileImage: null,
        studyId: 5,
        studyName: 'React 프로젝트',
        rating: 5,
        comment: '프론트엔드 개발에 자신감이 생겼습니다. 감사합니다!',
        createdAt: '2025-01-07T17:30:00.000Z'
    }
];

// 리더 ID로 리뷰 목록 조회
export const getReviewsByLeaderId = (leaderId: number): LeaderReview[] => {
    return mockLeaderReviews.filter(review => review.leaderId === leaderId);
};

// 리더 평균 평점 계산
export const getLeaderAverageRating = (leaderId: number): number => {
    const reviews = getReviewsByLeaderId(leaderId);
    if (reviews.length === 0) return 0;
    const sum = reviews.reduce((acc, review) => acc + review.rating, 0);
    return sum / reviews.length;
};

export const getStudiesByStatus = (status: string) => {
    return mockStudies.filter(study => study.status === status);
};

export const getRecruitingStudies = () => {
    return getStudiesByStatus('RECRUITING');
};

export const getStudiesByTopic = (topic: string) => {
    return mockStudies.filter(study => study.topic === topic);
};

export const searchStudies = (keyword: string) => {
    const lowerKeyword = keyword.toLowerCase();
    return mockStudies.filter(study =>
        study.name.toLowerCase().includes(lowerKeyword) ||
        study.description.toLowerCase().includes(lowerKeyword) ||
        study.topic.toLowerCase().includes(keyword.toLowerCase())
    );
};

export const getRegionById = (id: number) => {
    return mockRegions.find(region => region.id === id);
};

export const getUserById = (id: number) => {
    return mockUsers.find(user => user.id === id);
};
