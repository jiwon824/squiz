/**
 * ToggleSwitch 컴포넌트
 * 알림 설정 등에서 사용하는 ON/OFF 토글 스위치입니다.
 */

interface ToggleSwitchProps {
    /** 토글 상태 */
    checked: boolean;
    /** 상태 변경 핸들러 */
    onChange: (checked: boolean) => void;
    /** 비활성화 여부 */
    disabled?: boolean;
    /** 접근성 레이블 */
    ariaLabel?: string;
}

export const ToggleSwitch = ({
    checked,
    onChange,
    disabled = false,
    ariaLabel,
}: ToggleSwitchProps) => {
    const handleChange = () => {
        if (!disabled) {
            onChange(!checked);
        }
    };

    return (
        <label className="toggle-switch">
            <input
                type="checkbox"
                checked={checked}
                onChange={handleChange}
                disabled={disabled}
                aria-label={ariaLabel}
            />
            <span className="toggle-slider" />
        </label>
    );
};
