package com.ssafy.domain.study.entity;

public enum Difficulty {
    BEGINNER("입문"),
    ELEMENTARY("초급"),
    INTERMEDIATE("중급"),
    ADVANCED("고급");

    private final String description;

    Difficulty(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
