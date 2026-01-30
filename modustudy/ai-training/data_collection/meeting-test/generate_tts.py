"""
TTS 음성 생성 스크립트
edge-tts를 사용하여 각 발언을 mp3 파일로 변환
+ 전체 음성 파일(MIXED) 생성

사용법:
1. pip install edge-tts pydub
2. python generate_tts.py
"""

import asyncio
import edge_tts
from pathlib import Path
import json
import os
import subprocess

AUDIO_DIR = Path(__file__).parent / "audio"
BASE_DIR = Path(__file__).parent

# 한국어 음성 (남/여 구분)
VOICES = {
    "male": ["ko-KR-InJoonNeural", "ko-KR-HyunsuNeural"],
    "female": ["ko-KR-SunHiNeural", "ko-KR-YuJinNeural"]
}

# 발화자별 성별 매핑 (임의 지정)
SPEAKER_GENDER = {
    "배소희": "female",
    "강현우": "male",
    "박준혁": "male",
    "김민수": "male",
    "성경훈": "male",
    "임수빈": "female",
    "윤상훈": "male",
    "한지민": "female",
    "오세진": "male",
    "조유진": "female",
    "조문희": "female",
    "송민재": "male",
    "신재혁": "male",
    "정다은": "female",
    "윤태호": "male",
    "이지은": "female",
    "황예린": "female",
    "최서연": "female",
    "모두": "male",  # 기타
}


def get_voice_for_speaker(speaker_name: str, speaker_idx: int) -> str:
    """발화자별 음성 선택"""
    gender = SPEAKER_GENDER.get(speaker_name, "male" if speaker_idx % 2 == 0 else "female")
    voices = VOICES[gender]
    return voices[speaker_idx % len(voices)]


async def generate_audio(text: str, output_path: Path, voice: str):
    """TTS로 음성 생성"""
    try:
        communicate = edge_tts.Communicate(text, voice)
        await communicate.save(str(output_path))
        return True
    except Exception as e:
        print(f"오류: {output_path.name} - {e}")
        return False


def merge_audio_files(audio_files: list, output_path: Path):
    """
    ffmpeg를 사용하여 여러 mp3 파일을 하나로 합치기
    """
    if not audio_files:
        return False

    # 파일 목록 생성
    list_file = output_path.parent / f"{output_path.stem}_list.txt"
    with open(list_file, 'w', encoding='utf-8') as f:
        for audio_file in audio_files:
            f.write(f"file '{audio_file}'\n")

    try:
        cmd = [
            "ffmpeg", "-f", "concat", "-safe", "0",
            "-i", str(list_file),
            "-c", "copy",
            "-y", str(output_path)
        ]
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=300)

        if result.returncode == 0:
            print(f"MIXED 생성 완료: {output_path.name}")
            os.unlink(list_file)
            return True
        else:
            print(f"MIXED 생성 실패: {result.stderr[:200]}")
            return False
    except Exception as e:
        print(f"MIXED 합성 오류: {e}")
        return False
    finally:
        if list_file.exists():
            os.unlink(list_file)


async def main():
    # 폴더 생성
    AUDIO_DIR.mkdir(exist_ok=True)

    # 발언 데이터 로드
    with open(BASE_DIR / "utterances.json", "r", encoding="utf-8") as f:
        utterances = json.load(f)

    # 회의 정보 로드
    with open(BASE_DIR / "meeting_info.json", "r", encoding="utf-8") as f:
        meeting_info = json.load(f)

    print(f"총 {len(utterances)}개 발언 음성 생성 시작...")
    print("=" * 60)

    # 발화자별 인덱스 추적
    speaker_indices = {}

    # 미팅별 오디오 파일 추적
    meeting_audio_files = {}

    # 순차 생성 (API 제한 고려)
    success_count = 0
    for i, item in enumerate(utterances):
        output_path = AUDIO_DIR / item["filename"]
        meeting_id = item["meeting_id"]

        # 미팅별 파일 리스트 초기화
        if meeting_id not in meeting_audio_files:
            meeting_audio_files[meeting_id] = []

        if not output_path.exists():
            # 발화자별 음성 선택
            speaker = item["speaker_name"]
            if speaker not in speaker_indices:
                speaker_indices[speaker] = len(speaker_indices)
            voice = get_voice_for_speaker(speaker, speaker_indices[speaker])

            if await generate_audio(item["content"], output_path, voice):
                success_count += 1
                meeting_audio_files[meeting_id].append(str(output_path))
                print(f"[{i+1}/{len(utterances)}] {output_path.name} ({speaker})")
            else:
                print(f"[{i+1}/{len(utterances)}] 실패: {output_path.name}")

            # API 제한 방지를 위한 딜레이
            if (i + 1) % 5 == 0:
                await asyncio.sleep(0.5)
        else:
            meeting_audio_files[meeting_id].append(str(output_path))
            print(f"[{i+1}/{len(utterances)}] 이미 존재: {output_path.name}")
            success_count += 1

        if (i + 1) % 20 == 0:
            print(f"진행: {i + 1}/{len(utterances)}")

    print("=" * 60)
    print(f"개별 음성 생성 완료: {success_count}/{len(utterances)}")

    # 미팅별 MIXED 파일 생성
    print("\n전체 음성(MIXED) 파일 생성 중...")
    print("=" * 60)

    for meeting_id, audio_files in meeting_audio_files.items():
        if audio_files:
            # 시간순 정렬 (파일명에 order가 포함되어 있음)
            audio_files.sort()
            mixed_output = AUDIO_DIR / f"meeting_{meeting_id}_mixed.mp3"
            merge_audio_files(audio_files, mixed_output)

    print("=" * 60)
    print("전체 음성 생성 완료!")

    # 결과 요약
    print("\n" + "=" * 60)
    print("생성된 파일 요약:")
    print("=" * 60)

    for meeting in meeting_info["meetings"]:
        meeting_id = meeting["meeting_id"]
        individual_count = len([f for f in (AUDIO_DIR).glob(f"meeting_{meeting_id}_user_*.mp3")])
        mixed_exists = (AUDIO_DIR / f"meeting_{meeting_id}_mixed.mp3").exists()
        print(f"미팅 {meeting_id} [{meeting['topic']}]: 개별 {individual_count}개, MIXED {'O' if mixed_exists else 'X'}")


if __name__ == "__main__":
    asyncio.run(main())
