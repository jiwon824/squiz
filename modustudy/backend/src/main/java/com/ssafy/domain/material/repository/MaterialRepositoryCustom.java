package com.ssafy.domain.material.repository;

import com.ssafy.domain.material.entity.Material;
import com.ssafy.domain.material.entity.MaterialType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MaterialRepositoryCustom {

    Page<Material> searchMaterials(Long studyId,
                                   Integer weekNumber,
                                   MaterialType materialType,
                                   String keyword,
                                   Pageable pageable);
}
