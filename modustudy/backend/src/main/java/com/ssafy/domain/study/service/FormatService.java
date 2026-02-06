package com.ssafy.domain.study.service;

import com.ssafy.domain.study.dto.response.FormatResponse;
import com.ssafy.domain.study.entity.Format;
import com.ssafy.domain.study.repository.FormatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Format 서비스
 */
 @Service
 @RequiredArgsConstructor
 @Transactional(readOnly = true)
 public class FormatService {

    private final FormatRepository formatRepository;

    /**
     * 모든 형식 조회
     */
    public List<FormatResponse> getAllFormats() {
        List<Format> formats = formatRepository.findAllByOrderBySortOrderAsc();
        return formats.stream()
                .map(FormatResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 형식 단건 조회
     */
    public FormatResponse getFormat(Long formatId) {
        Format format = formatRepository.findById(formatId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 형식입니다: " + formatId));
        return FormatResponse.from(format);
    }

    /**
     * 형식 존재 여부 확인
     */
    public boolean exists(Long formatId) {
        return formatRepository.existsById(formatId);
    }

    /**
     * 형식 엔티티 조회 (내부용)
     */
    public Format getFormatEntity(Long formatId) {
        return formatRepository.findById(formatId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 형식입니다: " + formatId));
    }
}
