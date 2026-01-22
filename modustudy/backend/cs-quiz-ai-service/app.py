from flask import Flask, jsonify
from flask_cors import CORS
import os
from config import Config

# 라우트 임포트
from routes import health_bp, similarity_bp, word_bp, visualization_bp


def create_app():
    """Flask 애플리케이션 팩토리"""
    app = Flask(__name__)
    
    # CORS 설정
    CORS(app, origins=Config.CORS_ORIGINS)
    
    # Blueprint 등록
    app.register_blueprint(health_bp)
    app.register_blueprint(similarity_bp)
    app.register_blueprint(word_bp)
    app.register_blueprint(visualization_bp)
    
    # 에러 핸들러
    @app.errorhandler(404)
    def not_found(error):
        return jsonify({"error": "엔드포인트를 찾을 수 없습니다"}), 404

    @app.errorhandler(500)
    def internal_error(error):
        return jsonify({"error": "서버 내부 오류가 발생했습니다"}), 500
    
    return app


if __name__ == '__main__':
    print("=" * 50)
    print("🚀 CS Word Quiz AI Service Starting...")
    print("=" * 50)
    
    # 데이터 디렉토리 생성
    os.makedirs(Config.DATA_DIR, exist_ok=True)
    
    # Flask 앱 생성
    app = create_app()
    
    print(f"📍 Server: http://{Config.HOST}:{Config.PORT}")
    print(f"🔧 Debug Mode: {Config.DEBUG}")
    print(f"🌐 CORS Origins: {Config.CORS_ORIGINS}")
    print("=" * 50)
    
    app.run(
        host=Config.HOST,
        port=Config.PORT,
        debug=Config.DEBUG
    )