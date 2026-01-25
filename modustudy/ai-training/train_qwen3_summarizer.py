"""
Qwen3-8B LoRA Fine-tuning for Meeting Summarization
ModuStudy/Squiz - SSAFY L40S 46GB GPU

Usage:
    python train_qwen3_summarizer.py --data data_collection/study_meeting_data_500.json
"""

import os

# ===== SSAFY GPU 서버 설정 (D106팀 - Device 3) =====
os.environ["CUDA_DEVICE_ORDER"] = "PCI_BUS_ID"
os.environ["CUDA_VISIBLE_DEVICES"] = "3"

import json
import argparse
from typing import Dict, List

import torch
from datasets import Dataset
from transformers import (
    AutoModelForCausalLM,
    AutoTokenizer,
    TrainingArguments,
    Trainer,
    DataCollatorForLanguageModeling,
    BitsAndBytesConfig,
)
from peft import (
    LoraConfig,
    get_peft_model,
    prepare_model_for_kbit_training,
    TaskType,
)

# ===== 설정 =====
MODEL_NAME = "Qwen/Qwen3-8B"  # Qwen3-8B
MAX_SEQ_LENGTH = 4096

# LoRA 설정 (QLoRA 4-bit 최적화)
LORA_R = 64
LORA_ALPHA = 128
LORA_DROPOUT = 0.05

# 학습 하이퍼파라미터 (메모리 여유분으로 배치 사이즈 증가)
BATCH_SIZE = 4         # 1 → 4 (4-bit로 절약한 메모리 활용, 학습 안정성 ↑)
GRADIENT_ACCUMULATION_STEPS = 2  # effective batch size = 8 유지
LEARNING_RATE = 2e-4
NUM_EPOCHS = 4
WARMUP_RATIO = 0.1

# 양자화 설정
USE_4BIT = True        # 4-bit QLoRA (품질 유지 + 메모리 절약 + regularization 효과)


def load_data(data_path: str) -> List[Dict]:
    """학습 데이터 로드"""
    with open(data_path, "r", encoding="utf-8") as f:
        data = json.load(f)
    print(f"[DATA] 로드 완료: {len(data)}개 샘플")
    return data


def format_prompt(transcript: str) -> str:
    """입력 프롬프트 포맷"""
    return f"""다음은 IT 개발 스터디의 회의록입니다. 핵심 내용을 요약해주세요.

회의 내용:
{transcript}

요약:"""


def format_training_sample(sample: Dict, tokenizer) -> Dict:
    """학습용 샘플 포맷팅"""
    prompt = format_prompt(sample["transcript"])
    response = sample["summary"]

    # Qwen chat format
    messages = [
        {"role": "system", "content": "당신은 IT 스터디 회의록을 요약하는 전문가입니다."},
        {"role": "user", "content": prompt},
        {"role": "assistant", "content": response}
    ]

    # apply_chat_template으로 포맷팅
    text = tokenizer.apply_chat_template(
        messages,
        tokenize=False,
        add_generation_prompt=False
    )

    return {"text": text}


def tokenize_function(examples, tokenizer):
    """토크나이징 + labels 마스킹 (assistant 응답만 학습)"""
    result = tokenizer(
        examples["text"],
        truncation=True,
        max_length=MAX_SEQ_LENGTH,
        padding=False,
    )

    # labels 복사
    labels = result["input_ids"].copy()

    # assistant 응답 이전 부분은 -100으로 마스킹 (loss 계산에서 제외)
    # Qwen3 chat template: <|im_start|>assistant\n 이후가 응답
    text = examples["text"]
    assistant_marker = "<|im_start|>assistant\n"  # 개행까지 포함

    idx = text.find(assistant_marker)
    if idx != -1:
        # assistant 마커까지 포함한 prefix
        prefix_text = text[:idx + len(assistant_marker)]
        prefix_tokens = tokenizer(prefix_text, add_special_tokens=False)["input_ids"]

        # prefix 부분 마스킹
        mask_len = min(len(prefix_tokens), len(labels))
        for i in range(mask_len):
            labels[i] = -100

    result["labels"] = labels
    return result


def create_dataset(data: List[Dict], tokenizer) -> Dataset:
    """HuggingFace Dataset 생성"""
    formatted_data = [format_training_sample(sample, tokenizer) for sample in data]
    dataset = Dataset.from_list(formatted_data)

    # 토크나이징
    tokenized_dataset = dataset.map(
        lambda x: tokenize_function(x, tokenizer),
        remove_columns=dataset.column_names,
        desc="Tokenizing",
    )

    return tokenized_dataset


def setup_model_and_tokenizer():
    """모델 및 토크나이저 설정 (bf16 또는 4-bit 양자화 + LoRA)"""
    print(f"[MODEL] {MODEL_NAME} 로딩 중...")
    print(f"[CONFIG] USE_4BIT={USE_4BIT}, LoRA_R={LORA_R}, BATCH_SIZE={BATCH_SIZE}")

    # 토크나이저
    tokenizer = AutoTokenizer.from_pretrained(
        MODEL_NAME,
        trust_remote_code=True,
        padding_side="right",
    )
    if tokenizer.pad_token is None:
        tokenizer.pad_token = tokenizer.eos_token

    if USE_4BIT:
        # 4-bit 양자화 (메모리 절약 모드)
        print("[MODEL] 4-bit 양자화 모드")
        bnb_config = BitsAndBytesConfig(
            load_in_4bit=True,
            bnb_4bit_use_double_quant=True,
            bnb_4bit_quant_type="nf4",
            bnb_4bit_compute_dtype=torch.bfloat16,
        )
        model = AutoModelForCausalLM.from_pretrained(
            MODEL_NAME,
            quantization_config=bnb_config,
            device_map="auto",
            trust_remote_code=True,
            torch_dtype=torch.bfloat16,
        )
        model = prepare_model_for_kbit_training(model)
    else:
        # bf16 전체 정밀도 (품질 최적화 모드)
        print("[MODEL] bf16 전체 정밀도 모드 (품질 최적화)")
        model = AutoModelForCausalLM.from_pretrained(
            MODEL_NAME,
            device_map="auto",
            trust_remote_code=True,
            torch_dtype=torch.bfloat16,
        )
        # bf16에서도 gradient checkpointing을 위해 필요
        model.enable_input_require_grads()

    # LoRA 설정
    lora_config = LoraConfig(
        r=LORA_R,
        lora_alpha=LORA_ALPHA,
        lora_dropout=LORA_DROPOUT,
        bias="none",
        task_type=TaskType.CAUSAL_LM,
        target_modules=[
            "q_proj", "k_proj", "v_proj", "o_proj",
            "gate_proj", "up_proj", "down_proj",
        ],
    )

    model = get_peft_model(model, lora_config)
    model.print_trainable_parameters()

    return model, tokenizer


def main():
    global USE_4BIT

    parser = argparse.ArgumentParser(description="Qwen3 Meeting Summarizer Training")
    parser.add_argument("--data", type=str, required=True, help="학습 데이터 JSON 경로")
    parser.add_argument("--output", type=str, default="./outputs/qwen3-summarizer", help="출력 디렉토리")
    parser.add_argument("--epochs", type=int, default=NUM_EPOCHS, help="에포크 수")
    parser.add_argument("--batch-size", type=int, default=BATCH_SIZE, help="배치 사이즈")
    parser.add_argument("--lr", type=float, default=LEARNING_RATE, help="학습률")
    parser.add_argument("--use-4bit", action="store_true", help="4-bit 양자화 사용 (메모리 절약)")
    args = parser.parse_args()

    # 양자화 모드 설정 (CLI에서 --use-4bit 지정 시 4bit 사용)
    if args.use_4bit:
        USE_4BIT = True
        print("[CONFIG] 4-bit 양자화 모드 (메모리 절약)")
    else:
        print("[CONFIG] bf16 전체 정밀도 모드 (품질 최적화)")

    # 데이터 로드
    data = load_data(args.data)

    # Train/Validation 분할 (90/10)
    split_idx = int(len(data) * 0.9)
    train_data = data[:split_idx]
    val_data = data[split_idx:]
    print(f"[SPLIT] Train: {len(train_data)}, Validation: {len(val_data)}")

    # 모델 & 토크나이저 설정
    model, tokenizer = setup_model_and_tokenizer()

    # 데이터셋 생성
    print("[DATASET] 데이터셋 생성 중...")
    train_dataset = create_dataset(train_data, tokenizer)
    val_dataset = create_dataset(val_data, tokenizer)

    # 토큰 길이 통계
    train_lengths = [len(x["input_ids"]) for x in train_dataset]
    print(f"[STATS] 토큰 길이 - 평균: {sum(train_lengths)/len(train_lengths):.0f}, "
          f"최대: {max(train_lengths)}, 최소: {min(train_lengths)}")

    # Data Collator (CausalLM용)
    data_collator = DataCollatorForLanguageModeling(
        tokenizer=tokenizer,
        mlm=False,  # Causal LM이므로 MLM 비활성화
    )

    # Optimizer 선택 (양자화 여부에 따라)
    optimizer_choice = "paged_adamw_8bit" if USE_4BIT else "adamw_torch"

    # 학습 설정
    training_args = TrainingArguments(
        output_dir=args.output,
        num_train_epochs=args.epochs,
        per_device_train_batch_size=args.batch_size,
        per_device_eval_batch_size=args.batch_size,
        gradient_accumulation_steps=GRADIENT_ACCUMULATION_STEPS,
        learning_rate=args.lr,
        warmup_ratio=WARMUP_RATIO,
        logging_steps=10,
        eval_strategy="steps",
        eval_steps=50,
        save_strategy="steps",
        save_steps=50,
        save_total_limit=3,
        load_best_model_at_end=True,
        metric_for_best_model="eval_loss",
        greater_is_better=False,
        bf16=True,
        gradient_checkpointing=True,
        optim=optimizer_choice,
        report_to="none",  # wandb 사용시 "wandb"로 변경
        dataloader_pin_memory=False,
    )

    print(f"[TRAIN] Optimizer: {optimizer_choice}")

    # Trainer
    trainer = Trainer(
        model=model,
        args=training_args,
        train_dataset=train_dataset,
        eval_dataset=val_dataset,
        data_collator=data_collator,
    )

    # 학습 시작
    print("\n" + "=" * 50)
    print("[TRAIN] 학습 시작!")
    print("=" * 50 + "\n")

    trainer.train()

    # 모델 저장
    print(f"\n[SAVE] 모델 저장: {args.output}")
    trainer.save_model()
    tokenizer.save_pretrained(args.output)

    print("\n[DONE] 학습 완료!")


if __name__ == "__main__":
    main()
