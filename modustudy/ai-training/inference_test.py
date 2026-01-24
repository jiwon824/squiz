"""
Qwen3 Meeting Summarizer - Inference Test
학습된 모델로 요약 테스트

Usage:
    python inference_test.py --model ./outputs/qwen3-summarizer --input "회의 내용..."
    python inference_test.py --model ./outputs/qwen3-summarizer --file test_transcript.txt
"""

import os

# ===== SSAFY GPU 서버 설정 (D106팀 - Device 3) =====
os.environ["CUDA_DEVICE_ORDER"] = "PCI_BUS_ID"
os.environ["CUDA_VISIBLE_DEVICES"] = "3"

import argparse
import torch
from transformers import AutoModelForCausalLM, AutoTokenizer, BitsAndBytesConfig
from peft import PeftModel


def load_model(model_path: str, base_model: str = "Qwen/Qwen3-8B"):
    """학습된 LoRA 모델 로드"""
    print(f"[MODEL] 로딩 중: {model_path}")

    # 4-bit 양자화
    bnb_config = BitsAndBytesConfig(
        load_in_4bit=True,
        bnb_4bit_use_double_quant=True,
        bnb_4bit_quant_type="nf4",
        bnb_4bit_compute_dtype=torch.bfloat16,
    )

    # 토크나이저
    tokenizer = AutoTokenizer.from_pretrained(model_path, trust_remote_code=True)

    # 베이스 모델 로드
    model = AutoModelForCausalLM.from_pretrained(
        base_model,
        quantization_config=bnb_config,
        device_map="auto",
        trust_remote_code=True,
        torch_dtype=torch.bfloat16,
    )

    # LoRA 어댑터 로드
    model = PeftModel.from_pretrained(model, model_path)
    model.eval()

    print("[MODEL] 로드 완료!")
    return model, tokenizer


def summarize(model, tokenizer, transcript: str, max_new_tokens: int = 1024) -> str:
    """회의록 요약 생성"""
    messages = [
        {"role": "system", "content": "당신은 IT 스터디 회의록을 요약하는 전문가입니다."},
        {"role": "user", "content": f"""다음은 IT 개발 스터디의 회의록입니다. 핵심 내용을 요약해주세요.

회의 내용:
{transcript}

요약:"""}
    ]

    # 토크나이징
    text = tokenizer.apply_chat_template(
        messages,
        tokenize=False,
        add_generation_prompt=True
    )

    inputs = tokenizer(text, return_tensors="pt").to(model.device)

    # 생성
    with torch.no_grad():
        outputs = model.generate(
            **inputs,
            max_new_tokens=max_new_tokens,
            do_sample=True,
            temperature=0.3,
            top_p=0.9,
            repetition_penalty=1.1,
            pad_token_id=tokenizer.pad_token_id,
            eos_token_id=tokenizer.eos_token_id,
        )

    # 디코딩 (입력 부분 제외)
    response = tokenizer.decode(
        outputs[0][inputs["input_ids"].shape[1]:],
        skip_special_tokens=True
    )

    return response.strip()


def main():
    parser = argparse.ArgumentParser(description="Meeting Summarizer Inference")
    parser.add_argument("--model", type=str, required=True, help="학습된 모델 경로")
    parser.add_argument("--base-model", type=str, default="Qwen/Qwen3-8B", help="베이스 모델")
    parser.add_argument("--input", type=str, help="직접 입력할 회의 내용")
    parser.add_argument("--file", type=str, help="회의 내용이 담긴 텍스트 파일")
    parser.add_argument("--max-tokens", type=int, default=1024, help="최대 생성 토큰 수")
    args = parser.parse_args()

    # 모델 로드
    model, tokenizer = load_model(args.model, args.base_model)

    # 입력 텍스트 가져오기
    if args.input:
        transcript = args.input
    elif args.file:
        with open(args.file, "r", encoding="utf-8") as f:
            transcript = f.read()
    else:
        print("회의 내용을 입력하세요 (Ctrl+D 또는 빈 줄 두 번으로 종료):")
        lines = []
        empty_count = 0
        try:
            while True:
                line = input()
                if line == "":
                    empty_count += 1
                    if empty_count >= 2:
                        break
                else:
                    empty_count = 0
                lines.append(line)
        except EOFError:
            pass
        transcript = "\n".join(lines)

    if not transcript.strip():
        print("[ERROR] 입력이 비어있습니다.")
        return

    # 요약 생성
    print("\n" + "=" * 50)
    print("[INPUT] 회의 내용:")
    print("=" * 50)
    print(transcript[:500] + "..." if len(transcript) > 500 else transcript)

    print("\n" + "=" * 50)
    print("[OUTPUT] 요약 결과:")
    print("=" * 50)

    summary = summarize(model, tokenizer, transcript, args.max_tokens)
    print(summary)


if __name__ == "__main__":
    main()
