"""
단어 퀴즈 관련 라우트
"""
from flask import Blueprint, request, jsonify
from services import AIService, WordService

word_bp = Blueprint('words', __name__, url_prefix='/api')

ai_service = AIService()
word_service = WordService()


@word_bp.route('/words', methods=['GET'])
def get_all_words():
    """모든 단어 조회"""
    try:
        words = word_service.get_all_words()
        return jsonify({
            "words": [w.to_dict() for w in words],
            "total": len(words)
        })
    except Exception as e:
        print(f"❌ 오류 발생: {str(e)}")
        return jsonify({"error": str(e)}), 500


@word_bp.route('/words/random', methods=['GET'])
def get_random_word():
    """랜덤 단어 조회"""
    try:
        # 쿼리 파라미터로 난이도 필터링 가능
        difficulty = request.args.get('difficulty')
        category = request.args.get('category')
        
        word = word_service.get_random_word(difficulty, category)
        
        if not word:
            return jsonify({"error": "조건에 맞는 단어가 없습니다"}), 404
        
        # 정답은 제외하고 반환
        return jsonify(word.to_dict(include_answer=False))
        
    except Exception as e:
        print(f"❌ 오류 발생: {str(e)}")
        return jsonify({"error": str(e)}), 500


@word_bp.route('/words/<int:word_id>', methods=['GET'])
def get_word_by_id(word_id):
    """ID로 단어 조회"""
    try:
        word = word_service.get_word_by_id(word_id)
        
        if not word:
            return jsonify({"error": "단어를 찾을 수 없습니다"}), 404
        
        # 정답은 제외하고 반환
        return jsonify(word.to_dict(include_answer=False))
        
    except Exception as e:
        print(f"❌ 오류 발생: {str(e)}")
        return jsonify({"error": str(e)}), 500


@word_bp.route('/words/<int:word_id>/answer', methods=['POST'])
def check_answer(word_id):
    """정답 확인 (ID 기반)"""
    try:
        word = word_service.get_word_by_id(word_id)
        
        if not word:
            return jsonify({"error": "단어를 찾을 수 없습니다"}), 404
        
        request_data = request.json
        user_word = request_data.get('userWord', '').strip()
        
        if not user_word:
            return jsonify({"error": "userWord가 필요합니다"}), 400
        
        # 유사도 계산
        similarity, score, is_correct = ai_service.get_similarity_score(
            user_word, word.answer
        )
        
        return jsonify({
            "wordId": word_id,
            "userWord": user_word,
            "similarity": similarity,
            "score": score,
            "isCorrect": is_correct,
            "answer": word.answer if is_correct else None  # 정답일 때만 반환
        })
        
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
