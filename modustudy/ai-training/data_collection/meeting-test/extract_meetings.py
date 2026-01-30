"""
회의록 추출 및 처리 스크립트
1. 가장 긴 회의록 10개 추출
2. 발화자별 발언 시간순 분리
3. TTS로 MP3 파일 생성
4. DB 저장용 SQL 생성
"""

import json
import re
import os
from pathlib import Path
from datetime import datetime
import asyncio

# 경로 설정
BASE_DIR = Path(__file__).parent
DATA_DIR = BASE_DIR.parent
AUDIO_DIR = BASE_DIR / "audio"
SQL_DIR = BASE_DIR / "sql"

# 폴더 생성
AUDIO_DIR.mkdir(exist_ok=True)
SQL_DIR.mkdir(exist_ok=True)


def load_meetings(json_path: str) -> list:
    """회의록 JSON 로드"""
    with open(json_path, 'r', encoding='utf-8') as f:
        return json.load(f)


def extract_conversation_from_text(text: str) -> str:
    """텍스트에서 회의 내용만 추출 (발화자: 발언 형식)"""
    # user 태그 안의 내용에서 '회의 내용:' 이후 부분 추출
    match = re.search(r'회의 내용:\s*\n(.*?)(?:<\|im_end\|>|$)', text, re.DOTALL)
    if match:
        return match.group(1).strip()
    return ""


def parse_utterances(conversation: str) -> list:
    """
    회의 내용에서 발화자별 발언 파싱
    형식: "이름: 발언 내용"
    """
    utterances = []

    # 줄 단위로 파싱 (더 정확한 방식)
    lines = conversation.split('\n')
    current_speaker = None
    current_content = []

    # 이름 패턴: 줄 시작에서 한글 2~4자 + 콜론
    name_pattern = re.compile(r'^([가-힣]{2,4}):\s*(.*)$')

    for line in lines:
        line = line.strip()
        if not line:
            continue

        match = name_pattern.match(line)
        if match:
            # 이전 발화자의 발언 저장
            if current_speaker and current_content:
                content = ' '.join(current_content).strip()
                if content:
                    utterances.append({
                        'order': len(utterances) + 1,
                        'speaker': current_speaker,
                        'content': content,
                        'timestamp_seconds': len(utterances) * 10
                    })

            # 새 발화자 시작
            current_speaker = match.group(1)
            current_content = [match.group(2)] if match.group(2) else []
        elif current_speaker:
            # 연속된 발언
            current_content.append(line)

    # 마지막 발화자 처리
    if current_speaker and current_content:
        content = ' '.join(current_content).strip()
        if content:
            utterances.append({
                'order': len(utterances) + 1,
                'speaker': current_speaker,
                'content': content,
                'timestamp_seconds': len(utterances) * 10
            })

    return utterances


def get_top_meetings(meetings: list, top_n: int = 10) -> list:
    """발언 수 기준으로 가장 긴 회의록 top_n개 추출"""
    meeting_data = []

    for idx, item in enumerate(meetings):
        text = item.get('text', '')
        conversation = extract_conversation_from_text(text)
        utterances = parse_utterances(conversation)

        if utterances:
            meeting_data.append({
                'original_index': idx,
                'topic': item.get('topic', 'IT 스터디'),
                'type': item.get('type', 'regular_summary'),
                'conversation': conversation,
                'utterances': utterances,
                'utterance_count': len(utterances),
                'speakers': list(set(u['speaker'] for u in utterances))
            })

    # 발언 수 기준 정렬 (내림차순)
    meeting_data.sort(key=lambda x: x['utterance_count'], reverse=True)

    return meeting_data[:top_n]


def generate_speaker_audio_data(meetings: list) -> list:
    """
    발화자별 음성 데이터 생성을 위한 구조 생성
    각 발언을 시간순으로 정렬하고 파일명 생성
    """
    audio_data = []

    for meeting_idx, meeting in enumerate(meetings, start=1):
        meeting_id = meeting_idx + 100  # 테스트용 meeting_id (101~110)

        # 발화자별 user_id 매핑 생성
        speakers = meeting['speakers']
        speaker_user_map = {speaker: (meeting_idx * 10 + i + 1) for i, speaker in enumerate(speakers)}

        # 발언 데이터 처리
        for utterance in meeting['utterances']:
            speaker = utterance['speaker']
            user_id = speaker_user_map[speaker]

            audio_data.append({
                'meeting_id': meeting_id,
                'meeting_topic': meeting['topic'],
                'user_id': user_id,
                'speaker_name': speaker,
                'order': utterance['order'],
                'content': utterance['content'],
                'timestamp_seconds': utterance['timestamp_seconds'],
                'filename': f"meeting_{meeting_id}_user_{user_id}_order_{utterance['order']:03d}.mp3"
            })

    return audio_data


def save_meeting_info(meetings: list, audio_data: list):
    """회의 정보를 JSON으로 저장"""
    output = {
        'generated_at': datetime.now().isoformat(),
        'total_meetings': len(meetings),
        'total_utterances': len(audio_data),
        'meetings': []
    }

    for meeting_idx, meeting in enumerate(meetings, start=1):
        meeting_id = meeting_idx + 100
        meeting_audio = [a for a in audio_data if a['meeting_id'] == meeting_id]

        output['meetings'].append({
            'meeting_id': meeting_id,
            'topic': meeting['topic'],
            'type': meeting['type'],
            'utterance_count': meeting['utterance_count'],
            'speakers': meeting['speakers'],
            'speaker_user_map': {s: (meeting_idx * 10 + i + 1) for i, s in enumerate(meeting['speakers'])},
            'utterances': meeting['utterances'][:5]  # 샘플 5개만
        })

    with open(BASE_DIR / 'meeting_info.json', 'w', encoding='utf-8') as f:
        json.dump(output, f, ensure_ascii=False, indent=2)

    print(f"회의 정보 저장: {BASE_DIR / 'meeting_info.json'}")


def generate_sql_scripts(meetings: list, audio_data: list):
    """DB 저장용 SQL 스크립트 생성 (erd.sql 구조에 맞게)"""

    sql_lines = []
    sql_lines.append("-- ============================================")
    sql_lines.append("-- 미팅 테스트 데이터 SQL")
    sql_lines.append(f"-- 생성일: {datetime.now().isoformat()}")
    sql_lines.append("-- ============================================\n")

    # 1. 테스트용 user 데이터
    sql_lines.append("-- 1. 테스트 사용자 (이미 존재하면 생략)")
    all_users = {}
    for meeting_idx, meeting in enumerate(meetings, start=1):
        for i, speaker in enumerate(meeting['speakers']):
            user_id = meeting_idx * 10 + i + 1
            if user_id not in all_users:
                all_users[user_id] = speaker

    sql_lines.append("INSERT IGNORE INTO `user` (`id`, `email`, `nickname`, `name`, `role`, `is_active`) VALUES")
    user_values = []
    for user_id, name in all_users.items():
        email = f"test_user_{user_id}@test.com"
        user_values.append(f"  ({user_id}, '{email}', '{name}', '{name}', 'USER', TRUE)")
    sql_lines.append(",\n".join(user_values) + ";\n")

    # 2. 테스트용 study 데이터
    sql_lines.append("-- 2. 테스트 스터디 (study_id = 100)")
    sql_lines.append("""INSERT IGNORE INTO `study` (`id`, `leader_id`, `name`, `description`, `topic_id`, `study_type`, `status`) VALUES
  (100, 111, '테스트 스터디', 'AI 테스트용 스터디', 1, 'PLANNED', 'IN_PROGRESS');\n""")

    # 3. 테스트용 workspace 데이터
    sql_lines.append("-- 3. 테스트 워크스페이스")
    sql_lines.append("INSERT IGNORE INTO `workspace` (`id`, `study_id`) VALUES (100, 100);\n")

    # 4. meeting 테이블
    sql_lines.append("-- 4. 미팅 테이블")
    sql_lines.append("INSERT INTO `meeting` (`id`, `study_id`, `workspace_id`, `title`, `meeting_type`, `status`, `started_at`, `ended_at`, `participant_count`, `stt_status`, `summary_status`) VALUES")
    meeting_values = []
    for meeting_idx, meeting in enumerate(meetings, start=1):
        meeting_id = meeting_idx + 100
        topic = meeting['topic'].replace("'", "''")
        participant_count = len(meeting['speakers'])
        meeting_values.append(
            f"  ({meeting_id}, 100, 100, '{topic} 스터디 회의 {meeting_idx}', 'DAILY', 'ENDED', NOW() - INTERVAL {meeting_idx} DAY, NOW() - INTERVAL {meeting_idx} DAY + INTERVAL 1 HOUR, {participant_count}, 'PENDING', 'PENDING')"
        )
    sql_lines.append(",\n".join(meeting_values) + ";\n")

    # 5. meeting_participant 테이블
    sql_lines.append("-- 5. 미팅 참가자")
    sql_lines.append("INSERT INTO `meeting_participant` (`meeting_id`, `user_id`, `joined_at`, `left_at`) VALUES")
    participant_values = []
    for meeting_idx, meeting in enumerate(meetings, start=1):
        meeting_id = meeting_idx + 100
        for i, speaker in enumerate(meeting['speakers']):
            user_id = meeting_idx * 10 + i + 1
            participant_values.append(
                f"  ({meeting_id}, {user_id}, NOW() - INTERVAL {meeting_idx} DAY, NOW() - INTERVAL {meeting_idx} DAY + INTERVAL 1 HOUR)"
            )
    sql_lines.append(",\n".join(participant_values) + ";\n")

    # 6. meeting_audio_recording 테이블 (INDIVIDUAL 트랙)
    sql_lines.append("-- 6. 미팅 오디오 녹음 (화자별 개별 트랙)")
    sql_lines.append("INSERT INTO `meeting_audio_recording` (`meeting_id`, `user_id`, `track_type`, `recording_url`, `format`, `created_at`) VALUES")
    audio_values = []
    processed_user_meetings = set()

    for audio in audio_data:
        key = (audio['meeting_id'], audio['user_id'])
        if key not in processed_user_meetings:
            processed_user_meetings.add(key)
            # 각 화자별 첫 번째 발언 파일을 대표 녹음으로 사용
            recording_url = f"/uploads/meetings/{audio['meeting_id']}/audio/{audio['filename']}"
            audio_values.append(
                f"  ({audio['meeting_id']}, {audio['user_id']}, 'INDIVIDUAL', '{recording_url}', 'mp3', NOW())"
            )
    sql_lines.append(",\n".join(audio_values) + ";\n")

    # 7. meeting_transcript 테이블 (발언 단위)
    sql_lines.append("-- 7. 미팅 트랜스크립트 (발언 단위)")
    sql_lines.append("INSERT INTO `meeting_transcript` (`meeting_id`, `user_id`, `content`, `timestamp_seconds`) VALUES")
    transcript_values = []
    for audio in audio_data:
        content = audio['content'].replace("'", "''")[:500]  # 500자 제한
        transcript_values.append(
            f"  ({audio['meeting_id']}, {audio['user_id']}, '{content}', {audio['timestamp_seconds']})"
        )
    sql_lines.append(",\n".join(transcript_values) + ";\n")

    # SQL 파일 저장
    sql_content = "\n".join(sql_lines)
    with open(SQL_DIR / 'insert_test_data.sql', 'w', encoding='utf-8') as f:
        f.write(sql_content)

    print(f"SQL 스크립트 저장: {SQL_DIR / 'insert_test_data.sql'}")

    return sql_content


def generate_tts_script(audio_data: list):
    """TTS 생성용 스크립트 생성"""

    script = '''"""
TTS 음성 생성 스크립트
edge-tts를 사용하여 각 발언을 mp3 파일로 변환
"""

import asyncio
import edge_tts
from pathlib import Path
import json

AUDIO_DIR = Path(__file__).parent / "audio"
VOICE = "ko-KR-SunHiNeural"  # 한국어 여성 음성

async def generate_audio(text: str, output_path: Path):
    """TTS로 음성 생성"""
    communicate = edge_tts.Communicate(text, VOICE)
    await communicate.save(str(output_path))
    print(f"생성: {output_path.name}")

async def main():
    # 발언 데이터 로드
    with open(Path(__file__).parent / "utterances.json", "r", encoding="utf-8") as f:
        utterances = json.load(f)

    print(f"총 {len(utterances)}개 발언 음성 생성 시작...")

    # 순차 생성 (API 제한 고려)
    for i, item in enumerate(utterances):
        output_path = AUDIO_DIR / item["filename"]
        if not output_path.exists():
            await generate_audio(item["content"], output_path)

        if (i + 1) % 10 == 0:
            print(f"진행: {i + 1}/{len(utterances)}")

    print("\\n음성 생성 완료!")

if __name__ == "__main__":
    asyncio.run(main())
'''

    with open(BASE_DIR / 'generate_tts.py', 'w', encoding='utf-8') as f:
        f.write(script)

    # 발언 데이터도 저장
    with open(BASE_DIR / 'utterances.json', 'w', encoding='utf-8') as f:
        json.dump(audio_data, f, ensure_ascii=False, indent=2)

    print(f"TTS 스크립트 저장: {BASE_DIR / 'generate_tts.py'}")
    print(f"발언 데이터 저장: {BASE_DIR / 'utterances.json'}")


def main():
    print("=" * 60)
    print("회의록 추출 및 처리 시작")
    print("=" * 60)

    # 1. 회의록 로드
    json_path = DATA_DIR / "training_data_merged.json"
    print(f"\n1. 회의록 로드: {json_path}")
    meetings = load_meetings(json_path)
    print(f"   총 {len(meetings)}개 회의록 로드됨")

    # 2. 가장 긴 회의록 10개 추출
    print("\n2. 가장 긴 회의록 10개 추출 중...")
    top_meetings = get_top_meetings(meetings, top_n=10)
    print(f"   추출된 회의록:")
    for i, m in enumerate(top_meetings, 1):
        print(f"   {i}. [{m['topic']}] 발언 {m['utterance_count']}개, 발화자 {len(m['speakers'])}명")

    # 3. 발화자별 음성 데이터 구조 생성
    print("\n3. 발화자별 음성 데이터 구조 생성...")
    audio_data = generate_speaker_audio_data(top_meetings)
    print(f"   총 {len(audio_data)}개 발언 처리됨")

    # 4. 회의 정보 저장
    print("\n4. 회의 정보 저장...")
    save_meeting_info(top_meetings, audio_data)

    # 5. SQL 스크립트 생성
    print("\n5. SQL 스크립트 생성...")
    generate_sql_scripts(top_meetings, audio_data)

    # 6. TTS 스크립트 생성
    print("\n6. TTS 스크립트 생성...")
    generate_tts_script(audio_data)

    print("\n" + "=" * 60)
    print("처리 완료!")
    print("=" * 60)
    print("\n다음 단계:")
    print("1. pip install edge-tts")
    print("2. python generate_tts.py  (음성 파일 생성)")
    print("3. sql/insert_test_data.sql 실행 (DB 데이터 삽입)")


if __name__ == "__main__":
    main()
