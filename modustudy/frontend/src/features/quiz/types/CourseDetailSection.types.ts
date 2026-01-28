/**
 * =============================================================================
 * CourseDetailSection.types.ts - 코스 상세 페이지 타입 정의
 * =============================================================================
 * 
 * 목적 (PURPOSE):
 * 코스 상세 페이지에서 사용되는 타입들을 정의합니다.
 * API 응답 스키마에 맞춰 Section과 CourseDetail 인터페이스를 정의합니다.
 * 
 * =============================================================================
 */

import type { CourseCategory } from './QuizCourse.types';

// -----------------------------------------------------------------------------
// 섹션 상태 타입 (SECTION STATUS TYPE)
// -----------------------------------------------------------------------------
/**
 * SectionStatus - 섹션의 진행 상태를 나타내는 타입
 * 
 * - 'Locked': 잠김 상태 (이전 섹션을 완료해야 해금됨)
 * - 'InProgress': 진행 중 (해금되었지만 아직 완료하지 않음)
 * - 'Completed': 완료됨 (통과함)
 */
export type SectionStatus = 'Locked' | 'InProgress' | 'Completed';

// -----------------------------------------------------------------------------
// 코스 상세 섹션 인터페이스 (COURSE DETAIL SECTION INTERFACE)
// -----------------------------------------------------------------------------
/**
 * CourseDetailSection - 코스 상세 페이지의 섹션 인터페이스
 *
 * API 스키마 기반:
 * @property {number} sectionNumber - 섹션 번호 (1부터 시작)
 * @property {string} name - 섹션 이름
 * @property {boolean} isUnlocked - 해금 여부 (true면 시작 가능)
 * @property {boolean} isPassed - 통과 여부 (true면 완료됨)
 * @property {number | null} bestScore - 최고 점수 (시도하지 않았으면 null)
 * @property {number} attemptCount - 시도 횟수
 * @property {number | null} inProgressAttemptId - 진행 중인 시도 ID (없으면 null)
 */
export interface CourseDetailSection {
    sectionNumber: number;
    name: string;
    isUnlocked: boolean;
    isPassed: boolean;
    bestScore: number | null;
    attemptCount: number;
    inProgressAttemptId: number | null;
}

// -----------------------------------------------------------------------------
// 코스 상세 인터페이스 (COURSE DETAIL INTERFACE)
// -----------------------------------------------------------------------------
/**
 * CourseDetail - 코스 상세 페이지의 메인 데이터 인터페이스
 * 
 * @property {string} courseId - 코스의 고유 식별자
 * @property {string} name - 코스 이름 (한국어)
 * @property {string} description - 코스 설명
 * @property {CourseCategory} category - 코스 카테고리
 * @property {number} totalSections - 총 섹션 수
 * @property {CourseDetailSection[]} sections - 섹션 목록
 */
export interface CourseDetail {
    courseId: string;
    name: string;
    description: string;
    category: CourseCategory;
    totalSections: number;
    sections: CourseDetailSection[];
}

// -----------------------------------------------------------------------------
// 헬퍼 함수: 섹션 상태 결정 (HELPER: DETERMINE SECTION STATUS)
// -----------------------------------------------------------------------------
/**
 * getSectionStatus - 섹션의 상태를 결정하는 헬퍼 함수
 * 
 * @param section - CourseDetailSection 객체
 * @returns SectionStatus - 'Locked', 'InProgress', 또는 'Completed'
 */
export const getSectionStatus = (section: CourseDetailSection): SectionStatus => {
    if (section.isPassed) {
        return 'Completed';
    }
    if (section.isUnlocked) {
        return 'InProgress';
    }
    return 'Locked';
};
