"""
AI Service 모듈 테스트 스크립트
"""
import sys
import os

# 프로젝트 루트를 Python 경로에 추가
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from services import AIService, WordService
from models import Word


def test_ai_service():
    """AI 서비스 테스트"""
    print("\n" + "="*50)
    print("🤖 AI Service 테스트")
    print("="*50)
    
    ai_service = AIService()
    
    # 1. 유사도 계산 테스트
    print("\n1. 유사도 계산 테스트:")
    word1, word2 = "알고리즘", "자료구조"
    similarity, score, is_correct = ai_service.get_similarity_score(word1, word2)
    print(f"   '{word1}' vs '{word2}'")
    print(f"   유사도: {similarity:.4f}, 점수: {score}, 정답: {is_correct}")
    
    # 2. 정답 체크 테스트
    print("\n2. 정답 체크 테스트:")
    word1, word2 = "스택", "스택"
    similarity, score, is_correct = ai_service.get_similarity_score(word1, word2)
    print(f"   '{word1}' vs '{word2}'")
    print(f"   유사도: {similarity:.4f}, 점수: {score}, 정답: {is_correct}")
    
    # 3. 배치 유사도 테스트
    print("\n3. 배치 유사도 테스트:")
    user_words = ["정렬", "탐색", "자료구조", "알고리즘"]
    answer_word = "알고리즘"
    results = ai_service.calculate_batch_similarity(user_words, answer_word)
    print(f"   정답: '{answer_word}'")
    for word, similarity, score, is_correct in results:
        print(f"   - '{word}': {score}점 (유사도: {similarity:.4f})")
    
    print("\n✅ AI Service 테스트 완료!")


def test_word_service():
    """단어 서비스 테스트"""
    print("\n" + "="*50)
    print("📚 Word Service 테스트")
    print("="*50)
    
    word_service = WordService()
    
    # 1. 전체 단어 개수
    print(f"\n1. 전체 단어 개수: {word_service.total_words}개")
    
    # 2. 카테고리 목록
    categories = word_service.get_categories()
    print(f"\n2. 카테고리 목록 ({len(categories)}개):")
    for cat in categories:
        print(f"   - {cat}")
    
    # 3. 난이도 목록
    difficulties = word_service.get_difficulties()
    print(f"\n3. 난이도 목록 ({len(difficulties)}개):")
    for diff in difficulties:
        print(f"   - {diff}")
    
    # 4. 랜덤 단어 조회
    print("\n4. 랜덤 단어 조회:")
    random_word = word_service.get_random_word()
    if random_word:
        print(f"   ID: {random_word.id}")
        print(f"   정답: {random_word.answer}")
        print(f"   카테고리: {random_word.category}")
        print(f"   난이도: {random_word.difficulty}")
        print(f"   힌트: {random_word.hints[0]}")
    
    # 5. 난이도별 필터링
    print("\n5. 난이도별 필터링 (easy):")
    easy_word = word_service.get_random_word(difficulty="easy")
    if easy_word:
        print(f"   정답: {easy_word.answer} (난이도: {easy_word.difficulty})")
    
    # 6. ID로 단어 조회
    print("\n6. ID로 단어 조회 (ID=1):")
    word = word_service.get_word_by_id(1)
    if word:
        print(f"   정답: {word.answer}")
        print(f"   카테고리: {word.category}")
    
    print("\n✅ Word Service 테스트 완료!")


def test_models():
    """모델 테스트"""
    print("\n" + "="*50)
    print("📦 Model 테스트")
    print("="*50)
    
    # Word 모델 테스트
    print("\n1. Word 모델 생성:")
    word = Word(
        id=999,
        answer="테스트",
        category="테스트카테고리",
        difficulty="medium",
        hints=["힌트1", "힌트2", "힌트3"]
    )
    print(f"   생성된 단어: {word.answer}")
    
    # 딕셔너리 변환 (정답 제외)
    print("\n2. 딕셔너리 변환 (정답 제외):")
    word_dict = word.to_dict(include_answer=False)
    print(f"   {word_dict}")
    
    # 딕셔너리 변환 (정답 포함)
    print("\n3. 딕셔너리 변환 (정답 포함):")
    word_dict_with_answer = word.to_dict(include_answer=True)
    print(f"   {word_dict_with_answer}")
    
    # 딕셔너리에서 생성
    print("\n4. 딕셔너리에서 Word 생성:")
    new_word = Word.from_dict(word_dict_with_answer)
    print(f"   복원된 단어: {new_word.answer}")
    
    print("\n✅ Model 테스트 완료!")


def main():
    """메인 테스트 함수"""
    print("\n" + "🚀 AI Service 모듈 테스트 시작 🚀".center(50))
    
    try:
        # 모델 테스트
        test_models()
        
        # 단어 서비스 테스트
        test_word_service()
        
        # AI 서비스 테스트
        test_ai_service()
        
        print("\n" + "="*50)
        print("🎉 모든 테스트 완료!")
        print("="*50 + "\n")
        
    except Exception as e:
        print(f"\n❌ 테스트 실패: {str(e)}")
        import traceback
        traceback.print_exc()
        return 1
    
    return 0


if __name__ == "__main__":
    exit(main())
