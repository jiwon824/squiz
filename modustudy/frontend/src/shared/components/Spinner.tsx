import React from 'react';
import { Loader2 } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

interface SpinnerProps {
    size?: number;
    className?: string;
}

const styles = {
    base: 'animate-spin text-study-blue',
};

export const Spinner: React.FC<SpinnerProps> = ({ size = 24, className = '' }) => {
    return (
        <Loader2
            className={cn(styles.base, className)}
            size={size}
        />
    );
};
