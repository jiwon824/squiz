"""
스터디 회의록 텍스트를 음성으로 변환하는 스크립트
- study_meeting_data_500.json에서 20개 랜덤 선택
- gTTS를 사용하여 한국어 음성으로 변환
- test-voice 폴더에 MP3 파일로 저장
"""

import json
import random
import os
from pathlib import Path

# gTTS 설치 확인
try:
    from gtts import gTTS
except ImportError:
    print("gTTS가 설치되어 있지 않습니다. 설치 중...")
    import subprocess
    subprocess.check_call(['pip', 'install', 'gtts'])
    from gtts import gTTS

def load_meeting_data(json_path: str) -> list:
    """JSON 파일에서 회의록 데이터 로드"""
    with open(json_path, 'r', encoding='utf-8') as f:
        return json.load(f)

def text_to_speech(text: str, output_path: str, lang: str = 'ko') -> bool:
    """텍스트를 음성으로 변환하여 저장"""
    try:
        tts = gTTS(text=text, lang=lang, slow=False)
        tts.save(output_path)
        return True
    except Exception as e:
        print(f"TTS 변환 실패: {e}")
        return False

def main():
    import sys

    # 시작 인덱스 (명령줄 인자로 받음, 기본값 1)
    start_index = int(sys.argv[1]) if len(sys.argv) > 1 else 1

    # 경로 설정
    script_dir = Path(__file__).parent
    data_dir = script_dir.parent
    json_path = data_dir / "study_meeting_data_500.json"
    output_dir = script_dir  # test-voice 폴더

    print(f"데이터 파일: {json_path}")
    print(f"출력 폴더: {output_dir}")
    print(f"시작 인덱스: {start_index}")

    # 데이터 로드
    print("\n회의록 데이터 로딩 중...")
    meetings = load_meeting_data(str(json_path))
    print(f"총 {len(meetings)}개의 회의록 발견")

    # 20개 랜덤 선택
    sample_size = min(20, len(meetings))
    selected_meetings = random.sample(meetings, sample_size)
    print(f"{sample_size}개 회의록 랜덤 선택 완료")

    # TTS 변환
    print("\n음성 변환 시작...")
    success_count = 0

    for i, meeting in enumerate(selected_meetings, start_index):
        topic = meeting.get('topic', 'unknown').replace('/', '_')
        transcript = meeting.get('transcript', '')

        if not transcript:
            print(f"[{i}] 스킵 - 회의록 내용 없음")
            continue

        # 파일명 생성 (topic + index)
        filename = f"meeting_{i:02d}_{topic[:20]}.mp3"
        output_path = output_dir / filename

        # 중복 파일 체크
        if output_path.exists():
            print(f"[{i}] 스킵 - 파일 이미 존재: {filename}")
            continue

        print(f"[{i}] 변환 중: {filename}")

        if text_to_speech(transcript, str(output_path)):
            success_count += 1
            print(f"  -> 완료 ({len(transcript)} 글자)")
        else:
            print(f"  -> 실패")

    print(f"\n=== 완료 ===")
    print(f"성공: {success_count}/{sample_size}")
    print(f"저장 위치: {output_dir}")

if __name__ == "__main__":
    main()
