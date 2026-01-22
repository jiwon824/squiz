# 테스트 작성 가이드

## 개요

이 문서는 CI/CD 환경에서도 안정적으로 통과하는 테스트를 작성하기 위한 가이드입니다.
로컬에서 통과하지만 CI/CD에서 실패하는 문제를 예방하기 위한 핵심 원칙들을 정리했습니다.

---

## 핵심 원칙

### 1. FK 제약조건을 고려한 엔티티 생성 순서

**문제**: 하드코딩된 ID 사용 시 CI/CD에서 FK 제약조건 위반 발생

```java
// ❌ 잘못된 예시 - 하드코딩된 ID
StudyComment comment = StudyComment.builder()
        .studyId(1L)      // Study ID 1이 존재하지 않을 수 있음
        .userId(10L)      // User ID 10이 존재하지 않을 수 있음
        .content("댓글")
        .build();
```

```java
// ✅ 올바른 예시 - 실제 엔티티 생성 후 ID 사용
@Autowired
private UserRepository userRepository;

@Autowired
private StudyRepository studyRepository;

private User user;
private Study study;

@BeforeEach
void setUp() {
    // 1. 부모 엔티티 먼저 생성
    user = userRepository.save(User.builder()
            .userId("testuser")
            .email("test@test.com")
            .nickname("테스트유저")
            .name("테스트")
            .role(Role.USER)
            .isActive(true)
            .isOnline(false)
            .isSearchable(true)
            .totalExp(0)
            .currentPoints(0)
            .currentLevel(1)
            .levelName("Bronze")
            .build());

    // 2. flush()로 즉시 DB에 반영
    userRepository.flush();

    // 3. 자식 엔티티에서 실제 ID 사용
    study = studyRepository.save(Study.builder()
            .leaderId(user.getId())  // 실제 User ID 사용
            .name("테스트 스터디")
            .topic("Java")
            .studyType(StudyType.PLANNED)
            .build());

    studyRepository.flush();
}
```

### 2. flush() 사용 시점

**원칙**: 부모 엔티티 저장 후 자식 엔티티 저장 전에 `flush()` 호출

```java
// 엔티티 생성 순서
userRepository.save(user);
userRepository.flush();       // User INSERT 즉시 실행

studyRepository.save(study);
studyRepository.flush();      // Study INSERT 즉시 실행

commentRepository.save(comment);  // 이제 FK 제약조건 만족
```

**이유**: JPA는 트랜잭션 끝에 일괄 처리(batch)하므로, `flush()` 없이는 자식 엔티티 저장 시점에 부모가 아직 DB에 없을 수 있음

### 3. 벌크 삭제 후 영속성 컨텍스트 클리어

**문제**: 벌크 삭제 후 조회 시 `StaleObjectStateException` 발생

```java
// ❌ 잘못된 예시
commentRepository.deleteByStudyId(studyId);
// 영속성 컨텍스트에 삭제된 엔티티가 남아있음
List<StudyComment> result = commentRepository.findAll();  // StaleObjectStateException!
```

```java
// ✅ 올바른 예시
@Autowired
private EntityManager entityManager;

@Test
void deleteByStudyId_Success() {
    commentRepository.deleteByStudyId(studyId);
    entityManager.flush();   // 삭제 쿼리 즉시 실행
    entityManager.clear();   // 영속성 컨텍스트 클리어

    List<StudyComment> result = commentRepository.findAll();  // 정상 동작
}
```

### 4. 테스트 데이터 격리

**원칙**: 각 테스트는 독립적으로 실행 가능해야 함

```java
@SpringBootTest
@Transactional  // 테스트 후 롤백
class MyRepositoryTest {

    @BeforeEach
    void setUp() {
        // 매 테스트마다 필요한 데이터 생성
    }
}
```

### 5. 하드코딩 ID 사용 금지

```java
// ❌ 금지
Long userId = 10L;
Long studyId = 1L;

// ✅ 권장
Long userId = user.getId();
Long studyId = study.getId();
```

---

## CI/CD vs 로컬 환경 차이

| 항목 | 로컬 | CI/CD |
|------|------|-------|
| DB 상태 | 이전 데이터 남아있음 | 매번 DROP/CREATE |
| FK 체크 | 기존 데이터로 우회 가능 | 엄격하게 적용 |
| 테스트 순서 | IDE 순서 (예측 가능) | Gradle 스캔 순서 (비예측) |
| 캐시 | Gradle 데몬 캐시 있음 | 캐시 없음 |

---

## CI 환경 로컬 테스트 방법

CI/CD와 동일한 환경으로 로컬에서 테스트하려면:

### Windows
```batch
cd modustudy\backend
test-like-ci.bat                           # 전체 테스트
test-like-ci.bat StudyCommentRepositoryTest   # 특정 테스트
```

### Linux/Mac
```bash
cd modustudy/backend
./test-like-ci.sh                           # 전체 테스트
./test-like-ci.sh StudyCommentRepositoryTest   # 특정 테스트
```

이 스크립트는:
1. 테스트 DB를 DROP 후 새로 CREATE
2. CI/CD와 동일한 환경변수 설정
3. 테스트 실행

---

## 테스트 코드 체크리스트

테스트 작성 시 다음을 확인하세요:

- [ ] 하드코딩된 ID 대신 실제 엔티티 ID 사용
- [ ] 부모 엔티티 저장 후 `flush()` 호출
- [ ] 벌크 삭제 후 `entityManager.flush()` + `entityManager.clear()` 호출
- [ ] `@Transactional` 어노테이션 적용
- [ ] `@BeforeEach`에서 필요한 모든 의존 엔티티 생성
- [ ] `test-like-ci.bat` 또는 `test-like-ci.sh`로 CI 환경 테스트 수행

---

## 자주 발생하는 에러와 해결법

### 1. SQLIntegrityConstraintViolationException (FK 위반)

**원인**: 부모 엔티티 없이 자식 엔티티 저장 시도

**해결**:
```java
// 부모 먼저 저장 + flush
parentRepository.save(parent);
parentRepository.flush();
// 그 다음 자식 저장
childRepository.save(child);
```

### 2. StaleObjectStateException / ObjectOptimisticLockingFailureException

**원인**: 벌크 삭제 후 영속성 컨텍스트에 stale 엔티티 존재

**해결**:
```java
repository.deleteByXxx(id);
entityManager.flush();
entityManager.clear();
```

### 3. 로컬 통과, CI 실패

**원인**: 로컬 DB에 이전 테스트 데이터 존재

**해결**: `test-like-ci.bat` 스크립트로 CI 환경 재현 후 테스트

---

## 참고 자료

- [Spring Data JPA Testing](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#testing)
- [JPA 영속성 컨텍스트](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#pc)
