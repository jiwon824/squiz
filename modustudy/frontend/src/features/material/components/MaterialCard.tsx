/**
 * 자료 카드 컴포넌트
 */

import { cn } from '@/shared/utils/cn';
import {
  FileText,
  Link as LinkIcon,
  Image,
  Video,
  Download,
  Eye,
  MessageSquare,
  Calendar,
  User,
} from 'lucide-react';
import type { MaterialListResponse, MaterialType } from '../types';
import { materialApi } from '@/api/endpoints/materialApi';

interface MaterialCardProps {
  material: MaterialListResponse;
  onClick?: () => void;
  onDownload?: () => void;
}

// 자료 타입별 아이콘
const getTypeIcon = (type: MaterialType) => {
  switch (type) {
    case 'LINK':
      return <LinkIcon size={20} />;
    case 'FILE':
      return <FileText size={20} />;
    case 'IMAGE':
      return <Image size={20} />;
    case 'VIDEO':
      return <Video size={20} />;
    default:
      return <FileText size={20} />;
  }
};

// 자료 타입별 라벨
const getTypeLabel = (type: MaterialType) => {
  switch (type) {
    case 'LINK':
      return '링크';
    case 'FILE':
      return '파일';
    case 'IMAGE':
      return '이미지';
    case 'VIDEO':
      return '영상';
    default:
      return '파일';
  }
};

// 파일 크기 포맷
const formatFileSize = (bytes: number | null) => {
  if (!bytes) return '';
  const units = ['B', 'KB', 'MB', 'GB'];
  let unitIndex = 0;
  let size = bytes;
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex++;
  }
  return `${size.toFixed(1)} ${units[unitIndex]}`;
};

// 날짜 포맷
const formatDate = (dateString: string) => {
  const date = new Date(dateString);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));

  if (days === 0) {
    const hours = Math.floor(diff / (1000 * 60 * 60));
    if (hours === 0) {
      const minutes = Math.floor(diff / (1000 * 60));
      return `${minutes}분 전`;
    }
    return `${hours}시간 전`;
  }
  if (days < 7) return `${days}일 전`;

  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
};

export const MaterialCard: React.FC<MaterialCardProps> = ({
  material,
  onClick,
  onDownload,
}) => {
  const handleDownload = (e: React.MouseEvent) => {
    e.stopPropagation();

    if (material.materialType === 'LINK' && material.url) {
      // 링크 타입: 새 탭에서 열기
      window.open(material.url, '_blank');
    } else if (material.fileUrl) {
      // 파일 타입: 다운로드
      const downloadUrl = materialApi.getFileDownloadUrl(material.fileUrl);
      window.open(downloadUrl, '_blank');
    }

    onDownload?.();
  };

  return (
    <div className="material-card" onClick={onClick}>
      {/* 카드 헤더 */}
      <div className="material-card__header">
        <div className={cn('material-card__type', `material-card__type--${material.materialType.toLowerCase()}`)}>
          {getTypeIcon(material.materialType)}
          <span>{getTypeLabel(material.materialType)}</span>
        </div>
        {material.weekNumber && (
          <span className="material-card__week">{material.weekNumber}주차</span>
        )}
      </div>

      {/* 카드 본문 */}
      <div className="material-card__body">
        <h3 className="material-card__title">{material.title}</h3>
        {material.description && (
          <p className="material-card__description">{material.description}</p>
        )}
        {material.fileSize && (
          <span className="material-card__file-size">
            {formatFileSize(material.fileSize)}
          </span>
        )}
      </div>

      {/* 카드 푸터 */}
      <div className="material-card__footer">
        <div className="material-card__meta">
          <div className="material-card__uploader">
            <User size={14} />
            <span>{material.uploader.nickname}</span>
          </div>
          <div className="material-card__date">
            <Calendar size={14} />
            <span>{formatDate(material.createdAt)}</span>
          </div>
        </div>

        <div className="material-card__stats">
          <span className="material-card__stat">
            <Eye size={14} />
            {material.viewCount}
          </span>
          <span className="material-card__stat">
            <MessageSquare size={14} />
            {material.commentCount}
          </span>
        </div>
      </div>

      {/* 다운로드/열기 버튼 */}
      <button
        className="material-card__action"
        onClick={handleDownload}
        title={material.materialType === 'LINK' ? '링크 열기' : '다운로드'}
      >
        {material.materialType === 'LINK' ? (
          <LinkIcon size={18} />
        ) : (
          <Download size={18} />
        )}
      </button>
    </div>
  );
};
