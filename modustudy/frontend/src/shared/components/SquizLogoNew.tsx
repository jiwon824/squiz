import { DotLottieReact } from '@lottiefiles/dotlottie-react';
import logoAnimation from '../../assets/logos/squiz new logo.lottie';

interface SquizLogoNewProps {
    width?: number | string;
    height?: number | string;
    className?: string;
}

export const SquizLogoNew = ({
    width = 200,
    height = 200,
    className = '',
}: SquizLogoNewProps) => {
    return (
        <div className={className} style={{ width, height }}>
            <DotLottieReact
                src={logoAnimation}
                loop={false}
                autoplay={false}
                style={{ width: '100%', height: '100%' }}
            />
        </div>
    );
};