#!/bin/bash
# AI 추론 서버 API 테스트
# 사용법: ./test-api.sh [host]

HOST=${1:-"http://localhost:8000"}

echo "=========================================="
echo "AI 추론 서버 API 테스트"
echo "Host: $HOST"
echo "=========================================="

# 1. 헬스체크
echo ""
echo "[1] 헬스체크"
echo "GET $HOST/health"
curl -s "$HOST/health" | python3 -m json.tool
echo ""

# 2. 요약 테스트
echo "[2] 요약 API 테스트"
echo "POST $HOST/api/summarize"
curl -s -X POST "$HOST/api/summarize" \
    -H "Content-Type: application/json" \
    -d '{
        "transcript": "김민수: 오늘은 Docker 기초에 대해 공부해봤어요. 컨테이너랑 이미지 개념이 중요하더라고요.\n이지은: 네, 저도 처음엔 헷갈렸는데 이제 이해됐어요. Dockerfile 작성법도 배웠죠.\n박준혁: 다음 시간에는 Docker Compose로 멀티 컨테이너 환경을 구성해보면 좋겠어요.",
        "max_tokens": 256,
        "temperature": 0.7
    }' | python3 -m json.tool
echo ""

# 3. 퀴즈 생성 테스트
echo "[3] 퀴즈 생성 API 테스트"
echo "POST $HOST/api/quiz"
curl -s -X POST "$HOST/api/quiz" \
    -H "Content-Type: application/json" \
    -d '{
        "summary": "Docker 기초 스터디. 주요 내용: 1) 컨테이너와 이미지 개념 학습 2) Dockerfile 작성법 이해 3) 다음 시간에 Docker Compose 학습 예정",
        "num_questions": 3,
        "max_tokens": 512,
        "temperature": 0.7
    }' | python3 -m json.tool
echo ""

echo "=========================================="
echo "테스트 완료!"
echo "=========================================="
echo ""
echo "STT 테스트 (음성 파일 필요):"
echo "  curl -X POST '$HOST/api/stt' -F 'file=@audio.wav'"
echo ""
echo "전체 파이프라인 테스트:"
echo "  curl -X POST '$HOST/api/process-meeting' -F 'file=@audio.wav'"
