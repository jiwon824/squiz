import { useCallback, useEffect, useState } from 'react';
import { useTimer } from './hooks/useTimer';
import {
  submitReview,
  type ReviewContentType,
  type ReviewSubmitResponse,
} from '@/api/endpoints/reviewApi';

// FSRS 상태 코드 → 한글 레이블
const STATE_LABELS: Record<number, string> = {
  0: 'New',
  1: 'Learning',
  2: 'Review',
  3: 'Relearning',
};

// --- 데모용 상수 (실제 환경에서는 라우트 파라미터 등으로 대체) ---
const DEMO_CONTENT_TYPE: ReviewContentType = 'COURSE_QUESTION';
const DEMO_CONTENT_ID = 1;

export default function ReviewQuizPage() {
  const { elapsedTime, isRunning, start, stop, reset } = useTimer();
  const [result, setResult] = useState<ReviewSubmitResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // 페이지 마운트 시 타이머 자동 시작
  useEffect(() => {
    start();
  }, [start]);

  const handleSubmit = useCallback(
    async (isCorrect: boolean) => {
      if (submitting) return;

      const responseTimeMs = stop();
      setSubmitting(true);
      setError(null);

      try {
        const data = await submitReview({
          contentType: DEMO_CONTENT_TYPE,
          contentId: DEMO_CONTENT_ID,
          isCorrect,
          responseTimeMs,
        });
        setResult(data);
        console.log('[ReviewQuizPage] FSRS 결과:', data);
      } catch (err) {
        const message = err instanceof Error ? err.message : '알 수 없는 오류';
        setError(message);
        console.error('[ReviewQuizPage] 제출 실패:', err);
      } finally {
        setSubmitting(false);
      }
    },
    [stop, submitting],
  );

  const handleReset = useCallback(() => {
    setResult(null);
    setError(null);
    reset();
    start();
  }, [reset, start]);

  return (
    <div style={{ maxWidth: 480, margin: '2rem auto', fontFamily: 'sans-serif' }}>
      <h2>FSRS 복습 PoC</h2>

      {/* 타이머 상태 */}
      <p>
        타이머: <strong>{isRunning ? '측정 중…' : `${elapsedTime}ms`}</strong>
      </p>

      {/* 제출 버튼 */}
      {!result && (
        <div style={{ display: 'flex', gap: 12, marginTop: 16 }}>
          <button
            onClick={() => handleSubmit(true)}
            disabled={submitting}
            style={{ padding: '8px 20px', background: '#4caf50', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer' }}
          >
            정답 제출
          </button>
          <button
            onClick={() => handleSubmit(false)}
            disabled={submitting}
            style={{ padding: '8px 20px', background: '#f44336', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer' }}
          >
            오답 제출
          </button>
        </div>
      )}

      {/* 에러 표시 */}
      {error && (
        <p style={{ color: 'red', marginTop: 12 }}>오류: {error}</p>
      )}

      {/* FSRS 결과 표시 */}
      {result && (
        <div style={{ marginTop: 20, padding: 16, border: '1px solid #ddd', borderRadius: 8 }}>
          <h3>FSRS 스케줄링 결과</h3>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <tbody>
              <tr><td>상태</td><td><strong>{STATE_LABELS[result.state] ?? result.state}</strong></td></tr>
              <tr><td>안정도 (S)</td><td>{result.stability.toFixed(2)}</td></tr>
              <tr><td>난이도 (D)</td><td>{result.difficulty.toFixed(2)}</td></tr>
              <tr><td>다음 복습까지</td><td>{result.scheduledDays}일</td></tr>
              <tr><td>다음 복습 일시</td><td>{result.nextReviewAt}</td></tr>
              <tr><td>응답 시간</td><td>{elapsedTime}ms</td></tr>
            </tbody>
          </table>
          <button
            onClick={handleReset}
            style={{ marginTop: 12, padding: '6px 16px', cursor: 'pointer' }}
          >
            다시 시도
          </button>
        </div>
      )}
    </div>
  );
}
