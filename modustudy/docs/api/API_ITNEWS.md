# 뉴스 및 북마크 API 문서

## 📋 목차
- [개요](#개요)
- [뉴스 크롤링 시스템](#뉴스-크롤링-시스템)
- [데이터베이스 구조](#데이터베이스-구조)
- [API 명세](#api-명세)
  - [뉴스 조회 API](#뉴스-조회-api)
  - [북마크 API](#북마크-api)
- [에러 코드](#에러-코드)
- [사용 예시](#사용-예시)

---

## 📖 개요

SQUIZ 플랫폼의 IT 뉴스 크롤링 및 북마크 기능을 제공하는 API입니다.

### 주요 기능
- ✅ Google News IT 뉴스 자동 크롤링
- ✅ 뉴스 목록 조회 (페이징)
- ✅ 북마크 추가/삭제/조회
- ✅ 사용자별 북마크 관리

### 기술 스택
- **Backend**: Spring Boot 3.x, Java 21
- **Database**: MySQL 8.x
- **Crawler**: Jsoup (RSS Parser)
- **Scheduler**: Quartz (1시간마다 자동 실행)

---

## 🤖 뉴스 크롤링 시스템

### 크롤링 소스
- **Google News RSS API**
- 키워드 기반 검색: 20개 IT 관련 키워드

### 크롤링 키워드
```
IT기술, 인공지능+AI, 빅데이터, 클라우드+컴퓨팅, 사이버보안,
소프트웨어+개발, 프론트엔드+개발, 백엔드+개발, DevOps,
머신러닝, 블록체인, 코딩테스트, 개발자+채용, 스타트업+기술,
자바스크립트, 파이썬+프로그래밍, 쿠버네티스+Docker,
데이터사이언스, 모바일+앱개발, 웹개발
```

### 스케줄
- **실행 주기**: 매 시간 정각 (Cron: `0 0 * * * ?`)
- **처리량**: 키워드당 최대 5개 뉴스
- **예상 저장량**: 시간당 30~50개 (중복 제거 후)

### 중복 방지
- `source_url` 기준으로 중복 체크
- 이미 존재하는 뉴스는 저장하지 않음

---

## 🗄️ 데이터베이스 구조

### it_news 테이블
```sql
CREATE TABLE `it_news` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `title` VARCHAR(500) NOT NULL,
    `summary` TEXT,
    `source_url` VARCHAR(1000) NOT NULL UNIQUE,
    `source_name` VARCHAR(100),
    `thumbnail_url` VARCHAR(1000),
    `category` VARCHAR(50) DEFAULT 'IT',
    `published_at` TIMESTAMP,
    `view_count` INT DEFAULT 0,
    `is_active` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_published_at` (`published_at`),
    INDEX `idx_category` (`category`),
    INDEX `idx_created_at` (`created_at`)
);
```

### news_bookmark 테이블
```sql
CREATE TABLE `news_bookmark` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `news_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`news_id`) REFERENCES `it_news`(`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_news` (`user_id`, `news_id`)
);
```

---

## 📡 API 명세

### Base URL
```
http://localhost:8080/api
```

---

## 뉴스 조회 API

### 1. 최신 뉴스 목록 조회

**GET** `/news`

최신 뉴스 20개를 발행일 기준 내림차순으로 조회합니다.

#### Response
```json
[
  {
    "id": 1,
    "title": "AI 기술의 최신 동향",
    "summary": "인공지능 기술이 급속도로 발전하고 있습니다...",
    "sourceUrl": "https://news.google.com/...",
    "sourceName": "테크뉴스",
    "thumbnailUrl": null,
    "category": "IT",
    "publishedAt": "2026-01-26T10:00:00",
    "viewCount": 0
  }
]
```

#### Example
```bash
GET /api/news
```

---

### 2. 뉴스 상세 조회

**GET** `/news/{newsId}`

뉴스 상세 정보를 조회하며, **조회수가 자동으로 1 증가**합니다.

#### Path Parameters
| Parameter | Type | Description |
|-----------|------|-------------|
| newsId | Long | 뉴스 ID |

#### Response - Success (200)
```json
{
  "id": 1,
  "title": "AI 기술의 최신 동향",
  "summary": "인공지능 기술이 급속도로 발전하고 있습니다...",
  "sourceUrl": "https://news.google.com/...",
  "sourceName": "테크뉴스",
  "thumbnailUrl": null,
  "category": "IT",
  "publishedAt": "2026-01-26T10:00:00",
  "viewCount": 1
}
```

#### Response - Error (404)
```json
{
  "message": "뉴스를 찾을 수 없습니다."
}
```

#### Example
```bash
GET /api/news/1
```

---

### 3. 인기 뉴스 조회

**GET** `/news/popular`

조회수가 높은 순으로 정렬된 인기 뉴스 10개를 조회합니다.

#### Response
```json
[
  {
    "id": 5,
    "title": "ChatGPT 업데이트 소식",
    "summary": "OpenAI가 새로운 기능을...",
    "sourceUrl": "https://news.google.com/...",
    "sourceName": "AI타임스",
    "thumbnailUrl": null,
    "category": "IT",
    "publishedAt": "2026-01-25T15:00:00",
    "viewCount": 1523
  },
  {
    "id": 12,
    "title": "클라우드 서비스 시장 분석",
    "summary": "클라우드 컴퓨팅 시장이...",
    "sourceUrl": "https://news.google.com/...",
    "sourceName": "IT조선",
    "thumbnailUrl": null,
    "category": "IT",
    "publishedAt": "2026-01-25T14:30:00",
    "viewCount": 987
  }
]
```

#### Example
```bash
GET /api/news/popular
```

---

### 4. 카테고리별 뉴스 조회

**GET** `/news/category/{category}`

특정 카테고리의 뉴스를 최신순으로 조회합니다.

#### Path Parameters
| Parameter | Type | Description |
|-----------|------|-------------|
| category | String | 카테고리명 (예: "IT") |

#### Response
```json
[
  {
    "id": 1,
    "title": "AI 기술의 최신 동향",
    "summary": "인공지능 기술이 급속도로...",
    "sourceUrl": "https://news.google.com/...",
    "sourceName": "테크뉴스",
    "thumbnailUrl": null,
    "category": "IT",
    "publishedAt": "2026-01-26T10:00:00",
    "viewCount": 0
  }
]
```

#### Example
```bash
GET /api/news/category/IT
```

---

### 5. 뉴스 검색

**GET** `/news/search`

제목에 키워드가 포함된 뉴스를 검색합니다.

#### Request Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| keyword | String | Yes | 검색 키워드 |

#### Response
```json
[
  {
    "id": 3,
    "title": "AI 챗봇 기술 발전",
    "summary": "AI 기반 챗봇이...",
    "sourceUrl": "https://news.google.com/...",
    "sourceName": "테크뉴스",
    "thumbnailUrl": null,
    "category": "IT",
    "publishedAt": "2026-01-26T09:00:00",
    "viewCount": 5
  }
]
```

#### Example
```bash
GET /api/news/search?keyword=AI
```

---

## 북마크 API

### 1. 북마크 추가

**POST** `/news/{newsId}/bookmark`

#### Headers
```
Authorization: Bearer {JWT_TOKEN}
```

#### Path Parameters
| Parameter | Type | Description |
|-----------|------|-------------|
| newsId | Long | 뉴스 ID |

#### Response - Success (200)
```json
{
  "message": "북마크가 추가되었습니다."
}
```

#### Response - Error (400)
```json
{
  "message": "이미 북마크한 뉴스입니다."
}
```

#### Example
```bash
POST /api/news/1/bookmark
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

### 2. 북마크 삭제

**DELETE** `/news/{newsId}/bookmark`

#### Headers
```
Authorization: Bearer {JWT_TOKEN}
```

#### Path Parameters
| Parameter | Type | Description |
|-----------|------|-------------|
| newsId | Long | 뉴스 ID |

#### Response - Success (200)
```json
{
  "message": "북마크가 삭제되었습니다."
}
```

#### Response - Error (400)
```json
{
  "message": "북마크를 찾을 수 없습니다."
}
```

#### Example
```bash
DELETE /api/news/1/bookmark
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

### 3. 내 북마크 목록 조회

**GET** `/news/bookmarks`

#### Headers
```
Authorization: Bearer {JWT_TOKEN}
```

#### Response - Success (200)
```json
[
  {
    "id": 1,
    "title": "AI 기술의 최신 동향",
    "summary": "인공지능 기술이 급속도로...",
    "sourceUrl": "https://news.google.com/...",
    "sourceName": "테크뉴스",
    "thumbnailUrl": null,
    "category": "IT",
    "publishedAt": "2026-01-26T10:00:00",
    "viewCount": 0
  },
  {
    "id": 5,
    "title": "클라우드 서비스 시장 분석",
    "summary": "클라우드 컴퓨팅 시장이...",
    "sourceUrl": "https://news.google.com/...",
    "sourceName": "IT조선",
    "thumbnailUrl": null,
    "category": "IT",
    "publishedAt": "2026-01-26T09:30:00",
    "viewCount": 0
  }
]
```

#### Example
```bash
GET /api/news/bookmarks
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

### 4. 북마크 여부 확인

**GET** `/news/{newsId}/bookmark/check`

#### Headers
```
Authorization: Bearer {JWT_TOKEN}
```

#### Path Parameters
| Parameter | Type | Description |
|-----------|------|-------------|
| newsId | Long | 뉴스 ID |

#### Response - Success (200)
```json
{
  "isBookmarked": true
}
```

#### Example
```bash
GET /api/news/1/bookmark/check
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## 🧪 테스트용 API (인증 불필요)

개발/테스트 목적으로 JWT 인증 없이 사용할 수 있는 API입니다.

### 1. 테스트용 북마크 추가
**POST** `/news/test/{newsId}/bookmark/{userId}`

### 2. 테스트용 북마크 삭제
**DELETE** `/news/test/{newsId}/bookmark/{userId}`

### 3. 테스트용 북마크 목록 조회
**GET** `/news/test/bookmarks/{userId}`

### 4. 테스트용 북마크 여부 확인
**GET** `/news/test/{newsId}/bookmark/check/{userId}`

---

## ⚠️ 에러 코드

### HTTP Status Codes

| Status Code | Description |
|-------------|-------------|
| 200 | 성공 |
| 400 | 잘못된 요청 (중복 북마크, 존재하지 않는 북마크 등) |
| 401 | 인증 실패 (JWT 토큰 없음/만료) |
| 404 | 리소스를 찾을 수 없음 (존재하지 않는 뉴스 ID) |
| 500 | 서버 내부 오류 |

### Error Response Format
```json
{
  "message": "에러 메시지",
  "timestamp": "2026-01-26T10:00:00"
}
```

---

## 💡 사용 예시

### 프론트엔드 통합 예시 (React)

#### 1. 최신 뉴스 목록 조회
```javascript
const fetchLatestNews = async () => {
  const response = await fetch('http://localhost:8080/api/news');
  const data = await response.json();
  return data; // 배열 형태로 반환
};
```

#### 2. 뉴스 상세 조회 (조회수 증가)
```javascript
const fetchNewsDetail = async (newsId) => {
  const response = await fetch(`http://localhost:8080/api/news/${newsId}`);
  const data = await response.json();
  return data;
};
```

#### 3. 인기 뉴스 조회
```javascript
const fetchPopularNews = async () => {
  const response = await fetch('http://localhost:8080/api/news/popular');
  const data = await response.json();
  return data;
};
```

#### 4. 카테고리별 뉴스 조회
```javascript
const fetchNewsByCategory = async (category) => {
  const response = await fetch(
    `http://localhost:8080/api/news/category/${category}`
  );
  const data = await response.json();
  return data;
};
```

#### 5. 뉴스 검색
```javascript
const searchNews = async (keyword) => {
  const response = await fetch(
    `http://localhost:8080/api/news/search?keyword=${encodeURIComponent(keyword)}`
  );
  const data = await response.json();
  return data;
};
```

#### 6. 북마크 추가
```javascript
const addBookmark = async (newsId, token) => {
  const response = await fetch(
    `http://localhost:8080/api/news/${newsId}/bookmark`,
    {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    }
  );
  const data = await response.json();
  return data;
};
```

#### 3. 북마크 삭제
```javascript
const removeBookmark = async (newsId, token) => {
  const response = await fetch(
    `http://localhost:8080/api/news/${newsId}/bookmark`,
    {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    }
  );
  const data = await response.json();
  return data;
};
```

#### 4. 내 북마크 목록 조회
```javascript
const fetchMyBookmarks = async (token) => {
  const response = await fetch(
    'http://localhost:8080/api/news/bookmarks',
    {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  const data = await response.json();
  return data;
};
```

#### 5. 북마크 여부 확인
```javascript
const checkBookmark = async (newsId, token) => {
  const response = await fetch(
    `http://localhost:8080/api/news/${newsId}/bookmark/check`,
    {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  const data = await response.json();
  return data.isBookmarked;
};
```

---

## 🔄 데이터 흐름

### 뉴스 크롤링 → 저장 흐름
```
1. Quartz Scheduler (매 시간 정각)
   ↓
2. NewsScrapingJob 실행
   ↓
3. NewsScraperService.scrapeGoogleNewsIT()
   ↓
4. 20개 키워드 순회
   ↓
5. Google News RSS 요청 (키워드별)
   ↓
6. RSS 파싱 (Jsoup)
   ↓
7. 중복 체크 (sourceUrl)
   ↓
8. DB 저장 (ItNews)
```

### 북마크 추가 흐름
```
1. Client: POST /api/news/{newsId}/bookmark
   ↓
2. JWT 인증 (Spring Security)
   ↓
3. NewsBookmarkController
   ↓
4. NewsBookmarkService.addBookmark()
   ↓
5. 중복 체크 (user_id, news_id)
   ↓
6. NewsBookmark 엔티티 생성
   ↓
7. DB 저장
   ↓
8. Response: {"message": "북마크가 추가되었습니다."}
```

---

## 📊 성능 고려사항

### 인덱스
- `it_news.published_at`: 최신 뉴스 조회 최적화
- `it_news.category`: 카테고리별 필터링 최적화
- `news_bookmark(user_id, news_id)`: 북마크 중복 체크 최적화

### 페이징
- 기본 페이지 크기: 20개
- 최대 페이지 크기: 100개 (필요시 조정 가능)

### 캐싱 (향후 구현 예정)
- Redis를 활용한 뉴스 목록 캐싱
- 북마크 여부 캐싱

---

## 🛠️ 개발 정보

### 패키지 구조
```
com.ssafy.domain.news
├── controller
│   ├── NewsController.java
│   └── NewsBookmarkController.java
├── service
│   ├── NewsScraperService.java
│   └── NewsBookmarkService.java
├── repository
│   ├── ItNewsRepository.java
│   └── NewsBookmarkRepository.java
├── entity
│   ├── ItNews.java
│   └── NewsBookmark.java
├── dto
│   └── response
│       └── NewsResponse.java
└── job
    └── NewsScrapingJob.java
```

### 테스트
```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트만 실행
./gradlew test --tests NewsScraperServiceTest
./gradlew test --tests NewsBookmarkServiceTest
```

---

## 📝 변경 이력

### v1.1.0 (2026-01-26)
- ✅ 뉴스 상세 조회 API (조회수 자동 증가)
- ✅ 인기 뉴스 조회 API (조회수 높은 순 10개)
- ✅ 카테고리별 뉴스 조회 API
- ✅ 뉴스 검색 API (제목 기반)
- ✅ 컨트롤러 테스트 코드 작성 (NewsControllerTest)

### v1.0.0 (2026-01-26)
- ✅ Google News IT 뉴스 크롤링 기능 구현
- ✅ 뉴스 목록 조회 API (최신순 20개)
- ✅ 북마크 CRUD API
- ✅ Quartz 스케줄러 설정 (1시간마다)
- ✅ 테스트 코드 작성 (NewsScraperServiceTest, NewsBookmarkServiceTest)
---

## 📌 참고사항

### Google News RSS URL 형식
```
https://news.google.com/rss/search?q={키워드}&hl=ko&gl=KR&ceid=KR:ko
```

### Cron 표현식
```
"0 0 * * * ?" - 매 시간 정각
"0 */30 * * * ?" - 30분마다
"0 0 */3 * * ?" - 3시간마다
```

---

**마지막 업데이트**: 2026-01-26