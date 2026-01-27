import { create } from 'zustand';
import { RecruitmentPost, RecruitmentComment, ReportRequest } from './types';
import { useUIStore } from '@/store/uiStore';

interface RecruitmentStore {
    posts: RecruitmentPost[];
    comments: RecruitmentComment[];
    loading: boolean;

    // Actions
    addPost: (post: Omit<RecruitmentPost, 'id' | 'createdAt' | 'updatedAt' | 'views'>) => void;
    updatePost: (id: string, updates: Partial<RecruitmentPost>) => void;
    deletePost: (id: string) => void;
    addComment: (postId: string, content: string, author: { id: string; name: string; avatar?: string }) => void;
    deleteComment: (commentId: string) => void;
    report: (request: ReportRequest) => void;
    toggleComplete: (id: string) => void;
}

// Mock 초기 데이터
const MOCK_POSTS: RecruitmentPost[] = [
    {
        id: '1',
        title: 'React 실무 스터디원 모집합니다 (신입 환영)',
        content: '안녕하세요! 함께 React 프로젝트를 만들어보며 성장할 스터디원을 찾고 있습니다. 매주 토요일 오후 2시에 강남역 근처에서 오프라인으로 진행할 예정입니다.',
        authorId: 'user1',
        authorName: 'ReactMaster',
        authorAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=user1',
        category: 'study',
        tags: ['React', 'TypeScript', 'Frontend'],
        views: 124,
        createdAt: new Date(Date.now() - 3600000 * 2).toISOString(),
        updatedAt: new Date(Date.now() - 3600000 * 2).toISOString(),
        isCompleted: false,
        memberCount: 2,
        maxMembers: 4,
    },
    {
        id: '2',
        title: 'Spring Boot 백엔드 토이 프로젝트 모집',
        content: '간단한 커뮤니티 서비스를 함께 개발할 백엔드 개발자를 찾습니다. JPA와 QueryDSL을 주로 사용할 예정입니다.',
        authorId: 'user2',
        authorName: 'JavaDev',
        authorAvatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=user2',
        category: 'project',
        tags: ['Java', 'Spring Boot', 'Backend'],
        views: 89,
        createdAt: new Date(Date.now() - 3600000 * 24).toISOString(),
        updatedAt: new Date(Date.now() - 3600000 * 24).toISOString(),
        isCompleted: true,
        memberCount: 3,
        maxMembers: 3,
    }
];

export const useRecruitmentStore = create<RecruitmentStore>((set) => ({
    posts: MOCK_POSTS,
    comments: [],
    loading: false,

    addPost: (post) => set((state) => ({
        posts: [
            {
                ...post,
                id: Math.random().toString(36).substr(2, 9),
                createdAt: new Date().toISOString(),
                updatedAt: new Date().toISOString(),
                views: 0,
            },
            ...state.posts,
        ]
    })),

    updatePost: (id, updates) => set((state) => ({
        posts: state.posts.map((p) => (p.id === id ? { ...p, ...updates, updatedAt: new Date().toISOString() } : p))
    })),

    deletePost: (id) => set((state) => ({
        posts: state.posts.filter((p) => p.id !== id)
    })),

    addComment: (postId, content, author) => set((state) => ({
        comments: [
            ...state.comments,
            {
                id: Math.random().toString(36).substr(2, 9),
                postId,
                content,
                authorId: author.id,
                authorName: author.name,
                authorAvatar: author.avatar,
                createdAt: new Date().toISOString(),
            }
        ]
    })),

    deleteComment: (commentId) => set((state) => ({
        comments: state.comments.filter((c) => c.id !== commentId)
    })),

    report: (request) => {
        console.log('Report submitted:', request);
        useUIStore.getState().showToast('신고가 접수되었습니다.', 'success');
    },

    toggleComplete: (id) => set((state) => ({
        posts: state.posts.map((p) => (p.id === id ? { ...p, isCompleted: !p.isCompleted } : p))
    }))
}));
