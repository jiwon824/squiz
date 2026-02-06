// 페이지 로딩 상태 관리 훅
import { useCallback, useEffect, useRef } from 'react';
import { useUIStore } from '@/store/uiStore';

interface UsePageLoadingOptions {
    /** 자동 진행률 시뮬레이션 활성화 (기본값: true) */
    autoProgress?: boolean;
    /** 진행률 업데이트 간격 (ms, 기본값: 300) */
    progressInterval?: number;
}

/**
 * 페이지 로딩 상태를 관리하는 훅
 * - 전역 로딩 상태와 연동
 * - 자동 진행률 시뮬레이션 지원
 * - 단계별 수동 진행률 설정 가능
 *
 * @example
 * // 기본 사용법 (자동 진행률)
 * const { startLoading, finishLoading } = usePageLoading();
 * useEffect(() => {
 *   startLoading();
 *   fetchData().finally(() => finishLoading());
 * }, []);
 *
 * @example
 * // 수동 진행률 설정 (미팅룸 진입 등)
 * const { startLoading, setProgress, finishLoading } = usePageLoading({ autoProgress: false });
 * // 미팅 정보 확인 완료
 * setProgress(30);
 * // 입장 완료
 * setProgress(60);
 * // SFU 연결 완료
 * setProgress(90);
 * // 완료
 * finishLoading();
 */
export const usePageLoading = (options: UsePageLoadingOptions = {}) => {
    const { autoProgress = true, progressInterval = 300 } = options;

    const {
        startLoading: storeStartLoading,
        setLoadingProgress,
        finishLoading: storeFinishLoading,
        globalLoading,
    } = useUIStore();

    const progressTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);
    const currentProgressRef = useRef(0);

    // 자동 진행률 시뮬레이션 중지
    const stopAutoProgress = useCallback(() => {
        if (progressTimerRef.current) {
            clearInterval(progressTimerRef.current);
            progressTimerRef.current = null;
        }
    }, []);

    // 자동 진행률 시뮬레이션 시작
    const startAutoProgress = useCallback(() => {
        if (!autoProgress) return;

        // 진행률 단계: 10 → 30 → 50 → 70 → 85 → 90 (90 이후 멈춤)
        const progressSteps = [10, 30, 50, 70, 85, 90];
        let stepIndex = 0;

        progressTimerRef.current = setInterval(() => {
            if (stepIndex < progressSteps.length) {
                const newProgress = progressSteps[stepIndex];
                currentProgressRef.current = newProgress;
                setLoadingProgress(newProgress);
                stepIndex++;
            } else {
                // 90%에 도달하면 타이머 정지 (완료 시까지 대기)
                stopAutoProgress();
            }
        }, progressInterval);
    }, [autoProgress, progressInterval, setLoadingProgress, stopAutoProgress]);

    // 로딩 시작
    const startLoading = useCallback(() => {
        currentProgressRef.current = 0;
        storeStartLoading();
        startAutoProgress();
    }, [storeStartLoading, startAutoProgress]);

    // 진행률 수동 설정
    const setProgress = useCallback(
        (progress: number) => {
            stopAutoProgress(); // 수동 설정 시 자동 진행 중지
            currentProgressRef.current = progress;
            setLoadingProgress(progress);
        },
        [setLoadingProgress, stopAutoProgress]
    );

    // 로딩 완료
    const finishLoading = useCallback(() => {
        stopAutoProgress();
        // 100%로 설정 후 완료 처리
        setLoadingProgress(100);
        // 약간의 딜레이 후 로딩 상태 종료 (100% 애니메이션 보여주기 위함)
        setTimeout(() => {
            storeFinishLoading();
            currentProgressRef.current = 0;
        }, 150);
    }, [setLoadingProgress, storeFinishLoading, stopAutoProgress]);

    // 컴포넌트 언마운트 시 타이머 정리
    useEffect(() => {
        return () => {
            stopAutoProgress();
        };
    }, [stopAutoProgress]);

    return {
        startLoading,
        setProgress,
        finishLoading,
        isLoading: globalLoading.isLoading,
        progress: globalLoading.progress,
    };
};
