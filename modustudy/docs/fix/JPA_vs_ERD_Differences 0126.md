# JPA Entity vs ERD.sql 차이점 분석

## 개요
이 문서는 JPA Entity 클래스와 ERD.sql 스키마 간의 차이점을 정리한 문서입니다.

---

## 1. User 테이블

### ERD.sql 정의
```sql
CREATE TABLE `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `email` VARCHAR(100) NOT NULL UNIQUE,
    `password` VARCHAR(255),
    `name` VARCHAR(50),
    `nickname` VARCHAR(50) NOT NULL UNIQUE,
    `profile_image` VARCHAR(500),
    `role` ENUM('USER', 'ADMIN') DEFAULT 'USER',
    `is_active` BOOLEAN DEFAULT TRUE,
    `is_online` BOOLEAN DEFAULT FALSE,
    `last_seen_at` TIMESTAMP,
    `is_searchable` BOOLEAN DEFAULT TRUE,
    `leader_rating` FLOAT DEFAULT 0.0,
    `leader_review_count` INT DEFAULT 0,
    `last_login_at` TIMESTAMP,
    `created_at` TIMESTAMP,
    `updated_at` TIMESTAMP
);
```

### JPA Entity 추가 필드 (ERD에 없음)
| 필드명 | 타입 | 설명 |
|--------|------|------|
| `userId` | String | 사용자 ID (email과 별도) |
| `department` | String | 부서 |
| `position` | String | 직위 |
| `bio` | String (TEXT) | 자기소개 |
| `interests` | String (JSON) | 관심 분야 |
| `techStacks` | String (JSON) | 기술 스택 |
| `totalExp` | Integer | 총 경험치 |
| `currentPoints` | Integer | 현재 포인트 |
| `currentLevel` | Integer | 현재 레벨 |
| `levelName` | String | 레벨 이름 |

### 차이점 요약
- JPA Entity에 게이미피케이션 관련 필드(totalExp, currentPoints, currentLevel, levelName)가 직접 포함되어 있음
- ERD에서는 이러한 정보가 `user_stats` 테이블에 분리되어 있음
- JPA Entity에 추가 프로필 정보(bio, interests, techStacks)가 포함됨
- ERD에서는 `profile` 테이블로 분리되어 있음

---

## 2. UserSocialAccount 테이블

### ERD.sql 정의
```sql
CREATE TABLE `user_social_account` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `provider` ENUM('GOOGLE', 'KAKAO', 'NAVER') NOT NULL,
    `provider_user_id` VARCHAR(100) NOT NULL,
    `email` VARCHAR(100),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### JPA Entity 추가 필드 (ERD에 없음)
| 필드명 | 타입 | 설명 |
|--------|------|------|
| `isPrimary` | Boolean | 기본 소셜 계정 여부 |
| `linkedAt` | LocalDateTime | 연동 시점 |

---

## 3. UserStats 테이블

### ERD.sql 정의
```sql
CREATE TABLE `user_stats` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL UNIQUE,
    `level` INT DEFAULT 1,
    `level_name` VARCHAR(50) DEFAULT '새싹',
    `total_activity_days` INT DEFAULT 0,
    `current_streak` INT DEFAULT 0,
    `max_streak` INT DEFAULT 0,
    `last_activity_date` DATE,
    `total_studies_joined` INT DEFAULT 0,
    `total_studies_led` INT DEFAULT 0,
    `total_chat_count` INT DEFAULT 0,
    `total_quiz_count` INT DEFAULT 0,
    `total_materials_uploaded` INT DEFAULT 0,
    `total_retrospectives` INT DEFAULT 0,
    `updated_at` TIMESTAMP
);
```

### JPA Entity 상태
- **거의 비어있음** - BaseEntity만 상속하고 필드 없음
- ERD에 정의된 통계 필드들이 JPA Entity에 구현되지 않음

---

## 4. Report 테이블

### ERD.sql 정의
```sql
CREATE TABLE `report` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL,
    `report_type` ENUM('APPLICATION', 'MONTHLY_RESULT') NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` JSON,
    `report_month` DATE,
    `status` ENUM('DRAFT', 'SUBMITTED') DEFAULT 'DRAFT',
    `created_by` BIGINT NOT NULL,
    `created_at` TIMESTAMP,
    `updated_at` TIMESTAMP
);
```

### JPA Entity 상태
- **비어있음** - BaseEntity만 상속하고 필드 없음
- ERD에 정의된 모든 필드가 JPA Entity에 구현되지 않음

---

## 5. StudyMember 테이블

### ERD.sql 정의
```sql
CREATE TABLE `study_member` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `study_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `role` ENUM('LEADER', 'MEMBER') DEFAULT 'MEMBER',
    `status` ENUM('PENDING', 'APPROVED', 'REJECTED', 'LEFT', 'KICKED') DEFAULT 'PENDING',
    `joined_at` TIMESTAMP,
    `left_at` TIMESTAMP,
    `is_probation` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP
);
```

### JPA Entity 상태
- **일치함** - 대부분의 필드가 ERD와 일치
- MemberRole enum: LEADER, MEMBER
- MemberStatus enum 값이 다를 수 있음 (확인 필요)

---

## 6. ERD에만 존재하는 테이블 (JPA Entity 없음)

| 테이블명 | 설명 |
|----------|------|
| `profile` | 사용자 프로필 (JPA에서는 User에 통합) |
| `region` | 지역 정보 |
| `study` | 스터디 기본 정보 |
| `study_session` | 스터디 세션 |
| `study_bookmark` | 스터디 북마크 |
| `study_leader_review` | 스터디장 평가 |
| `format` | 스터디 형식 |
| `topic` | 스터디 주제 |
| `workspace` | 워크스페이스 |
| `message` | 채팅 메시지 |
| `meeting` | 미팅 |
| `meeting_transcript` | 미팅 STT 결과 |
| `meeting_summary` | 미팅 요약 |
| `meeting_participant_summary` | 참가자별 요약 |
| `quiz_course_stats` | 퀴즈 코스 통계 |
| `quiz_course_record` | 퀴즈 코스 기록 |
| `quiz_course_answer` | 퀴즈 코스 답안 |
| `daily_report` | 데일리 리포트 |
| `daily_item` | 데일리 아이템 |
| `it_news` | IT 뉴스 |
| `news_bookmark` | 뉴스 북마크 |
| `QRTZ_*` | Quartz 스케줄러 테이블들 |

---

## 7. JPA Entity에만 존재하는 테이블/필드

일부 JPA Entity는 ERD에 없는 필드를 포함하거나, ERD와 다른 구조를 가짐:

### QuizCourse
- JPA: `sections` 관계 매핑 (OneToMany)
- ERD: 별도의 `quiz_course_section` 테이블과 FK 관계

### Badge
- JPA: `BadgeCategory` enum 사용
- ERD: String ENUM으로 정의

---

## 8. ENUM 값 차이

### MemberStatus (StudyMember)
| ERD | JPA |
|-----|-----|
| PENDING, APPROVED, REJECTED, LEFT, KICKED | 확인 필요 |

### Report Status
| ERD | JPA |
|-----|-----|
| DRAFT, SUBMITTED | 구현 안됨 |

---

## 9. 권장 사항

### 즉시 수정 필요
1. **UserStats Entity** - ERD에 정의된 필드들 구현 필요
2. **Report Entity** - ERD에 정의된 필드들 구현 필요

### 설계 검토 필요
1. User 테이블의 게이미피케이션 필드
   - 현재: User Entity에 직접 포함
   - ERD 설계: user_stats 테이블로 분리
   - **결정 필요**: 어느 방식을 따를 것인지

2. Profile 정보
   - 현재: User Entity에 직접 포함 (bio, interests, techStacks)
   - ERD 설계: profile 테이블로 분리
   - **결정 필요**: 어느 방식을 따를 것인지

### 새로 구현 필요한 Entity
- Region
- Study (기본 테이블)
- StudySession
- Workspace
- Message
- Meeting (기본 테이블)
- 기타 ERD에만 존재하는 테이블들

---

## 10. 결론

현재 JPA Entity와 ERD.sql 간에 상당한 차이가 존재합니다. 주요 원인:
1. **일부 Entity가 비어있음** (UserStats, Report 등)
2. **정규화 수준 차이** (User에 프로필/통계 정보 통합 vs 분리)
3. **ERD에 정의된 테이블 중 미구현된 Entity 다수**

개발 진행 시 ERD.sql을 기준으로 Entity를 구현하거나, 현재 JPA Entity 구조에 맞게 ERD를 수정하는 결정이 필요합니다.
