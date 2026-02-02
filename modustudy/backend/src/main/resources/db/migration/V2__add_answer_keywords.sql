-- 서술형 문제 채점용 키워드 컬럼 추가
ALTER TABLE study_quiz_question
ADD COLUMN answer_keywords JSON DEFAULT NULL AFTER correct_answer;
