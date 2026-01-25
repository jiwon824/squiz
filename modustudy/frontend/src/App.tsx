// 전역 설정 (QueryClient, Theme, 전역 가드)

import { AppRouter } from './routes';
import { ToastContainer } from './shared/components';

const App = () => {
    return (
        <>
            <AppRouter />
            <ToastContainer />
        </>
    );
};

export default App;
