import './LoadingFallback.css';

/**
 * Suspense fallback 로딩 컴포넌트
 * 코드 스플리팅으로 lazy 로드되는 페이지 로딩 시 표시
 */
export const LoadingFallback = () => {
    return (
        <div className="loading-fallback">
            <div className="loading-spinner"></div>
            <p className="loading-text">로딩 중...</p>
        </div>
    );
};
