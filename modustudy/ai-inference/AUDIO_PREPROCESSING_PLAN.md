# 음성 전처리 구현 계획

## 목표
Whisper STT 정확도 향상을 위한 음성 전처리 파이프라인 추가

## 전처리 항목 (최적화)

| 순서 | 필터 | 목적 | 파라미터 |
|-----|-----|-----|---------|
| 1 | highpass | 저주파 잡음 제거 (에어컨, 팬) | f=75Hz (남성 저음 보존) |
| 2 | lowpass | 고주파 잡음 제거 (하울링) | f=8000Hz |
| 3 | afftdn | FFT 기반 노이즈 감쇠 | nf=-25dB |
| 4 | loudnorm | 볼륨 정규화 | 기본값 |

## ffmpeg 명령어 (최적화)

```bash
ffmpeg -i input.webm \
  -af "highpass=f=75,lowpass=f=8000,afftdn=nf=-25,loudnorm" \
  -c:a pcm_s16le \
  -ar 16000 -ac 1 \
  -threads 0 \
  -y output.wav
```

- `-c:a pcm_s16le`: 무압축 PCM (Whisper 선호)
- `-ar 16000`: 16kHz 샘플레이트 (Whisper 최적)
- `-ac 1`: 모노 채널
- `-threads 0`: CPU 자동 최적화

## 구현 완료

**파일**: `inference_server.py`

**함수**:
- `preprocess_audio(input_path: str) -> str`: 전처리 수행, 실패 시 원본 반환
- `cleanup_preprocessed(original_path, preprocessed_path)`: 전처리 파일 삭제

**적용 위치**:
1. `speech_to_text()` - STT 동기 API
2. `process_stt_job()` - STT 비동기 작업
3. `process_meeting_job()` - 미팅 처리
4. `process_meeting_full_job()` - 전체 미팅 처리 (Claude 포함)

## 처리 흐름

```
원본 음성 (webm/wav)
    ↓
preprocess_audio() - UUID로 고유 파일명 생성
    ↓
전처리된 음성 (wav, 16kHz, mono, PCM)
    ↓
Whisper STT
    ↓
cleanup_preprocessed() - 임시 파일 삭제
```

## 예상 성능 (A10G)

| 음성 길이 | 전처리 시간 | Whisper 시간 |
|---------|-----------|-------------|
| 1분 | ~2초 | ~5초 |
| 10분 | ~10초 | ~50초 |
| 1시간 | ~1분 | ~5분 |

## 에러 처리

- **ffmpeg 실패**: 원본 파일로 Whisper 진행 (fallback)
- **타임아웃 (5분)**: 원본 파일 사용
- **파일명 충돌**: UUID 8자리로 방지

## 개선 사항 (2025-01-30)

1. `highpass` 75Hz로 조정 (남성 저음역대 보존)
2. `-c:a pcm_s16le` 명시 (무압축 PCM)
3. `-threads 0` 추가 (CPU 효율화)
4. UUID로 출력 파일명 충돌 방지
