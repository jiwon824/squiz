# CS Word Quiz AI Service

CS 단어 유사도 계산 AI 백엔드 서비스

## 🚀 빠른 시작

### 요구사항
- Python 3.11.9
- pip

### 설치 및 실행
```bash
# 1. 가상환경 생성
python -m venv venv
source venv\Scripts\activate  # Windows
source venv/bin/activate  # Mac/Linux

# 2. 패키지 설치
pip install -r requirements.txt

# 3. 환경 변수 설정
cp .env.example .env

# 4. 서버 실행
python app.py
```

서버 주소: `http://localhost:5000`

**⚠️ 첫 실행 시 AI 모델 다운로드로 1~2분 소요됩니다 (약 400MB)**

---

## 📡 API 엔드포인트

### 1. 서버 상태 확인
```http
GET /health
```

### 2. 단어 유사도 계산 (핵심 기능)
```http
POST /api/similarity
Content-Type: application/json

{
  "userWord": "정렬",
  "answerWord": "알고리즘"
}
```

**응답:**
```json
{
  "userWord": "정렬",
  "answerWord": "알고리즘",
  "similarity": 0.5837,
  "score": 58.37,
  "isCorrect": false
}
```

### 3. 랜덤 문제 가져오기
```http
GET /api/words/random
GET /api/words/random?difficulty=easy
GET /api/words/random?category=자료구조
```

**응답:**
```json
{
  "id": 1,
  "category": "기초개념",
  "difficulty": "easy",
  "hints": ["힌트1", "힌트2", "힌트3"]
}
```

### 4. 정답 확인
```http
POST /api/words/{wordId}/answer
Content-Type: application/json

{
  "userWord": "알고리즘"
}
```

**응답 (정답):**
```json
{
  "wordId": 1,
  "userWord": "알고리즘",
  "score": 100.0,
  "isCorrect": true,
  "answer": "알고리즘"
}
```

**응답 (오답):**
```json
{
  "wordId": 1,
  "userWord": "정렬",
  "score": 58.37,
  "isCorrect": false,
  "answer": null
}
```

### 5. 여러 단어 유사도 비교
```http
POST /api/batch-similarity
Content-Type: application/json

{
  "userWords": ["정렬", "탐색", "자료구조"],
  "answerWord": "알고리즘"
}
```

---

## 🎮 게임 흐름
```
1. GET /api/words/random
   → { id: 1, hints: [...] }

2. POST /api/words/1/answer { "userWord": "정렬" }
   → { score: 58.37, isCorrect: false }

3. POST /api/words/1/answer { "userWord": "알고리즘" }
   → { score: 100.0, isCorrect: true, answer: "알고리즘" }
```


## 📊 유사도 점수 가이드

| 점수 | 의미 |
|------|------|
| 90-100 | 거의 정답 |
| 70-89 | 매우 관련 높음 |
| 50-69 | 어느 정도 관련 있음 |
| 30-49 | 약간 관련 있음 |
| 0-29 | 관련 없음 |

---

## 🛠 기술 스택

- **Flask** 3.0.0 - 웹 프레임워크
- **Sentence Transformers** - 한국어 AI 모델 (`jhgan/ko-sroberta-multitask`)
- **Flask-CORS** - CORS 처리


## 🏗 프로젝트 구조

```
cs-quiz-ai-service/
├── app.py                      # Flask 애플리케이션 진입점
├── config.py                   # 설정 파일
├── requirements.txt            # 의존성 패키지
├── ARCHITECTURE.md             # 아키텍처 상세 문서
├── MODULARIZATION_EVALUATION.md # 모듈화 평가 보고서
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

### 모듈 설명

- **`models/`**: 데이터 구조를 정의하는 모델 클래스들
- **`services/`**: 비즈니스 로직을 처리하는 서비스 클래스들 (싱글톤 패턴)
- **`routes/`**: API 엔드포인트를 정의하는 Blueprint들

자세한 아키텍처 설명은 [ARCHITECTURE.md](./ARCHITECTURE.md)를 참고하세요.


## 📁 데이터 구조

**data/words.json:**
```json
{
  "words": [
    {
      "id": 1,
      "answer": "알고리즘",
      "category": "기초개념",
      "difficulty": "easy",
      "hints": ["힌트1", "힌트2", "힌트3"]
    }
  ]
}
```

---

## 🐛 문제 해결

**모듈 import 오류:**
```bash
pip install --upgrade sentence-transformers
```

**CORS 오류:**
- `.env`의 `CORS_ORIGINS`에 프론트엔드 주소 추가

**포트 충돌:**
- `.env`에서 `PORT` 변경

---

## 📞 API 테스트

Postman 또는 curl로 테스트:
```bash
curl http://localhost:5000/health
```