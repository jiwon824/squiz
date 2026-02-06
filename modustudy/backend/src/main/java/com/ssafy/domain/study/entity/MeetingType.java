package com.ssafy.domain.study.entity;

public enum MeetingType {
    ONLINE("온라인"),
    OFFLINE("오프라인"),
    HYBRID("혼합");

    private final String description;

    MeetingType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
