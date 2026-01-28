/**
 * 자료 상세 모달 컴포넌트
 */

import { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { cn } from '@/shared/utils/cn';
import {
  X,
  Download,
  ExternalLink,
  FileText,
  Image,
  Video,
  Link as LinkIcon,
  File,
  Calendar,
  Eye,
  User,
  Clock,
  MessageCircle,
  Send,
  Trash2,
} from 'lucide-react';
import { materialApi } from '@/api/endpoints/materialApi';
import { useAuthStore } from '@/store/authStore';
import type { MaterialDetailResponse, MaterialListResponse, MaterialCommentResponse } from '../types';

interface MaterialDetailModalProps {
  studyId: number;
  material: MaterialListResponse;
  onClose: () => void;
  onViewCountUpdate?: (materialId: number, viewCount: number) => void;
}

// 파일 크기 포맷
const formatFileSize = (bytes: number | null) => {
  if (!bytes) return '-';
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
  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

// 타입별 아이콘
const getTypeIcon = (type: string) => {
  switch (type) {
    case 'IMAGE':
      return <Image size={20} />;
    case 'VIDEO':
      return <Video size={20} />;
    case 'LINK':
      return <LinkIcon size={20} />;
    case 'FILE':
    default:
      return <FileText size={20} />;
  }
};

// 타입 한글명
const getTypeName = (type: string) => {
  switch (type) {
    case 'IMAGE':
      return '이미지';
    case 'VIDEO':
      return '영상';
    case 'LINK':
      return '링크';
    case 'FILE':
    default:
      return '파일';
  }
};

// 댓글 시간 포맷 (상대 시간)
const formatRelativeTime = (dateString: string) => {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMin = Math.floor(diffMs / 60000);
  const diffHour = Math.floor(diffMs / 3600000);
  const diffDay = Math.floor(diffMs / 86400000);

  if (diffMin < 1) return '방금 전';
  if (diffMin < 60) return `${diffMin}분 전`;
  if (diffHour < 24) return `${diffHour}시간 전`;
  if (diffDay < 7) return `${diffDay}일 전`;
  return date.toLocaleDateString('ko-KR');
};

export const MaterialDetailModal: React.FC<MaterialDetailModalProps> = ({
  studyId,
  material,
  onClose,
  onViewCountUpdate,
}) => {
  const { user } = useAuthStore();
  const [detail, setDetail] = useState<MaterialDetailResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const hasFetchedRef = useRef(false);
  const commentsListRef = useRef<HTMLDivElement>(null);

  // 댓글 관련 상태
  const [comments, setComments] = useState<MaterialCommentResponse[]>([]);
  const [commentText, setCommentText] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 상세 정보 조회 (중복 호출 방지)
  useEffect(() => {
    // 이미 조회했으면 스킵 (React Strict Mode 중복 방지)
    if (hasFetchedRef.current) return;
    hasFetchedRef.current = true;

    const fetchDetail = async () => {
      setIsLoading(true);
      try {
        const data = await materialApi.getMaterialDetail(studyId, material.id);
        if (data) {
          setDetail(data);
          // 조회수가 업데이트되면 부모 컴포넌트에 알림
          if (onViewCountUpdate && data.viewCount !== undefined && data.viewCount !== material.viewCount) {
            onViewCountUpdate(material.id, data.viewCount);
          }
        } else {
          // 데이터가 없으면 목록 데이터로 대체
          setDetail({
            ...material,
            fileName: null,
          } as MaterialDetailResponse);
        }
      } catch (err: any) {
        // 상세 조회 실패시 목록 데이터로 대체
        setDetail({
          ...material,
          fileName: null,
        } as MaterialDetailResponse);
      } finally {
        setIsLoading(false);
      }
    };

    // 댓글 목록 조회
    const fetchComments = async () => {
      try {
        const data = await materialApi.getComments(studyId, material.id);
        // 배열인지 확인하고 설정
        setComments(Array.isArray(data) ? data : []);
      } catch (err) {
        // 댓글 조회 실패시 빈 배열
        setComments([]);
      }
    };

    fetchDetail();
    fetchComments();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [studyId, material.id]);

  // 링크 열기
  const handleOpenLink = () => {
    if (detail?.url) {
      window.open(detail.url, '_blank', 'noopener,noreferrer');
    }
  };

  // 파일 다운로드
  const handleDownload = () => {
    if (detail?.fileUrl) {
      const downloadUrl = materialApi.getFileDownloadUrl(detail.fileUrl);
      window.open(downloadUrl, '_blank');
    }
  };

  // 댓글 작성
  const handleSubmitComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!commentText.trim() || isSubmitting) return;

    setIsSubmitting(true);
    try {
      await materialApi.createComment(studyId, material.id, { content: commentText.trim() });
      // 댓글 목록 새로고침
      const updatedComments = await materialApi.getComments(studyId, material.id);
      setComments(Array.isArray(updatedComments) ? updatedComments : []);
      setCommentText('');
      // 댓글 목록 스크롤을 맨 아래로
      setTimeout(() => {
        if (commentsListRef.current) {
          commentsListRef.current.scrollTop = commentsListRef.current.scrollHeight;
        }
      }, 100);
    } catch (err) {
      // 댓글 작성 실패
    } finally {
      setIsSubmitting(false);
    }
  };

  // 댓글 삭제
  const handleDeleteComment = async (commentId: number) => {
    if (!window.confirm('댓글을 삭제하시겠습니까?')) return;

    try {
      await materialApi.deleteComment(studyId, material.id, commentId);
      setComments((prev) => prev.filter((c) => c.id !== commentId));
    } catch (err) {
      // 댓글 삭제 실패
    }
  };

  const data = detail || material;

  return createPortal(
    <div className="material-modal-overlay" onClick={onClose}>
      <div
        className={cn('material-modal', 'material-detail-modal')}
        onClick={(e) => e.stopPropagation()}
      >
        {/* 헤더 */}
        <div className="material-modal__header">
          <div className="material-detail__type-badge">
            {getTypeIcon(data.materialType)}
            <span>{getTypeName(data.materialType)}</span>
          </div>
          <button className="material-modal__close" onClick={onClose}>
            <X size={20} />
          </button>
        </div>

        {/* 본문 */}
        <div className={cn('material-modal__body', 'material-detail__body')}>
          {isLoading ? (
            <div className="material-detail__loading">
              <div className="loading-spinner" />
              <span>불러오는 중...</span>
            </div>
          ) : (
            <>
              {/* 제목 */}
              <h2 className="material-detail__title">{data.title}</h2>

              {/* 메타 정보 */}
              <div className="material-detail__meta">
                <div className="meta-item">
                  <User size={14} />
                  <span>{data.uploader.nickname}</span>
                </div>
                <div className="meta-item">
                  <Clock size={14} />
                  <span>{formatDate(data.createdAt)}</span>
                </div>
                <div className="meta-item">
                  <Eye size={14} />
                  <span>조회 {data.viewCount}</span>
                </div>
                {data.weekNumber && (
                  <div className="meta-item">
                    <Calendar size={14} />
                    <span>{data.weekNumber}주차</span>
                  </div>
                )}
              </div>

              {/* 설명 */}
              {data.description && (
                <div className="material-detail__description">
                  <p>{data.description}</p>
                </div>
              )}

              {/* 파일 정보 */}
              {data.materialType !== 'LINK' && data.fileUrl && (
                <div className="material-detail__file-info">
                  <File size={16} />
                  <span className="file-name">{detail?.fileName || '파일'}</span>
                  <span className="file-size">{formatFileSize(data.fileSize)}</span>
                </div>
              )}

              {/* 링크 미리보기 */}
              {data.materialType === 'LINK' && data.url && (
                <div className="material-detail__link-preview">
                  <LinkIcon size={16} />
                  <a
                    href={data.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="link-url"
                  >
                    {data.url}
                  </a>
                </div>
              )}

              {/* 댓글 섹션 */}
              <div className="material-detail__comments">
                <div className="comments-header">
                  <MessageCircle size={16} />
                  <span>댓글 {comments.length}개</span>
                </div>

                {/* 댓글 목록 */}
                <div className="comments-list" ref={commentsListRef}>
                  {comments.length === 0 ? (
                    <p className="comments-empty">아직 댓글이 없습니다.</p>
                  ) : (
                    comments.map((comment) => (
                      <div key={comment.id} className="comment-item">
                        <div className="comment-header">
                          <div className="comment-author">
                            {comment.user?.profileImage ? (
                              <img
                                src={comment.user.profileImage}
                                alt={comment.user.nickname}
                                className="comment-avatar"
                              />
                            ) : (
                              <div className="comment-avatar-placeholder">
                                <User size={14} />
                              </div>
                            )}
                            <span className="comment-nickname">{comment.user?.nickname || '알 수 없음'}</span>
                          </div>
                          <div className="comment-meta">
                            <span className="comment-time">{formatRelativeTime(comment.createdAt)}</span>
                            {user && comment.user && String(user.id) === String(comment.user.id) && (
                              <button
                                type="button"
                                className="comment-delete"
                                onClick={() => handleDeleteComment(comment.id)}
                              >
                                <Trash2 size={14} />
                              </button>
                            )}
                          </div>
                        </div>
                        <p className="comment-content">{comment.content}</p>
                      </div>
                    ))
                  )}
                </div>

                {/* 댓글 입력 */}
                <form className="comment-form" onSubmit={handleSubmitComment}>
                  <input
                    type="text"
                    value={commentText}
                    onChange={(e) => setCommentText(e.target.value)}
                    placeholder="댓글을 입력하세요..."
                    className="comment-input"
                    disabled={isSubmitting}
                  />
                  <button
                    type="submit"
                    className="comment-submit"
                    disabled={!commentText.trim() || isSubmitting}
                  >
                    <Send size={16} />
                  </button>
                </form>
              </div>
            </>
          )}
        </div>

        {/* 액션 버튼 */}
        <div className={cn('material-modal__actions', 'material-detail__actions')}>
          <button type="button" className="btn-cancel" onClick={onClose}>
            닫기
          </button>

          {data.materialType === 'LINK' ? (
            <button
              type="button"
              className="btn-submit"
              onClick={handleOpenLink}
              disabled={!data.url}
            >
              <ExternalLink size={16} />
              링크 열기
            </button>
          ) : (
            <button
              type="button"
              className="btn-submit"
              onClick={handleDownload}
              disabled={!data.fileUrl}
            >
              <Download size={16} />
              다운로드
            </button>
          )}
        </div>
      </div>
    </div>,
    document.body
  );
};
