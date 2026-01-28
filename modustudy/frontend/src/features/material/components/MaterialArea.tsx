/**
 * 자료실 영역 컴포넌트
 * 워크스페이스 내에서 자료 목록을 보여줍니다.
 */

import { useState, useEffect, useCallback } from 'react';
import { cn } from '@/shared/utils/cn';
import {
  Search,
  Grid,
  List,
  Upload,
  Link as LinkIcon,
  X,
  FolderOpen,
  RefreshCw,
  AlertCircle,
} from 'lucide-react';
import { MaterialCard } from './MaterialCard';
import { MaterialUploadModal } from './MaterialUploadModal';
import { materialApi } from '@/api/endpoints/materialApi';
import { useUIStore } from '@/store/uiStore';
import type { MaterialListResponse, MaterialType, MaterialSortOption } from '../types';
import '../styles/material.css';

interface MaterialAreaProps {
  studyId: number;
}

export const MaterialArea: React.FC<MaterialAreaProps> = ({ studyId }) => {
  const [materials, setMaterials] = useState<MaterialListResponse[]>([]);
  const [filteredMaterials, setFilteredMaterials] = useState<MaterialListResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [filterType, setFilterType] = useState<MaterialType | 'ALL'>('ALL');
  const [sortOption, setSortOption] = useState<MaterialSortOption>('latest');
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [uploadType, setUploadType] = useState<'file' | 'link'>('file');

  const showToast = useUIStore((state) => state.showToast);

  // 자료 목록 조회
  const fetchMaterials = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await materialApi.getMaterials(studyId);
      console.log('[MaterialArea] 자료 목록 조회 성공:', data);
      setMaterials(data || []);
      setFilteredMaterials(data || []);
    } catch (err: any) {
      console.error('[MaterialArea] 자료 목록 조회 실패:', err);
      const errorMessage = err?.response?.data?.message || err?.message || '자료를 불러오는데 실패했습니다.';
      setError(errorMessage);
      setMaterials([]);
      setFilteredMaterials([]);
    } finally {
      setIsLoading(false);
    }
  }, [studyId]);

  // 초기 로드
  useEffect(() => {
    fetchMaterials();
  }, [fetchMaterials]);

  // 필터 및 정렬 적용
  useEffect(() => {
    let result = [...materials];

    // 타입 필터
    if (filterType !== 'ALL') {
      result = result.filter((m) => m.materialType === filterType);
    }

    // 검색 필터
    if (searchKeyword.trim()) {
      const keyword = searchKeyword.toLowerCase();
      result = result.filter(
        (m) =>
          m.title.toLowerCase().includes(keyword) ||
          m.description?.toLowerCase().includes(keyword) ||
          m.uploader.nickname.toLowerCase().includes(keyword)
      );
    }

    // 정렬
    switch (sortOption) {
      case 'latest':
        result.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        break;
      case 'oldest':
        result.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
        break;
      case 'popular':
        result.sort((a, b) => b.viewCount - a.viewCount);
        break;
      case 'week':
        result.sort((a, b) => (b.weekNumber || 0) - (a.weekNumber || 0));
        break;
    }

    setFilteredMaterials(result);
  }, [materials, filterType, searchKeyword, sortOption]);

  // 업로드 완료 핸들러
  const handleUploadComplete = () => {
    setIsUploadModalOpen(false);
    showToast?.('자료가 업로드되었습니다.', 'success');
    fetchMaterials();
  };

  // 새로고침 핸들러
  const handleRefresh = () => {
    fetchMaterials();
  };

  // 파일 업로드 버튼 클릭
  const handleFileUpload = () => {
    setUploadType('file');
    setIsUploadModalOpen(true);
  };

  // 링크 추가 버튼 클릭
  const handleLinkAdd = () => {
    setUploadType('link');
    setIsUploadModalOpen(true);
  };

  return (
    <div className="material-area">
      {/* 헤더 */}
      <div className="material-area__header">
        <div className="material-area__title-section">
          <FolderOpen size={24} />
          <h2 className="material-area__title">자료실</h2>
          <span className="material-area__count">{filteredMaterials.length}개</span>
        </div>

        <div className="material-area__actions">
          <button
            className="material-area__refresh-btn"
            onClick={handleRefresh}
            disabled={isLoading}
            title="새로고침"
          >
            <RefreshCw size={16} className={isLoading ? 'spinning' : ''} />
          </button>
          <button className="material-area__upload-btn" onClick={handleFileUpload}>
            <Upload size={16} />
            <span>파일 업로드</span>
          </button>
          <button className="material-area__link-btn" onClick={handleLinkAdd}>
            <LinkIcon size={16} />
            <span>링크 추가</span>
          </button>
        </div>
      </div>

      {/* 필터 바 */}
      <div className="material-area__filter-bar">
        {/* 검색 */}
        <div className="material-area__search">
          <Search size={16} />
          <input
            type="text"
            placeholder="자료 검색..."
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
          />
          {searchKeyword && (
            <button onClick={() => setSearchKeyword('')}>
              <X size={14} />
            </button>
          )}
        </div>

        {/* 타입 필터 */}
        <div className="material-area__type-filter">
          <button
            className={cn('filter-btn', filterType === 'ALL' && 'active')}
            onClick={() => setFilterType('ALL')}
          >
            전체
          </button>
          <button
            className={cn('filter-btn', filterType === 'FILE' && 'active')}
            onClick={() => setFilterType('FILE')}
          >
            파일
          </button>
          <button
            className={cn('filter-btn', filterType === 'LINK' && 'active')}
            onClick={() => setFilterType('LINK')}
          >
            링크
          </button>
          <button
            className={cn('filter-btn', filterType === 'IMAGE' && 'active')}
            onClick={() => setFilterType('IMAGE')}
          >
            이미지
          </button>
          <button
            className={cn('filter-btn', filterType === 'VIDEO' && 'active')}
            onClick={() => setFilterType('VIDEO')}
          >
            영상
          </button>
        </div>

        {/* 정렬 및 뷰 모드 */}
        <div className="material-area__controls">
          <select
            className="material-area__sort"
            value={sortOption}
            onChange={(e) => setSortOption(e.target.value as MaterialSortOption)}
          >
            <option value="latest">최신순</option>
            <option value="oldest">오래된순</option>
            <option value="popular">조회순</option>
            <option value="week">주차순</option>
          </select>

          <div className="material-area__view-toggle">
            <button
              className={cn('view-btn', viewMode === 'grid' && 'active')}
              onClick={() => setViewMode('grid')}
              title="그리드 뷰"
            >
              <Grid size={16} />
            </button>
            <button
              className={cn('view-btn', viewMode === 'list' && 'active')}
              onClick={() => setViewMode('list')}
              title="리스트 뷰"
            >
              <List size={16} />
            </button>
          </div>
        </div>
      </div>

      {/* 자료 목록 */}
      <div className={cn('material-area__content', viewMode === 'list' && 'list-view')}>
        {isLoading ? (
          <div className="material-area__loading">
            <div className="loading-spinner" />
            <span>자료를 불러오는 중...</span>
          </div>
        ) : error ? (
          <div className="material-area__error">
            <AlertCircle size={48} />
            <p>{error}</p>
            <button onClick={handleRefresh}>다시 시도</button>
          </div>
        ) : filteredMaterials.length > 0 ? (
          <div className={cn('material-grid', viewMode === 'list' && 'list-view')}>
            {filteredMaterials.map((material) => (
              <MaterialCard
                key={material.id}
                material={material}
                onClick={() => console.log('자료 클릭:', material.id)}
              />
            ))}
          </div>
        ) : (
          <div className="material-area__empty">
            <FolderOpen size={48} />
            <p>등록된 자료가 없습니다.</p>
            <span>파일을 업로드하거나 링크를 추가해보세요.</span>
          </div>
        )}
      </div>

      {/* 업로드 모달 */}
      {isUploadModalOpen && (
        <MaterialUploadModal
          studyId={studyId}
          type={uploadType}
          onClose={() => setIsUploadModalOpen(false)}
          onComplete={handleUploadComplete}
        />
      )}
    </div>
  );
};
