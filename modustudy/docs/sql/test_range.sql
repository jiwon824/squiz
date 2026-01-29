INSERT INTO `quiz_course_question` (`id`, `quiz_course_id`, `section_number`, `question_number`, `question_text`, `question_type`, `options`, `correct_answer`, `explanation`)
VALUES
    (1575, 5, 1, 159, 'Optional을 사용할 때, 값이 없을 경우 기본값을 제공하는 메서드는 무엇인가요?  optionalString.orElse(____);', 'MULTIPLE_CHOICE', '[{"id": "A", "text": "\"기본값\""}, {"id": "B", "text": "null"}, {"id": "C", "text": "Optional.empty()"}, {"id": "D", "text": "getString()"}]', 'A', 'orElse 메서드는 Optional이 값이 없을 경우 제공할 기본값을 설정합니다.');
