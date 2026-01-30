"""
가장 긴 회의록 10개를 음성으로 변환하는 스크립트
- 이미 존재하는 파일은 건너뜀
"""

import json
import os
from pathlib import Path
from gtts import gTTS

def load_meeting_data(json_path: str) -> list:
    with open(json_path, 'r', encoding='utf-8') as f:
        return json.load(f)

def text_to_speech(text: str, output_path: str, lang: str = 'ko') -> bool:
    try:
        tts = gTTS(text=text, lang=lang, slow=False)
        tts.save(output_path)
        return True
    except Exception as e:
        print(f"TTS 변환 실패: {e}")
        return False

def main():
    script_dir = Path(__file__).parent
    data_dir = script_dir.parent
    json_path = data_dir / "study_meeting_data_500.json"
    output_dir = script_dir

    print("회의록 데이터 로딩 중...")
    meetings = load_meeting_data(str(json_path))
    print(f"총 {len(meetings)}개의 회의록 발견")

    # transcript 길이 기준으로 정렬 (긴 순서)
    meetings_with_length = []
    for idx, m in enumerate(meetings):
        transcript = m.get('transcript', '')
        topic = m.get('topic', 'unknown').replace('/', '_')
        meetings_with_length.append({
            'idx': idx,
            'topic': topic,
            'transcript': transcript,
            'length': len(transcript)
        })

    meetings_sorted = sorted(meetings_with_length, key=lambda x: x['length'], reverse=True)

    print(f"\n가장 긴 회의록 TOP 10:")
    for i, m in enumerate(meetings_sorted[:10], 1):
        print(f"  {i}. {m['topic'][:30]} - {m['length']} 글자")

    # 이미 존재하는 파일 확인
    existing_files = set(f.stem for f in output_dir.glob("*.mp3"))

    # 긴 회의록부터 10개 선택 (빠진 것만)
    print("\n음성 변환 시작...")
    success_count = 0
    target_count = 10

    for m in meetings_sorted:
        if success_count >= target_count:
            break

        topic = m['topic'][:20]
        transcript = m['transcript']

        if not transcript:
            continue

        # 파일명 생성
        filename = f"long_{success_count + 1:02d}_{topic}.mp3"
        output_path = output_dir / filename

        print(f"[{success_count + 1}/{target_count}] 변환 중: {filename} ({m['length']} 글자)")

        if text_to_speech(transcript, str(output_path)):
            success_count += 1
            print(f"  -> 완료")
        else:
            print(f"  -> 실패")

    print(f"\n=== 완료 ===")
    print(f"성공: {success_count}/{target_count}")

if __name__ == "__main__":
    main()
