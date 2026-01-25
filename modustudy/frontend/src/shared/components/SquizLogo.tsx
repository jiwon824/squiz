
import { DotLottieReact } from '@lottiefiles/dotlottie-react';
// .lottie 파일을 import하면 Vite가 자동으로 파일 경로(URL)를 반환해줍니다.
import logoAnimation from '../../assets/logos/squiz new logo.lottie';

interface SquizLogoProps {
    width?: number | string;
    height?: number | string;
    loop?: boolean;
    autoplay?: boolean;
    className?: string;
}

export const SquizLogo = ({
    width = 200,
    height = 200,
    loop = true,
    autoplay = true,
    className = '',
}: SquizLogoProps) => {
    return (
        <div className={className} style={{ width, height }}>
            <DotLottieReact
                src={logoAnimation}
                loop={loop}
                autoplay={autoplay}
                style={{ width: '100%', height: '100%' }}
            />
        </div>
    );
};
