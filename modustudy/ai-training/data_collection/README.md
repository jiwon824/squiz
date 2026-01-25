# 학습 데이터 생성 가이드

## 개요

IT 스터디 회의록 요약을 위한 **합성 데이터**를 GPT-4o-mini로 생성합니다.

> **왜 합성 데이터?** → [why_synthetic_data.md](../../docs/ai/why_synthetic_data.md) 참고

## 데이터 구조

| 타입 | 설명 | 샘플 수 |
|------|------|--------|
| Type A | 일반 회의록 → 요약 | 500개 |
| Type B | 긴 회의 파트 → 파트 요약 | 150개 (50회의 × 3파트) |
| Type C | 파트 요약들 → 통합 요약 | 50개 |
| **합계** | | **700개** |

## 스크립트

| 스크립트 | 용도 | 실행 시간 |
|---------|------|----------|
| `generate_study_meetings.py` | Type A 데이터 생성 | ~2시간 (500개) |
| `generate_chunk_summaries.py` | Type B/C 데이터 생성 | ~1시간 (50개) |
| `merge_training_data.py` | 데이터 병합 + Train/Val 분리 | 즉시 |

## 사용법

### 1. Type A 데이터 생성 (일반 요약)
```bash
cd data_collection

# 500개 생성
python generate_study_meetings.py \
    --num 500 \
    --output study_meeting_data_500.json \
    --api-key "your-openai-api-key"
```

### 2. Type B/C 데이터 생성 (긴 회의 청크)
```bash
# 50개 긴 회의 → 150개 파트 요약 + 50개 통합 요약
python generate_chunk_summaries.py \
    --num 50 \
    --output chunk_summary_data.json \
    --api-key "your-openai-api-key"
```

### 3. 데이터 병합
```bash
python merge_training_data.py

# 출력 파일:
# - training_data_merged.json (전체 700개)
# - training_data_train.json (630개, 90%)
# - training_data_val.json (70개, 10%)
```

## 출력 형식 (ChatML)

```json
{
  "text": "<|im_start|>system\n당신은 IT 스터디 회의록을 요약하는 전문가입니다.<|im_end|>\n<|im_start|>user\n다음 IT 스터디 회의 내용을 요약해주세요.\n\n회의 내용:\n김민수: ...<|im_end|>\n<|im_start|>assistant\n## 오늘의 주제\n...<|im_end|>",
  "type": "regular_summary",
  "topic": "BFS/DFS 알고리즘"
}
```

## 데이터 파일

| 파일 | 설명 |
|------|------|
| `study_meeting_data_500.json` | Type A 원본 (500개) |
| `study_meeting_data_extra.json` | Type A 추가 (110개) |
| `chunk_summary_data.json` | Type B/C 원본 |
| `chunk_summary_data_extra.json` | Type B/C 추가 |
| `training_data_train.json` | **학습용 (630개)** |
| `training_data_val.json` | **검증용 (70개)** |

## 예상 비용

| 항목 | 토큰 | 비용 |
|------|------|------|
| Type A (500개) | ~2M 토큰 | ~$3 |
| Type B/C (50세트) | ~1M 토큰 | ~$1.5 |
| **합계** | ~3M 토큰 | **~$5** |

## 다음 단계

1. 생성된 데이터를 SSAFY GPU 서버에 업로드
2. `train_qwen3_summarizer.ipynb` 실행
3. Before/After 비교로 학습 효과 확인
