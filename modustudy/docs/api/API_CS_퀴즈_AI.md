# CS 단어 퀴즈 AI 서비스 API 문서

## 서비스 개요

CS 용어 단어 맞추기 퀴즈를 위한 AI 기반 백엔드 서비스입니다.

### 주요 기능
- 🤖 **AI 기반 유사도 계산**: 사용자 답변과 정답의 의미적 유사도 분석
- 📚 **동의어 사전**: 동일 개념의 다양한 표현 처리 (해시테이블 = 해시맵 = 딕셔너리)
- 🎯 **부분 일치 보너스**: 공백/대소문자 차이 보정 (SQL인젝션 ≈ SQL 인젝션)
- 📊 **3D 시각화**: 임베딩 공간에서 단어 간 관계 시각화

### Base URL
```
http://localhost:5000
```

---

## 목차
1. [헬스 체크](#1-헬스-체크)
2. [유사도 계산](#2-유사도-계산)
3. [단어 퀴즈](#3-단어-퀴즈)
4. [동의어](#4-동의어)
5. [시각화](#5-시각화)

---

## 1. 헬스 체크

### 1.1 서비스 정보 조회
```
GET /
```

**Response:**
```json
{
  "service": "CS Word Quiz AI Service",
  "version": "2.0.0",
  "status": "running",
  "features": [
    "AI 기반 유사도 계산 (Sentence Transformer)",
    "CS 동의어 사전 매칭",
    "부분 일치 보너스",
    "카테고리 기반 보너스",
    "점수 스케일링"
  ],
  "endpoints": {
    "health": "GET /health",
    "similarity": "POST /api/similarity",
    "words": {...}
  }
}
```

---

### 1.2 서버 상태 확인
```
GET /health
```

**Response:**
```json
{
  "status": "healthy",
  "version": "2.0.0",
  "model": "jhgan/ko-sroberta-multitask",
  "model_status": "loaded",
  "words_count": 50
}
```

---

### 1.3 설정 조회
```
GET /api/config
```

**Response:**
```json
{
  "similarity_config": {
    "min_score": 0.0,
    "max_score": 100.0,
    "partial_match_bonus": 15.0,
    "synonym_bonus": 25.0,
    "category_bonus": 10.0
  },
  "model": "jhgan/ko-sroberta-multitask",
  "version": "2.0.0"
}
```

---

## 2. 유사도 계산

### 2.1 단어 유사도 계산 (단일)

사용자 입력과 정답의 유사도를 계산합니다.

```
POST /api/similarity
```

**Request Body:**
```json
{
  "userWord": "해시테이블",
  "answerWord": "해시맵",
  "category": "자료구조"  // 선택
}
```

**Response:**
```json
{
  "userWord": "해시테이블",
  "answerWord": "해시맵",
  "rawSimilarity": 0.58,      // AI 원본 유사도 (0~1)
  "similarity": 0.95,         // 보정된 유사도 (동의어 보너스 적용)
  "score": 95.0,              // 최종 점수 (0~100)
  "isCorrect": true,          // 정답 여부 (score >= 95)
  "bonuses": {
    "partial_match": 5.0,     // 부분 일치 보너스
    "category": 10.0          // 카테고리 보너스
  }
}
```

---

### 2.2 유사도 일괄 계산 (배치)

여러 단어를 한 번에 계산합니다.

```
POST /api/batch-similarity
```

**Request Body:**
```json
{
  "userWords": ["스택", "큐", "배열"],
  "answerWord": "LIFO",
  "category": "자료구조"  // 선택
}
```

**Response:**
```json
{
  "answerWord": "LIFO",
  "category": "자료구조",
  "results": [
    {
      "userWord": "스택",
      "rawSimilarity": 0.45,
      "similarity": 0.75,
      "score": 75.0,
      "isCorrect": false,
      "bonuses": {"related_keyword": 20.0}
    },
    ...
  ],
  "totalAttempts": 3
}
```

---

## 3. 단어 퀴즈

### 3.1 전체 단어 목록 조회

정답은 제외한 단어 목록을 반환합니다.

```
GET /api/words
```

**Response:**
```json
{
  "words": [
    {
      "id": 1,
      "category": "자료구조",
      "difficulty": "medium",
      "hints": ["LIFO 구조", "push, pop 연산"]
    },
    ...
  ],
  "total": 50
}
```

---

### 3.2 랜덤 단어 조회

퀴즈를 위한 랜덤 단어를 반환합니다.

```
GET /api/words/random
GET /api/words/random?difficulty=easy
GET /api/words/random?category=자료구조
GET /api/words/random?difficulty=medium&category=알고리즘
```

**Query Parameters:**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| difficulty | string | easy, medium, hard |
| category | string | 카테고리명 |

**Response:**
```json
{
  "id": 1,
  "category": "자료구조",
  "difficulty": "medium",
  "hints": ["LIFO 구조", "push, pop 연산"]
}
```

---

### 3.3 ID로 단어 조회
```
GET /api/words/{id}
```

**Response:**
```json
{
  "id": 1,
  "category": "자료구조",
  "difficulty": "medium",
  "hints": ["LIFO 구조", "push, pop 연산"]
}
```

---

### 3.4 정답 확인 ⭐

사용자의 답변을 확인하고 점수를 반환합니다. **프론트엔드에서 주로 사용할 API입니다.**

```
POST /api/words/{id}/answer
```

**Request Body:**
```json
{
  "userWord": "스택"
}
```

**Response:**
```json
{
  "wordId": 1,
  "userWord": "스택",
  "answerWord": "Stack",          // 정답일 때만 표시
  "rawSimilarity": 0.85,
  "similarity": 0.98,
  "score": 98.0,
  "isCorrect": true,
  "bonuses": {
    "partial_match": 5.0,
    "category": 10.0
  },
  "answer": "Stack"               // 정답일 때만 표시, 오답이면 null
}
```

---

### 3.5 카테고리 목록 조회
```
GET /api/categories
```

**Response:**
```json
{
  "categories": ["자료구조", "알고리즘", "네트워크", "데이터베이스", "운영체제"],
  "total": 5
}
```

---

### 3.6 난이도 목록 조회
```
GET /api/difficulties
```

**Response:**
```json
{
  "difficulties": ["easy", "medium", "hard"],
  "total": 3
}
```

---

### 3.7 오늘의 문제 조회 ⭐

매일 00시에 변경되는 오늘의 문제를 반환합니다. **대시보드 CS 퀴즈 위젯용**

```
GET /api/words/daily
```

**Response:**
```json
{
  "id": 15,
  "category": "자료구조",
  "difficulty": "medium",
  "hints": ["LIFO 구조", "push, pop 연산"],
  "date": "2026-01-22"
}
```

> 📌 같은 날에는 항상 같은 문제가 반환됩니다.

---

### 3.8 어제의 문제 및 정답 조회

어제의 문제와 정답을 반환합니다. 대시보드에서 "어제의 정답은?" 표시용.

```
GET /api/words/yesterday
```

**Response:**
```json
{
  "id": 3,
  "answer": "스택",
  "category": "자료구조",
  "difficulty": "easy",
  "hints": ["LIFO 구조", "push, pop 연산"],
  "date": "2026-01-21"
}
```

---

### 3.9 리더보드 조회 (명예의 전당) ⭐

일일 퀴즈 리더보드를 조회합니다.

```
GET /api/leaderboard
GET /api/leaderboard?date=2026-01-22&limit=10
```

**Query Parameters:**
| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| date | string | 오늘 | 조회할 날짜 (YYYY-MM-DD) |
| limit | number | 10 | 조회할 순위 개수 |

**Response:**
```json
{
  "date": "2026-01-22",
  "rankings": [
    {"rank": 1, "nickname": "홍길동", "attempts": 3, "time": 45},
    {"rank": 2, "nickname": "김철수", "attempts": 5, "time": 120},
    {"rank": 3, "nickname": "이영희", "attempts": 5, "time": 180}
  ],
  "total": 3
}
```

> 📌 순위 기준: 시도 횟수 오름차순 → 소요 시간 오름차순

---

### 3.10 리더보드 저장

퀴즈 완료 시 기록을 저장합니다.

```
POST /api/leaderboard
```

**Request Body:**
```json
{
  "nickname": "홍길동",
  "attempts": 3,
  "time": 45
}
```

**Response:**
```json
{
  "success": true,
  "rank": 1,
  "date": "2026-01-22",
  "totalPlayers": 15
}
```

---

## 4. 동의어

### 4.1 동의어 조회
```
GET /api/synonyms/{word}
```

**Example:**
```
GET /api/synonyms/해시테이블
```

**Response:**
```json
{
  "word": "해시테이블",
  "synonyms": ["해시맵", "딕셔너리", "Hash Table", "HashMap"],
  "total": 4
}
```

---

### 4.2 동의어 여부 확인
```
POST /api/check-synonym
```

**Request Body:**
```json
{
  "word1": "해시테이블",
  "word2": "해시맵"
}
```

**Response:**
```json
{
  "word1": "해시테이블",
  "word2": "해시맵",
  "areSynonyms": true
}
```

---

## 5. 시각화

3D 공간에서 단어들의 관계를 시각화하기 위한 좌표를 제공합니다.

### 5.1 3D 임베딩 좌표 (단일)

```
POST /api/embedding-3d
```

**Request Body:**
```json
{
  "userWord": "스택",
  "answerWord": "Stack",
  "referenceWords": ["큐", "배열"],  // 선택
  "category": "자료구조"              // 선택
}
```

**Response:**
```json
{
  "points": [
    {"word": "Stack", "x": 0.0, "y": 0.5, "z": 0.3, "type": "answer"},
    {"word": "스택", "x": 0.1, "y": 0.48, "z": 0.28, "type": "user"},
    {"word": "큐", "x": -0.3, "y": 0.2, "z": 0.1, "type": "reference"}
  ],
  "rawSimilarity": 0.85,
  "similarity": 0.98,
  "score": 98.0,
  "distance3d": 0.12,
  "isCorrect": true,
  "bonuses": {...},
  "variance_explained": [0.45, 0.32, 0.15]
}
```

---

### 5.2 3D 임베딩 좌표 (배치)

퀴즈 히스토리 시각화용입니다.

```
POST /api/embedding-3d-batch
```

**Request Body:**
```json
{
  "answerWord": "Stack",
  "attemptWords": ["배열", "큐", "스택"],
  "category": "자료구조"  // 선택
}
```

**Response:**
```json
{
  "answerWord": "Stack",
  "points": [
    {"word": "Stack", "x": 0, "y": 0.5, "z": 0.3, "type": "answer"},
    {"word": "배열", "x": -0.5, "y": 0.2, "z": 0.1, "type": "attempt", "similarity": 0.3, "score": 30, "order": 1},
    {"word": "큐", "x": -0.2, "y": 0.3, "z": 0.2, "type": "attempt", "similarity": 0.5, "score": 50, "order": 2},
    {"word": "스택", "x": 0.1, "y": 0.48, "z": 0.28, "type": "attempt", "similarity": 0.98, "score": 98, "order": 3}
  ],
  "attempts": [
    {"word": "배열", "rawSimilarity": 0.25, "similarity": 0.3, "score": 30, "order": 1, "isCorrect": false, "bonuses": {}},
    ...
  ],
  "totalAttempts": 3,
  "variance_explained": [0.45, 0.32, 0.15]
}
```

---

### 5.3 구체 시각화

정답을 중심으로 유사도에 따라 위치를 표시합니다.

```
POST /api/embedding-sphere
```

**Request Body:**
```json
{
  "userWord": "스택",
  "answerWord": "Stack",
  "category": "자료구조"  // 선택
}
```

**Response:**
```json
{
  "center": {"x": 0, "y": 0, "z": 0, "word": "Stack"},
  "userPoint": {"x": 0.02, "y": 0.01, "z": 0.01, "word": "스택", "radius": 0.02},
  "rawSimilarity": 0.85,
  "similarity": 0.98,
  "score": 98.0,
  "radius": 0.02,
  "isCorrect": true,
  "bonuses": {...},
  "message": "🎯 거의 정답이에요!"
}
```

**proximity message 기준:**
| 점수 | 메시지 |
|------|--------|
| 95+ | 🎯 거의 정답이에요! |
| 85+ | 🔥 아주 가까워요! |
| 70+ | 👍 꽤 가까워요! |
| 50+ | 🤔 조금 멀어요... |
| 30+ | ❄️ 많이 멀어요... |
| 30- | 🌌 완전히 다른 방향이에요! |

---

## 에러 응답

모든 API는 오류 발생 시 다음 형태로 응답합니다:

```json
{
  "error": "에러 메시지"
}
```

**HTTP 상태 코드:**
| 코드 | 설명 |
|------|------|
| 400 | 잘못된 요청 (필수 파라미터 누락) |
| 404 | 리소스를 찾을 수 없음 |
| 500 | 서버 내부 오류 |

---

## 프론트엔드 사용 예시

### 기본 퀴즈 플로우

```javascript
// 1. 랜덤 단어 가져오기
const word = await fetch('/api/words/random?difficulty=medium')
  .then(res => res.json());

// 2. 힌트 표시
console.log(word.hints);  // ["LIFO 구조", "push, pop 연산"]

// 3. 사용자 답변 확인
const result = await fetch(`/api/words/${word.id}/answer`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ userWord: '스택' })
}).then(res => res.json());

// 4. 결과 처리
if (result.isCorrect) {
  console.log(`정답! 점수: ${result.score}`);
  console.log(`정답: ${result.answer}`);
} else {
  console.log(`아쉬워요! 유사도: ${result.similarity}`);
}
```

### 3D 시각화 플로우

```javascript
// 답변 시도 기록과 함께 3D 시각화
const vizData = await fetch('/api/embedding-3d-batch', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    answerWord: 'Stack',
    attemptWords: userAttempts,
    category: '자료구조'
  })
}).then(res => res.json());

// Three.js 등으로 points 렌더링
vizData.points.forEach(point => {
  addPointToScene(point.x, point.y, point.z, point.type);
});
```
