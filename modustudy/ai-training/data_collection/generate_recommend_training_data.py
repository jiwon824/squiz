"""
템플릿 추천 LoRA 학습 데이터 합성 스크립트
- GPT-4o-mini로 사용자 프로필 → 추천 JSON 학습 데이터 생성
- 다양한 기술스택/스케줄/선호도 조합
- ChatML 포맷으로 출력

사용법:
  python generate_recommend_training_data.py --api-key YOUR_KEY
"""

import os
import json
import time
import random
import argparse
from pathlib import Path


# ===== 설정 =====
TOTAL_TARGET = 2000
BATCH_SIZE = 5
OUTPUT_DIR = os.path.dirname(os.path.abspath(__file__))
OUTPUT_PATH = os.path.join(OUTPUT_DIR, "recommend_training_data.json")
CHECKPOINT_PATH = os.path.join(OUTPUT_DIR, "recommend_checkpoint.json")

SYSTEM_PROMPT = """당신은 IT 스터디 템플릿을 추천하는 전문가입니다.
사용자의 기술 스택, 가용 스케줄, 선호도를 분석하여 최적의 스터디 템플릿을 추천합니다.

반드시 아래 JSON 형식으로만 응답하세요:
{"template_type": "ALGORITHM|CS|PROJECT|INTERVIEW|READING", "topic": "구체적 주제", "format": "진행 형식", "difficulty": "BEGINNER|INTERMEDIATE|ADVANCED", "goal": "목표", "textbook": "교재/자료", "schedule_suggestion": {"days": ["요일"], "time": "시간대"}, "reason": "추천 이유"}"""

# ===== 프로필 생성용 풀 =====

TECH_STACKS = {
    "backend": {
        "junior": ["Java", "Spring Boot", "MySQL"],
        "mid": ["Java", "Spring Boot", "JPA", "MySQL", "Redis", "Docker"],
        "senior": ["Java", "Spring Boot", "JPA", "Kafka", "Redis", "Kubernetes", "AWS"],
    },
    "frontend": {
        "junior": ["JavaScript", "React", "CSS"],
        "mid": ["TypeScript", "React", "Next.js", "TailwindCSS"],
        "senior": ["TypeScript", "React", "Next.js", "Redux", "GraphQL", "Storybook"],
    },
    "fullstack": {
        "junior": ["JavaScript", "React", "Node.js", "Express"],
        "mid": ["TypeScript", "React", "Next.js", "Node.js", "PostgreSQL", "Docker"],
        "senior": ["TypeScript", "React", "Next.js", "NestJS", "PostgreSQL", "Redis", "AWS"],
    },
    "mobile": {
        "junior": ["Kotlin", "Android"],
        "mid": ["Kotlin", "Jetpack Compose", "Flutter", "Dart"],
        "senior": ["Kotlin", "Jetpack Compose", "Swift", "SwiftUI", "Firebase"],
    },
    "devops": {
        "junior": ["Linux", "Docker", "Git"],
        "mid": ["Docker", "Kubernetes", "Jenkins", "AWS", "Terraform"],
        "senior": ["Kubernetes", "AWS", "Terraform", "Ansible", "Prometheus", "Grafana"],
    },
    "data_ai": {
        "junior": ["Python", "Pandas", "NumPy"],
        "mid": ["Python", "Pandas", "Scikit-learn", "PyTorch", "SQL"],
        "senior": ["Python", "PyTorch", "TensorFlow", "Spark", "Airflow", "MLflow"],
    },
    "beginner": {
        "junior": ["Python"],
        "mid": ["Python", "Git"],
        "senior": ["Python", "Git", "SQL"],
    },
}

SCHEDULE_PATTERNS = [
    {"days": {"월": {"start": "19:00", "end": "22:00"}, "수": {"start": "19:00", "end": "22:00"}}},
    {"days": {"화": {"start": "20:00", "end": "22:00"}, "목": {"start": "20:00", "end": "22:00"}}},
    {"days": {"월": {"start": "19:00", "end": "21:00"}, "수": {"start": "19:00", "end": "21:00"}, "금": {"start": "19:00", "end": "21:00"}}},
    {"days": {"토": {"start": "10:00", "end": "13:00"}}},
    {"days": {"토": {"start": "14:00", "end": "18:00"}, "일": {"start": "10:00", "end": "12:00"}}},
    {"days": {"월": {"start": "21:00", "end": "23:00"}}},
    {"days": {"화": {"start": "19:00", "end": "21:00"}, "토": {"start": "10:00", "end": "12:00"}}},
    {"days": {"수": {"start": "18:00", "end": "20:00"}, "금": {"start": "18:00", "end": "20:00"}}},
    {"days": {"월": {"start": "07:00", "end": "09:00"}, "수": {"start": "07:00", "end": "09:00"}}},
    {"days": {"일": {"start": "14:00", "end": "18:00"}}},
    {"days": {"화": {"start": "19:00", "end": "22:00"}, "목": {"start": "19:00", "end": "22:00"}, "토": {"start": "10:00", "end": "13:00"}}},
    {"days": {}},  # 미지정
]

STUDY_TYPES = ["ALGORITHM", "CS", "PROJECT", "INTERVIEW", "READING", None]
DIFFICULTIES = ["BEGINNER", "INTERMEDIATE", "ADVANCED", None]

TEXTBOOKS = {
    "ALGORITHM": [
        "백준 단계별 문제", "프로그래머스 코딩테스트 연습", "이것이 코딩테스트다",
        "알고리즘 문제해결 전략", "LeetCode Top 100", "Do it! 자료구조와 함께 배우는 알고리즘 입문",
    ],
    "CS": [
        "혼자 공부하는 컴퓨터 구조+운영체제", "그림으로 배우는 네트워크", "면접을 위한 CS 전공지식 노트",
        "운영체제 공룡책", "컴퓨터 네트워킹: 하향식 접근", "데이터베이스 개론",
    ],
    "PROJECT": [
        "토이 프로젝트 기획서", "GitHub 오픈소스", "실전 프로젝트",
        "Spring Boot + React 실전", "클론 코딩", "사이드 프로젝트",
    ],
    "INTERVIEW": [
        "면접을 위한 CS 전공지식 노트", "자바 면접 질문 모음", "프론트엔드 면접 가이드",
        "코딩 인터뷰 완전 분석", "시스템 디자인 인터뷰", "이력서/포트폴리오 작성 가이드",
    ],
    "READING": [
        "클린 코드", "리팩터링", "디자인 패턴", "도메인 주도 설계",
        "가상 면접 사례로 배우는 대규모 시스템 설계", "이펙티브 자바",
        "모던 자바 인 액션", "Real MySQL", "HTTP 완벽 가이드",
    ],
}


def get_client(api_key):
    from openai import OpenAI
    return OpenAI(api_key=api_key)


def generate_random_profile():
    """랜덤 사용자 프로필 생성"""
    role = random.choice(list(TECH_STACKS.keys()))
    level = random.choice(["junior", "mid", "senior"])
    tech_stack = TECH_STACKS[role][level]

    schedule = random.choice(SCHEDULE_PATTERNS)
    study_type = random.choice(STUDY_TYPES)
    difficulty = random.choice(DIFFICULTIES)

    return {
        "role": role,
        "level": level,
        "tech_stack": tech_stack,
        "schedule": schedule["days"],
        "study_type": study_type,
        "difficulty": difficulty,
    }


def format_schedule_str(schedule):
    if not schedule:
        return "미지정"
    parts = []
    for day, time_info in schedule.items():
        if isinstance(time_info, dict):
            parts.append(f"{day}: {time_info.get('start', '')}~{time_info.get('end', '')}")
        else:
            parts.append(f"{day}: {time_info}")
    return ", ".join(parts) if parts else "미지정"


def generate_recommendation(client, profile):
    """GPT-4o-mini로 추천 JSON 생성"""
    tech_str = ", ".join(profile["tech_stack"])
    schedule_str = format_schedule_str(profile["schedule"])
    study_type_str = profile["study_type"] or "자유"
    difficulty_str = profile["difficulty"] or "자유"

    user_msg = f"""사용자 정보:
- 기술 스택: {tech_str}
- 가용 시간: {schedule_str}
- 희망 유형: {study_type_str}
- 희망 난이도: {difficulty_str}

이 사용자에게 적합한 스터디 템플릿을 추천해주세요."""

    try:
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": user_msg},
            ],
            temperature=0.8,
            max_tokens=512,
        )
        raw = response.choices[0].message.content.strip()

        # JSON 파싱 검증
        json_text = raw
        if "```json" in json_text:
            json_text = json_text.split("```json")[1].split("```")[0].strip()
        elif "```" in json_text:
            json_text = json_text.split("```")[1].split("```")[0].strip()

        parsed = json.loads(json_text)

        # 필수 필드 검증
        required = ["template_type", "topic", "format", "difficulty", "goal", "reason"]
        for field in required:
            if field not in parsed:
                return None, None

        # template_type 검증
        valid_types = ["ALGORITHM", "CS", "PROJECT", "INTERVIEW", "READING"]
        if parsed["template_type"] not in valid_types:
            return None, None

        # difficulty 검증
        valid_diff = ["BEGINNER", "INTERMEDIATE", "ADVANCED"]
        if parsed["difficulty"] not in valid_diff:
            return None, None

        return user_msg, json.dumps(parsed, ensure_ascii=False)

    except (json.JSONDecodeError, KeyError, IndexError) as e:
        return None, None
    except Exception as e:
        print(f"    [API ERROR] {e}")
        return None, None


def format_chatml(user_msg, assistant_msg):
    """ChatML 포맷으로 변환"""
    text = (
        f"<|im_start|>system\n{SYSTEM_PROMPT}<|im_end|>\n"
        f"<|im_start|>user\n{user_msg}<|im_end|>\n"
        f"<|im_start|>assistant\n{assistant_msg}<|im_end|>"
    )
    return text


def save_checkpoint(data, count):
    with open(CHECKPOINT_PATH, "w", encoding="utf-8") as f:
        json.dump({"data": data, "count": count}, f, ensure_ascii=False, indent=2)


def load_checkpoint():
    if os.path.exists(CHECKPOINT_PATH):
        with open(CHECKPOINT_PATH, "r", encoding="utf-8") as f:
            return json.load(f)
    return None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--api-key", required=True, help="OpenAI API key")
    parser.add_argument("--resume", action="store_true", help="체크포인트에서 재개")
    args = parser.parse_args()

    client = get_client(args.api_key)

    all_data = []
    generated = 0

    if args.resume:
        checkpoint = load_checkpoint()
        if checkpoint:
            all_data = checkpoint["data"]
            generated = checkpoint["count"]
            print(f"체크포인트에서 재개: {generated}개")

    print("=" * 70)
    print(f"템플릿 추천 학습 데이터 생성: 목표 {TOTAL_TARGET}개")
    print("=" * 70)

    failed = 0

    while generated < TOTAL_TARGET:
        profile = generate_random_profile()
        user_msg, rec_json = generate_recommendation(client, profile)

        if user_msg and rec_json:
            chatml = format_chatml(user_msg, rec_json)
            all_data.append({
                "text": chatml,
                "type": "recommend",
                "profile_role": profile["role"],
                "profile_level": profile["level"],
                "study_type": profile.get("study_type", "free"),
            })
            generated += 1
            failed = 0
        else:
            failed += 1
            if failed > 10:
                print(f"  [WARNING] 연속 실패 {failed}회, 잠시 대기...")
                time.sleep(5)
                failed = 0

        if generated % 20 == 0:
            print(f"  진행: {generated}/{TOTAL_TARGET} ({generated/TOTAL_TARGET*100:.1f}%)")
            save_checkpoint(all_data, generated)

        time.sleep(0.3)

    # 저장
    random.shuffle(all_data)
    split_idx = int(len(all_data) * 0.9)
    train_data = all_data[:split_idx]
    val_data = all_data[split_idx:]

    with open(OUTPUT_PATH, "w", encoding="utf-8") as f:
        json.dump(all_data, f, ensure_ascii=False, indent=2)

    train_path = os.path.join(OUTPUT_DIR, "recommend_training_data_train.json")
    val_path = os.path.join(OUTPUT_DIR, "recommend_training_data_val.json")

    with open(train_path, "w", encoding="utf-8") as f:
        json.dump(train_data, f, ensure_ascii=False, indent=2)
    with open(val_path, "w", encoding="utf-8") as f:
        json.dump(val_data, f, ensure_ascii=False, indent=2)

    # 통계
    role_counts = {}
    type_counts = {}
    for item in all_data:
        r = item.get("profile_role", "unknown")
        role_counts[r] = role_counts.get(r, 0) + 1
        t = item.get("study_type", "unknown")
        type_counts[str(t)] = type_counts.get(str(t), 0) + 1

    print(f"\n{'='*70}")
    print(f"추천 학습 데이터 생성 완료!")
    print(f"{'='*70}")
    print(f"  총 데이터:  {len(all_data)}개")
    print(f"  학습:       {len(train_data)}개")
    print(f"  검증:       {len(val_data)}개")
    print(f"\n  역할별:")
    for r, c in sorted(role_counts.items()):
        print(f"    {r}: {c}개")
    print(f"\n  유형별:")
    for t, c in sorted(type_counts.items()):
        print(f"    {t}: {c}개")
    print(f"\n  저장: {OUTPUT_PATH}")
    print(f"{'='*70}")

    if os.path.exists(CHECKPOINT_PATH):
        os.remove(CHECKPOINT_PATH)


if __name__ == "__main__":
    main()
