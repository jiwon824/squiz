## 26. 지역(Region) API

스터디 지역 정보를 관리하는 API입니다. 시/도(Level 1)와 시/군/구(Level 2) 계층 구조를 지원합니다.

### Base URL: `/api/regions`

---

### 26.1 모든 시/도 목록 조회

**Endpoint:** `GET /api/regions/provinces`

**설명:** 전국 17개 시/도 목록을 조회합니다.

**인증:** 불필요

**Request Example:**
```
GET /api/regions/provinces
```

**Response:**
```json
{
  "data": [
    {
      "id": 1,
      "code": "SEOUL",
      "name": "서울특별시",
      "fullName": "서울특별시",
      "level": 1,
      "parentId": null
    },
    {
      "id": 2,
      "code": "BUSAN",
      "name": "부산광역시",
      "fullName": "부산광역시",
      "level": 1,
      "parentId": null
    },
    {
      "id": 3,
      "code": "DAEGU",
      "name": "대구광역시",
      "fullName": "대구광역시",
      "level": 1,
      "parentId": null
    }
  ]
}
```

---

### 26.2 시/도 드롭다운 옵션 조회

**Endpoint:** `GET /api/regions/provinces/options`

**설명:** 드롭다운 선택용 시/도 목록을 조회합니다.

**인증:** 불필요

**Request Example:**
```
GET /api/regions/provinces/options
```

**Response:**
```json
{
  "data": [
    {
      "id": 1,
      "code": "SEOUL",
      "label": "서울특별시"
    },
    {
      "id": 2,
      "code": "BUSAN",
      "label": "부산광역시"
    },
    {
      "id": 3,
      "code": "DAEGU",
      "label": "대구광역시"
    }
  ]
}
```

---

### 26.3 시/도 + 하위 지역 전체 조회 (계층 구조)

**Endpoint:** `GET /api/regions/hierarchy`

**설명:** 모든 시/도와 하위 시/군/구를 계층 구조로 조회합니다.

**인증:** 불필요

**Request Example:**
```
GET /api/regions/hierarchy
```

**Response:**
```json
{
  "data": [
    {
      "id": 1,
      "code": "SEOUL",
      "name": "서울특별시",
      "districts": [
        {
          "id": 18,
          "code": "SEOUL_GANGNAM",
          "name": "강남구",
          "fullName": "서울특별시 강남구"
        },
        {
          "id": 19,
          "code": "SEOUL_GANGDONG",
          "name": "강동구",
          "fullName": "서울특별시 강동구"
        },
        {
          "id": 35,
          "code": "SEOUL_SONGPA",
          "name": "송파구",
          "fullName": "서울특별시 송파구"
        }
      ]
    },
    {
      "id": 2,
      "code": "BUSAN",
      "name": "부산광역시",
      "districts": [
        {
          "id": 43,
          "code": "BUSAN_HAEUNDAE",
          "name": "해운대구",
          "fullName": "부산광역시 해운대구"
        }
      ]
    }
  ]
}
```

---

### 26.4 특정 시/도의 시/군/구 목록 조회

**Endpoint:** `GET /api/regions/provinces/{provinceId}/districts`

**설명:** 특정 시/도에 속한 시/군/구 목록을 조회합니다.

**인증:** 불필요

**Path Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| provinceId | Long | Y | 시/도 ID |

**Request Example:**
```
GET /api/regions/provinces/1/districts
```

**Response:**
```json
{
  "data": [
    {
      "id": 18,
      "code": "SEOUL_GANGNAM",
      "name": "강남구",
      "fullName": "서울특별시 강남구",
      "level": 2,
      "parentId": 1
    },
    {
      "id": 19,
      "code": "SEOUL_GANGDONG",
      "name": "강동구",
      "fullName": "서울특별시 강동구",
      "level": 2,
      "parentId": 1
    }
  ]
}
```

---

### 26.5 특정 시/도의 시/군/구 드롭다운 옵션 조회

**Endpoint:** `GET /api/regions/provinces/{provinceId}/districts/options`

**설명:** 드롭다운 선택용 시/군/구 목록을 조회합니다.

**인증:** 불필요

**Path Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| provinceId | Long | Y | 시/도 ID |

**Request Example:**
```
GET /api/regions/provinces/1/districts/options
```

**Response:**
```json
{
  "data": [
    {
      "id": 18,
      "code": "SEOUL_GANGNAM",
      "label": "강남구"
    },
    {
      "id": 19,
      "code": "SEOUL_GANGDONG",
      "label": "강동구"
    }
  ]
}
```

---

### 26.6 지역 상세 조회 (ID)

**Endpoint:** `GET /api/regions/{id}`

**설명:** 지역 ID로 상세 정보를 조회합니다.

**인증:** 불필요

**Path Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| id | Long | Y | 지역 ID |

**Request Example:**
```
GET /api/regions/18
```

**Response:**
```json
{
  "data": {
    "id": 18,
    "code": "SEOUL_GANGNAM",
    "name": "강남구",
    "fullName": "서울특별시 강남구",
    "level": 2,
    "parentId": 1
  }
}
```

**Error Response:** (404 Not Found)
```json
{
  "status": 404,
  "code": "REGION_NOT_FOUND",
  "message": "지역을 찾을 수 없습니다: 999"
}
```

---

### 26.7 지역 상세 조회 (코드)

**Endpoint:** `GET /api/regions/code/{code}`

**설명:** 지역 코드로 상세 정보를 조회합니다.

**인증:** 불필요

**Path Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| code | String | Y | 지역 코드 (예: SEOUL, GYEONGBUK_GUMI) |

**Request Example:**
```
GET /api/regions/code/SEOUL_GANGNAM
```

**Response:**
```json
{
  "data": {
    "id": 18,
    "code": "SEOUL_GANGNAM",
    "name": "강남구",
    "fullName": "서울특별시 강남구",
    "level": 2,
    "parentId": 1
  }
}
```

**Error Response:** (404 Not Found)
```json
{
  "status": 404,
  "code": "REGION_NOT_FOUND",
  "message": "지역을 찾을 수 없습니다: INVALID_CODE"
}
```

---

### 26.8 지역 검색

**Endpoint:** `GET /api/regions/search`

**설명:** 지역명으로 검색합니다. 시/도명과 시/군/구명 모두 검색됩니다.

**인증:** 불필요

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| keyword | String | Y | 검색 키워드 |

**Request Example:**
```
GET /api/regions/search?keyword=강남
```

**Response:**
```json
{
  "data": [
    {
      "id": 18,
      "code": "SEOUL_GANGNAM",
      "name": "강남구",
      "fullName": "서울특별시 강남구",
      "level": 2,
      "parentId": 1
    }
  ]
}
```

**Request Example (시/도명으로 검색):**
```
GET /api/regions/search?keyword=서울
```

**Response:**
```json
{
  "data": [
    {
      "id": 1,
      "code": "SEOUL",
      "name": "서울특별시",
      "fullName": "서울특별시",
      "level": 1,
      "parentId": null
    },
    {
      "id": 18,
      "code": "SEOUL_GANGNAM",
      "name": "강남구",
      "fullName": "서울특별시 강남구",
      "level": 2,
      "parentId": 1
    },
    {
      "id": 19,
      "code": "SEOUL_GANGDONG",
      "name": "강동구",
      "fullName": "서울특별시 강동구",
      "level": 2,
      "parentId": 1
    }
  ]
}
```

---

### 26.9 지역 에러 코드

| 코드 | HTTP 상태 | 설명 |
|------|-----------|------|
| REGION_NOT_FOUND | 404 | 지역을 찾을 수 없음 |

---

### 26.10 지역 코드 규칙

| Level | 형식 | 예시 |
|-------|------|------|
| 1 (시/도) | `{시도영문}` | SEOUL, BUSAN, GYEONGGI |
| 2 (시/군/구) | `{시도영문}_{시군구영문}` | SEOUL_GANGNAM, GYEONGBUK_GUMI |

---

### 26.11 전국 시/도 코드 목록

| 코드 | 시/도명 |
|------|--------|
| SEOUL | 서울특별시 |
| BUSAN | 부산광역시 |
| DAEGU | 대구광역시 |
| INCHEON | 인천광역시 |
| GWANGJU | 광주광역시 |
| DAEJEON | 대전광역시 |
| ULSAN | 울산광역시 |
| SEJONG | 세종특별자치시 |
| GYEONGGI | 경기도 |
| GANGWON | 강원특별자치도 |
| CHUNGBUK | 충청북도 |
| CHUNGNAM | 충청남도 |
| JEONBUK | 전북특별자치도 |
| JEONNAM | 전라남도 |
| GYEONGBUK | 경상북도 |
| GYEONGNAM | 경상남도 |
| JEJU | 제주특별자치도 |