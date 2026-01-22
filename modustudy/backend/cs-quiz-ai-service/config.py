import os
from dotenv import load_dotenv

load_dotenv()

class Config:
    """애플리케이션 설정"""
    
    # Flask 설정
    DEBUG = os.getenv('DEBUG', 'True').lower() == 'true'
    HOST = os.getenv('HOST', '0.0.0.0')
    PORT = int(os.getenv('PORT', 5000))
    
    # 모델 설정
    MODEL_NAME = os.getenv('MODEL_NAME', 'jhgan/ko-sroberta-multitask')
    
    # CORS 설정
    CORS_ORIGINS = os.getenv('CORS_ORIGINS', 'http://localhost:5173').split(',')
    
    # 데이터 경로
    DATA_DIR = os.path.join(os.path.dirname(__file__), 'data')
    WORDS_FILE = os.path.join(DATA_DIR, 'words.json')