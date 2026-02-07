import { useCallback, useEffect, useRef, useState } from 'react';

const MAX_RESPONSE_TIME_MS = 30_000;

/**
 * Page Visibility API를 지원하는 고정밀 타이머 훅
 */
export function useTimer() {
  // 전체 누적 경과 시간 (ms)
  const [elapsedTime, setElapsedTime] = useState<number>(0);
  const [isRunning, setIsRunning] = useState(false);

  // 마지막으로 타이머가 시작(혹은 재개)된 시점
  const startTimeRef = useRef<number | null>(null);
  // 일시정지 전까지 쌓인 누적 시간
  const accumulatedTimeRef = useRef<number>(0);

  // 타이머 시작
  const start = useCallback(() => {
    startTimeRef.current = performance.now();
    accumulatedTimeRef.current = 0;
    setElapsedTime(0);
    setIsRunning(true);
  }, []);

  // 타이머 정지 및 최종 시간 반환
  const stop = useCallback((): number => {
    // 타이머가 실행 중이지 않고, 누적 시간도 없으면(시작조차 안 함) 경고
    if ((!isRunning && accumulatedTimeRef.current === 0) || (isRunning && startTimeRef.current === null)) {
      return accumulatedTimeRef.current;
    }

    const currentSession = performance.now() - (startTimeRef.current ?? 0);
    const total = Math.round(accumulatedTimeRef.current + currentSession);
    const capped = Math.min(total, MAX_RESPONSE_TIME_MS);

    setElapsedTime(capped);
    setIsRunning(false);
    startTimeRef.current = null;

    return capped;
  }, [isRunning]);

  // 타이머 초기화
  const reset = useCallback(() => {
    startTimeRef.current = null;
    accumulatedTimeRef.current = 0;
    setElapsedTime(0);
    setIsRunning(false);
  }, []);

  // 탭 전환(Visibility) 감지 로직
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (!isRunning) return;

      if (document.hidden) {
        // 1. 탭을 나감: 지금까지의 세션 시간을 누적시키고 시작점 초기화
        if (startTimeRef.current !== null) {
          accumulatedTimeRef.current += (performance.now() - startTimeRef.current);
          startTimeRef.current = null;
        }
      } else {
        // 2. 탭으로 돌아옴: 새로운 시작점 기록 (재개)
        startTimeRef.current = performance.now();
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [isRunning]);

  // 타이머 일시정지
  const pause = useCallback(() => {
    if (!isRunning || startTimeRef.current === null) return;

    accumulatedTimeRef.current += performance.now() - startTimeRef.current;
    startTimeRef.current = null;
    setIsRunning(false);
  }, [isRunning]);

  // 타이머 재개
  const resume = useCallback(() => {
    if (isRunning || startTimeRef.current !== null) return;

    startTimeRef.current = performance.now();
    setIsRunning(true);
  }, [isRunning]);

  return { elapsedTime, isRunning, start, stop, reset, pause, resume };
}