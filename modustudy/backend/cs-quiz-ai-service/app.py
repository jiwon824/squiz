"""
CS Word Quiz AI Service - Flask 애플리케이션 (옵션 C: 하이브리드 최적화)

Features:
- AI 기반 유사도 계산 (Sentence Transformer)
- CS 동의어 사전 매칭 (해시테이블 = 해시맵 = 딕셔너리)
- 부분 일치 보너스 (SQL인젝션 ↔ SQL 인젝션)
- 카테고리 기반 보너스
- 점수 스케일링
"""
import os
from flask import Flask
from flask_cors import CORS

from config import Config
from routes import health_bp, similarity_bp, word_bp, visualization_bp


def create_app() -> Flask:
    """Flask 애플리케이션 팩토리"""
    
    app = Flask(__name__)
    
    # CORS 설정
    CORS(app, origins=Config.CORS_ORIGINS)
    
    # Blueprint 등록
    app.register_blueprint(health_bp)
    app.register_blueprint(similarity_bp)
    app.register_blueprint(word_bp)
    app.register_blueprint(visualization_bp)
    
    return app


def main():
    """애플리케이션 메인 진입점"""
    
    print("=" * 60)
    print("🚀 CS Word Quiz AI Service v2.0 (Option C: Hybrid)")
    print("=" * 60)
    
    # 데이터 디렉토리 생성
    os.makedirs(Config.DATA_DIR, exist_ok=True)
    
    # 앱 생성
    app = create_app()
    
    print(f"📍 Server: http://{Config.HOST}:{Config.PORT}")
    print(f"🔧 Debug Mode: {Config.DEBUG}")
    print(f"🌐 CORS Origins: {Config.CORS_ORIGINS}")
    print(f"🤖 Model: {Config.MODEL_NAME}")
    print("=" * 60)
    print("✨ Features:")
    print("   - AI 기반 유사도 계산")
    print("   - CS 동의어 사전 (70개+ 용어)")
    print("   - 부분 일치 보너스")
    print("   - 카테고리 보너스")
    print("   - 점수 스케일링")
    print("=" * 60)
    
    # 서버 실행
    app.run(
        host=Config.HOST,
        port=Config.PORT,
        debug=Config.DEBUG
    )


if __name__ == '__main__':
    main()