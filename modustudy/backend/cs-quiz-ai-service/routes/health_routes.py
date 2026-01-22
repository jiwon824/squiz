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
        "version": "2.0.0",
        "status": "running",
        "features": [
            "AI 기반 유사도 계산 (Sentence Transformer)",
            "CS 동의어 사전 매칭",
            "부분 일치 보너스",
            "카테고리 기반 보너스",
            "점수 스케일링"
        ],
        "endpoints": {
            "health": "GET /health",
            "config": "GET /api/config",
            "similarity": "POST /api/similarity",
            "batch_similarity": "POST /api/batch-similarity",
            "words": {
                "random": "GET /api/words/random",
                "all": "GET /api/words",
                "by_id": "GET /api/words/<id>",
                "check_answer": "POST /api/words/<id>/answer"
            },
            "synonyms": "GET /api/synonyms/<word>",
            "visualization": {
                "embedding_3d": "POST /api/embedding-3d",
                "embedding_3d_batch": "POST /api/embedding-3d-batch",
                "embedding_sphere": "POST /api/embedding-sphere"
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
            "version": "2.0.0",
            "model": Config.MODEL_NAME,
            "model_status": model_status,
            "words_count": words_count,
            "optimization": "Option C (Hybrid)"
        })
    except Exception as e:
        return jsonify({
            "status": "error",
            "error": str(e)
        }), 500


@health_bp.route('/api/config', methods=['GET'])
def get_config():
    """현재 설정 반환"""
    return jsonify({
        "similarity_config": ai_service.get_config(),
        "model": ai_service.model_name,
        "version": "2.0.0"
    })