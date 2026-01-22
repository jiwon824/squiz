"""
단어 퀴즈 관련 데이터 모델
"""
from typing import List, Optional
from dataclasses import dataclass


@dataclass
class Word:
    """단어 퀴즈 데이터 모델"""
    id: int
    answer: str
    category: str
    difficulty: str
    hints: List[str]

    def to_dict(self, include_answer: bool = False) -> dict:
        """딕셔너리로 변환"""
        result = {
            "id": self.id,
            "category": self.category,
            "difficulty": self.difficulty,
            "hints": self.hints
        }
        if include_answer:
            result["answer"] = self.answer
        return result

    @classmethod
    def from_dict(cls, data: dict) -> 'Word':
        """딕셔너리에서 생성"""
        return cls(
            id=data['id'],
            answer=data['answer'],
            category=data['category'],
            difficulty=data['difficulty'],
            hints=data['hints']
        )


@dataclass
class WordResponse:
    """단어 조회 응답 모델"""
    id: int
    category: str
    difficulty: str
    hints: List[str]


@dataclass
class SimilarityRequest:
    """유사도 계산 요청 모델"""
    user_word: str
    answer_word: str


@dataclass
class SimilarityResponse:
    """유사도 계산 응답 모델"""
    user_word: str
    answer_word: str
    similarity: float
    score: float
    is_correct: bool
