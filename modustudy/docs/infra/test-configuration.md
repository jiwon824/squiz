# 테스트 환경 설정 가이드

## 개요

CI/CD 파이프라인에서 테스트를 MySQL 데이터베이스로 실행하기 위한 설정입니다.
H2 인메모리 DB 대신 실제 MySQL을 사용하여 운영 환경과 동일한 조건에서 테스트합니다.

### 테스트 유형

| 유형 | DB 필요 | 속도 | 용도 |
|------|---------|------|------|
| 단위 테스트 (Mockito) | X | 빠름 | Service 로직, 비즈니스 규칙 검증 |
| 통합 테스트 (JPA) | O | 느림 | JPA Repository CRUD 검증 |
| 통합 테스트 (MyBatis) | O | 느림 | MyBatis Mapper SQL 검증 |

## 변경 사항

### 1. 테스트용 application.properties 생성

**파일 위치**: `src/test/resources/application.properties`

```properties
# Database 설정 (환경변수로 오버라이드 가능)
spring.datasource.url=${TEST_DB_URL:jdbc:mysql://localhost:3306/ssafy_test_db?...}
spring.datasource.username=${TEST_DB_USERNAME:ssafy}
spring.datasource.password=${TEST_DB_PASSWORD:ssafy}

# JPA - 테스트 시 테이블 자동 생성
spring.jpa.generate-ddl=true
spring.jpa.hibernate.ddl-auto=create

# MyBatis 설정 (MyBatis Mapper 통합 테스트용)
mybatis.mapper-locations=classpath:mapper/**/*.xml
mybatis.configuration.map-underscore-to-camel-case=true
mybatis.configuration.default-fetch-size=100
mybatis.configuration.default-statement-timeout=30
```

### 2. 테스트 클래스 Security 설정

Spring Security 인증을 우회하기 위해 테스트 클래스에 `@WithMockUser` 추가:

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "testuser", roles = {"USER"})
class StudyControllerTest {
    // ...
}
```

**적용된 테스트 클래스**:
- `StudyControllerTest`
- `ApplicationControllerTest`
- `LeaderControllerTest`

### 3. 삭제된 파일

- `src/test/resources/application-test.properties` - 프로필 충돌 방지를 위해 삭제

### 4. Mockito 단위 테스트 설정

DB 연결 없이 Service 로직을 테스트할 때 사용:

```java
@ExtendWith(MockitoExtension.class)
class FriendServiceTest {
    @Mock
    private FriendshipMapper friendshipMapper;

    @InjectMocks
    private FriendService friendService;

    @Test
    void testMethod() {
        // given
        given(friendshipMapper.findById(1L)).willReturn(mockFriendship);

        // when
        var result = friendService.getFriend(1L);

        // then
        assertThat(result).isNotNull();
        verify(friendshipMapper, times(1)).findById(1L);
    }
}
```

**장점**: DB 없이 실행 가능, 빠른 속도, CI/CD에서 안정적으로 실행

### 5. MyBatis Mapper 통합 테스트 설정

실제 DB에서 SQL을 검증할 때 사용:

```java
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FriendshipMapperTest {
    @Autowired
    private FriendshipMapper friendshipMapper;

    @Test
    void findByUsers_ShouldReturnFriendship() {
        // 실제 DB에 데이터 insert 후 조회 테스트
    }
}
```

**주의**: `@MybatisTest`는 `mybatis-spring-boot-starter-test` 의존성 필요

## 환경별 설정

### 로컬 개발 환경

기본값 사용 (별도 설정 불필요):
- DB URL: `localhost:3306/ssafy_test_db`
- Username: `ssafy`
- Password: `ssafy`

**테스트 DB 생성**:
```bash
mysql -u ssafy -pssafy -e "CREATE DATABASE IF NOT EXISTS ssafy_test_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

**테스트 실행**:
```bash
./gradlew test
```

### EC2 CI 환경

`/etc/environment` 또는 GitLab CI 변수에 설정:

```bash
# /etc/environment
TEST_DB_URL=jdbc:mysql://localhost:3306/squiz_test?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true&useSSL=false
TEST_DB_USERNAME=ssafy
TEST_DB_PASSWORD=ssafy
```

**테스트 DB 생성** (EC2 MySQL 컨테이너):
```bash
docker exec -it mysql mysql -u ssafy -pssafy -e "CREATE DATABASE IF NOT EXISTS squiz_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

## 트러블슈팅

### 1. 테이블 생성 안 됨

**증상**: `Table 'xxx' doesn't exist` 에러

**해결**:
- `spring.jpa.hibernate.ddl-auto=create` 확인
- `spring.jpa.generate-ddl=true` 확인
- MySQL dialect 자동 감지 (명시적 설정 불필요)

### 2. 403 Forbidden 에러

**증상**: 테스트에서 API 호출 시 403 에러

**해결**:
- 테스트 클래스에 `@WithMockUser` 추가
- import: `org.springframework.security.test.context.support.WithMockUser`

### 3. 환경변수 인식 안 됨

**증상**: `PropertyPlaceholderHelper` 에러

**해결**:
- 환경변수에 기본값 설정: `${VAR_NAME:defaultValue}`
- build.gradle에 `spring-dotenv` 의존성 추가

## 의존성

```groovy
// build.gradle
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'org.springframework.security:spring-security-test'
testImplementation 'me.paulschwarz:spring-dotenv:4.0.0'

// MyBatis 통합 테스트용 (선택사항 - @MybatisTest 사용 시 필요)
testImplementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter-test:3.0.3'
```

## CI/CD 파이프라인에서의 테스트

### 권장 테스트 전략

1. **단위 테스트 (필수)**: Mockito 기반, DB 불필요
   - Service 로직 검증
   - 비즈니스 규칙 검증
   - 예외 처리 검증

2. **통합 테스트 (선택)**: 실제 DB 필요
   - SQL 쿼리 검증
   - 트랜잭션 동작 검증

### GitLab CI 설정 예시

```yaml
test:
  stage: test
  script:
    - ./gradlew test
  variables:
    TEST_DB_URL: jdbc:mysql://mysql:3306/test_db
    TEST_DB_USERNAME: test
    TEST_DB_PASSWORD: test
```
