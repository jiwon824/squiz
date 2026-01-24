#!/usr/bin/env python3
"""
GGUF 양자화 변환 스크립트
- 병합된 모델 → GGUF 포맷 변환
- Q4_K_M 양자화 적용 (품질/크기 균형)

필요 패키지:
  pip install llama-cpp-python

또는 llama.cpp 직접 사용:
  git clone https://github.com/ggerganov/llama.cpp
  cd llama.cpp && make
"""

import os
import subprocess
import argparse
from pathlib import Path


def check_llama_cpp():
    """llama.cpp 설치 확인"""
    llama_cpp_path = Path.home() / "llama.cpp"

    if not llama_cpp_path.exists():
        print("llama.cpp가 설치되어 있지 않습니다.")
        print("\n설치 방법:")
        print("  git clone https://github.com/ggerganov/llama.cpp")
        print("  cd llama.cpp")
        print("  pip install -r requirements.txt")
        print("  make")
        return None

    return llama_cpp_path


def convert_to_gguf(
    model_path: str = "./models/qwen3-8b-summarizer-merged",
    output_path: str = "./models/qwen3-8b-summarizer.gguf",
    quantization: str = "q4_k_m",
):
    """
    HuggingFace 모델을 GGUF로 변환 및 양자화

    Args:
        model_path: 병합된 HF 모델 경로
        output_path: GGUF 출력 경로
        quantization: 양자화 타입 (q4_k_m, q5_k_m, q8_0 등)
    """
    print("=" * 60)
    print("GGUF 양자화 변환")
    print("=" * 60)
    print(f"입력 모델: {model_path}")
    print(f"출력 경로: {output_path}")
    print(f"양자화: {quantization.upper()}")
    print("=" * 60)

    llama_cpp_path = check_llama_cpp()
    if not llama_cpp_path:
        print("\n수동 변환 방법:")
        print("-" * 40)
        print("# 1. llama.cpp 클론")
        print("git clone https://github.com/ggerganov/llama.cpp")
        print("cd llama.cpp")
        print("pip install -r requirements.txt")
        print("")
        print("# 2. FP16 GGUF로 변환")
        print(f"python convert_hf_to_gguf.py {model_path} --outfile {output_path.replace('.gguf', '-f16.gguf')} --outtype f16")
        print("")
        print("# 3. 양자화")
        print(f"./llama-quantize {output_path.replace('.gguf', '-f16.gguf')} {output_path} {quantization}")
        print("-" * 40)
        return

    # 경로 설정
    convert_script = llama_cpp_path / "convert_hf_to_gguf.py"
    quantize_bin = llama_cpp_path / "llama-quantize"

    fp16_output = output_path.replace(".gguf", "-f16.gguf")

    # 출력 디렉토리 생성
    os.makedirs(os.path.dirname(output_path), exist_ok=True)

    # Step 1: HF → GGUF FP16
    print("\n[1/2] HuggingFace → GGUF (FP16) 변환 중...")
    cmd_convert = [
        "python", str(convert_script),
        model_path,
        "--outfile", fp16_output,
        "--outtype", "f16"
    ]

    result = subprocess.run(cmd_convert, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"변환 실패: {result.stderr}")
        return
    print("FP16 변환 완료!")

    # Step 2: 양자화
    print(f"\n[2/2] 양자화 중 ({quantization.upper()})...")
    cmd_quantize = [
        str(quantize_bin),
        fp16_output,
        output_path,
        quantization
    ]

    result = subprocess.run(cmd_quantize, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"양자화 실패: {result.stderr}")
        return
    print("양자화 완료!")

    # FP16 중간 파일 삭제 (선택)
    if os.path.exists(fp16_output):
        os.remove(fp16_output)
        print(f"중간 파일 삭제: {fp16_output}")

    # 파일 크기 확인
    if os.path.exists(output_path):
        size_gb = os.path.getsize(output_path) / (1024**3)
        print(f"\n✅ GGUF 변환 완료!")
        print(f"파일: {output_path}")
        print(f"크기: {size_gb:.2f} GB")

    print("\n" + "=" * 60)
    print("다음 단계: EC2 추론 서버 배포")
    print("  scp models/qwen3-8b-summarizer.gguf ec2-user@your-server:/path/")
    print("=" * 60)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="GGUF 양자화 변환")
    parser.add_argument("--model", type=str, default="./models/qwen3-8b-summarizer-merged", help="병합된 모델 경로")
    parser.add_argument("--output", type=str, default="./models/qwen3-8b-summarizer.gguf", help="GGUF 출력 경로")
    parser.add_argument("--quant", type=str, default="q4_k_m",
                       choices=["q4_0", "q4_k_m", "q5_0", "q5_k_m", "q8_0", "f16"],
                       help="양자화 타입")

    args = parser.parse_args()

    convert_to_gguf(
        model_path=args.model,
        output_path=args.output,
        quantization=args.quant,
    )
