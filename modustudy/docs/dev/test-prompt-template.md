# 테스트 작성 프롬프트 템플릿

AI에게 테스트 작성을 요청할 때 아래 내용을 프롬프트에 포함하세요.

---

## 프롬프트 템플릿

```
[테스트 요청 내용]

## 테스트 작성 규칙 (필수 준수)

1. **하드코딩 ID 금지**: `userId(1L)`, `studyId(10L)` 같은 하드코딩 금지.
   반드시 실제 엔티티를 먼저 생성하고 `entity.getId()` 사용

2. **엔티티 생성 순서**:
   - User → Study → StudyComment 순서로 부모 먼저 생성
   - 각 부모 엔티티 저장 후 `repository.flush()` 호출

3. **벌크 삭제 테스트**:
   - `deleteByXxx()` 후 반드시 `entityManager.flush()` + `entityManager.clear()` 호출

4. **필수 import**:
   ```java
   import jakarta.persistence.EntityManager;
   ```

5. **필수 필드**:
   ```java
   @Autowired
   private EntityManager entityManager;
   ```

6. **User 엔티티 생성 예시**:
   ```java
   User user = userRepository.save(User.builder()
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
   userRepository.flush();
   ```
```

---

## 예시: StudyCommentRepository 테스트 요청

```
StudyCommentRepository에 대한 통합 테스트를 작성해줘.

테스트 케이스:
- 댓글 생성
- 댓글 조회
- 댓글 삭제

## 테스트 작성 규칙 (필수 준수)

1. **하드코딩 ID 금지**: `userId(1L)`, `studyId(10L)` 같은 하드코딩 금지.
   반드시 실제 엔티티를 먼저 생성하고 `entity.getId()` 사용

2. **엔티티 생성 순서**:
   - User → Study → StudyComment 순서로 부모 먼저 생성
   - 각 부모 엔티티 저장 후 `repository.flush()` 호출

3. **벌크 삭제 테스트**:
   - `deleteByXxx()` 후 반드시 `entityManager.flush()` + `entityManager.clear()` 호출

4. **필수 import**:
   ```java
   import jakarta.persistence.EntityManager;
   ```

5. **필수 필드**:
   ```java
   @Autowired
   private EntityManager entityManager;
   ```

6. **User 엔티티 생성 예시**:
   ```java
   User user = userRepository.save(User.builder()
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
   userRepository.flush();
   ```
```

---

## 간단 버전 (복사용)

```
[테스트 요청 내용]

테스트 규칙:
- 하드코딩 ID 금지 → 실제 엔티티 생성 후 getId() 사용
- 부모 엔티티 저장 후 flush() 호출
- 벌크 삭제 후 entityManager.flush() + clear() 호출
- User 생성 시 필수 필드: userId, email, nickname, name, role, isActive, isOnline, isSearchable, totalExp, currentPoints, currentLevel, levelName
```
