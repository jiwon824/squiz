"""
YouTube 개발 강의 자막 수집 스크립트

용도: Whisper STT 파인튜닝 데이터
예상 시간: 30분~1시간 (100개 영상 기준)
"""

import subprocess
import os
import json
import glob
import re
from pathlib import Path

# ===== 설정 =====
OUTPUT_DIR = "./youtube_data"
os.makedirs(OUTPUT_DIR, exist_ok=True)

# 한국어 개발 강의 채널 (대량)
YOUTUBE_CHANNELS = [
    # === 프로그래밍 기초/풀스택 ===
    {"name": "nomadcoders", "url": "https://www.youtube.com/@nomadcoders/videos"},
    {"name": "codingapple", "url": "https://www.youtube.com/@codingapple/videos"},
    {"name": "opentutorials", "url": "https://www.youtube.com/@opentutorials/videos"},  # 생활코딩
    {"name": "dreamcoding", "url": "https://www.youtube.com/@dream-coding/videos"},  # 드림코딩
    {"name": "jocoding", "url": "https://www.youtube.com/@jocoding/videos"},  # 조코딩

    # === 심화/전문 ===
    {"name": "pocu", "url": "https://www.youtube.com/@poaborern/videos"},  # POCU 아카데미
    {"name": "honglab", "url": "https://www.youtube.com/@honglab/videos"},  # 홍정모
    {"name": "baekjoon", "url": "https://www.youtube.com/@baaborern/videos"},  # 백준 알고리즘
    {"name": "rethink", "url": "https://www.youtube.com/@rethink-something/videos"},  # 리씽크
    {"name": "techlead", "url": "https://www.youtube.com/@TechLeadKorea/videos"},

    # === 기업 기술 ===
    {"name": "woowahan", "url": "https://www.youtube.com/@woowahantech/videos"},  # 우아한테크
    {"name": "nhncloud", "url": "https://www.youtube.com/@NHNCloud/videos"},
    {"name": "navercloud", "url": "https://www.youtube.com/@navercloudplatform/videos"},
    {"name": "kakaotech", "url": "https://www.youtube.com/@kaaborern.tech/videos"},
    {"name": "linedevelopers", "url": "https://www.youtube.com/@LINEDevelopers/videos"},
    {"name": "samsungsds", "url": "https://www.youtube.com/@SamsungSDSKR/videos"},
    {"name": "nextstep", "url": "https://www.youtube.com/@nextstep_/videos"},  # 넥스트스텝

    # === 개발자 토크/팟캐스트 ===
    {"name": "devbadak", "url": "https://www.youtube.com/@devbadak/videos"},  # 개발바닥
    {"name": "codesurf", "url": "https://www.youtube.com/@codesurf/videos"},  # 코드서프
    {"name": "itsohard", "url": "https://www.youtube.com/@itsohard2/videos"},  # IT 대기업 현실
    {"name": "jscode", "url": "https://www.youtube.com/@jscode-youtube/videos"},

    # === 프론트엔드 특화 ===
    {"name": "zerochoidev", "url": "https://www.youtube.com/@zerochoidev/videos"},  # 제로초
    {"name": "lablup", "url": "https://www.youtube.com/@lablup/videos"},

    # === 백엔드/인프라 ===
    {"name": "ttbkk", "url": "https://www.youtube.com/@ttbkk/videos"},  # 쉬운코딩
    {"name": "systemdesign", "url": "https://www.youtube.com/@SystemDesignKR/videos"},

    # === 취업/커리어 ===
    {"name": "dongbinna", "url": "https://www.youtube.com/@dongbinna/videos"},  # 동빈나
    {"name": "gyulee", "url": "https://www.youtube.com/@gureumi-coding/videos"},

    # === 알고리즘 ===
    {"name": "rios", "url": "https://www.youtube.com/@RiosCode/videos"},
    {"name": "techhwan", "url": "https://www.youtube.com/@TechHwan/videos"},

    # === AI/ML ===
    {"name": "minsuksung", "url": "https://www.youtube.com/@minsuk-sung/videos"},  # 성민석
    {"name": "aikorea", "url": "https://www.youtube.com/@AIKorea/videos"},
    {"name": "deepsystems", "url": "https://www.youtube.com/@DeepSystems/videos"},

    # === 최근 추가된 유명 채널 ===
    {"name": "yalco", "url": "https://www.youtube.com/@yalco-coding/videos"},  # 얄코
    {"name": "inflearn", "url": "https://www.youtube.com/@inflearn/videos"},  # 인프런
    {"name": "goorm", "url": "https://www.youtube.com/@goaborner/videos"},  # 구름
    {"name": "programmers", "url": "https://www.youtube.com/@programmers/videos"},  # 프로그래머스
    {"name": "ssafy", "url": "https://www.youtube.com/@SSAFY_official/videos"},  # 삼성 싸피
]

# 영상당 설정
MAX_VIDEOS_PER_CHANNEL = 30  # 채널당 최대 영상 수


def install_ytdlp():
    """yt-dlp 설치"""
    print("yt-dlp 설치 확인 중...")
    try:
        subprocess.run(["yt-dlp", "--version"], capture_output=True, check=True)
        print("✓ yt-dlp 이미 설치됨")
    except:
        print("yt-dlp 설치 중...")
        subprocess.run(["pip", "install", "-q", "yt-dlp"], check=True)
        print("✓ 설치 완료!")


def download_subtitles(channel, max_videos=30):
    """채널에서 자막 다운로드"""
    name = channel["name"]
    url = channel["url"]
    output_path = os.path.join(OUTPUT_DIR, name)
    os.makedirs(output_path, exist_ok=True)

    print(f"\n{'='*50}")
    print(f"📺 {name}")
    print(f"   URL: {url}")
    print(f"   최대: {max_videos}개")

    cmd = [
        "yt-dlp",
        "--write-auto-sub",
        "--write-sub",
        "--sub-lang", "ko",
        "--sub-format", "vtt",
        "--skip-download",
        "--no-overwrites",
        "--playlist-end", str(max_videos),
        "--ignore-errors",
        "--no-warnings",
        "-o", f"{output_path}/%(title)s.%(ext)s",
        url
    ]

    try:
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=300)
        vtt_count = len(glob.glob(f"{output_path}/*.vtt"))
        print(f"   ✓ 완료: {vtt_count}개 자막 수집")
        return vtt_count
    except subprocess.TimeoutExpired:
        print(f"   ⚠ 타임아웃 (5분)")
        return 0
    except Exception as e:
        print(f"   ✗ 실패: {e}")
        return 0


def clean_vtt_text(text):
    """VTT 자막에서 불필요한 부분 제거"""
    # 타임스탬프 제거
    text = re.sub(r'\d{2}:\d{2}:\d{2}\.\d{3} --> \d{2}:\d{2}:\d{2}\.\d{3}', '', text)
    # 위치 태그 제거
    text = re.sub(r'align:start position:\d+%', '', text)
    # HTML 태그 제거
    text = re.sub(r'<[^>]+>', '', text)
    # WEBVTT 헤더 제거
    text = re.sub(r'WEBVTT\nKind:.*\nLanguage:.*\n', '', text)
    # 중복 줄 제거
    lines = text.split('\n')
    unique_lines = []
    for line in lines:
        line = line.strip()
        if line and (not unique_lines or unique_lines[-1] != line):
            unique_lines.append(line)
    return ' '.join(unique_lines)


def parse_vtt(vtt_path):
    """VTT 파일 파싱"""
    try:
        with open(vtt_path, "r", encoding="utf-8") as f:
            content = f.read()
        return clean_vtt_text(content)
    except:
        return ""


def process_all_subtitles():
    """모든 자막 파일 처리"""
    print(f"\n{'='*50}")
    print("📝 자막 파일 처리 중...")
    print(f"{'='*50}")

    all_data = []
    vtt_files = glob.glob(f"{OUTPUT_DIR}/**/*.vtt", recursive=True)

    for vtt_path in vtt_files:
        text = parse_vtt(vtt_path)
        if len(text) > 200:  # 최소 200자
            filename = Path(vtt_path).stem
            channel = Path(vtt_path).parent.name
            all_data.append({
                "source": "youtube",
                "channel": channel,
                "title": filename,
                "text": text,
                "char_count": len(text),
                "word_count": len(text.split())
            })

    print(f"✓ 처리 완료: {len(all_data)}개 자막")
    return all_data


def create_training_format(data):
    """학습용 형식으로 변환"""
    training_data = []

    for item in data:
        text = item["text"]

        # 긴 텍스트는 청크로 분할 (4000자 단위)
        chunks = [text[i:i+4000] for i in range(0, len(text), 4000)]

        for i, chunk in enumerate(chunks):
            if len(chunk) > 500:  # 최소 500자
                training_data.append({
                    "source": item["channel"],
                    "title": f"{item['title']}_{i}" if len(chunks) > 1 else item["title"],
                    "text": chunk,
                    "type": "lecture"
                })

    return training_data


def save_dataset(data, training_data):
    """데이터셋 저장"""
    # 원본 저장
    raw_path = os.path.join(OUTPUT_DIR, "youtube_subtitles_raw.json")
    with open(raw_path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    # 학습용 저장
    train_path = os.path.join(OUTPUT_DIR, "youtube_training_data.json")
    with open(train_path, "w", encoding="utf-8") as f:
        json.dump(training_data, f, ensure_ascii=False, indent=2)

    # 통계
    total_chars = sum(d["char_count"] for d in data)
    total_words = sum(d["word_count"] for d in data)

    print(f"\n{'='*50}")
    print("💾 데이터셋 저장 완료!")
    print(f"{'='*50}")
    print(f"📁 원본: {raw_path}")
    print(f"   - {len(data)}개 영상")
    print(f"   - {total_chars:,}자 / {total_words:,}단어")
    print(f"\n📁 학습용: {train_path}")
    print(f"   - {len(training_data)}개 샘플")
    print(f"{'='*50}")


def main():
    """메인 실행"""
    print("="*50)
    print("🎬 YouTube 개발 강의 자막 수집기")
    print("="*50)
    print(f"📋 대상 채널: {len(YOUTUBE_CHANNELS)}개")
    print(f"📋 채널당 최대: {MAX_VIDEOS_PER_CHANNEL}개 영상")
    print(f"📋 예상 수집: ~{len(YOUTUBE_CHANNELS) * MAX_VIDEOS_PER_CHANNEL}개 자막")
    print("="*50)

    # 1. yt-dlp 설치 확인
    install_ytdlp()

    # 2. 각 채널에서 자막 수집
    total_collected = 0
    for channel in YOUTUBE_CHANNELS:
        count = download_subtitles(channel, max_videos=MAX_VIDEOS_PER_CHANNEL)
        total_collected += count

    print(f"\n📊 총 수집: {total_collected}개 자막 파일")

    # 3. 자막 파일 처리
    data = process_all_subtitles()

    # 4. 학습용 형식 변환
    training_data = create_training_format(data)

    # 5. 저장
    save_dataset(data, training_data)

    print("\n✅ 완료! 다음 단계:")
    print("1. youtube_data/youtube_training_data.json 확인")
    print("2. Whisper/Qwen3 학습 노트북에서 사용")


if __name__ == "__main__":
    main()
