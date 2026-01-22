# AI Service 모듈화 구조

## 📁 디렉토리 구조

```
cs-quiz-ai-service/
├── app.py                      # Flask 애플리케이션 진입점
├── config.py                   # 설정 파일
├── requirements.txt            # 의존성 패키지
├── data/
│   └── words.json             # 단어 퀴즈 데이터
├── models/                     # 데이터 모델
│   ├── __init__.py
│   └── word.py                # 단어 관련 모델
├── services/                   # 비즈니스 로직
│   ├── __init__.py
│   ├── ai_service.py          # AI 모델 및 유사도 계산
│   └── word_service.py        # 단어 데이터 관리
└── routes/                     # API 엔드포인트
    ├── __init__.py
    ├── health_routes.py       # 헬스 체크
    ├── similarity_routes.py   # 유사도 계산
    └── word_routes.py         # 단어 퀴즈
```

## 🎯 모듈 설명

### 1. Models (`models/`)
데이터 구조를 정의하는 모델 클래스들

- **`word.py`**: 단어 퀴즈 관련 데이터 모델
  - `Word`: 단어 퀴즈 데이터 모델
  - `WordResponse`: 단어 조회 응답 모델
  - `SimilarityRequest`: 유사도 계산 요청 모델
  - `SimilarityResponse`: 유사도 계산 응답 모델

### 2. Services (`services/`)
비즈니스 로직을 처리하는 서비스 클래스들

- **`ai_service.py`**: AI 모델 관리 및 유사도 계산
  - 싱글톤 패턴으로 구현
  - Sentence Transformer 모델 로딩 및 관리
  - 단어 간 코사인 유사도 계산
  - 배치 유사도 계산

- **`word_service.py`**: 단어 퀴즈 데이터 관리
  - 싱글톤 패턴으로 구현
  - 단어 데이터 로딩 및 캐싱
  - 카테고리/난이도별 필터링
  - 랜덤 단어 선택

### 3. Routes (`routes/`)
API 엔드포인트를 정의하는 Blueprint들

- **`health_routes.py`**: 헬스 체크 관련 엔드포인트
  - `GET /`: 서비스 정보
  - `GET /health`: 서버 상태 확인

- **`similarity_routes.py`**: 유사도 계산 관련 엔드포인트
  - `POST /api/similarity`: 단어 유사도 계산
  - `POST /api/batch-similarity`: 배치 유사도 계산

- **`word_routes.py`**: 단어 퀴즈 관련 엔드포인트
  - `GET /api/words`: 모든 단어 조회
  - `GET /api/words/random`: 랜덤 단어 조회
  - `GET /api/words/<id>`: ID로 단어 조회
  - `POST /api/words/<id>/answer`: 정답 확인
  - `GET /api/categories`: 카테고리 목록
  - `GET /api/difficulties`: 난이도 목록

## 🔧 주요 개선사항

### 1. **관심사의 분리 (Separation of Concerns)**
- 데이터 모델, 비즈니스 로직, API 엔드포인트를 명확히 분리
- 각 모듈이 단일 책임을 가지도록 설계

### 2. **재사용성 향상**
- 서비스 클래스를 싱글톤 패턴으로 구현하여 인스턴스 재사용
- 모델 클래스로 데이터 구조를 명확히 정의

### 3. **유지보수성 개선**
- 기능별로 파일을 분리하여 코드 찾기 쉬움
- Blueprint를 사용한 라우트 그룹화

### 4. **확장성 증대**
- 새로운 기능 추가 시 해당 모듈만 수정
- 테스트 코드 작성이 용이한 구조

### 5. **타입 안정성**
- dataclass를 사용한 명확한 데이터 구조
- 타입 힌트로 코드 가독성 향상

## 📝 사용 예시

### 서비스 사용
```python
from services import AIService, WordService

# AI 서비스 사용
ai_service = AIService()
similarity = ai_service.calculate_similarity("알고리즘", "자료구조")

# 단어 서비스 사용
word_service = WordService()
random_word = word_service.get_random_word(difficulty="easy")
```

### 모델 사용
```python
from models import Word

# 단어 객체 생성
word = Word(
    id=1,
    answer="알고리즘",
    category="기초개념",
    difficulty="easy",
    hints=["힌트1", "힌트2"]
)

# 딕셔너리로 변환 (정답 제외)
word_dict = word.to_dict(include_answer=False)
```

## 🚀 실행 방법

기존과 동일하게 실행:
```bash
python app.py
```

## 🔄 마이그레이션 가이드

기존 코드에서 새로운 모듈화된 구조로 마이그레이션:

1. **기존 방식**:
   ```python
   # app.py에서 직접 함수 호출
   similarity = calculate_similarity(word1, word2)
   ```

2. **새로운 방식**:
   ```python
   # 서비스 클래스 사용
   from services import AIService
   ai_service = AIService()
   similarity = ai_service.calculate_similarity(word1, word2)
   ```

## 📊 성능 최적화

- **싱글톤 패턴**: AI 모델과 단어 데이터를 한 번만 로딩
- **캐싱**: 로딩된 데이터를 메모리에 유지
- **지연 로딩**: 필요할 때만 모델 로딩

## 🧪 테스트

각 모듈을 독립적으로 테스트할 수 있습니다:

```python
# services 테스트
from services import AIService

def test_similarity():
    ai_service = AIService()
    similarity = ai_service.calculate_similarity("테스트", "테스트")
    assert similarity == 1.0
```

## 📚 추가 개발 가이드

### 새로운 엔드포인트 추가
1. `routes/` 폴더에 새로운 Blueprint 파일 생성
2. `routes/__init__.py`에 Blueprint 추가
3. `app.py`에서 Blueprint 등록

### 새로운 서비스 추가
1. `services/` 폴더에 새로운 서비스 파일 생성
2. `services/__init__.py`에 서비스 클래스 추가
3. 필요한 라우트에서 서비스 임포트

### 새로운 모델 추가
1. `models/` 폴더에 새로운 모델 파일 생성
2. `models/__init__.py`에 모델 클래스 추가
3. 서비스나 라우트에서 모델 사용
