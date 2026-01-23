"""
애플리케이션 설정
"""
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
    CORS_ORIGINS = os.getenv('CORS_ORIGINS', 'http://localhost:5173,http://localhost:3000').split(',')
    
    # 데이터 경로
    DATA_DIR = os.path.join(os.path.dirname(__file__), 'data')
    WORDS_FILE = os.path.join(DATA_DIR, 'words.json')
    
    # ============================================
    # 유사도 계산 설정 (옵션 A: 엄격한 동의어)
    # ============================================
    SIMILARITY_CONFIG = {
        # 점수 범위 설정
        'min_score': 0.0,            # 최소 점수 (스케일링 제거)
        'max_score': 100.0,          # 최대 점수
        
        # 보너스 설정
        'partial_match_bonus': 15.0,  # 부분 일치 보너스 (SQL인젝션 vs SQL 인젝션)
        'synonym_bonus': 25.0,        # 동의어 보너스 (해시테이블 vs 해시맵) - 현재 미사용
        'related_keyword_bonus': 20.0, # 관련 키워드 보너스 (LIFO vs 스택) - 정답은 아님!
        'category_bonus': 10.0,       # 같은 카테고리 보너스
        
        # 스케일링 설정 (AI 유사도 그대로 사용)
        'scale_factor': 1.0,          # 스케일링 없음
        'scale_offset': 0.0,          # 기본 점수 없음
    }
