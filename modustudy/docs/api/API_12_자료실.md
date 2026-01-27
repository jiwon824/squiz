# 자료실(Material) 도메인 구현 완료 보고서

## 📋 구현 현황

### ✅ 완료된 항목

| Task ID | 설명 | 상태 |
|---------|------|------|
| SC-120-B-1 | MaterialRepository | ✅ 완료 |
| SC-120-B-2 | MaterialCommentRepository | ✅ 완료 |
| SC-120-B-3 | DTO (Request/Response) | ✅ 완료 |
| SC-120-B-4 | MaterialException | ✅ 완료 |
| SC-120-B-5 | MaterialService | ✅ 완료 |
| SC-120-B-6 | MaterialCommentService | ✅ 완료 |
| SC-120-B-10 | MaterialController | ✅ 완료 |
| SC-120-B-11 | 자료 CRUD API | ✅ 완료 (파일 업로드 제외) |
| SC-120-B-12 | 댓글 API | ✅ 완료 |
| SC-120-B-13 | Repository 테스트 | ✅ 완료 |
| SC-120-B-14 | Service 테스트 | ✅ 완료 |
| SC-120-B-15 | Controller 테스트 | ✅ 완료 |

### ⏳ 미완료 항목

| Task ID | 설명 | 상태 | 비고 |
|---------|------|------|------|
| SC-120-B-7 | FileStorageService | ⏳ 대기 | 파일 저장 방식 결정 필요 (로컬/S3) |
| SC-120-B-8 | 파일 업로드 API | ⏳ 대기 | FileStorageService 구현 후 |
| SC-120-B-9 | 파일 다운로드 API | ⏳ 대기 | FileStorageService 구현 후 |

---

## 📁 파일 구조

```
com.ssafy.domain.material/
├── controller/
│   ├── MaterialController.java
│   ├── MaterialControllerTest.java
│   ├── MaterialCommentController.java
│   └── MaterialCommentControllerTest.java
├── dto/
│   ├── request/
│   │   ├── MaterialCreateRequest.java
│   │   ├── MaterialUpdateRequest.java
│   │   ├── MaterialSearchCondition.java
│   │   ├── MaterialFileUploadRequest.java
│   │   └── MaterialCommentCreateRequest.java
│   └── response/
│       ├── MaterialListResponse.java
│       ├── MaterialDetailResponse.java
│       ├── MaterialCreateResponse.java
│       ├── MaterialCommentResponse.java
│       ├── MaterialCommentCreateResponse.java
│       └── UploaderInfo.java
├── entity/
│   ├── Material.java
│   ├── MaterialComment.java
│   └── MaterialType.java
├── repository/
│   ├── MaterialRepository.java
│   ├── MaterialRepositoryCustom.java
│   ├── MaterialRepositoryImpl.java
│   ├── MaterialRepositoryTest.java
│   ├── MaterialCommentRepository.java
│   └── MaterialCommentRepositoryTest.java
└── service/
    ├── MaterialService.java
    ├── MaterialServiceTest.java
    ├── MaterialCommentService.java
    └── MaterialCommentServiceTest.java

com.ssafy.common.exception/
└── MaterialException.java
```

---

## 🔌 API 엔드포인트

### 자료 API

| Method | Endpoint | 설명 | 구현 |
|--------|----------|------|------|
| GET | `/api/v1/studies/{studyId}/materials` | 자료 목록 조회 | ✅ |
| GET | `/api/v1/studies/{studyId}/materials/{materialId}` | 자료 상세 조회 | ✅ |
| POST | `/api/v1/studies/{studyId}/materials` | 링크 자료 생성 | ✅ |
| POST | `/api/v1/studies/{studyId}/materials` (multipart) | 파일 업로드 | ⏳ |
| PUT | `/api/v1/studies/{studyId}/materials/{materialId}` | 자료 수정 | ✅ |
| DELETE | `/api/v1/studies/{studyId}/materials/{materialId}` | 자료 삭제 | ✅ |

### 댓글 API

| Method | Endpoint | 설명 | 구현 |
|--------|----------|------|------|
| GET | `.../materials/{materialId}/comments` | 댓글 목록 | ✅ |
| POST | `.../materials/{materialId}/comments` | 댓글 작성 | ✅ |
| DELETE | `.../materials/{materialId}/comments/{commentId}` | 댓글 삭제 | ✅ |

---

## 🔍 API 명세 vs 구현 비교

### 차이점

| 항목 | API 명세 | 현재 구현 | 비고 |
|------|----------|-----------|------|
| 응답 래퍼 | `{"success": true, "data": {...}}` | 직접 데이터 반환 | 프로젝트 패턴 준수 |
| 인증 | JWT Bearer Token | `User-Id` 헤더 | JWT 구현 후 수정 필요 |
| 파일 업로드 | multipart/form-data | 미구현 | FileStorageService 필요 |
| fileUrl 필드 | S3 URL 반환 | filePath 반환 | 파일 서비스 구현 후 수정 |

### 응답 필드 매핑

**MaterialListResponse:**
- ✅ id, title, description, materialType, weekNumber, viewCount, commentCount, createdAt
- ✅ uploader (id, nickname, profileImage)
- ⏳ fileUrl (현재 url/filePath로 분리)

**MaterialDetailResponse:**
- ✅ id, title, description, materialType, weekNumber, viewCount, createdAt
- ✅ uploader, fileName
- ⏳ fileUrl, updatedAt (엔티티에 없음)

---

## 🧪 테스트 현황

### Repository 테스트
- MaterialRepositoryTest: 7개 @Nested 클래스, 20+ 테스트
- MaterialCommentRepositoryTest: 6개 @Nested 클래스, 15+ 테스트

### Service 테스트
- MaterialServiceTest: 6개 @Nested 클래스
- MaterialCommentServiceTest: 4개 @Nested 클래스

### Controller 테스트
- MaterialControllerTest: 5개 @Nested 클래스
- MaterialCommentControllerTest: 3개 @Nested 클래스

---

## 📝 TODO (향후 작업)

### 1. FileStorageService 구현 (파일 저장 방식 결정 후)

```java
public interface FileStorageService {
    // 파일 업로드
    FileUploadResult upload(MultipartFile file, String directory);
    
    // 파일 삭제
    void delete(String filePath);
    
    // 파일 URL 생성
    String getFileUrl(String filePath);
}
```

**옵션:**
- A) 로컬 저장: `/uploads/materials/` 디렉토리
- B) AWS S3: 클라우드 스토리지

### 2. 파일 업로드 API 추가

```java
@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<MaterialCreateResponse> uploadFileMaterial(
        @PathVariable Long studyId,
        @RequestHeader("User-Id") Long userId,
        @RequestParam("file") MultipartFile file,
        @Valid @ModelAttribute MaterialFileUploadRequest request) {
    // FileStorageService로 파일 저장
    // MaterialService.createFileMaterial() 호출
}
```

### 3. JWT 인증 적용 (인증 모듈 완성 후)

```java
// 현재
@RequestHeader("User-Id") Long userId

// 변경 예정
@AuthenticationPrincipal UserDetails userDetails
Long userId = userDetails.getId();
```

### 4. 스터디 멤버 검증 추가

```java
// 스터디 멤버만 자료 접근 가능하도록 검증
if (!studyMemberService.isMember(studyId, userId)) {
    throw new StudyException.NotStudyMemberException();
}
```

---

## 📌 참고사항

### 헤더 규칙
- `User-Id`: 사용자 ID (Long)
- `Is-Leader`: 스터디장 여부 (Boolean, 삭제 시 사용)

### 에러 코드
| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| MATERIAL_NOT_FOUND | 404 | 자료를 찾을 수 없음 |
| NOT_MATERIAL_OWNER | 403 | 본인 자료만 수정/삭제 가능 |
| FILE_SIZE_EXCEEDED | 400 | 파일 크기 초과 |
| INVALID_FILE_TYPE | 400 | 지원하지 않는 파일 형식 |
| COMMENT_NOT_FOUND | 404 | 댓글을 찾을 수 없음 |
| NOT_COMMENT_AUTHOR | 403 | 본인 댓글만 삭제 가능 |

---

## 🗓️ 완료일
- 2025-01-26