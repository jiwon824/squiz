"""
SQL 파일 파서 - 코드 내 따옴표 이스케이프 처리
Usage:
    python sql_parser.py quiz_course_generated_data.sql
    python sql_parser.py quiz_course_generated_data.sql --execute
"""

import re
import sys
import argparse
from pathlib import Path


def escape_sql_string(content: str) -> str:
    """
    SQL 문자열 내 특수문자 이스케이프
    - 작은따옴표(') → '' (SQL 표준)
    - 백슬래시(\) → \\\\ (MySQL)
    """
    # 이미 이스케이프된 작은따옴표는 건드리지 않음
    # 먼저 '' 를 임시 토큰으로 변환
    content = content.replace("''", "___ESCAPED_QUOTE___")
    # 이스케이프 안 된 작은따옴표 이스케이프
    content = content.replace("'", "''")
    # 임시 토큰 복원
    content = content.replace("___ESCAPED_QUOTE___", "''")
    return content


def process_insert_statement(sql: str) -> str:
    """
    INSERT 문의 VALUES 내 문자열 값들을 파싱하고 이스케이프 처리
    """
    # VALUES 절 찾기
    values_match = re.search(r'VALUES\s*\n?\s*(.+)', sql, re.DOTALL | re.IGNORECASE)
    if not values_match:
        return sql

    return sql  # 복잡한 파싱 대신 아래 방식 사용


def fix_sql_file(input_path: str, output_path: str = None) -> str:
    """
    SQL 파일 읽어서 문자열 내 따옴표 문제 해결

    방식: 문자열 리터럴 내부만 찾아서 이스케이프 처리
    """
    with open(input_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # MySQL에서 문자열은 작은따옴표로 감싸짐
    # 작은따옴표 안의 내용에서 이스케이프 안 된 작은따옴표 찾아서 처리

    result = []
    i = 0
    in_string = False
    string_start = -1

    while i < len(content):
        char = content[i]

        if not in_string:
            if char == "'":
                in_string = True
                string_start = i
                result.append(char)
            else:
                result.append(char)
        else:
            # 문자열 내부
            if char == "'":
                # 다음 문자도 ' 이면 이스케이프된 것
                if i + 1 < len(content) and content[i + 1] == "'":
                    result.append("''")
                    i += 1  # 다음 따옴표 스킵
                else:
                    # 문자열 종료
                    in_string = False
                    result.append(char)
            elif char == "\\":
                # 백슬래시 이스케이프
                if i + 1 < len(content):
                    next_char = content[i + 1]
                    result.append(char)
                    result.append(next_char)
                    i += 1
                else:
                    result.append(char)
            else:
                result.append(char)

        i += 1

    fixed_content = ''.join(result)

    # 출력 파일 저장
    if output_path:
        with open(output_path, 'w', encoding='utf-8') as f:
            f.write(fixed_content)
        print(f"Fixed SQL saved to: {output_path}")

    return fixed_content


def split_into_statements(sql_content: str) -> list:
    """
    SQL 파일을 개별 INSERT 문으로 분리
    세미콜론(;) 기준으로 분리하되, 문자열 내부 세미콜론은 무시
    """
    statements = []
    current = []
    in_string = False

    for i, char in enumerate(sql_content):
        if char == "'" and (i == 0 or sql_content[i-1] != "\\"):
            # 이스케이프된 따옴표가 아니면 문자열 토글
            if i + 1 < len(sql_content) and sql_content[i + 1] == "'":
                # 이스케이프된 작은따옴표 ''
                current.append(char)
                continue
            in_string = not in_string

        if char == ';' and not in_string:
            current.append(char)
            stmt = ''.join(current).strip()
            if stmt and not stmt.startswith('--'):
                statements.append(stmt)
            current = []
        else:
            current.append(char)

    # 마지막 문장
    if current:
        stmt = ''.join(current).strip()
        if stmt and not stmt.startswith('--'):
            statements.append(stmt)

    return statements


def execute_sql(sql_content: str, host: str, user: str, password: str, database: str):
    """
    MySQL에 SQL 실행
    """
    try:
        import mysql.connector
    except ImportError:
        print("mysql-connector-python 설치 필요: pip install mysql-connector-python")
        return

    conn = mysql.connector.connect(
        host=host,
        user=user,
        password=password,
        database=database,
        charset='utf8mb4'
    )
    cursor = conn.cursor()

    statements = split_into_statements(sql_content)
    total = len(statements)
    success = 0
    failed = 0

    for i, stmt in enumerate(statements, 1):
        if not stmt.strip():
            continue
        try:
            cursor.execute(stmt)
            success += 1
            if i % 100 == 0:
                print(f"Progress: {i}/{total} statements executed")
        except Exception as e:
            failed += 1
            print(f"Error at statement {i}: {str(e)[:100]}")
            print(f"Statement preview: {stmt[:200]}...")

    conn.commit()
    cursor.close()
    conn.close()

    print(f"\nComplete: {success} success, {failed} failed out of {total}")


def main():
    parser = argparse.ArgumentParser(description='SQL 파일 파서 및 실행기')
    parser.add_argument('input_file', help='입력 SQL 파일')
    parser.add_argument('--output', '-o', help='출력 파일 (기본: input_fixed.sql)')
    parser.add_argument('--execute', '-e', action='store_true', help='MySQL에 실행')
    parser.add_argument('--host', default='localhost', help='MySQL 호스트')
    parser.add_argument('--user', default='root', help='MySQL 사용자')
    parser.add_argument('--password', '-p', default='', help='MySQL 비밀번호')
    parser.add_argument('--database', '-d', default='modustudy', help='데이터베이스명')

    args = parser.parse_args()

    input_path = Path(args.input_file)
    if not input_path.exists():
        print(f"파일을 찾을 수 없습니다: {input_path}")
        sys.exit(1)

    output_path = args.output or str(input_path.stem) + '_fixed.sql'

    print(f"Processing: {input_path}")
    fixed_sql = fix_sql_file(str(input_path), output_path)

    if args.execute:
        print(f"\nExecuting SQL on {args.host}/{args.database}...")
        execute_sql(fixed_sql, args.host, args.user, args.password, args.database)


if __name__ == '__main__':
    main()
