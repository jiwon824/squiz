package com.ssafy.domain.quiz.entity;

import com.ssafy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_course")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizCourse extends BaseEntity {
    /**
     * 코스 고유 코드 (예: JAVA, PYTHON).
     * 코스 목록/상세 API에서 클라이언트 구분값으로 사용된다.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /**
     * 코스 표시 이름.
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 코스 설명.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 코스 완료 시 부여되는 배지 코드.
     * 배지 이름은 Badge 테이블과 조인하여 응답에서 제공된다.
     */
    @Column(name = "badge_code", length = 50)
    private String badgeCode;

    /**
     * 코스 전체 섹션 수.
     */
    @Column(name = "total_sections")
    private Integer totalSections;

    /**
     * 코스 노출 여부.
     * db에 있는데, 사용자한테는 안 보여주고 싶을 수 있음
     * 특정 시즌에만 보이는 이벤트 퀴즈 코스 등
     */
    @Column(name = "is_active")
    private Boolean isActive;

    /**
     * 목록 정렬 우선순위.
     */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /**
     * 코스에 속한 섹션 목록.
     */
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sectionNumber ASC")
    private List<QuizCourseSection> sections = new ArrayList<>();
}
