"""
단어 퀴즈 데이터 관리 서비스
"""
import json
import os
import random
from typing import List, Optional
from config import Config
from models.word import Word


class WordService:
    """단어 퀴즈 데이터 관리 서비스"""
    
    _instance = None
    _words_data = None
    
    def __new__(cls):
        """싱글톤 패턴 구현"""
        if cls._instance is None:
            cls._instance = super(WordService, cls).__new__(cls)
        return cls._instance
    
    def __init__(self):
        """초기화"""
        if self._words_data is None:
            self.load_words()
    
    def load_words(self) -> dict:
        """단어 데이터 로딩"""
        if self._words_data is None:
            if not os.path.exists(Config.WORDS_FILE):
                print("⚠️  단어 데이터 파일이 없습니다. 빈 데이터로 시작합니다.")
                self._words_data = {"words": []}
            else:
                with open(Config.WORDS_FILE, 'r', encoding='utf-8') as f:
                    self._words_data = json.load(f)
                print(f"✅ 단어 데이터 로딩 완료: {len(self._words_data['words'])}개")
        return self._words_data
    
    def get_all_words(self) -> List[Word]:
        """모든 단어 조회"""
        data = self.load_words()
        return [Word.from_dict(w) for w in data['words']]
    
    def get_word_by_id(self, word_id: int) -> Optional[Word]:
        """ID로 단어 조회"""
        data = self.load_words()
        word_dict = next((w for w in data['words'] if w['id'] == word_id), None)
        
        if word_dict:
            return Word.from_dict(word_dict)
        return None
    
    def get_random_word(
        self, 
        difficulty: Optional[str] = None, 
        category: Optional[str] = None
    ) -> Optional[Word]:
        """
        랜덤 단어 조회
        
        Args:
            difficulty: 난이도 필터 (optional)
            category: 카테고리 필터 (optional)
            
        Returns:
            Optional[Word]: 랜덤 단어 또는 None
        """
        data = self.load_words()
        
        if not data['words']:
            return None
        
        filtered_words = data['words']
        
        # 난이도 필터링
        if difficulty:
            filtered_words = [w for w in filtered_words if w.get('difficulty') == difficulty]
        
        # 카테고리 필터링
        if category:
            filtered_words = [w for w in filtered_words if w.get('category') == category]
        
        if not filtered_words:
            return None
        
        word_dict = random.choice(filtered_words)
        return Word.from_dict(word_dict)
    
    def get_categories(self) -> List[str]:
        """카테고리 목록 조회"""
        data = self.load_words()
        categories = list(set(w['category'] for w in data['words']))
        return sorted(categories)
    
    def get_difficulties(self) -> List[str]:
        """난이도 목록 조회"""
        data = self.load_words()
        difficulties = list(set(w['difficulty'] for w in data['words']))
        
        # easy, medium, hard 순서로 정렬
        order = {'easy': 0, 'medium': 1, 'hard': 2}
        return sorted(difficulties, key=lambda x: order.get(x, 99))
    
    def get_words_by_category(self, category: str) -> List[Word]:
        """카테고리별 단어 조회"""
        data = self.load_words()
        filtered = [w for w in data['words'] if w.get('category') == category]
        return [Word.from_dict(w) for w in filtered]
    
    def get_words_by_difficulty(self, difficulty: str) -> List[Word]:
        """난이도별 단어 조회"""
        data = self.load_words()
        filtered = [w for w in data['words'] if w.get('difficulty') == difficulty]
        return [Word.from_dict(w) for w in filtered]
    
    @property
    def total_words(self) -> int:
        """전체 단어 개수"""
        data = self.load_words()
        return len(data['words'])
    
    def reload_words(self):
        """단어 데이터 재로딩"""
        self._words_data = None
        self.load_words()