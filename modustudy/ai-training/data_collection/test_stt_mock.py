#!/usr/bin/env python
"""
STT API Mock 테스트 스크립트
- 실제 AI 서버 없이 로컬에서 프론트엔드 연동 테스트용
- Flask로 간단한 Mock 서버 실행
"""
import json
import time
from flask import Flask, request, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)  # 프론트엔드 CORS 허용


@app.route('/health', methods=['GET'])
def health():
    """헬스체크"""
    return jsonify({
        'status': 'ok',
        'llm_loaded': True,
        'whisper_loaded': True,
        'whisper_model': 'mock-whisper-model'
    })


@app.route('/api/stt', methods=['POST'])
def stt():
    """STT Mock - 음성 파일을 받아서 Mock 텍스트 반환"""
    if 'file' not in request.files:
        return jsonify({'error': 'No file provided'}), 400

    file = request.files['file']
    file_size = len(file.read())
    file.seek(0)

    # 파일 크기에 따라 처리 시간 시뮬레이션 (1MB당 1초)
    delay = min(file_size / (1024 * 1024), 5)  # 최대 5초
    time.sleep(delay)

    # Mock 응답
    mock_text = f"""안녕하세요, 오늘 회의를 시작하겠습니다.
첫 번째 안건은 프로젝트 진행 상황 공유입니다.
현재 백엔드 API 개발이 80% 완료되었고, 프론트엔드는 60% 정도 진행되었습니다.
다음 주까지 통합 테스트를 완료할 예정입니다.
두 번째 안건은 코드 리뷰 일정입니다.
매주 수요일 오후 2시에 코드 리뷰를 진행하기로 했습니다.
오늘 회의는 여기까지입니다. 감사합니다."""

    return jsonify({
        'text': mock_text,
        'language': 'ko',
        'language_probability': 0.95,
        'duration': delay * 10,  # Mock duration
        'segments': [
            {'start': 0, 'end': 5, 'text': mock_text[:50]},
            {'start': 5, 'end': 10, 'text': mock_text[50:100]},
        ]
    })


@app.route('/api/summarize', methods=['POST'])
def summarize():
    """요약 Mock"""
    data = request.json
    transcript = data.get('transcript', '')

    # Mock 요약
    mock_summary = """이번 회의에서는 프로젝트 진행 상황과 코드 리뷰 일정에 대해 논의했습니다.
백엔드 API 개발이 80% 완료되었으며, 프론트엔드는 60% 진행 중입니다.
다음 주까지 통합 테스트 완료 예정이며, 매주 수요일 오후 2시에 코드 리뷰를 진행합니다."""

    return jsonify({
        'summary': mock_summary,
        'tokens_used': len(transcript.split()) * 2
    })


if __name__ == '__main__':
    print("=" * 50)
    print("STT Mock Server")
    print("http://localhost:8000")
    print("=" * 50)
    print("\nEndpoints:")
    print("  GET  /health  - 서버 상태")
    print("  POST /api/stt - 음성 -> 텍스트")
    print("  POST /api/summarize - 텍스트 -> 요약")
    print("\n프론트엔드에서 VITE_AI_SERVER_URL=http://localhost:8000 설정 후 테스트")
    print("=" * 50)

    app.run(host='0.0.0.0', port=8000, debug=True)
