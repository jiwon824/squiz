"""
단어 퀴즈 관련 라우트 (옵션 C: 하이브리드 최적화)
"""
from flask import Blueprint, request, jsonify
from services import AIService, WordService
from config import Config

import datetime
import random
import pymysql

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

def get_db_connection():
    """MySQL 연결 생성"""
    return pymysql.connect(
        host=Config.DB_HOST,
        port=Config.DB_PORT,
        user=Config.DB_USERNAME,
        password=Config.DB_PASSWORD,
        database=Config.DB_NAME,
        charset='utf8mb4',
        cursorclass=pymysql.cursors.DictCursor
    )


@word_bp.route('/leaderboard', methods=['GET'])
def get_leaderboard():
    """
    리더보드 조회 (명예의 전당) - MySQL 기반

    Query Parameters:
    - date: 조회할 날짜 (YYYY-MM-DD, 기본값: 오늘)
    - limit: 조회 개수 (기본값: 10)
    """
    try:
        date = request.args.get('date', datetime.date.today().isoformat())
        limit = int(request.args.get('limit', 10))

        conn = get_db_connection()
        try:
            with conn.cursor() as cursor:
                # comendle_attempt + comendle_daily + user 조인하여 리더보드 조회
                # 정렬: 시도 횟수 오름차순 → 소요시간 오름차순
                cursor.execute("""
                    SELECT
                        u.nickname,
                        ca.guess_count AS attempts,
                        TIMESTAMPDIFF(SECOND, ca.created_at, ca.solved_at) AS time
                    FROM comendle_attempt ca
                    JOIN comendle_daily cd ON ca.daily_id = cd.id
                    JOIN user u ON ca.user_id = u.id
                    WHERE cd.game_date = %s
                      AND ca.is_solved = 1
                      AND ca.user_id IS NOT NULL
                    ORDER BY ca.guess_count ASC, TIMESTAMPDIFF(SECOND, ca.created_at, ca.solved_at) ASC
                    LIMIT %s
                """, (date, limit))
                rows = cursor.fetchall()
        finally:
            conn.close()

        result = []
        for i, row in enumerate(rows):
            result.append({
                "rank": i + 1,
                "nickname": row['nickname'],
                "attempts": row['attempts'],
                "time": row['time'] if row['time'] is not None else 0
            })

        return jsonify({
            "date": date,
            "rankings": result,
            "total": len(result)
        })

    except Exception as e:
        print(f"❌ 리더보드 조회 오류: {str(e)}")
        return jsonify({"error": str(e)}), 500


@word_bp.route('/leaderboard', methods=['POST'])
def save_leaderboard():
    """
    리더보드 저장 - MySQL 기반
    comendle_attempt 테이블에 풀이 기록 저장

    Request Body:
    {
        "nickname": "홍길동",
        "attempts": 3,
        "time": 45
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

        conn = get_db_connection()
        try:
            with conn.cursor() as cursor:
                # 사용자 ID 조회
                cursor.execute("SELECT id FROM user WHERE nickname = %s", (nickname,))
                user_row = cursor.fetchone()
                if not user_row:
                    return jsonify({"error": "존재하지 않는 사용자입니다"}), 404
                user_id = user_row['id']

                # 오늘 날짜의 daily_id 조회
                cursor.execute("SELECT id FROM comendle_daily WHERE game_date = %s", (today,))
                daily_row = cursor.fetchone()
                if not daily_row:
                    return jsonify({"error": "오늘의 문제가 없습니다"}), 404
                daily_id = daily_row['id']

                # 이미 풀었는지 확인 (중복 방지)
                cursor.execute("""
                    SELECT id FROM comendle_attempt
                    WHERE daily_id = %s AND user_id = %s AND is_solved = 1
                """, (daily_id, user_id))
                existing = cursor.fetchone()

                if existing:
                    # 이미 풀었으면 기존 기록 유지
                    pass
                else:
                    # 새 기록 저장
                    now = datetime.datetime.now()
                    solved_at = now
                    created_at = now - datetime.timedelta(seconds=int(time_taken))

                    cursor.execute("""
                        INSERT INTO comendle_attempt
                            (daily_id, user_id, is_solved, guess_count, solved_at, created_at)
                        VALUES (%s, %s, 1, %s, %s, %s)
                    """, (daily_id, user_id, int(attempts), solved_at, created_at))

                conn.commit()

                # 순위 계산
                cursor.execute("""
                    SELECT COUNT(*) + 1 AS `rank`
                    FROM comendle_attempt ca
                    JOIN comendle_daily cd ON ca.daily_id = cd.id
                    WHERE cd.game_date = %s
                      AND ca.is_solved = 1
                      AND ca.user_id IS NOT NULL
                      AND (ca.guess_count < %s
                           OR (ca.guess_count = %s
                               AND TIMESTAMPDIFF(SECOND, ca.created_at, ca.solved_at) < %s))
                """, (today, int(attempts), int(attempts), int(time_taken)))
                rank_row = cursor.fetchone()
                rank = rank_row['rank'] if rank_row else 1

                # 총 참가자 수
                cursor.execute("""
                    SELECT COUNT(*) AS total
                    FROM comendle_attempt ca
                    JOIN comendle_daily cd ON ca.daily_id = cd.id
                    WHERE cd.game_date = %s AND ca.is_solved = 1 AND ca.user_id IS NOT NULL
                """, (today,))
                total_row = cursor.fetchone()
                total = total_row['total'] if total_row else 0
        finally:
            conn.close()

        return jsonify({
            "success": True,
            "rank": rank,
            "date": today,
            "totalPlayers": total
        })

    except Exception as e:
        print(f"❌ 리더보드 저장 오류: {str(e)}")
        return jsonify({"error": str(e)}), 500