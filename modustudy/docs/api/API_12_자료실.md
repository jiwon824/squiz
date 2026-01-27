# 자료실(Material) 도메인 구현 완료 보고서

## 📋 구현 현황

### ✅ 전체 완료!

| Task ID | 설명 | 상태 |
|---------|------|------|
| SC-120-B-1 | MaterialRepository | ✅ 완료 |
| SC-120-B-2 | MaterialCommentRepository | ✅ 완료 |
| SC-120-B-3 | DTO (Request/Response) | ✅ 완료 |
| SC-120-B-4 | MaterialException | ✅ 완료 |
| SC-120-B-5 | MaterialService | ✅ 완료 |
| SC-120-B-6 | MaterialCommentService | ✅ 완료 |
| SC-120-B-7 | FileStorageService | ✅ 완료 |
| SC-120-B-8 | 파일 업로드 API | ✅ 완료 |
| SC-120-B-9 | 파일 다운로드 API | ✅ 완료 |
| SC-120-B-10 | MaterialController | ✅ 완료 |
| SC-120-B-11 | 자료 CRUD API | ✅ 완료 |
| SC-120-B-12 | 댓글 API | ✅ 완료 |
| SC-120-B-13 | Repository 테스트 | ✅ 완료 |
| SC-120-B-14 | Service 테스트 | ✅ 완료 |
| SC-120-B-15 | Controller 테스트 | ✅ 완료 |

---

## 📁 파일 구조

```
com.ssafy.domain.material/
├── controller/
│   ├── MaterialController.java          # 자료 CRUD + 파일 업로드
│   ├── MaterialControllerTest.java
│   ├── MaterialCommentController.java   # 댓글 CRUD
│   ├── MaterialCommentControllerTest.java
│   └── FileController.java              # 파일 다운로드 🆕
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
    ├── FileStorageService.java           # 파일 저장 인터페이스 🆕
    ├── MaterialFileStorageService.java   # 파일 저장 구현체 🆕
    ├── MaterialFileStorageServiceTest.java 🆕
    ├── MaterialService.java
    ├── MaterialServiceTest.java
    ├── MaterialCommentService.java
    └── MaterialCommentServiceTest.java

com.ssafy.common.exception/
└── MaterialException.java
    ├── MaterialNotFoundException
    ├── NotMaterialOwnerException
    ├── CommentNotFoundException
    ├── NotCommentAuthorException
    ├── FileUploadFailedException        🆕
    └── InvalidFileTypeException         🆕
```

---

## 🔌 API 엔드포인트

### 자료 API

| Method | Endpoint | 설명 | 구현 |
|--------|----------|------|------|
| GET | `/api/v1/studies/{studyId}/materials` | 자료 목록 조회 | ✅ |
| GET | `/api/v1/studies/{studyId}/materials/{materialId}` | 자료 상세 조회 | ✅ |
| POST | `/api/v1/studies/{studyId}/materials` | 링크 자료 생성 | ✅ |
| POST | `/api/v1/studies/{studyId}/materials/upload` | 파일 업로드 | ✅ 🆕 |
| PUT | `/api/v1/studies/{studyId}/materials/{materialId}` | 자료 수정 | ✅ |
| DELETE | `/api/v1/studies/{studyId}/materials/{materialId}` | 자료 삭제 | ✅ |

### 파일 다운로드 API 🆕

| Method | Endpoint | 설명 | 구현 |
|--------|----------|------|------|
| GET | `/files/{path}` | 파일 조회 (브라우저에서 열기) | ✅ |
| GET | `/files/{path}?download=true` | 파일 다운로드 | ✅ |

### 댓글 API

| Method | Endpoint | 설명 | 구현 |
|--------|----------|------|------|
| GET | `/api/v1/materials/{materialId}/comments` | 댓글 목록 | ✅ |
| POST | `/api/v1/materials/{materialId}/comments` | 댓글 작성 | ✅ |
| PUT | `/api/v1/materials/comments/{commentId}` | 댓글 수정 | ✅ |
| DELETE | `/api/v1/materials/comments/{commentId}` | 댓글 삭제 | ✅ |

---

## 📤 파일 업로드 기능 상세

### 파일 업로드 API

```
POST /api/v1/studies/{studyId}/materials/upload
Content-Type: multipart/form-data

Headers:
  User-Id: {userId}

Body (form-data):
  file: (파일)
  title: 알고리즘 정리 노트
  description: 파일 업로드 테스트 (선택)
  weekNumber: 1 (선택)
```

### 응답 예시

```json
{
  "id": 1,
  "title": "알고리즘 정리 노트",
  "materialType": "FILE",
  "filePath": "materials/study_1/28cc7d16-e021-4f8e-a8d4-4a15d35e316f.xlsx",
  "createdAt": "2026-01-27T17:45:44"
}
```

### 지원 파일 형식

| 카테고리 | 확장자 | MaterialType |
|----------|--------|--------------|
| 문서 | pdf, doc, docx, ppt, pptx, xls, xlsx, txt, md | FILE |
| 이미지 | jpg, jpeg, png, gif, webp, svg | IMAGE |
| 영상 | mp4, avi, mov, wmv | VIDEO |
| 압축 | zip, rar, 7z | FILE |

### 파일 저장 경로

```
./uploads/
└── materials/
    └── study_{studyId}/
        └── {UUID}.{extension}
```

---

## 📥 파일 다운로드 기능 상세

### 파일 조회 (브라우저에서 열기)

```
GET /files/materials/study_1/28cc7d16-e021-4f8e-a8d4-4a15d35e316f.xlsx
```

### 파일 다운로드 (다운로드 창)

```
GET /files/materials/study_1/28cc7d16-e021-4f8e-a8d4-4a15d35e316f.xlsx?download=true
```

### 보안

- uploadDir 외부 경로 접근 차단
- Path Traversal 공격 방지

---

## 🧪 테스트 현황

### Repository 테스트
- MaterialRepositoryTest: 7개 @Nested 클래스, 20+ 테스트 ✅
- MaterialCommentRepositoryTest: 6개 @Nested 클래스, 15+ 테스트 ✅

### Service 테스트
- MaterialServiceTest: 6개 @Nested 클래스 ✅
- MaterialCommentServiceTest: 4개 @Nested 클래스 ✅
- MaterialFileStorageServiceTest: 4개 @Nested 클래스 ✅ 🆕
  - 파일 업로드 테스트 (다양한 형식)
  - 파일 삭제 테스트
  - 파일 URL 생성 테스트
  - MaterialType 감지 테스트

### Controller 테스트
- MaterialControllerTest: 5개 @Nested 클래스 ✅
- MaterialCommentControllerTest: 3개 @Nested 클래스 ✅

### 포스트맨 테스트 ✅ 🆕
- 파일 업로드 성공 확인
- 파일 다운로드 성공 확인

---

## ⚙️ Security 설정

`SecurityConfig.java`에 추가 필요:

```java
.requestMatchers("/files/**").permitAll()
```

---

## 📝 에러 코드

| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| MATERIAL_NOT_FOUND | 404 | 자료를 찾을 수 없음 |
| NOT_MATERIAL_OWNER | 403 | 본인 자료만 수정/삭제 가능 |
| FILE_UPLOAD_FAILED | 400 | 파일 업로드 실패 🆕 |
| INVALID_FILE_TYPE | 400 | 지원하지 않는 파일 형식 🆕 |
| COMMENT_NOT_FOUND | 404 | 댓글을 찾을 수 없음 |
| NOT_COMMENT_AUTHOR | 403 | 본인 댓글만 삭제 가능 |

---

## 📌 참고사항

### 헤더 규칙
- `User-Id`: 사용자 ID (Long)
- `Is-Leader`: 스터디장 여부 (Boolean, 삭제 시 사용)

### 파일 크기 제한
- `application.properties` 설정:
```properties
spring.servlet.multipart.max-file-size=1GB
spring.servlet.multipart.max-request-size=1GB
```

### 기존 LocalFileStorageService와의 관계
- `com.ssafy.common.storage.LocalFileStorageService`: Meeting용 파일 저장
- `com.ssafy.domain.material.service.MaterialFileStorageService`: Material용 파일 저장 🆕

---

## 🗓️ 완료일
- Phase 1 (CRUD): 2025-01-26
- Phase 2 (파일 업로드/다운로드): 2025-01-27 ✅