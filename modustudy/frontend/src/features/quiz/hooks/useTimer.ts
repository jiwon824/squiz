import { useCallback, useRef, useState } from 'react';

const MAX_RESPONSE_TIME_MS = 30_000;

/**
 * 고정밀 타이머 훅 (performance.now 기반)
 * - start: 타이머 시작
 * - stop: 타이머 정지 후 경과 시간(ms) 반환 (최대 30초 캡)
 * - reset: 타이머 초기화
 */
export function useTimer() {
  const startTimeRef = useRef<number | null>(null);
  const [elapsedTime, setElapsedTime] = useState<number>(0);
  const [isRunning, setIsRunning] = useState(false);

  const start = useCallback(() => {
    startTimeRef.current = performance.now();
    setElapsedTime(0);
    setIsRunning(true);
    console.log('[useTimer] 타이머 시작');
  }, []);

  const stop = useCallback((): number => {
    if (startTimeRef.current === null) {
      console.warn('[useTimer] 타이머가 시작되지 않은 상태에서 stop 호출');
      return 0;
    }

    const raw = performance.now() - startTimeRef.current;
    const capped = Math.min(Math.round(raw), MAX_RESPONSE_TIME_MS);

    setElapsedTime(capped);
    setIsRunning(false);
    console.log(`[useTimer] 타이머 정지 — ${capped}ms (원시값: ${Math.round(raw)}ms)`);
    return capped;
  }, []);

  const reset = useCallback(() => {
    startTimeRef.current = null;
    setElapsedTime(0);
    setIsRunning(false);
    console.log('[useTimer] 타이머 초기화');
  }, []);

  return { elapsedTime, isRunning, start, stop, reset };
}
