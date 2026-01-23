-- =============================================
-- V2: Add updated_at column to all tables that extend BaseEntity
-- =============================================

-- 프로시저로 컬럼 추가 (이미 있으면 무시)
DELIMITER //

CREATE PROCEDURE add_updated_at_if_not_exists(IN table_name VARCHAR(100))
BEGIN
    SET @column_exists = (
        SELECT COUNT(*)
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = table_name
        AND COLUMN_NAME = 'updated_at'
    );

    IF @column_exists = 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', table_name, '` ADD COLUMN `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //

DELIMITER ;

-- 모든 테이블에 updated_at 추가
CALL add_updated_at_if_not_exists('user');
CALL add_updated_at_if_not_exists('user_social_account');
CALL add_updated_at_if_not_exists('login_history');
CALL add_updated_at_if_not_exists('user_schedule');
CALL add_updated_at_if_not_exists('user_organization');
CALL add_updated_at_if_not_exists('profile');
CALL add_updated_at_if_not_exists('refresh_token');
CALL add_updated_at_if_not_exists('friendship');
CALL add_updated_at_if_not_exists('user_block');
CALL add_updated_at_if_not_exists('dm_conversation');
CALL add_updated_at_if_not_exists('direct_message');
CALL add_updated_at_if_not_exists('region');
CALL add_updated_at_if_not_exists('study');
CALL add_updated_at_if_not_exists('study_template');
CALL add_updated_at_if_not_exists('study_comment');
CALL add_updated_at_if_not_exists('study_member');
CALL add_updated_at_if_not_exists('study_session');
CALL add_updated_at_if_not_exists('study_bookmark');
CALL add_updated_at_if_not_exists('study_application');
CALL add_updated_at_if_not_exists('study_leader_review');
CALL add_updated_at_if_not_exists('channel');
CALL add_updated_at_if_not_exists('message');
CALL add_updated_at_if_not_exists('meeting');
CALL add_updated_at_if_not_exists('meeting_participant');
CALL add_updated_at_if_not_exists('meeting_participant_summary');
CALL add_updated_at_if_not_exists('meeting_transcript');
CALL add_updated_at_if_not_exists('meeting_summary');
CALL add_updated_at_if_not_exists('meeting_photo');
CALL add_updated_at_if_not_exists('meeting_recording');
CALL add_updated_at_if_not_exists('meeting_action_item');
CALL add_updated_at_if_not_exists('meeting_chat_message');
CALL add_updated_at_if_not_exists('meeting_audio_recording');
CALL add_updated_at_if_not_exists('meeting_stt_file');
CALL add_updated_at_if_not_exists('meeting_stt_summary');
CALL add_updated_at_if_not_exists('attendance');
CALL add_updated_at_if_not_exists('session_memo');
CALL add_updated_at_if_not_exists('quiz_category');
CALL add_updated_at_if_not_exists('quiz_question_pool');
CALL add_updated_at_if_not_exists('quiz_question_pool_option');
CALL add_updated_at_if_not_exists('quiz_practice_stats');
CALL add_updated_at_if_not_exists('quiz_practice_record');
CALL add_updated_at_if_not_exists('quiz_practice_answer');
CALL add_updated_at_if_not_exists('quiz_contest');
CALL add_updated_at_if_not_exists('quiz_contest_state');
CALL add_updated_at_if_not_exists('quiz_question');
CALL add_updated_at_if_not_exists('quiz_participant');
CALL add_updated_at_if_not_exists('quiz_contest_chat');
CALL add_updated_at_if_not_exists('quiz_answer');
CALL add_updated_at_if_not_exists('study_quiz');
CALL add_updated_at_if_not_exists('study_quiz_question');
CALL add_updated_at_if_not_exists('study_quiz_attempt');
CALL add_updated_at_if_not_exists('wrong_answer_note');
CALL add_updated_at_if_not_exists('quiz_course');
CALL add_updated_at_if_not_exists('quiz_course_section');
CALL add_updated_at_if_not_exists('quiz_course_question');
CALL add_updated_at_if_not_exists('user_course_progress');
CALL add_updated_at_if_not_exists('user_section_attempt');
CALL add_updated_at_if_not_exists('daily_contribution');
CALL add_updated_at_if_not_exists('contribution_detail');
CALL add_updated_at_if_not_exists('user_stats');
CALL add_updated_at_if_not_exists('level_config');
CALL add_updated_at_if_not_exists('reward_policy');
CALL add_updated_at_if_not_exists('exp_transaction');
CALL add_updated_at_if_not_exists('point_transaction');
CALL add_updated_at_if_not_exists('hint_cost');
CALL add_updated_at_if_not_exists('badge');
CALL add_updated_at_if_not_exists('user_badge');
CALL add_updated_at_if_not_exists('penalty');
CALL add_updated_at_if_not_exists('user_penalty');
CALL add_updated_at_if_not_exists('material');
CALL add_updated_at_if_not_exists('material_comment');
CALL add_updated_at_if_not_exists('curriculum');
CALL add_updated_at_if_not_exists('progress');
CALL add_updated_at_if_not_exists('retrospective');
CALL add_updated_at_if_not_exists('retrospective_item');
CALL add_updated_at_if_not_exists('notification');
CALL add_updated_at_if_not_exists('notification_setting');
CALL add_updated_at_if_not_exists('fcm_token');
CALL add_updated_at_if_not_exists('report');
CALL add_updated_at_if_not_exists('ai_feedback');
CALL add_updated_at_if_not_exists('daily_report');
CALL add_updated_at_if_not_exists('daily_item');
CALL add_updated_at_if_not_exists('board_post');
CALL add_updated_at_if_not_exists('board_comment');
CALL add_updated_at_if_not_exists('board_like');
CALL add_updated_at_if_not_exists('team_recruit');
CALL add_updated_at_if_not_exists('team_application');
CALL add_updated_at_if_not_exists('comendle_word');
CALL add_updated_at_if_not_exists('comendle_daily');
CALL add_updated_at_if_not_exists('comendle_attempt');
CALL add_updated_at_if_not_exists('comendle_guess');
CALL add_updated_at_if_not_exists('comendle_streak');
CALL add_updated_at_if_not_exists('it_news');
CALL add_updated_at_if_not_exists('news_bookmark');

-- 프로시저 삭제
DROP PROCEDURE IF EXISTS add_updated_at_if_not_exists;
