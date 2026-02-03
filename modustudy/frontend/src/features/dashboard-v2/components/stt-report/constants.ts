// STT 미팅 리포트 상수 정의

import type { MeetingReport } from './types';

/** 화자별 색상 팔레트 (디자인 토큰 + 확장 색상) */
export const SPEAKER_COLORS = [
    'var(--color-primary)',       // 파랑
    'var(--color-secondary)',     // 초록
    'var(--color-accent)',        // 노랑
    'var(--color-error)',         // 빨강
    '#8B5CF6',                   // 보라
    '#EC4899',                   // 핑크
] as const;

/** Tailwind 클래스용 화자별 색상 */
export const SPEAKER_COLOR_CLASSES = [
    { bg: 'bg-primary', border: 'border-primary', text: 'text-primary' },
    { bg: 'bg-secondary', border: 'border-secondary', text: 'text-secondary' },
    { bg: 'bg-accent', border: 'border-accent', text: 'text-accent' },
    { bg: 'bg-error', border: 'border-error', text: 'text-error' },
    { bg: 'bg-[#8B5CF6]', border: 'border-[#8B5CF6]', text: 'text-[#8B5CF6]' },
    { bg: 'bg-[#EC4899]', border: 'border-[#EC4899]', text: 'text-[#EC4899]' },
] as const;

/** 화자별 색상 인덱스 반환 */
export const getSpeakerColorIndex = (speaker: string, participants: string[]): number => {
    const idx = participants.indexOf(speaker);
    return idx >= 0 ? idx % SPEAKER_COLORS.length : 0;
};

/** 화자별 CSS 변수 색상 반환 */
export const getSpeakerColor = (speaker: string, participants: string[]): string => {
    return SPEAKER_COLORS[getSpeakerColorIndex(speaker, participants)];
};

/** 화자별 Tailwind 클래스 반환 */
export const getSpeakerClasses = (speaker: string, participants: string[]) => {
    return SPEAKER_COLOR_CLASSES[getSpeakerColorIndex(speaker, participants)];
};

// Mock 데이터
export const MOCK_REPORTS: MeetingReport[] = [
    {
        id: 1,
        studyName: 'React 스터디',
        meetingTitle: '주간 회의 - React Hooks 심화',
        date: '2026-01-25',
        duration: '1시간 30분',
        participants: ['김철수', '이영희', '박민수', '정지원', '최현우'],
        participantCount: 5,
        summary: 'useState와 useEffect의 실행 순서, 클린업 함수의 동작 원리에 대해 논의했습니다. 특히 의존성 배열의 올바른 사용법과 일반적인 실수에 대해 깊이 있게 다루었습니다.',
        keywords: ['React Hooks', 'useEffect', '클린업 함수', '의존성 배열', 'useState'],
        highlights: [
            '의존성 배열이 빈 배열일 때 컴포넌트 마운트 시 한 번만 실행',
            'useEffect 클린업 함수는 언마운트 시 또는 다음 effect 실행 전에 호출',
            'useState의 함수형 업데이트를 사용하면 이전 상태에 안전하게 접근 가능',
            'custom hook을 활용하여 로직 재사용성 높이기',
        ],
        actionItems: [
            '다음 주까지 각자 custom hook 예제 1개씩 작성해오기',
            'useReducer vs useState 비교 자료 준비 (김철수)',
            'React 18 동시성 기능 발표 준비 (이영희)',
        ],
        transcript: [
            { speaker: '김철수', time: '00:00', text: '오늘은 React Hooks 심화 내용을 다뤄보겠습니다.' },
            { speaker: '이영희', time: '02:30', text: 'useEffect의 클린업 함수가 언제 실행되는지 정확히 모르겠어요.' },
            { speaker: '김철수', time: '03:15', text: '클린업 함수는 두 가지 경우에 실행됩니다. 컴포넌트가 언마운트될 때와 다음 effect가 실행되기 전에요.' },
            { speaker: '박민수', time: '05:00', text: '그럼 의존성 배열에 값이 있으면 그 값이 바뀔 때마다 클린업이 먼저 실행되는 거네요?' },
            { speaker: '김철수', time: '05:45', text: '맞습니다. 그래서 이벤트 리스너 등록 같은 경우 클린업에서 제거해줘야 해요.' },
        ],
    },
    {
        id: 2,
        studyName: 'TypeScript 스터디',
        meetingTitle: '제네릭과 유틸리티 타입',
        date: '2026-01-24',
        duration: '2시간',
        participants: ['김철수', '이영희', '박민수', '정지원'],
        participantCount: 4,
        summary: 'TypeScript의 제네릭 문법과 Partial, Pick 같은 유틸리티 타입 활용법을 학습했습니다. 실제 프로젝트에서 타입 안전성을 높이는 방법을 논의했습니다.',
        keywords: ['제네릭', 'Partial', 'Pick', '타입 추론', 'Omit', 'Record'],
        highlights: [
            '제네릭을 사용하면 재사용 가능한 컴포넌트를 만들 수 있음',
            'Partial<T>는 모든 속성을 선택적으로 만듦',
            'Pick<T, K>는 특정 속성만 선택하여 새 타입 생성',
            'Omit<T, K>는 특정 속성을 제외한 타입 생성',
        ],
        actionItems: [
            '기존 프로젝트에서 any 타입 제거하기',
            '유틸리티 타입 활용 예제 코드 공유하기',
        ],
        transcript: [
            { speaker: '이영희', time: '00:00', text: '오늘은 TypeScript 제네릭에 대해 알아보겠습니다.' },
            { speaker: '박민수', time: '01:00', text: '제네릭이 정확히 뭔가요?' },
            { speaker: '이영희', time: '01:30', text: '제네릭은 타입을 변수처럼 사용하는 기능이에요. 함수나 클래스를 정의할 때 타입을 나중에 지정할 수 있습니다.' },
        ],
    },
    {
        id: 3,
        studyName: 'JavaScript 스터디',
        meetingTitle: '비동기 프로그래밍 마스터',
        date: '2026-01-22',
        duration: '1시간 45분',
        participants: ['김철수', '최현우', '박민수'],
        participantCount: 3,
        summary: 'JavaScript의 비동기 처리 방식인 Promise, async/await에 대해 심도 있게 학습했습니다. 에러 핸들링과 동시성 제어에 대해서도 다루었습니다.',
        keywords: ['Promise', 'async/await', '비동기', 'try-catch', 'Promise.all'],
        highlights: [
            'Promise는 비동기 작업의 완료 또는 실패를 나타내는 객체',
            'async/await는 Promise를 더 읽기 쉽게 사용하는 문법',
            'Promise.all()로 여러 비동기 작업을 병렬 처리',
            'Promise.allSettled()는 모든 Promise가 처리될 때까지 대기',
        ],
        actionItems: [
            'API 호출 코드를 async/await로 리팩토링하기',
            '에러 핸들링 패턴 정리해서 공유하기',
        ],
        transcript: [
            { speaker: '최현우', time: '00:00', text: 'JavaScript 비동기 처리에 대해 복습해봅시다.' },
            { speaker: '박민수', time: '00:30', text: 'Promise와 async/await의 차이가 뭔가요?' },
            { speaker: '최현우', time: '01:00', text: 'async/await는 Promise를 더 동기적으로 보이게 작성하는 문법적 설탕이에요.' },
        ],
    },
];
