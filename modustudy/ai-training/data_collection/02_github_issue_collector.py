"""
GitHub 한국어 Issue/PR 수집 스크립트

용도: Qwen3 요약 파인튜닝 데이터
예상 시간: 2~4시간 (API rate limit)
"""

import requests
import json
import time
import os
from datetime import datetime

# ===== 설정 =====
OUTPUT_DIR = "./github_data"
os.makedirs(OUTPUT_DIR, exist_ok=True)

# GitHub Personal Access Token (선택, 없으면 rate limit 60/hour)
# https://github.com/settings/tokens 에서 생성
GITHUB_TOKEN = os.environ.get("GITHUB_TOKEN", "")

# 한국어 개발 프로젝트 목록
KOREAN_REPOS = [
    # 토스
    "toss/slash",
    "toss/tossface",

    # 카카오
    "kakao/kakao-android-sdk",
    "kakao/kakao-ios-sdk",

    # 네이버
    "naver/naver-openapi-guide",
    "naver/egjs",
    "naver/billboard.js",

    # 우아한형제들
    "woowacourse/woowacourse-docs",
    "woowacourse/java-baseball",
    "woowacourse/java-racingcar",

    # 라인
    "line/line-bot-sdk-python",

    # 개인/커뮤니티
    "jojoldu/freelec-springboot2-webservice",
    "gyoogle/tech-interview-for-developer",
    "JaeYeopHan/Interview_Question_for_Beginner",
    "baeharam/Must-Know-About-Frontend",
    "cheese10yun/spring-guide",

    # 한국어 프로젝트
    "Kyubyong/g2pK",
    "haven-jeon/KoNLP",
    "lovit/soynlp",
    "kakao/khaiii",
]

# API 설정
HEADERS = {
    "Accept": "application/vnd.github.v3+json",
}
if GITHUB_TOKEN:
    HEADERS["Authorization"] = f"token {GITHUB_TOKEN}"


def check_rate_limit():
    """API rate limit 확인"""
    resp = requests.get("https://api.github.com/rate_limit", headers=HEADERS)
    data = resp.json()
    remaining = data["rate"]["remaining"]
    reset_time = datetime.fromtimestamp(data["rate"]["reset"])
    print(f"API 남은 횟수: {remaining}, 리셋 시간: {reset_time}")
    return remaining


def get_issues(repo, state="all", per_page=100, max_pages=5):
    """레포지토리의 이슈 가져오기"""
    issues = []

    for page in range(1, max_pages + 1):
        url = f"https://api.github.com/repos/{repo}/issues"
        params = {
            "state": state,
            "per_page": per_page,
            "page": page,
            "sort": "comments",  # 댓글 많은 순
            "direction": "desc"
        }

        try:
            resp = requests.get(url, headers=HEADERS, params=params)

            if resp.status_code == 403:
                print("Rate limit 도달. 1분 대기...")
                time.sleep(60)
                continue

            if resp.status_code != 200:
                print(f"Error {resp.status_code}: {repo}")
                break

            data = resp.json()
            if not data:
                break

            issues.extend(data)
            print(f"  {repo} - 페이지 {page}: {len(data)}개 이슈")

            time.sleep(0.5)  # Rate limit 방지

        except Exception as e:
            print(f"Error: {e}")
            break

    return issues


def get_issue_comments(repo, issue_number):
    """이슈의 댓글 가져오기"""
    url = f"https://api.github.com/repos/{repo}/issues/{issue_number}/comments"

    try:
        resp = requests.get(url, headers=HEADERS)
        if resp.status_code == 200:
            return resp.json()
    except:
        pass

    return []


def is_korean(text):
    """한국어 포함 여부 확인"""
    if not text:
        return False
    korean_count = sum(1 for c in text if '가' <= c <= '힣')
    return korean_count > 5  # 한글 5자 이상


def process_issue(repo, issue):
    """이슈 데이터 처리"""
    title = issue.get("title", "")
    body = issue.get("body", "") or ""

    # 한국어 포함 여부 확인
    if not is_korean(title + body):
        return None

    # PR은 제외 (별도 처리 가능)
    if issue.get("pull_request"):
        return None

    # 댓글 가져오기
    comments = get_issue_comments(repo, issue["number"])
    comment_texts = []

    for comment in comments[:10]:  # 최대 10개 댓글
        comment_body = comment.get("body", "")
        if comment_body and is_korean(comment_body):
            comment_texts.append({
                "author": comment["user"]["login"],
                "body": comment_body[:1000]  # 최대 1000자
            })

    # 라벨 추출
    labels = [label["name"] for label in issue.get("labels", [])]

    return {
        "repo": repo,
        "number": issue["number"],
        "title": title,
        "body": body[:3000],  # 최대 3000자
        "labels": labels,
        "comments": comment_texts,
        "comment_count": len(comment_texts),
        "state": issue["state"],
        "created_at": issue["created_at"],
        "url": issue["html_url"]
    }


def create_conversation_format(issue_data):
    """대화 형식으로 변환 (학습용)"""
    conversation = f"[이슈] {issue_data['title']}\n\n"
    conversation += f"[본문]\n{issue_data['body']}\n\n"

    for i, comment in enumerate(issue_data['comments'], 1):
        conversation += f"[댓글 {i} - {comment['author']}]\n{comment['body']}\n\n"

    # 요약 (제목 + 라벨 기반)
    summary = issue_data['title']
    if issue_data['labels']:
        summary += f" [{', '.join(issue_data['labels'])}]"

    return {
        "conversation": conversation.strip(),
        "summary": summary,
        "labels": issue_data['labels'],
        "repo": issue_data['repo'],
        "url": issue_data['url']
    }


def collect_all_repos():
    """모든 레포지토리에서 이슈 수집"""
    all_issues = []

    print("="*50)
    print("GitHub 이슈 수집 시작")
    print(f"대상 레포: {len(KOREAN_REPOS)}개")
    print("="*50)

    check_rate_limit()

    for repo in KOREAN_REPOS:
        print(f"\n수집 중: {repo}")

        issues = get_issues(repo, max_pages=3)  # 레포당 최대 300개

        for issue in issues:
            processed = process_issue(repo, issue)
            if processed:
                all_issues.append(processed)
                print(f"  ✓ #{issue['number']}: {issue['title'][:40]}...")

        # Rate limit 체크
        remaining = check_rate_limit()
        if remaining < 100:
            print("Rate limit 낮음. 5분 대기...")
            time.sleep(300)

    return all_issues


def save_dataset(issues):
    """데이터셋 저장"""
    # 원본 저장
    raw_path = os.path.join(OUTPUT_DIR, "github_issues_raw.json")
    with open(raw_path, "w", encoding="utf-8") as f:
        json.dump(issues, f, ensure_ascii=False, indent=2)

    # 학습용 형식으로 변환
    training_data = []
    for issue in issues:
        if issue['comment_count'] > 0:  # 댓글 있는 것만
            formatted = create_conversation_format(issue)
            training_data.append(formatted)

    train_path = os.path.join(OUTPUT_DIR, "github_training_data.json")
    with open(train_path, "w", encoding="utf-8") as f:
        json.dump(training_data, f, ensure_ascii=False, indent=2)

    print(f"\n{'='*50}")
    print("데이터셋 저장 완료!")
    print(f"- 원본: {raw_path} ({len(issues)}개)")
    print(f"- 학습용: {train_path} ({len(training_data)}개)")
    print(f"{'='*50}")


def main():
    """메인 실행"""
    print("="*50)
    print("GitHub 한국어 Issue 수집기")
    print("="*50)

    if not GITHUB_TOKEN:
        print("\n⚠️  GITHUB_TOKEN 없음. Rate limit: 60/hour")
        print("토큰 설정: export GITHUB_TOKEN='your_token'")
        print("토큰 생성: https://github.com/settings/tokens\n")
    else:
        print("✓ GITHUB_TOKEN 설정됨. Rate limit: 5000/hour\n")

    # 이슈 수집
    issues = collect_all_repos()

    # 저장
    save_dataset(issues)

    print("\n완료! 다음 단계:")
    print("1. github_data/github_training_data.json 확인")
    print("2. Qwen3 학습 노트북에서 사용")


if __name__ == "__main__":
    main()
