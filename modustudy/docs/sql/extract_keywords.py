#!/usr/bin/env python3
"""
서술형 문제 핵심 키워드 추출 스크립트

GPT API를 사용하여 quiz_course_question 테이블의 SHORT_ANSWER 문제에서
핵심 키워드를 추출하고 keywords 컬럼에 저장합니다.

사용법:
    python extract_keywords.py --dry-run  # 테스트 (DB 수정 안함)
    python extract_keywords.py            # 실제 실행

환경변수:
    OPENAI_API_KEY: OpenAI API 키
    DB_HOST: MySQL 호스트 (기본값: localhost)
    DB_PORT: MySQL 포트 (기본값: 3306)
    DB_NAME: 데이터베이스 이름 (기본값: squiz)
    DB_USER: DB 사용자 (기본값: root)
    DB_PASSWORD: DB 비밀번호
"""

import os
import sys
import json
import time
import argparse
import logging
from typing import List, Dict, Optional
from dataclasses import dataclass

import mysql.connector
from mysql.connector import Error
from openai import OpenAI

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler('extract_keywords.log', encoding='utf-8')
    ]
)
logger = logging.getLogger(__name__)


@dataclass
class Question:
    """퀴즈 문제 데이터 클래스"""
    id: int
    question_text: str
    correct_answer: str
    keywords: Optional[str] = None


class KeywordExtractor:
    """GPT를 활용한 키워드 추출기"""

    def __init__(self, api_key: str):
        self.client = OpenAI(api_key=api_key)
        self.model = "gpt-4o-mini"  # 비용 효율적인 모델 사용

    def extract_keywords(self, question_text: str, correct_answer: str) -> List[str]:
        """
        문제와 정답에서 핵심 키워드를 추출합니다.

        Args:
            question_text: 문제 텍스트
            correct_answer: 정답 텍스트

        Returns:
            핵심 키워드 리스트
        """
        prompt = f"""당신은 IT/CS 퀴즈 채점 시스템의 키워드 추출 전문가입니다.

주어진 서술형 문제와 정답을 분석하여, 사용자의 답변에 반드시 포함되어야 하는 핵심 키워드를 추출해주세요.

## 규칙
1. 키워드는 정답의 핵심 개념을 나타내는 단어/구문입니다.
2. 3~7개의 키워드를 추출해주세요.
3. 너무 일반적인 단어(예: "것", "위해", "하기")는 제외하세요.
4. 영어 약어(DDL, SQL 등)는 그대로 유지하세요.
5. 정답이 짧은 단어(1-2단어)인 경우, 그 단어 자체를 키워드로 사용하세요.
6. 정답에 "또는"이 포함된 경우, 각 선택지를 개별 키워드로 추출하세요.

## 문제
{question_text}

## 정답
{correct_answer}

## 출력 형식
JSON 배열로만 응답해주세요. 설명 없이 키워드 배열만 출력하세요.
예: ["키워드1", "키워드2", "키워드3"]
"""

        try:
            response = self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": "JSON 배열 형식으로만 응답하세요."},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.3,  # 일관된 결과를 위해 낮은 temperature
                max_tokens=200
            )

            content = response.choices[0].message.content.strip()

            # JSON 파싱
            # 코드 블록으로 감싸진 경우 처리
            if content.startswith("```"):
                content = content.split("```")[1]
                if content.startswith("json"):
                    content = content[4:]
                content = content.strip()

            keywords = json.loads(content)

            if isinstance(keywords, list):
                return [str(k).strip() for k in keywords if k]
            else:
                logger.warning(f"예상치 못한 응답 형식: {content}")
                return []

        except json.JSONDecodeError as e:
            logger.error(f"JSON 파싱 실패: {content}, 에러: {e}")
            return []
        except Exception as e:
            logger.error(f"GPT API 호출 실패: {e}")
            return []


class DatabaseManager:
    """MySQL 데이터베이스 관리자"""

    def __init__(self, host: str, port: int, database: str, user: str, password: str):
        self.config = {
            'host': host,
            'port': port,
            'database': database,
            'user': user,
            'password': password,
            'charset': 'utf8mb4',
            'collation': 'utf8mb4_unicode_ci'
        }
        self.connection = None

    def connect(self):
        """데이터베이스에 연결합니다."""
        try:
            self.connection = mysql.connector.connect(**self.config)
            logger.info(f"DB 연결 성공: {self.config['host']}:{self.config['port']}/{self.config['database']}")
        except Error as e:
            logger.error(f"DB 연결 실패: {e}")
            raise

    def disconnect(self):
        """데이터베이스 연결을 종료합니다."""
        if self.connection and self.connection.is_connected():
            self.connection.close()
            logger.info("DB 연결 종료")

    def get_short_answer_questions(self) -> List[Question]:
        """SHORT_ANSWER 타입의 문제를 조회합니다."""
        cursor = self.connection.cursor(dictionary=True)

        query = """
            SELECT id, question_text, correct_answer, keywords
            FROM quiz_course_question
            WHERE question_type = 'SHORT_ANSWER'
            ORDER BY id
        """

        cursor.execute(query)
        rows = cursor.fetchall()
        cursor.close()

        questions = [
            Question(
                id=row['id'],
                question_text=row['question_text'],
                correct_answer=row['correct_answer'],
                keywords=row['keywords']
            )
            for row in rows
        ]

        logger.info(f"SHORT_ANSWER 문제 {len(questions)}개 조회됨")
        return questions

    def update_keywords(self, question_id: int, keywords: List[str]) -> bool:
        """문제의 키워드를 업데이트합니다."""
        cursor = self.connection.cursor()

        keywords_json = json.dumps(keywords, ensure_ascii=False)

        query = """
            UPDATE quiz_course_question
            SET keywords = %s
            WHERE id = %s
        """

        try:
            cursor.execute(query, (keywords_json, question_id))
            self.connection.commit()
            cursor.close()
            return True
        except Error as e:
            logger.error(f"키워드 업데이트 실패 (id={question_id}): {e}")
            self.connection.rollback()
            cursor.close()
            return False


def main():
    parser = argparse.ArgumentParser(description='서술형 문제 키워드 추출')
    parser.add_argument('--dry-run', action='store_true', help='테스트 모드 (DB 수정 안함)')
    parser.add_argument('--limit', type=int, default=0, help='처리할 문제 수 제한 (0=전체)')
    parser.add_argument('--skip-existing', action='store_true', default=True, help='이미 키워드가 있는 문제 건너뛰기')
    args = parser.parse_args()

    # 환경변수에서 설정 읽기 (없으면 기본값 사용)
    api_key = os.environ.get('OPENAI_API_KEY', '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')

    db_config = {
        'host': os.environ.get('DB_HOST', 'localhost'),
        'port': int(os.environ.get('DB_PORT', 3306)),
        'database': os.environ.get('DB_NAME', 'ssafy_web_db'),
        'user': os.environ.get('DB_USER', 'ssafy'),
        'password': os.environ.get('DB_PASSWORD', 'ssafy')
    }

    # 초기화
    extractor = KeywordExtractor(api_key)
    db = DatabaseManager(**db_config)

    try:
        db.connect()

        # 문제 조회
        questions = db.get_short_answer_questions()

        if args.limit > 0:
            questions = questions[:args.limit]
            logger.info(f"처리 제한: {args.limit}개")

        # 통계
        total = len(questions)
        processed = 0
        skipped = 0
        failed = 0

        for i, q in enumerate(questions, 1):
            # 이미 키워드가 있으면 건너뛰기
            if args.skip_existing and q.keywords:
                skipped += 1
                logger.debug(f"[{i}/{total}] ID {q.id}: 이미 키워드 존재, 건너뜀")
                continue

            logger.info(f"[{i}/{total}] ID {q.id} 처리 중...")
            logger.debug(f"  문제: {q.question_text[:50]}...")
            logger.debug(f"  정답: {q.correct_answer[:50]}...")

            # 키워드 추출
            keywords = extractor.extract_keywords(q.question_text, q.correct_answer)

            if not keywords:
                failed += 1
                logger.warning(f"  키워드 추출 실패")
                continue

            logger.info(f"  추출된 키워드: {keywords}")

            # DB 업데이트
            if args.dry_run:
                logger.info(f"  [DRY-RUN] DB 업데이트 건너뜀")
            else:
                if db.update_keywords(q.id, keywords):
                    processed += 1
                else:
                    failed += 1

            # Rate limiting (GPT API 제한 방지)
            time.sleep(0.5)

        # 결과 출력
        logger.info("=" * 50)
        logger.info("처리 완료!")
        logger.info(f"  전체: {total}개")
        logger.info(f"  처리됨: {processed}개")
        logger.info(f"  건너뜀: {skipped}개")
        logger.info(f"  실패: {failed}개")

    finally:
        db.disconnect()


if __name__ == '__main__':
    main()
