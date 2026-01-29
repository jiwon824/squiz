# Whisper 파인튜닝 모델 문제 분석

## 문제 요약

IT 도메인 파인튜닝된 Whisper 모델(`whisper-it-ct2`)이 기본 모델보다 성능이 현저히 떨어지는 현상 발생.

## 사용된 학습 데이터

### 데이터 수집 스크립트
`ai-training/data_collection/01_youtube_subtitle_collector.py`

### 데이터 소스: YouTube IT/개발 강의 자막
35개 이상 채널에서 자막 수집:

| 카테고리 | 채널 |
|---------|------|
| 프로그래밍 기초 | nomadcoders, codingapple, 생활코딩, 드림코딩, 조코딩 |
| 심화/전문 | POCU, 홍정모, 백준, 리씽크 |
| 기업 기술 | 우아한테크, NHN Cloud, 네이버클라우드, 카카오테크, LINE |
| 알고리즘 | 동빈나, RiosCode, TechHwan |
| AI/ML | 성민석, AI Korea |
| 기타 | 얄코, 인프런, 구름, 프로그래머스, SSAFY |

### 설정
- 채널당 최대 30개 영상
- 한국어 자막만 수집
- 최소 30초 이상 자막

### 핵심 문제: 오디오-텍스트 페어 부재

**Whisper 파인튜닝에 필요한 것:**
```
{
  "audio": "path/to/audio.wav",  // 실제 음성 파일
  "text": "안녕하세요 오늘은..."    // 해당 음성의 전사
}
```

**실제 수집된 데이터:**
```
{
  "text": "안녕하세요 오늘은...",   // 자막 텍스트만
  "type": "regular_summary",
  "topic": "Java/Spring"
}
```

YouTube 자막은 **텍스트만** 있고, 해당 구간의 **오디오 파일이 없음**.
따라서 Whisper가 "이 소리 → 이 텍스트" 매핑을 학습할 수 없었음.

| 항목 | 파인튜닝 모델 | 기본 medium | 기본 large-v3 |
|------|--------------|-------------|---------------|
| 처리 시간 | 102-143초 | **24.49초** | 35.90초 |
| 유사도 | 14% | **85%** | 83% |
| 출력 언어 | 영어 (번역) | 한국어 | 한국어 |

## 증상

1. **영어로 번역 출력**: 한국어 음성 입력 → 영어 텍스트 출력
2. **처리 속도 저하**: 기본 모델 대비 4~6배 느림
3. **정확도 급락**: 85% → 14%

## 원인 분석

### 1. CT2 변환 시 설정 손실

학습 노트북(`03_whisper_finetune_train_only_final.ipynb`)에서는 한국어 transcribe 설정을 했음:

```python
# 학습 시 설정
model.config.forced_decoder_ids = processor.get_decoder_prompt_ids(language="ko", task="transcribe")
```

그러나 CTranslate2 변환 시 이 설정이 손실됨:

```python
# CT2 변환 코드
converter = ctranslate2.converters.TransformersConverter(merged_path)
converter.convert(ct2_path, quantization="float16")
```

**문제점:**
- `TransformersConverter`는 모델 가중치만 변환
- `forced_decoder_ids` 설정은 CT2 포맷에 포함되지 않음
- faster-whisper가 CT2 모델 로드 시 기본값(번역 모드?)으로 동작

### 2. LoRA 학습으로 인한 언어 이해 능력 손상

LoRA 파인튜닝이 모델의 기존 한국어 처리 능력을 방해했을 가능성:

```python
# LoRA 설정
model = get_peft_model(model, LoraConfig(
    r=16,
    lora_alpha=32,
    target_modules=["q_proj", "v_proj"],
    task_type="SEQ_2_SEQ_LM"
))
```

- `q_proj`, `v_proj`만 타겟으로 학습
- 언어 선택/태스크 결정 관련 레이어에 영향을 줬을 수 있음

### 3. 학습 데이터 품질/양 문제

- IT 도메인 특화 데이터가 충분하지 않았을 수 있음
- 일반 한국어 음성 인식 능력이 오히려 저하됨

## 시도한 해결책

### inference_server.py에서 명시적 설정

```python
segments, info = whisper_model.transcribe(
    tmp_path,
    language="ko",
    task="transcribe",  # 명시적 추가
    beam_size=5,
    vad_filter=True,
)
```

**결과**: 효과 없음. CT2 모델 내부에 번역 태스크가 고정되어 있는 것으로 추정.

## 현재 해결책

**기본 large-v3 모델 사용** (파인튜닝 모델 폐기)

```bash
# .env 설정
WHISPER_MODEL=large-v3
WHISPER_DEVICE=cuda
```

## 향후 개선 방안

### 1. 올바른 학습 데이터 구축 (가장 중요)

현재 데이터의 문제: **오디오 없이 텍스트만 있음**

Whisper 파인튜닝을 위한 올바른 데이터 구축 방법:

```python
# 방법 1: YouTube 영상에서 오디오+자막 동시 추출
yt-dlp -x --audio-format wav --write-subs --sub-lang ko VIDEO_URL

# 방법 2: TTS로 합성 데이터 생성
from gtts import gTTS
tts = gTTS(text="자바스크립트 변수 선언", lang='ko')
tts.save("audio.wav")

# 방법 3: 실제 스터디 미팅 녹음 + 수동 전사
{
  "audio": "meeting_001.wav",
  "text": "수동으로 전사한 텍스트"
}
```

**필요한 데이터 규모:**
- 최소: 10시간 (약 1,000개 샘플)
- 권장: 50시간 이상

### 2. CT2 변환 시 설정 보존

`preprocessor_config.json`에 language/task 설정 추가:

```json
{
  "feature_size": 128,
  "language": "ko",
  "task": "transcribe"
}
```

### 3. 학습 데이터에 prefix 토큰 포함

`WhisperDataCollator`에서 라벨에 언어/태스크 토큰을 명시적으로 포함:

```python
# 한국어 transcribe prefix 토큰
forced_decoder_ids = self.processor.get_decoder_prompt_ids(language="ko", task="transcribe")
prefix_tokens = [token_id for _, token_id in forced_decoder_ids]

# prefix + 텍스트 + EOS 조합
full_tokens = prefix_tokens + text_tokens + [self.processor.tokenizer.eos_token_id]
```

### 4. 파인튜닝 대신 후처리 방식 권장

Whisper large-v3는 이미 한국어 성능이 우수함 (85%+ 정확도).
파인튜닝보다 **STT 후처리**가 더 효율적:

```python
# IT 용어 교정 사전
CORRECTIONS = {
    "자바스크립트": "JavaScript",
    "리액트": "React",
    "스프링 부트": "Spring Boot",
    "도커": "Docker",
    "쿠버네티스": "Kubernetes",
}

def post_process_stt(text):
    for wrong, correct in CORRECTIONS.items():
        text = text.replace(wrong, correct)
    return text
```

장점:
- 즉시 적용 가능
- 모델 재학습 불필요
- 용어 추가/수정 용이

## 참고 파일

- 학습 노트북: `ai-training/03_whisper_finetune_train_only_final (1).ipynb`
- 기존 수정 기록: `ai-training/03_whisper_finetune_fix.md`
- 추론 서버: `ai-inference/inference_server.py`

## 결론

### 근본 원인
**오디오-텍스트 페어 데이터 없이 텍스트만으로 학습 시도** → Whisper가 음성 특징을 학습할 수 없었음

### 현재 최선의 선택
**기본 large-v3 모델 사용** (파인튜닝 모델 폐기)
- 처리 시간: 35.9초 / 2.7MB 오디오
- 정확도: 83%+
- IT 용어 인식: 양호 (GET, POST, API, Docker 등)

### 파인튜닝 재시도 조건
1. **오디오-텍스트 페어 데이터 확보** (최소 10시간, 권장 50시간)
   - YouTube 오디오 + 자막 동시 추출
   - 또는 TTS 합성 데이터 생성
   - 또는 실제 스터디 녹음 + 수동 전사
2. CT2 변환 시 설정 보존 방법 적용
3. LoRA 타겟 모듈 재검토 (언어 선택 관련 레이어 제외)

### 권장 대안
파인튜닝 대신 **STT 후처리 파이프라인** 구축:
- 기본 large-v3로 STT 수행
- IT 용어 교정 사전으로 후처리
- 비용/시간 대비 효과가 더 좋음
