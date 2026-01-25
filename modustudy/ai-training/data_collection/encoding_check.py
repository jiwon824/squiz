"""
한글 깨짐/인코딩 오류 검사
"""
import json
import os
import sys
import re

sys.stdout.reconfigure(encoding='utf-8')

# 깨진 문자 패턴
BROKEN_PATTERNS = [
    r'[�ï¿½]',           # 대체 문자
    r'[\ufffd]',          # Unicode replacement char
    r'[ã¤ã¥ã£ã¢]',       # 잘못된 인코딩
    r'[\x00-\x08]',       # 제어 문자
    r'[^\x00-\x7F가-힣ㄱ-ㅎㅏ-ㅣ\s\.,!?\-\(\)\[\]{}:;\'\"@#$%^&*+=/<>~`\\|_0-9a-zA-Z]',  # 비정상 문자
]

# 의미없는 반복 패턴
GIBBERISH_PATTERNS = [
    r'(.)\1{10,}',        # 같은 문자 10회 이상 반복
    r'(\s{5,})',          # 공백 5개 이상 연속
    r'[ㅋㅎㅠㅜ]{10,}',    # ㅋㅋㅋ 등 10회 이상
]


def check_text_quality(text, source=""):
    """텍스트 품질 검사"""
    issues = []

    # 1. 깨진 문자 검사
    for pattern in BROKEN_PATTERNS:
        matches = re.findall(pattern, text)
        if matches:
            issues.append(f"깨진문자: {matches[:5]}")

    # 2. 의미없는 반복 검사
    for pattern in GIBBERISH_PATTERNS:
        if re.search(pattern, text):
            issues.append(f"반복패턴: {pattern}")

    # 3. 빈 텍스트 또는 너무 짧은 텍스트
    if len(text.strip()) < 50:
        issues.append("너무짧음")

    # 4. 한글이 거의 없는 경우
    korean_chars = sum(1 for c in text if '가' <= c <= '힣' or 'ㄱ' <= c <= 'ㅣ')
    if len(text) > 100 and korean_chars / len(text) < 0.1:
        issues.append(f"한글부족({korean_chars/len(text)*100:.1f}%)")

    return issues


def analyze_github():
    """GitHub 데이터 검사"""
    path = "./github_data/github_training_data.json"
    if not os.path.exists(path):
        print("[!] GitHub 데이터 없음")
        return

    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)

    print("=" * 60)
    print("[GitHub 인코딩/품질 검사]")
    print("=" * 60)

    problematic = []
    for i, item in enumerate(data):
        text = item.get("conversation", "")
        issues = check_text_quality(text, item.get("repo", ""))
        if issues:
            problematic.append({
                "index": i,
                "repo": item.get("repo", ""),
                "issues": issues,
                "sample": text[:100]
            })

    if problematic:
        print(f"\n[문제 발견] {len(problematic)}개 / {len(data)}개 ({len(problematic)/len(data)*100:.1f}%)")
        print("\n샘플 (최대 5개):")
        for p in problematic[:5]:
            print(f"  #{p['index']} [{p['repo']}]")
            print(f"    이슈: {p['issues']}")
            print(f"    내용: {p['sample'][:80]}...")
            print()
    else:
        print(f"\n[OK] 문제 없음 ({len(data)}개 검사)")

    return problematic


def analyze_youtube():
    """YouTube 데이터 검사"""
    path = "./youtube_data/youtube_training_data.json"
    if not os.path.exists(path):
        print("[!] YouTube 데이터 없음")
        return

    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)

    print("\n" + "=" * 60)
    print("[YouTube 인코딩/품질 검사]")
    print("=" * 60)

    problematic = []
    for i, item in enumerate(data):
        text = item.get("text", "")
        issues = check_text_quality(text, item.get("source", ""))
        if issues:
            problematic.append({
                "index": i,
                "source": item.get("source", ""),
                "title": item.get("title", "")[:50],
                "issues": issues,
                "sample": text[:100]
            })

    if problematic:
        print(f"\n[문제 발견] {len(problematic)}개 / {len(data)}개 ({len(problematic)/len(data)*100:.1f}%)")
        print("\n샘플 (최대 10개):")
        for p in problematic[:10]:
            print(f"  #{p['index']} [{p['source']}] {p['title']}")
            print(f"    이슈: {p['issues']}")
            print(f"    내용: {p['sample'][:80]}...")
            print()
    else:
        print(f"\n[OK] 문제 없음 ({len(data)}개 검사)")

    return problematic


def show_random_samples():
    """랜덤 샘플 확인"""
    import random

    print("\n" + "=" * 60)
    print("[랜덤 샘플 확인]")
    print("=" * 60)

    # GitHub
    gh_path = "./github_data/github_training_data.json"
    if os.path.exists(gh_path):
        with open(gh_path, "r", encoding="utf-8") as f:
            gh_data = json.load(f)
        print("\n--- GitHub 랜덤 샘플 ---")
        for item in random.sample(gh_data, min(3, len(gh_data))):
            print(f"[{item['repo']}]")
            print(f"{item['conversation'][:300]}...")
            print()

    # YouTube
    yt_path = "./youtube_data/youtube_training_data.json"
    if os.path.exists(yt_path):
        with open(yt_path, "r", encoding="utf-8") as f:
            yt_data = json.load(f)
        print("\n--- YouTube 랜덤 샘플 ---")
        for item in random.sample(yt_data, min(3, len(yt_data))):
            print(f"[{item['source']}] {item['title'][:40]}")
            print(f"{item['text'][:300]}...")
            print()


if __name__ == "__main__":
    gh_issues = analyze_github()
    yt_issues = analyze_youtube()

    print("\n" + "=" * 60)
    print("[최종 결과]")
    print("=" * 60)
    gh_count = len(gh_issues) if gh_issues else 0
    yt_count = len(yt_issues) if yt_issues else 0

    if gh_count == 0 and yt_count == 0:
        print("모든 데이터 정상! 학습 진행 가능")
    else:
        print(f"GitHub 문제: {gh_count}개")
        print(f"YouTube 문제: {yt_count}개")
        print("\n문제 데이터 제거 후 진행 권장")

    # 랜덤 샘플 보기
    print("\n랜덤 샘플 확인하려면 'show_random_samples()' 호출")
    show_random_samples()
