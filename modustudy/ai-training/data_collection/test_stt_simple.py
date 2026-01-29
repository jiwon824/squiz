#!/usr/bin/env python
"""
간단한 STT API 테스트 스크립트
"""
import requests
import time
import sys
from pathlib import Path

AI_SERVER = "http://18.207.138.18:8000"
SCRIPT_DIR = Path(__file__).parent

def test_health():
    """서버 상태 확인"""
    print("=" * 50)
    print("1. AI 서버 상태 확인")
    print("=" * 50)

    try:
        response = requests.get(f"{AI_SERVER}/health", timeout=10)
        data = response.json()

        print(f"Status: {data.get('status')}")
        print(f"LLM Loaded: {data.get('llm_loaded')}")
        print(f"Whisper Loaded: {data.get('whisper_loaded')}")

        return data.get('whisper_loaded', False)
    except Exception as e:
        print(f"Error: {e}")
        return False


def test_stt(audio_file, timeout_sec=600):
    """STT 테스트"""
    print("\n" + "=" * 50)
    print(f"2. STT 테스트: {audio_file}")
    print("=" * 50)

    filepath = Path(audio_file)
    if not filepath.exists():
        print(f"파일 없음: {filepath}")
        return None

    file_size = filepath.stat().st_size / (1024 * 1024)
    print(f"파일 크기: {file_size:.2f} MB")
    print(f"타임아웃: {timeout_sec}초")

    print("\nSTT 처리 중...")
    start_time = time.time()

    try:
        with open(filepath, 'rb') as f:
            # 파일 확장자에 따라 MIME 타입 설정
            ext = filepath.suffix.lower()
            mime_types = {
                '.mp3': 'audio/mpeg',
                '.wav': 'audio/wav',
                '.webm': 'audio/webm',
                '.m4a': 'audio/mp4',
                '.ogg': 'audio/ogg'
            }
            mime_type = mime_types.get(ext, 'audio/mpeg')

            files = {'file': (filepath.name, f, mime_type)}
            response = requests.post(
                f"{AI_SERVER}/api/stt",
                files=files,
                timeout=timeout_sec
            )

        elapsed = time.time() - start_time

        if response.status_code == 200:
            data = response.json()

            print(f"\n완료! ({elapsed:.1f}초)")
            print(f"언어: {data.get('language')}")
            print(f"텍스트 길이: {len(data.get('text', ''))} 글자")

            text = data.get('text', '')
            print(f"\n--- 전체 텍스트 ---")
            print(text[:500] if len(text) > 500 else text)
            if len(text) > 500:
                print(f"... (총 {len(text)} 글자)")

            # 결과 저장
            result_file = filepath.with_suffix('.stt_result.txt')
            with open(result_file, 'w', encoding='utf-8') as f:
                f.write(text)
            print(f"\n결과 저장: {result_file}")

            return data
        else:
            print(f"\nError {response.status_code}")
            print(response.text[:500])
            return None

    except requests.exceptions.ReadTimeout:
        elapsed = time.time() - start_time
        print(f"\nTimeout after {elapsed:.1f} seconds")
        return None
    except Exception as e:
        print(f"\nError: {e}")
        return None


def main():
    # 서버 상태 확인
    if not test_health():
        print("\nWhisper 모델이 로드되지 않았습니다!")
        return

    # 테스트할 오디오 파일
    audio_files = [
        SCRIPT_DIR / "test_meeting.mp3",
    ]

    # 명령줄 인수로 파일 지정 가능
    if len(sys.argv) > 1:
        audio_files = [Path(arg) for arg in sys.argv[1:]]

    for audio_file in audio_files:
        test_stt(audio_file, timeout_sec=600)


if __name__ == '__main__':
    main()
