"""
회의 처리 API 테스트 스크립트
- study_meeting_data에서 샘플 가져와서 테스트
"""
import json
import requests
from pathlib import Path

# 샘플 회의록 데이터 로드
data_path = Path(__file__).parent.parent / "ai-training/data_collection/study_meeting_data_500.json"
with open(data_path, 'r', encoding='utf-8') as f:
    meetings = json.load(f)

# 긴 회의록 선택
longest = max(meetings, key=lambda x: len(x.get('transcript', '')))

print(f"주제: {longest['topic']}")
print(f"형식: {longest['format']}")
print(f"회의록 길이: {len(longest['transcript'])}자")
print("="*50)
print("회의록 내용 (앞부분):")
print(longest['transcript'][:500])
print("="*50)

# /api/summarize 테스트 (텍스트 기반)
API_BASE = "http://localhost:8000"

print("\n[1] 요약 API 테스트...")
try:
    resp = requests.post(f"{API_BASE}/api/summarize", json={
        "transcript": longest['transcript'],
        "max_tokens": 512,
        "temperature": 0.7
    }, timeout=120)
    result = resp.json()
    print(f"요약 결과:\n{result.get('summary', 'N/A')}")
    print(f"토큰 사용: {result.get('tokens_used', 'N/A')}")
except Exception as e:
    print(f"요약 실패: {e}")

print("\n[2] 액션아이템 추출 테스트...")
try:
    resp = requests.post(f"{API_BASE}/api/action-items", json={
        "transcript": longest['transcript'],
        "max_tokens": 512,
        "temperature": 0.7
    }, timeout=120)
    result = resp.json()
    print(f"액션아이템:\n{result.get('action_items', 'N/A')}")
except Exception as e:
    print(f"액션아이템 추출 실패: {e}")

print("\n[3] 퀴즈 생성 테스트...")
try:
    # 먼저 요약 가져오기
    summary = longest.get('summary', longest['transcript'][:500])
    resp = requests.post(f"{API_BASE}/api/quiz", json={
        "summary": summary,
        "num_questions": 3,
        "max_tokens": 1024,
        "temperature": 0.7
    }, timeout=120)
    result = resp.json()
    print(f"퀴즈:\n{result.get('quiz', 'N/A')}")
except Exception as e:
    print(f"퀴즈 생성 실패: {e}")

print("\n테스트 완료!")
