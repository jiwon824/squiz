0# ModuStudy Backend API

> Spring Boot 기반 스터디 관리 플랫폼 백엔드 서버

## 📋 프로젝트 정보

- **프로젝트명**: ssafy-web-project
- **Spring Boot**: 3.2.2
- **Java**: 21
- **빌드 도구**: Gradle
- **데이터베이스**: MySQL 8.x
- **서버 포트**: 8080

## 🚀 시작하기

### 실행 방법
```bash
# Gradle을 이용한 실행
./gradlew bootRun

# 또는 빌드 후 실행
./gradlew build
java -jar build/libs/ssafy-web-project-1.0-SNAPSHOT.jar
```

### API 문서
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/api-docs

## 📁 프로젝트 구조

```
backend/
├── src/main/
│   ├── java/com/ssafy/
│   │   ├── GroupCallApplication.java    # 메인 애플리케이션 진입점
│   │   │
│   │   ├── api/                          # 레거시 API (User 관련)
│   │   │   ├── controller/              # 레거시 컨트롤러
│   │   │   ├── request/                 # 요청 DTO
│   │   │   ├── response/                # 응답 DTO
│   │   │   └── service/                 # 레거시 서비스
│   │   │
│   │   ├── common/                       # 공통 유틸리티 및 설정
│   │   │   ├── auth/                    # JWT 인증 필터, UserDetails
│   │   │   ├── entity/                  # BaseEntity (공통 엔티티)
│   │   │   ├── exception/               # 예외 처리 핸들러
│   │   │   ├── model/                   # 공통 모델
│   │   │   ├── response/                # 공통 응답 포맷
│   │   │   ├── util/                    # JWT, 응답 유틸리티
│   │   │   └── websocket/               # WebSocket 설정 및 핸들러
│   │   │
│   │   ├── config/                       # 애플리케이션 설정
│   │   │   ├── IndexController.java     # SPA 라우팅 처리
│   │   │   ├── JpaConfig.java           # JPA 및 QueryDSL 설정
│   │   │   ├── PasswordEncoderConfig.java
│   │   │   ├── SecurityConfig.java      # Spring Security 설정
│   │   │   ├── SwaggerConfig.java       # API 문서화 설정
│   │   │   └── WebMvcConfig.java        # CORS, 정적 리소스 설정
│   │   │
│   │   ├── db/                          # 레거시 DB 엔티티
│   │   │   ├── entity/                  # 레거시 엔티티
│   │   │   └── repository/              # 레거시 리포지토리
│   │   │
│   │   ├── domain/                      # 도메인별 비즈니스 로직 (메인)
│   │   │   ├── ai/                      # AI 피드백
│   │   │   ├── attendance/              # 출석 관리
│   │   │   ├── board/                   # 게시판
│   │   │   ├── chat/                    # 그룹 채팅
│   │   │   ├── comendle/                # 코딩 게임
│   │   │   ├── daily/                   # 데일리 스크럼
│   │   │   ├── dm/                      # 다이렉트 메시지
│   │   │   ├── friend/                  # 친구 관리
│   │   │   ├── gamification/            # 게이미피케이션
│   │   │   ├── material/                # 학습 자료
│   │   │   ├── meeting/                 # 화상 회의
│   │   │   ├── news/                    # 뉴스
│   │   │   ├── notification/            # 알림
│   │   │   ├── quiz/                    # 퀴즈
│   │   │   ├── recruitment/             # 모집
│   │   │   ├── report/                  # 보고서
│   │   │   ├── retrospect/              # 회고
│   │   │   ├── study/                   # 스터디 관리
│   │   │   └── user/                    # 사용자 관리
│   │   │
│   │   └── infra/                       # 인프라 관련 코드
│   │
│   └── resources/
│       ├── application.properties       # 애플리케이션 설정 파일
│       └── dist/                        # 프론트엔드 빌드 파일
│
├── build.gradle                         # 빌드 및 의존성 설정
└── settings.gradle
```

## 🏗️ 아키텍처

### 계층형 구조
각 도메인은 다음과 같은 계층으로 구성됩니다:

```
domain/{domain-name}/
├── controller/     # REST API 엔드포인트 (@RestController)
├── dto/           # 데이터 전송 객체
│   ├── request/   # API 요청 DTO
│   └── response/  # API 응답 DTO
├── entity/        # JPA 엔티티 (DB 테이블 매핑)
├── repository/    # 데이터 접근 계층 (JPA Repository)
└── service/       # 비즈니스 로직 (@Service)
```

## 📦 도메인 모듈

### 사용자 & 인증
- **user**: 사용자 관리, 프로필, 소셜 로그인, 일정
- **friend**: 친구 추가 및 관리

### 스터디 관리
- **study**: 스터디 그룹 생성 및 관리
- **recruitment**: 스터디 멤버 모집
- **attendance**: 출석 체크 및 세션 메모
- **meeting**: 화상 회의 (WebSocket)

### 학습 기능
- **quiz**: 퀴즈 생성, 응답, 채점
- **material**: 학습 자료 업로드 및 관리
- **ai**: AI 기반 학습 피드백
- **comendle**: 코딩 게임 (Wordle 스타일)

### 커뮤니케이션
- **chat**: 그룹 채팅
- **dm**: 1:1 다이렉트 메시지
- **board**: 게시판 (공지, 자유게시판)
- **notification**: 실시간 알림

### 협업 & 기록
- **daily**: 데일리 스크럼 기록
- **retrospect**: 스터디 회고
- **report**: 학습 보고서

### 부가 기능
- **gamification**: 포인트, 배지, 레벨 시스템
- **news**: IT 뉴스 및 정보

## 🔑 주요 파일 설명

### 설정 파일 (config/)

| 파일 | 기능 |
|-----|------|
| `SecurityConfig.java` | Spring Security 설정, JWT 인증 필터 적용 |
| `SwaggerConfig.java` | API 문서 자동 생성 설정 |
| `JpaConfig.java` | JPA 및 QueryDSL 설정 |
| `WebMvcConfig.java` | CORS 설정, 정적 리소스 처리 |
| `PasswordEncoderConfig.java` | 비밀번호 암호화 Bean 등록 |
| `IndexController.java` | SPA 라우팅 처리 (모든 경로를 index.html로) |

### 공통 모듈 (common/)

#### auth/
- `JwtAuthenticationFilter.java`: JWT 토큰 검증 필터
- `SsafyUserDetailService.java`: Spring Security UserDetailsService 구현
- `SsafyUserDetails.java`: UserDetails 구현체

#### entity/
- `BaseEntity.java`: 공통 엔티티 필드 (생성일, 수정일 등)

#### exception/
- `handler/NotFoundHandler.java`: 404 예외 처리

#### response/
- `ApiResponse.java`: 표준 API 응답 포맷
- `ErrorResponse.java`: 에러 응답 포맷
- `PageResponse.java`: 페이징 응답 포맷

#### util/
- `JwtTokenUtil.java`: JWT 토큰 생성 및 검증 유틸리티
- `ResponseBodyWriteUtil.java`: HTTP 응답 작성 유틸리티

#### websocket/
- `WebSocketMessageBrokerSettings.java`: WebSocket 메시지 브로커 설정
- `WebSocketSessionManager.java`: WebSocket 세션 관리
- `BaseWebSocketEventHandler.java`: WebSocket 이벤트 핸들러

## 🔐 인증 & 보안

### JWT 토큰 인증
- **Access Token**: 15일 유효
- **Refresh Token**: 30일 유효
- **헤더**: `Authorization: Bearer {token}`

### 인증 제외 경로
```
/swagger-ui/**        # Swagger UI
/api-docs/**          # API 문서
/api/v1/auth/**       # 로그인, 회원가입
/api/v1/user          # 회원가입 (POST)
```

### 인증 필요 경로
- 위 경로를 제외한 모든 API 엔드포인트

## 🌐 API 엔드포인트

### 기본 URL 구조
```
http://localhost:8080/api/v1/{domain}/{resource}
```

### 주요 도메인별 엔드포인트

#### User (사용자)
- `POST /api/v1/auth/login` - 로그인
- `POST /api/v1/auth/logout` - 로그아웃
- `POST /api/v1/user` - 회원가입
- `GET /api/v1/user/{userId}` - 사용자 정보 조회
- `PUT /api/v1/user/{userId}` - 사용자 정보 수정

#### Study (스터디)
- `POST /api/v1/study` - 스터디 생성
- `GET /api/v1/study` - 스터디 목록 조회
- `GET /api/v1/study/{studyId}` - 스터디 상세 조회
- `PUT /api/v1/study/{studyId}` - 스터디 수정
- `DELETE /api/v1/study/{studyId}` - 스터디 삭제

#### Quiz (퀴즈)
- `POST /api/v1/quiz` - 퀴즈 생성
- `GET /api/v1/quiz/{quizId}` - 퀴즈 조회
- `POST /api/v1/quiz/{quizId}/submit` - 퀴즈 제출

#### Chat (채팅)
- `GET /api/v1/chat/{chatRoomId}/messages` - 채팅 메시지 조회
- WebSocket: `/ws/chat` - 실시간 채팅

#### Attendance (출석)
- `POST /api/v1/attendance` - 출석 체크
- `GET /api/v1/attendance/{studyId}` - 출석 기록 조회

> 💡 **Tip**: 전체 API 명세는 Swagger UI에서 확인하세요!

## 📊 데이터베이스

### 연결 정보
```properties
URL: jdbc:mysql://localhost:3306/ssafy_web_db
Username: id
Password: password
```

### JPA 설정
- **DDL Auto**: update (자동 스키마 업데이트)
- **Dialect**: MySQL
- **Show SQL**: true (개발 환경)

## 🛠️ 기술 스택 상세

### 핵심 프레임워크
- **Spring Boot Web**: REST API 개발
- **Spring Boot WebSocket**: 실시간 통신
- **Spring Security**: 인증/인가
- **Spring Data JPA**: ORM 및 데이터 접근

### 데이터베이스
- **MySQL Connector**: MySQL 드라이버
- **QueryDSL**: 타입 안전 쿼리 작성

### 보안
- **JWT (auth0)**: 토큰 기반 인증

### 문서화
- **Swagger (springdoc-openapi)**: API 문서 자동 생성

### 유틸리티
- **Lombok**: 보일러플레이트 코드 감소

### 개발 도구
- **Spring DevTools**: 핫 리로드

## 🔄 WebSocket 통신

### 연결 엔드포인트
```
ws://localhost:8080/ws
```

### 주요 사용처
- 실시간 채팅 (`/topic/chat/{roomId}`)
- 화상 회의 시그널링 (`/topic/meeting/{meetingId}`)
- 알림 (`/topic/notification/{userId}`)

## 📝 개발 가이드

### 새로운 도메인 추가 시

1. **패키지 생성**
   ```
   domain/{new-domain}/
   ├── controller/
   ├── dto/request/
   ├── dto/response/
   ├── entity/
   ├── repository/
   └── service/
   ```

2. **Entity 작성**
   - `BaseEntity` 상속
   - JPA 어노테이션 사용
   - Lombok 활용

3. **Repository 작성**
   - `JpaRepository` 상속
   - 필요시 QueryDSL 사용

4. **Service 작성**
   - `@Service` 어노테이션
   - 비즈니스 로직 구현
   - `@Transactional` 적용

5. **Controller 작성**
   - `@RestController` 어노테이션
   - `@RequestMapping("/api/v1/{domain}")`
   - Swagger 어노테이션 추가

### 코딩 컨벤션

- **Entity**: PascalCase (예: `User`, `StudyMember`)
- **DTO**: PascalCase + Suffix (예: `UserRequest`, `UserResponse`)
- **Repository**: Entity명 + Repository (예: `UserRepository`)
- **Service**: Entity명 + Service (예: `UserService`)
- **Controller**: Entity명 + Controller (예: `UserController`)

## 🐛 로깅

### 로그 레벨
- **Root**: INFO
- **com.ssafy**: DEBUG
- **Spring Web**: DEBUG
- **Hibernate SQL**: DEBUG

### 로그 파일
- `ssafy-web.log` (프로젝트 루트)

## 🔧 환경 설정

### application.properties
주요 설정 항목:
- 서버 포트 및 인코딩
- 데이터베이스 연결 정보
- JPA/Hibernate 설정
- JWT 시크릿 및 토큰 유효기간
- Swagger 경로
- 로깅 레벨
- CORS 설정

## 📞 Frontend 연동 가이드

### 1. API 호출 예시 (JavaScript/Axios)

#### 로그인
```javascript
const login = async (email, password) => {
  const response = await axios.post('/api/v1/auth/login', {
    email,
    password
  });
  const { accessToken, refreshToken } = response.data;
  // 토큰 저장
  localStorage.setItem('accessToken', accessToken);
  localStorage.setItem('refreshToken', refreshToken);
};
```

#### 인증이 필요한 API 호출
```javascript
const fetchUserInfo = async (userId) => {
  const token = localStorage.getItem('accessToken');
  const response = await axios.get(`/api/v1/user/${userId}`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  return response.data;
};
```

### 2. WebSocket 연결 예시 (SockJS + STOMP)

```javascript
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, (frame) => {
// 채팅 메시지 구독
  stompClient.subscribe('/topic/chat/1', (message) => {
    const chatMessage = JSON.parse(message.body);
});
  
  // 메시지 전송
  stompClient.send('/app/chat/1', {}, JSON.stringify({
    content: 'Hello!',
    sender: 'user1'
  }));
});
```

### 3. 응답 데이터 구조

#### 성공 응답
```json
{
  "success": true,
  "data": {
    // 실제 데이터
  },
  "message": "Success"
}
```

#### 에러 응답
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지"
  }
}
```

#### 페이징 응답
```json
{
  "success": true,
  "data": {
    "content": [...],
    "totalElements": 100,
    "totalPages": 10,
    "size": 10,
    "number": 0
  }
}
```

## 🚨 주의사항

1. **JWT 토큰 관리**
   - Access Token 만료 시 Refresh Token으로 갱신
   - 로그아웃 시 토큰 삭제 필수

2. **CORS 설정**
   - 개발 환경에서는 모든 Origin 허용
   - 운영 환경에서는 특정 도메인만 허용 필요

3. **파일 업로드**
   - 최대 파일 크기 제한 확인
   - 허용된 파일 타입만 업로드

4. **WebSocket 연결**
   - 연결 끊김 시 재연결 로직 구현 필요
   - 하트비트 메시지로 연결 유지

## 📚 참고 자료

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security](https://spring.io/projects/spring-security)
- [Swagger/OpenAPI](https://springdoc.org/)
- [QueryDSL](http://querydsl.com/)

## 👥 개발팀 연락처

- Backend 팀: [연락처 추가]
- Frontend 팀: [연락처 추가]

---

**Last Updated**: 2026-01-17

