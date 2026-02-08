#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
퀴즈 SQL 파일 분석 및 검증 스크립트
"""

import re
import json
from typing import List, Dict, Any
from dataclasses import dataclass, field

@dataclass
class Quiz:
    """퀴즈 문제 데이터 클래스"""
    line_number: int
    course_id: int
    section_number: int
    question_text: str
    question_type: str
    options: List[str]
    correct_answer: str
    answer_keywords: List[str]
    explanation: str
    difficulty_level: str
    raw_sql: str

    issues: List[str] = field(default_factory=list)
    needs_fix: bool = False


class QuizAnalyzer:
    """퀴즈 문제 분석기"""

    def __init__(self, sql_file_path: str):
        self.sql_file_path = sql_file_path
        self.quizzes: List[Quiz] = []
        self.issues_by_category = {
            'answer_error': [],
            'explanation_issue': [],
            'options_issue': [],
            'question_issue': []
        }

    def parse_sql_file(self):
        """SQL 파일 파싱"""
        print(f"파일 읽는 중: {self.sql_file_path}")

        with open(self.sql_file_path, 'r', encoding='utf-8') as f:
            content = f.read()

        # INSERT INTO quiz_question VALUES 패턴 찾기
        # 패턴: VALUES 다음에 오는 괄호로 묶인 데이터들
        pattern = r"INSERT INTO `quiz_question`[^;]+VALUES\s*\n((?:\s*\([^)]+\),?\s*\n?)+);"

        matches = re.finditer(pattern, content, re.MULTILINE | re.DOTALL)

        total_count = 0
        for match in matches:
            values_block = match.group(1)
            # 각 행 파싱 (괄호로 묶인 부분)
            row_pattern = r"\(\s*(\d+),\s*(\d+),\s*(\d+),\s*'([^']*(?:''[^']*)*)',\s*'([^']*)',\s*'(\[[^\]]*\])',\s*'([^']*)',\s*'(\[[^\]]*\])',\s*'([^']*(?:''[^']*)*)',\s*'([^']*)'\s*\)"

            for row_match in re.finditer(row_pattern, values_block):
                total_count += 1
                try:
                    quiz = self._parse_quiz_row(row_match, total_count)
                    self.quizzes.append(quiz)
                except Exception as e:
                    print(f"  [경고] 라인 파싱 실패 (문제 #{total_count}): {e}")

        print(f"총 {len(self.quizzes)}개 문제 파싱 완료")

    def _parse_quiz_row(self, match, line_num: int) -> Quiz:
        """개별 퀴즈 행 파싱"""
        # SQL에서 ''는 '로 이스케이프됨
        def unescape_sql(s: str) -> str:
            return s.replace("''", "'").replace("\\n", "\n")

        course_id = int(match.group(2))
        section_number = int(match.group(3))
        question_text = unescape_sql(match.group(4))
        question_type = match.group(5)

        # JSON 배열 파싱
        try:
            options_str = match.group(6).replace("''", "'")
            options = json.loads(options_str) if options_str.strip() != '[]' else []
        except json.JSONDecodeError as e:
            print(f"  [경고] options JSON 파싱 실패 (문제 #{line_num}): {e}")
            options = []

        correct_answer = match.group(7)

        try:
            keywords_str = match.group(8).replace("''", "'")
            answer_keywords = json.loads(keywords_str) if keywords_str.strip() != '[]' else []
        except json.JSONDecodeError as e:
            print(f"  [경고] answer_keywords JSON 파싱 실패 (문제 #{line_num}): {e}")
            answer_keywords = []

        explanation = unescape_sql(match.group(9))
        difficulty_level = match.group(10)

        return Quiz(
            line_number=line_num,
            course_id=course_id,
            section_number=section_number,
            question_text=question_text,
            question_type=question_type,
            options=options,
            correct_answer=correct_answer,
            answer_keywords=answer_keywords,
            explanation=explanation,
            difficulty_level=difficulty_level,
            raw_sql=match.group(0)
        )

    def analyze_quizzes(self):
        """1차, 2차, 3차 검토 통합 분석"""
        print("\n=== 퀴즈 분석 시작 ===")

        for idx, quiz in enumerate(self.quizzes, 1):
            # 1차 검토: 구조 검증
            self._check_structure(quiz, idx)

            # 2차 검토: 정답 및 해설 정확성
            self._check_answer_accuracy(quiz, idx)

            # 3차 검토: 선택지 및 문제 품질
            self._check_quality(quiz, idx)

            # 진행 상황 출력
            if idx % 1000 == 0:
                print(f"  분석 진행: {idx}/{len(self.quizzes)} 문제")

        print(f"\n분석 완료: 총 {len(self.quizzes)}개 문제")
        self._print_summary()

    def _check_structure(self, quiz: Quiz, idx: int):
        """1차 검토: 구조 검증"""
        # MULTIPLE_CHOICE인데 options가 없는 경우
        if quiz.question_type == 'MULTIPLE_CHOICE':
            if not quiz.options or len(quiz.options) < 2:
                quiz.issues.append(f"[구조] 객관식인데 선택지가 {len(quiz.options)}개")
                quiz.needs_fix = True
                self.issues_by_category['options_issue'].append(quiz)

            # 정답이 선택지에 없는 경우
            if quiz.correct_answer and quiz.correct_answer not in quiz.options:
                quiz.issues.append(f"[정답오류] 정답 '{quiz.correct_answer}'가 선택지에 없음")
                quiz.needs_fix = True
                self.issues_by_category['answer_error'].append(quiz)

        # 빈 해설
        if not quiz.explanation or len(quiz.explanation.strip()) < 10:
            quiz.issues.append("[해설] 해설이 너무 짧거나 없음")
            self.issues_by_category['explanation_issue'].append(quiz)

    def _check_answer_accuracy(self, quiz: Quiz, idx: int):
        """2차 검토: 정답 및 해설 정확성 (휴리스틱 기반)"""
        # 해설에 "정답:"이 있는데 실제 정답과 다른 경우 감지
        if "정답:" in quiz.explanation or "Answer:" in quiz.explanation:
            # 해설에서 정답 추출 시도
            explanation_lower = quiz.explanation.lower()
            correct_lower = quiz.correct_answer.lower()

            # 해설에 정답이 언급되는지 확인
            if correct_lower and correct_lower not in explanation_lower:
                # 선택지 번호 확인 (1, 2, 3, 4 등)
                if quiz.question_type == 'MULTIPLE_CHOICE':
                    for i, opt in enumerate(quiz.options, 1):
                        if opt.lower() == correct_lower:
                            # 정답 번호가 해설에 없는지 확인
                            if str(i) not in quiz.explanation:
                                quiz.issues.append(f"[해설] 해설에 정답 '{quiz.correct_answer}' 언급 없음")
                                self.issues_by_category['explanation_issue'].append(quiz)
                            break

        # 정답이 비어있는 경우
        if not quiz.correct_answer or quiz.correct_answer.strip() == '':
            quiz.issues.append("[정답오류] 정답이 비어있음")
            quiz.needs_fix = True
            self.issues_by_category['answer_error'].append(quiz)

    def _check_quality(self, quiz: Quiz, idx: int):
        """3차 검토: 선택지 및 문제 품질"""
        # 선택지 중복 검사
        if quiz.question_type == 'MULTIPLE_CHOICE' and quiz.options:
            unique_options = set(quiz.options)
            if len(unique_options) < len(quiz.options):
                quiz.issues.append("[선택지] 중복된 선택지 존재")
                self.issues_by_category['options_issue'].append(quiz)

        # 문제 텍스트가 너무 짧은 경우
        if len(quiz.question_text.strip()) < 10:
            quiz.issues.append("[문제] 문제 텍스트가 너무 짧음")
            self.issues_by_category['question_issue'].append(quiz)

    def _print_summary(self):
        """분석 요약 출력"""
        total_issues = sum(1 for q in self.quizzes if q.issues)
        needs_fix = sum(1 for q in self.quizzes if q.needs_fix)

        print(f"\n=== 분석 요약 ===")
        print(f"총 문제 수: {len(self.quizzes)}")
        print(f"이슈가 있는 문제: {total_issues}")
        print(f"수정 필요 문제: {needs_fix}")
        print(f"\n카테고리별:")
        print(f"  - 정답 오류: {len(self.issues_by_category['answer_error'])}")
        print(f"  - 해설 문제: {len(self.issues_by_category['explanation_issue'])}")
        print(f"  - 선택지 문제: {len(self.issues_by_category['options_issue'])}")
        print(f"  - 문제 내용: {len(self.issues_by_category['question_issue'])}")

    def generate_report(self, output_path: str):
        """상세 분석 리포트 생성"""
        print(f"\n리포트 생성 중: {output_path}")

        with open(output_path, 'w', encoding='utf-8') as f:
            f.write("# 퀴즈 문제 분석 리포트\n\n")

            # 통계
            total_issues = sum(1 for q in self.quizzes if q.issues)
            needs_fix = sum(1 for q in self.quizzes if q.needs_fix)

            f.write("## 분석 통계\n")
            f.write(f"- 총 검토 문제: {len(self.quizzes)}개\n")
            f.write(f"- 이슈 발견 문제: {total_issues}개\n")
            f.write(f"- 수정 필요 문제: {needs_fix}개\n\n")

            f.write("### 카테고리별 통계\n")
            f.write(f"- 정답 오류: {len(self.issues_by_category['answer_error'])}개\n")
            f.write(f"- 해설 문제: {len(self.issues_by_category['explanation_issue'])}개\n")
            f.write(f"- 선택지 문제: {len(self.issues_by_category['options_issue'])}개\n")
            f.write(f"- 문제 내용: {len(self.issues_by_category['question_issue'])}개\n\n")

            # 심각한 문제만 상세 출력
            f.write("## 수정 필요 문제 상세\n\n")

            critical_quizzes = [q for q in self.quizzes if q.needs_fix]

            for quiz in critical_quizzes[:100]:  # 최대 100개만 출력
                f.write(f"### 문제 #{quiz.line_number}\n")
                f.write(f"**코스**: {quiz.course_id}, **섹션**: {quiz.section_number}\n")
                f.write(f"**문제**: {quiz.question_text[:100]}...\n\n")

                if quiz.question_type == 'MULTIPLE_CHOICE':
                    f.write(f"**선택지**: {quiz.options}\n")

                f.write(f"**정답**: {quiz.correct_answer}\n")
                f.write(f"**해설**: {quiz.explanation[:200]}...\n\n")

                f.write("**발견된 이슈**:\n")
                for issue in quiz.issues:
                    f.write(f"- {issue}\n")
                f.write("\n---\n\n")

            if len(critical_quizzes) > 100:
                f.write(f"\n... 외 {len(critical_quizzes) - 100}개 문제 생략\n")

        print(f"리포트 생성 완료")


def main():
    sql_file = r"C:\SSAFY\S14P11D106\modustudy\docs\sql\quiz_course_generated_data(sol).sql"
    report_file = r"C:\SSAFY\S14P11D106\modustudy\docs\sql\quiz_analysis_report.md"

    analyzer = QuizAnalyzer(sql_file)
    analyzer.parse_sql_file()
    analyzer.analyze_quizzes()
    analyzer.generate_report(report_file)

    print("\n분석 완료!")


if __name__ == "__main__":
    main()
