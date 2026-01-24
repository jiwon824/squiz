"""
수집 데이터 품질 검사 스크립트
"""
import json
import os
import sys
from collections import Counter

# Windows 인코딩 설정
sys.stdout.reconfigure(encoding='utf-8')

def analyze_github_data():
    """GitHub 데이터 분석"""
    path = "./github_data/github_training_data.json"
    if not os.path.exists(path):
        print("[!] GitHub 데이터 없음")
        return

    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)

    print("=" * 60)
    print("[GitHub 데이터 품질 검사]")
    print("=" * 60)
    print(f"총 샘플 수: {len(data)}개")

    # 레포별 분포
    repos = Counter(d["repo"] for d in data)
    print(f"\n[레포별 분포] (상위 10개):")
    for repo, count in repos.most_common(10):
        print(f"  - {repo}: {count}개")

    # 텍스트 길이 분석
    conv_lengths = [len(d["conversation"]) for d in data]
    print(f"\n[대화 길이]:")
    print(f"  - 평균: {sum(conv_lengths)/len(conv_lengths):.0f}자")
    print(f"  - 최소: {min(conv_lengths)}자")
    print(f"  - 최대: {max(conv_lengths)}자")

    # 라벨 분포
    all_labels = []
    for d in data:
        all_labels.extend(d.get("labels", []))
    label_counts = Counter(all_labels)
    print(f"\n[라벨 분포] (상위 10개):")
    for label, count in label_counts.most_common(10):
        print(f"  - {label}: {count}개")

    # 빈 데이터 체크
    empty_conv = sum(1 for d in data if len(d["conversation"].strip()) < 50)
    print(f"\n[품질 이슈]:")
    print(f"  - 50자 미만 대화: {empty_conv}개 ({empty_conv/len(data)*100:.1f}%)")

    # 샘플 출력
    print(f"\n[샘플 데이터] (첫 번째):")
    sample = data[0]
    print(f"  - repo: {sample['repo']}")
    print(f"  - summary: {sample['summary'][:80]}...")
    print(f"  - conversation 앞 200자:")
    print(f"    {sample['conversation'][:200]}...")

    return data


def analyze_youtube_data():
    """YouTube 데이터 분석"""
    path = "./youtube_data/youtube_training_data.json"
    if not os.path.exists(path):
        print("[!] YouTube 데이터 없음")
        return

    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)

    print("\n" + "=" * 60)
    print("[YouTube 데이터 품질 검사]")
    print("=" * 60)
    print(f"총 샘플 수: {len(data)}개")

    # 채널별 분포
    sources = Counter(d["source"] for d in data)
    print(f"\n[채널별 분포] (상위 15개):")
    for source, count in sources.most_common(15):
        print(f"  - {source}: {count}개")

    # 텍스트 길이 분석
    text_lengths = [len(d["text"]) for d in data]
    print(f"\n[텍스트 길이]:")
    print(f"  - 평균: {sum(text_lengths)/len(text_lengths):.0f}자")
    print(f"  - 최소: {min(text_lengths)}자")
    print(f"  - 최대: {max(text_lengths)}자")
    print(f"  - 총 글자 수: {sum(text_lengths):,}자")

    # 품질 체크
    short_texts = sum(1 for d in data if len(d["text"]) < 500)
    long_texts = sum(1 for d in data if len(d["text"]) > 3500)
    print(f"\n[품질 이슈]:")
    print(f"  - 500자 미만: {short_texts}개 ({short_texts/len(data)*100:.1f}%)")
    print(f"  - 3500자 초과: {long_texts}개 ({long_texts/len(data)*100:.1f}%)")

    # 한글 비율 체크
    def korean_ratio(text):
        korean = sum(1 for c in text if '가' <= c <= '힣')
        return korean / len(text) if text else 0

    low_korean = sum(1 for d in data if korean_ratio(d["text"]) < 0.3)
    print(f"  - 한글 비율 30% 미만: {low_korean}개 ({low_korean/len(data)*100:.1f}%)")

    # 샘플 출력
    print(f"\n[샘플 데이터] (첫 번째):")
    sample = data[0]
    print(f"  - source: {sample['source']}")
    print(f"  - title: {sample['title'][:60]}...")
    print(f"  - text 앞 300자:")
    print(f"    {sample['text'][:300]}...")

    return data


def summary():
    """전체 요약"""
    github_path = "./github_data/github_training_data.json"
    youtube_path = "./youtube_data/youtube_training_data.json"

    github_count = 0
    youtube_count = 0
    github_size = 0
    youtube_size = 0

    if os.path.exists(github_path):
        with open(github_path, "r", encoding="utf-8") as f:
            github_data = json.load(f)
        github_count = len(github_data)
        github_size = os.path.getsize(github_path) / 1024 / 1024

    if os.path.exists(youtube_path):
        with open(youtube_path, "r", encoding="utf-8") as f:
            youtube_data = json.load(f)
        youtube_count = len(youtube_data)
        youtube_size = os.path.getsize(youtube_path) / 1024 / 1024

    print("\n" + "=" * 60)
    print("[전체 요약]")
    print("=" * 60)
    print(f"| 소스     | 샘플 수 | 파일 크기 |")
    print(f"|----------|---------|-----------|")
    print(f"| GitHub   | {github_count:>7} | {github_size:>6.1f} MB |")
    print(f"| YouTube  | {youtube_count:>7} | {youtube_size:>6.1f} MB |")
    print(f"| 합계     | {github_count + youtube_count:>7} | {github_size + youtube_size:>6.1f} MB |")
    print("=" * 60)

    # 학습 적합성 평가
    print("\n[학습 적합성 평가]")
    total = github_count + youtube_count
    if total >= 1000:
        print("  [OK] 샘플 수 충분 (1000개 이상)")
    else:
        print(f"  [WARNING] 샘플 수 부족 ({total}개 < 1000개 권장)")

    if github_size + youtube_size >= 5:
        print("  [OK] 데이터 크기 충분 (5MB 이상)")
    else:
        print(f"  [WARNING] 데이터 크기 부족 ({github_size + youtube_size:.1f}MB < 5MB 권장)")


if __name__ == "__main__":
    analyze_github_data()
    analyze_youtube_data()
    summary()
