"""
유사도 계산 관련 라우트 (옵션 C: 하이브리드 최적화)
"""
from flask import Blueprint, request, jsonify
from services import AIService
from synonyms import get_synonyms, are_synonyms

similarity_bp = Blueprint('similarity', __name__, url_prefix='/api')

ai_service = AIService()


@similarity_bp.route('/similarity', methods=['POST'])
def check_similarity():
    """
    단어 유사도 계산 API (옵션 C: 하이브리드)
    
    Request Body:
    {
        "userWord": "사용자 입력 단어",
        "answerWord": "정답 단어",
        "category": "카테고리 (선택)"
    }
    
    Response:
    {
        "userWord": "입력 단어",
        "answerWord": "정답 단어",
        "rawSimilarity": 0.58,      // 원본 AI 유사도
        "similarity": 0.75,         // 보정된 최종 유사도
        "score": 75.0,              // 최종 점수
        "isCorrect": false,
        "bonuses": {                // 적용된 보너스 내역
            "partial_match": 5.0,
            "category": 10.0
        }
    }
    """
    try:
        data = request.json
        
        if not data:
            return jsonify({"error": "JSON 데이터가 필요합니다"}), 400
        
        user_word = data.get('userWord', '').strip()
        answer_word = data.get('answerWord', '').strip()
        category = data.get('category')  # 선택적
        
        if not user_word or not answer_word:
            return jsonify({"error": "userWord와 answerWord가 필요합니다"}), 400
        
        # 유사도 계산 (옵션 C 적용)
        result = ai_service.calculate_similarity(user_word, answer_word, category)
        
        return jsonify(result.to_dict())
        
    except Exception as e:
        print(f"❌ 오류 발생: {str(e)}")
        return jsonify({"error": str(e)}), 500


@similarity_bp.route('/batch-similarity', methods=['POST'])
def batch_similarity():
    """
    여러 단어의 유사도를 한번에 계산 (배치 최적화)
    
    Request Body:
    {
        "userWords": ["단어1", "단어2", ...],
        "answerWord": "정답 단어",
        "category": "카테고리 (선택)"
    }
    
    Response:
    {
        "answerWord": "정답 단어",
        "category": "카테고리",
        "results": [
            {
                "userWord": "단어1",
                "rawSimilarity": 0.8,
                "similarity": 0.85,
                "score": 85.0,
                "isCorrect": false,
                "bonuses": {...}
            },
            ...
        ],
        "totalAttempts": 5
    }
    """
    try:
        data = request.json
        
        if not data:
            return jsonify({"error": "JSON 데이터가 필요합니다"}), 400
        
        user_words = data.get('userWords', [])
        answer_word = data.get('answerWord', '').strip()
        category = data.get('category')  # 선택적
        
        if not answer_word:
            return jsonify({"error": "answerWord가 필요합니다"}), 400
        
        if not isinstance(user_words, list):
            return jsonify({"error": "userWords는 배열이어야 합니다"}), 400
        
        # 배치 유사도 계산 (최적화)
        results = ai_service.calculate_batch_similarity(user_words, answer_word, category)
        
        return jsonify({
            "answerWord": answer_word,
            "category": category,
            "results": [r.to_dict() for r in results],
            "totalAttempts": len(results)
        })
        
    except Exception as e:
        print(f"❌ 오류 발생: {str(e)}")
        return jsonify({"error": str(e)}), 500


@similarity_bp.route('/synonyms/<word>', methods=['GET'])
def get_word_synonyms(word: str):
    """
    단어의 동의어 조회
    
    Response:
    {
        "word": "해시테이블",
        "synonyms": ["해시맵", "딕셔너리", ...],
        "total": 5
    }
    """
    try:
        synonyms = get_synonyms(word)
        
        return jsonify({
            "word": word,
            "synonyms": list(synonyms),
            "total": len(synonyms)
        })
        
    except Exception as e:
        print(f"❌ 오류 발생: {str(e)}")
        return jsonify({"error": str(e)}), 500


@similarity_bp.route('/check-synonym', methods=['POST'])
def check_synonym():
    """
    두 단어가 동의어인지 확인
    
    Request Body:
    {
        "word1": "해시테이블",
        "word2": "해시맵"
    }
    
    Response:
    {
        "word1": "해시테이블",
        "word2": "해시맵",
        "areSynonyms": true
    }
    """
    try:
        data = request.json
        
        if not data:
            return jsonify({"error": "JSON 데이터가 필요합니다"}), 400
        
        word1 = data.get('word1', '').strip()
        word2 = data.get('word2', '').strip()
        
        if not word1 or not word2:
            return jsonify({"error": "word1과 word2가 필요합니다"}), 400
        
        result = are_synonyms(word1, word2)
        
        return jsonify({
            "word1": word1,
            "word2": word2,
            "areSynonyms": result
        })
        
    except Exception as e:
        print(f"❌ 오류 발생: {str(e)}")
        return jsonify({"error": str(e)}), 500