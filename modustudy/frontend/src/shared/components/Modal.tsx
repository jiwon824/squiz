import React, { ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

interface ModalProps {
    isOpen: boolean;
    onClose: () => void;
    title?: string;
    children: ReactNode;
    maxWidth?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '3xl';
}

const styles = {
    overlay: 'fixed inset-0 z-[9999] flex items-center justify-center p-4',
    backdrop: 'absolute inset-0 bg-black/40 backdrop-blur-sm transition-opacity animate-in fade-in duration-300',
    content: {
        base: 'relative w-full bg-white rounded-[32px] shadow-2xl overflow-hidden p-8 animate-in fade-in zoom-in duration-300',
        maxWidth: {
            sm: 'max-w-sm',
            md: 'max-w-md',
            lg: 'max-w-lg',
            xl: 'max-w-xl',
            '2xl': 'max-w-2xl',
            '3xl': 'max-w-3xl',
        },
    },
    header: 'flex items-center justify-between mb-8',
    title: 'text-2xl font-extrabold text-on-surface tracking-tight',
    closeButton: 'p-2.5 hover:bg-surface-200 rounded-full transition-all text-on-surface-variant hover:text-on-surface hover:rotate-90 duration-300 ml-auto',
    body: 'modal-body',
};

export const Modal: React.FC<ModalProps> = ({
    isOpen,
    onClose,
    title,
    children,
    maxWidth = 'md'
}) => {
    if (!isOpen) return null;

    const modalContent = (
        <div className={styles.overlay}>
            {/* Backdrop: 블러 효과와 투명도 조절 */}
            <div
                className={styles.backdrop}
                onClick={onClose}
            />

            {/* Modal Body: 애니메이션과 그림자, 라운딩 적용 */}
            <div className={cn(
                styles.content.base,
                styles.content.maxWidth[maxWidth]
            )}>
                {/* 헤더: title이 있을 때만 표시 (showCloseButton으로 강제 표시 가능) */}
                {title && (
                    <div className={styles.header}>
                        <h2 className={styles.title}>{title}</h2>
                        <button
                            onClick={onClose}
                            className={styles.closeButton}
                        >
                            <X size={24} />
                        </button>
                    </div>
                )}
                <div className={styles.body}>
                    {children}
                </div>
            </div>
        </div>
    );

    // DOM 최상단에 렌더링하여 스타일 간섭 방지
    return createPortal(modalContent, document.body);
};
