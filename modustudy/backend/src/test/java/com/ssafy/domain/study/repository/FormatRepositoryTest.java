package com.ssafy.domain.study.repository;

import com.ssafy.domain.study.entity.Format;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class FormatRepositoryTest {

    @Autowired
    private FormatRepository formatRepository;

    @Autowired
    private EntityManager entityManager;

    private Format format1;
    private Format format2;
    private Format format3;

    @BeforeEach
    void setUp() {
        format1 = formatRepository.save(Format.builder()
                .name("문제 풀이")
                .description("알고리즘/자격증 문제 풀고 리뷰")
                .icon("edit")
                .sortOrder(1)
                .build());

        format2 = formatRepository.save(Format.builder()
                .name("독서/책 스터디")
                .description("기술 서적 읽고 토론")
                .icon("book")
                .sortOrder(2)
                .build());

        format3 = formatRepository.save(Format.builder()
                .name("강의 수강")
                .description("온라인 강의 같이 수강")
                .icon("video")
                .sortOrder(3)
                .build());

        formatRepository.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("전체 형식 목록 조회 - 정렬순으로 조회")
    void findAllByOrderBySortOrderAsc_Success() {
        // when
        List<Format> formats = formatRepository.findAllByOrderBySortOrderAsc();

        // then
        assertThat(formats).hasSize(3);
        assertThat(formats.get(0).getName()).isEqualTo("문제 풀이");
        assertThat(formats.get(1).getName()).isEqualTo("독서/책 스터디");
        assertThat(formats.get(2).getName()).isEqualTo("강의 수강");
    }

    @Test
    @DisplayName("형식 단건 조회")
    void findById_Success() {
        // when
        Format format = formatRepository.findById(format1.getId()).orElseThrow();

        // then
        assertThat(format.getName()).isEqualTo("문제 풀이");
        assertThat(format.getDescription()).isEqualTo("알고리즘/자격증 문제 풀고 리뷰");
        assertThat(format.getIcon()).isEqualTo("edit");
    }

    @Test
    @DisplayName("형식 생성")
    void save_Success() {
        // given
        Format newFormat = Format.builder()
                .name("모의 면접")
                .description("면접 연습 및 피드백")
                .icon("users")
                .sortOrder(4)
                .build();

        // when
        Format saved = formatRepository.save(newFormat);
        formatRepository.flush();

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("모의 면접");
    }

    @Test
    @DisplayName("형식 개수 조회")
    void count_Success() {
        // when
        long count = formatRepository.count();

        // then
        assertThat(count).isEqualTo(3);
    }
}
