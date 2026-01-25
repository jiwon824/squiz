"""
학습 데이터 병합 스크립트
- Type A (일반 요약): study_meeting_data_500.json + study_meeting_data_extra.json
- Type B (파트 요약): chunk_summary_data.json + chunk_summary_data_extra.json
- Type C (통합 요약): chunk_summary_data.json + chunk_summary_data_extra.json

출력: training_data_merged.json (Qwen3-8B 학습용)
"""

import json
import random

def load_json(path):
    with open(path, 'r', encoding='utf-8') as f:
        return json.load(f)

def format_type_a(item):
    """Type A: 일반 요약 - 회의록 → 요약"""
    return {
        "type": "regular_summary",
        "input": f"다음 IT 스터디 회의 내용을 요약해주세요.\n\n회의 내용:\n{item['transcript']}",
        "output": item['summary'],
        "topic": item.get('topic', ''),
        "format": item.get('format', ''),
    }

def format_type_b(item):
    """Type B: 파트 요약 - 청크 회의록 → 파트 요약"""
    part_info = f"{item['current_part']}/{item['total_parts']}"
    return {
        "type": "part_summary",
        "input": f"다음은 IT 스터디 회의의 {part_info} 파트입니다. 이 파트의 핵심 내용을 요약해주세요.\n\n회의 내용 (파트 {part_info}):\n{item['transcript']}",
        "output": item['summary'],
        "topic": item.get('topic', ''),
        "part_info": part_info,
    }

def format_type_c(item):
    """Type C: 통합 요약 - 파트 요약들 → 최종 요약"""
    return {
        "type": "integrated_summary",
        "input": f"다음은 IT 스터디 회의의 파트별 요약입니다. 전체 내용을 하나로 통합 요약해주세요.\n{item['part_summaries']}",
        "output": item['integrated_summary'],
        "topic": item.get('topic', ''),
        "total_parts": item.get('total_parts', 3),
    }

def create_chat_format(item):
    """Qwen3 ChatML 형식으로 변환"""
    system_prompts = {
        "regular_summary": "당신은 IT 스터디 회의록을 요약하는 전문가입니다. 핵심 내용을 정확하게 정리합니다.",
        "part_summary": "당신은 IT 스터디 회의록을 요약하는 전문가입니다. 파트별 핵심 내용을 정리합니다.",
        "integrated_summary": "당신은 IT 스터디 회의록을 요약하는 전문가입니다. 파트별 요약을 하나로 통합합니다.",
    }

    system = system_prompts.get(item['type'], system_prompts['regular_summary'])

    # ChatML 형식
    text = f"""<|im_start|>system
{system}<|im_end|>
<|im_start|>user
{item['input']}<|im_end|>
<|im_start|>assistant
{item['output']}<|im_end|>"""

    return {
        "text": text,
        "type": item['type'],
        "topic": item.get('topic', ''),
    }

def main():
    print("=" * 50)
    print("학습 데이터 병합 시작")
    print("=" * 50)

    # 1. Type A 로드 및 병합
    print("\n[Type A] 일반 요약 데이터 로드...")
    type_a_1 = load_json("study_meeting_data_500.json")
    type_a_2 = load_json("study_meeting_data_extra.json")
    type_a_all = type_a_1 + type_a_2
    print(f"  - study_meeting_data_500.json: {len(type_a_1)}개")
    print(f"  - study_meeting_data_extra.json: {len(type_a_2)}개")
    print(f"  - 합계: {len(type_a_all)}개")

    # 2. Type B, C 로드 및 병합
    print("\n[Type B/C] 청크 요약 데이터 로드...")
    chunk_1 = load_json("chunk_summary_data.json")
    chunk_2 = load_json("chunk_summary_data_extra.json")

    type_b_all = chunk_1.get('type_b_samples', []) + chunk_2.get('type_b_samples', [])
    type_c_all = chunk_1.get('type_c_samples', []) + chunk_2.get('type_c_samples', [])
    print(f"  - Type B (파트 요약): {len(type_b_all)}개")
    print(f"  - Type C (통합 요약): {len(type_c_all)}개")

    # 3. 포맷 변환
    print("\n[변환] 학습 형식으로 변환 중...")
    all_data = []

    for item in type_a_all:
        formatted = format_type_a(item)
        chat_format = create_chat_format(formatted)
        all_data.append(chat_format)

    for item in type_b_all:
        formatted = format_type_b(item)
        chat_format = create_chat_format(formatted)
        all_data.append(chat_format)

    for item in type_c_all:
        formatted = format_type_c(item)
        chat_format = create_chat_format(formatted)
        all_data.append(chat_format)

    print(f"  - 총 변환: {len(all_data)}개")

    # 4. 셔플
    print("\n[셔플] 데이터 섞는 중...")
    random.seed(42)
    random.shuffle(all_data)

    # 5. Train/Validation 분리 (90:10)
    split_idx = int(len(all_data) * 0.9)
    train_data = all_data[:split_idx]
    val_data = all_data[split_idx:]

    print(f"  - Train: {len(train_data)}개")
    print(f"  - Validation: {len(val_data)}개")

    # 6. 저장
    print("\n[저장] 파일 저장 중...")

    # 전체 데이터
    with open("training_data_merged.json", "w", encoding="utf-8") as f:
        json.dump(all_data, f, ensure_ascii=False, indent=2)
    print(f"  - training_data_merged.json 저장 완료")

    # Train/Val 분리 저장
    with open("training_data_train.json", "w", encoding="utf-8") as f:
        json.dump(train_data, f, ensure_ascii=False, indent=2)
    print(f"  - training_data_train.json 저장 완료")

    with open("training_data_val.json", "w", encoding="utf-8") as f:
        json.dump(val_data, f, ensure_ascii=False, indent=2)
    print(f"  - training_data_val.json 저장 완료")

    # 7. 통계
    print("\n" + "=" * 50)
    print("병합 완료!")
    print("=" * 50)
    print(f"총 데이터: {len(all_data)}개")
    print(f"  - Type A (일반 요약): {len(type_a_all)}개 ({len(type_a_all)/len(all_data)*100:.1f}%)")
    print(f"  - Type B (파트 요약): {len(type_b_all)}개 ({len(type_b_all)/len(all_data)*100:.1f}%)")
    print(f"  - Type C (통합 요약): {len(type_c_all)}개 ({len(type_c_all)/len(all_data)*100:.1f}%)")
    print(f"\nTrain: {len(train_data)}개 / Validation: {len(val_data)}개")
    print("\n출력 파일:")
    print("  - training_data_merged.json (전체)")
    print("  - training_data_train.json (학습용)")
    print("  - training_data_val.json (검증용)")

if __name__ == "__main__":
    main()
