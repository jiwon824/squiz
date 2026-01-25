"""
데이터 정제 스크립트
- 인코딩 오류 데이터 제거
- 영어 강의 제거
- 품질 기준 미달 제거
"""
import json
import os
import sys
import re
from datetime import datetime

sys.stdout.reconfigure(encoding='utf-8')

# 문제 문자 패턴 (이모지/일반 특수문자 제외)
BROKEN_CHARS = re.compile(r'[เฮวกណයзикஜր]')  # 태국어, 크메르어, 러시아어 등

def is_valid_github(item):
    """GitHub 데이터 유효성 검사"""
    text = item.get("conversation", "")

    # 너무 짧은 텍스트
    if len(text.strip()) < 50:
        return False, "too_short"

    return True, None


def is_valid_youtube(item):
    """YouTube 데이터 유효성 검사"""
    text = item.get("text", "")

    # 1. 너무 짧은 텍스트
    if len(text.strip()) < 100:
        return False, "too_short"

    # 2. 한글 비율 체크 (10% 미만이면 제거)
    korean_chars = sum(1 for c in text if '가' <= c <= '힣' or 'ㄱ' <= c <= 'ㅣ')
    korean_ratio = korean_chars / len(text) if text else 0
    if korean_ratio < 0.1:
        return False, f"low_korean({korean_ratio*100:.1f}%)"

    # 3. 외국어 문자 혼입 체크
    if BROKEN_CHARS.search(text):
        return False, "foreign_chars"

    return True, None


def clean_github():
    """GitHub 데이터 정제"""
    input_path = "./github_data/github_training_data.json"
    output_path = "./github_data/github_training_data_cleaned.json"
    removed_path = "./github_data/github_removed.json"

    if not os.path.exists(input_path):
        print("[!] GitHub 데이터 없음")
        return

    with open(input_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    print("=" * 60)
    print("[GitHub 데이터 정제]")
    print("=" * 60)
    print(f"원본: {len(data)}개")

    valid_data = []
    removed_data = []

    for item in data:
        is_valid, reason = is_valid_github(item)
        if is_valid:
            valid_data.append(item)
        else:
            item["_removed_reason"] = reason
            removed_data.append(item)

    # 저장
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(valid_data, f, ensure_ascii=False, indent=2)

    if removed_data:
        with open(removed_path, "w", encoding="utf-8") as f:
            json.dump(removed_data, f, ensure_ascii=False, indent=2)

    print(f"정제 후: {len(valid_data)}개")
    print(f"제거됨: {len(removed_data)}개")
    if removed_data:
        reasons = {}
        for item in removed_data:
            r = item.get("_removed_reason", "unknown")
            reasons[r] = reasons.get(r, 0) + 1
        print(f"제거 사유: {reasons}")
    print(f"저장: {output_path}")

    return valid_data, removed_data


def clean_youtube():
    """YouTube 데이터 정제"""
    input_path = "./youtube_data/youtube_training_data.json"
    output_path = "./youtube_data/youtube_training_data_cleaned.json"
    removed_path = "./youtube_data/youtube_removed.json"

    if not os.path.exists(input_path):
        print("[!] YouTube 데이터 없음")
        return

    with open(input_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    print("\n" + "=" * 60)
    print("[YouTube 데이터 정제]")
    print("=" * 60)
    print(f"원본: {len(data)}개")

    valid_data = []
    removed_data = []

    for item in data:
        is_valid, reason = is_valid_youtube(item)
        if is_valid:
            valid_data.append(item)
        else:
            item["_removed_reason"] = reason
            removed_data.append(item)

    # 저장
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(valid_data, f, ensure_ascii=False, indent=2)

    if removed_data:
        with open(removed_path, "w", encoding="utf-8") as f:
            json.dump(removed_data, f, ensure_ascii=False, indent=2)

    print(f"정제 후: {len(valid_data)}개")
    print(f"제거됨: {len(removed_data)}개")
    if removed_data:
        reasons = {}
        for item in removed_data:
            r = item.get("_removed_reason", "unknown")
            reasons[r] = reasons.get(r, 0) + 1
        print(f"제거 사유: {reasons}")
        print("\n제거된 항목:")
        for item in removed_data[:10]:
            print(f"  - [{item.get('source')}] {item.get('title', '')[:40]}... ({item.get('_removed_reason')})")
    print(f"저장: {output_path}")

    return valid_data, removed_data


def merge_cleaned():
    """정제된 데이터 통합"""
    gh_path = "./github_data/github_training_data_cleaned.json"
    yt_path = "./youtube_data/youtube_training_data_cleaned.json"
    output_path = "./training_data_final.json"

    all_data = []

    if os.path.exists(gh_path):
        with open(gh_path, "r", encoding="utf-8") as f:
            gh_data = json.load(f)
        for item in gh_data:
            all_data.append({
                "source": "github",
                "repo": item.get("repo", ""),
                "text": item.get("conversation", ""),
                "metadata": {
                    "summary": item.get("summary", ""),
                    "labels": item.get("labels", [])
                }
            })

    if os.path.exists(yt_path):
        with open(yt_path, "r", encoding="utf-8") as f:
            yt_data = json.load(f)
        for item in yt_data:
            all_data.append({
                "source": "youtube",
                "channel": item.get("source", ""),
                "text": item.get("text", ""),
                "metadata": {
                    "title": item.get("title", ""),
                    "video_id": item.get("video_id", "")
                }
            })

    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(all_data, f, ensure_ascii=False, indent=2)

    print("\n" + "=" * 60)
    print("[통합 데이터 생성]")
    print("=" * 60)
    print(f"총 샘플: {len(all_data)}개")
    print(f"저장: {output_path}")

    # 파일 크기
    size_mb = os.path.getsize(output_path) / 1024 / 1024
    print(f"파일 크기: {size_mb:.1f} MB")

    return all_data


def summary():
    """최종 요약"""
    print("\n" + "=" * 60)
    print("[최종 요약]")
    print("=" * 60)

    files = [
        ("원본 GitHub", "./github_data/github_training_data.json"),
        ("정제 GitHub", "./github_data/github_training_data_cleaned.json"),
        ("원본 YouTube", "./youtube_data/youtube_training_data.json"),
        ("정제 YouTube", "./youtube_data/youtube_training_data_cleaned.json"),
        ("통합 데이터", "./training_data_final.json"),
    ]

    print(f"| {'파일':<15} | {'샘플 수':>8} | {'크기':>8} |")
    print(f"|{'-'*17}|{'-'*10}|{'-'*10}|")

    for name, path in files:
        if os.path.exists(path):
            with open(path, "r", encoding="utf-8") as f:
                data = json.load(f)
            size = os.path.getsize(path) / 1024 / 1024
            print(f"| {name:<15} | {len(data):>8} | {size:>6.1f} MB |")
        else:
            print(f"| {name:<15} | {'N/A':>8} | {'N/A':>8} |")


if __name__ == "__main__":
    clean_github()
    clean_youtube()
    merge_cleaned()
    summary()

    print("\n[완료] 학습에 사용할 파일: training_data_final.json")
