package com.ssafy.domain.study.entity;

public enum StudyType {
    PLANNED("계획 스터디"),
    LIGHTNING("번개 스터디");

    private final String description;

    StudyType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
