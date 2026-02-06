package com.ssafy.config;

import com.ssafy.domain.study.entity.Format;
import com.ssafy.domain.study.entity.Region;
import com.ssafy.domain.study.entity.Topic;
import com.ssafy.domain.study.repository.FormatRepository;
import com.ssafy.domain.study.repository.RegionRepository;
import com.ssafy.domain.study.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final TopicRepository topicRepository;
    private final FormatRepository formatRepository;
    private final RegionRepository regionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initTopics();
        initFormats();
        initRegions();
    }

    // ========== Topic 초기 데이터 ==========
    private void initTopics() {
        if (topicRepository.count() > 0) {
            return;
        }
// 대분류 → 세부주제 (프론트 TOPIC_SUBTOPICS와 1:1 매칭)
        Map<String, List<String>> topicMap = new LinkedHashMap<>();
        topicMap.put("알고리즘/코딩테스트", List.of("백준", "프로그래머스", "SWEA", "LeetCode", "코딩테스트 대비"));
        topicMap.put("CS 기초", List.of("자료구조", "알고리즘 이론", "운영체제", "네트워크", "데이터베이스", "컴퓨터구조", "디자인패턴", "시스템 설계"));
        topicMap.put("프론트엔드", List.of("HTML/CSS", "JavaScript", "TypeScript", "React", "Vue", "Next.js", "웹 접근성/성능"));
        topicMap.put("백엔드", List.of("Java/Spring", "Python/Django", "Python/FastAPI", "Node.js/Express", "Go", "Kotlin", "API 설계"));
        topicMap.put("인프라/DevOps", List.of("Docker", "Kubernetes", "CI/CD", "AWS", "GCP", "Linux", "모니터링"));
        topicMap.put("AI/ML", List.of("머신러닝 기초", "딥러닝", "NLP", "컴퓨터 비전", "MLOps", "논문 리뷰"));
        topicMap.put("모바일", List.of("Android (Kotlin)", "Android (Java)", "iOS (Swift)", "Flutter", "React Native"));
        topicMap.put("자격증", List.of("정보처리기사", "SQLD/SQLP", "리눅스마스터", "네트워크관리사", "AWS 자격증", "Azure 자격증", "CKAD/CKA"));
        topicMap.put("취업 준비", List.of("기술 면접", "코딩테스트 대비", "포트폴리오", "이력서/자소서", "모의 면접"));
        topicMap.put("프로젝트", List.of("사이드 프로젝트", "클론 코딩", "오픈소스 기여", "해커톤 준비"));

        int parentOrder = 0;
        for (Map.Entry<String, List<String>> entry : topicMap.entrySet()) {
            Topic parent = topicRepository.save(Topic.builder()
                    .name(entry.getKey())
                    .sortOrder(parentOrder++)
                    .build());

            int childOrder = 0;
            for (String childName : entry.getValue()) {
                topicRepository.save(Topic.builder()
                        .name(childName)
                        .parent(parent)
                        .sortOrder(childOrder++)
                        .build());
            }
        }
}

    // ========== Format 초기 데이터 ==========
    private void initFormats() {
        if (formatRepository.count() > 0) {
            return;
        }
        String[] formats = {"문제 풀이", "독서/책 스터디", "강의 수강", "프로젝트", "모의 면접", "코드 리뷰", "발표/세미나", "토론"};
        for (int i = 0; i < formats.length; i++) {
            formatRepository.save(Format.builder()
                    .name(formats[i])
                    .sortOrder(i)
                    .build());
        }
}

    // ========== Region 초기 데이터 ==========
    private void initRegions() {
        if (regionRepository.count() > 0) {
            return;
        }
// 시/도 데이터 (코드, 이름)
        String[][] provinces = {
                {"SEOUL", "서울특별시"},
                {"BUSAN", "부산광역시"},
                {"DAEGU", "대구광역시"},
                {"INCHEON", "인천광역시"},
                {"GWANGJU", "광주광역시"},
                {"DAEJEON", "대전광역시"},
                {"ULSAN", "울산광역시"},
                {"SEJONG", "세종특별자치시"},
                {"GYEONGGI", "경기도"},
                {"GANGWON", "강원특별자치도"},
                {"CHUNGBUK", "충청북도"},
                {"CHUNGNAM", "충청남도"},
                {"JEONBUK", "전북특별자치도"},
                {"JEONNAM", "전라남도"},
                {"GYEONGBUK", "경상북도"},
                {"GYEONGNAM", "경상남도"},
                {"JEJU", "제주특별자치도"}
        };

        // 시/군/구 데이터 (시/도코드 → 시/군/구 이름 배열)
        Map<String, String[]> districtMap = new LinkedHashMap<>();
        districtMap.put("SEOUL", new String[]{"강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구", "금천구", "노원구", "도봉구", "동대문구", "동작구", "마포구", "서대문구", "서초구", "성동구", "성북구", "송파구", "양천구", "영등포구", "용산구", "은평구", "종로구", "중구", "중랑구"});
        districtMap.put("BUSAN", new String[]{"강서구", "금정구", "남구", "동구", "동래구", "부산진구", "북구", "사상구", "사하구", "서구", "수영구", "연제구", "영도구", "중구", "해운대구", "기장군"});
        districtMap.put("DAEGU", new String[]{"남구", "달서구", "동구", "북구", "서구", "수성구", "중구", "달성군"});
        districtMap.put("INCHEON", new String[]{"계양구", "남동구", "동구", "미추홀구", "부평구", "서구", "연수구", "중구", "강화군", "옹진군"});
        districtMap.put("GWANGJU", new String[]{"광산구", "남구", "동구", "북구", "서구"});
        districtMap.put("DAEJEON", new String[]{"대덕구", "동구", "서구", "유성구", "중구"});
        districtMap.put("ULSAN", new String[]{"남구", "동구", "북구", "중구", "울주군"});
        districtMap.put("SEJONG", new String[]{"세종시"});
        districtMap.put("GYEONGGI", new String[]{"수원시", "성남시", "고양시", "용인시", "부천시", "안산시", "안양시", "남양주시", "화성시", "평택시", "의정부시", "시흥시", "파주시", "김포시", "광명시", "광주시", "군포시", "하남시", "오산시", "이천시", "안성시", "의왕시", "양평군", "여주시", "과천시", "구리시", "포천시", "양주시", "동두천시", "가평군", "연천군"});
        districtMap.put("GANGWON", new String[]{"춘천시", "원주시", "강릉시", "동해시", "태백시", "속초시", "삼척시", "홍천군", "횡성군", "영월군", "평창군", "정선군", "철원군", "화천군", "양구군", "인제군", "고성군", "양양군"});
        districtMap.put("CHUNGBUK", new String[]{"청주시", "충주시", "제천시", "보은군", "옥천군", "영동군", "증평군", "진천군", "괴산군", "음성군", "단양군"});
        districtMap.put("CHUNGNAM", new String[]{"천안시", "공주시", "보령시", "아산시", "서산시", "논산시", "계룡시", "당진시", "금산군", "부여군", "서천군", "청양군", "홍성군", "예산군", "태안군"});
        districtMap.put("JEONBUK", new String[]{"전주시", "군산시", "익산시", "정읍시", "남원시", "김제시", "완주군", "진안군", "무주군", "장수군", "임실군", "순창군", "고창군", "부안군"});
        districtMap.put("JEONNAM", new String[]{"목포시", "여수시", "순천시", "나주시", "광양시", "담양군", "곡성군", "구례군", "고흥군", "보성군", "화순군", "장흥군", "강진군", "해남군", "영암군", "무안군", "함평군", "영광군", "장성군", "완도군", "진도군", "신안군"});
        districtMap.put("GYEONGBUK", new String[]{"포항시", "경주시", "김천시", "안동시", "구미시", "영주시", "영천시", "상주시", "문경시", "경산시", "군위군", "의성군", "청송군", "영양군", "영덕군", "청도군", "고령군", "성주군", "칠곡군", "예천군", "봉화군", "울진군", "울릉군"});
        districtMap.put("GYEONGNAM", new String[]{"창원시", "진주시", "통영시", "사천시", "김해시", "밀양시", "거제시", "양산시", "의령군", "함안군", "창녕군", "고성군", "남해군", "하동군", "산청군", "함양군", "거창군", "합천군"});
        districtMap.put("JEJU", new String[]{"제주시", "서귀포시"});

        for (int i = 0; i < provinces.length; i++) {
            String code = provinces[i][0];
            String name = provinces[i][1];

            Region province = regionRepository.save(Region.builder()
                    .code(code)
                    .name(name)
                    .fullName(name)
                    .level(1)
                    .sortOrder(i)
                    .build());

            String[] districts = districtMap.get(code);
            if (districts != null) {
                for (int j = 0; j < districts.length; j++) {
                    String districtName = districts[j];
                    String districtCode = code + "_" + toEnglishCode(districtName);

                    regionRepository.save(Region.builder()
                            .code(districtCode)
                            .name(districtName)
                            .fullName(name + " " + districtName)
                            .level(2)
                            .parent(province)
                            .sortOrder(j)
                            .build());
                }
            }
        }
}

    // 한글 시/군/구명 → 영문 코드 변환
    private String toEnglishCode(String name) {
        Map<String, String> codeMap = new LinkedHashMap<>();
        // 서울
        codeMap.put("강남구", "GANGNAM");
        codeMap.put("강동구", "GANGDONG");
        codeMap.put("강북구", "GANGBUK");
        codeMap.put("강서구", "GANGSEO");
        codeMap.put("관악구", "GWANAK");
        codeMap.put("광진구", "GWANGJIN");
        codeMap.put("구로구", "GURO");
        codeMap.put("금천구", "GEUMCHEON");
        codeMap.put("노원구", "NOWON");
        codeMap.put("도봉구", "DOBONG");
        codeMap.put("동대문구", "DONGDAEMUN");
        codeMap.put("동작구", "DONGJAK");
        codeMap.put("마포구", "MAPO");
        codeMap.put("서대문구", "SEODAEMUN");
        codeMap.put("서초구", "SEOCHO");
        codeMap.put("성동구", "SEONGDONG");
        codeMap.put("성북구", "SEONGBUK");
        codeMap.put("송파구", "SONGPA");
        codeMap.put("양천구", "YANGCHEON");
        codeMap.put("영등포구", "YEONGDEUNGPO");
        codeMap.put("용산구", "YONGSAN");
        codeMap.put("은평구", "EUNPYEONG");
        codeMap.put("종로구", "JONGNO");
        codeMap.put("중구", "JUNG");
        codeMap.put("중랑구", "JUNGNANG");
        // 부산
        codeMap.put("금정구", "GEUMJEONG");
        codeMap.put("남구", "NAM");
        codeMap.put("동구", "DONG");
        codeMap.put("동래구", "DONGNAE");
        codeMap.put("부산진구", "BUSANJIN");
        codeMap.put("북구", "BUK");
        codeMap.put("사상구", "SASANG");
        codeMap.put("사하구", "SAHA");
        codeMap.put("서구", "SEO");
        codeMap.put("수영구", "SUYEONG");
        codeMap.put("연제구", "YEONJE");
        codeMap.put("영도구", "YEONGDO");
        codeMap.put("해운대구", "HAEUNDAE");
        codeMap.put("기장군", "GIJANG");
        // 대구
        codeMap.put("달서구", "DALSEO");
        codeMap.put("수성구", "SUSEONG");
        codeMap.put("달성군", "DALSEONG");
        // 인천
        codeMap.put("계양구", "GYEYANG");
        codeMap.put("남동구", "NAMDONG");
        codeMap.put("미추홀구", "MICHUHOL");
        codeMap.put("부평구", "BUPYEONG");
        codeMap.put("연수구", "YEONSU");
        codeMap.put("강화군", "GANGHWA");
        codeMap.put("옹진군", "ONGJIN");
        // 광주
        codeMap.put("광산구", "GWANGSAN");
        // 대전
        codeMap.put("대덕구", "DAEDEOK");
        codeMap.put("유성구", "YUSEONG");
        // 울산
        codeMap.put("울주군", "ULJU");
        // 세종
        codeMap.put("세종시", "SEJONG");
        // 경기
        codeMap.put("수원시", "SUWON");
        codeMap.put("성남시", "SEONGNAM");
        codeMap.put("고양시", "GOYANG");
        codeMap.put("용인시", "YONGIN");
        codeMap.put("부천시", "BUCHEON");
        codeMap.put("안산시", "ANSAN");
        codeMap.put("안양시", "ANYANG");
        codeMap.put("남양주시", "NAMYANGJU");
        codeMap.put("화성시", "HWASEONG");
        codeMap.put("평택시", "PYEONGTAEK");
        codeMap.put("의정부시", "UIJEONGBU");
        codeMap.put("시흥시", "SIHEUNG");
        codeMap.put("파주시", "PAJU");
        codeMap.put("김포시", "GIMPO");
        codeMap.put("광명시", "GWANGMYEONG");
        codeMap.put("광주시", "GWANGJU");
        codeMap.put("군포시", "GUNPO");
        codeMap.put("하남시", "HANAM");
        codeMap.put("오산시", "OSAN");
        codeMap.put("이천시", "ICHEON");
        codeMap.put("안성시", "ANSEONG");
        codeMap.put("의왕시", "UIWANG");
        codeMap.put("양평군", "YANGPYEONG");
        codeMap.put("여주시", "YEOJU");
        codeMap.put("과천시", "GWACHEON");
        codeMap.put("구리시", "GURI");
        codeMap.put("포천시", "POCHEON");
        codeMap.put("양주시", "YANGJU");
        codeMap.put("동두천시", "DONGDUCHEON");
        codeMap.put("가평군", "GAPYEONG");
        codeMap.put("연천군", "YEONCHEON");
        // 강원
        codeMap.put("춘천시", "CHUNCHEON");
        codeMap.put("원주시", "WONJU");
        codeMap.put("강릉시", "GANGNEUNG");
        codeMap.put("동해시", "DONGHAE");
        codeMap.put("태백시", "TAEBAEK");
        codeMap.put("속초시", "SOKCHO");
        codeMap.put("삼척시", "SAMCHEOK");
        codeMap.put("홍천군", "HONGCHEON");
        codeMap.put("횡성군", "HOENGSEONG");
        codeMap.put("영월군", "YEONGWOL");
        codeMap.put("평창군", "PYEONGCHANG");
        codeMap.put("정선군", "JEONGSEON");
        codeMap.put("철원군", "CHEORWON");
        codeMap.put("화천군", "HWACHEON");
        codeMap.put("양구군", "YANGGU");
        codeMap.put("인제군", "INJE");
        codeMap.put("고성군", "GOSEONG");
        codeMap.put("양양군", "YANGYANG");
        // 충북
        codeMap.put("청주시", "CHEONGJU");
        codeMap.put("충주시", "CHUNGJU");
        codeMap.put("제천시", "JECHEON");
        codeMap.put("보은군", "BOEUN");
        codeMap.put("옥천군", "OKCHEON");
        codeMap.put("영동군", "YEONGDONG");
        codeMap.put("증평군", "JEUNGPYEONG");
        codeMap.put("진천군", "JINCHEON");
        codeMap.put("괴산군", "GOESAN");
        codeMap.put("음성군", "EUMSEONG");
        codeMap.put("단양군", "DANYANG");
        // 충남
        codeMap.put("천안시", "CHEONAN");
        codeMap.put("공주시", "GONGJU");
        codeMap.put("보령시", "BORYEONG");
        codeMap.put("아산시", "ASAN");
        codeMap.put("서산시", "SEOSAN");
        codeMap.put("논산시", "NONSAN");
        codeMap.put("계룡시", "GYERYONG");
        codeMap.put("당진시", "DANGJIN");
        codeMap.put("금산군", "GEUMSAN");
        codeMap.put("부여군", "BUYEO");
        codeMap.put("서천군", "SEOCHEON");
        codeMap.put("청양군", "CHEONGYANG");
        codeMap.put("홍성군", "HONGSEONG");
        codeMap.put("예산군", "YESAN");
        codeMap.put("태안군", "TAEAN");
        // 전북
        codeMap.put("전주시", "JEONJU");
        codeMap.put("군산시", "GUNSAN");
        codeMap.put("익산시", "IKSAN");
        codeMap.put("정읍시", "JEONGEUP");
        codeMap.put("남원시", "NAMWON");
        codeMap.put("김제시", "GIMJE");
        codeMap.put("완주군", "WANJU");
        codeMap.put("진안군", "JINAN");
        codeMap.put("무주군", "MUJU");
        codeMap.put("장수군", "JANGSU");
        codeMap.put("임실군", "IMSIL");
        codeMap.put("순창군", "SUNCHANG");
        codeMap.put("고창군", "GOCHANG");
        codeMap.put("부안군", "BUAN");
        // 전남
        codeMap.put("목포시", "MOKPO");
        codeMap.put("여수시", "YEOSU");
        codeMap.put("순천시", "SUNCHEON");
        codeMap.put("나주시", "NAJU");
        codeMap.put("광양시", "GWANGYANG");
        codeMap.put("담양군", "DAMYANG");
        codeMap.put("곡성군", "GOKSEONG");
        codeMap.put("구례군", "GURYE");
        codeMap.put("고흥군", "GOHEUNG");
        codeMap.put("보성군", "BOSEONG");
        codeMap.put("화순군", "HWASUN");
        codeMap.put("장흥군", "JANGHEUNG");
        codeMap.put("강진군", "GANGJIN");
        codeMap.put("해남군", "HAENAM");
        codeMap.put("영암군", "YEONGAM");
        codeMap.put("무안군", "MUAN");
        codeMap.put("함평군", "HAMPYEONG");
        codeMap.put("영광군", "YEONGGWANG");
        codeMap.put("장성군", "JANGSEONG");
        codeMap.put("완도군", "WANDO");
        codeMap.put("진도군", "JINDO");
        codeMap.put("신안군", "SINAN");
        // 경북
        codeMap.put("포항시", "POHANG");
        codeMap.put("경주시", "GYEONGJU");
        codeMap.put("김천시", "GIMCHEON");
        codeMap.put("안동시", "ANDONG");
        codeMap.put("구미시", "GUMI");
        codeMap.put("영주시", "YEONGJU");
        codeMap.put("영천시", "YEONGCHEON");
        codeMap.put("상주시", "SANGJU");
        codeMap.put("문경시", "MUNGYEONG");
        codeMap.put("경산시", "GYEONGSAN");
        codeMap.put("군위군", "GUNWI");
        codeMap.put("의성군", "UISEONG");
        codeMap.put("청송군", "CHEONGSONG");
        codeMap.put("영양군", "YEONGYANG");
        codeMap.put("영덕군", "YEONGDEOK");
        codeMap.put("청도군", "CHEONGDO");
        codeMap.put("고령군", "GORYEONG");
        codeMap.put("성주군", "SEONGJU");
        codeMap.put("칠곡군", "CHILGOK");
        codeMap.put("예천군", "YECHEON");
        codeMap.put("봉화군", "BONGHWA");
        codeMap.put("울진군", "ULJIN");
        codeMap.put("울릉군", "ULLEUNG");
        // 경남
        codeMap.put("창원시", "CHANGWON");
        codeMap.put("진주시", "JINJU");
        codeMap.put("통영시", "TONGYEONG");
        codeMap.put("사천시", "SACHEON");
        codeMap.put("김해시", "GIMHAE");
        codeMap.put("밀양시", "MIRYANG");
        codeMap.put("거제시", "GEOJE");
        codeMap.put("양산시", "YANGSAN");
        codeMap.put("의령군", "UIRYEONG");
        codeMap.put("함안군", "HAMAN");
        codeMap.put("창녕군", "CHANGNYEONG");
        codeMap.put("남해군", "NAMHAE");
        codeMap.put("하동군", "HADONG");
        codeMap.put("산청군", "SANCHEONG");
        codeMap.put("함양군", "HAMYANG");
        codeMap.put("거창군", "GEOCHANG");
        codeMap.put("합천군", "HAPCHEON");
        // 제주
        codeMap.put("제주시", "JEJU");
        codeMap.put("서귀포시", "SEOGWIPO");

        String code = codeMap.get(name);
        return code != null ? code : name.toUpperCase().replace(" ", "_");
    }
}

