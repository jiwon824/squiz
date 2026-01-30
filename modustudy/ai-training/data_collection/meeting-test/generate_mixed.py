"""
MIXED 음성 파일 생성 스크립트
ffmpeg를 사용하여 화자별 음성을 시간순으로 합성

사용법:
1. ffmpeg 설치 (winget install ffmpeg)
2. python generate_mixed.py
"""

from pathlib import Path
import json
import subprocess
import os
import shutil

AUDIO_DIR = Path(__file__).parent / "audio"
BASE_DIR = Path(__file__).parent


def find_ffmpeg():
    """ffmpeg 실행 파일 경로 찾기"""
    # 1. PATH에서 찾기
    ffmpeg_path = shutil.which("ffmpeg")
    if ffmpeg_path:
        return ffmpeg_path

    # 2. winget 설치 경로에서 찾기
    winget_packages = Path.home() / "AppData/Local/Microsoft/WinGet/Packages"
    if winget_packages.exists():
        for pkg_dir in winget_packages.glob("Gyan.FFmpeg*/ffmpeg*/bin/ffmpeg.exe"):
            if pkg_dir.exists():
                return str(pkg_dir)

    # 3. 일반적인 설치 경로
    common_paths = [
        r"C:\ffmpeg\bin\ffmpeg.exe",
        r"C:\Program Files\ffmpeg\bin\ffmpeg.exe",
        r"C:\Users\SSAFY\AppData\Local\Microsoft\WinGet\Links\ffmpeg.exe",
    ]
    for path in common_paths:
        if Path(path).exists():
            return path

    return None


def merge_with_ffmpeg(meeting_id: int, audio_files: list, ffmpeg_path: str) -> bool:
    """ffmpeg를 사용하여 오디오 합성"""
    existing_files = [f for f in audio_files if Path(f).exists()]

    if not existing_files:
        print(f"파일 없음: meeting {meeting_id}")
        return False

    # concat 리스트 파일 생성
    list_file = AUDIO_DIR / f"meeting_{meeting_id}_list.txt"
    with open(list_file, 'w', encoding='utf-8') as f:
        for audio_file in existing_files:
            # Windows 경로를 슬래시로 변환
            safe_path = audio_file.replace('\\', '/')
            f.write(f"file '{safe_path}'\n")

    output_path = AUDIO_DIR / f"meeting_{meeting_id}_mixed.mp3"

    try:
        cmd = [
            ffmpeg_path, "-f", "concat", "-safe", "0",
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
            print(f"MIXED 생성 실패 (meeting {meeting_id}): {result.stderr[:200]}")
            return False

    except FileNotFoundError:
        print("오류: ffmpeg가 설치되지 않았습니다.")
        print("설치 방법: winget install ffmpeg")
        return False
    except Exception as e:
        print(f"MIXED 생성 실패 (meeting {meeting_id}): {e}")
        return False
    finally:
        if list_file.exists():
            os.unlink(list_file)


def main():
    # ffmpeg 경로 찾기
    ffmpeg_path = find_ffmpeg()
    if not ffmpeg_path:
        print("오류: ffmpeg를 찾을 수 없습니다.")
        print("설치 방법: winget install ffmpeg")
        print("설치 후 새 터미널에서 다시 실행하세요.")
        return

    print(f"ffmpeg 경로: {ffmpeg_path}")

    # 발언 데이터 로드
    with open(BASE_DIR / "utterances.json", "r", encoding="utf-8") as f:
        utterances = json.load(f)

    # 미팅별로 그룹화
    meeting_audio_files = {}
    for item in utterances:
        meeting_id = item["meeting_id"]
        if meeting_id not in meeting_audio_files:
            meeting_audio_files[meeting_id] = []
        audio_path = AUDIO_DIR / item["filename"]
        meeting_audio_files[meeting_id].append(str(audio_path))

    print(f"총 {len(meeting_audio_files)}개 미팅의 MIXED 파일 생성 시작...")
    print("=" * 60)

    success = 0
    for meeting_id, audio_files in sorted(meeting_audio_files.items()):
        # 시간순 정렬 (파일명에 order가 포함되어 있음)
        audio_files.sort()
        if merge_with_ffmpeg(meeting_id, audio_files, ffmpeg_path):
            success += 1

    print("=" * 60)
    print(f"완료: {success}/{len(meeting_audio_files)} MIXED 파일 생성됨")


if __name__ == "__main__":
    main()
