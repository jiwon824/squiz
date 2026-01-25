"""
단어 퀴즈 관련 라우트 (옵션 C: 하이브리드 최적화)
"""
from flask import Blueprint, request, jsonify
from services import AIService, WordService

import datetime
import random

word_bp = Blueprint('words', __name__, url_prefix='/api')

ai_service = AIService()
word_service = WordService()


@word_bp.route('/words', methods=['GET'])
def get_all_words():
    """모든 단어 조회 (정답 제외)"""
    try:
        words = word_service.get_all_words()
        return jsonify({
            "words": [w.to_dict(include_answer=False) for w in words],
            "total": len(words)
        })
    except Exception as e:
        print(f"❌ 오류 발생: {str(e)}")
        return jsonify({"error": str(e)}), 500


@word_bp.route('/words/random', methods=['GET'])
def get_random_word():
    """
    랜덤 단어 조회
    
    Query Parameters:
    - difficulty: easy, medium, hard
    - category: 카테고리명
    """
    try:
        difficulty = request.args.get('difficulty')
        category = request.args.get('category')
        
        word = word_service.get_random_word(difficulty, category)
        
        if not word:
            return jsonify({"error": "조건에 맞는 단어가 없습니다"}), 404
        
        return jsonify(word.to_dict(include_answer=False))
        
    except Exception as e:
        print(f"❌ 오류 발생: {str(e)}")
        return jsonify({"error": str(e)}), 500


@word_bp.route('/words/<int:word_id>', methods=['GET'])
def get_word_by_id(word_id):
    """ID로 단어 조회 (정답 제외)"""
    try:
        word = word_service.get_word_by_id(word_id)
        
        if not word:
            return jsonify({"error": "단어를 찾을 수 없습니다"}), 404
        
        return jsonify(word.to_dict(include_answer=False))
        
    except Exception as e:
        print(f"❌ 오류 발생: {str(e)}")
        return jsonify({"error": str(e)}), 500


@word_bp.route('/words/<int:word_id>/answer', methods=['POST'])
def check_answer(word_id):
    """
    정답 확인 (옵션 C: 하이브리드 최적화 적용)
    
    Request Body:
    {
        "userWord": "사용자 입력 단어"
    }
    
    Response:
    {
        "wordId": 1,
        "userWord": "입력 단어",
        "answerWord": "정답",        // 정답일 때만
        "rawSimilarity": 0.58,
        "similarity": 0.75,
        "score": 75.0,
        "isCorrect": false,
        "bonuses": {
            "category": 10.0
        },
        "answer": null               // 정답이면 정답 표시
    }
    """
    try:
        word = word_service.get_word_by_id(word_id)
        
        if not word:
            return jsonify({"error": "단어를 찾을 수 없습니다"}), 404
        
        request_data = request.json
        user_word = request_data.get('userWord', '').strip()
        
        if not user_word:
            return jsonify({"error": "userWord가 필요합니다"}), 400
        
        # 유사도 계산 (옵션 C: 카테고리 포함)
        result = ai_service.calculate_similarity(
            user_word, 
            word.answer, 
            word.category  # 카테고리 전달!
        )
        
        response = {
            "wordId": word_id,
            **result.to_dict()
        }
        
        # 정답이면 answer 포함
        if result.is_correct:
            response["answer"] = word.answer
        else:
            response["answer"] = None
        
        return jsonify(response)
        
    except Exception as e:
        print(f"❌ 오류 발생: {str(e)}")
        return jsonify({"error": str(e)}), 500


@word_bp.route('/categories', methods=['GET'])
def get_categories():
    """카테고리 목록 조회"""
    try:
        categories = word_service.get_categories()
        
        return jsonify({
            "categories": categories,
            "total": len(categories)
        })
        
    except Exception as e:
        print(f"❌ 오류 발생: {str(e)}")
        return jsonify({"error": str(e)}), 500


@word_bp.route('/difficulties', methods=['GET'])
def get_difficulties():
    """난이도 목록 조회"""
    try:
        difficulties = word_service.get_difficulties()
        
        return jsonify({
            "difficulties": difficulties,
            "total": len(difficulties)
        })
        
    except Exception as e:
        print(f"❌ 오류 발생: {str(e)}")
        return jsonify({"error": str(e)}), 500
    
@word_bp.route('/words/daily', methods=['GET'])
def get_daily_word():
    """오늘의 문제 조회 (00시 기준 변경)"""
    try:
        words = word_service.get_all_words()
        
        if not words:
            return jsonify({"error": "단어 데이터가 없습니다"}), 404
        
        # 오늘 날짜를 seed로 사용
        today = datetime.date.today()
        seed = int(today.strftime("%Y%m%d"))  # 예: 20260122
        
        # seed 기반 랜덤 선택 (같은 날 = 같은 문제)
        random.seed(seed)
        daily_word = random.choice(words)
        
        # 응답 생성
        response = daily_word.to_dict(include_answer=False)
        response["date"] = today.isoformat()
        
        return jsonify(response)
        
    except Exception as e:
        print(f"❌ 오류 발생: {str(e)}")
        return jsonify({"error": str(e)}), 500
    
@word_bp.route('/words/yesterday', methods=['GET'])
def get_yesterday_word():
    """
    어제의 문제 및 정답 조회 (대시보드 위젯용)
    
    Response:
    {
        "id": 1,
        "answer": "스택",
        "category": "자료구조",
        "difficulty": "easy",
        "hints": [...],
        "date": "2026-01-21"
    }
    """
    try:
        words = word_service.get_all_words()
        
        if not words:
            return jsonify({"error": "단어 데이터가 없습니다"}), 404
        
        # 어제 날짜를 seed로 사용
        yesterday = datetime.date.today() - datetime.timedelta(days=1)
        seed = int(yesterday.strftime("%Y%m%d"))
        
        random.seed(seed)
        yesterday_word = random.choice(words)
        
        # 정답 포함해서 반환
        response = yesterday_word.to_dict(include_answer=True)
        response["date"] = yesterday.isoformat()
        
        return jsonify(response)
        
    except Exception as e:
        print(f"❌ 오류 발생: {str(e)}")
        return jsonify({"error": str(e)}), 500

# 리더보드 저장소 (메모리 - 서버 재시작 시 초기화)
# TODO: 나중에 DB로 변경
leaderboard_data = {}


@word_bp.route('/leaderboard', methods=['GET'])
def get_leaderboard():
    """
    리더보드 조회 (명예의 전당)
    
    Query Parameters:
    - date: 조회할 날짜 (YYYY-MM-DD, 기본값: 오늘)
    - limit: 조회 개수 (기본값: 10)
    
    Response:
    {
        "date": "2026-01-22",
        "rankings": [
            {"rank": 1, "nickname": "홍길동", "attempts": 3, "time": 45},
            {"rank": 2, "nickname": "김철수", "attempts": 5, "time": 120},
            ...
        ],
        "total": 10
    }
    """
    try:
        date = request.args.get('date', datetime.date.today().isoformat())
        limit = int(request.args.get('limit', 10))
        
        # 해당 날짜 리더보드 조회
        rankings = leaderboard_data.get(date, [])
        
        # 정렬: 시도 횟수 오름차순 → 시간 오름차순
        sorted_rankings = sorted(rankings, key=lambda x: (x['attempts'], x['time']))
        
        # 순위 부여
        result = []
        for i, entry in enumerate(sorted_rankings[:limit]):
            result.append({
                "rank": i + 1,
                "nickname": entry['nickname'],
                "attempts": entry['attempts'],
                "time": entry['time']
            })
        
        return jsonify({
            "date": date,
            "rankings": result,
            "total": len(result)
        })
        
    except Exception as e:
        print(f"❌ 오류 발생: {str(e)}")
        return jsonify({"error": str(e)}), 500


@word_bp.route('/leaderboard', methods=['POST'])
def save_leaderboard():
    """
    리더보드 저장
    
    Request Body:
    {
        "nickname": "홍길동",
        "attempts": 3,
        "time": 45
    }
    
    Response:
    {
        "success": true,
        "rank": 1,
        "date": "2026-01-22"
    }
    """
    try:
        data = request.json
        
        if not data:
            return jsonify({"error": "JSON 데이터가 필요합니다"}), 400
        
        nickname = data.get('nickname', '').strip()
        attempts = data.get('attempts')
        time_taken = data.get('time')
        
        if not nickname:
            return jsonify({"error": "nickname이 필요합니다"}), 400
        if attempts is None or time_taken is None:
            return jsonify({"error": "attempts와 time이 필요합니다"}), 400
        
        today = datetime.date.today().isoformat()
        
        # 오늘 날짜 리더보드 초기화
        if today not in leaderboard_data:
            leaderboard_data[today] = []
        
        # 새 기록 추가
        new_entry = {
            "nickname": nickname,
            "attempts": int(attempts),
            "time": int(time_taken)
        }
        leaderboard_data[today].append(new_entry)
        
        # 순위 계산
        sorted_rankings = sorted(
            leaderboard_data[today], 
            key=lambda x: (x['attempts'], x['time'])
        )
        rank = next(
            i + 1 for i, entry in enumerate(sorted_rankings)
            if entry['nickname'] == nickname and 
               entry['attempts'] == new_entry['attempts'] and
               entry['time'] == new_entry['time']
        )
        
        return jsonify({
            "success": True,
            "rank": rank,
            "date": today,
            "totalPlayers": len(leaderboard_data[today])
        })
        
    except Exception as e:
        print(f"❌ 오류 발생: {str(e)}")
        return jsonify({"error": str(e)}), 500