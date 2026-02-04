ALTER TABLE board_post
    ADD COLUMN study_id BIGINT NULL AFTER user_id;

CREATE INDEX idx_board_post_study_id ON board_post (study_id);

ALTER TABLE board_post
    ADD CONSTRAINT board_post_ibfk_2 FOREIGN KEY (study_id) REFERENCES study (id);
