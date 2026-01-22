"""
AI 모델 및 유사도 계산 서비스 (옵션 C: 하이브리드 최적화)

- 기본 AI 유사도 + 동의어 사전 + 부분 일치 + 카테고리 보너스 + 점수 스케일링
"""
from sentence_transformers import SentenceTransformer
from sklearn.metrics.pairwise import cosine_similarity
import numpy as np
from typing import List, Tuple, Optional, Dict, Any

from config import Config
from models.word import SimilarityResult
from synonyms import (
    are_synonyms, 
    normalize_word, 
    get_synonyms,
    is_category_related,
    is_related_keyword
)


class AIService:
    """AI 모델 관리 및 유사도 계산 서비스 (옵션 C: 하이브리드 최적화)"""
    
    _instance: Optional['AIService'] = None
    _model: Optional[SentenceTransformer] = None
    _initialized: bool = False
    
    def __new__(cls) -> 'AIService':
        """싱글톤 패턴 구현"""
        if cls._instance is None:
            cls._instance = super(AIService, cls).__new__(cls)
        return cls._instance
    
    def __init__(self):
        """초기화 (한 번만 실행)"""
        if not AIService._initialized:
            self._config = Config.SIMILARITY_CONFIG
            AIService._initialized = True
    
    def load_model(self) -> SentenceTransformer:
        """Sentence Transformer 모델 로딩"""
        if AIService._model is None:
            print(f"🤖 모델 로딩 중: {Config.MODEL_NAME}")
            try:
                AIService._model = SentenceTransformer(Config.MODEL_NAME)
                print("✅ 모델 로딩 완료!")
            except Exception as e:
                print(f"❌ 모델 로딩 실패: {str(e)}")
                raise RuntimeError(f"모델 로딩 실패: {str(e)}")
        return AIService._model
    
    def get_model(self) -> SentenceTransformer:
        """모델 인스턴스 반환"""
        if AIService._model is None:
            self.load_model()
        return AIService._model
    
    # ============================================
    # 핵심 유사도 계산 메서드
    # ============================================
    
    def _calculate_raw_similarity(self, word1: str, word2: str) -> float:
        """
        AI 모델을 사용한 원본 코사인 유사도 계산
        
        Args:
            word1: 첫 번째 단어
            word2: 두 번째 단어
            
        Returns:
            float: 코사인 유사도 (0~1)
        """
        if not word1 or not word2:
            return 0.0
            
        model = self.get_model()
        
        # 임베딩 생성
        embeddings = model.encode([word1, word2])
        
        # 코사인 유사도 계산
        similarity = cosine_similarity(
            embeddings[0].reshape(1, -1),
            embeddings[1].reshape(1, -1)
        )[0][0]
        
        return float(max(0, similarity))  # 음수 방지
    
    # ============================================
    # 보너스 계산 메서드 (옵션 C)
    # ============================================
    
    def _calculate_partial_match_bonus(self, word1: str, word2: str) -> float:
        """
        부분 일치 보너스 계산
        
        예: "SQL인젝션" vs "SQL 인젝션", "해시테이블" vs "해시 테이블"
        
        Args:
            word1: 사용자 입력 단어
            word2: 정답 단어
            
        Returns:
            float: 부분 일치 보너스 (0 ~ partial_match_bonus)
        """
        w1 = normalize_word(word1)
        w2 = normalize_word(word2)
        
        if not w1 or not w2:
            return 0.0
        
        # 정규화 후 완전 일치
        if w1 == w2:
            return self._config['partial_match_bonus']
        
        # 한쪽이 다른 쪽을 포함
        if w1 in w2 or w2 in w1:
            ratio = min(len(w1), len(w2)) / max(len(w1), len(w2))
            return self._config['partial_match_bonus'] * ratio * 0.7
        
        # 공통 부분 문자열 체크 (길이 3 이상)
        common_length = self._longest_common_substring_length(w1, w2)
        if common_length >= 3:
            ratio = common_length / max(len(w1), len(w2))
            return self._config['partial_match_bonus'] * ratio * 0.5
        
        return 0.0
    
    def _longest_common_substring_length(self, s1: str, s2: str) -> int:
        """두 문자열의 최장 공통 부분 문자열 길이 (DP)"""
        if not s1 or not s2:
            return 0
            
        m, n = len(s1), len(s2)
        dp = [[0] * (n + 1) for _ in range(m + 1)]
        max_length = 0
        
        for i in range(1, m + 1):
            for j in range(1, n + 1):
                if s1[i-1] == s2[j-1]:
                    dp[i][j] = dp[i-1][j-1] + 1
                    max_length = max(max_length, dp[i][j])
        
        return max_length
    
    def _calculate_synonym_bonus(self, word1: str, word2: str) -> float:
        """
        동의어 보너스 계산
        
        Args:
            word1: 사용자 입력 단어
            word2: 정답 단어
            
        Returns:
            float: 동의어 보너스
        """
        if are_synonyms(word1, word2):
            return self._config['synonym_bonus']
        return 0.0
    
    def _calculate_related_keyword_bonus(self, word1: str, word2: str) -> float:
        """
        관련 키워드 보너스 계산 (옵션 A)
        
        LIFO → 스택: 정답은 아니지만 높은 보너스
        
        Args:
            word1: 사용자 입력 단어
            word2: 정답 단어
            
        Returns:
            float: 관련 키워드 보너스
        """
        if is_related_keyword(word1, word2):
            return self._config['related_keyword_bonus']
        return 0.0
    
    def _calculate_category_bonus(
        self, 
        word1: str, 
        word2: str, 
        category: Optional[str] = None
    ) -> float:
        """
        카테고리 기반 보너스 계산
        
        같은 카테고리의 단어면 보너스 부여
        
        Args:
            word1: 사용자 입력 단어
            word2: 정답 단어
            category: 정답 단어의 카테고리
            
        Returns:
            float: 카테고리 보너스
        """
        if not category:
            return 0.0
        
        if is_category_related(word1, category):
            return self._config['category_bonus']
        
        return 0.0
    
    def _scale_score(self, raw_score: float) -> float:
        """
        점수 스케일링 (너무 낮은 점수 방지, 분포 조정)
        
        Args:
            raw_score: 원본 점수 (0-100)
            
        Returns:
            float: 스케일링된 점수
        """
        min_score = self._config['min_score']
        max_score = self._config['max_score']
        scale_factor = self._config['scale_factor']
        scale_offset = self._config['scale_offset']
        
        # 기본 오프셋 + 스케일링 적용
        # 점수가 0이 아닌 경우에만 스케일링
        if raw_score > 0:
            scaled = scale_offset + (raw_score * scale_factor)
        else:
            scaled = 0
        
        # 최소/최대 범위 제한
        if scaled > 0 and scaled < min_score:
            scaled = min_score
        
        return min(scaled, max_score)
    
    # ============================================
    # 메인 유사도 계산 API
    # ============================================
    
    def calculate_similarity(
        self, 
        user_word: str, 
        answer_word: str,
        category: Optional[str] = None
    ) -> SimilarityResult:
        """
        두 단어 간의 유사도 계산 (옵션 C: 하이브리드)
        
        Args:
            user_word: 사용자 입력 단어
            answer_word: 정답 단어
            category: 정답 단어의 카테고리 (선택)
            
        Returns:
            SimilarityResult: 유사도 계산 결과
        """
        # 입력 검증
        user_word = user_word.strip() if user_word else ""
        answer_word = answer_word.strip() if answer_word else ""
        
        if not user_word or not answer_word:
            return SimilarityResult(
                user_word=user_word,
                answer_word=answer_word,
                raw_similarity=0.0,
                final_similarity=0.0,
                score=0.0,
                is_correct=False,
                bonuses={}
            )
        
        # 1️⃣ 정답 체크 (정규화 후 비교)
        is_correct = normalize_word(user_word) == normalize_word(answer_word)
        
        if is_correct:
            return SimilarityResult(
                user_word=user_word,
                answer_word=answer_word,
                raw_similarity=1.0,
                final_similarity=1.0,
                score=100.0,
                is_correct=True,
                bonuses={"exact_match": 100.0}
            )
        
        # 2️⃣ 동의어 체크 (동의어면 정답 처리)
        if are_synonyms(user_word, answer_word):
            return SimilarityResult(
                user_word=user_word,
                answer_word=answer_word,
                raw_similarity=1.0,
                final_similarity=1.0,
                score=100.0,
                is_correct=True,  # 동의어는 정답 처리!
                bonuses={"synonym_match": 100.0}
            )
        
        # 3️⃣ AI 모델로 원본 유사도 계산
        raw_similarity = self._calculate_raw_similarity(user_word, answer_word)
        
        # 4️⃣ 보너스 계산
        bonuses = {}
        total_bonus = 0.0
        
        # 부분 일치 보너스
        partial_bonus = self._calculate_partial_match_bonus(user_word, answer_word)
        if partial_bonus > 0:
            bonuses["partial_match"] = round(partial_bonus, 2)
            total_bonus += partial_bonus
        
        # 관련 키워드 보너스 (LIFO → 스택 등)
        related_bonus = self._calculate_related_keyword_bonus(user_word, answer_word)
        if related_bonus > 0:
            bonuses["related_keyword"] = round(related_bonus, 2)
            total_bonus += related_bonus
        
        # 카테고리 보너스
        category_bonus = self._calculate_category_bonus(user_word, answer_word, category)
        if category_bonus > 0:
            bonuses["category"] = round(category_bonus, 2)
            total_bonus += category_bonus
        
        # 5️⃣ 최종 점수 계산
        base_score = raw_similarity * 100
        raw_final_score = base_score + total_bonus
        
        # 점수 스케일링
        final_score = self._scale_score(raw_final_score)
        final_score = round(final_score, 2)
        
        # 최종 유사도 (0-1 범위, 정답 아니면 최대 0.99)
        final_similarity = min(final_score / 100, 0.99)
        
        return SimilarityResult(
            user_word=user_word,
            answer_word=answer_word,
            raw_similarity=raw_similarity,
            final_similarity=final_similarity,
            score=final_score,
            is_correct=False,
            bonuses=bonuses
        )
    
    def calculate_batch_similarity(
        self,
        user_words: List[str],
        answer_word: str,
        category: Optional[str] = None
    ) -> List[SimilarityResult]:
        """
        여러 단어의 유사도를 한번에 계산 (배치 최적화)
        
        Args:
            user_words: 사용자가 입력한 단어 리스트
            answer_word: 정답 단어
            category: 정답 단어의 카테고리
            
        Returns:
            List[SimilarityResult]: 유사도 계산 결과 리스트 (점수 순 정렬)
        """
        if not user_words or not answer_word:
            return []
        
        # 중복 제거 및 정리
        unique_words = list(dict.fromkeys(w.strip() for w in user_words if w.strip()))
        
        if not unique_words:
            return []
        
        # 배치 임베딩 생성 (최적화: 한 번에 encode)
        model = self.get_model()
        all_words = unique_words + [answer_word]
        embeddings = model.encode(all_words)
        
        answer_embedding = embeddings[-1].reshape(1, -1)
        
        results = []
        for i, word in enumerate(unique_words):
            # 정답/동의어 체크
            is_exact_match = normalize_word(word) == normalize_word(answer_word)
            is_synonym = are_synonyms(word, answer_word)
            
            if is_exact_match or is_synonym:
                results.append(SimilarityResult(
                    user_word=word,
                    answer_word=answer_word,
                    raw_similarity=1.0,
                    final_similarity=1.0,
                    score=100.0,
                    is_correct=True,
                    bonuses={"exact_match" if is_exact_match else "synonym_match": 100.0}
                ))
                continue
            
            # AI 유사도 계산 (이미 계산된 임베딩 사용)
            word_embedding = embeddings[i].reshape(1, -1)
            raw_similarity = float(cosine_similarity(word_embedding, answer_embedding)[0][0])
            raw_similarity = max(0, raw_similarity)
            
            # 보너스 계산
            bonuses = {}
            total_bonus = 0.0
            
            partial_bonus = self._calculate_partial_match_bonus(word, answer_word)
            if partial_bonus > 0:
                bonuses["partial_match"] = round(partial_bonus, 2)
                total_bonus += partial_bonus
            
            # 관련 키워드 보너스
            related_bonus = self._calculate_related_keyword_bonus(word, answer_word)
            if related_bonus > 0:
                bonuses["related_keyword"] = round(related_bonus, 2)
                total_bonus += related_bonus
            
            category_bonus = self._calculate_category_bonus(word, answer_word, category)
            if category_bonus > 0:
                bonuses["category"] = round(category_bonus, 2)
                total_bonus += category_bonus
            
            # 최종 점수 계산
            base_score = raw_similarity * 100
            final_score = self._scale_score(base_score + total_bonus)
            final_score = round(final_score, 2)
            final_similarity = min(final_score / 100, 0.99)
            
            results.append(SimilarityResult(
                user_word=word,
                answer_word=answer_word,
                raw_similarity=raw_similarity,
                final_similarity=final_similarity,
                score=final_score,
                is_correct=False,
                bonuses=bonuses
            ))
        
        # 점수 순으로 정렬
        results.sort(key=lambda x: x.score, reverse=True)
        
        return results
    
    # ============================================
    # 레거시 호환 메서드 (기존 코드 호환용)
    # ============================================
    
    def get_similarity_score(
        self, 
        word1: str, 
        word2: str,
        category: Optional[str] = None
    ) -> Tuple[float, float, bool]:
        """
        유사도와 점수, 정답 여부를 함께 반환 (레거시 호환)
        
        Args:
            word1: 사용자 입력 단어
            word2: 정답 단어
            category: 카테고리 (선택)
            
        Returns:
            Tuple[float, float, bool]: (유사도, 점수, 정답여부)
        """
        result = self.calculate_similarity(word1, word2, category)
        return result.final_similarity, result.score, result.is_correct
    
    # ============================================
    # 속성
    # ============================================
    
    @property
    def model_name(self) -> str:
        """모델 이름 반환"""
        return Config.MODEL_NAME
    
    @property
    def is_loaded(self) -> bool:
        """모델 로딩 상태 확인"""
        return AIService._model is not None
    
    def get_config(self) -> Dict[str, Any]:
        """현재 설정 반환"""
        return self._config.copy()