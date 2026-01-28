/**
 * 자료 업로드 모달 컴포넌트
 */

import { useState, useRef } from 'react';
import { createPortal } from 'react-dom';
import { cn } from '@/shared/utils/cn';
import {
  X,
  Upload,
  Link as LinkIcon,
  FileText,
  Image,
  Video,
  File,
  AlertCircle,
} from 'lucide-react';
import { materialApi } from '@/api/endpoints/materialApi';

interface MaterialUploadModalProps {
  studyId: number;
  type: 'file' | 'link';
  onClose: () => void;
  onComplete: () => void;
}

// 허용되는 파일 확장자
const ALLOWED_EXTENSIONS = [
  // 문서
  'pdf', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx', 'txt', 'md',
  // 이미지
  'jpg', 'jpeg', 'png', 'gif', 'webp', 'svg',
  // 영상
  'mp4', 'avi', 'mov', 'wmv',
  // 압축
  'zip', 'rar', '7z',
];

// 파일 확장자로 아이콘 결정
const getFileIcon = (fileName: string) => {
  const ext = fileName.split('.').pop()?.toLowerCase() || '';

  if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg'].includes(ext)) {
    return <Image size={24} />;
  }
  if (['mp4', 'avi', 'mov', 'wmv'].includes(ext)) {
    return <Video size={24} />;
  }
  if (['pdf', 'doc', 'docx', 'txt', 'md'].includes(ext)) {
    return <FileText size={24} />;
  }
  return <File size={24} />;
};

// 파일 크기 포맷
const formatFileSize = (bytes: number) => {
  const units = ['B', 'KB', 'MB', 'GB'];
  let unitIndex = 0;
  let size = bytes;
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex++;
  }
  return `${size.toFixed(1)} ${units[unitIndex]}`;
};

export const MaterialUploadModal: React.FC<MaterialUploadModalProps> = ({
  studyId,
  type,
  onClose,
  onComplete,
}) => {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [weekNumber, setWeekNumber] = useState<number | ''>('');
  const [url, setUrl] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  // 파일 선택 핸들러
  const handleFileSelect = (file: File) => {
    const ext = file.name.split('.').pop()?.toLowerCase() || '';

    if (!ALLOWED_EXTENSIONS.includes(ext)) {
      setError(`지원하지 않는 파일 형식입니다. (${ext})`);
      return;
    }

    // 1GB 제한
    if (file.size > 1024 * 1024 * 1024) {
      setError('파일 크기는 1GB 이하여야 합니다.');
      return;
    }

    setSelectedFile(file);
    setError(null);

    // 제목이 비어있으면 파일명으로 설정
    if (!title) {
      const nameWithoutExt = file.name.replace(/\.[^/.]+$/, '');
      setTitle(nameWithoutExt);
    }
  };

  // 파일 입력 변경 핸들러
  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      handleFileSelect(file);
    }
  };

  // 드래그 앤 드롭 핸들러
  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);

    const file = e.dataTransfer.files?.[0];
    if (file) {
      handleFileSelect(file);
    }
  };

  // 업로드 핸들러
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!title.trim()) {
      setError('제목을 입력해주세요.');
      return;
    }

    if (type === 'file' && !selectedFile) {
      setError('파일을 선택해주세요.');
      return;
    }

    if (type === 'link' && !url.trim()) {
      setError('링크를 입력해주세요.');
      return;
    }

    setIsUploading(true);
    setError(null);

    try {
      if (type === 'file' && selectedFile) {
        await materialApi.uploadMaterial(
          studyId,
          selectedFile,
          title.trim(),
          description.trim() || undefined,
          weekNumber ? Number(weekNumber) : undefined
        );
      } else if (type === 'link') {
        await materialApi.createMaterial(studyId, {
          title: title.trim(),
          description: description.trim() || undefined,
          materialType: 'LINK',
          url: url.trim(),
          weekNumber: weekNumber ? Number(weekNumber) : undefined,
        });
      }

      onComplete();
    } catch (err: any) {
      // 에러 메시지 추출
      const errorMsg = err?.response?.data?.message
        || err?.response?.data?.error
        || err?.message
        || '업로드에 실패했습니다.';
      setError(errorMsg);
    } finally {
      setIsUploading(false);
    }
  };

  return createPortal(
    <div className="material-modal-overlay" onClick={onClose}>
      <div className="material-modal" onClick={(e) => e.stopPropagation()}>
        {/* 헤더 */}
        <div className="material-modal__header">
          <h3>
            {type === 'file' ? (
              <>
                <Upload size={20} />
                파일 업로드
              </>
            ) : (
              <>
                <LinkIcon size={20} />
                링크 추가
              </>
            )}
          </h3>
          <button className="material-modal__close" onClick={onClose}>
            <X size={20} />
          </button>
        </div>

        {/* 본문 */}
        <form className="material-modal__body" onSubmit={handleSubmit}>
          {/* 파일 선택 영역 (파일 타입일 때만) */}
          {type === 'file' && (
            <div
              className={cn(
                'material-modal__dropzone',
                isDragging && 'dragging',
                selectedFile && 'has-file'
              )}
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
              onClick={() => fileInputRef.current?.click()}
            >
              <input
                ref={fileInputRef}
                type="file"
                onChange={handleFileInputChange}
                style={{ display: 'none' }}
              />

              {selectedFile ? (
                <div className="material-modal__selected-file">
                  {getFileIcon(selectedFile.name)}
                  <div className="file-info">
                    <span className="file-name">{selectedFile.name}</span>
                    <span className="file-size">{formatFileSize(selectedFile.size)}</span>
                  </div>
                  <button
                    type="button"
                    className="file-remove"
                    onClick={(e) => {
                      e.stopPropagation();
                      setSelectedFile(null);
                    }}
                  >
                    <X size={16} />
                  </button>
                </div>
              ) : (
                <div className="material-modal__dropzone-content">
                  <Upload size={32} />
                  <p>파일을 드래그하거나 클릭하여 선택</p>
                  <span>최대 1GB, PDF, 문서, 이미지, 영상, 압축파일 지원</span>
                </div>
              )}
            </div>
          )}

          {/* 링크 입력 (링크 타입일 때만) */}
          {type === 'link' && (
            <div className="material-modal__field">
              <label>링크 URL *</label>
              <input
                type="url"
                placeholder="https://example.com"
                value={url}
                onChange={(e) => setUrl(e.target.value)}
                required
              />
            </div>
          )}

          {/* 제목 */}
          <div className="material-modal__field">
            <label>제목 *</label>
            <input
              type="text"
              placeholder="자료 제목을 입력하세요"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              maxLength={100}
              required
            />
          </div>

          {/* 설명 */}
          <div className="material-modal__field">
            <label>설명</label>
            <textarea
              placeholder="자료에 대한 설명을 입력하세요 (선택)"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              maxLength={500}
              rows={3}
            />
          </div>

          {/* 주차 */}
          <div className="material-modal__field">
            <label>주차</label>
            <input
              type="number"
              placeholder="예: 1"
              value={weekNumber}
              onChange={(e) => setWeekNumber(e.target.value ? Number(e.target.value) : '')}
              min={1}
              max={52}
            />
          </div>

          {/* 에러 메시지 */}
          {error && (
            <div className="material-modal__error">
              <AlertCircle size={16} />
              <span>{error}</span>
            </div>
          )}

          {/* 버튼 */}
          <div className="material-modal__actions">
            <button type="button" className="btn-cancel" onClick={onClose}>
              취소
            </button>
            <button
              type="submit"
              className="btn-submit"
              disabled={isUploading}
            >
              {isUploading ? '업로드 중...' : type === 'file' ? '업로드' : '추가'}
            </button>
          </div>
        </form>
      </div>
    </div>,
    document.body
  );
};
