#!/usr/bin/env python
"""
SQL 파일 내 JSON 따옴표 이스케이프 수정
MySQL JSON 타입에서 문자열 내부 따옴표는 \\" 로 이스케이프해야 함
"""

import sys


def fix_json_escapes(sql):
    """JSON 문자열 내부의 \" 를 \\" 로 변경"""
    result = []
    i = 0
    in_sql_string = False
    in_json = False

    while i < len(sql):
        char = sql[i]

        # 작은따옴표 시작/끝 확인
        if char == "'" and (i == 0 or sql[i-1:i] != '\\'):
            if i + 1 < len(sql) and sql[i+1] == "'":
                # 이스케이프된 ''
                result.append("''")
                i += 2
                continue
            in_sql_string = not in_sql_string
            result.append(char)
            i += 1
            continue

        # SQL 문자열 내부에서 JSON 시작/끝 확인
        if in_sql_string:
            if sql[i:i+2] == '[{':
                in_json = True
            elif sql[i:i+2] == '}]':
                in_json = False
                result.append('}]')
                i += 2
                continue

        # JSON 내부에서 \" 패턴 -> \\" 로 변경
        # 단, 이미 \\" 인 경우는 건너뜀
        if in_sql_string and in_json:
            # 현재 위치에서 패턴 확인
            if sql[i:i+2] == '\\"':
                # 이미 \\" 인지 확인 (이전 문자가 \가 아님)
                if i > 0 and sql[i-1] == '\\':
                    # 이미 \\" 형태
                    result.append(char)
                    i += 1
                    continue
                # \" -> \\"
                result.append('\\\\"')
                i += 2
                continue

        result.append(char)
        i += 1

    return ''.join(result)


def main():
    input_file = sys.argv[1] if len(sys.argv) > 1 else 'quiz_course_generated_data_fixed.sql'
    output_file = sys.argv[2] if len(sys.argv) > 2 else 'quiz_course_generated_data_fixed2.sql'

    with open(input_file, 'r', encoding='utf-8') as f:
        content = f.read()

    fixed = fix_json_escapes(content)

    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(fixed)

    # 변경 확인
    orig_backslash_quote = content.count('\\"')
    fixed_double_backslash = fixed.count('\\\\"')

    print(f'Original \\" count: {orig_backslash_quote}')
    print(f'Fixed \\\\" count: {fixed_double_backslash}')
    print(f'Saved to {output_file}')


if __name__ == '__main__':
    main()
