export interface RecruitmentPost {
    id: string;
    title: string;
    content: string;
    authorId: string;
    authorName: string;
    authorAvatar?: string;
    category: 'study' | 'project' | 'mentoring';
    tags: string[];
    views: number;
    createdAt: string;
    updatedAt: string;
    isCompleted: boolean;
    memberCount: number;
    maxMembers: number;
}

export interface RecruitmentComment {
    id: string;
    postId: string;
    authorId: string;
    authorName: string;
    authorAvatar?: string;
    content: string;
    createdAt: string;
}

export interface ReportRequest {
    targetId: string;
    targetType: 'post' | 'comment';
    reason: string;
    reporterId: string;
}
