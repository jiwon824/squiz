"""
긴 미팅 청크 처리용 학습 데이터 생성
- Type B: 파트별 요약 (청크 → 파트 요약)
- Type C: 통합 요약 (파트 요약들 → 최종 요약)

기존 generate_study_meetings.py와 별도로 사용
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
client = None

# ===== 스터디 카테고리 (기존과 동일) =====
STUDY_SCENARIOS = [
    {"topic": "알고리즘/백준", "format": "문제 풀이", "example": "BFS/DFS 문제 풀이 리뷰"},
    {"topic": "알고리즘/프로그래머스", "format": "문제 풀이", "example": "카카오 기출 문제 분석"},
    {"topic": "CS/자료구조", "format": "독서", "example": "트리, 그래프 챕터 정리"},
    {"topic": "CS/운영체제", "format": "강의 수강", "example": "프로세스 스케줄링 강의 리뷰"},
    {"topic": "CS/네트워크", "format": "발표/세미나", "example": "TCP/IP 발표"},
    {"topic": "React", "format": "프로젝트", "example": "투두앱 컴포넌트 설계"},
    {"topic": "React", "format": "코드 리뷰", "example": "커스텀 훅 PR 리뷰"},
    {"topic": "Java/Spring", "format": "코드 리뷰", "example": "JPA N+1 문제 해결 리뷰"},
    {"topic": "Java/Spring", "format": "프로젝트", "example": "REST API 설계"},
    {"topic": "Docker", "format": "프로젝트", "example": "멀티스테이지 빌드 설정"},
    {"topic": "AWS", "format": "발표/세미나", "example": "EC2, S3 아키텍처 발표"},
    {"topic": "딥러닝", "format": "강의 수강", "example": "CNN 기초 강의 리뷰"},
    {"topic": "기술 면접", "format": "모의 면접", "example": "CS 질문 모의면접"},
]

NAMES = [
    "윤상훈", "성경훈", "김민재", "박지원", "조문희", "신재혁",
    "김민수", "이지은", "박준혁", "최서연", "정다은",
    "강현우", "조유진", "윤태호", "임수빈", "한지민",
]

# ===== 청크 생성용 프롬프트 =====
GENERATE_CHUNK_TRANSCRIPT_PROMPT = """당신은 IT 개발 스터디 그룹의 온라인 화상회의를 STT(음성인식)로 변환한 텍스트를 생성합니다.

이것은 **긴 미팅의 {current_part}/{total_parts} 파트**입니다.

스터디 정보:
- 주제: {topic}
- 형식: {format}
- 오늘 내용: {example}
- 참석자: {participants}
- 파트: {current_part}/{total_parts}

**파트별 내용 가이드:**
{part_guide}

**STT 변환 텍스트 특징:**
1. 필러/추임새: "음", "어", "그", "아", "뭐지", "잠깐만"
2. 구어체: "~거든요", "~잖아요", "~인 거 같아요"
3. 반복/수정: "그 그거", "아니 그게 아니라"
4. 간투사: "네네", "아아", "맞아맞아"

요구사항:
1. 이 파트에 해당하는 내용만 생성
2. 총 {utterances} 발언
3. 파트1이면 인사/시작, 마지막 파트면 마무리/다음 주 예고 포함

출력 형식:
[이름]: [발언 내용]
"""

PART_GUIDES = {
    1: "파트 1 - 도입부: 인사, 오늘 할 내용 소개, 첫 번째 주제 시작",
    2: "파트 2 - 본론: 주요 내용 심화 토론, 코드 리뷰, 질의응답",
    3: "파트 3 - 마무리: 남은 내용 정리, 액션 아이템 확정, 다음 주 예고, 인사",
}

GENERATE_PART_SUMMARY_PROMPT = """다음은 IT 개발 스터디 미팅의 **{current_part}/{total_parts} 파트** 회의록입니다.
이 파트의 핵심 내용만 요약해주세요.

스터디 정보:
- 주제: {topic}
- 형식: {format}
- 파트: {current_part}/{total_parts}

회의록 (파트 {current_part}):
{transcript}

다음 형식으로 **이 파트의 내용만** 요약하세요:

## 파트 {current_part} 요약
[이 파트에서 다룬 핵심 내용을 2-3문장으로 요약]

## 다룬 내용
- [이 파트에서 논의한 내용 1]
- [이 파트에서 논의한 내용 2]

## 이 파트의 액션 아이템 (있는 경우만)
- [이름]: [할 일]

**주의: 요약은 구어체가 아닌 문어체로 작성하세요.**
"""

GENERATE_INTEGRATED_SUMMARY_PROMPT = """다음은 IT 개발 스터디 미팅의 파트별 요약입니다.
전체 내용을 하나의 통합된 요약으로 정리해주세요.

스터디 정보:
- 주제: {topic}
- 형식: {format}
- 총 파트 수: {total_parts}

{part_summaries}

위 파트별 요약을 바탕으로 **전체 미팅을 하나로 통합 요약**하세요:

## 요약
[전체 스터디의 핵심 내용을 3-4문장으로 통합 요약]

## 다룬 내용
- [전체 미팅에서 다룬 주요 내용 1]
- [전체 미팅에서 다룬 주요 내용 2]
- [전체 미팅에서 다룬 주요 내용 3]
- [전체 미팅에서 다룬 주요 내용 4]

## 액션 아이템
- [이름]: [구체적인 할 일 또는 과제]

## 키워드
[관련 기술 키워드, 쉼표로 구분]

**주의: 중복 내용은 제거하고, 시간순으로 자연스럽게 통합하세요.**
"""


def generate_chunk_transcript(scenario: dict, participants: list,
                               current_part: int, total_parts: int,
                               model: str = "gpt-4o-mini") -> str:
    """특정 파트의 회의 대화 생성"""
    response = client.chat.completions.create(
        model=model,
        messages=[
            {"role": "system", "content": "당신은 IT 개발 스터디 그룹의 회의를 시뮬레이션하는 AI입니다."},
            {"role": "user", "content": GENERATE_CHUNK_TRANSCRIPT_PROMPT.format(
                topic=scenario["topic"],
                format=scenario["format"],
                example=scenario["example"],
                participants=", ".join(participants),
                current_part=current_part,
                total_parts=total_parts,
                part_guide=PART_GUIDES.get(current_part, "본론 내용"),
                utterances="25-35개",
            )}
        ],
        temperature=0.85,
        max_tokens=2500,
    )
    return response.choices[0].message.content


def generate_part_summary(scenario: dict, transcript: str,
                          current_part: int, total_parts: int,
                          model: str = "gpt-4o-mini") -> str:
    """파트별 요약 생성"""
    response = client.chat.completions.create(
        model=model,
        messages=[
            {"role": "system", "content": "당신은 IT 스터디 회의록을 요약하는 전문가입니다."},
            {"role": "user", "content": GENERATE_PART_SUMMARY_PROMPT.format(
                topic=scenario["topic"],
                format=scenario["format"],
                transcript=transcript,
                current_part=current_part,
                total_parts=total_parts,
            )}
        ],
        temperature=0.3,
        max_tokens=800,
    )
    return response.choices[0].message.content


def generate_integrated_summary(scenario: dict, part_summaries: list,
                                 model: str = "gpt-4o-mini") -> str:
    """파트 요약들을 통합한 최종 요약 생성"""
    # 파트 요약들을 포맷팅
    formatted_summaries = ""
    for i, summary in enumerate(part_summaries, 1):
        formatted_summaries += f"\n### [파트 {i}/{len(part_summaries)} 요약]\n{summary}\n"

    response = client.chat.completions.create(
        model=model,
        messages=[
            {"role": "system", "content": "당신은 IT 스터디 회의록을 요약하는 전문가입니다."},
            {"role": "user", "content": GENERATE_INTEGRATED_SUMMARY_PROMPT.format(
                topic=scenario["topic"],
                format=scenario["format"],
                total_parts=len(part_summaries),
                part_summaries=formatted_summaries,
            )}
        ],
        temperature=0.3,
        max_tokens=1200,
    )
    return response.choices[0].message.content


def generate_long_meeting_data(scenario: dict, model: str = "gpt-4o-mini") -> dict:
    """
    긴 미팅 데이터 생성 (3파트)

    Returns:
        {
            "type_b_samples": [...],  # 파트별 요약 학습 데이터
            "type_c_sample": {...},   # 통합 요약 학습 데이터
        }
    """
    total_parts = 3
    num_participants = random.randint(3, 5)
    participants = random.sample(NAMES, num_participants)

    transcripts = []
    part_summaries = []
    type_b_samples = []

    # 각 파트 생성
    for part in range(1, total_parts + 1):
        print(f"    파트 {part}/{total_parts} 생성 중...", flush=True)

        # 파트 대화 생성
        transcript = generate_chunk_transcript(
            scenario, participants, part, total_parts, model
        )
        transcripts.append(transcript)
        time.sleep(1)

        # 파트 요약 생성
        part_summary = generate_part_summary(
            scenario, transcript, part, total_parts, model
        )
        part_summaries.append(part_summary)
        time.sleep(1)

        # Type B 샘플 (파트 → 파트 요약)
        type_b_samples.append({
            "type": "part_summary",
            "topic": scenario["topic"],
            "format": scenario["format"],
            "current_part": part,
            "total_parts": total_parts,
            "transcript": transcript,
            "summary": part_summary,
        })

    # Type C: 통합 요약 생성
    print(f"    통합 요약 생성 중...", flush=True)
    integrated_summary = generate_integrated_summary(scenario, part_summaries, model)

    # Type C 샘플 (파트 요약들 → 통합 요약)
    formatted_part_summaries = ""
    for i, summary in enumerate(part_summaries, 1):
        formatted_part_summaries += f"\n[파트 {i}/{total_parts} 요약]\n{summary}\n"

    type_c_sample = {
        "type": "integrated_summary",
        "topic": scenario["topic"],
        "format": scenario["format"],
        "total_parts": total_parts,
        "part_summaries": formatted_part_summaries,
        "integrated_summary": integrated_summary,
    }

    return {
        "type_b_samples": type_b_samples,
        "type_c_sample": type_c_sample,
    }


def generate_dataset(num_long_meetings: int = 50,
                     output_path: str = "chunk_summary_data.json",
                     model: str = "gpt-4o-mini"):
    """
    청크 처리용 데이터셋 생성

    Args:
        num_long_meetings: 생성할 긴 미팅 수
        output_path: 출력 파일 경로
        model: 사용할 GPT 모델

    생성 결과:
        - Type B 샘플: num_long_meetings × 3 (파트당 1개)
        - Type C 샘플: num_long_meetings (미팅당 1개)
    """
    type_b_samples = []
    type_c_samples = []

    print(f"긴 미팅 청크 데이터 {num_long_meetings}개 생성 시작...", flush=True)
    print(f"예상 생성량: Type B {num_long_meetings * 3}개, Type C {num_long_meetings}개", flush=True)
    print("=" * 50, flush=True)

    for i in range(num_long_meetings):
        scenario = STUDY_SCENARIOS[i % len(STUDY_SCENARIOS)]

        try:
            print(f"\n[{i+1}/{num_long_meetings}] {scenario['topic']} - {scenario['format']}", flush=True)

            result = generate_long_meeting_data(scenario, model)

            type_b_samples.extend(result["type_b_samples"])
            type_c_samples.append(result["type_c_sample"])

            print(f"  [OK] 완료 (Type B: {len(result['type_b_samples'])}개, Type C: 1개)", flush=True)

            # 중간 저장 (5개마다)
            if (i + 1) % 5 == 0:
                save_data = {
                    "type_b_samples": type_b_samples,
                    "type_c_samples": type_c_samples,
                }
                with open(output_path, "w", encoding="utf-8") as f:
                    json.dump(save_data, f, ensure_ascii=False, indent=2)
                print(f"\n  [SAVE] 중간 저장: Type B {len(type_b_samples)}개, Type C {len(type_c_samples)}개", flush=True)

            time.sleep(2)  # Rate limit 방지

        except Exception as e:
            print(f"  [ERROR] 에러: {e}", flush=True)
            time.sleep(5)
            continue

    # 최종 저장
    save_data = {
        "type_b_samples": type_b_samples,
        "type_c_samples": type_c_samples,
    }
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(save_data, f, ensure_ascii=False, indent=2)

    print(f"\n{'=' * 50}", flush=True)
    print(f"[DONE] 완료!", flush=True)
    print(f"  - Type B (파트 요약): {len(type_b_samples)}개", flush=True)
    print(f"  - Type C (통합 요약): {len(type_c_samples)}개", flush=True)
    print(f"[FILE] 저장 위치: {output_path}", flush=True)

    return save_data


def main():
    global client

    import argparse
    parser = argparse.ArgumentParser(description="긴 미팅 청크 처리용 데이터 생성")
    parser.add_argument("--num", type=int, default=50, help="생성할 긴 미팅 수 (기본: 50)")
    parser.add_argument("--output", type=str, default="chunk_summary_data.json", help="출력 파일")
    parser.add_argument("--api-key", type=str, help="OpenAI API 키")
    parser.add_argument("--model", type=str, default="gpt-4o-mini", help="사용할 모델")

    args = parser.parse_args()

    api_key = args.api_key or os.environ.get("OPENAI_API_KEY")
    if not api_key:
        print("[ERROR] OpenAI API 키가 필요합니다.", flush=True)
        exit(1)

    client = OpenAI(api_key=api_key)

    generate_dataset(
        num_long_meetings=args.num,
        output_path=args.output,
        model=args.model
    )


if __name__ == "__main__":
    main()
