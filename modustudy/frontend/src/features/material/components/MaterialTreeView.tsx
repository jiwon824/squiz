/**
 * 자료 트리뷰 컴포넌트
 * 주차별 > 타입별로 자료를 폴더 구조로 보여줍니다.
 */

import { useState, useMemo } from 'react';
import { cn } from '@/shared/utils/cn';
import {
  ChevronRight,
  Folder,
  FolderOpen,
  FileText,
  Image,
  Video,
  Link as LinkIcon,
  File,
  Eye,
  MessageCircle,
} from 'lucide-react';
import type { MaterialListResponse, MaterialType } from '../types';

interface MaterialTreeViewProps {
  materials: MaterialListResponse[];
  onMaterialClick: (material: MaterialListResponse) => void;
}

// 타입별 그룹
interface TypeGroup {
  type: MaterialType;
  label: string;
  materials: MaterialListResponse[];
}

// 주차별 그룹
interface WeekGroup {
  weekNumber: number | null;
  label: string;
  typeGroups: TypeGroup[];
  totalCount: number;
}

// 타입 순서 및 라벨
const TYPE_CONFIG: { type: MaterialType; label: string }[] = [
  { type: 'IMAGE', label: '이미지' },
  { type: 'FILE', label: '파일' },
  { type: 'LINK', label: '링크' },
  { type: 'VIDEO', label: '동영상' },
];

// 타입별 아이콘
const getTypeIcon = (type: string, size: number = 16) => {
  switch (type) {
    case 'IMAGE':
      return <Image size={size} />;
    case 'VIDEO':
      return <Video size={size} />;
    case 'LINK':
      return <LinkIcon size={size} />;
    case 'FILE':
    default:
      return <FileText size={size} />;
  }
};

// 타입별 폴더 색상
const getTypeFolderColor = (type: string) => {
  switch (type) {
    case 'IMAGE':
      return 'tree-folder--image';
    case 'VIDEO':
      return 'tree-folder--video';
    case 'LINK':
      return 'tree-folder--link';
    case 'FILE':
    default:
      return 'tree-folder--file';
  }
};

export const MaterialTreeView: React.FC<MaterialTreeViewProps> = ({
  materials,
  onMaterialClick,
}) => {
  // 펼쳐진 폴더 상태 관리
  const [expandedFolders, setExpandedFolders] = useState<Set<string>>(new Set(['week-null']));

  // 주차별 > 타입별로 자료 그룹화
  const weekGroups = useMemo(() => {
    // 주차별로 먼저 그룹화
    const weekMap: Map<number | null, MaterialListResponse[]> = new Map();

    materials.forEach((material) => {
      const week = material.weekNumber;
      if (!weekMap.has(week)) {
        weekMap.set(week, []);
      }
      weekMap.get(week)!.push(material);
    });

    // 주차 정렬 (있는 것 먼저, 오름차순)
    const sortedWeeks = Array.from(weekMap.keys()).sort((a, b) => {
      if (a === null) return 1;
      if (b === null) return -1;
      return a - b;
    });

    // 각 주차 내에서 타입별로 그룹화
    const result: WeekGroup[] = sortedWeeks.map((week) => {
      const weekMaterials = weekMap.get(week)!;

      // 타입별로 그룹화
      const typeMap: Map<MaterialType, MaterialListResponse[]> = new Map();
      weekMaterials.forEach((material) => {
        const type = material.materialType;
        if (!typeMap.has(type)) {
          typeMap.set(type, []);
        }
        typeMap.get(type)!.push(material);
      });

      // 타입 그룹 생성 (정해진 순서대로, 해당 타입이 있는 경우만)
      const typeGroups: TypeGroup[] = TYPE_CONFIG
        .filter((config) => typeMap.has(config.type))
        .map((config) => ({
          type: config.type,
          label: config.label,
          materials: typeMap.get(config.type)!,
        }));

      return {
        weekNumber: week,
        label: week ? `${week}주차` : '미분류',
        typeGroups,
        totalCount: weekMaterials.length,
      };
    });

    return result;
  }, [materials]);

  // 폴더 토글
  const toggleFolder = (folderId: string, e?: React.MouseEvent) => {
    e?.stopPropagation();
    setExpandedFolders((prev) => {
      const next = new Set(prev);
      if (next.has(folderId)) {
        next.delete(folderId);
      } else {
        next.add(folderId);
      }
      return next;
    });
  };

  // 모두 펼치기
  const expandAll = () => {
    const allIds: string[] = [];
    weekGroups.forEach((weekGroup) => {
      allIds.push(`week-${weekGroup.weekNumber}`);
      weekGroup.typeGroups.forEach((typeGroup) => {
        allIds.push(`week-${weekGroup.weekNumber}-type-${typeGroup.type}`);
      });
    });
    setExpandedFolders(new Set(allIds));
  };

  // 모두 접기
  const collapseAll = () => {
    setExpandedFolders(new Set());
  };

  return (
    <div className="material-tree-view" role="tree" aria-orientation="vertical">
      {/* 트리뷰 컨트롤 */}
      <div className="material-tree-view__controls">
        <button onClick={expandAll} className="tree-control-btn">
          모두 펼치기
        </button>
        <button onClick={collapseAll} className="tree-control-btn">
          모두 접기
        </button>
      </div>

      {/* 트리 구조 */}
      <div className="material-tree-view__tree">
        {weekGroups.map((weekGroup) => {
          const weekFolderId = `week-${weekGroup.weekNumber}`;
          const isWeekExpanded = expandedFolders.has(weekFolderId);

          return (
            <div key={weekFolderId} className="tree-folder tree-folder--week" role="treeitem" aria-expanded={isWeekExpanded}>
              {/* 주차 폴더 헤더 */}
              <div
                className={cn('tree-folder__header', isWeekExpanded && 'expanded')}
                onClick={() => toggleFolder(weekFolderId)}
              >
                <button className="tree-folder__toggle" onClick={(e) => toggleFolder(weekFolderId, e)}>
                  <ChevronRight
                    size={16}
                    className={cn('toggle-icon', isWeekExpanded && 'rotated')}
                  />
                </button>
                <div className="tree-folder__icon">
                  {isWeekExpanded ? <FolderOpen size={18} /> : <Folder size={18} />}
                </div>
                <span className="tree-folder__name">{weekGroup.label}</span>
                <span className="tree-folder__count">{weekGroup.totalCount}</span>
              </div>

              {/* 주차 폴더 내용 (타입별 폴더들) */}
              {isWeekExpanded && (
                <div className="tree-folder__content" role="group">
                  {weekGroup.typeGroups.map((typeGroup) => {
                    const typeFolderId = `week-${weekGroup.weekNumber}-type-${typeGroup.type}`;
                    const isTypeExpanded = expandedFolders.has(typeFolderId);

                    return (
                      <div
                        key={typeFolderId}
                        className={cn('tree-folder tree-folder--type', getTypeFolderColor(typeGroup.type))}
                        role="treeitem"
                        aria-expanded={isTypeExpanded}
                      >
                        {/* 타입 폴더 헤더 */}
                        <div
                          className={cn('tree-folder__header tree-folder__header--sub', isTypeExpanded && 'expanded')}
                          onClick={() => toggleFolder(typeFolderId)}
                        >
                          <button className="tree-folder__toggle" onClick={(e) => toggleFolder(typeFolderId, e)}>
                            <ChevronRight
                              size={14}
                              className={cn('toggle-icon', isTypeExpanded && 'rotated')}
                            />
                          </button>
                          <div className="tree-folder__icon tree-folder__icon--type">
                            {getTypeIcon(typeGroup.type, 16)}
                          </div>
                          <span className="tree-folder__name">{typeGroup.label}</span>
                          <span className="tree-folder__count">{typeGroup.materials.length}</span>
                        </div>

                        {/* 타입 폴더 내용 (자료 목록) */}
                        {isTypeExpanded && (
                          <div className="tree-folder__content tree-folder__content--items" role="group">
                            {typeGroup.materials.map((material) => (
                              <div
                                key={material.id}
                                className="tree-item"
                                role="treeitem"
                                onClick={() => onMaterialClick(material)}
                              >
                                <div className="tree-item__icon">{getTypeIcon(material.materialType)}</div>
                                <div className="tree-item__info">
                                  <span className="tree-item__title">{material.title}</span>
                                  <div className="tree-item__meta">
                                    <span className="tree-item__uploader">{material.uploader.nickname}</span>
                                    <span className="tree-item__stat">
                                      <Eye size={12} />
                                      {material.viewCount}
                                    </span>
                                    <span className="tree-item__stat">
                                      <MessageCircle size={12} />
                                      {material.commentCount}
                                    </span>
                                  </div>
                                </div>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          );
        })}

        {/* 빈 상태 */}
        {weekGroups.length === 0 && (
          <div className="tree-empty">
            <File size={32} />
            <span>자료가 없습니다.</span>
          </div>
        )}
      </div>
    </div>
  );
};
