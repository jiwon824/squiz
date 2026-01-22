"""
데이터 모델 패키지
"""
from .word import Word, WordResponse, SimilarityRequest, SimilarityResponse

__all__ = [
    'Word',
    'WordResponse',
    'SimilarityRequest',
    'SimilarityResponse'
]
