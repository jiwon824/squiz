"""
유사도 계산 관련 라우트
"""
from flask import Blueprint, request, jsonify
from services import AIService

similarity_bp = Blueprint('similarity', __name__, url_prefix='/api')

ai_service = AIService()


@similarity_bp.route('/similarity', methods=['POST'])
def check_similarity():
    """단어 유사도 계산 API"""
    try:
        data = request.json
        
        if not data:
            return jsonify({"error": "JSON 데이터가 필요합니다"}), 400
        
        user_word = data.get('userWord', '').strip()
        answer_word = data.get('answerWord', '').strip()
        
        if not user_word or not answer_word:
            return jsonify({"error": "userWord와 answerWord가 필요합니다"}), 400
        
        # 유사도 계산
        similarity, score, is_correct = ai_service.get_similarity_score(
            user_word, answer_word
        )
        
        return jsonify({
            "userWord": user_word,
            "answerWord": answer_word,
            "similarity": similarity,
            "score": score,
            "isCorrect": is_correct
        })
        
    except Exception as e:
        print(f"❌ 오류 발생: {str(e)}")
        return jsonify({"error": str(e)}), 500


@similarity_bp.route('/batch-similarity', methods=['POST'])
def batch_similarity():
    """여러 단어의 유사도를 한번에 계산"""
    try:
        data = request.json
        
        if not data:
            return jsonify({"error": "JSON 데이터가 필요합니다"}), 400
        
        user_words = data.get('userWords', [])
        answer_word = data.get('answerWord', '').strip()
        
        if not answer_word:
            return jsonify({"error": "answerWord가 필요합니다"}), 400
        
        if not isinstance(user_words, list):
            return jsonify({"error": "userWords는 배열이어야 합니다"}), 400
        
        # 배치 유사도 계산
        results_data = ai_service.calculate_batch_similarity(user_words, answer_word)
        
        results = [
            {
                "word": word,
                "similarity": similarity,
                "score": score,
                "isCorrect": is_correct
            }
            for word, similarity, score, is_correct in results_data
        ]
        
        return jsonify({
            "answerWord": answer_word,
            "results": results,
            "totalAttempts": len(results)
        })
        
    except Exception as e:
        print(f"❌ 오류 발생: {str(e)}")
        return jsonify({"error": str(e)}), 500
