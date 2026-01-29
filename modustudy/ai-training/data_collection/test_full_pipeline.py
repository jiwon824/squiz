#!/usr/bin/env python
"""
A10G GPU 서버 전체 파이프라인 테스트
1. STT (음성 → 텍스트)
2. 회의 요약
3. 액션 아이템 추출
4. 복습 퀴즈 생성
"""

import requests
import time
import os
import json
from pathlib import Path
from datetime import datetime

# GPU 서버 주소
GPU_SERVER = "http://3.88.71.92:8000"

# 결과 저장 디렉토리
RESULTS_DIR = Path(__file__).parent / "test_results"
RESULTS_DIR.mkdir(exist_ok=True)


def print_section(title):
    print("\n" + "=" * 70)
    print(f"  {title}")
    print("=" * 70)


def test_health():
    """서버 상태 확인"""
    print_section("1. 서버 상태 확인")

    try:
        resp = requests.get(f"{GPU_SERVER}/health", timeout=10)
        data = resp.json()
        print(f"Status: {data.get('status')}")
        print(f"LLM Loaded: {data.get('llm_loaded')}")
        print(f"Whisper Loaded: {data.get('whisper_loaded')}")
        print(f"Whisper Model: {data.get('whisper_model')}")
        print(f"LLM Model: {data.get('model_path')}")

        if not data.get('llm_loaded') or not data.get('whisper_loaded'):
            print("\n[ERROR] 모델이 모두 로드되지 않았습니다!")
            return False
        return True
    except Exception as e:
        print(f"[ERROR] {e}")
        return False


def test_stt(audio_path: str) -> str:
    """STT 테스트"""
    print_section("2. STT (Speech-to-Text)")

    if not os.path.exists(audio_path):
        print(f"[ERROR] 파일 없음: {audio_path}")
        return None

    file_size = os.path.getsize(audio_path) / (1024 * 1024)
    print(f"파일: {audio_path}")
    print(f"크기: {file_size:.2f} MB")
    print("\nSTT 처리 중...")

    start_time = time.time()

    try:
        with open(audio_path, 'rb') as f:
            files = {'file': (os.path.basename(audio_path), f, 'audio/mpeg')}
            resp = requests.post(
                f"{GPU_SERVER}/api/stt",
                files=files,
                timeout=600
            )

        elapsed = time.time() - start_time

        if resp.status_code == 200:
            data = resp.json()
            transcript = data.get('text', '')

            print(f"\n[SUCCESS] 처리 시간: {elapsed:.2f}초")
            print(f"언어: {data.get('language')}")
            print(f"세그먼트 수: {len(data.get('segments', []))}")
            print(f"텍스트 길이: {len(transcript)} 글자")
            print(f"\n--- 텍스트 미리보기 (처음 500자) ---")
            print(transcript[:500] + "..." if len(transcript) > 500 else transcript)

            return transcript
        else:
            print(f"[ERROR] {resp.status_code}: {resp.text}")
            return None

    except Exception as e:
        print(f"[ERROR] {e}")
        return None


def test_summarize(transcript: str) -> str:
    """요약 테스트"""
    print_section("3. 회의 요약 생성")

    if not transcript:
        print("[SKIP] 트랜스크립트 없음")
        return None

    print(f"입력 텍스트 길이: {len(transcript)} 글자")
    print("\n요약 생성 중...")

    start_time = time.time()

    try:
        resp = requests.post(
            f"{GPU_SERVER}/api/summarize",
            json={
                "transcript": transcript,
                "max_tokens": 1024,
                "temperature": 0.7
            },
            timeout=300
        )

        elapsed = time.time() - start_time

        if resp.status_code == 200:
            data = resp.json()
            summary = data.get('summary', '')
            tokens = data.get('tokens_used', 0)

            print(f"\n[SUCCESS] 처리 시간: {elapsed:.2f}초")
            print(f"토큰 사용: {tokens}")
            print(f"\n--- 요약 결과 ---")
            print(summary)

            return summary
        else:
            print(f"[ERROR] {resp.status_code}: {resp.text}")
            return None

    except Exception as e:
        print(f"[ERROR] {e}")
        return None


def test_action_items(transcript: str) -> str:
    """액션 아이템 추출 테스트"""
    print_section("4. 액션 아이템 추출")

    if not transcript:
        print("[SKIP] 트랜스크립트 없음")
        return None

    print("액션 아이템 추출 중...")

    start_time = time.time()

    try:
        resp = requests.post(
            f"{GPU_SERVER}/api/action-items",
            json={
                "transcript": transcript,
                "max_tokens": 512,
                "temperature": 0.5
            },
            timeout=300
        )

        elapsed = time.time() - start_time

        if resp.status_code == 200:
            data = resp.json()
            action_items = data.get('action_items', '')
            tokens = data.get('tokens_used', 0)

            print(f"\n[SUCCESS] 처리 시간: {elapsed:.2f}초")
            print(f"토큰 사용: {tokens}")
            print(f"\n--- 액션 아이템 ---")
            print(action_items)

            return action_items
        else:
            print(f"[ERROR] {resp.status_code}: {resp.text}")
            return None

    except Exception as e:
        print(f"[ERROR] {e}")
        return None


def test_quiz(summary: str, num_questions: int = 5) -> str:
    """복습 퀴즈 생성 테스트"""
    print_section("5. 복습 퀴즈 생성")

    if not summary:
        print("[SKIP] 요약 없음")
        return None

    print(f"퀴즈 {num_questions}문제 생성 중...")

    start_time = time.time()

    try:
        resp = requests.post(
            f"{GPU_SERVER}/api/quiz",
            json={
                "summary": summary,
                "num_questions": num_questions,
                "max_tokens": 1024,
                "temperature": 0.7
            },
            timeout=300
        )

        elapsed = time.time() - start_time

        if resp.status_code == 200:
            data = resp.json()
            quiz = data.get('quiz', '')
            tokens = data.get('tokens_used', 0)

            print(f"\n[SUCCESS] 처리 시간: {elapsed:.2f}초")
            print(f"토큰 사용: {tokens}")
            print(f"\n--- 복습 퀴즈 ---")
            print(quiz)

            return quiz
        else:
            print(f"[ERROR] {resp.status_code}: {resp.text}")
            return None

    except Exception as e:
        print(f"[ERROR] {e}")
        return None


def save_results(results: dict, audio_name: str):
    """결과 저장"""
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = f"pipeline_result_{audio_name}_{timestamp}.json"
    filepath = RESULTS_DIR / filename

    with open(filepath, 'w', encoding='utf-8') as f:
        json.dump(results, f, ensure_ascii=False, indent=2)

    print(f"\n결과 저장: {filepath}")

    # 읽기 쉬운 텍스트 형식도 저장
    txt_filepath = RESULTS_DIR / f"pipeline_result_{audio_name}_{timestamp}.txt"
    with open(txt_filepath, 'w', encoding='utf-8') as f:
        f.write("=" * 70 + "\n")
        f.write("  AI 파이프라인 테스트 결과\n")
        f.write(f"  테스트 시간: {timestamp}\n")
        f.write("=" * 70 + "\n\n")

        if results.get('transcript'):
            f.write("## STT 결과\n")
            f.write("-" * 50 + "\n")
            f.write(results['transcript'] + "\n\n")

        if results.get('summary'):
            f.write("## 회의 요약\n")
            f.write("-" * 50 + "\n")
            f.write(results['summary'] + "\n\n")

        if results.get('action_items'):
            f.write("## 액션 아이템\n")
            f.write("-" * 50 + "\n")
            f.write(results['action_items'] + "\n\n")

        if results.get('quiz'):
            f.write("## 복습 퀴즈\n")
            f.write("-" * 50 + "\n")
            f.write(results['quiz'] + "\n\n")

        f.write("\n## 처리 시간\n")
        f.write("-" * 50 + "\n")
        for key, value in results.get('timing', {}).items():
            f.write(f"- {key}: {value:.2f}초\n")
        f.write(f"- 총 시간: {results.get('total_time', 0):.2f}초\n")

    print(f"텍스트 결과 저장: {txt_filepath}")


def main():
    import sys

    # 테스트 파일 경로
    script_dir = Path(__file__).parent
    audio_path = script_dir / "test_meeting.mp3"

    if len(sys.argv) > 1:
        audio_path = Path(sys.argv[1])

    print("=" * 70)
    print("  A10G GPU 서버 전체 파이프라인 테스트")
    print(f"  서버: {GPU_SERVER}")
    print(f"  오디오: {audio_path}")
    print("=" * 70)

    total_start = time.time()
    timing = {}
    results = {
        'audio_file': str(audio_path),
        'server': GPU_SERVER,
        'timestamp': datetime.now().isoformat(),
    }

    # 1. 서버 상태 확인
    if not test_health():
        print("\n[ABORT] 서버 상태 불량")
        return

    # 2. STT
    stt_start = time.time()
    transcript = test_stt(str(audio_path))
    timing['stt'] = time.time() - stt_start
    results['transcript'] = transcript

    if not transcript:
        print("\n[ABORT] STT 실패")
        return

    # 3. 요약
    summary_start = time.time()
    summary = test_summarize(transcript)
    timing['summary'] = time.time() - summary_start
    results['summary'] = summary

    # 4. 액션 아이템
    action_start = time.time()
    action_items = test_action_items(transcript)
    timing['action_items'] = time.time() - action_start
    results['action_items'] = action_items

    # 5. 퀴즈
    quiz_start = time.time()
    quiz = test_quiz(summary, num_questions=5)
    timing['quiz'] = time.time() - quiz_start
    results['quiz'] = quiz

    # 결과 정리
    total_time = time.time() - total_start
    results['timing'] = timing
    results['total_time'] = total_time

    # 결과 저장
    audio_name = audio_path.stem
    save_results(results, audio_name)

    # 최종 요약
    print_section("테스트 완료 - 결과 요약")
    print(f"총 처리 시간: {total_time:.2f}초")
    print(f"\n처리 단계별 시간:")
    for step, duration in timing.items():
        status = "✓" if results.get(step) else "✗"
        print(f"  {status} {step}: {duration:.2f}초")

    success_count = sum(1 for k in ['transcript', 'summary', 'action_items', 'quiz'] if results.get(k))
    print(f"\n성공: {success_count}/4 단계")


if __name__ == '__main__':
    main()
