# Whisper 파인튜닝 문제 해결 기록

## 문제 현상

파인튜닝된 Whisper 모델이 한국어 음성을 **영어로 번역**해버림

- **입력 음성**: "혹시 자바스크립트 관련해서 좋은 자료 있으면 공유해주세요"
- **잘못된 출력**: "If you have good data regarding JavaScript, please share."
- **기대 출력**: "혹시 자바스크립트 관련해서 좋은 자료 있으면 공유해주세요"

## 원인 분석

### 1. 학습 데이터 라벨에 언어/태스크 토큰 누락

기존 `WhisperDataCollator`에서 텍스트를 토큰화할 때 Whisper 전용 prefix 토큰을 포함하지 않았음:

```python
# 기존 코드 (문제)
tokenized = self.processor.tokenizer(f["sentence"]).input_ids
```

Whisper 모델은 디코딩 시 다음 형식의 라벨을 기대함:
```
<|startoftranscript|><|ko|><|transcribe|><|notimestamps|> 텍스트 <|endoftext|>
```

- `<|ko|>`: 한국어 출력 지시
- `<|transcribe|>`: 번역(translate)이 아닌 전사(transcribe) 지시

prefix 토큰 없이 학습하면 모델이 기본값(영어 번역)으로 동작함.

### 2. CT2 변환 시 설정 파일 누락

`ct2-whisper-converter` 명령어가 환경에 없어서 변환 실패 후, `TransformersConverter`로 대체했으나 `preprocessor_config.json` 파일이 복사되지 않음.

large-v3 모델은 **128 mel bins**를 사용하는데, 설정 파일 없이는 기본값 80으로 처리되어 shape mismatch 에러 발생:
```
ValueError: expected shape (1, 128, 3000), but got shape (1, 80, 3000)
```

## 해결 방법

### 1. WhisperDataCollator 수정 (cell-3)

```python
@dataclass
class WhisperDataCollator:
    processor: Any

    def __call__(self, features: List[Dict[str, Any]]) -> Dict[str, torch.Tensor]:
        input_features = []
        label_features = []

        # 한국어 transcribe prefix 토큰 가져오기
        forced_decoder_ids = self.processor.get_decoder_prompt_ids(language="ko", task="transcribe")
        prefix_tokens = [token_id for _, token_id in forced_decoder_ids]

        for f in features:
            audio, sr = sf.read(f["audio_path"])
            if len(audio.shape) > 1: audio = audio.mean(axis=1)
            if sr != 16000: audio = librosa.resample(audio, orig_sr=sr, target_sr=16000)
            feat = self.processor.feature_extractor(audio, sampling_rate=16000).input_features[0]
            input_features.append({"input_features": feat})

            # 텍스트 토큰화 (prefix 없이)
            text_tokens = self.processor.tokenizer(f["sentence"], add_special_tokens=False).input_ids

            # prefix + 텍스트 + EOS 조합
            full_tokens = prefix_tokens + text_tokens + [self.processor.tokenizer.eos_token_id]
            label_features.append({"input_ids": full_tokens})

        batch = self.processor.feature_extractor.pad(input_features, return_tensors="pt")
        labels_batch = self.processor.tokenizer.pad(label_features, return_tensors="pt")
        labels = labels_batch["input_ids"].masked_fill(labels_batch.attention_mask.ne(1), -100)
        batch["labels"] = labels
        return batch
```

### 2. CT2 변환 코드 수정 (cell-5)

```python
# Python API로 CT2 변환
import ctranslate2
import shutil

converter = ctranslate2.converters.TransformersConverter(merged_path)
converter.convert(ct2_path, quantization="float16")

# preprocessor_config.json 복사 (large-v3용 128 mel bins)
src_config = os.path.join(merged_path, "preprocessor_config.json")
dst_config = os.path.join(ct2_path, "preprocessor_config.json")
if os.path.exists(src_config):
    shutil.copy(src_config, dst_config)
```

## 배포 절차

1. 수정된 노트북으로 재학습 실행
2. CT2 모델 압축: `tar -czvf whisper-it-ct2.tar.gz whisper-it-ct2/`
3. AWS 서버로 전송 및 압축 해제
4. Docker 컨테이너 재시작

## 참고

- Whisper large-v3: 128 mel bins (이전 버전은 80)
- faster-whisper 1.2.1 사용
- ctranslate2 4.6.3 사용
