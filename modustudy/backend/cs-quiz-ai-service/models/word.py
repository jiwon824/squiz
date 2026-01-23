"""
단어 퀴즈 관련 데이터 모델
"""
from typing import List, Optional, Dict, Any
from dataclasses import dataclass, field


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
class SimilarityResult:
    """유사도 계산 결과 데이터 클래스 (최적화)"""
    user_word: str
    answer_word: str
    raw_similarity: float       # 원본 AI 유사도
    final_similarity: float     # 보정된 최종 유사도
    score: float                # 0-100 점수
    is_correct: bool            # 정답 여부
    bonuses: Dict[str, float] = field(default_factory=dict)  # 적용된 보너스 내역
    
    def to_dict(self) -> Dict[str, Any]:
        """딕셔너리로 변환 (API 응답용)"""
        return {
            "userWord": self.user_word,
            "answerWord": self.answer_word,
            "rawSimilarity": round(self.raw_similarity, 4),
            "similarity": round(self.final_similarity, 4),
            "score": self.score,
            "isCorrect": self.is_correct,
            "bonuses": self.bonuses
        }


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
    category: Optional[str] = None