#!/usr/bin/env python
"""
SQL 문자열 내부 줄바꿈 수정 스크립트
"""

import sys

def fix_multiline_strings(sql):
    """SQL 문자열 내부의 줄바꿈을 공백으로 변경"""
    result = []
    i = 0
    in_string = False

    while i < len(sql):
        char = sql[i]

        if char == "'":
            # 작은따옴표
            if i + 1 < len(sql) and sql[i+1] == "'":
                # 이스케이프된 '' - 그대로 추가
                result.append("''")
                i += 2
                continue
            in_string = not in_string
            result.append(char)
        elif char == '\n' and in_string:
            # 문자열 내부 줄바꿈 -> 공백으로 대체
            result.append(' ')
        else:
            result.append(char)

        i += 1

    return ''.join(result)


def main():
    input_file = sys.argv[1] if len(sys.argv) > 1 else 'quiz_course_generated_data.sql'
    output_file = sys.argv[2] if len(sys.argv) > 2 else 'quiz_course_generated_data_fixed.sql'

    with open(input_file, 'r', encoding='utf-8') as f:
        content = f.read()

    fixed = fix_multiline_strings(content)

    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(fixed)

    original_lines = content.count('\n')
    fixed_lines = fixed.count('\n')
    print(f'Original lines: {original_lines}')
    print(f'Fixed lines: {fixed_lines}')
    print(f'Lines merged: {original_lines - fixed_lines}')
    print(f'Saved to {output_file}')


if __name__ == '__main__':
    main()
