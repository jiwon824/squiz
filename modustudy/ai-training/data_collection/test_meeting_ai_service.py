#!/usr/bin/env python
"""
Meeting AI 서비스 통합 테스트 스크립트
- STT (Speech-to-Text)
- 요약 (Summary)
- 키워드 추출 (Keywords)
- 액션 아이템 생성 (Action Items)
"""

import requests
import time
import json
from pathlib import Path
from datetime import datetime


# AI 서버 주소
AI_SERVER = "http://18.207.138.18:8000"

# 테스트 파일
SCRIPT_DIR = Path(__file__).parent
TEST_AUDIO = SCRIPT_DIR / "test_meeting.mp3"
RESULT_DIR = SCRIPT_DIR / "test_results"


def print_section(title):
    """섹션 헤더 출력"""
    print("\n" + "=" * 70)
    print(f"  {title}")
    print("=" * 70)


def save_result(filename, content):
    """결과 저장"""
    RESULT_DIR.mkdir(exist_ok=True)
    result_path = RESULT_DIR / filename

    if isinstance(content, (dict, list)):
        with open(result_path, 'w', encoding='utf-8') as f:
            json.dump(content, f, ensure_ascii=False, indent=2)
    else:
        with open(result_path, 'w', encoding='utf-8') as f:
            f.write(str(content))

    print(f"✓ 결과 저장: {result_path}")
    return result_path


def test_health_check():
    """AI 서버 상태 확인"""
    print_section("1. AI 서버 상태 확인")

    try:
        response = requests.get(f"{AI_SERVER}/health", timeout=10)
        data = response.json()

        print(f"Status: {data.get('status')}")
        print(f"LLM Loaded: {data.get('llm_loaded')}")
        print(f"Whisper Loaded: {data.get('whisper_loaded')}")
        print(f"Whisper Model: {data.get('whisper_model')}")

        if not data.get('whisper_loaded'):
            print("\n⚠️  WARNING: Whisper 모델이 로드되지 않았습니다!")
            return False

        return True

    except Exception as e:
        print(f"❌ 서버 연결 실패: {e}")
        return False


def test_stt_only():
    """STT만 단독 테스트"""
    print_section("2. STT (Speech-to-Text) 테스트")

    if not TEST_AUDIO.exists():
        print(f"❌ 파일 없음: {TEST_AUDIO}")
        return None

    file_size = TEST_AUDIO.stat().st_size / (1024 * 1024)
    print(f"파일: {TEST_AUDIO.name}")
    print(f"크기: {file_size:.2f} MB")

    print("\nSTT 처리 중...", end="", flush=True)
    start_time = time.time()

    try:
        with open(TEST_AUDIO, 'rb') as f:
            files = {'file': (TEST_AUDIO.name, f, 'audio/mpeg')}
            response = requests.post(
                f"{AI_SERVER}/api/stt",
                files=files,
                timeout=300  # 5분
            )

        elapsed = time.time() - start_time

        if response.status_code == 200:
            data = response.json()

            print(f"\n✓ 완료 ({elapsed:.1f}초)")
            print(f"언어: {data.get('language')}")
            print(f"언어 확률: {data.get('language_probability', 0):.1%}")
            print(f"세그먼트 수: {len(data.get('segments', []))}")

            text = data.get('text', '')
            print(f"\n--- 전체 텍스트 (처음 300자) ---")
            print(text[:300])

            # 결과 저장
            save_result('stt_result.json', data)
            save_result('stt_transcript.txt', text)

            return data
        else:
            print(f"\n❌ Error {response.status_code}: {response.text}")
            return None

    except Exception as e:
        print(f"\n❌ Error: {e}")
        return None


def test_meeting_process_full():
    """미팅 전체 처리 테스트 (STT + 요약 + 키워드 + 액션아이템)"""
    print_section("3. Meeting 전체 처리 테스트 (비동기)")

    if not TEST_AUDIO.exists():
        print(f"❌ 파일 없음: {TEST_AUDIO}")
        return None

    print(f"파일: {TEST_AUDIO.name}")
    print("\n미팅 처리 작업 등록 중...")

    try:
        # 1. 작업 등록
        with open(TEST_AUDIO, 'rb') as f:
            files = {
                'mixed_audio': (TEST_AUDIO.name, f, 'audio/mpeg')
            }
            data = {
                'user_ids': '',  # 개별 오디오 없음
                'generate_quiz': 'false'  # 퀴즈는 옵션
            }

            response = requests.post(
                f"{AI_SERVER}/api/process-meeting-full",
                files=files,
                data=data,
                timeout=30
            )

        if response.status_code != 200:
            print(f"❌ 작업 등록 실패: {response.status_code} - {response.text}")
            return None

        result = response.json()
        job_id = result.get('job_id')
        print(f"✓ 작업 등록 완료")
        print(f"Job ID: {job_id}")

        # 2. 결과 폴링
        print("\n처리 중", end="", flush=True)
        start_time = time.time()
        max_wait = 600  # 10분

        while True:
            time.sleep(3)
            print(".", end="", flush=True)

            # 상태 조회
            status_response = requests.get(
                f"{AI_SERVER}/api/jobs/{job_id}",
                timeout=10
            )

            if status_response.status_code != 200:
                continue

            status_data = status_response.json()
            current_status = status_data.get('status')

            if current_status == 'completed':
                elapsed = time.time() - start_time
                print(f"\n✓ 처리 완료! ({elapsed:.1f}초)")

                result_data = status_data.get('result', {})

                # 결과 출력
                print("\n--- 처리 결과 ---")

                # 1. Transcript
                transcript = result_data.get('transcript', '')
                print(f"\n[Transcript] 길이: {len(transcript)} 글자")
                print(f"내용 (처음 200자):\n{transcript[:200]}")

                # 2. Summary
                summary = result_data.get('summary', '')
                print(f"\n[Summary] 길이: {len(summary)} 글자")
                print(f"내용:\n{summary}")

                # 3. Keywords
                keywords = result_data.get('keywords', [])
                print(f"\n[Keywords] 개수: {len(keywords)}")
                print(f"키워드: {', '.join(keywords[:10])}")

                # 4. Action Items
                action_items = result_data.get('action_items', [])
                print(f"\n[Action Items] 개수: {len(action_items)}")
                for i, item in enumerate(action_items[:5], 1):
                    user_id = item.get('user_id', 'N/A')
                    content = item.get('content', '')
                    print(f"  {i}. [User {user_id}] {content}")

                # 결과 저장
                save_result('meeting_full_result.json', status_data)
                save_result('meeting_transcript.txt', transcript)
                save_result('meeting_summary.txt', summary)

                return status_data

            elif current_status == 'failed':
                error = status_data.get('error', 'Unknown error')
                print(f"\n❌ 처리 실패: {error}")
                save_result('meeting_error.json', status_data)
                return None

            # 타임아웃 체크
            if time.time() - start_time > max_wait:
                print(f"\n⚠️  타임아웃 ({max_wait}초)")
                return None

    except Exception as e:
        print(f"\n❌ Error: {e}")
        return None


def test_summarize_only(transcript):
    """요약만 단독 테스트"""
    print_section("4. 요약 (Summarize) 단독 테스트")

    if not transcript:
        print("⚠️  Transcript가 없어서 요약 테스트를 건너뜁니다.")
        return None

    print(f"Transcript 길이: {len(transcript)} 글자")
    print("\n요약 생성 중...")

    try:
        payload = {
            'transcript': transcript,
            'max_tokens': 500
        }

        response = requests.post(
            f"{AI_SERVER}/api/summarize",
            json=payload,
            timeout=60
        )

        if response.status_code == 200:
            data = response.json()
            summary = data.get('summary', '')
            tokens_used = data.get('tokens_used', 0)

            print(f"✓ 요약 생성 완료")
            print(f"토큰 사용: {tokens_used}")
            print(f"\n--- 요약 내용 ---\n{summary}")

            save_result('summarize_only_result.json', data)
            return data
        else:
            print(f"❌ Error {response.status_code}: {response.text}")
            return None

    except Exception as e:
        print(f"❌ Error: {e}")
        return None


def main():
    """메인 테스트 실행"""
    print("=" * 70)
    print("  Meeting AI 서비스 통합 테스트")
    print(f"  서버: {AI_SERVER}")
    print(f"  시간: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 70)

    # 1. 헬스체크
    if not test_health_check():
        print("\n⚠️  AI 서버가 준비되지 않았습니다. 테스트를 중단합니다.")
        return

    # 2. STT 단독 테스트
    stt_result = test_stt_only()

    # 3. Meeting 전체 처리 테스트
    meeting_result = test_meeting_process_full()

    # 4. 요약 단독 테스트 (STT 결과 활용)
    if stt_result:
        transcript = stt_result.get('text', '')
        test_summarize_only(transcript)

    # 최종 요약
    print_section("테스트 완료")
    print(f"결과 저장 경로: {RESULT_DIR}")
    print("\n테스트 항목:")
    print("  ✓ AI 서버 헬스체크")
    print(f"  {'✓' if stt_result else '✗'} STT 단독 테스트")
    print(f"  {'✓' if meeting_result else '✗'} Meeting 전체 처리 테스트")
    print("\n" + "=" * 70)


if __name__ == '__main__':
    main()
