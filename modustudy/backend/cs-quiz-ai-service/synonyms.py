"""
CS 용어 동의어 사전 (옵션 A: 엄격한 동의어만)

⚠️ 규칙:
- 동의어: 같은 개념의 다른 표현 (영어/한글, 오타, 줄임말)
- 제외: 특성, 원리, 관련 개념 (이것들은 CATEGORY_KEYWORDS로 이동)

예시:
- ✅ 동의어: 스택 = stack = 스텍 (같은 것)
- ❌ 제외: LIFO, 후입선출 (스택의 특성이지 스택 자체가 아님)
"""

# CS 용어 동의어 매핑 (엄격한 버전)
# key: 정규화된 단어, value: 동의어 리스트 (같은 개념의 다른 표현만!)
CS_SYNONYMS = {
    # ============================================
    # 자료구조
    # ============================================
    "해시테이블": ["해시맵", "해쉬테이블", "해쉬맵", "hashtable", "hashmap", "hash table", "hash map"],
    "연결리스트": ["링크드리스트", "linkedlist", "linked list", "링크리스트"],
    "배열": ["어레이", "array"],
    "스택": ["stack", "스텍"],  # LIFO, 후입선출 제거
    "큐": ["queue", "큐우"],    # FIFO, 선입선출, 대기열 제거
    "트리": ["tree"],
    "그래프": ["graph", "그레프"],
    "힙": ["heap"],            # 우선순위큐 제거 (다른 개념)
    
    # ============================================
    # 알고리즘
    # ============================================
    "알고리즘": ["algorithm", "알고리듬"],  # 알고, 로직 제거
    "정렬": ["소팅", "sorting", "sort"],
    "탐색": ["서치", "search", "searching"],  # 검색 제거 (다른 뉘앙스)
    "재귀": ["재귀함수", "recursion", "recursive"],
    "동적계획법": ["DP", "다이나믹프로그래밍", "dynamic programming", "동적프로그래밍"],  # 메모이제이션 제거
    "이진탐색": ["이분탐색", "binary search", "바이너리서치", "이진검색"],
    "분할정복": ["divide and conquer", "디바이드앤컨커"],
    
    # ============================================
    # 객체지향
    # ============================================
    "객체지향": ["OOP", "객체지향프로그래밍", "object oriented programming"],
    "클래스": ["class", "클레스"],
    "상속": ["inheritance", "인헤리턴스"],  # extends, 확장 제거
    "다형성": ["polymorphism", "폴리모피즘"],
    "캡슐화": ["encapsulation", "인캡슐레이션"],  # 은닉화, 정보은닉 제거
    "추상화": ["abstraction", "앱스트랙션"],
    "인터페이스": ["interface"],
    
    # ============================================
    # 데이터베이스
    # ============================================
    "데이터베이스": ["DB", "디비", "database", "데이타베이스"],  # DBMS 제거
    "SQL": ["에스큐엘", "structured query language"],  # 쿼리 제거
    "정규화": ["normalization", "노말라이제이션", "노멀라이제이션"],
    "트랜잭션": ["transaction", "트랜젝션", "트렌젝션"],
    "인덱스": ["index", "색인"],
    "조인": ["join", "테이블조인"],
    "SQL인젝션": ["sql injection", "에스큐엘인젝션", "sqli"],  # SQL삽입, 인젝션공격 제거
    
    # ============================================
    # 네트워크
    # ============================================
    "네트워크": ["network", "네트웍"],
    "프로토콜": ["protocol"],
    "HTTP": ["에이치티티피", "hypertext transfer protocol"],
    "TCP": ["티씨피", "transmission control protocol"],
    "IP": ["아이피", "internet protocol"],
    "REST": ["레스트", "restful"],  # REST API 제거
    "API": ["에이피아이", "application programming interface"],
    
    # ============================================
    # 웹개발
    # ============================================
    "쿠키": ["cookie", "cookies"],
    "세션": ["session"],
    "JWT": ["제이더블유티", "json web token"],  # 토큰 제거 (더 넓은 개념)
    "CORS": ["코르스", "cross origin resource sharing"],
    
    # ============================================
    # 보안
    # ============================================
    "해싱": ["hashing", "해쉬", "hash"],  # 해시함수 제거
    "암호화": ["encryption", "인크립션"],
    "인증": ["authentication"],  # auth, 로그인 제거
    "인가": ["authorization"],
    
    # ============================================
    # 시스템/운영체제
    # ============================================
    "컴파일러": ["compiler"],
    "인터프리터": ["interpreter"],
    "운영체제": ["OS", "operating system", "오에스"],
    "프로세스": ["process"],
    "쓰레드": ["thread", "스레드", "쓰래드"],  # 경량프로세스 제거
    "교착상태": ["deadlock", "데드락", "데드록"],
    "메모리": ["memory"],  # 램, RAM 제거 (다른 개념)
    "캐시": ["cache", "캐쉬"],
    "페이징": ["paging"],
    
    # ============================================
    # 프레임워크/도구
    # ============================================
    "스프링": ["spring", "스프링프레임워크"],  # 스프링부트 제거 (다른 것)
    "스프링부트": ["spring boot", "springboot"],
    "JPA": ["제이피에이", "java persistence api"],  # 하이버네이트 제거 (구현체)
    "리액트": ["react", "reactjs", "react.js", "리엑트"],
    "뷰": ["vue", "vuejs", "vue.js", "뷰제이에스"],
    "깃": ["git", "깃"],
    "깃허브": ["github", "깃헙"],  # 깃과 분리
    "도커": ["docker"],  # 컨테이너 제거

    # ============================================
    # 디자인패턴
    # ============================================
    "싱글톤": ["singleton", "싱글턴", "싱글톤패턴"],
    "팩토리": ["factory", "팩토리패턴"],
    "옵저버": ["observer", "옵저버패턴"],
    "디자인패턴": ["design pattern", "설계패턴"],
    
    # ============================================
    # 개발방법론
    # ============================================
    "TDD": ["테스트주도개발", "test driven development"],
    "리팩토링": ["refactoring", "리팩터링", "리펙토링"],
    "CI/CD": ["cicd", "씨아이씨디"],  # 지속적통합/배포 제거 (설명)
    "애자일": ["agile", "에자일"],  # 스크럼 제거 (다른 것)
    
    # ============================================
    # 자바스크립트
    # ============================================
    "클로저": ["closure", "클로져"],
    "호이스팅": ["hoisting", "호이스트"],
    "Promise": ["프로미스", "프라미스"],
    "콜백": ["callback", "콜백함수"],
    "비동기": ["async", "asynchronous", "비동기처리"],
    
    # ============================================
    # 기타
    # ============================================
    "JSON": ["제이슨", "javascript object notation"],
    "XML": ["엑스엠엘", "extensible markup language"],
    "머신러닝": ["machine learning", "ML", "기계학습"],
    "딥러닝": ["deep learning", "DL", "심층학습"],
}


# 관련 키워드 (동의어는 아니지만 높은 연관성 → 카테고리 보너스용)
# 여기에 있는 키워드는 동의어가 아니라 "관련 개념"
RELATED_KEYWORDS = {
    "스택": ["LIFO", "후입선출", "push", "pop"],
    "큐": ["FIFO", "선입선출", "enqueue", "dequeue", "대기열"],
    "힙": ["우선순위큐", "priority queue", "heapify"],
    "해시테이블": ["딕셔너리", "맵", "해시", "충돌", "버킷"],
    "알고리즘": ["로직", "알고"],
    "트랜잭션": ["ACID", "커밋", "롤백"],
    "캡슐화": ["은닉화", "정보은닉", "private"],
    "상속": ["extends", "확장", "부모클래스", "자식클래스"],
    "깃": ["버전관리", "커밋", "브랜치", "머지"],
    "도커": ["컨테이너", "이미지", "dockerfile"],
    "JWT": ["토큰", "bearer", "access token"],
    "메모리": ["램", "RAM", "힙메모리", "스택메모리"],
    "쓰레드": ["경량프로세스", "멀티쓰레드"],
    "동적계획법": ["메모이제이션", "타뷸레이션", "최적부분구조"],
}


# 카테고리별 관련 키워드 (유사도 보너스용)
CATEGORY_KEYWORDS = {
    "자료구조": ["배열", "리스트", "스택", "큐", "트리", "그래프", "해시", "힙", "연결", "노드", "자료", "구조", "LIFO", "FIFO", "후입선출", "선입선출"],
    "알고리즘": ["정렬", "탐색", "검색", "재귀", "동적", "분할", "그리디", "완전", "알고", "시간복잡도", "공간복잡도", "빅오"],
    "객체지향": ["클래스", "객체", "상속", "다형", "캡슐", "추상", "인터페이스", "OOP", "메서드", "속성", "은닉"],
    "데이터베이스": ["SQL", "쿼리", "테이블", "인덱스", "트랜잭션", "정규화", "조인", "DB", "관계형", "ACID"],
    "네트워크": ["HTTP", "TCP", "IP", "프로토콜", "소켓", "통신", "패킷", "요청", "응답", "포트"],
    "보안": ["암호", "해시", "인증", "보안", "취약점", "공격", "방어", "토큰", "권한", "인가"],
    "운영체제": ["프로세스", "쓰레드", "메모리", "스케줄", "동기화", "교착", "페이지", "커널", "컨텍스트"],
    "웹개발": ["쿠키", "세션", "API", "REST", "HTTP", "요청", "응답", "서버", "클라이언트"],
    "프레임워크": ["스프링", "리액트", "뷰", "장고", "플라스크", "노드", "프레임워크", "라이브러리"],
    "시스템": ["컴파일", "인터프리터", "운영체제", "메모리", "CPU", "하드웨어"],
    "기초개념": ["알고리즘", "자료구조", "네트워크", "컴퓨터", "프로그래밍"],
    "프로그래밍기법": ["재귀", "반복", "함수", "메서드", "루프"],
    "백엔드": ["서버", "데이터베이스", "API", "인증"],
    "프론트엔드": ["화면", "UI", "컴포넌트", "렌더링", "DOM"],
    "자바스크립트": ["함수", "변수", "스코프", "클로저", "콜백", "프로미스", "비동기"],
    "협업도구": ["깃", "버전관리", "커밋", "브랜치"],
    "데브옵스": ["배포", "컨테이너", "자동화", "파이프라인", "CI", "CD"],
    "소프트웨어공학": ["테스트", "리팩토링", "패턴", "설계", "TDD"],
    "디자인패턴": ["싱글톤", "팩토리", "옵저버", "패턴", "GoF"],
    "데이터포맷": ["JSON", "XML", "데이터", "포맷"],
    "웹보안": ["CORS", "보안", "인증", "권한", "XSS", "CSRF"],
}


def normalize_word(word: str) -> str:
    """
    단어 정규화 (공백 제거, 소문자 변환)
    
    Args:
        word: 정규화할 단어
        
    Returns:
        str: 정규화된 단어
    """
    if not word:
        return ""
    return word.lower().replace(" ", "").replace("-", "").replace("_", "").strip()


def get_synonyms(word: str) -> set:
    """
    단어의 동의어 집합 반환 (엄격한 동의어만)
    
    Args:
        word: 검색할 단어
        
    Returns:
        set: 동의어 집합 (자기 자신 포함)
    """
    word_normalized = normalize_word(word)
    
    if not word_normalized:
        return set()
    
    synonyms = {word, word.lower(), word_normalized}
    
    # 정규화된 단어로 검색
    for key, values in CS_SYNONYMS.items():
        key_normalized = normalize_word(key)
        values_normalized = [normalize_word(v) for v in values]
        
        # 키와 일치하거나 값 중 하나와 일치하면
        if (word_normalized == key_normalized or 
            word_normalized in values_normalized):
            synonyms.add(key)
            synonyms.update(values)
    
    return synonyms


def are_synonyms(word1: str, word2: str) -> bool:
    """
    두 단어가 동의어인지 확인 (엄격한 기준)
    
    Args:
        word1: 첫 번째 단어
        word2: 두 번째 단어
        
    Returns:
        bool: 동의어 여부
    """
    if not word1 or not word2:
        return False
    
    w1_normalized = normalize_word(word1)
    w2_normalized = normalize_word(word2)
    
    # 정규화 후 같으면 동의어
    if w1_normalized == w2_normalized:
        return True
    
    # 동의어 사전에서 확인
    synonyms1 = get_synonyms(word1)
    
    for syn in synonyms1:
        if normalize_word(syn) == w2_normalized:
            return True
    
    return False


def get_related_keywords(word: str) -> list:
    """
    단어의 관련 키워드 반환 (동의어는 아니지만 연관성 높은 것)
    
    Args:
        word: 검색할 단어
        
    Returns:
        list: 관련 키워드 리스트
    """
    word_normalized = normalize_word(word)
    
    for key, values in RELATED_KEYWORDS.items():
        if normalize_word(key) == word_normalized:
            return values
    
    return []


def is_related_keyword(word1: str, word2: str) -> bool:
    """
    word1이 word2의 관련 키워드인지 확인
    
    Args:
        word1: 확인할 단어
        word2: 기준 단어
        
    Returns:
        bool: 관련 키워드 여부
    """
    related = get_related_keywords(word2)
    word1_normalized = normalize_word(word1)
    
    for keyword in related:
        if normalize_word(keyword) == word1_normalized:
            return True
    
    return False


def get_category_keywords(category: str) -> list:
    """
    카테고리의 관련 키워드 반환
    
    Args:
        category: 카테고리명
        
    Returns:
        list: 관련 키워드 리스트
    """
    return CATEGORY_KEYWORDS.get(category, [])


def is_category_related(word: str, category: str) -> bool:
    """
    단어가 특정 카테고리와 관련 있는지 확인
    
    Args:
        word: 확인할 단어
        category: 카테고리명
        
    Returns:
        bool: 관련 여부
    """
    if not word or not category:
        return False
    
    word_normalized = normalize_word(word)
    keywords = get_category_keywords(category)
    
    for keyword in keywords:
        keyword_normalized = normalize_word(keyword)
        if keyword_normalized in word_normalized or word_normalized in keyword_normalized:
            return True
    
    return False