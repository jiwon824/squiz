#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Java 파일 정리 스크립트
- 한글 인코딩 문제 수정
- 불필요한 로그 제거 (System.out, log.debug/info/trace, printStackTrace)
- 주석처리된 코드 제거 (설명 주석은 유지)
"""

import os
import re
from pathlib import Path

# 제거할 로그 패턴
LOG_PATTERNS = [
    r'^\s*System\.out\.println\([^)]*\);?\s*$',
    r'^\s*System\.out\.print\([^)]*\);?\s*$',
    r'^\s*System\.err\.println\([^)]*\);?\s*$',
    r'^\s*log\.debug\([^)]*\);?\s*$',
    r'^\s*log\.info\([^)]*\);?\s*$',
    r'^\s*log\.trace\([^)]*\);?\s*$',
    r'^\s*e\.printStackTrace\(\);?\s*$',
]

# 설명 주석 키워드 (이런 주석은 유지)
EXPLANATORY_KEYWORDS = [
    'TODO', 'FIXME', 'NOTE', 'XXX', 'HACK', 'BUG',
    '/**', '설명', '주의', '참고', '예시', '예제',
    '@param', '@return', '@throws', '@author', '@since',
    '사용', '호출', '반환', '처리', '검증', '조회', '수정', '삭제', '생성',
]

def is_explanatory_comment(line):
    """설명 주석인지 확인"""
    stripped = line.strip()

    # JavaDoc 주석
    if stripped.startswith('/**') or stripped.startswith('*'):
        return True

    # 설명 키워드 포함
    for keyword in EXPLANATORY_KEYWORDS:
        if keyword in line:
            return True

    # 짧은 주석은 대부분 설명
    if stripped.startswith('//'):
        comment_text = stripped[2:].strip()
        # 코드가 아닌 자연어 설명으로 보이면 유지
        if not any(char in comment_text for char in ['(', ')', '{', '}', ';', '=']):
            return True
        # 주석이 매우 짧으면 (20자 이하) 설명일 가능성 높음
        if len(comment_text) <= 20:
            return True

    return False

def is_commented_code(line):
    """주석처리된 코드인지 확인"""
    stripped = line.strip()

    # 설명 주석이면 코드가 아님
    if is_explanatory_comment(line):
        return False

    # 주석처리된 import문
    if stripped.startswith('//') and 'import ' in stripped:
        return True

    # 주석처리된 코드 패턴
    code_patterns = [
        r'//\s*\w+\s*\(',  # 메서드 호출
        r'//\s*\w+\s*=',   # 변수 할당
        r'//\s*\w+\s*\.',  # 메서드 체이닝
        r'//\s*new\s+',    # 객체 생성
        r'//\s*return\s+', # return문
        r'//\s*if\s*\(',   # if문
        r'//\s*for\s*\(',  # for문
        r'//\s*while\s*\(', # while문
        r'//\s*\.', # 메서드 체이닝 시작
    ]

    for pattern in code_patterns:
        if re.search(pattern, stripped):
            return True

    return False

def should_remove_line(line):
    """라인을 제거해야 하는지 확인"""
    # 로그 패턴 체크
    for pattern in LOG_PATTERNS:
        if re.match(pattern, line):
            return True

    # 주석처리된 코드 체크
    if is_commented_code(line):
        return True

    return False

def process_java_file(file_path):
    """Java 파일 처리"""
    try:
        # UTF-8로 읽기 시도
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
    except UnicodeDecodeError:
        # 실패하면 다른 인코딩으로 시도
        try:
            with open(file_path, 'r', encoding='cp949') as f:
                content = f.read()
        except:
            print(f"❌ 인코딩 오류: {file_path}")
            return False

    lines = content.split('\n')
    modified_lines = []
    removed_count = 0
    in_multiline_comment = False

    for i, line in enumerate(lines):
        # 멀티라인 주석 시작/종료 추적
        if '/*' in line and not line.strip().startswith('//'):
            in_multiline_comment = True
        if '*/' in line:
            in_multiline_comment = False

        # 멀티라인 주석 내부는 건너뛰기 (JavaDoc 등)
        if in_multiline_comment or line.strip().startswith('*'):
            modified_lines.append(line)
            continue

        # 제거할 라인인지 확인
        if should_remove_line(line):
            removed_count += 1
            continue

        modified_lines.append(line)

    # 변경사항이 있으면 파일 저장
    if removed_count > 0:
        new_content = '\n'.join(modified_lines)
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        return removed_count

    return 0

def main():
    """메인 함수"""
    backend_src = Path(r'c:\SSAFY\S14P11D106\modustudy\backend\src')

    if not backend_src.exists():
        print(f"❌ 디렉토리를 찾을 수 없습니다: {backend_src}")
        return

    # 모든 Java 파일 찾기
    java_files = list(backend_src.rglob('*.java'))
    total_files = len(java_files)
    modified_files = 0
    total_removed_lines = 0

    print(f"🔍 총 {total_files}개의 Java 파일을 검사합니다...\n")

    for i, java_file in enumerate(java_files, 1):
        removed = process_java_file(java_file)
        if removed > 0:
            modified_files += 1
            total_removed_lines += removed
            rel_path = java_file.relative_to(backend_src)
            print(f"✅ [{i}/{total_files}] {rel_path} - {removed}줄 제거")
        elif i % 50 == 0:
            print(f"⏳ 진행중... {i}/{total_files}")

    print(f"\n{'='*60}")
    print(f"✨ 작업 완료!")
    print(f"   - 검사한 파일: {total_files}개")
    print(f"   - 수정한 파일: {modified_files}개")
    print(f"   - 제거한 줄: {total_removed_lines}줄")
    print(f"{'='*60}")

if __name__ == '__main__':
    main()
