"""
LoRA 어댑터 병합 스크립트
- Base 모델 + LoRA 어댑터 → 단일 모델로 병합
- SSAFY GPU 서버에서 실행
"""

import os
import torch
import argparse
from transformers import AutoModelForCausalLM, AutoTokenizer
from peft import PeftModel

# GPU 설정 (D106팀 Device 3)
os.environ["CUDA_DEVICE_ORDER"] = "PCI_BUS_ID"
os.environ["CUDA_VISIBLE_DEVICES"] = "3"


def merge_lora(
    base_model_name: str = "Qwen/Qwen3-8B",
    lora_path: str = "./outputs/qwen3-summarizer/final",
    output_path: str = "./models/qwen3-8b-summarizer-merged",
    push_to_hub: bool = False,
    hub_repo: str = None,
):
    """
    LoRA 어댑터를 베이스 모델에 병합

    Args:
        base_model_name: 베이스 모델 이름 (HuggingFace)
        lora_path: LoRA 어댑터 경로
        output_path: 병합된 모델 저장 경로
        push_to_hub: HuggingFace Hub에 업로드 여부
        hub_repo: Hub 저장소 이름
    """
    print("=" * 60)
    print("LoRA 어댑터 병합 시작")
    print("=" * 60)
    print(f"베이스 모델: {base_model_name}")
    print(f"LoRA 경로: {lora_path}")
    print(f"출력 경로: {output_path}")
    print("=" * 60)

    # 1. 토크나이저 로드
    print("\n[1/5] 토크나이저 로드 중...")
    tokenizer = AutoTokenizer.from_pretrained(
        base_model_name,
        trust_remote_code=True,
    )
    print("토크나이저 로드 완료!")

    # 2. 베이스 모델 로드 (전체 정밀도)
    print("\n[2/5] 베이스 모델 로드 중... (몇 분 소요)")
    base_model = AutoModelForCausalLM.from_pretrained(
        base_model_name,
        torch_dtype=torch.float16,
        device_map="auto",
        trust_remote_code=True,
    )
    print(f"베이스 모델 로드 완료! 메모리: {base_model.get_memory_footprint() / 1024**3:.2f} GB")

    # 3. LoRA 어댑터 로드 및 병합
    print("\n[3/5] LoRA 어댑터 로드 중...")
    model = PeftModel.from_pretrained(base_model, lora_path)
    print("LoRA 어댑터 로드 완료!")

    print("\n[4/5] 모델 병합 중...")
    model = model.merge_and_unload()
    print("모델 병합 완료!")

    # 4. 병합된 모델 저장
    print(f"\n[5/5] 병합된 모델 저장 중... ({output_path})")
    os.makedirs(output_path, exist_ok=True)

    model.save_pretrained(output_path, safe_serialization=True)
    tokenizer.save_pretrained(output_path)
    print("모델 저장 완료!")

    # 5. (선택) HuggingFace Hub 업로드
    if push_to_hub and hub_repo:
        print(f"\n[선택] HuggingFace Hub 업로드 중... ({hub_repo})")
        model.push_to_hub(hub_repo)
        tokenizer.push_to_hub(hub_repo)
        print("Hub 업로드 완료!")

    print("\n" + "=" * 60)
    print("✅ LoRA 병합 완료!")
    print("=" * 60)
    print(f"병합된 모델 경로: {output_path}")
    print("\n다음 단계: GGUF 양자화")
    print("  python convert_to_gguf.py")
    print("=" * 60)

    return output_path


def test_merged_model(model_path: str):
    """병합된 모델 테스트"""
    print("\n" + "=" * 60)
    print("병합된 모델 테스트")
    print("=" * 60)

    tokenizer = AutoTokenizer.from_pretrained(model_path, trust_remote_code=True)
    model = AutoModelForCausalLM.from_pretrained(
        model_path,
        torch_dtype=torch.float16,
        device_map="auto",
        trust_remote_code=True,
    )

    test_input = """다음 IT 스터디 회의 내용을 요약해주세요.

회의 내용:
김민수: 오늘은 Docker 기초에 대해 공부해봤어요.
이지은: 네, 컨테이너랑 이미지 개념이 처음엔 헷갈렸는데 이제 좀 이해됐어요.
박준혁: Dockerfile 작성하는 거 해봤는데 생각보다 간단하더라고요.
김민수: 다음 주는 docker-compose로 멀티 컨테이너 구성해보죠."""

    prompt = f"""<|im_start|>system
당신은 IT 스터디 회의록을 요약하는 전문가입니다.<|im_end|>
<|im_start|>user
{test_input}<|im_end|>
<|im_start|>assistant
"""

    inputs = tokenizer(prompt, return_tensors="pt").to(model.device)

    with torch.no_grad():
        outputs = model.generate(
            **inputs,
            max_new_tokens=512,
            temperature=0.7,
            do_sample=True,
            pad_token_id=tokenizer.pad_token_id,
        )

    response = tokenizer.decode(outputs[0], skip_special_tokens=False)
    assistant_start = response.find("<|im_start|>assistant\n") + len("<|im_start|>assistant\n")
    assistant_end = response.find("<|im_end|>", assistant_start)
    summary = response[assistant_start:assistant_end] if assistant_end != -1 else response[assistant_start:]

    print("\n[테스트 출력]")
    print(summary.strip())
    print("\n" + "=" * 60)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="LoRA 어댑터 병합")
    parser.add_argument("--base-model", type=str, default="Qwen/Qwen3-8B", help="베이스 모델")
    parser.add_argument("--lora-path", type=str, default="./outputs/qwen3-summarizer/final", help="LoRA 어댑터 경로")
    parser.add_argument("--output", type=str, default="./models/qwen3-8b-summarizer-merged", help="출력 경로")
    parser.add_argument("--test", action="store_true", help="병합 후 테스트 실행")
    parser.add_argument("--push-to-hub", action="store_true", help="HuggingFace Hub 업로드")
    parser.add_argument("--hub-repo", type=str, help="Hub 저장소 이름")

    args = parser.parse_args()

    output_path = merge_lora(
        base_model_name=args.base_model,
        lora_path=args.lora_path,
        output_path=args.output,
        push_to_hub=args.push_to_hub,
        hub_repo=args.hub_repo,
    )

    if args.test:
        test_merged_model(output_path)
