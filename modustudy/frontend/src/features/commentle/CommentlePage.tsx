export const CommentlePage = () => {
    return (
        <div style={{ padding: '2rem', textAlign: 'center', minHeight: '100vh' }}>
            <h1>꼬멘틀 CS ver</h1>
            <p>오늘의 컴퓨터 사이언스 용어 맞추기 퀴즈 페이지입니다.</p>
            <button onClick={() => window.history.back()}>뒤로 가기</button>
        </div>
    );
};
