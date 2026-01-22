"""
헬스 체크 관련 라우트
"""
from flask import Blueprint, jsonify
from services import AIService, WordService
from config import Config

health_bp = Blueprint('health', __name__)

ai_service = AIService()
word_service = WordService()


@health_bp.route('/')
def index():
    """루트 엔드포인트"""
    return jsonify({
        "service": "CS Word Quiz AI Service",
        "version": "1.0.0",
        "status": "running",
        "endpoints": {
            "health": "/health",
            "similarity": "/api/similarity",
            "batch_similarity": "/api/batch-similarity",
            "words": {
                "random": "/api/words/random",
                "all": "/api/words",
                "by_id": "/api/words/<id>"
            }
        }
    })


@health_bp.route('/health', methods=['GET'])
def health_check():
    """서버 상태 확인"""
    try:
        model_status = "loaded" if ai_service.is_loaded else "not_loaded"
        words_count = word_service.total_words
        
        return jsonify({
            "status": "healthy",
            "model": Config.MODEL_NAME,
            "model_status": model_status,
            "words_count": words_count
        })
    except Exception as e:
        return jsonify({
            "status": "error",
            "error": str(e)
        }), 500
