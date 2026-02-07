package com.ssafy.domain.quiz.service;

/**
 * FSRS v14 알고리즘 상수 정의
 */
 public final class FsrsConstants {

    private FsrsConstants() {
    }

    // ── FSRS v14 가중치 (17개) — 분(minute) 기반 간격 최적화 ──
    // 시연용으로 빠르게 복습 기간이 되도록 설정함: 1차 복습 ~1–3분 목표

    public static final double[] W = {
            0.0003, 0.0008, 0.002, 0.008, // w[0]~w[3]: 초기 안정성 S0(G) — Rating 1~4 (분 단위, 데모용 축소)
            5.14,                         // w[4]: 초기 난이도 기준값
            1.2,                          // w[5]: 초기 난이도 Rating 감쇠 계수
            0.68,                         // w[6]: 난이도 변화 계수
            0.015,                        // w[7]: 난이도 Mean Reversion 비율
            1.59,                         // w[8]: 안정성 증가 기본 계수
            0.18,                         // w[9]: 안정성 감쇠 지수
            0.3,                          // w[10]: Retrievability 보정 계수
            0.5,                          // w[11]: 오답 시 안정성 기본 계수
            0.95,                         // w[12]: 오답 시 난이도 지수
            0.2,                          // w[13]: 오답 시 이전 안정성 지수
            1.2,                          // w[14]: 오답 시 Retrievability 보정
            0.5,                          // w[15]: Hard 패널티
            2.0                           // w[16]: Easy 보너스
    };

    // ── 응답 시간 기반 자동 Rating 임계값 ──

    /** 5초 초과 → Hard */
    public static final long HARD_THRESHOLD_MS = 5000L;

    /** 2초 이하 → Easy */
    public static final long EASY_THRESHOLD_MS = 2000L;

    // ── 목표 기억 유지율 ──

    public static final double DESIRED_RETENTION = 0.85;

    // ── Power Forgetting Curve 상수 ──

    public static final double DECAY = -0.5;
    public static final double FACTOR = 19.0 / 81.0;

    // ── 카드 상태 ──

    public static final int STATE_NEW = 0;
    public static final int STATE_LEARNING = 1;
    public static final int STATE_REVIEW = 2;
    public static final int STATE_RELEARNING = 3;

    // ── Rating ──

    public static final int RATING_AGAIN = 1;
    public static final int RATING_HARD = 2;
    public static final int RATING_GOOD = 3;
    public static final int RATING_EASY = 4;
}
