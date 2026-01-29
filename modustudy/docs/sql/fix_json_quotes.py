"""
SQL 파일 내 JSON 컬럼의 따옴표 이스케이프 처리
- options 컬럼의 JSON 내 코드에서 "가 제대로 이스케이프 안 된 경우 수정

Usage:
    python fix_json_quotes.py quiz_course_generated_data.sql
"""

import re
import sys
import json
from pathlib import Path


def fix_json_in_sql(sql_content: str) -> str:
    """
    SQL INSERT 문에서 JSON 컬럼 값을 찾아 수정
    """
    lines = sql_content.split('\n')
    fixed_lines = []

    for line in lines:
        if 'MULTIPLE_CHOICE' in line or 'SHORT_ANSWER' in line:
            # JSON 패턴 찾기: '[{...}]' 형태
            # SQL에서 JSON은 '[{"id":...' 형태로 시작
            fixed_line = fix_json_in_line(line)
            fixed_lines.append(fixed_line)
        else:
            fixed_lines.append(line)

    return '\n'.join(fixed_lines)


def fix_json_in_line(line: str) -> str:
    """
    한 줄에서 JSON 배열 찾아서 수정
    패턴: '[{"id": "A", "text": "..."}]'
    """
    # JSON 배열 패턴 찾기 (SQL 문자열 내부)
    # '[{" 로 시작하고 }]' 로 끝나는 부분

    result = []
    i = 0

    while i < len(line):
        # JSON 배열 시작 찾기: '[{"
        if line[i:i+3] == '\'[{':
            # JSON 시작점 찾음
            json_start = i + 1  # '[' 위치

            # 대응하는 }]' 찾기
            bracket_count = 0
            j = json_start
            json_end = -1

            while j < len(line):
                if line[j] == '[':
                    bracket_count += 1
                elif line[j] == ']':
                    bracket_count -= 1
                    if bracket_count == 0:
                        # }]' 패턴 확인
                        if j + 1 < len(line) and line[j + 1] == "'":
                            json_end = j + 1  # ]' 다음 위치
                            break
                j += 1

            if json_end > json_start:
                # JSON 부분 추출 (따옴표 제외)
                json_str = line[json_start:json_end]

                # JSON 수정 시도
                try:
                    fixed_json = fix_json_string(json_str)
                    result.append("'")
                    result.append(fixed_json)
                    result.append("'")
                    i = json_end + 1
                    continue
                except:
                    pass

        result.append(line[i])
        i += 1

    return ''.join(result)


def fix_json_string(json_str: str) -> str:
    """
    JSON 문자열 내부의 이스케이프 문제 수정
    코드 내 " 가 이스케이프 안 된 경우 처리
    """
    # 먼저 파싱 시도
    try:
        json.loads(json_str)
        return json_str  # 이미 유효하면 그대로 반환
    except json.JSONDecodeError as e:
        pass

    # JSON 수정 시도
    # "text": "...코드..." 내부의 따옴표 처리

    # 패턴: "text": "내용" 에서 내용 안의 이스케이프 안 된 따옴표 찾기
    # "text": " 다음부터 다음 ", 또는 "} 전까지가 텍스트 값

    fixed = []
    i = 0

    while i < len(json_str):
        # "text": " 패턴 찾기
        text_match = re.match(r'"text"\s*:\s*"', json_str[i:])
        if text_match:
            # "text": " 부분 추가
            fixed.append(json_str[i:i + text_match.end()])
            i += text_match.end()

            # 텍스트 값 끝 찾기 - "} 또는 ", 패턴
            text_value = []
            while i < len(json_str):
                # 이스케이프된 따옴표
                if json_str[i:i+2] == '\\"':
                    text_value.append('\\"')
                    i += 2
                    continue

                # 텍스트 값 종료 확인: "} 또는 "}] 또는 ",
                if json_str[i] == '"':
                    # 다음 문자 확인
                    next_chars = json_str[i+1:i+3] if i+2 < len(json_str) else json_str[i+1:]
                    if next_chars.startswith('}') or next_chars.startswith(','):
                        # 텍스트 값 종료
                        fixed.append(''.join(text_value))
                        fixed.append('"')
                        i += 1
                        break
                    else:
                        # 내부 따옴표 - 이스케이프 필요
                        text_value.append('\\"')
                        i += 1
                        continue
                else:
                    text_value.append(json_str[i])
                    i += 1
        else:
            fixed.append(json_str[i])
            i += 1

    result = ''.join(fixed)

    # 검증
    try:
        json.loads(result)
        return result
    except:
        # 수정 실패시 원본 반환
        return json_str


def process_sql_file_v2(input_path: str, output_path: str):
    """
    더 robust한 방식으로 SQL 파일 처리
    INSERT 문의 각 VALUES 튜플을 개별 처리
    """
    with open(input_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # INSERT INTO ... VALUES 패턴 찾기
    # 각 튜플 (id, ..., 'JSON', ...) 처리

    result = []
    i = 0
    fixed_count = 0
    error_count = 0

    while i < len(content):
        # '[{" 패턴 찾기 (JSON 배열 시작)
        if content[i:i+4] == "'[{\"":
            json_start = i + 1  # '[' 위치 (' 다음)

            # 대응하는 ]' 찾기
            j = json_start
            bracket_depth = 0
            in_string = False
            escape_next = False

            while j < len(content):
                char = content[j]

                if escape_next:
                    escape_next = False
                    j += 1
                    continue

                if char == '\\':
                    escape_next = True
                    j += 1
                    continue

                if char == '"' and not escape_next:
                    in_string = not in_string

                if not in_string:
                    if char == '[':
                        bracket_depth += 1
                    elif char == ']':
                        bracket_depth -= 1
                        if bracket_depth == 0:
                            # JSON 끝 찾음
                            if j + 1 < len(content) and content[j + 1] == "'":
                                json_end = j + 1  # ]' 위치
                                json_str = content[json_start:json_end]

                                # JSON 검증 및 수정
                                try:
                                    json.loads(json_str)
                                    # 유효한 JSON - 그대로 사용
                                    result.append("'")
                                    result.append(json_str)
                                    result.append("'")
                                except json.JSONDecodeError:
                                    # JSON 수정 필요
                                    fixed_json = aggressive_fix_json(json_str)
                                    result.append("'")
                                    result.append(fixed_json)
                                    result.append("'")
                                    fixed_count += 1

                                i = json_end + 1
                                break
                j += 1
            else:
                # JSON 끝을 못 찾음
                result.append(content[i])
                i += 1
        else:
            result.append(content[i])
            i += 1

    output = ''.join(result)

    with open(output_path, 'w', encoding='utf-8') as f:
        f.write(output)

    print(f"처리 완료!")
    print(f"- 수정된 JSON: {fixed_count}개")
    print(f"- 저장 위치: {output_path}")

    return output


def aggressive_fix_json(json_str: str) -> str:
    """
    JSON 문자열 강제 수정
    "text": "..." 내부의 모든 이스케이프 안 된 따옴표를 이스케이프
    """
    # 단계별 수정

    # 1. "text": "값" 패턴에서 값 내부 따옴표 이스케이프
    pattern = r'("text"\s*:\s*")([^"]*(?:"[^"]*)*?)("(?:\s*[,}]))'

    def fix_text_value(match):
        prefix = match.group(1)  # "text": "
        value = match.group(2)   # 텍스트 값
        suffix = match.group(3)  # " 또는 ",  또는 "}

        # 값 내부의 이스케이프 안 된 따옴표 찾아서 이스케이프
        # 이미 이스케이프된 \" 는 유지
        fixed_value = ""
        i = 0
        while i < len(value):
            if value[i:i+2] == '\\"':
                fixed_value += '\\"'
                i += 2
            elif value[i] == '"':
                fixed_value += '\\"'
                i += 1
            else:
                fixed_value += value[i]
                i += 1

        return prefix + fixed_value + suffix

    # 여러 번 반복 적용
    prev = ""
    current = json_str
    max_iter = 10

    for _ in range(max_iter):
        current = re.sub(pattern, fix_text_value, current)
        if current == prev:
            break
        prev = current

    # 검증
    try:
        json.loads(current)
        return current
    except json.JSONDecodeError as e:
        # 더 공격적인 수정 시도
        return manual_fix_json(json_str)


def manual_fix_json(json_str: str) -> str:
    """
    수동으로 JSON 구조 파싱하며 수정
    """
    result = []
    i = 0

    while i < len(json_str):
        # "text": " 다음부터 값 추출
        if json_str[i:i+8] == '"text":"' or json_str[i:i+9] == '"text": "':
            # "text": " 또는 "text":" 찾음
            match = re.match(r'"text"\s*:\s*"', json_str[i:])
            if match:
                result.append(match.group(0))
                i += match.end()

                # 텍스트 값 추출 - 다음 "} 또는 ", 까지
                value_chars = []
                while i < len(json_str):
                    # 현재 문자와 다음 문자 확인
                    curr = json_str[i]
                    next_two = json_str[i:i+2] if i + 1 < len(json_str) else ""

                    if curr == '\\' and len(next_two) == 2:
                        # 이스케이프 시퀀스
                        value_chars.append(next_two)
                        i += 2
                        continue

                    if curr == '"':
                        # 종료 조건 확인: "} 또는 ", 또는 "]
                        rest = json_str[i+1:i+3]
                        if rest.startswith('}') or rest.startswith(',') or rest.startswith(']'):
                            # 텍스트 값 종료
                            result.append(''.join(value_chars))
                            result.append('"')
                            i += 1
                            break
                        else:
                            # 값 내부 따옴표 - 이스케이프
                            value_chars.append('\\"')
                            i += 1
                            continue

                    value_chars.append(curr)
                    i += 1
                continue

        result.append(json_str[i])
        i += 1

    fixed = ''.join(result)

    try:
        json.loads(fixed)
        return fixed
    except:
        # 최후의 수단 - 원본 반환
        print(f"Warning: Could not fix JSON")
        return json_str


def main():
    if len(sys.argv) < 2:
        print("Usage: python fix_json_quotes.py <input.sql> [output.sql]")
        sys.exit(1)

    input_path = sys.argv[1]

    if len(sys.argv) >= 3:
        output_path = sys.argv[2]
    else:
        p = Path(input_path)
        output_path = str(p.parent / f"{p.stem}_fixed{p.suffix}")

    if not Path(input_path).exists():
        print(f"파일을 찾을 수 없습니다: {input_path}")
        sys.exit(1)

    print(f"입력: {input_path}")
    print(f"출력: {output_path}")
    print("처리 중...")

    process_sql_file_v2(input_path, output_path)


if __name__ == '__main__':
    main()
