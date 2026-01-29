#!/usr/bin/env python
"""
A10G GPU 서버 STT 테스트 스크립트
"""

import requests
import time
import os
import sys
from pathlib import Path

# GPU 서버 주소
GPU_SERVER = "http://3.88.71.92:8000"

def test_health():
    """서버 상태 확인"""
    print("=" * 60)
    print("1. 서버 상태 확인")
    print("=" * 60)

    try:
        resp = requests.get(f"{GPU_SERVER}/health", timeout=10)
        data = resp.json()
        print(f"Status: {data.get('status')}")
        print(f"LLM Loaded: {data.get('llm_loaded')}")
        print(f"Whisper Loaded: {data.get('whisper_loaded')}")
        print(f"Whisper Model: {data.get('whisper_model')}")
        return data.get('whisper_loaded', False)
    except Exception as e:
        print(f"Error: {e}")
        return False


def test_stt_sync(audio_path: str):
    """동기 STT 테스트"""
    print("\n" + "=" * 60)
    print("2. 동기 STT 테스트 (POST /api/stt)")
    print("=" * 60)

    if not os.path.exists(audio_path):
        print(f"파일 없음: {audio_path}")
        return None

    file_size = os.path.getsize(audio_path) / (1024 * 1024)
    print(f"파일: {audio_path}")
    print(f"크기: {file_size:.2f} MB")

    print("\nSTT 요청 중...")
    start_time = time.time()

    try:
        with open(audio_path, 'rb') as f:
            files = {'file': (os.path.basename(audio_path), f, 'audio/mpeg')}
            resp = requests.post(
                f"{GPU_SERVER}/api/stt",
                files=files,
                timeout=300  # 5분 타임아웃
            )

        elapsed = time.time() - start_time

        if resp.status_code == 200:
            data = resp.json()
            print(f"\n처리 시간: {elapsed:.2f}초")
            print(f"언어: {data.get('language')}")
            print(f"언어 확률: {data.get('language_probability', 0):.2%}")
            print(f"세그먼트 수: {len(data.get('segments', []))}")

            # 결과를 파일로 저장
            result_path = Path(audio_path).with_suffix('.stt_result.txt')
            with open(result_path, 'w', encoding='utf-8') as f:
                f.write(f"처리 시간: {elapsed:.2f}초\n")
                f.write(f"언어: {data.get('language')}\n")
                f.write(f"세그먼트 수: {len(data.get('segments', []))}\n\n")
                f.write("=== 전체 텍스트 ===\n")
                f.write(data.get('text', ''))
                f.write("\n\n=== 세그먼트 ===\n")
                for seg in data.get('segments', []):
                    f.write(f"[{seg.get('start', 0):.1f}s - {seg.get('end', 0):.1f}s] {seg.get('text', '')}\n")

            print(f"\n결과 저장됨: {result_path}")

            # 콘솔에 일부 출력 (ASCII 안전하게)
            text = data.get('text', '')
            print(f"\n--- 전체 텍스트 (처음 500자) ---")
            try:
                print(text[:500])
            except:
                print(text[:500].encode('utf-8', errors='replace').decode('utf-8'))

            return data
        else:
            print(f"Error {resp.status_code}: {resp.text}")
            return None

    except requests.exceptions.Timeout:
        print(f"타임아웃 (5분 초과)")
        return None
    except Exception as e:
        print(f"Error: {e}")
        return None


def test_stt_async(audio_path: str):
    """비동기 STT 테스트"""
    print("\n" + "=" * 60)
    print("3. 비동기 STT 테스트 (POST /api/stt/async)")
    print("=" * 60)

    if not os.path.exists(audio_path):
        print(f"파일 없음: {audio_path}")
        return None

    print(f"파일: {audio_path}")
    print("\n비동기 STT 요청 중...")
    start_time = time.time()

    try:
        with open(audio_path, 'rb') as f:
            files = {'file': (os.path.basename(audio_path), f, 'audio/mpeg')}
            resp = requests.post(
                f"{GPU_SERVER}/api/stt/async",
                files=files,
                timeout=30
            )

        if resp.status_code != 200:
            print(f"Error {resp.status_code}: {resp.text}")
            return None

        data = resp.json()
        job_id = data.get('job_id')
        print(f"Job ID: {job_id}")

        # 폴링으로 결과 대기
        print("결과 대기 중", end="", flush=True)
        while True:
            time.sleep(2)
            print(".", end="", flush=True)

            status_resp = requests.get(f"{GPU_SERVER}/api/jobs/{job_id}", timeout=10)
            if status_resp.status_code != 200:
                continue

            status_data = status_resp.json()
            if status_data.get('status') == 'completed':
                elapsed = time.time() - start_time
                print(f"\n\n처리 완료! ({elapsed:.2f}초)")

                result = status_data.get('result', {})
                print(f"언어: {result.get('language')}")
                print(f"세그먼트 수: {len(result.get('segments', []))}")
                print(f"\n--- 전체 텍스트 ---")
                print(result.get('text', '')[:1000])

                return result

            elif status_data.get('status') == 'failed':
                print(f"\n실패: {status_data.get('error')}")
                return None

            if time.time() - start_time > 300:
                print("\n타임아웃 (5분)")
                return None

    except Exception as e:
        print(f"\nError: {e}")
        return None


def compare_with_ground_truth(stt_text: str, ground_truth_path: str):
    """정답과 비교"""
    print("\n" + "=" * 60)
    print("4. 정답 비교")
    print("=" * 60)

    if not os.path.exists(ground_truth_path):
        print(f"정답 파일 없음: {ground_truth_path}")
        return

    with open(ground_truth_path, 'r', encoding='utf-8') as f:
        ground_truth = f.read()

    print(f"정답 길이: {len(ground_truth)} 글자")
    print(f"STT 결과 길이: {len(stt_text)} 글자")

    # 간단한 유사도 계산 (글자 수 기반)
    gt_chars = set(ground_truth.replace(' ', '').replace('\n', ''))
    stt_chars = set(stt_text.replace(' ', '').replace('\n', ''))

    common = len(gt_chars & stt_chars)
    total = len(gt_chars | stt_chars)

    if total > 0:
        similarity = common / total * 100
        print(f"글자 집합 유사도: {similarity:.1f}%")


def main():
    # 테스트 파일 경로
    script_dir = Path(__file__).parent
    audio_path = script_dir / "test_meeting.mp3"
    ground_truth_path = script_dir / "test_meeting.txt"

    # 커맨드라인 인자로 파일 지정 가능
    if len(sys.argv) > 1:
        audio_path = Path(sys.argv[1])

    print("=" * 60)
    print("A10G GPU 서버 STT 테스트")
    print(f"서버: {GPU_SERVER}")
    print("=" * 60)

    # 1. 서버 상태 확인
    if not test_health():
        print("\nWhisper 모델이 로드되지 않았습니다.")
        return

    # 2. 동기 STT 테스트
    result = test_stt_sync(str(audio_path))

    # 3. 정답 비교 (있으면)
    if result and ground_truth_path.exists():
        compare_with_ground_truth(result.get('text', ''), str(ground_truth_path))

    # 4. 세그먼트 상세 (처음 5개)
    if result and result.get('segments'):
        print("\n" + "=" * 60)
        print("5. 세그먼트 상세 (처음 5개)")
        print("=" * 60)
        for i, seg in enumerate(result['segments'][:5]):
            start = seg.get('start', 0)
            end = seg.get('end', 0)
            text = seg.get('text', '')
            print(f"[{start:.1f}s - {end:.1f}s] {text}")


if __name__ == '__main__':
    main()
