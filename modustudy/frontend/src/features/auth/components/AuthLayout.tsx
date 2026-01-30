import React from 'react';
import { SquizLogoNew } from '@/shared/components/SquizLogoNew';
import '../styles/AuthLayout.css';

interface AuthLayoutProps {
    children: React.ReactNode;
    pageState?: string;
    hideBranding?: boolean;
    leftContent?: React.ReactNode;
}

const AuthLayout: React.FC<AuthLayoutProps> = ({ children, pageState = '', hideBranding = false, leftContent }) => {
    const [isLoaded, setIsLoaded] = React.useState(false);

    React.useEffect(() => {
        const timer = setTimeout(() => setIsLoaded(true), 100);
        return () => clearTimeout(timer);
    }, []);

    return (
        <div className={`auth-page ${pageState} ${isLoaded ? 'loaded' : ''}`}>
            <div className="auth-wrapper">
                {/* 왼쪽: 브랜딩 영역 */}
                {!hideBranding && (
                    <div className="auth-visual">
                        {leftContent ? (
                            <div className="visual-content visual-content--custom">
                                {leftContent}
                            </div>
                        ) : (
                            <div className="visual-content visual-content--centered">
                                <div className="visual-logo-wrapper visual-logo-wrapper--large">
                                    <SquizLogoNew width={240} height={80} />
                                </div>

                                <div className="slogan-group slogan-group--centered">
                                    <div className="primary-slogan-wrapper">
                                        <h2 className="primary-line-1">
                                            가장 밀도 높은 <span className="text-highlight">몰입</span>
                                        </h2>
                                        <h2 className="primary-line-2">
                                            성장을 흡수하는 기쁨
                                        </h2>
                                    </div>
                                    <div className="slogan-divider"></div>
                                    <p className="secondary-slogan">
                                        Squeeze your Brain, Absorb the Growth.
                                    </p>
                                </div>
                            </div>
                        )}
                        <div className="visual-background">
                            <div className="circle circle-1"></div>
                            <div className="circle circle-2"></div>
                            <div className="mesh-overlay"></div>
                        </div>
                    </div>
                )}

                {/* 오른쪽: 폼 영역 */}
                <div className="auth-form-container">
                    <div className="form-content">
                        {children}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default AuthLayout;
