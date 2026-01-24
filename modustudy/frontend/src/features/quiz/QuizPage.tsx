export const QuizPage = () => {
    return (
        <div style={{ padding: '2rem', textAlign: 'center', minHeight: '100vh' }}>
            <h1>온라인 퀴즈 대회</h1>
            <p>전국 스터디 팀과 경쟁하는 퀴즈 배틀 페이지입니다.</p>
            <button onClick={() => window.history.back()}>뒤로 가기</button>
        </div>
    );
};
