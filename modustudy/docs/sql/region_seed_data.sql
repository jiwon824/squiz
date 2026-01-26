-- ============================================
-- Region 시드 데이터
-- 17개 시/도 + 229개 시/군/구
-- ============================================

-- 시/도 (Level 1) - 17개
INSERT INTO `region` (`code`, `name`, `level`, `full_name`, `sort_order`, `is_active`) VALUES
('SEOUL', '서울특별시', 1, '서울특별시', 1, true),
('BUSAN', '부산광역시', 1, '부산광역시', 2, true),
('DAEGU', '대구광역시', 1, '대구광역시', 3, true),
('INCHEON', '인천광역시', 1, '인천광역시', 4, true),
('GWANGJU', '광주광역시', 1, '광주광역시', 5, true),
('DAEJEON', '대전광역시', 1, '대전광역시', 6, true),
('ULSAN', '울산광역시', 1, '울산광역시', 7, true),
('SEJONG', '세종특별자치시', 1, '세종특별자치시', 8, true),
('GYEONGGI', '경기도', 1, '경기도', 9, true),
('GANGWON', '강원특별자치도', 1, '강원특별자치도', 10, true),
('CHUNGBUK', '충청북도', 1, '충청북도', 11, true),
('CHUNGNAM', '충청남도', 1, '충청남도', 12, true),
('JEONBUK', '전북특별자치도', 1, '전북특별자치도', 13, true),
('JEONNAM', '전라남도', 1, '전라남도', 14, true),
('GYEONGBUK', '경상북도', 1, '경상북도', 15, true),
('GYEONGNAM', '경상남도', 1, '경상남도', 16, true),
('JEJU', '제주특별자치도', 1, '제주특별자치도', 17, true);

-- 서울특별시 (25개 구)
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'SEOUL_GANGNAM', '강남구', 2, '서울특별시 강남구', 1, true FROM `region` WHERE code = 'SEOUL';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'SEOUL_GANGDONG', '강동구', 2, '서울특별시 강동구', 2, true FROM `region` WHERE code = 'SEOUL';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'SEOUL_GANGBUK', '강북구', 2, '서울특별시 강북구', 3, true FROM `region` WHERE code = 'SEOUL';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'SEOUL_GANGSEO', '강서구', 2, '서울특별시 강서구', 4, true FROM `region` WHERE code = 'SEOUL';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'SEOUL_GWANAK', '관악구', 2, '서울특별시 관악구', 5, true FROM `region` WHERE code = 'SEOUL';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'SEOUL_GWANGJIN', '광진구', 2, '서울특별시 광진구', 6, true FROM `region` WHERE code = 'SEOUL';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'SEOUL_GURO', '구로구', 2, '서울특별시 구로구', 7, true FROM `region` WHERE code = 'SEOUL';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'SEOUL_GEUMCHEON', '금천구', 2, '서울특별시 금천구', 8, true FROM `region` WHERE code = 'SEOUL';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'SEOUL_NOWON', '노원구', 2, '서울특별시 노원구', 9, true FROM `region` WHERE code = 'SEOUL';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'SEOUL_DOBONG', '도봉구', 2, '서울특별시 도봉구', 10, true FROM `region` WHERE code = 'SEOUL';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'SEOUL_DONGDAEMUN', '동대문구', 2, '서울특별시 동대문구', 11, true FROM `region` WHERE code = 'SEOUL';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'SEOUL_DONGJAK', '동작구', 2, '서울특별시 동작구', 12, true FROM `region` WHERE code = 'SEOUL';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'SEOUL_MAPO', '마포구', 2, '서울특별시 마포구', 13, true FROM `region` WHERE code = 'SEOUL';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'SEOUL_SEODAEMUN', '서대문구', 2, '서울특별시 서대문구', 14, true FROM `region` WHERE code = 'SEOUL';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'SEOUL_SEOCHO', '서초구', 2, '서울특별시 서초구', 15, true FROM `region` WHERE code = 'SEOUL';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'SEOUL_SEONGDONG', '성동구', 2, '서울특별시 성동구', 16, true FROM `region` WHERE code = 'SEOUL';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'SEOUL_SEONGBUK', '성북구', 2, '서울특별시 성북구', 17, true FROM `region` WHERE code = 'SEOUL';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'SEOUL_SONGPA', '송파구', 2, '서울특별시 송파구', 18, true FROM `region` WHERE code = 'SEOUL';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'SEOUL_YANGCHEON', '양천구', 2, '서울특별시 양천구', 19, true FROM `region` WHERE code = 'SEOUL';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'SEOUL_YEONGDEUNGPO', '영등포구', 2, '서울특별시 영등포구', 20, true FROM `region` WHERE code = 'SEOUL';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'SEOUL_YONGSAN', '용산구', 2, '서울특별시 용산구', 21, true FROM `region` WHERE code = 'SEOUL';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'SEOUL_EUNPYEONG', '은평구', 2, '서울특별시 은평구', 22, true FROM `region` WHERE code = 'SEOUL';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'SEOUL_JONGNO', '종로구', 2, '서울특별시 종로구', 23, true FROM `region` WHERE code = 'SEOUL';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'SEOUL_JUNG', '중구', 2, '서울특별시 중구', 24, true FROM `region` WHERE code = 'SEOUL';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'SEOUL_JUNGNANG', '중랑구', 2, '서울특별시 중랑구', 25, true FROM `region` WHERE code = 'SEOUL';

-- 부산광역시 (16개 구/군)
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'BUSAN_GANGSEO', '강서구', 2, '부산광역시 강서구', 1, true FROM `region` WHERE code = 'BUSAN';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'BUSAN_GEUMJEONG', '금정구', 2, '부산광역시 금정구', 2, true FROM `region` WHERE code = 'BUSAN';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'BUSAN_GIJANG', '기장군', 2, '부산광역시 기장군', 3, true FROM `region` WHERE code = 'BUSAN';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'BUSAN_NAM', '남구', 2, '부산광역시 남구', 4, true FROM `region` WHERE code = 'BUSAN';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'BUSAN_DONG', '동구', 2, '부산광역시 동구', 5, true FROM `region` WHERE code = 'BUSAN';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'BUSAN_DONGNAE', '동래구', 2, '부산광역시 동래구', 6, true FROM `region` WHERE code = 'BUSAN';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'BUSAN_BUSANJIN', '부산진구', 2, '부산광역시 부산진구', 7, true FROM `region` WHERE code = 'BUSAN';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'BUSAN_BUK', '북구', 2, '부산광역시 북구', 8, true FROM `region` WHERE code = 'BUSAN';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'BUSAN_SASANG', '사상구', 2, '부산광역시 사상구', 9, true FROM `region` WHERE code = 'BUSAN';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'BUSAN_SAHA', '사하구', 2, '부산광역시 사하구', 10, true FROM `region` WHERE code = 'BUSAN';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'BUSAN_SEO', '서구', 2, '부산광역시 서구', 11, true FROM `region` WHERE code = 'BUSAN';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'BUSAN_SUYEONG', '수영구', 2, '부산광역시 수영구', 12, true FROM `region` WHERE code = 'BUSAN';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'BUSAN_YEONJE', '연제구', 2, '부산광역시 연제구', 13, true FROM `region` WHERE code = 'BUSAN';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'BUSAN_YEONGDO', '영도구', 2, '부산광역시 영도구', 14, true FROM `region` WHERE code = 'BUSAN';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'BUSAN_JUNG', '중구', 2, '부산광역시 중구', 15, true FROM `region` WHERE code = 'BUSAN';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'BUSAN_HAEUNDAE', '해운대구', 2, '부산광역시 해운대구', 16, true FROM `region` WHERE code = 'BUSAN';

-- 대구광역시 (8개 구 + 1개 군)
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'DAEGU_NAM', '남구', 2, '대구광역시 남구', 1, true FROM `region` WHERE code = 'DAEGU';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'DAEGU_DALSEO', '달서구', 2, '대구광역시 달서구', 2, true FROM `region` WHERE code = 'DAEGU';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'DAEGU_DALSEONG', '달성군', 2, '대구광역시 달성군', 3, true FROM `region` WHERE code = 'DAEGU';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'DAEGU_DONG', '동구', 2, '대구광역시 동구', 4, true FROM `region` WHERE code = 'DAEGU';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'DAEGU_BUK', '북구', 2, '대구광역시 북구', 5, true FROM `region` WHERE code = 'DAEGU';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'DAEGU_SEO', '서구', 2, '대구광역시 서구', 6, true FROM `region` WHERE code = 'DAEGU';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'DAEGU_SUSEONG', '수성구', 2, '대구광역시 수성구', 7, true FROM `region` WHERE code = 'DAEGU';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'DAEGU_JUNG', '중구', 2, '대구광역시 중구', 8, true FROM `region` WHERE code = 'DAEGU';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'DAEGU_GUNWI', '군위군', 2, '대구광역시 군위군', 9, true FROM `region` WHERE code = 'DAEGU';

-- 인천광역시 (8개 구 + 2개 군)
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'INCHEON_GANGHWA', '강화군', 2, '인천광역시 강화군', 1, true FROM `region` WHERE code = 'INCHEON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'INCHEON_GYEYANG', '계양구', 2, '인천광역시 계양구', 2, true FROM `region` WHERE code = 'INCHEON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'INCHEON_MICHUHOL', '미추홀구', 2, '인천광역시 미추홀구', 3, true FROM `region` WHERE code = 'INCHEON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'INCHEON_NAMDONG', '남동구', 2, '인천광역시 남동구', 4, true FROM `region` WHERE code = 'INCHEON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'INCHEON_DONG', '동구', 2, '인천광역시 동구', 5, true FROM `region` WHERE code = 'INCHEON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'INCHEON_BUPYEONG', '부평구', 2, '인천광역시 부평구', 6, true FROM `region` WHERE code = 'INCHEON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'INCHEON_SEO', '서구', 2, '인천광역시 서구', 7, true FROM `region` WHERE code = 'INCHEON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'INCHEON_YEONSU', '연수구', 2, '인천광역시 연수구', 8, true FROM `region` WHERE code = 'INCHEON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'INCHEON_ONGJIN', '옹진군', 2, '인천광역시 옹진군', 9, true FROM `region` WHERE code = 'INCHEON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'INCHEON_JUNG', '중구', 2, '인천광역시 중구', 10, true FROM `region` WHERE code = 'INCHEON';

-- 광주광역시 (5개 구)
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GWANGJU_GWANGSAN', '광산구', 2, '광주광역시 광산구', 1, true FROM `region` WHERE code = 'GWANGJU';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GWANGJU_NAM', '남구', 2, '광주광역시 남구', 2, true FROM `region` WHERE code = 'GWANGJU';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GWANGJU_DONG', '동구', 2, '광주광역시 동구', 3, true FROM `region` WHERE code = 'GWANGJU';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GWANGJU_BUK', '북구', 2, '광주광역시 북구', 4, true FROM `region` WHERE code = 'GWANGJU';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GWANGJU_SEO', '서구', 2, '광주광역시 서구', 5, true FROM `region` WHERE code = 'GWANGJU';

-- 대전광역시 (5개 구)
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'DAEJEON_DAEDEOK', '대덕구', 2, '대전광역시 대덕구', 1, true FROM `region` WHERE code = 'DAEJEON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'DAEJEON_DONG', '동구', 2, '대전광역시 동구', 2, true FROM `region` WHERE code = 'DAEJEON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'DAEJEON_SEO', '서구', 2, '대전광역시 서구', 3, true FROM `region` WHERE code = 'DAEJEON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'DAEJEON_YUSEONG', '유성구', 2, '대전광역시 유성구', 4, true FROM `region` WHERE code = 'DAEJEON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'DAEJEON_JUNG', '중구', 2, '대전광역시 중구', 5, true FROM `region` WHERE code = 'DAEJEON';

-- 울산광역시 (4개 구 + 1개 군)
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'ULSAN_NAM', '남구', 2, '울산광역시 남구', 1, true FROM `region` WHERE code = 'ULSAN';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'ULSAN_DONG', '동구', 2, '울산광역시 동구', 2, true FROM `region` WHERE code = 'ULSAN';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'ULSAN_BUK', '북구', 2, '울산광역시 북구', 3, true FROM `region` WHERE code = 'ULSAN';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'ULSAN_ULJU', '울주군', 2, '울산광역시 울주군', 4, true FROM `region` WHERE code = 'ULSAN';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'ULSAN_JUNG', '중구', 2, '울산광역시 중구', 5, true FROM `region` WHERE code = 'ULSAN';

-- 경기도 (31개 시/군)
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_GAPYEONG', '가평군', 2, '경기도 가평군', 1, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_GOYANG', '고양시', 2, '경기도 고양시', 2, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_GWACHEON', '과천시', 2, '경기도 과천시', 3, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_GWANGMYEONG', '광명시', 2, '경기도 광명시', 4, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_GWANGJU', '광주시', 2, '경기도 광주시', 5, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_GURI', '구리시', 2, '경기도 구리시', 6, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_GUNPO', '군포시', 2, '경기도 군포시', 7, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_GIMPO', '김포시', 2, '경기도 김포시', 8, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_NAMYANGJU', '남양주시', 2, '경기도 남양주시', 9, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_DONGDUCHEON', '동두천시', 2, '경기도 동두천시', 10, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_BUCHEON', '부천시', 2, '경기도 부천시', 11, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_SEONGNAM', '성남시', 2, '경기도 성남시', 12, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_SUWON', '수원시', 2, '경기도 수원시', 13, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_SIHEUNG', '시흥시', 2, '경기도 시흥시', 14, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_ANSAN', '안산시', 2, '경기도 안산시', 15, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_ANSEONG', '안성시', 2, '경기도 안성시', 16, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_ANYANG', '안양시', 2, '경기도 안양시', 17, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_YANGJU', '양주시', 2, '경기도 양주시', 18, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_YANGPYEONG', '양평군', 2, '경기도 양평군', 19, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_YEOJU', '여주시', 2, '경기도 여주시', 20, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_YEONCHEON', '연천군', 2, '경기도 연천군', 21, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_OSAN', '오산시', 2, '경기도 오산시', 22, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_YONGIN', '용인시', 2, '경기도 용인시', 23, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_UIWANG', '의왕시', 2, '경기도 의왕시', 24, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_UIJEONGBU', '의정부시', 2, '경기도 의정부시', 25, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_ICHEON', '이천시', 2, '경기도 이천시', 26, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_PAJU', '파주시', 2, '경기도 파주시', 27, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_PYEONGTAEK', '평택시', 2, '경기도 평택시', 28, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_POCHEON', '포천시', 2, '경기도 포천시', 29, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_HANAM', '하남시', 2, '경기도 하남시', 30, true FROM `region` WHERE code = 'GYEONGGI';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGGI_HWASEONG', '화성시', 2, '경기도 화성시', 31, true FROM `region` WHERE code = 'GYEONGGI';

-- 강원특별자치도 (18개 시/군)
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GANGWON_GANGNEUNG', '강릉시', 2, '강원특별자치도 강릉시', 1, true FROM `region` WHERE code = 'GANGWON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GANGWON_GOSEONG', '고성군', 2, '강원특별자치도 고성군', 2, true FROM `region` WHERE code = 'GANGWON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GANGWON_DONGHAE', '동해시', 2, '강원특별자치도 동해시', 3, true FROM `region` WHERE code = 'GANGWON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GANGWON_SAMCHEOK', '삼척시', 2, '강원특별자치도 삼척시', 4, true FROM `region` WHERE code = 'GANGWON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GANGWON_SOKCHO', '속초시', 2, '강원특별자치도 속초시', 5, true FROM `region` WHERE code = 'GANGWON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GANGWON_YANGGU', '양구군', 2, '강원특별자치도 양구군', 6, true FROM `region` WHERE code = 'GANGWON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GANGWON_YANGYANG', '양양군', 2, '강원특별자치도 양양군', 7, true FROM `region` WHERE code = 'GANGWON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GANGWON_YEONGWOL', '영월군', 2, '강원특별자치도 영월군', 8, true FROM `region` WHERE code = 'GANGWON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GANGWON_WONJU', '원주시', 2, '강원특별자치도 원주시', 9, true FROM `region` WHERE code = 'GANGWON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GANGWON_INJE', '인제군', 2, '강원특별자치도 인제군', 10, true FROM `region` WHERE code = 'GANGWON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GANGWON_JEONGSEON', '정선군', 2, '강원특별자치도 정선군', 11, true FROM `region` WHERE code = 'GANGWON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GANGWON_CHEORWON', '철원군', 2, '강원특별자치도 철원군', 12, true FROM `region` WHERE code = 'GANGWON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GANGWON_CHUNCHEON', '춘천시', 2, '강원특별자치도 춘천시', 13, true FROM `region` WHERE code = 'GANGWON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GANGWON_TAEBAEK', '태백시', 2, '강원특별자치도 태백시', 14, true FROM `region` WHERE code = 'GANGWON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GANGWON_PYEONGCHANG', '평창군', 2, '강원특별자치도 평창군', 15, true FROM `region` WHERE code = 'GANGWON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GANGWON_HONGCHEON', '홍천군', 2, '강원특별자치도 홍천군', 16, true FROM `region` WHERE code = 'GANGWON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GANGWON_HWACHEON', '화천군', 2, '강원특별자치도 화천군', 17, true FROM `region` WHERE code = 'GANGWON';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GANGWON_HOENGSEONG', '횡성군', 2, '강원특별자치도 횡성군', 18, true FROM `region` WHERE code = 'GANGWON';

-- 충청북도 (11개 시/군)
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'CHUNGBUK_GOESAN', '괴산군', 2, '충청북도 괴산군', 1, true FROM `region` WHERE code = 'CHUNGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'CHUNGBUK_DANYANG', '단양군', 2, '충청북도 단양군', 2, true FROM `region` WHERE code = 'CHUNGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'CHUNGBUK_BOEUN', '보은군', 2, '충청북도 보은군', 3, true FROM `region` WHERE code = 'CHUNGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'CHUNGBUK_YEONGDONG', '영동군', 2, '충청북도 영동군', 4, true FROM `region` WHERE code = 'CHUNGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'CHUNGBUK_OKCHEON', '옥천군', 2, '충청북도 옥천군', 5, true FROM `region` WHERE code = 'CHUNGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'CHUNGBUK_EUMSEONG', '음성군', 2, '충청북도 음성군', 6, true FROM `region` WHERE code = 'CHUNGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'CHUNGBUK_JECHEON', '제천시', 2, '충청북도 제천시', 7, true FROM `region` WHERE code = 'CHUNGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'CHUNGBUK_JEUNGPYEONG', '증평군', 2, '충청북도 증평군', 8, true FROM `region` WHERE code = 'CHUNGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'CHUNGBUK_JINCHEON', '진천군', 2, '충청북도 진천군', 9, true FROM `region` WHERE code = 'CHUNGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'CHUNGBUK_CHEONGJU', '청주시', 2, '충청북도 청주시', 10, true FROM `region` WHERE code = 'CHUNGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'CHUNGBUK_CHUNGJU', '충주시', 2, '충청북도 충주시', 11, true FROM `region` WHERE code = 'CHUNGBUK';

-- 충청남도 (15개 시/군)
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'CHUNGNAM_GYERYONG', '계룡시', 2, '충청남도 계룡시', 1, true FROM `region` WHERE code = 'CHUNGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'CHUNGNAM_GONGJU', '공주시', 2, '충청남도 공주시', 2, true FROM `region` WHERE code = 'CHUNGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'CHUNGNAM_GEUMSAN', '금산군', 2, '충청남도 금산군', 3, true FROM `region` WHERE code = 'CHUNGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'CHUNGNAM_NONSAN', '논산시', 2, '충청남도 논산시', 4, true FROM `region` WHERE code = 'CHUNGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'CHUNGNAM_DANGJIN', '당진시', 2, '충청남도 당진시', 5, true FROM `region` WHERE code = 'CHUNGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'CHUNGNAM_BORYEONG', '보령시', 2, '충청남도 보령시', 6, true FROM `region` WHERE code = 'CHUNGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'CHUNGNAM_BUYEO', '부여군', 2, '충청남도 부여군', 7, true FROM `region` WHERE code = 'CHUNGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'CHUNGNAM_SEOSAN', '서산시', 2, '충청남도 서산시', 8, true FROM `region` WHERE code = 'CHUNGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'CHUNGNAM_SEOCHEON', '서천군', 2, '충청남도 서천군', 9, true FROM `region` WHERE code = 'CHUNGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'CHUNGNAM_ASAN', '아산시', 2, '충청남도 아산시', 10, true FROM `region` WHERE code = 'CHUNGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'CHUNGNAM_YESAN', '예산군', 2, '충청남도 예산군', 11, true FROM `region` WHERE code = 'CHUNGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'CHUNGNAM_CHEONAN', '천안시', 2, '충청남도 천안시', 12, true FROM `region` WHERE code = 'CHUNGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'CHUNGNAM_CHEONGYANG', '청양군', 2, '충청남도 청양군', 13, true FROM `region` WHERE code = 'CHUNGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'CHUNGNAM_TAEAN', '태안군', 2, '충청남도 태안군', 14, true FROM `region` WHERE code = 'CHUNGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'CHUNGNAM_HONGSEONG', '홍성군', 2, '충청남도 홍성군', 15, true FROM `region` WHERE code = 'CHUNGNAM';

-- 전북특별자치도 (14개 시/군)
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONBUK_GOCHANG', '고창군', 2, '전북특별자치도 고창군', 1, true FROM `region` WHERE code = 'JEONBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONBUK_GUNSAN', '군산시', 2, '전북특별자치도 군산시', 2, true FROM `region` WHERE code = 'JEONBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONBUK_GIMJE', '김제시', 2, '전북특별자치도 김제시', 3, true FROM `region` WHERE code = 'JEONBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONBUK_NAMWON', '남원시', 2, '전북특별자치도 남원시', 4, true FROM `region` WHERE code = 'JEONBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONBUK_MUJU', '무주군', 2, '전북특별자치도 무주군', 5, true FROM `region` WHERE code = 'JEONBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONBUK_BUAN', '부안군', 2, '전북특별자치도 부안군', 6, true FROM `region` WHERE code = 'JEONBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONBUK_SUNCHANG', '순창군', 2, '전북특별자치도 순창군', 7, true FROM `region` WHERE code = 'JEONBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONBUK_WANJU', '완주군', 2, '전북특별자치도 완주군', 8, true FROM `region` WHERE code = 'JEONBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONBUK_IKSAN', '익산시', 2, '전북특별자치도 익산시', 9, true FROM `region` WHERE code = 'JEONBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONBUK_IMSIL', '임실군', 2, '전북특별자치도 임실군', 10, true FROM `region` WHERE code = 'JEONBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONBUK_JANGSU', '장수군', 2, '전북특별자치도 장수군', 11, true FROM `region` WHERE code = 'JEONBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONBUK_JEONJU', '전주시', 2, '전북특별자치도 전주시', 12, true FROM `region` WHERE code = 'JEONBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONBUK_JEONGEUP', '정읍시', 2, '전북특별자치도 정읍시', 13, true FROM `region` WHERE code = 'JEONBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONBUK_JINAN', '진안군', 2, '전북특별자치도 진안군', 14, true FROM `region` WHERE code = 'JEONBUK';

-- 전라남도 (22개 시/군)
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONNAM_GANGJIN', '강진군', 2, '전라남도 강진군', 1, true FROM `region` WHERE code = 'JEONNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONNAM_GOHEUNG', '고흥군', 2, '전라남도 고흥군', 2, true FROM `region` WHERE code = 'JEONNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONNAM_GOKSEONG', '곡성군', 2, '전라남도 곡성군', 3, true FROM `region` WHERE code = 'JEONNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONNAM_GWANGYANG', '광양시', 2, '전라남도 광양시', 4, true FROM `region` WHERE code = 'JEONNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONNAM_GURYE', '구례군', 2, '전라남도 구례군', 5, true FROM `region` WHERE code = 'JEONNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONNAM_NAJU', '나주시', 2, '전라남도 나주시', 6, true FROM `region` WHERE code = 'JEONNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONNAM_DAMYANG', '담양군', 2, '전라남도 담양군', 7, true FROM `region` WHERE code = 'JEONNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONNAM_MOKPO', '목포시', 2, '전라남도 목포시', 8, true FROM `region` WHERE code = 'JEONNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONNAM_MUAN', '무안군', 2, '전라남도 무안군', 9, true FROM `region` WHERE code = 'JEONNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONNAM_BOSEONG', '보성군', 2, '전라남도 보성군', 10, true FROM `region` WHERE code = 'JEONNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONNAM_SUNCHEON', '순천시', 2, '전라남도 순천시', 11, true FROM `region` WHERE code = 'JEONNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONNAM_SINAN', '신안군', 2, '전라남도 신안군', 12, true FROM `region` WHERE code = 'JEONNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONNAM_YEOSU', '여수시', 2, '전라남도 여수시', 13, true FROM `region` WHERE code = 'JEONNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONNAM_YEONGGWANG', '영광군', 2, '전라남도 영광군', 14, true FROM `region` WHERE code = 'JEONNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONNAM_YEONGAM', '영암군', 2, '전라남도 영암군', 15, true FROM `region` WHERE code = 'JEONNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONNAM_WANDO', '완도군', 2, '전라남도 완도군', 16, true FROM `region` WHERE code = 'JEONNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONNAM_JANGSEONG', '장성군', 2, '전라남도 장성군', 17, true FROM `region` WHERE code = 'JEONNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONNAM_JANGHEUNG', '장흥군', 2, '전라남도 장흥군', 18, true FROM `region` WHERE code = 'JEONNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONNAM_JINDO', '진도군', 2, '전라남도 진도군', 19, true FROM `region` WHERE code = 'JEONNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONNAM_HAMPYEONG', '함평군', 2, '전라남도 함평군', 20, true FROM `region` WHERE code = 'JEONNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONNAM_HAENAM', '해남군', 2, '전라남도 해남군', 21, true FROM `region` WHERE code = 'JEONNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEONNAM_HWASUN', '화순군', 2, '전라남도 화순군', 22, true FROM `region` WHERE code = 'JEONNAM';

-- 경상북도 (21개 시/군) - 군위군은 대구로 편입
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGBUK_GYEONGSAN', '경산시', 2, '경상북도 경산시', 1, true FROM `region` WHERE code = 'GYEONGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGBUK_GYEONGJU', '경주시', 2, '경상북도 경주시', 2, true FROM `region` WHERE code = 'GYEONGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGBUK_GORYEONG', '고령군', 2, '경상북도 고령군', 3, true FROM `region` WHERE code = 'GYEONGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGBUK_GUMI', '구미시', 2, '경상북도 구미시', 4, true FROM `region` WHERE code = 'GYEONGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGBUK_GIMCHEON', '김천시', 2, '경상북도 김천시', 5, true FROM `region` WHERE code = 'GYEONGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGBUK_MUNGYEONG', '문경시', 2, '경상북도 문경시', 6, true FROM `region` WHERE code = 'GYEONGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGBUK_BONGHWA', '봉화군', 2, '경상북도 봉화군', 7, true FROM `region` WHERE code = 'GYEONGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGBUK_SANGJU', '상주시', 2, '경상북도 상주시', 8, true FROM `region` WHERE code = 'GYEONGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGBUK_SEONGJU', '성주군', 2, '경상북도 성주군', 9, true FROM `region` WHERE code = 'GYEONGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGBUK_ANDONG', '안동시', 2, '경상북도 안동시', 10, true FROM `region` WHERE code = 'GYEONGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGBUK_YEONGDEOK', '영덕군', 2, '경상북도 영덕군', 11, true FROM `region` WHERE code = 'GYEONGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGBUK_YEONGYANG', '영양군', 2, '경상북도 영양군', 12, true FROM `region` WHERE code = 'GYEONGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGBUK_YEONGJU', '영주시', 2, '경상북도 영주시', 13, true FROM `region` WHERE code = 'GYEONGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGBUK_YEONGCHEON', '영천시', 2, '경상북도 영천시', 14, true FROM `region` WHERE code = 'GYEONGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGBUK_YECHEON', '예천군', 2, '경상북도 예천군', 15, true FROM `region` WHERE code = 'GYEONGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGBUK_ULLEUNG', '울릉군', 2, '경상북도 울릉군', 16, true FROM `region` WHERE code = 'GYEONGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGBUK_UISEONG', '의성군', 2, '경상북도 의성군', 17, true FROM `region` WHERE code = 'GYEONGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGBUK_CHEONGDO', '청도군', 2, '경상북도 청도군', 18, true FROM `region` WHERE code = 'GYEONGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGBUK_CHEONGSONG', '청송군', 2, '경상북도 청송군', 19, true FROM `region` WHERE code = 'GYEONGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGBUK_CHILGOK', '칠곡군', 2, '경상북도 칠곡군', 20, true FROM `region` WHERE code = 'GYEONGBUK';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGBUK_POHANG', '포항시', 2, '경상북도 포항시', 21, true FROM `region` WHERE code = 'GYEONGBUK';

-- 경상남도 (18개 시/군)
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGNAM_GEOJE', '거제시', 2, '경상남도 거제시', 1, true FROM `region` WHERE code = 'GYEONGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGNAM_GEOCHANG', '거창군', 2, '경상남도 거창군', 2, true FROM `region` WHERE code = 'GYEONGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGNAM_GOSEONG', '고성군', 2, '경상남도 고성군', 3, true FROM `region` WHERE code = 'GYEONGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGNAM_GIMHAE', '김해시', 2, '경상남도 김해시', 4, true FROM `region` WHERE code = 'GYEONGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGNAM_NAMHAE', '남해군', 2, '경상남도 남해군', 5, true FROM `region` WHERE code = 'GYEONGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGNAM_MIRYANG', '밀양시', 2, '경상남도 밀양시', 6, true FROM `region` WHERE code = 'GYEONGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGNAM_SACHEON', '사천시', 2, '경상남도 사천시', 7, true FROM `region` WHERE code = 'GYEONGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGNAM_SANCHEONG', '산청군', 2, '경상남도 산청군', 8, true FROM `region` WHERE code = 'GYEONGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGNAM_YANGSAN', '양산시', 2, '경상남도 양산시', 9, true FROM `region` WHERE code = 'GYEONGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGNAM_UIRYEONG', '의령군', 2, '경상남도 의령군', 10, true FROM `region` WHERE code = 'GYEONGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGNAM_JINJU', '진주시', 2, '경상남도 진주시', 11, true FROM `region` WHERE code = 'GYEONGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGNAM_CHANGNYEONG', '창녕군', 2, '경상남도 창녕군', 12, true FROM `region` WHERE code = 'GYEONGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGNAM_CHANGWON', '창원시', 2, '경상남도 창원시', 13, true FROM `region` WHERE code = 'GYEONGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGNAM_TONGYEONG', '통영시', 2, '경상남도 통영시', 14, true FROM `region` WHERE code = 'GYEONGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGNAM_HADONG', '하동군', 2, '경상남도 하동군', 15, true FROM `region` WHERE code = 'GYEONGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGNAM_HAMAN', '함안군', 2, '경상남도 함안군', 16, true FROM `region` WHERE code = 'GYEONGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGNAM_HAMYANG', '함양군', 2, '경상남도 함양군', 17, true FROM `region` WHERE code = 'GYEONGNAM';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'GYEONGNAM_HAPCHEON', '합천군', 2, '경상남도 합천군', 18, true FROM `region` WHERE code = 'GYEONGNAM';

-- 제주특별자치도 (2개 시)
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEJU_JEJU', '제주시', 2, '제주특별자치도 제주시', 1, true FROM `region` WHERE code = 'JEJU';
INSERT INTO `region` (`parent_id`, `code`, `name`, `level`, `full_name`, `sort_order`, `is_active`)
SELECT id, 'JEJU_SEOGWIPO', '서귀포시', 2, '제주특별자치도 서귀포시', 2, true FROM `region` WHERE code = 'JEJU';