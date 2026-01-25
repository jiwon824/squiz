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

export const Modal: React.FC<ModalProps> = ({
    isOpen,
    onClose,
    title,
    children,
    maxWidth = 'md'
}) => {
    if (!isOpen) return null;

    const maxWidthClasses = {
        sm: 'max-w-sm',
        md: 'max-w-md',
        lg: 'max-w-lg',
        xl: 'max-w-xl',
        '2xl': 'max-w-2xl',
        '3xl': 'max-w-3xl',
    };

    const modalContent = (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4">
            {/* Backdrop: 블러 효과와 투명도 조절 */}
            <div
                className="absolute inset-0 bg-black/40 backdrop-blur-sm transition-opacity animate-in fade-in duration-300"
                onClick={onClose}
            />

            {/* Modal Body: 애니메이션과 그림자, 라운딩 적용 */}
            <div className={cn(
                'relative w-full bg-white rounded-[32px] shadow-2xl overflow-hidden p-8 animate-in fade-in zoom-in duration-300',
                maxWidthClasses[maxWidth]
            )}>
                <div className="flex items-center justify-between mb-8">
                    {title && <h2 className="text-2xl font-extrabold text-on-surface tracking-tight">{title}</h2>}
                    <button
                        onClick={onClose}
                        className="p-2.5 hover:bg-surface-200 rounded-full transition-all text-on-surface-variant hover:text-on-surface hover:rotate-90 duration-300"
                    >
                        <X size={24} />
                    </button>
                </div>
                <div className="modal-body">
                    {children}
                </div>
            </div>
        </div>
    );

    // DOM 최상단에 렌더링하여 스타일 간섭 방지
    return createPortal(modalContent, document.body);
};
