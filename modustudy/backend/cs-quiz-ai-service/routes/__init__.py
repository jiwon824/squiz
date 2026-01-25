"""
API 라우트 패키지
"""
from .health_routes import health_bp
from .similarity_routes import similarity_bp
from .word_routes import word_bp
from .visualization_routes import visualization_bp

__all__ = [
    'health_bp',
    'similarity_bp',
    'word_bp',
    'visualization_bp'
]
