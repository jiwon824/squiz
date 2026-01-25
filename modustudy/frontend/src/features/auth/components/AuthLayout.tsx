import React from 'react';
import { Target, Users, Zap } from 'lucide-react';
import SquizLogoSvg from '@/assets/logos/SQUIZ_LOGO.svg';
import '../styles/AuthLayout.css';

interface AuthLayoutProps {
    children: React.ReactNode;
    pageState?: string;
    hideBranding?: boolean;
}

const AuthLayout: React.FC<AuthLayoutProps> = ({ children, pageState = '', hideBranding = false }) => {
    const [isLoaded, setIsLoaded] = React.useState(false);

    React.useEffect(() => {
        const timer = setTimeout(() => setIsLoaded(true), 100);
        return () => clearTimeout(timer);
    }, []);

    return (
        <div className={`auth-page ${pageState} ${isLoaded ? 'loaded' : ''}`}>
            <div className="auth-wrapper">
                {/* Left Side - Visual Branding */}
                {!hideBranding && (
                    <div className="auth-visual">
                        <div className="visual-content">
                            <div className="visual-logo-wrapper">
                                <div className="logo-glow"></div>
                                <img src={SquizLogoSvg} alt="SQUIZ" className="visual-logo-svg" />
                            </div>

                            <div className="slogan-group">
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
                                    Squeeze your Brain, Absorb the Growth.<br />
                                </p>
                            </div>

                            <div className="value-cards">
                                <div className="value-card">
                                    <div className="value-icon-wrapper">
                                        <Target size={20} />
                                    </div>
                                    <div className="value-card-content">
                                        <h4>몰입</h4>
                                        <span>Study Tool</span>
                                    </div>
                                </div>
                                <div className="value-card">
                                    <div className="value-icon-wrapper">
                                        <Users size={20} />
                                    </div>
                                    <div className="value-card-content">
                                        <h4>협동</h4>
                                        <span>Quiz Platform</span>
                                    </div>
                                </div>
                                <div className="value-card">
                                    <div className="value-icon-wrapper">
                                        <Zap size={20} />
                                    </div>
                                    <div className="value-card-content">
                                        <h4>편리</h4>
                                        <span>AI Record</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div className="visual-background">
                            <div className="circle circle-1"></div>
                            <div className="circle circle-2"></div>
                            <div className="mesh-overlay"></div>
                        </div>
                    </div>
                )}

                {/* Right Side - Form Container */}
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
