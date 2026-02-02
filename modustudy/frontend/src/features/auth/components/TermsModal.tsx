import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';

interface TermsModalProps {
    title: string;
    content: string;
    onClose: () => void;
}

/**
 * 약관/개인정보처리방침 모달
 * - md raw text를 간단한 HTML로 변환하여 표시
 */
export const TermsModal = ({ title, content, onClose }: TermsModalProps) => {
    // ESC 키로 닫기
    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            if (e.key === 'Escape') onClose();
        };
        document.addEventListener('keydown', handleKeyDown);
        // 스크롤 방지
        document.body.style.overflow = 'hidden';
        return () => {
            document.removeEventListener('keydown', handleKeyDown);
            document.body.style.overflow = '';
        };
    }, [onClose]);

    return createPortal(
        <div className="terms-modal-overlay" onClick={onClose}>
            <div className="terms-modal" onClick={(e) => e.stopPropagation()}>
                {/* 헤더 */}
                <div className="terms-modal-header">
                    <h3>{title}</h3>
                    <button
                        className="terms-modal-close"
                        onClick={onClose}
                        aria-label="닫기"
                    >
                        <X size={20} />
                    </button>
                </div>

                {/* 본문 */}
                <div
                    className="terms-modal-body"
                    dangerouslySetInnerHTML={{ __html: parseMarkdown(content) }}
                />

                {/* 하단 */}
                <div className="terms-modal-footer">
                    <button className="btn-primary" onClick={onClose}>
                        확인
                    </button>
                </div>
            </div>
        </div>,
        document.body
    );
};

/**
 * 간단한 마크다운 → HTML 변환
 */
function parseMarkdown(md: string): string {
    return md
        // 헤딩
        .replace(/^### (.+)$/gm, '<h4>$1</h4>')
        .replace(/^## (.+)$/gm, '<h3>$1</h3>')
        .replace(/^# (.+)$/gm, '<h2>$1</h2>')
        // 수평선
        .replace(/^---$/gm, '<hr />')
        // 볼드
        .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
        // 리스트
        .replace(/^- (.+)$/gm, '<li>$1</li>')
        .replace(/(<li>.*<\/li>\n?)+/g, (match) => `<ul>${match}</ul>`)
        // ※ 강조
        .replace(/^(※.+)$/gm, '<p class="terms-note">$1</p>')
        // 일반 단락 (빈 줄 기준)
        .replace(/^(?!<[hul]|<hr|<p|<li)(.+)$/gm, '<p>$1</p>')
        // 빈 줄 정리
        .replace(/\n{2,}/g, '\n');
}
