"""
AI 모델 및 유사도 계산 서비스
"""
from sentence_transformers import SentenceTransformer
from sklearn.metrics.pairwise import cosine_similarity
import numpy as np
from typing import List, Tuple
from config import Config


class AIService:
    """AI 모델 관리 및 유사도 계산 서비스"""
    
    _instance = None
    _model = None
    
    def __new__(cls):
        """싱글톤 패턴 구현"""
        if cls._instance is None:
            cls._instance = super(AIService, cls).__new__(cls)
        return cls._instance
    
    def __init__(self):
        """초기화"""
        if self._model is None:
            self.load_model()
    
    def load_model(self) -> SentenceTransformer:
        """Sentence Transformer 모델 로딩"""
        if self._model is None:
            print(f"🤖 모델 로딩 중: {Config.MODEL_NAME}")
            self._model = SentenceTransformer(Config.MODEL_NAME)
            print("✅ 모델 로딩 완료!")
        return self._model
    
    def get_model(self) -> SentenceTransformer:
        """모델 인스턴스 반환"""
        if self._model is None:
            self.load_model()
        return self._model
    
    def calculate_similarity(self, word1: str, word2: str) -> float:
        """
        두 단어 간의 코사인 유사도 계산
        
        Args:
            word1: 첫 번째 단어
            word2: 두 번째 단어
            
        Returns:
            float: 코사인 유사도 (0~1)
        """
        model = self.get_model()
        
        # 임베딩 생성
        embeddings = model.encode([word1, word2])
        
        # 코사인 유사도 계산
        similarity = cosine_similarity(
            embeddings[0].reshape(1, -1),
            embeddings[1].reshape(1, -1)
        )[0][0]
        
        return float(similarity)
    
    def calculate_batch_similarity(
        self, 
        user_words: List[str], 
        answer_word: str
    ) -> List[Tuple[str, float, float, bool]]:
        """
        여러 단어의 유사도를 한번에 계산
        
        Args:
            user_words: 사용자가 입력한 단어 리스트
            answer_word: 정답 단어
            
        Returns:
            List[Tuple[str, float, float, bool]]: (단어, 유사도, 점수, 정답여부) 리스트
        """
        results = []
        
        for word in user_words:
            word = word.strip()
            if word:
                similarity = self.calculate_similarity(word, answer_word)
                score = round(similarity * 100, 2)
                is_correct = word == answer_word
                results.append((word, similarity, score, is_correct))
        
        # 유사도 순으로 정렬
        results.sort(key=lambda x: x[1], reverse=True)
        
        return results
    
    def get_similarity_score(self, word1: str, word2: str) -> Tuple[float, float, bool]:
        """
        유사도와 점수, 정답 여부를 함께 반환
        
        Args:
            word1: 사용자 입력 단어
            word2: 정답 단어
            
        Returns:
            Tuple[float, float, bool]: (유사도, 점수, 정답여부)
        """
        similarity = self.calculate_similarity(word1, word2)
        score = round(similarity * 100, 2)
        is_correct = word1 == word2
        
        return similarity, score, is_correct
    
    @property
    def model_name(self) -> str:
        """모델 이름 반환"""
        return Config.MODEL_NAME
    
    @property
    def is_loaded(self) -> bool:
        """모델 로딩 상태 확인"""
        return self._model is not None
