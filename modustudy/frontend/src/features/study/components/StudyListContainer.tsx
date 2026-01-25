import React from 'react';

interface StudyListContainerProps {
    children: React.ReactNode;
    maxWidth?: string;
    className?: string;
}

const StudyListContainer: React.FC<StudyListContainerProps> = ({
    children,
    maxWidth = '100%',
    className = '',
}) => {
    return (
        <div className={`page-container ${className}`} style={{ maxWidth }}>
            {children}
        </div>
    );
};

export default StudyListContainer;
