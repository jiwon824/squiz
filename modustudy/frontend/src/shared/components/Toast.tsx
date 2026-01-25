// Toast 알림 컴포넌트
import { X, CheckCircle, AlertCircle, Info, AlertTriangle } from 'lucide-react';
import { useUIStore } from '@/store/uiStore';
import { cn } from '@/shared/utils/cn';

// 토스트 타입별 스타일 및 아이콘
const toastStyles = {
    success: {
        container: 'bg-white border-success text-success',
        icon: CheckCircle,
    },
    error: {
        container: 'bg-white border-error text-error',
        icon: AlertCircle,
    },
    warning: {
        container: 'bg-white border-warning text-warning',
        icon: AlertTriangle,
    },
    info: {
        container: 'bg-white border-primary text-primary',
        icon: Info,
    },
};

export const ToastContainer = () => {
    const { toasts, removeToast } = useUIStore();

    if (toasts.length === 0) return null;

    return (
        <div className="fixed bottom-6 right-6 z-[9999] flex flex-col gap-3 pointer-events-auto">
            {toasts.map((toast) => {
                const style = toastStyles[toast.type];
                const Icon = style.icon;

                return (
                    <div
                        key={toast.id}
                        className={cn(
                            'flex items-center gap-3 px-5 py-4 rounded-2xl border-2 shadow-xl',
                            'animate-slideUp min-w-[300px] max-w-[400px]',
                            style.container
                        )}
                    >
                        <Icon size={20} className="flex-shrink-0" />
                        <span className="flex-1 text-sm font-semibold text-text-primary">
                            {toast.message}
                        </span>
                        <button
                            onClick={() => removeToast(toast.id)}
                            className="flex-shrink-0 p-1 hover:bg-black/5 rounded-lg transition-colors"
                        >
                            <X size={16} className="text-text-tertiary" />
                        </button>
                    </div>
                );
            })}
        </div>
    );
};

export default ToastContainer;
