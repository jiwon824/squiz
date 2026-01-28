-- V12: 사용자 스터디 선호 설정 컬럼 추가
-- available_days: 가능한 요일 (JSON 배열, 예: ["MON","WED","FRI"])
-- preferred_time_slots: 선호 시간대 (JSON 배열, 예: ["EVENING","NIGHT"])
-- preferred_duration_weeks: 선호 스터디 기간 (2~8주)

ALTER TABLE `user`
    ADD COLUMN `available_days` JSON NULL COMMENT '가능한 요일 (JSON 배열)' AFTER `tech_stacks`,
    ADD COLUMN `preferred_time_slots` JSON NULL COMMENT '선호 시간대 (JSON 배열)' AFTER `available_days`,
    ADD COLUMN `preferred_duration_weeks` INT NULL COMMENT '선호 스터디 기간 (주)' AFTER `preferred_time_slots`;
