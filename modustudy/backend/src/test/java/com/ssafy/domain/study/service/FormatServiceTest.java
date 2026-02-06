package com.ssafy.domain.study.service;

import com.ssafy.domain.study.dto.response.FormatResponse;
import com.ssafy.domain.study.entity.Format;
import com.ssafy.domain.study.repository.FormatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FormatServiceTest {

    @Mock
    private FormatRepository formatRepository;

    @InjectMocks
    private FormatService formatService;

    private Format format1;
    private Format format2;
    private Format format3;

    @BeforeEach
    void setUp() {
        // 형식 1: 문제 풀이
        format1 = Format.builder()
                .name("문제 풀이")
                .description("알고리즘 문제를 함께 풀어요")
                .icon("problem-solving-icon")
                .sortOrder(1)
                .build();
        ReflectionTestUtils.setField(format1, "id", 1L);

        // 형식 2: 강의 수강
        format2 = Format.builder()
                .name("강의 수강")
                .description("온라인 강의를 함께 수강해요")
                .icon("lecture-icon")
                .sortOrder(2)
                .build();
        ReflectionTestUtils.setField(format2, "id", 2L);

        // 형식 3: 책 스터디
        format3 = Format.builder()
                .name("책 스터디")
                .description("책을 함께 읽고 토론해요")
                .icon("book-icon")
                .sortOrder(3)
                .build();
        ReflectionTestUtils.setField(format3, "id", 3L);
    }

    @Nested
    @DisplayName("getAllFormats - 전체 형식 목록 조회")
    class GetAllFormatsTest {

        @Test
        @DisplayName("성공 - 형식 목록 반환 (정렬순)")
        void success() {
            // given
            given(formatRepository.findAllByOrderBySortOrderAsc())
                    .willReturn(Arrays.asList(format1, format2, format3));

            // when
            List<FormatResponse> result = formatService.getAllFormats();

            // then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).getName()).isEqualTo("문제 풀이");
            assertThat(result.get(1).getName()).isEqualTo("강의 수강");
            assertThat(result.get(2).getName()).isEqualTo("책 스터디");
            verify(formatRepository).findAllByOrderBySortOrderAsc();
        }

        @Test
        @DisplayName("성공 - 빈 목록 반환")
        void success_EmptyList() {
            // given
            given(formatRepository.findAllByOrderBySortOrderAsc())
                    .willReturn(Collections.emptyList());

            // when
            List<FormatResponse> result = formatService.getAllFormats();

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getFormat - 형식 단건 조회")
    class GetFormatTest {

        @Test
        @DisplayName("성공 - 형식 정보 반환")
        void success() {
            // given
            Long formatId = 1L;
            given(formatRepository.findById(formatId))
                    .willReturn(Optional.of(format1));

            // when
            FormatResponse result = formatService.getFormat(formatId);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("문제 풀이");
            assertThat(result.getDescription()).isEqualTo("알고리즘 문제를 함께 풀어요");
            assertThat(result.getIcon()).isEqualTo("problem-solving-icon");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 형식")
        void fail_NotFound() {
            // given
            Long formatId = 999L;
            given(formatRepository.findById(formatId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> formatService.getFormat(formatId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 형식");
        }
    }

    @Nested
    @DisplayName("exists - 형식 존재 여부 확인")
    class ExistsTest {

        @Test
        @DisplayName("존재하는 형식 - true 반환")
        void exists_True() {
            // given
            Long formatId = 1L;
            given(formatRepository.existsById(formatId)).willReturn(true);

            // when
            boolean result = formatService.exists(formatId);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 형식 - false 반환")
        void exists_False() {
            // given
            Long formatId = 999L;
            given(formatRepository.existsById(formatId)).willReturn(false);

            // when
            boolean result = formatService.exists(formatId);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getFormatEntity - 엔티티 조회")
    class GetFormatEntityTest {

        @Test
        @DisplayName("성공 - 엔티티 반환")
        void success() {
            // given
            Long formatId = 1L;
            given(formatRepository.findById(formatId))
                    .willReturn(Optional.of(format1));

            // when
            Format result = formatService.getFormatEntity(formatId);

            // then
            assertThat(result).isEqualTo(format1);
            assertThat(result.getName()).isEqualTo("문제 풀이");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 형식")
        void fail_NotFound() {
            // given
            Long formatId = 999L;
            given(formatRepository.findById(formatId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> formatService.getFormatEntity(formatId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 형식");
        }
    }
}
