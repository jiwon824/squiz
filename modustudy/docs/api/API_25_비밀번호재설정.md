# 비밀번호 재설정 API 문서

## 📋 개요

사용자가 비밀번호를 잊어버렸을 때 이메일 인증을 통해 비밀번호를 재설정할 수 있는 기능입니다.

### 주요 특징
- 이메일 기반 인증
- 토큰 유효시간: 30분
- 일회용 토큰 (사용 후 만료)
- 비밀번호 변경 시 모든 리프레시 토큰 삭제 (보안)

---

## 🔄 프로세스 흐름

```
1. 사용자가 이메일 입력
   ↓
2. 백엔드에서 재설정 토큰 생성 및 이메일 전송
   ↓
3. 사용자가 이메일에서 링크 클릭
   ↓
4. 프론트엔드에서 토큰 검증
   ↓
5. 새 비밀번호 입력
   ↓
6. 백엔드에서 비밀번호 업데이트
   ↓
7. 완료 (새 비밀번호로 로그인 가능)
```

---

## 📡 API 엔드포인트

### 1. 비밀번호 재설정 요청 (이메일 전송)

**Endpoint:** `POST /api/v1/auth/password/reset-request`

**Description:** 사용자 이메일로 비밀번호 재설정 링크를 전송합니다.

**Request Body:**
```json
{
  "email": "user@example.com"
}
```

**Response (Success):**
```json
{
  "success": true,
  "data": {
    "message": "비밀번호 재설정 이메일이 전송되었습니다."
  }
}
```

**Response (Error - 계정 없음):**
```json
{
  "success": false,
  "error": {
    "code": "USER_NOT_FOUND",
    "message": "해당 이메일로 가입된 계정이 없습니다."
  }
}
```

**Response (Error - 비밀번호 미설정):**
```json
{
  "success": false,
  "error": {
    "code": "PASSWORD_NOT_SET",
    "message": "비밀번호가 설정되지 않은 계정입니다. 프로필 설정을 먼저 완료해주세요."
  }
}
```

---

### 2. 토큰 유효성 검증

**Endpoint:** `GET /api/v1/auth/password/reset/verify`

**Description:** 재설정 토큰이 유효한지 확인합니다.

**Query Parameters:**
- `token` (required): 이메일로 받은 재설정 토큰

**Example:**
```
GET /api/v1/auth/password/reset/verify?token=550e8400-e29b-41d4-a716-446655440000
```

**Response (유효한 토큰):**
```json
{
  "success": true,
  "data": true
}
```

**Response (만료되거나 사용된 토큰):**
```json
{
  "success": true,
  "data": false
}
```

---

### 3. 비밀번호 재설정 실행

**Endpoint:** `POST /api/v1/auth/password/reset`

**Description:** 새로운 비밀번호로 변경합니다.

**Request Body:**
```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "newPassword": "newSecurePassword123!"
}
```

**Response (Success):**
```json
{
  "success": true,
  "data": {
    "message": "비밀번호가 성공적으로 변경되었습니다."
  }
}
```

**Response (Error - 유효하지 않은 토큰):**
```json
{
  "success": false,
  "error": {
    "code": "INVALID_TOKEN",
    "message": "유효하지 않은 토큰입니다."
  }
}
```

**Response (Error - 만료된 토큰):**
```json
{
  "success": false,
  "error": {
    "code": "TOKEN_EXPIRED",
    "message": "만료되었거나 이미 사용된 토큰입니다."
  }
}
```

---

## 🗄️ 데이터베이스 스키마

### password_reset_token 테이블

```sql
CREATE TABLE password_reset_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id)
);
```

**컬럼 설명:**
- `id`: 고유 식별자
- `user_id`: 사용자 ID (FK)
- `token`: UUID 형식의 재설정 토큰
- `expires_at`: 토큰 만료 시간 (생성 후 30분)
- `used`: 토큰 사용 여부
- `created_at`: 토큰 생성 시간

---

## 📧 이메일 템플릿

### 비밀번호 재설정 이메일

**제목:** [SQUIZ] 비밀번호 재설정 안내

**본문:**
```
안녕하세요, SQUIZ입니다.

비밀번호 재설정을 요청하셨습니다.
아래 링크를 클릭하여 새 비밀번호를 설정해주세요.

http://localhost:3000/password/reset?token={token}

본 링크는 30분간 유효합니다.
요청하지 않으셨다면 이 메일을 무시해주세요.

감사합니다.
```

---

## ⚙️ 설정 (application.properties)

```properties
# 이메일 설정 (Gmail)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# 프론트엔드 URL
app.frontend.url=http://localhost:3000

# OAuth2 설정 (카카오, 네이버, 구글)
spring.security.oauth2.client.registration.kakao.client-id=${KAKAO_REST_API_KEY}
spring.security.oauth2.client.registration.kakao.client-secret=${KAKAO_CLIENT_SECRET_KEY}
# ... (나머지 OAuth2 설정)
```

### 환경 변수 (.env)

```env
# Gmail 앱 비밀번호
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# OAuth2 Credentials
KAKAO_REST_API_KEY=xxx
KAKAO_CLIENT_SECRET_KEY=xxx
NAVER_CLIENT_ID=xxx
NAVER_CLIENT_SECRET=xxx
GOOGLE_CLIENT_ID=xxx
GOOGLE_CLIENT_SECRET=xxx
```

---

## 🔐 보안 고려사항

### 1. 토큰 보안
- UUID v4 사용 (추측 불가능)
- 30분 유효기간
- 일회용 (사용 후 `used = true`)
- DB에 평문 저장 (이메일로만 전송되므로 안전)

### 2. 비밀번호 변경 시
- 기존 모든 리프레시 토큰 삭제
- 사용자는 모든 기기에서 재로그인 필요
- 비밀번호는 BCrypt로 암호화 저장

### 3. 이메일 검증
- 가입된 이메일만 재설정 가능
- 비밀번호가 설정된 계정만 재설정 가능
- 소셜 로그인 전용 계정은 프로필 설정 필요

---

## 🧪 테스트 시나리오

### 시나리오 1: 정상적인 비밀번호 재설정

```bash
# 1. 재설정 요청
curl -X POST http://localhost:8080/api/v1/auth/password/reset-request \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com"}'

# 2. 이메일에서 토큰 확인

# 3. 토큰 검증
curl -X GET "http://localhost:8080/api/v1/auth/password/reset/verify?token=YOUR_TOKEN"

# 4. 비밀번호 변경
curl -X POST http://localhost:8080/api/v1/auth/password/reset \
  -H "Content-Type: application/json" \
  -d '{
    "token":"YOUR_TOKEN",
    "newPassword":"newPassword123!"
  }'

# 5. 새 비밀번호로 로그인
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email":"user@example.com",
    "password":"newPassword123!"
  }'
```

### 시나리오 2: 만료된 토큰

```bash
# 30분 경과 후 토큰 사용 시도
curl -X POST http://localhost:8080/api/v1/auth/password/reset \
  -H "Content-Type: application/json" \
  -d '{
    "token":"EXPIRED_TOKEN",
    "newPassword":"newPassword123!"
  }'

# 예상 응답: TOKEN_EXPIRED 에러
```

### 시나리오 3: 이미 사용된 토큰

```bash
# 같은 토큰으로 두 번째 시도
curl -X POST http://localhost:8080/api/v1/auth/password/reset \
  -H "Content-Type: application/json" \
  -d '{
    "token":"USED_TOKEN",
    "newPassword":"anotherPassword123!"
  }'

# 예상 응답: TOKEN_EXPIRED 에러
```

---

## 📊 에러 코드 정리

| 에러 코드 | HTTP Status | 설명 |
|---------|------------|------|
| USER_NOT_FOUND | 400 | 해당 이메일로 가입된 계정이 없음 |
| PASSWORD_NOT_SET | 400 | 비밀번호가 설정되지 않은 계정 (프로필 설정 필요) |
| INVALID_TOKEN | 400 | 유효하지 않은 토큰 |
| TOKEN_EXPIRED | 400 | 만료되었거나 이미 사용된 토큰 |

---

## 🚀 배포 시 변경사항

### application.properties (배포 환경)

```properties
# 프론트엔드 URL (배포 환경)
app.frontend.url=https://i14d106.p.ssafy.io

# OAuth2 Redirect URI (배포 환경)
spring.security.oauth2.client.registration.kakao.redirect-uri=https://i14d106.p.ssafy.io/login/callback
spring.security.oauth2.client.registration.naver.redirect-uri=https://i14d106.p.ssafy.io/login/callback
spring.security.oauth2.client.registration.google.redirect-uri=https://i14d106.p.ssafy.io/login/callback
```

### 이메일 템플릿 링크

배포 시 이메일 링크가 자동으로 변경됩니다:
```
https://i14d106.p.ssafy.io/password/reset?token={token}
```

---

## 📝 프론트엔드 연동 가이드

### 1. 비밀번호 재설정 요청 페이지

```typescript
// /password/forgot 페이지
const handleSubmit = async (email: string) => {
  try {
    await authApi.requestPasswordReset(email);
    alert('비밀번호 재설정 이메일이 전송되었습니다.');
  } catch (error) {
    alert('이메일 전송에 실패했습니다.');
  }
};
```

### 2. 비밀번호 재설정 페이지

```typescript
// /password/reset?token=xxx 페이지
const handleResetPassword = async (token: string, newPassword: string) => {
  try {
    // 1. 토큰 검증
    const isValid = await authApi.verifyResetToken(token);
    if (!isValid) {
      alert('유효하지 않거나 만료된 링크입니다.');
      return;
    }
    
    // 2. 비밀번호 재설정
    await authApi.resetPassword(token, newPassword);
    alert('비밀번호가 변경되었습니다. 로그인해주세요.');
    navigate('/login');
  } catch (error) {
    alert('비밀번호 변경에 실패했습니다.');
  }
};
```

### 3. API 함수 (authApi.ts)

```typescript
export const authApi = {
  // 비밀번호 재설정 요청
  requestPasswordReset: async (email: string) => {
    const response = await api.post('/api/v1/auth/password/reset-request', { email });
    return response.data;
  },
  
  // 토큰 검증
  verifyResetToken: async (token: string) => {
    const response = await api.get(`/api/v1/auth/password/reset/verify?token=${token}`);
    return response.data.data; // boolean
  },
  
  // 비밀번호 재설정
  resetPassword: async (token: string, newPassword: string) => {
    const response = await api.post('/api/v1/auth/password/reset', {
      token,
      newPassword
    });
    return response.data;
  }
};
```

---

## ✅ 체크리스트

### 백엔드
- [x] PasswordResetToken 엔티티 생성
- [x] PasswordResetTokenRepository 생성
- [x] EmailService 구현
- [x] OAuth2Service에 비밀번호 재설정 로직 추가
- [x] AuthController에 API 엔드포인트 추가
- [x] SecurityConfig에 경로 허용 추가
- [x] application.properties 설정
- [x] Gmail 앱 비밀번호 발급

### 프론트엔드
- [ ] 비밀번호 찾기 페이지 구현
- [ ] 비밀번호 재설정 페이지 구현
- [ ] API 연동
- [ ] 에러 핸들링
- [ ] UX/UI 개선

### 배포
- [ ] 환경변수 설정 (MAIL_USERNAME, MAIL_PASSWORD)
- [ ] 프론트엔드 URL 변경
- [ ] OAuth2 Redirect URI 업데이트
- [ ] 이메일 발송 테스트

---

## 🔍 트러블슈팅

### Q1. 이메일이 전송되지 않아요
**A:** Gmail 앱 비밀번호를 제대로 설정했는지 확인하세요.
- 2단계 인증 활성화 필요
- 앱 비밀번호는 공백 없이 16자리
- `.env` 파일에 올바르게 입력

### Q2. "비밀번호가 설정되지 않은 계정" 에러
**A:** 소셜 로그인으로만 가입하고 프로필 설정을 하지 않은 경우입니다.
- 프로필 설정에서 비밀번호를 먼저 설정해야 함
- 또는 소셜 로그인으로 계속 이용 가능

### Q3. 토큰이 만료되었다고 나와요
**A:** 토큰 유효시간은 30분입니다.
- 이메일을 다시 요청하세요
- 새로운 토큰으로 재시도

### Q4. 비밀번호 변경 후 로그인이 안돼요
**A:** 모든 기기에서 재로그인이 필요합니다.
- 보안을 위해 기존 세션이 모두 만료됨
- 새 비밀번호로 다시 로그인

---

## 📚 참고 자료

- [Spring Mail 공식 문서](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#mail)
- [Gmail 앱 비밀번호 설정](https://support.google.com/accounts/answer/185833)
- [BCrypt 암호화](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)

---

**작성일:** 2026-01-21
**버전:** 1.0.0
