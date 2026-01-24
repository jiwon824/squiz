"""
GPT-4를 사용해 IT 개발 스터디 회의록 + 요약 데이터 생성
ModuStudy/Squiz 플랫폼용
"""
import sys
import os

# Windows 콘솔 UTF-8 설정
if sys.platform == 'win32':
    sys.stdout.reconfigure(encoding='utf-8')
import json
import time
import random
from openai import OpenAI

# OpenAI API 키
OPENAI_API_KEY = os.environ.get("OPENAI_API_KEY", "")
client = None

# ===== 스터디 카테고리 (주제 + 형식) =====
STUDY_SCENARIOS = [
    # 알고리즘/코딩테스트
    {"topic": "알고리즘/백준", "format": "문제 풀이", "example": "BFS/DFS 문제 풀이 리뷰"},
    {"topic": "알고리즘/프로그래머스", "format": "문제 풀이", "example": "카카오 기출 문제 분석"},
    {"topic": "알고리즘/LeetCode", "format": "문제 풀이", "example": "Two Pointer 문제 풀이"},
    {"topic": "코딩테스트 대비", "format": "모의 면접", "example": "실전 코테 모의고사"},

    # CS 기초
    {"topic": "CS/자료구조", "format": "독서", "example": "트리, 그래프 챕터 정리"},
    {"topic": "CS/운영체제", "format": "강의 수강", "example": "프로세스 스케줄링 강의 리뷰"},
    {"topic": "CS/네트워크", "format": "발표/세미나", "example": "TCP/IP 발표"},
    {"topic": "CS/데이터베이스", "format": "토론", "example": "인덱스 최적화 토론"},
    {"topic": "CS/디자인패턴", "format": "독서", "example": "싱글톤, 팩토리 패턴 정리"},

    # 프론트엔드
    {"topic": "React", "format": "프로젝트", "example": "투두앱 컴포넌트 설계"},
    {"topic": "React", "format": "코드 리뷰", "example": "커스텀 훅 PR 리뷰"},
    {"topic": "TypeScript", "format": "독서", "example": "제네릭 타입 챕터"},
    {"topic": "Next.js", "format": "프로젝트", "example": "SSR vs SSG 적용"},
    {"topic": "Vue", "format": "강의 수강", "example": "Composition API 강의"},

    # 백엔드
    {"topic": "Java/Spring", "format": "코드 리뷰", "example": "JPA N+1 문제 해결 리뷰"},
    {"topic": "Java/Spring", "format": "프로젝트", "example": "REST API 설계"},
    {"topic": "Python/FastAPI", "format": "프로젝트", "example": "비동기 API 구현"},
    {"topic": "Node.js", "format": "코드 리뷰", "example": "Express 미들웨어 리뷰"},

    # 인프라/DevOps
    {"topic": "Docker", "format": "프로젝트", "example": "멀티스테이지 빌드 설정"},
    {"topic": "Kubernetes", "format": "강의 수강", "example": "Pod, Service 개념"},
    {"topic": "CI/CD", "format": "프로젝트", "example": "GitHub Actions 파이프라인"},
    {"topic": "AWS", "format": "발표/세미나", "example": "EC2, S3 아키텍처 발표"},

    # AI/ML
    {"topic": "딥러닝", "format": "강의 수강", "example": "CNN 기초 강의 리뷰"},
    {"topic": "NLP", "format": "논문 리뷰", "example": "Transformer 논문 리딩"},
    {"topic": "머신러닝", "format": "프로젝트", "example": "분류 모델 학습"},

    # 자격증
    {"topic": "정보처리기사", "format": "문제 풀이", "example": "실기 기출 문제 풀이"},
    {"topic": "SQLD", "format": "문제 풀이", "example": "SQL 문제 풀이"},
    {"topic": "AWS 자격증", "format": "독서", "example": "SAA 덤프 리뷰"},

    # 취업 준비
    {"topic": "기술 면접", "format": "모의 면접", "example": "CS 질문 모의면접"},
    {"topic": "포트폴리오", "format": "코드 리뷰", "example": "프로젝트 README 리뷰"},
    {"topic": "이력서/자소서", "format": "토론", "example": "자소서 피드백"},
]

# 스터디 길이 유형 (MAX_SEQ_LENGTH=4096 기준 - Qwen3-8B 학습용)
# 4096 토큰 ≈ 20분 회의 (transcript + summary 포함)
MEETING_LENGTHS = [
    {"type": "짧은 진도 확인", "utterances": "15-25개", "duration": "5-10분", "description": "간단히 진도 확인하고 다음 할 일 정하는 짧은 미팅", "weight": 0.25},
    {"type": "일반 스터디", "utterances": "40-55개", "duration": "12-18분", "description": "주제에 대해 토론하고 질의응답하는 보통 스터디", "weight": 0.40},
    {"type": "발표/세미나형", "utterances": "50-70개", "duration": "15-22분", "description": "한 명이 주도적으로 발표하고 다른 사람들이 질문하는 스터디. 발표자는 길게(8-15문장), 청중은 짧게(1-2문장) 발언", "weight": 0.20},
    {"type": "심화 토론형", "utterances": "45-60개", "duration": "14-20분", "description": "깊이 있는 토론과 코드 리뷰가 포함된 스터디. 코드 설명 시 길게(5-10문장) 발언", "weight": 0.15},
]

# 참석자 이름 풀
NAMES = [
    "윤상훈", "성경훈", "김민재", "박지원", "조문희", "신재혁",
    "김민수", "이지은", "박준혁", "최서연", "정다은",
    "강현우", "조유진", "윤태호", "임수빈", "한지민",
    "송민재", "오세진", "황예린", "류재현", "배소희",
]

GENERATE_TRANSCRIPT_PROMPT = """당신은 IT 개발 스터디 그룹의 온라인 화상회의를 STT(음성인식)로 변환한 텍스트를 생성합니다.

스터디 정보:
- 주제: {topic}
- 형식: {format}
- 오늘 내용: {example}
- 참석자: {participants} (총 {num_participants}명)
- 스터디 유형: {meeting_type} ({meeting_duration})
- 스터디 설명: {meeting_description}

**중요: STT 변환 텍스트의 특징을 반드시 반영하세요:**
1. 필러/추임새: "음", "어", "그", "아", "뭐지", "잠깐만", "그니까"
2. 구어체: "~거든요", "~잖아요", "~인 거 같아요", "~했었는데"
3. 반복/수정: "그 그거", "아니 그게 아니라", "다시 말하면"
4. 불완전한 문장: "그래서 그...", "근데 이게..."
5. 간투사: "네네", "아아", "오오", "맞아맞아"
6. 기술 용어 변형 (가끔): "리액트" 대신 "리엑트", "컴포넌트" 대신 "컴퍼넌트"
7. 자연스러운 대화 흐름: 끼어들기, 맞장구, 질문

**대화 패턴 (아래 중 랜덤하게 섞어서 사용):**

패턴1 - 발표형: 한 명이 5-10문장으로 길게 설명, 다른 사람들은 짧은 질문/맞장구
패턴2 - 토론형: 서로 의견 주고받기, 각자 2-4문장씩 번갈아 발언
패턴3 - 질의응답형: 한 명이 질문하면 다른 한 명이 상세히 답변 (5-8문장)
패턴4 - 코드리뷰형: 코드 작성자가 길게 설명 → 리뷰어들이 피드백/개선점 제안
패턴5 - 문제풀이형: 문제 설명 → 각자 접근법 공유 → 최적해 토론
패턴6 - 경험공유형: 한 명이 경험담/삽질기를 길게 풀어놓음 → 공감/추가 팁

**하나의 스터디 안에서 위 패턴들이 자연스럽게 전환되어야 함**

요구사항:
1. 발표/설명 시 한 번에 길게 (5-10문장), 맞장구/질문은 짧게 (1-3문장)
2. 구체적인 기술 내용 포함 (코드 예시, 개념 설명, 도구 사용법, 에러 해결 등 상세히)
3. 스터디 유형에 맞는 대화 길이와 분위기로 생성
4. 총 대화 길이: {meeting_utterances} 발언

출력 형식:
[이름]: [발언 내용]

예시:
김민수: 어 네 그럼 오늘 스터디 시작할게요. 음 이번 주제가 BFS였죠?
이지은: 네네 맞아요. 저 백준 1260번 풀어왔는데요 음 DFS랑 BFS 둘 다 구현했거든요.
박준혁: 아 저도요. 근데 그 BFS에서 visited 체크 위치가 좀 헷갈리더라고요 이게...
김민수: 아아 그거 맞아요 그게 큐에 넣을 때 체크하느냐 뺄 때 체크하느냐 그건데...
"""

GENERATE_SUMMARY_PROMPT = """다음은 IT 개발 스터디의 STT(음성인식) 회의록입니다.
필러, 구어체, 불완전한 문장이 있지만 핵심 내용을 파악하여 깔끔하게 요약해주세요.

스터디 정보:
- 주제: {topic}
- 형식: {format}

회의록 (STT 변환):
{transcript}

다음 형식으로 **깔끔하고 정돈된** 요약을 작성하세요:

## 요약
[스터디의 핵심 내용을 2-3문장으로 명확하게 요약]

## 다룬 내용
- [학습/논의한 주요 내용 1]
- [학습/논의한 주요 내용 2]
- [학습/논의한 주요 내용 3]

## 액션 아이템
- [이름]: [구체적인 할 일 또는 과제]

## 키워드
[관련 기술 키워드, 쉼표로 구분]

**주의: 요약은 구어체가 아닌 문어체로 깔끔하게 작성하세요.**
"""


def generate_transcript(scenario: dict, model: str = "gpt-4o-mini") -> str:
    """스터디 회의 대화 생성"""
    num_participants = random.randint(3, 5)
    participants = random.sample(NAMES, num_participants)

    # 가중치 기반 스터디 길이 유형 선택
    weights = [m["weight"] for m in MEETING_LENGTHS]
    meeting_length = random.choices(MEETING_LENGTHS, weights=weights, k=1)[0]

    response = client.chat.completions.create(
        model=model,
        messages=[
            {"role": "system", "content": "당신은 IT 개발 스터디 그룹의 회의를 시뮬레이션하는 AI입니다. 현실적이고 자연스러운 대화를 생성합니다."},
            {"role": "user", "content": GENERATE_TRANSCRIPT_PROMPT.format(
                topic=scenario["topic"],
                format=scenario["format"],
                example=scenario["example"],
                participants=", ".join(participants),
                num_participants=num_participants,
                meeting_type=meeting_length["type"],
                meeting_duration=meeting_length["duration"],
                meeting_description=meeting_length["description"],
                meeting_utterances=meeting_length["utterances"],
            )}
        ],
        temperature=0.85,
        max_tokens=4000,  # 50-70 발언 수용 (4096 seq length 내)
    )
    return response.choices[0].message.content


def generate_summary(scenario: dict, transcript: str, model: str = "gpt-4o-mini") -> str:
    """스터디 요약 생성"""
    response = client.chat.completions.create(
        model=model,
        messages=[
            {"role": "system", "content": "당신은 IT 스터디 회의록을 요약하는 전문가입니다. 기술 내용을 정확하게 정리합니다."},
            {"role": "user", "content": GENERATE_SUMMARY_PROMPT.format(
                topic=scenario["topic"],
                format=scenario["format"],
                transcript=transcript,
            )}
        ],
        temperature=0.3,
        max_tokens=2000,  # 긴 회의록 요약 수용
    )
    return response.choices[0].message.content


def generate_dataset(num_samples: int = 100, output_path: str = "study_meeting_data.json", model: str = "gpt-4o-mini"):
    """스터디 데이터셋 생성"""
    dataset = []

    print(f"IT 스터디 회의 데이터 {num_samples}개 생성 시작...", flush=True)
    print(f"모델: {model}", flush=True)
    print(f"시나리오 수: {len(STUDY_SCENARIOS)}", flush=True)
    print("=" * 50, flush=True)

    for i in range(num_samples):
        scenario = STUDY_SCENARIOS[i % len(STUDY_SCENARIOS)]

        try:
            print(f"\n[{i+1}/{num_samples}] {scenario['topic']} - {scenario['format']}", flush=True)

            # 1. 회의 대화 생성
            print("  -> 대화 생성 중...", flush=True)
            transcript = generate_transcript(scenario, model=model)

            # 2. 요약 생성
            print("  -> 요약 생성 중...", flush=True)
            summary = generate_summary(scenario, transcript, model=model)

            # 3. 학습용 포맷으로 저장
            dataset.append({
                "topic": scenario["topic"],
                "format": scenario["format"],
                "transcript": transcript,
                "summary": summary,
                # 학습용 텍스트 (input-output 쌍)
                "text": f"회의 내용:\n{transcript}\n\n요약:\n{summary}"
            })

            print(f"  [OK] 완료", flush=True)

            # Rate limit 방지
            time.sleep(1.5)

            # 중간 저장 (10개마다)
            if (i + 1) % 10 == 0:
                with open(output_path, "w", encoding="utf-8") as f:
                    json.dump(dataset, f, ensure_ascii=False, indent=2)
                print(f"\n  [SAVE] 중간 저장: {len(dataset)}개", flush=True)

        except Exception as e:
            print(f"  [ERROR] 에러: {e}", flush=True)
            time.sleep(5)  # 에러 시 잠시 대기
            continue

    # 최종 저장
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(dataset, f, ensure_ascii=False, indent=2)

    print(f"\n{'=' * 50}", flush=True)
    print(f"[DONE] 완료! 총 {len(dataset)}개 데이터 생성", flush=True)
    print(f"[FILE] 저장 위치: {output_path}", flush=True)

    return dataset


def main():
    global client

    import argparse
    parser = argparse.ArgumentParser(description="IT 스터디 회의 데이터 생성")
    parser.add_argument("--num", type=int, default=100, help="생성할 샘플 수 (기본: 100)")
    parser.add_argument("--output", type=str, default="study_meeting_data.json", help="출력 파일")
    parser.add_argument("--api-key", type=str, help="OpenAI API 키")
    parser.add_argument("--model", type=str, default="gpt-4o-mini",
                        choices=["gpt-4", "gpt-4-turbo", "gpt-4o", "gpt-4o-mini", "gpt-5-nano", "gpt-5-mini", "gpt-5"],
                        help="사용할 모델 (기본: gpt-4o-mini)")

    args = parser.parse_args()

    # API 키 설정
    api_key = args.api_key or os.environ.get("OPENAI_API_KEY")
    if not api_key:
        print("[ERROR] 오류: OpenAI API 키가 필요합니다.", flush=True)
        print("   --api-key 인자 또는 OPENAI_API_KEY 환경변수 설정", flush=True)
        exit(1)

    client = OpenAI(api_key=api_key)

    # 데이터 생성
    generate_dataset(num_samples=args.num, output_path=args.output, model=args.model)


if __name__ == "__main__":
    main()
