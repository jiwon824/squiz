package com.ssafy.domain.gamification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PenaltyType {
    THREE_DAY_QUIT(
            "작심삼일",
            "3회 연속 결석",
            "😅",
            "3회 연속 출석",
            3
    ),
    LATE_KING(
            "지각왕",
            "3회 연속 지각",
            "⏰",
            "3일 연속 정시 출석",
            3
    ),
    GHOST_MEMBER(
            "유령회원",
            "7일 이상 미접속",
            "👻",
            "3일 연속 로그인",
            3
    ),
    SILENT_MEMBER(
            "조용한자",
            "14일간 채팅 0회",
            "🤐",
            "3일간 활발한 채팅",
            3
    ),
    FREE_RIDER(
            "무임승차",
            "자료/회고록 작성 0회 (한 달)",
            "🚌",
            "자료 업로드 3회 또는 회고록 3회",
            3
    );

    private final String name;
    private final String description;
    private final String icon;
    private final String removalCondition;
    private final Integer removalRequired;
}
