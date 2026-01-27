"""
퀴즈 생성 LoRA 학습 데이터 + quiz_course DB 데이터 합성 스크립트
- GPT-4o-mini로 구조화된 JSON 퀴즈 생성
- 출력 1: ChatML 포맷 학습 데이터 (LoRA 파인튜닝용)
- 출력 2: quiz_course SQL INSERT (DB 삽입용)

사용법:
  python generate_quiz_training_data.py --api-key YOUR_KEY
  python generate_quiz_training_data.py --api-key YOUR_KEY --resume
"""

import os
import json
import time
import random
import argparse
from pathlib import Path


# ===== 설정 =====
TOTAL_TARGET = 3000
SUMMARY_BASED_COUNT = 1500
KEYWORD_BASED_COUNT = 1000
CODE_BASED_COUNT = 500
BATCH_SIZE = 5

OUTPUT_DIR = os.path.dirname(os.path.abspath(__file__))
OUTPUT_PATH = os.path.join(OUTPUT_DIR, "quiz_training_data.json")
CHECKPOINT_PATH = os.path.join(OUTPUT_DIR, "quiz_checkpoint.json")
SQL_OUTPUT_PATH = os.path.join(OUTPUT_DIR, "..", "..", "docs", "sql", "quiz_course_generated_data.sql")

# ===== 시스템 프롬프트 (JSON 출력) =====
SYSTEM_PROMPT = """당신은 IT 스터디 내용을 바탕으로 복습 퀴즈를 생성하는 전문가입니다.

반드시 아래 JSON 형식으로만 응답하세요:
{"questions": [{"question_text": "질문 내용", "question_type": "MULTIPLE_CHOICE", "options": [{"id": "A", "text": "보기1"}, {"id": "B", "text": "보기2"}, {"id": "C", "text": "보기3"}, {"id": "D", "text": "보기4"}], "correct_answer": "A", "explanation": "해설 내용"}]}

question_type은 다음 중 하나:
- MULTIPLE_CHOICE: 4지선다 (options 필수, correct_answer는 "A"~"D" 중 하나)
- MULTIPLE_CHOICE_MULTIPLE: 복수정답 (options 필수, correct_answer는 JSON 배열 예: ["A","B"])
- SHORT_ANSWER: 단답형 (options는 null, correct_answer는 정답 텍스트)"""

# ===== 토픽 → quiz_course 매핑 =====
# 기존 OS(id=1), NETWORK(id=2)는 이미 있으므로 id=3부터 시작
QUIZ_COURSE_MAP = {
    # course_id: 3 - 데이터베이스
    "CS/데이터베이스/기본":       {"course_id": 3, "section": 1},
    "CS/데이터베이스/설계":       {"course_id": 3, "section": 2},
    "CS/데이터베이스/최적화":     {"course_id": 3, "section": 2},
    "CS/데이터베이스/트랜잭션":   {"course_id": 3, "section": 3},
    "정보처리기사/DB":            {"course_id": 3, "section": 1},

    # course_id: 4 - 알고리즘과 자료구조
    "알고리즘/탐색":              {"course_id": 4, "section": 1},
    "알고리즘/정렬":              {"course_id": 4, "section": 1},
    "알고리즘/DP":                {"course_id": 4, "section": 2},
    "알고리즘/DP심화":            {"course_id": 4, "section": 2},
    "알고리즘/그리디":            {"course_id": 4, "section": 2},
    "알고리즘/기법":              {"course_id": 4, "section": 3},
    "알고리즘/그래프":            {"course_id": 4, "section": 3},
    "알고리즘/문자열":            {"course_id": 4, "section": 3},
    "알고리즘/수학":              {"course_id": 4, "section": 2},
    "알고리즘/시간복잡도":        {"course_id": 4, "section": 1},
    "자료구조/선형":              {"course_id": 4, "section": 1},
    "자료구조/비선형":            {"course_id": 4, "section": 3},
    "자료구조/해시":              {"course_id": 4, "section": 3},
    "자료구조/고급트리":          {"course_id": 4, "section": 3},
    "자료구조/그래프표현":        {"course_id": 4, "section": 1},
    "자료구조/스택큐활용":        {"course_id": 4, "section": 1},

    # course_id: 5 - Java와 Spring
    "Java/기본":                  {"course_id": 5, "section": 1},
    "Java/심화":                  {"course_id": 5, "section": 1},
    "Spring/Core":                {"course_id": 5, "section": 2},
    "Spring/JPA":                 {"course_id": 5, "section": 3},
    "Spring/Security":            {"course_id": 5, "section": 2},
    "Spring/Web":                 {"course_id": 5, "section": 2},

    # course_id: 6 - 프론트엔드 React
    "React/기본":                 {"course_id": 6, "section": 1},
    "React/Hooks":                {"course_id": 6, "section": 2},
    "React/상태관리":             {"course_id": 6, "section": 2},
    "Next.js/SSR":                {"course_id": 6, "section": 3},
    "TypeScript/기본":            {"course_id": 6, "section": 3},
    "CSS/레이아웃":               {"course_id": 6, "section": 1},

    # course_id: 7 - 인프라와 DevOps
    "Docker/기본":                {"course_id": 7, "section": 1},
    "Kubernetes/기본":            {"course_id": 7, "section": 3},
    "CI/CD":                      {"course_id": 7, "section": 2},
    "AWS/클라우드":               {"course_id": 7, "section": 3},
    "모니터링/로깅":              {"course_id": 7, "section": 2},

    # course_id: 8 - 보안
    "보안":                       {"course_id": 8, "section": 1},

    # course_id: 9 - 디자인 패턴
    "디자인패턴/생성":            {"course_id": 9, "section": 1},
    "디자인패턴/구조":            {"course_id": 9, "section": 2},
    "디자인패턴/행위":            {"course_id": 9, "section": 3},
    "아키텍처":                   {"course_id": 9, "section": 3},

    # course_id: 10 - Git과 협업
    "Git/기본":                   {"course_id": 10, "section": 1},
    "Git/협업":                   {"course_id": 10, "section": 2},

    # course_id: 11 - 운영체제 (기존 id=1과 별개, 생성데이터용)
    "CS/운영체제/프로세스":       {"course_id": 11, "section": 1},
    "CS/운영체제/동기화":         {"course_id": 11, "section": 2},
    "CS/운영체제/메모리":         {"course_id": 11, "section": 3},
    "CS/운영체제/스케줄링":       {"course_id": 11, "section": 1},
    "CS/운영체제/파일시스템":     {"course_id": 11, "section": 3},
    "CS/운영체제/인터럽트":       {"course_id": 11, "section": 1},

    # course_id: 12 - 네트워크 (기존 id=2와 별개, 생성데이터용)
    "CS/네트워크/프로토콜":       {"course_id": 12, "section": 1},
    "CS/네트워크/웹":             {"course_id": 12, "section": 2},
    "CS/네트워크/인프라":         {"course_id": 12, "section": 3},
    "CS/네트워크/OSI계층":        {"course_id": 12, "section": 1},
    "CS/네트워크/소켓프로그래밍": {"course_id": 12, "section": 2},

    # course_id: 13 - 테스트/기타
    "테스트":                     {"course_id": 13, "section": 1},
    "정보처리기사/SW공학":        {"course_id": 13, "section": 2},

    # course_id: 14 - AI/ML
    "AI/머신러닝":                {"course_id": 14, "section": 1},
    "AI/딥러닝":                  {"course_id": 14, "section": 2},
    "AI/NLP":                     {"course_id": 14, "section": 2},
    "AI/데이터분석":              {"course_id": 14, "section": 1},
    "AI/MLOps":                   {"course_id": 14, "section": 3},

    # course_id: 15 - Python
    "Python/기본":                {"course_id": 15, "section": 1},
    "Python/심화":                {"course_id": 15, "section": 2},
    "Python/라이브러리":          {"course_id": 15, "section": 3},

    # course_id: 16 - Node.js
    "Node.js/기본":               {"course_id": 16, "section": 1},
    "Node.js/Express":            {"course_id": 16, "section": 2},

    # course_id: 17 - 웹 기초
    "웹/HTML_CSS기초":            {"course_id": 17, "section": 1},
    "웹/브라우저":                {"course_id": 17, "section": 2},
    "웹/성능최적화":              {"course_id": 17, "section": 3},
    "웹/API설계":                 {"course_id": 17, "section": 3},

    # course_id: 18 - 모바일
    "모바일/Android":             {"course_id": 18, "section": 1},
    "모바일/iOS":                 {"course_id": 18, "section": 2},
    "모바일/Flutter":             {"course_id": 18, "section": 3},
    "모바일/ReactNative":         {"course_id": 18, "section": 3},

    # course_id: 19 - Kotlin
    "Kotlin/기본":                {"course_id": 19, "section": 1},
    "Kotlin/코루틴":              {"course_id": 19, "section": 2},

    # course_id: 20 - NoSQL/캐시/메시지큐
    "NoSQL/MongoDB":              {"course_id": 20, "section": 1},
    "NoSQL/Redis":                {"course_id": 20, "section": 2},
    "메시지큐/Kafka":             {"course_id": 20, "section": 3},
    "메시지큐/RabbitMQ":          {"course_id": 20, "section": 3},

    # course_id: 21 - 시스템 디자인
    "시스템디자인/기본":          {"course_id": 21, "section": 1},
    "시스템디자인/사례":          {"course_id": 21, "section": 2},
    "마이크로서비스/기본":        {"course_id": 21, "section": 2},
    "마이크로서비스/DDD":         {"course_id": 21, "section": 3},

    # course_id: 22 - 컴퓨터 구조
    "CS/컴퓨터구조/기본":         {"course_id": 22, "section": 1},
    "CS/컴퓨터구조/메모리체계":   {"course_id": 22, "section": 2},
    "CS/컴퓨터구조/병렬처리":     {"course_id": 22, "section": 3},

    # course_id: 23 - Linux
    "Linux/기본명령어":           {"course_id": 23, "section": 1},
    "Linux/시스템관리":           {"course_id": 23, "section": 2},
    "Linux/셸스크립트":           {"course_id": 23, "section": 3},

    # course_id: 24 - 정보처리기사 확장
    "정보처리기사/데이터통신":    {"course_id": 24, "section": 1},
    "정보처리기사/정보보안":      {"course_id": 24, "section": 2},
    "정보처리기사/운영체제":      {"course_id": 24, "section": 1},
    "정보처리기사/프로그래밍":    {"course_id": 24, "section": 3},

    # course_id: 25 - SQLD
    "SQLD/기본":                  {"course_id": 25, "section": 1},
    "SQLD/SQL활용":               {"course_id": 25, "section": 2},
    "SQLD/최적화":                {"course_id": 25, "section": 3},

    # 기존 코스에 추가 매핑
    "함수형프로그래밍/기본":      {"course_id": 9, "section": 3},   # 디자인패턴
    "클라우드/GCP":               {"course_id": 7, "section": 3},   # DevOps
    "클라우드/Azure":             {"course_id": 7, "section": 3},   # DevOps
    "클라우드/서버리스":          {"course_id": 7, "section": 3},   # DevOps
    "보안/웹보안":                {"course_id": 8, "section": 1},   # 보안
    "보안/인증인가":              {"course_id": 8, "section": 2},   # 보안
    "보안/암호학":                {"course_id": 8, "section": 3},   # 보안
    "CS/데이터베이스/분산DB":      {"course_id": 3, "section": 3},   # DB
    "CS/컴퓨터구조/논리회로":     {"course_id": 22, "section": 1},  # 컴퓨터구조
    "CS/이산수학":                {"course_id": 22, "section": 3},  # 컴퓨터구조
    "데이터엔지니어링/ETL":       {"course_id": 3, "section": 3},   # DB
    "데이터엔지니어링/SQL심화":   {"course_id": 3, "section": 2},   # DB
    "테스트/단위테스트":          {"course_id": 13, "section": 1},  # SW공학
    "테스트/통합E2E":             {"course_id": 13, "section": 1},  # SW공학
    "검색엔진/Elasticsearch":     {"course_id": 20, "section": 2},  # NoSQL/캐시
    "IaC/Terraform":              {"course_id": 7, "section": 3},   # DevOps
    "네트워크/보안프로토콜":      {"course_id": 12, "section": 3},  # 네트워크
    "협업/애자일":                {"course_id": 13, "section": 2},  # SW공학
    "협업/코드리뷰":              {"course_id": 10, "section": 2},  # Git
}

# quiz_course 정의 (SQL 생성용)
QUIZ_COURSES = {
    3:  {"code": "DB", "name": "데이터베이스", "description": "SQL, 정규화, 트랜잭션, 인덱스 등 데이터베이스 핵심 개념을 학습합니다.", "badge_code": "DB_MASTER",
         "sections": {1: ("SQL과 관계형 DB", "SQL 기본 문법과 관계형 데이터베이스 개념을 학습합니다."),
                      2: ("정규화와 설계", "정규화, ERD, 인덱스, 쿼리 최적화를 학습합니다."),
                      3: ("트랜잭션과 동시성", "트랜잭션 ACID, 격리 수준, 락과 동시성 제어를 학습합니다.")}},
    4:  {"code": "ALGORITHM", "name": "알고리즘과 자료구조", "description": "정렬, 탐색, DP, 그래프 등 핵심 알고리즘과 자료구조를 학습합니다.", "badge_code": "ALGO_MASTER",
         "sections": {1: ("탐색과 정렬", "DFS, BFS, 이진탐색, 정렬 알고리즘과 선형 자료구조를 학습합니다."),
                      2: ("DP와 그리디", "다이나믹 프로그래밍과 그리디 알고리즘을 학습합니다."),
                      3: ("고급 자료구조와 기법", "트리, 그래프, 해시, 투포인터 등 고급 기법을 학습합니다.")}},
    5:  {"code": "JAVA_SPRING", "name": "Java와 Spring", "description": "Java 기본/심화와 Spring Boot, JPA 핵심 개념을 학습합니다.", "badge_code": "JAVA_MASTER",
         "sections": {1: ("Java 기본과 심화", "Java OOP, 제네릭, 스트림, 동시성을 학습합니다."),
                      2: ("Spring Boot 핵심", "IoC/DI, AOP, MVC, Security를 학습합니다."),
                      3: ("JPA와 데이터 접근", "영속성 컨텍스트, 연관관계 매핑, N+1 문제를 학습합니다.")}},
    6:  {"code": "REACT", "name": "프론트엔드 React", "description": "React, Hooks, 상태관리, Next.js, TypeScript를 학습합니다.", "badge_code": "REACT_MASTER",
         "sections": {1: ("React 기초", "컴포넌트, JSX, props, state, CSS 레이아웃을 학습합니다."),
                      2: ("Hooks와 상태관리", "useState, useEffect 등 Hooks와 상태관리 라이브러리를 학습합니다."),
                      3: ("Next.js와 TypeScript", "SSR/SSG, App Router, TypeScript 타입 시스템을 학습합니다.")}},
    7:  {"code": "DEVOPS", "name": "인프라와 DevOps", "description": "Docker, CI/CD, Kubernetes, AWS 클라우드를 학습합니다.", "badge_code": "DEVOPS_MASTER",
         "sections": {1: ("Docker와 컨테이너", "Docker 기본, Dockerfile, Docker Compose를 학습합니다."),
                      2: ("CI/CD와 모니터링", "파이프라인 자동화, 배포 전략, 로깅/모니터링을 학습합니다."),
                      3: ("Kubernetes와 클라우드", "K8s 오케스트레이션과 AWS 핵심 서비스를 학습합니다.")}},
    8:  {"code": "SECURITY", "name": "정보보안 기초", "description": "웹 보안 취약점, 인증/암호화, 네트워크 보안을 학습합니다.", "badge_code": "SECURITY_MASTER",
         "sections": {1: ("웹 보안과 방어", "XSS, CSRF, SQL Injection, OWASP Top 10을 학습합니다."),
                      2: ("인증과 암호화", "대칭키/비대칭키, 해싱, SSL/TLS를 학습합니다."),
                      3: ("보안 실무", "보안 헤더, CSP, 인증서 관리를 학습합니다.")}},
    9:  {"code": "DESIGN_PATTERN", "name": "디자인 패턴", "description": "GoF 디자인 패턴과 소프트웨어 아키텍처 원칙을 학습합니다.", "badge_code": "PATTERN_MASTER",
         "sections": {1: ("생성 패턴", "싱글톤, 팩토리, 빌더, 프로토타입 패턴을 학습합니다."),
                      2: ("구조 패턴", "어댑터, 데코레이터, 프록시, 퍼사드 패턴을 학습합니다."),
                      3: ("행위 패턴과 아키텍처", "옵저버, 전략, 커맨드 패턴과 SOLID, 아키텍처를 학습합니다.")}},
    10: {"code": "GIT", "name": "Git과 협업", "description": "Git 기본 명령어, 브랜치 전략, 코드 리뷰 워크플로우를 학습합니다.", "badge_code": "GIT_MASTER",
         "sections": {1: ("Git 기본", "commit, branch, merge, rebase 등 기본 명령어를 학습합니다."),
                      2: ("브랜치 전략과 협업", "Git Flow, PR, 코드 리뷰, 커밋 컨벤션을 학습합니다."),
                      3: ("Git 고급", "stash, cherry-pick, reflog, 서브모듈을 학습합니다.")}},
    11: {"code": "OS_EXT", "name": "운영체제 심화", "description": "프로세스, 동기화, 메모리 관리, CPU 스케줄링을 심화 학습합니다.", "badge_code": "OS_EXT_MASTER",
         "sections": {1: ("프로세스와 스케줄링", "프로세스/스레드, IPC, CPU 스케줄링을 학습합니다."),
                      2: ("동기화", "데드락, 세마포어, 뮤텍스, 임계영역을 학습합니다."),
                      3: ("메모리 관리", "가상 메모리, 페이징, 페이지 교체 알고리즘을 학습합니다.")}},
    12: {"code": "NETWORK_EXT", "name": "네트워크 심화", "description": "TCP/UDP, HTTP, DNS, 네트워크 인프라를 심화 학습합니다.", "badge_code": "NETWORK_EXT_MASTER",
         "sections": {1: ("전송 계층 프로토콜", "TCP/UDP, 흐름제어, 혼잡제어를 학습합니다."),
                      2: ("HTTP와 웹", "HTTP 메서드, REST API, CORS, WebSocket을 학습합니다."),
                      3: ("네트워크 인프라", "DNS, NAT, OSI 7계층, 로드밸런서를 학습합니다.")}},
    13: {"code": "SW_ENG", "name": "소프트웨어 공학", "description": "테스트, SDLC, 요구사항 분석, UML을 학습합니다.", "badge_code": "SW_ENG_MASTER",
         "sections": {1: ("테스트", "유닛/통합/E2E 테스트, TDD, JUnit, Jest를 학습합니다."),
                      2: ("SW 공학", "SDLC, UML, 애자일, 스크럼을 학습합니다."),
                      3: ("품질과 유지보수", "코드 리뷰, 리팩토링, 기술 부채를 학습합니다.")}},
    14: {"code": "AI_ML", "name": "AI와 머신러닝", "description": "머신러닝, 딥러닝, NLP, 데이터분석, MLOps를 학습합니다.", "badge_code": "AI_MASTER",
         "sections": {1: ("머신러닝과 데이터분석", "지도/비지도학습, 회귀, 분류, EDA, Pandas를 학습합니다."),
                      2: ("딥러닝과 NLP", "CNN, RNN, Transformer, BERT, GPT를 학습합니다."),
                      3: ("AI 실무와 MLOps", "전이학습, LoRA, 파인튜닝, MLflow, 모델 서빙을 학습합니다.")}},
    15: {"code": "PYTHON", "name": "Python", "description": "Python 기본 문법, 심화 기능, 주요 라이브러리를 학습합니다.", "badge_code": "PYTHON_MASTER",
         "sections": {1: ("Python 기초", "자료형, 함수, 클래스, 파일 입출력을 학습합니다."),
                      2: ("Python 심화", "데코레이터, 제너레이터, asyncio, 타입힌트를 학습합니다."),
                      3: ("Python 라이브러리", "FastAPI, Django, SQLAlchemy, pytest를 학습합니다.")}},
    16: {"code": "NODEJS", "name": "Node.js", "description": "Node.js 런타임과 Express/NestJS 백엔드 개발을 학습합니다.", "badge_code": "NODEJS_MASTER",
         "sections": {1: ("Node.js 기본", "이벤트 루프, 모듈, Stream, 비동기 처리를 학습합니다."),
                      2: ("Express와 NestJS", "미들웨어, 라우팅, ORM, 인증을 학습합니다."),
                      3: ("Node.js 실무", "성능 최적화, 클러스터링, 배포를 학습합니다.")}},
    17: {"code": "WEB_BASIC", "name": "웹 기초", "description": "HTML/CSS, 브라우저 동작, 웹 성능 최적화, API 설계를 학습합니다.", "badge_code": "WEB_MASTER",
         "sections": {1: ("HTML/CSS와 접근성", "시맨틱 마크업, ARIA, SEO를 학습합니다."),
                      2: ("브라우저와 렌더링", "DOM, CSSOM, Critical Rendering Path를 학습합니다."),
                      3: ("성능 최적화와 API", "Core Web Vitals, REST/GraphQL/gRPC를 학습합니다.")}},
    18: {"code": "MOBILE", "name": "모바일 개발", "description": "Android, iOS, Flutter, React Native 앱 개발을 학습합니다.", "badge_code": "MOBILE_MASTER",
         "sections": {1: ("Android", "Activity, Jetpack, ViewModel, Coroutines를 학습합니다."),
                      2: ("iOS", "Swift, SwiftUI, Combine, Core Data를 학습합니다."),
                      3: ("크로스 플랫폼", "Flutter, React Native, 상태관리를 학습합니다.")}},
    19: {"code": "KOTLIN", "name": "Kotlin", "description": "Kotlin 문법, 코루틴, 함수형 프로그래밍을 학습합니다.", "badge_code": "KOTLIN_MASTER",
         "sections": {1: ("Kotlin 기본", "data class, null safety, 스코프 함수를 학습합니다."),
                      2: ("코루틴", "suspend, Flow, Dispatchers, 에러 핸들링을 학습합니다."),
                      3: ("Kotlin 실무", "Spring Boot + Kotlin, 테스트를 학습합니다.")}},
    20: {"code": "NOSQL_MQ", "name": "NoSQL과 메시지큐", "description": "MongoDB, Redis, Kafka, RabbitMQ를 학습합니다.", "badge_code": "NOSQL_MASTER",
         "sections": {1: ("MongoDB", "Document 모델링, Aggregation, 인덱스를 학습합니다."),
                      2: ("Redis와 Elasticsearch", "캐싱 전략, Pub/Sub, 풀텍스트 검색을 학습합니다."),
                      3: ("Kafka와 RabbitMQ", "이벤트 스트리밍, 메시지 브로커를 학습합니다.")}},
    21: {"code": "SYSTEM_DESIGN", "name": "시스템 디자인", "description": "대규모 시스템 설계, 마이크로서비스, DDD를 학습합니다.", "badge_code": "SYSDESIGN_MASTER",
         "sections": {1: ("시스템 설계 기본", "확장성, CAP, 로드밸런서, 캐싱을 학습합니다."),
                      2: ("설계 사례와 MSA", "URL 단축기, 채팅, API Gateway를 학습합니다."),
                      3: ("DDD와 이벤트 드리븐", "바운디드 컨텍스트, 사가, CQRS를 학습합니다.")}},
    22: {"code": "COMPUTER_ARCH", "name": "컴퓨터 구조", "description": "CPU, 메모리 계층, 병렬 처리를 학습합니다.", "badge_code": "ARCH_MASTER",
         "sections": {1: ("CPU와 명령어", "파이프라이닝, 분기 예측, RISC/CISC를 학습합니다."),
                      2: ("메모리 체계", "캐시, RAM, TLB, 인터럽트를 학습합니다."),
                      3: ("병렬 처리", "멀티코어, GPU, SIMD, 암달의 법칙을 학습합니다.")}},
    23: {"code": "LINUX", "name": "Linux", "description": "리눅스 명령어, 시스템 관리, 셸 스크립트를 학습합니다.", "badge_code": "LINUX_MASTER",
         "sections": {1: ("기본 명령어", "파일, 프로세스, 권한 관리를 학습합니다."),
                      2: ("시스템 관리", "systemd, 사용자/방화벽/패키지 관리를 학습합니다."),
                      3: ("셸 스크립트", "Bash 스크립팅, sed, awk, 자동화를 학습합니다.")}},
    24: {"code": "CERT_EIP", "name": "정보처리기사", "description": "정보처리기사 실기 대비 - 데이터통신, 보안, OS, 프로그래밍을 학습합니다.", "badge_code": "EIP_MASTER",
         "sections": {1: ("데이터통신과 OS", "신호, 다중화, 프로세스, 디스크 스케줄링을 학습합니다."),
                      2: ("정보보안", "암호화, PKI, 방화벽, 접근 통제를 학습합니다."),
                      3: ("프로그래밍", "C, Java, Python 코드 해석, 출력 예측을 학습합니다.")}},
    25: {"code": "CERT_SQLD", "name": "SQLD", "description": "SQLD 자격증 대비 - 데이터 모델링, SQL 활용, 최적화를 학습합니다.", "badge_code": "SQLD_MASTER",
         "sections": {1: ("데이터 모델링", "ERD, 정규화, 식별자, 무결성을 학습합니다."),
                      2: ("SQL 활용", "JOIN, 서브쿼리, 윈도우 함수, 집합 연산을 학습합니다."),
                      3: ("SQL 최적화", "옵티마이저, 실행 계획, 인덱스 설계를 학습합니다.")}},
}


# ===== IT 주제별 키워드 =====

TOPIC_KEYWORDS = {
    # ===== 알고리즘 =====
    "알고리즘/탐색": {
        "keywords": ["DFS", "BFS", "이진 탐색", "선형 탐색", "순차 탐색",
                      "깊이 우선 탐색", "너비 우선 탐색", "A* 알고리즘",
                      "다익스트라", "벨만-포드", "플로이드-워셜", "위상 정렬",
                      "최단 경로", "경로 탐색", "미로 탐색", "그래프 탐색"],
        "context": "코딩테스트 준비 스터디에서 탐색 알고리즘을 학습",
    },
    "알고리즘/정렬": {
        "keywords": ["버블 정렬", "선택 정렬", "삽입 정렬", "퀵 정렬", "머지 정렬",
                      "힙 정렬", "카운팅 정렬", "기수 정렬", "Tim sort",
                      "안정 정렬", "불안정 정렬", "비교 기반 정렬",
                      "시간 복잡도 O(nlogn)", "최선 최악 평균"],
        "context": "알고리즘 스터디에서 정렬 알고리즘의 원리와 비교를 학습",
    },
    "알고리즘/DP": {
        "keywords": ["다이나믹 프로그래밍", "메모이제이션", "탑다운", "바텀업",
                      "피보나치", "배낭 문제", "LCS", "LIS", "최장 공통 부분수열",
                      "최장 증가 부분수열", "동전 교환", "계단 오르기",
                      "최적 부분 구조", "중복 부분 문제", "점화식", "DP 테이블"],
        "context": "코딩테스트 준비 스터디에서 다이나믹 프로그래밍을 학습",
    },
    "알고리즘/DP심화": {
        "keywords": ["트리 DP", "비트마스크 DP", "구간 DP", "확률 DP",
                      "기댓값 DP", "자릿수 DP", "Digit DP",
                      "볼록 껍질 트릭", "Knuth 최적화", "분할 정복 최적화",
                      "외판원 문제", "TSP", "행렬 곱셈 순서",
                      "편집 거리", "팰린드롬 분할"],
        "context": "알고리즘 심화 스터디에서 고급 DP 기법과 최적화를 학습",
    },
    "알고리즘/그리디": {
        "keywords": ["그리디", "탐욕 알고리즘", "활동 선택", "허프만 코딩",
                      "크루스칼", "프림", "최소 신장 트리", "MST",
                      "분할 가능 배낭", "회의실 배정", "거스름돈",
                      "그리디 vs DP", "지역 최적", "전역 최적"],
        "context": "알고리즘 스터디에서 그리디 알고리즘과 최적해 전략을 학습",
    },
    "알고리즘/기법": {
        "keywords": ["투 포인터", "슬라이딩 윈도우", "백트래킹", "분할 정복",
                      "비트마스킹", "브루트포스", "누적합", "구간합",
                      "이분 탐색 응용", "매개변수 탐색", "좌표 압축",
                      "스위핑", "유니온 파인드", "세그먼트 트리"],
        "context": "코딩테스트 준비 스터디에서 자주 사용되는 알고리즘 기법을 학습",
    },
    "알고리즘/그래프": {
        "keywords": ["그래프 이론", "BFS 최단 경로", "0-1 BFS", "다익스트라 우선순위 큐",
                      "벨만-포드 음수 사이클", "플로이드-워셜 경유지",
                      "위상 정렬 큐", "위상 정렬 DFS",
                      "최소 신장 트리", "크루스칼 유니온 파인드", "프림 힙",
                      "강한 연결 요소", "SCC", "타잔", "코사라주",
                      "단절점", "단절선", "이분 그래프", "네트워크 플로우"],
        "context": "그래프 알고리즘 심화 스터디에서 최단 경로와 그래프 이론을 학습",
    },
    "알고리즘/문자열": {
        "keywords": ["KMP", "문자열 매칭", "라빈-카프", "보이어-무어",
                      "트라이", "아호-코라식", "접미사 배열", "Suffix Array",
                      "LCP 배열", "Z 알고리즘", "매내처",
                      "회문 판별", "팰린드롬", "문자열 해싱",
                      "정규표현식", "regex", "파싱"],
        "context": "알고리즘 스터디에서 문자열 탐색과 매칭 알고리즘을 학습",
    },
    "알고리즘/수학": {
        "keywords": ["유클리드 호제법", "GCD", "LCM", "소수 판별",
                      "에라토스테네스의 체", "소인수분해", "모듈러 연산",
                      "페르마 소정리", "확장 유클리드", "중국인 나머지 정리",
                      "조합론", "이항 계수", "파스칼 삼각형",
                      "행렬 거듭제곱", "빠른 거듭제곱", "확률과 기댓값"],
        "context": "수학 기반 알고리즘 스터디에서 정수론과 조합론을 학습",
    },
    "알고리즘/시간복잡도": {
        "keywords": ["시간 복잡도", "공간 복잡도", "Big O", "빅오",
                      "O(1)", "O(log n)", "O(n)", "O(n log n)", "O(n²)", "O(2^n)",
                      "최선", "최악", "평균", "아모타이즈드",
                      "마스터 정리", "점근적 분석", "재귀 시간복잡도",
                      "NP", "NP-완전", "P vs NP"],
        "context": "알고리즘 스터디에서 시간/공간 복잡도 분석 방법을 학습",
    },

    # ===== 자료구조 =====
    "자료구조/선형": {
        "keywords": ["배열", "링크드 리스트", "스택", "큐", "덱",
                      "원형 큐", "우선순위 큐", "이중 연결 리스트",
                      "동적 배열", "ArrayList", "LinkedList",
                      "LIFO", "FIFO", "push", "pop", "peek"],
        "context": "자료구조 스터디에서 선형 자료구조의 구현과 활용을 학습",
    },
    "자료구조/비선형": {
        "keywords": ["트리", "이진 트리", "BST", "AVL 트리", "레드-블랙 트리",
                      "B-Tree", "힙", "최소 힙", "최대 힙",
                      "그래프", "인접 행렬", "인접 리스트", "가중치 그래프",
                      "방향 그래프", "무방향 그래프", "트라이", "세그먼트 트리"],
        "context": "자료구조 스터디에서 비선형 자료구조와 트리/그래프를 학습",
    },
    "자료구조/해시": {
        "keywords": ["해시맵", "해시셋", "해시 함수", "해시 충돌",
                      "체이닝", "오픈 어드레싱", "해시 테이블",
                      "HashMap", "HashSet", "Dictionary",
                      "해시 버킷", "로드 팩터", "리해싱"],
        "context": "자료구조 스터디에서 해시 기반 자료구조와 충돌 해결을 학습",
    },
    "자료구조/고급트리": {
        "keywords": ["B+Tree", "2-3 트리", "스플레이 트리", "펜윅 트리",
                      "세그먼트 트리", "레이지 프로파게이션", "머지 소트 트리",
                      "이진 인덱스 트리", "BIT", "퍼시스턴트 세그먼트 트리",
                      "트리의 지름", "LCA", "최소 공통 조상",
                      "오일러 투어", "HLD", "Heavy-Light Decomposition"],
        "context": "고급 자료구조 스터디에서 세그먼트 트리와 트리 기법을 학습",
    },
    "자료구조/그래프표현": {
        "keywords": ["인접 행렬", "인접 리스트", "간선 리스트",
                      "가중치 그래프", "방향 그래프", "무방향 그래프",
                      "DAG", "사이클 검출", "연결 요소",
                      "차수", "진입 차수", "진출 차수",
                      "희소 그래프", "밀집 그래프", "완전 그래프",
                      "이분 그래프", "플래너 그래프"],
        "context": "자료구조 스터디에서 그래프 표현 방법과 특성을 학습",
    },
    "자료구조/스택큐활용": {
        "keywords": ["괄호 검사", "후위 표기법", "중위 표기법", "수식 계산",
                      "단조 스택", "모노톤 스택", "히스토그램 최대 직사각형",
                      "BFS 레벨 탐색", "원형 큐 구현", "양방향 큐",
                      "최솟값 스택", "Min Stack", "두 스택으로 큐 구현",
                      "우선순위 큐 활용", "다중 소스 BFS"],
        "context": "자료구조 활용 스터디에서 스택/큐 응용 문제를 학습",
    },

    # ===== CS/운영체제 =====
    "CS/운영체제/프로세스": {
        "keywords": ["프로세스", "스레드", "멀티프로세스", "멀티스레드",
                      "컨텍스트 스위칭", "PCB", "프로세스 상태",
                      "좀비 프로세스", "고아 프로세스", "fork", "exec",
                      "IPC", "공유 메모리", "메시지 큐", "파이프", "소켓"],
        "context": "CS 면접 준비 스터디에서 프로세스와 스레드 개념을 학습",
    },
    "CS/운영체제/동기화": {
        "keywords": ["데드락", "세마포어", "뮤텍스", "모니터", "스핀락",
                      "임계 영역", "경쟁 조건", "상호 배제",
                      "데드락 방지", "데드락 회피", "데드락 감지",
                      "뮤텍스 vs 세마포어", "조건 변수", "생산자-소비자"],
        "context": "CS 면접 준비 스터디에서 동기화와 데드락 개념을 학습",
    },
    "CS/운영체제/메모리": {
        "keywords": ["가상 메모리", "페이징", "세그멘테이션", "페이지 폴트",
                      "TLB", "페이지 교체", "LRU", "FIFO", "LFU",
                      "스와핑", "스래싱", "워킹셋", "메모리 단편화",
                      "내부 단편화", "외부 단편화", "메모리 풀"],
        "context": "CS 스터디에서 가상 메모리와 페이징 알고리즘을 학습",
    },
    "CS/운영체제/스케줄링": {
        "keywords": ["CPU 스케줄링", "FCFS", "SJF", "라운드 로빈", "우선순위 스케줄링",
                      "MLQ", "MLFQ", "선점형", "비선점형",
                      "응답 시간", "대기 시간", "반환 시간",
                      "기아 상태", "에이징", "타임 퀀텀"],
        "context": "CS 스터디에서 CPU 스케줄링 알고리즘을 학습",
    },
    "CS/운영체제/파일시스템": {
        "keywords": ["파일 시스템", "inode", "디렉토리 구조", "파일 할당",
                      "연속 할당", "연결 할당", "인덱스 할당",
                      "FAT", "NTFS", "ext4", "저널링",
                      "디스크 스케줄링", "FCFS", "SSTF", "SCAN", "C-SCAN",
                      "RAID", "RAID 0", "RAID 1", "RAID 5", "RAID 6"],
        "context": "운영체제 스터디에서 파일 시스템과 디스크 관리를 학습",
    },
    "CS/운영체제/인터럽트": {
        "keywords": ["인터럽트", "하드웨어 인터럽트", "소프트웨어 인터럽트",
                      "인터럽트 벡터", "인터럽트 핸들러", "ISR",
                      "시스템 콜", "커널 모드", "사용자 모드",
                      "트랩", "폴링", "DMA",
                      "부팅 과정", "BIOS", "UEFI", "부트로더", "MBR"],
        "context": "운영체제 스터디에서 인터럽트와 시스템 콜 메커니즘을 학습",
    },

    # ===== CS/네트워크 =====
    "CS/네트워크/프로토콜": {
        "keywords": ["TCP", "UDP", "HTTP", "HTTPS", "FTP", "SMTP",
                      "3-way handshake", "4-way handshake",
                      "TCP 흐름 제어", "TCP 혼잡 제어", "슬라이딩 윈도우",
                      "신뢰성 전송", "비신뢰성 전송", "포트 번호",
                      "소켓", "TCP vs UDP"],
        "context": "네트워크 스터디에서 전송 계층 프로토콜을 학습",
    },
    "CS/네트워크/웹": {
        "keywords": ["HTTP 메서드", "GET", "POST", "PUT", "DELETE", "PATCH",
                      "상태 코드", "200", "301", "404", "500",
                      "REST API", "RESTful", "CORS", "쿠키", "세션",
                      "JWT", "OAuth", "WebSocket", "HTTP/2", "HTTP/3"],
        "context": "네트워크 스터디에서 웹 프로토콜과 API 통신을 학습",
    },
    "CS/네트워크/인프라": {
        "keywords": ["DNS", "IP", "서브넷", "서브넷 마스크", "CIDR",
                      "NAT", "DHCP", "ARP", "라우팅", "스위칭",
                      "OSI 7계층", "TCP/IP 4계층", "방화벽", "VPN",
                      "로드 밸런서", "CDN", "프록시", "리버스 프록시"],
        "context": "네트워크 스터디에서 네트워크 인프라와 계층 구조를 학습",
    },
    "CS/네트워크/OSI계층": {
        "keywords": ["물리 계층", "데이터링크 계층", "네트워크 계층",
                      "전송 계층", "세션 계층", "표현 계층", "응용 계층",
                      "이더넷", "MAC 주소", "프레임", "패킷", "세그먼트",
                      "허브", "스위치", "라우터", "L4 스위치", "L7 스위치",
                      "MTU", "MSS", "IP 단편화"],
        "context": "네트워크 스터디에서 OSI 7계층 모델과 계층별 장비를 학습",
    },
    "CS/네트워크/소켓프로그래밍": {
        "keywords": ["소켓", "TCP 소켓", "UDP 소켓", "서버 소켓",
                      "bind", "listen", "accept", "connect",
                      "멀티플렉싱", "select", "poll", "epoll", "kqueue",
                      "논블로킹 I/O", "비동기 I/O", "이벤트 드리븐",
                      "Netty", "NIO", "HTTP 서버 구현"],
        "context": "네트워크 프로그래밍 스터디에서 소켓과 I/O 멀티플렉싱을 학습",
    },

    # ===== CS/데이터베이스 =====
    "CS/데이터베이스/기본": {
        "keywords": ["SQL", "DDL", "DML", "DCL", "SELECT", "INSERT", "UPDATE", "DELETE",
                      "JOIN", "INNER JOIN", "LEFT JOIN", "RIGHT JOIN", "FULL OUTER JOIN",
                      "서브쿼리", "인라인 뷰", "스칼라 서브쿼리",
                      "GROUP BY", "HAVING", "ORDER BY", "WHERE", "DISTINCT"],
        "context": "데이터베이스 스터디에서 SQL 기본 문법과 쿼리 작성을 학습",
    },
    "CS/데이터베이스/설계": {
        "keywords": ["정규화", "1NF", "2NF", "3NF", "BCNF", "비정규화",
                      "ERD", "엔티티", "관계", "속성", "기본키", "외래키",
                      "함수적 종속", "이상 현상", "삽입 이상", "갱신 이상", "삭제 이상",
                      "반정규화", "슈퍼키", "후보키"],
        "context": "데이터베이스 스터디에서 정규화와 DB 설계를 학습",
    },
    "CS/데이터베이스/최적화": {
        "keywords": ["인덱스", "B-Tree", "B+Tree", "해시 인덱스", "클러스터드 인덱스",
                      "비클러스터드 인덱스", "커버링 인덱스", "복합 인덱스",
                      "실행 계획", "옵티마이저", "풀 스캔", "인덱스 스캔",
                      "쿼리 튜닝", "슬로우 쿼리", "EXPLAIN"],
        "context": "데이터베이스 스터디에서 인덱스와 쿼리 최적화를 학습",
    },
    "CS/데이터베이스/트랜잭션": {
        "keywords": ["트랜잭션", "ACID", "원자성", "일관성", "격리성", "지속성",
                      "격리 수준", "READ UNCOMMITTED", "READ COMMITTED",
                      "REPEATABLE READ", "SERIALIZABLE",
                      "락", "공유 락", "배타 락", "데드락",
                      "MVCC", "낙관적 락", "비관적 락"],
        "context": "데이터베이스 스터디에서 트랜잭션과 동시성 제어를 학습",
    },
    "CS/데이터베이스/분산DB": {
        "keywords": ["분산 데이터베이스", "샤딩", "파티셔닝", "수평 파티셔닝",
                      "수직 파티셔닝", "레인지 파티셔닝", "해시 파티셔닝",
                      "레플리케이션", "마스터-슬레이브", "마스터-마스터",
                      "CAP 정리", "일관성", "가용성", "분할 허용성",
                      "Eventual Consistency", "강한 일관성", "쿼럼",
                      "2PC", "3PC", "분산 트랜잭션"],
        "context": "데이터베이스 심화 스터디에서 분산 DB와 CAP 이론을 학습",
    },

    # ===== CS/컴퓨터 구조 추가 =====
    "CS/컴퓨터구조/논리회로": {
        "keywords": ["논리 게이트", "AND", "OR", "NOT", "XOR", "NAND", "NOR",
                      "반가산기", "전가산기", "멀티플렉서", "디멀티플렉서",
                      "플립플롭", "래치", "레지스터", "카운터",
                      "부울 대수", "카르노 맵", "진리표",
                      "2의 보수", "고정 소수점", "부동 소수점", "IEEE 754"],
        "context": "컴퓨터 구조 스터디에서 논리 회로와 수 체계를 학습",
    },

    # ===== CS/이산수학 =====
    "CS/이산수학": {
        "keywords": ["집합", "관계", "함수", "명제 논리", "술어 논리",
                      "증명 기법", "귀납법", "귀류법", "비둘기집 원리",
                      "그래프 이론", "오일러 경로", "해밀턴 경로",
                      "트리", "신장 트리", "그래프 색칠",
                      "순열", "조합", "이항 정리", "점화식",
                      "부울 대수", "오토마타", "정규 언어"],
        "context": "이산수학 스터디에서 CS 수학적 기초를 학습",
    },

    # ===== Java/Spring =====
    "Java/기본": {
        "keywords": ["JVM", "JDK", "JRE", "가비지 컬렉션", "GC",
                      "힙 메모리", "스택 메모리", "클래스 로더",
                      "접근 제어자", "상속", "다형성", "캡슐화", "추상화",
                      "인터페이스", "추상 클래스", "제네릭", "어노테이션",
                      "Enum", "Optional", "final", "static"],
        "context": "Java 스터디에서 Java 기본 문법과 OOP 개념을 학습",
    },
    "Java/심화": {
        "keywords": ["람다", "스트림", "함수형 인터페이스", "메서드 레퍼런스",
                      "리플렉션", "프록시", "직렬화", "Comparable", "Comparator",
                      "Collections", "HashMap 내부 구조", "ConcurrentHashMap",
                      "synchronized", "volatile", "Atomic", "ThreadLocal",
                      "ExecutorService", "CompletableFuture"],
        "context": "Java 심화 스터디에서 람다, 스트림, 동시성 프로그래밍을 학습",
    },
    "Spring/Core": {
        "keywords": ["IoC", "DI", "의존성 주입", "Bean", "컨테이너",
                      "ApplicationContext", "BeanFactory", "빈 스코프",
                      "싱글톤", "프로토타입", "컴포넌트 스캔",
                      "AOP", "프록시", "어드바이스", "포인트컷",
                      "인터셉터", "필터", "빈 생명주기", "콜백"],
        "context": "Spring 스터디에서 Spring Core와 IoC/DI를 학습",
    },
    "Spring/JPA": {
        "keywords": ["JPA", "Hibernate", "영속성 컨텍스트", "엔티티 매니저",
                      "1차 캐시", "더티 체킹", "지연 로딩", "즉시 로딩",
                      "N+1 문제", "fetch join", "EntityGraph",
                      "양방향 매핑", "연관관계", "OneToMany", "ManyToOne",
                      "CASCADE", "orphanRemoval", "JPQL", "QueryDSL"],
        "context": "Spring JPA 스터디에서 ORM과 영속성 관리를 학습",
    },
    "Spring/Security": {
        "keywords": ["Spring Security", "인증", "인가", "SecurityFilterChain",
                      "UserDetailsService", "PasswordEncoder", "BCrypt",
                      "JWT", "토큰 기반 인증", "OAuth2", "소셜 로그인",
                      "CSRF", "CORS", "세션 관리", "권한 체크",
                      "hasRole", "hasAuthority", "PreAuthorize"],
        "context": "Spring Security 스터디에서 인증/인가와 보안 설정을 학습",
    },
    "Spring/Web": {
        "keywords": ["Spring MVC", "DispatcherServlet", "Controller",
                      "RequestMapping", "PathVariable", "RequestParam", "RequestBody",
                      "ResponseEntity", "ExceptionHandler", "ControllerAdvice",
                      "Validation", "DTO", "VO", "Entity",
                      "RestTemplate", "WebClient", "OpenFeign"],
        "context": "Spring Web 스터디에서 MVC 패턴과 REST API 개발을 학습",
    },

    # ===== 프론트엔드 =====
    "React/기본": {
        "keywords": ["컴포넌트", "JSX", "props", "state", "이벤트 핸들링",
                      "조건부 렌더링", "리스트 렌더링", "key", "form",
                      "제어 컴포넌트", "비제어 컴포넌트", "Ref",
                      "가상 DOM", "재조정", "React.createElement",
                      "함수 컴포넌트", "클래스 컴포넌트"],
        "context": "React 스터디에서 React 기본 개념과 컴포넌트를 학습",
    },
    "React/Hooks": {
        "keywords": ["useState", "useEffect", "useContext", "useReducer",
                      "useMemo", "useCallback", "useRef", "useLayoutEffect",
                      "커스텀 훅", "훅 규칙", "의존성 배열",
                      "클린업 함수", "stale closure", "렌더링 최적화",
                      "React.memo", "리렌더링 방지"],
        "context": "React Hooks 스터디에서 각 Hook의 사용법과 최적화를 학습",
    },
    "React/상태관리": {
        "keywords": ["Redux", "Redux Toolkit", "Zustand", "Recoil", "Jotai",
                      "Context API", "전역 상태", "로컬 상태",
                      "액션", "리듀서", "스토어", "디스패치",
                      "미들웨어", "Redux Thunk", "Redux Saga",
                      "React Query", "SWR", "서버 상태"],
        "context": "프론트엔드 스터디에서 React 상태 관리 라이브러리를 비교 학습",
    },
    "Next.js/SSR": {
        "keywords": ["Next.js", "SSR", "CSR", "SSG", "ISR",
                      "getServerSideProps", "getStaticProps", "getStaticPaths",
                      "App Router", "Pages Router", "Server Components",
                      "하이드레이션", "프리렌더링", "동적 라우팅",
                      "미들웨어", "API Routes", "Image 최적화"],
        "context": "Next.js 스터디에서 서버 사이드 렌더링과 라우팅을 학습",
    },
    "TypeScript/기본": {
        "keywords": ["TypeScript", "타입", "인터페이스", "type", "interface",
                      "제네릭", "유니온 타입", "인터섹션 타입",
                      "타입 가드", "타입 추론", "타입 단언",
                      "enum", "literal type", "utility type",
                      "Partial", "Required", "Pick", "Omit", "Record"],
        "context": "TypeScript 스터디에서 타입 시스템과 고급 타입을 학습",
    },
    "CSS/레이아웃": {
        "keywords": ["Flexbox", "Grid", "display", "position", "float",
                      "반응형 디자인", "미디어 쿼리", "모바일 퍼스트",
                      "TailwindCSS", "styled-components", "CSS Modules",
                      "BEM", "CSS-in-JS", "CSS 변수",
                      "애니메이션", "트랜지션", "transform"],
        "context": "프론트엔드 스터디에서 CSS 레이아웃과 스타일링을 학습",
    },

    # ===== DevOps/인프라 =====
    "Docker/기본": {
        "keywords": ["Docker", "컨테이너", "이미지", "Dockerfile",
                      "Docker Compose", "볼륨", "네트워크", "포트 매핑",
                      "레이어", "캐시", "멀티스테이지 빌드",
                      "Docker Hub", "레지스트리", "컨테이너 vs VM",
                      "docker run", "docker build", "docker-compose up"],
        "context": "Docker 스터디에서 컨테이너화와 Docker 사용법을 학습",
    },
    "Kubernetes/기본": {
        "keywords": ["Kubernetes", "쿠버네티스", "Pod", "Deployment", "Service",
                      "ReplicaSet", "Namespace", "ConfigMap", "Secret",
                      "Ingress", "PersistentVolume", "PVC",
                      "kubectl", "노드", "클러스터", "마스터",
                      "헬스체크", "롤링 업데이트", "롤백"],
        "context": "Kubernetes 스터디에서 컨테이너 오케스트레이션을 학습",
    },
    "CI/CD": {
        "keywords": ["CI/CD", "Jenkins", "GitHub Actions", "GitLab CI",
                      "파이프라인", "빌드", "테스트", "배포",
                      "블루-그린 배포", "카나리 배포", "롤링 배포",
                      "ArgoCD", "GitOps", "자동화", "트리거",
                      "아티팩트", "환경 변수", "시크릿 관리"],
        "context": "CI/CD 스터디에서 지속적 통합과 배포 자동화를 학습",
    },
    "AWS/클라우드": {
        "keywords": ["AWS", "EC2", "S3", "RDS", "Lambda", "ECS", "EKS",
                      "VPC", "서브넷", "보안 그룹", "IAM",
                      "CloudFront", "Route53", "SQS", "SNS",
                      "DynamoDB", "ElastiCache", "CloudWatch",
                      "오토 스케일링", "로드 밸런서", "ALB", "NLB"],
        "context": "AWS 스터디에서 클라우드 인프라와 서비스를 학습",
    },
    "모니터링/로깅": {
        "keywords": ["Prometheus", "Grafana", "ELK", "Elasticsearch",
                      "Logstash", "Kibana", "APM", "Datadog",
                      "메트릭", "로그", "트레이싱", "알림",
                      "대시보드", "SLI", "SLO", "SLA",
                      "에러 트래킹", "Sentry", "분산 추적"],
        "context": "모니터링 스터디에서 로깅과 메트릭 수집/분석을 학습",
    },

    # ===== 설계/패턴 =====
    "디자인패턴/생성": {
        "keywords": ["싱글톤", "팩토리 메서드", "추상 팩토리", "빌더",
                      "프로토타입", "생성 패턴", "객체 생성",
                      "Singleton", "Factory", "Builder",
                      "정적 팩토리 메서드", "의존성 주입"],
        "context": "디자인 패턴 스터디에서 생성 패턴을 학습",
    },
    "디자인패턴/구조": {
        "keywords": ["어댑터", "브릿지", "컴포지트", "데코레이터",
                      "퍼사드", "플라이웨이트", "프록시",
                      "구조 패턴", "래퍼", "인터페이스 변환",
                      "Adapter", "Decorator", "Proxy", "Facade"],
        "context": "디자인 패턴 스터디에서 구조 패턴을 학습",
    },
    "디자인패턴/행위": {
        "keywords": ["옵저버", "전략", "커맨드", "상태", "템플릿 메서드",
                      "책임 연쇄", "이터레이터", "중재자", "메멘토",
                      "행위 패턴", "Observer", "Strategy", "Command",
                      "State", "Template Method"],
        "context": "디자인 패턴 스터디에서 행위 패턴을 학습",
    },
    "아키텍처": {
        "keywords": ["MVC", "MVVM", "MVP", "클린 아키텍처", "헥사고날",
                      "레이어드 아키텍처", "DDD", "도메인 주도 설계",
                      "CQRS", "이벤트 소싱", "마이크로서비스", "모노리스",
                      "SOLID", "DIP", "ISP", "OCP", "LSP", "SRP",
                      "관심사의 분리", "느슨한 결합"],
        "context": "소프트웨어 아키텍처 스터디에서 설계 원칙과 패턴을 학습",
    },

    # ===== Git/테스트/기타 =====
    "Git/기본": {
        "keywords": ["Git", "commit", "push", "pull", "clone", "fetch",
                      "브랜치", "merge", "rebase", "cherry-pick",
                      "stash", "reset", "revert", "HEAD",
                      "staging area", "워킹 디렉토리", ".gitignore"],
        "context": "Git 스터디에서 Git 기본 명령어와 워크플로우를 학습",
    },
    "Git/협업": {
        "keywords": ["PR", "Pull Request", "코드 리뷰", "Git Flow",
                      "GitHub Flow", "Trunk Based", "브랜치 전략",
                      "컨플릭트", "머지 충돌", "리뷰어",
                      "커밋 컨벤션", "Conventional Commits",
                      "스쿼시 머지", "리베이스 머지", "포크"],
        "context": "Git 협업 스터디에서 팀 워크플로우와 코드 리뷰를 학습",
    },
    "테스트": {
        "keywords": ["유닛 테스트", "통합 테스트", "E2E 테스트", "TDD",
                      "BDD", "JUnit", "Mockito", "Jest", "Pytest",
                      "mock", "stub", "spy", "커버리지",
                      "테스트 더블", "Red-Green-Refactor",
                      "AAA 패턴", "Given-When-Then"],
        "context": "테스트 스터디에서 테스트 전략과 프레임워크를 학습",
    },
    "보안": {
        "keywords": ["XSS", "CSRF", "SQL Injection", "OWASP",
                      "인증", "인가", "암호화", "해싱", "솔트",
                      "bcrypt", "AES", "RSA", "대칭키", "비대칭키",
                      "SSL/TLS", "인증서", "HTTPS",
                      "보안 헤더", "Content Security Policy"],
        "context": "보안 스터디에서 웹 보안 취약점과 방어 기법을 학습",
    },
    "정보처리기사/SW공학": {
        "keywords": ["소프트웨어 공학", "SDLC", "요구사항 분석", "UML",
                      "유스케이스", "시퀀스 다이어그램", "클래스 다이어그램",
                      "애자일", "스크럼", "칸반", "폭포수 모델", "나선형 모델",
                      "V 모델", "프로토타이핑", "테스트 케이스",
                      "블랙박스 테스트", "화이트박스 테스트"],
        "context": "정보처리기사 실기 대비 스터디에서 SW 공학 문제를 풀이",
    },
    "정보처리기사/DB": {
        "keywords": ["ERD", "정규화", "SQL", "DDL", "DML", "DCL",
                      "트랜잭션", "뷰", "인덱스", "프로시저",
                      "트리거", "함수", "커서",
                      "이상 현상", "관계 대수", "관계 해석"],
        "context": "정보처리기사 실기 대비 스터디에서 DB 관련 문제를 풀이",
    },

    # ===== AI/ML =====
    "AI/머신러닝": {
        "keywords": ["머신러닝", "지도학습", "비지도학습", "강화학습",
                      "회귀", "분류", "클러스터링", "차원축소",
                      "과적합", "과소적합", "교차검증", "하이퍼파라미터",
                      "Decision Tree", "Random Forest", "SVM", "KNN",
                      "앙상블", "배깅", "부스팅", "XGBoost"],
        "context": "AI/ML 스터디에서 머신러닝 알고리즘과 학습 방법을 학습",
    },
    "AI/딥러닝": {
        "keywords": ["딥러닝", "신경망", "CNN", "RNN", "LSTM",
                      "Transformer", "어텐션", "Self-Attention",
                      "역전파", "경사하강법", "옵티마이저", "Adam",
                      "배치 정규화", "드롭아웃", "활성화 함수",
                      "GPT", "BERT", "LoRA", "파인튜닝", "전이학습"],
        "context": "딥러닝 스터디에서 신경망 아키텍처와 학습 기법을 학습",
    },
    "AI/NLP": {
        "keywords": ["자연어처리", "토큰화", "임베딩", "Word2Vec", "GloVe",
                      "BERT", "GPT", "T5", "Seq2Seq", "어텐션 메커니즘",
                      "BPE", "SentencePiece", "Hugging Face", "Transformers",
                      "텍스트 분류", "감성 분석", "개체명 인식", "NER"],
        "context": "NLP 스터디에서 자연어처리 모델과 기법을 학습",
    },
    "AI/데이터분석": {
        "keywords": ["Pandas", "NumPy", "Matplotlib", "Seaborn", "데이터 전처리",
                      "결측치 처리", "이상치 탐지", "피처 엔지니어링",
                      "EDA", "상관분석", "데이터 시각화", "Jupyter",
                      "스케일링", "정규화", "원-핫 인코딩", "레이블 인코딩"],
        "context": "데이터 분석 스터디에서 데이터 전처리와 시각화를 학습",
    },
    "AI/MLOps": {
        "keywords": ["MLflow", "Kubeflow", "Airflow", "모델 서빙",
                      "A/B 테스트", "모델 모니터링", "데이터 드리프트",
                      "피처 스토어", "모델 레지스트리", "파이프라인 자동화",
                      "ONNX", "TensorRT", "모델 최적화", "양자화", "가지치기"],
        "context": "MLOps 스터디에서 ML 파이프라인 자동화와 모델 운영을 학습",
    },

    # ===== Python =====
    "Python/기본": {
        "keywords": ["변수", "자료형", "리스트", "튜플", "딕셔너리", "셋",
                      "조건문", "반복문", "함수", "클래스", "모듈",
                      "리스트 컴프리헨션", "슬라이싱", "언패킹",
                      "f-string", "with문", "파일 입출력"],
        "context": "Python 기초 스터디에서 Python 문법과 자료형을 학습",
    },
    "Python/심화": {
        "keywords": ["데코레이터", "제너레이터", "이터레이터", "컨텍스트 매니저",
                      "메타클래스", "디스크립터", "abc 모듈", "프로토콜",
                      "GIL", "멀티프로세싱", "asyncio", "코루틴",
                      "타입힌트", "dataclass", "pydantic", "매직 메서드"],
        "context": "Python 심화 스터디에서 고급 문법과 동시성 프로그래밍을 학습",
    },
    "Python/라이브러리": {
        "keywords": ["requests", "FastAPI", "Flask", "Django", "SQLAlchemy",
                      "celery", "pytest", "logging", "argparse",
                      "os", "pathlib", "json", "csv", "re",
                      "collections", "itertools", "functools", "typing"],
        "context": "Python 스터디에서 주요 라이브러리와 프레임워크를 학습",
    },

    # ===== Node.js/Express =====
    "Node.js/기본": {
        "keywords": ["Node.js", "이벤트 루프", "논블로킹 I/O", "콜백",
                      "npm", "yarn", "pnpm", "package.json", "모듈 시스템",
                      "CommonJS", "ESM", "Buffer", "Stream",
                      "process", "child_process", "cluster", "워커 스레드"],
        "context": "Node.js 스터디에서 런타임 환경과 비동기 처리를 학습",
    },
    "Node.js/Express": {
        "keywords": ["Express", "미들웨어", "라우팅", "에러 핸들링",
                      "NestJS", "Koa", "Fastify", "REST API",
                      "인증 미들웨어", "Passport", "Multer", "CORS",
                      "Prisma", "Sequelize", "TypeORM", "Mongoose"],
        "context": "Node.js 백엔드 스터디에서 Express와 ORM을 학습",
    },

    # ===== 웹 기초 =====
    "웹/HTML_CSS기초": {
        "keywords": ["HTML5", "시맨틱 태그", "form", "input", "table",
                      "meta 태그", "SEO", "접근성", "ARIA", "WAI",
                      "alt 텍스트", "스크린 리더", "키보드 네비게이션",
                      "head", "body", "section", "article", "nav", "aside"],
        "context": "웹 기초 스터디에서 HTML5 시맨틱 마크업과 접근성을 학습",
    },
    "웹/브라우저": {
        "keywords": ["브라우저 렌더링", "DOM", "CSSOM", "렌더 트리",
                      "리플로우", "리페인트", "레이아웃", "페인트",
                      "Critical Rendering Path", "DOMContentLoaded", "load",
                      "Web API", "Web Worker", "Service Worker",
                      "IndexedDB", "LocalStorage", "SessionStorage", "Cache API"],
        "context": "웹 기초 스터디에서 브라우저 동작 원리와 렌더링 과정을 학습",
    },
    "웹/성능최적화": {
        "keywords": ["Lighthouse", "Core Web Vitals", "LCP", "FID", "CLS",
                      "코드 스플리팅", "트리 셰이킹", "번들 최적화",
                      "이미지 최적화", "WebP", "레이지 로딩", "프리로드",
                      "CDN", "캐싱 전략", "HTTP 캐시", "ETag",
                      "gzip", "Brotli", "minification", "서버 사이드 렌더링"],
        "context": "웹 성능 최적화 스터디에서 Core Web Vitals와 최적화 전략을 학습",
    },
    "웹/API설계": {
        "keywords": ["REST API", "RESTful", "API 버저닝", "HATEOAS",
                      "GraphQL", "스키마", "쿼리", "뮤테이션", "구독",
                      "gRPC", "Protocol Buffers", "서비스 정의",
                      "OpenAPI", "Swagger", "API 문서화",
                      "Rate Limiting", "페이지네이션", "필터링"],
        "context": "API 설계 스터디에서 REST/GraphQL/gRPC 설계 패턴을 학습",
    },

    # ===== 모바일 =====
    "모바일/Android": {
        "keywords": ["Android", "Activity", "Fragment", "Intent",
                      "ViewModel", "LiveData", "Room", "Retrofit",
                      "RecyclerView", "Navigation", "Jetpack Compose",
                      "Hilt", "Dagger", "Coroutines", "Flow",
                      "매니페스트", "권한", "생명주기"],
        "context": "Android 스터디에서 안드로이드 앱 개발과 Jetpack을 학습",
    },
    "모바일/iOS": {
        "keywords": ["Swift", "SwiftUI", "UIKit", "View", "ViewController",
                      "Combine", "async/await", "GCD", "DispatchQueue",
                      "Core Data", "UserDefaults", "Codable",
                      "AutoLayout", "Storyboard", "프로토콜 지향",
                      "ARC", "Optional", "클로저", "델리게이트"],
        "context": "iOS 스터디에서 Swift와 SwiftUI 앱 개발을 학습",
    },
    "모바일/Flutter": {
        "keywords": ["Flutter", "Dart", "Widget", "StatelessWidget", "StatefulWidget",
                      "BuildContext", "Navigator", "Route", "Provider",
                      "Riverpod", "Bloc", "GetX", "상태관리",
                      "Hot Reload", "pubspec.yaml", "플랫폼 채널",
                      "Firebase", "REST API 통합"],
        "context": "Flutter 스터디에서 크로스 플랫폼 모바일 앱 개발을 학습",
    },
    "모바일/ReactNative": {
        "keywords": ["React Native", "Expo", "StyleSheet", "FlatList",
                      "Navigation", "AsyncStorage", "네이티브 모듈",
                      "브릿지", "Hermes", "Metro 번들러",
                      "Hot Reloading", "Fast Refresh", "Linking",
                      "Push Notification", "앱 배포", "Code Push"],
        "context": "React Native 스터디에서 크로스 플랫폼 앱 개발을 학습",
    },

    # ===== Kotlin =====
    "Kotlin/기본": {
        "keywords": ["Kotlin", "val", "var", "data class", "sealed class",
                      "null safety", "?.", "?:", "let", "apply", "run", "also",
                      "확장 함수", "스코프 함수", "고차 함수", "인라인 함수",
                      "when", "object", "companion object", "enum class"],
        "context": "Kotlin 스터디에서 Kotlin 기본 문법과 함수형 기능을 학습",
    },
    "Kotlin/코루틴": {
        "keywords": ["코루틴", "suspend", "launch", "async", "await",
                      "CoroutineScope", "Dispatchers", "Job", "Deferred",
                      "Flow", "StateFlow", "SharedFlow", "collect",
                      "withContext", "runBlocking", "supervisorScope",
                      "취소", "타임아웃", "에러 핸들링"],
        "context": "Kotlin 코루틴 스터디에서 비동기 프로그래밍을 학습",
    },

    # ===== NoSQL/캐시/메시지큐 =====
    "NoSQL/MongoDB": {
        "keywords": ["MongoDB", "Document", "Collection", "BSON",
                      "인덱스", "Aggregation Pipeline", "Replica Set",
                      "샤딩", "CRUD", "find", "aggregate",
                      "$match", "$group", "$project", "$lookup",
                      "스키마 설계", "임베딩 vs 레퍼런스", "Atlas"],
        "context": "NoSQL 스터디에서 MongoDB 데이터 모델링과 쿼리를 학습",
    },
    "NoSQL/Redis": {
        "keywords": ["Redis", "인메모리", "String", "List", "Set", "Hash", "Sorted Set",
                      "TTL", "만료", "영속성", "RDB", "AOF",
                      "Pub/Sub", "Lua 스크립트", "트랜잭션",
                      "클러스터", "센티널", "레플리케이션",
                      "캐시 전략", "Look-Aside", "Write-Through", "Write-Back"],
        "context": "Redis 스터디에서 인메모리 데이터 스토어와 캐싱 전략을 학습",
    },
    "메시지큐/Kafka": {
        "keywords": ["Kafka", "토픽", "파티션", "프로듀서", "컨슈머",
                      "컨슈머 그룹", "오프셋", "브로커", "주키퍼",
                      "Kafka Streams", "Kafka Connect", "KSQL",
                      "이벤트 소싱", "CDC", "정확히 한 번 전달",
                      "리텐션", "압축", "ISR", "리플리케이션 팩터"],
        "context": "Kafka 스터디에서 이벤트 스트리밍과 메시지 큐를 학습",
    },
    "메시지큐/RabbitMQ": {
        "keywords": ["RabbitMQ", "AMQP", "Exchange", "Queue", "Binding",
                      "Direct Exchange", "Fanout", "Topic Exchange",
                      "메시지 큐", "DLQ", "Dead Letter Queue",
                      "ACK", "NACK", "프리페치", "QoS",
                      "클러스터링", "미러링", "지연 큐"],
        "context": "RabbitMQ 스터디에서 메시지 브로커와 비동기 통신을 학습",
    },

    # ===== 시스템 디자인 =====
    "시스템디자인/기본": {
        "keywords": ["시스템 디자인", "확장성", "가용성", "일관성",
                      "CAP 정리", "PACELC", "수평적 확장", "수직적 확장",
                      "로드 밸런서", "리버스 프록시", "CDN",
                      "캐싱", "데이터베이스 복제", "파티셔닝", "샤딩",
                      "Rate Limiter", "API Gateway", "서킷 브레이커"],
        "context": "시스템 디자인 스터디에서 대규모 시스템 설계 원칙을 학습",
    },
    "시스템디자인/사례": {
        "keywords": ["URL 단축기", "채팅 시스템", "뉴스 피드", "검색 엔진",
                      "알림 시스템", "파일 저장소", "동영상 스트리밍",
                      "소셜 미디어", "이커머스", "결제 시스템",
                      "예약 시스템", "실시간 순위표", "분산 캐시",
                      "분산 락", "유일 ID 생성기", "웹 크롤러"],
        "context": "시스템 디자인 스터디에서 실제 서비스 설계 사례를 분석",
    },

    # ===== 컴퓨터 구조 =====
    "CS/컴퓨터구조/기본": {
        "keywords": ["CPU", "ALU", "레지스터", "프로그램 카운터",
                      "명령어 사이클", "파이프라이닝", "분기 예측",
                      "RISC", "CISC", "ISA", "명령어 집합",
                      "캐시 메모리", "L1", "L2", "L3", "캐시 히트",
                      "메모리 계층", "지역성 원리", "시간적 지역성", "공간적 지역성"],
        "context": "컴퓨터 구조 스터디에서 CPU 구조와 명령어 처리를 학습",
    },
    "CS/컴퓨터구조/메모리체계": {
        "keywords": ["RAM", "ROM", "SRAM", "DRAM", "플래시 메모리",
                      "가상 메모리", "MMU", "TLB", "페이지 테이블",
                      "버스", "시스템 버스", "주소 버스", "데이터 버스",
                      "인터럽트", "DMA", "폴링", "벡터 인터럽트",
                      "바이트 오더", "빅 엔디안", "리틀 엔디안"],
        "context": "컴퓨터 구조 스터디에서 메모리 시스템과 입출력을 학습",
    },
    "CS/컴퓨터구조/병렬처리": {
        "keywords": ["병렬 처리", "멀티코어", "하이퍼스레딩", "SIMD", "MIMD",
                      "GPU", "GPGPU", "CUDA", "병렬 알고리즘",
                      "암달의 법칙", "구스타프슨의 법칙",
                      "동기화", "원자적 연산", "메모리 모델",
                      "캐시 일관성", "MESI 프로토콜"],
        "context": "컴퓨터 구조 스터디에서 병렬 처리와 멀티코어 아키텍처를 학습",
    },

    # ===== Linux =====
    "Linux/기본명령어": {
        "keywords": ["ls", "cd", "pwd", "mkdir", "rm", "cp", "mv",
                      "cat", "grep", "find", "chmod", "chown",
                      "ps", "top", "kill", "df", "du",
                      "tar", "zip", "ssh", "scp", "wget", "curl"],
        "context": "Linux 스터디에서 리눅스 기본 명령어와 파일 시스템을 학습",
    },
    "Linux/시스템관리": {
        "keywords": ["systemd", "systemctl", "journalctl", "crontab",
                      "사용자 관리", "그룹 관리", "useradd", "usermod",
                      "프로세스 관리", "데몬", "서비스", "백그라운드",
                      "방화벽", "iptables", "ufw", "SELinux",
                      "패키지 관리", "apt", "yum", "dnf"],
        "context": "Linux 시스템 관리 스터디에서 서버 운영과 관리를 학습",
    },
    "Linux/셸스크립트": {
        "keywords": ["Bash", "셸 스크립트", "변수", "조건문", "반복문",
                      "함수", "파이프", "리다이렉션", "sed", "awk",
                      "정규표현식", "환경변수", "PATH", "alias",
                      "exit code", "trap", "xargs", "tee"],
        "context": "셸 스크립트 스터디에서 Bash 스크립팅과 자동화를 학습",
    },

    # ===== 정보처리기사 세분화 =====
    "정보처리기사/데이터통신": {
        "keywords": ["데이터 통신", "신호", "아날로그", "디지털", "변조",
                      "다중화", "TDM", "FDM", "WDM", "회선 교환", "패킷 교환",
                      "프로토콜", "흐름 제어", "오류 제어", "슬라이딩 윈도우",
                      "패리티 비트", "CRC", "해밍 코드"],
        "context": "정보처리기사 실기 대비 스터디에서 데이터 통신 이론을 학습",
    },
    "정보처리기사/정보보안": {
        "keywords": ["대칭키", "비대칭키", "DES", "AES", "RSA",
                      "해시 함수", "MD5", "SHA", "디지털 서명",
                      "PKI", "인증서", "SSL", "TLS", "VPN",
                      "방화벽", "IDS", "IPS", "접근 통제",
                      "DAC", "MAC", "RBAC"],
        "context": "정보처리기사 실기 대비 스터디에서 정보보안 문제를 풀이",
    },
    "정보처리기사/운영체제": {
        "keywords": ["프로세스 상태", "스케줄링", "교착상태", "은행원 알고리즘",
                      "메모리 관리", "페이지 교체", "워킹 셋", "스래싱",
                      "UNIX", "파일 시스템", "inode", "디렉토리 구조",
                      "디스크 스케줄링", "FCFS", "SSTF", "SCAN", "C-SCAN"],
        "context": "정보처리기사 실기 대비 스터디에서 운영체제 문제를 풀이",
    },
    "정보처리기사/프로그래밍": {
        "keywords": ["C언어", "포인터", "구조체", "배열", "함수",
                      "Java", "Python", "출력 예측", "코드 해석",
                      "알고리즘 추적", "변수 추적", "재귀 함수",
                      "클래스", "상속", "오버라이딩", "오버로딩"],
        "context": "정보처리기사 실기 대비 스터디에서 프로그래밍 코드 문제를 풀이",
    },

    # ===== SQLD =====
    "SQLD/기본": {
        "keywords": ["데이터 모델링", "엔티티", "속성", "관계", "식별자",
                      "ERD", "논리 모델", "물리 모델", "정규화",
                      "반정규화", "슈퍼키", "후보키", "대체키",
                      "참조 무결성", "도메인 무결성", "NULL"],
        "context": "SQLD 자격증 대비 스터디에서 데이터 모델링을 학습",
    },
    "SQLD/SQL활용": {
        "keywords": ["SELECT", "WHERE", "GROUP BY", "HAVING", "ORDER BY",
                      "JOIN", "서브쿼리", "집합 연산자", "UNION", "INTERSECT",
                      "윈도우 함수", "RANK", "ROW_NUMBER", "LAG", "LEAD",
                      "CASE", "DECODE", "NVL", "COALESCE", "PIVOT"],
        "context": "SQLD 자격증 대비 스터디에서 SQL 활용을 학습",
    },
    "SQLD/최적화": {
        "keywords": ["옵티마이저", "실행 계획", "힌트", "인덱스",
                      "조인 기법", "Nested Loop", "Sort Merge", "Hash Join",
                      "통계 정보", "카디널리티", "선택도",
                      "테이블 파티셔닝", "뷰", "시퀀스", "동의어"],
        "context": "SQLD 자격증 대비 스터디에서 SQL 최적화를 학습",
    },

    # ===== 마이크로서비스/DDD =====
    "마이크로서비스/기본": {
        "keywords": ["마이크로서비스", "모노리스", "서비스 분리", "API Gateway",
                      "서비스 디스커버리", "로드 밸런싱", "서킷 브레이커",
                      "사가 패턴", "이벤트 드리븐", "CQRS",
                      "분산 트랜잭션", "보상 트랜잭션", "2PC",
                      "서비스 메시", "Istio", "사이드카 패턴"],
        "context": "마이크로서비스 스터디에서 분산 아키텍처 설계를 학습",
    },
    "마이크로서비스/DDD": {
        "keywords": ["DDD", "도메인 주도 설계", "바운디드 컨텍스트",
                      "애그리거트", "엔티티", "값 객체", "도메인 이벤트",
                      "리포지토리", "팩토리", "도메인 서비스",
                      "유비쿼터스 언어", "컨텍스트 맵",
                      "전략적 설계", "전술적 설계", "안티코럽션 레이어"],
        "context": "DDD 스터디에서 도메인 주도 설계의 전략적/전술적 패턴을 학습",
    },

    # ===== 함수형 프로그래밍 =====
    "함수형프로그래밍/기본": {
        "keywords": ["함수형 프로그래밍", "순수 함수", "불변성", "부수 효과",
                      "일급 함수", "고차 함수", "클로저", "커링",
                      "합성 함수", "모나드", "펑터", "map", "flatMap",
                      "filter", "reduce", "fold", "참조 투명성",
                      "재귀", "꼬리 재귀", "패턴 매칭"],
        "context": "함수형 프로그래밍 스터디에서 FP 핵심 개념과 패턴을 학습",
    },

    # ===== 클라우드 확장 =====
    "클라우드/GCP": {
        "keywords": ["GCP", "Google Cloud", "Compute Engine", "Cloud Run",
                      "Cloud Functions", "BigQuery", "Cloud Storage",
                      "Pub/Sub", "Cloud SQL", "Firestore", "Spanner",
                      "GKE", "Cloud Build", "IAM", "VPC"],
        "context": "GCP 스터디에서 Google Cloud 서비스와 아키텍처를 학습",
    },
    "클라우드/Azure": {
        "keywords": ["Azure", "Virtual Machine", "App Service", "Azure Functions",
                      "Cosmos DB", "Blob Storage", "Azure SQL",
                      "Azure DevOps", "AKS", "Service Bus",
                      "Active Directory", "Key Vault", "Monitor"],
        "context": "Azure 스터디에서 Microsoft Azure 서비스를 학습",
    },
    "클라우드/서버리스": {
        "keywords": ["서버리스", "Lambda", "Cloud Functions", "Azure Functions",
                      "FaaS", "BaaS", "콜드 스타트", "웜 스타트",
                      "이벤트 트리거", "API Gateway", "Step Functions",
                      "DynamoDB", "S3 이벤트", "SQS 트리거",
                      "Vercel", "Netlify", "Cloudflare Workers"],
        "context": "서버리스 스터디에서 서버리스 아키텍처와 FaaS를 학습",
    },

    # ===== 보안 세분화 =====
    "보안/웹보안": {
        "keywords": ["XSS", "Stored XSS", "Reflected XSS", "DOM XSS",
                      "CSRF", "SSRF", "SQL Injection", "NoSQL Injection",
                      "XXE", "Command Injection", "Path Traversal",
                      "OWASP Top 10", "보안 헤더", "CSP",
                      "HTTP Only", "Secure", "SameSite"],
        "context": "웹 보안 스터디에서 OWASP 취약점과 방어 기법을 학습",
    },
    "보안/인증인가": {
        "keywords": ["OAuth 2.0", "OpenID Connect", "SAML", "JWT",
                      "Access Token", "Refresh Token", "PKCE",
                      "SSO", "MFA", "TOTP", "FIDO2", "WebAuthn",
                      "RBAC", "ABAC", "ACL", "세션 관리",
                      "토큰 저장소", "토큰 갱신", "토큰 무효화"],
        "context": "인증/인가 스터디에서 OAuth2, JWT, SSO를 학습",
    },
    "보안/암호학": {
        "keywords": ["대칭키 암호", "AES", "DES", "3DES", "ChaCha20",
                      "비대칭키 암호", "RSA", "ECC", "Diffie-Hellman",
                      "해시 함수", "SHA-256", "SHA-3", "HMAC",
                      "솔트", "페퍼", "키 스트레칭", "bcrypt", "scrypt", "Argon2",
                      "PKI", "인증서", "CA", "디지털 서명"],
        "context": "암호학 스터디에서 암호화 알고리즘과 프로토콜을 학습",
    },

    # ===== 데이터 엔지니어링 =====
    "데이터엔지니어링/ETL": {
        "keywords": ["ETL", "ELT", "데이터 파이프라인", "Airflow", "DAG",
                      "Apache Spark", "Hadoop", "MapReduce", "HDFS",
                      "데이터 레이크", "데이터 웨어하우스", "데이터 마트",
                      "스키마 온 리드", "스키마 온 라이트",
                      "배치 처리", "스트림 처리", "Flink"],
        "context": "데이터 엔지니어링 스터디에서 ETL 파이프라인과 빅데이터를 학습",
    },
    "데이터엔지니어링/SQL심화": {
        "keywords": ["분석 함수", "PARTITION BY", "LAG", "LEAD", "NTILE",
                      "CTE", "재귀 CTE", "WITH RECURSIVE",
                      "PIVOT", "UNPIVOT", "LATERAL JOIN",
                      "JSON 함수", "정규표현식", "문자열 함수",
                      "성능 튜닝", "쿼리 플랜", "인덱스 설계"],
        "context": "SQL 심화 스터디에서 분석 함수와 고급 쿼리를 학습",
    },

    # ===== 테스트 세분화 =====
    "테스트/단위테스트": {
        "keywords": ["단위 테스트", "JUnit5", "Mockito", "Jest", "Pytest",
                      "mock", "stub", "spy", "ArgumentCaptor",
                      "테스트 더블", "AAA 패턴", "Given-When-Then",
                      "@Test", "@BeforeEach", "@AfterEach", "Assertions",
                      "파라미터 테스트", "테스트 픽스처"],
        "context": "테스트 스터디에서 단위 테스트 작성과 Mock을 학습",
    },
    "테스트/통합E2E": {
        "keywords": ["통합 테스트", "E2E 테스트", "Selenium", "Cypress",
                      "Playwright", "TestContainers", "Spring Boot Test",
                      "@SpringBootTest", "@DataJpaTest", "@WebMvcTest",
                      "테스트 커버리지", "JaCoCo", "SonarQube",
                      "성능 테스트", "JMeter", "K6", "부하 테스트"],
        "context": "테스트 스터디에서 통합 테스트와 E2E 테스트를 학습",
    },

    # ===== Elasticsearch =====
    "검색엔진/Elasticsearch": {
        "keywords": ["Elasticsearch", "인덱스", "도큐먼트", "매핑",
                      "풀텍스트 검색", "역인덱스", "분석기", "토크나이저",
                      "쿼리 DSL", "match", "term", "bool",
                      "집계", "Kibana", "Logstash", "Beats",
                      "ELK 스택", "클러스터", "샤드", "레플리카"],
        "context": "Elasticsearch 스터디에서 검색 엔진과 ELK 스택을 학습",
    },

    # ===== Terraform/IaC =====
    "IaC/Terraform": {
        "keywords": ["Terraform", "Infrastructure as Code", "HCL",
                      "Provider", "Resource", "Module", "State",
                      "Plan", "Apply", "Destroy", "Backend",
                      "변수", "출력", "데이터 소스", "프로비저너",
                      "Terraform Cloud", "Workspace", "Import"],
        "context": "IaC 스터디에서 Terraform을 활용한 인프라 자동화를 학습",
    },

    # ===== 네트워크 보안/프로토콜 심화 =====
    "네트워크/보안프로토콜": {
        "keywords": ["TLS", "SSL", "핸드셰이크", "인증서 체인",
                      "HTTPS", "mTLS", "Certificate Pinning",
                      "IPSec", "VPN", "터널링", "SSH",
                      "DNSSEC", "DoH", "DoT", "Zero Trust",
                      "네트워크 세그멘테이션", "DDoS 방어"],
        "context": "네트워크 보안 스터디에서 보안 프로토콜과 네트워크 방어를 학습",
    },

    # ===== 소프트스킬/협업 =====
    "협업/애자일": {
        "keywords": ["애자일", "스크럼", "칸반", "스프린트", "백로그",
                      "데일리 스탠드업", "스프린트 리뷰", "회고",
                      "스토리 포인트", "번다운 차트", "벨로시티",
                      "유저 스토리", "에픽", "이터레이션",
                      "Jira", "Confluence", "Notion", "Trello"],
        "context": "애자일 스터디에서 스크럼과 칸반 방법론을 학습",
    },
    "협업/코드리뷰": {
        "keywords": ["코드 리뷰", "PR", "Pull Request", "리뷰어",
                      "코드 스멜", "리팩토링", "클린 코드", "네이밍 컨벤션",
                      "코드 컨벤션", "정적 분석", "ESLint", "Prettier",
                      "Checkstyle", "SonarQube", "코드 품질",
                      "기술 부채", "LGTM", "페어 프로그래밍"],
        "context": "코드 리뷰 스터디에서 효과적인 코드 리뷰와 품질 관리를 학습",
    },
}

# ===== 코드 기반 퀴즈 주제 =====

CODE_TOPICS = [
    # Python 기본
    {"lang": "Python", "topic": "리스트 컴프리헨션과 제너레이터", "difficulty": "중급"},
    {"lang": "Python", "topic": "데코레이터와 클로저", "difficulty": "중급"},
    {"lang": "Python", "topic": "클래스와 상속", "difficulty": "초급"},
    {"lang": "Python", "topic": "매직 메서드(__init__, __str__, __repr__)", "difficulty": "중급"},
    {"lang": "Python", "topic": "이터레이터와 제너레이터 프로토콜", "difficulty": "고급"},
    {"lang": "Python", "topic": "예외 처리 (try/except/finally)", "difficulty": "초급"},
    {"lang": "Python", "topic": "딕셔너리와 defaultdict", "difficulty": "초급"},
    {"lang": "Python", "topic": "문자열 포매팅과 정규표현식", "difficulty": "중급"},
    {"lang": "Python", "topic": "멀티스레딩과 GIL", "difficulty": "고급"},
    {"lang": "Python", "topic": "타입 힌트와 dataclass", "difficulty": "중급"},
    # Python 알고리즘
    {"lang": "Python", "topic": "정렬 알고리즘 구현 (퀵정렬, 머지정렬)", "difficulty": "중급"},
    {"lang": "Python", "topic": "DFS/BFS 구현 (재귀/스택/큐)", "difficulty": "중급"},
    {"lang": "Python", "topic": "다이나믹 프로그래밍 (피보나치, 배낭문제)", "difficulty": "고급"},
    {"lang": "Python", "topic": "이진 탐색 구현과 응용", "difficulty": "중급"},
    {"lang": "Python", "topic": "그래프 최단경로 (다익스트라)", "difficulty": "고급"},
    {"lang": "Python", "topic": "투 포인터와 슬라이딩 윈도우", "difficulty": "중급"},
    {"lang": "Python", "topic": "스택/큐 활용 문제", "difficulty": "초급"},
    {"lang": "Python", "topic": "해시맵 활용 (Counter, 아나그램)", "difficulty": "초급"},
    {"lang": "Python", "topic": "재귀와 백트래킹", "difficulty": "중급"},
    {"lang": "Python", "topic": "유니온 파인드 구현", "difficulty": "고급"},
    # Java 기본
    {"lang": "Java", "topic": "Stream API와 Lambda", "difficulty": "중급"},
    {"lang": "Java", "topic": "Collections 프레임워크 (List, Set, Map)", "difficulty": "초급"},
    {"lang": "Java", "topic": "멀티스레드와 동기화 (synchronized, volatile)", "difficulty": "고급"},
    {"lang": "Java", "topic": "제네릭과 와일드카드", "difficulty": "중급"},
    {"lang": "Java", "topic": "Optional 사용법", "difficulty": "초급"},
    {"lang": "Java", "topic": "인터페이스와 추상 클래스", "difficulty": "초급"},
    {"lang": "Java", "topic": "Comparable과 Comparator", "difficulty": "중급"},
    {"lang": "Java", "topic": "예외 처리와 커스텀 예외", "difficulty": "초급"},
    {"lang": "Java", "topic": "equals()와 hashCode() 오버라이딩", "difficulty": "중급"},
    {"lang": "Java", "topic": "Enum과 활용 패턴", "difficulty": "중급"},
    # Java/Spring
    {"lang": "Java", "topic": "JPA 엔티티 매핑과 연관관계", "difficulty": "중급"},
    {"lang": "Java", "topic": "Spring Bean 생명주기와 스코프", "difficulty": "중급"},
    {"lang": "Java", "topic": "Spring MVC Controller 작성", "difficulty": "초급"},
    {"lang": "Java", "topic": "JPA Repository 쿼리 메서드", "difficulty": "중급"},
    {"lang": "Java", "topic": "Spring Security 설정", "difficulty": "고급"},
    {"lang": "Java", "topic": "Spring AOP와 어노테이션", "difficulty": "고급"},
    {"lang": "Java", "topic": "JUnit5 테스트 작성", "difficulty": "중급"},
    {"lang": "Java", "topic": "Spring Validation과 DTO", "difficulty": "초급"},
    # JavaScript/TypeScript
    {"lang": "JavaScript", "topic": "비동기 처리 (Promise, async/await)", "difficulty": "중급"},
    {"lang": "JavaScript", "topic": "클로저와 스코프 체인", "difficulty": "중급"},
    {"lang": "JavaScript", "topic": "프로토타입과 this 바인딩", "difficulty": "고급"},
    {"lang": "JavaScript", "topic": "배열 고차함수 (map, filter, reduce)", "difficulty": "초급"},
    {"lang": "JavaScript", "topic": "이벤트 루프와 콜백 큐", "difficulty": "고급"},
    {"lang": "JavaScript", "topic": "구조 분해 할당과 스프레드 연산자", "difficulty": "초급"},
    {"lang": "JavaScript", "topic": "모듈 시스템 (import/export, CommonJS)", "difficulty": "중급"},
    {"lang": "JavaScript", "topic": "WeakMap, WeakSet, Symbol", "difficulty": "고급"},
    {"lang": "JavaScript", "topic": "React useState와 useEffect", "difficulty": "중급"},
    {"lang": "JavaScript", "topic": "React useMemo와 useCallback", "difficulty": "중급"},
    {"lang": "JavaScript", "topic": "React Context API 사용법", "difficulty": "중급"},
    {"lang": "JavaScript", "topic": "React 커스텀 훅 만들기", "difficulty": "고급"},
    {"lang": "TypeScript", "topic": "제네릭 타입과 유틸리티 타입", "difficulty": "중급"},
    {"lang": "TypeScript", "topic": "인터페이스와 타입 가드", "difficulty": "중급"},
    {"lang": "TypeScript", "topic": "조건부 타입과 매핑된 타입", "difficulty": "고급"},
    # SQL
    {"lang": "SQL", "topic": "JOIN과 서브쿼리", "difficulty": "중급"},
    {"lang": "SQL", "topic": "윈도우 함수 (ROW_NUMBER, RANK, LAG)", "difficulty": "고급"},
    {"lang": "SQL", "topic": "GROUP BY와 HAVING", "difficulty": "초급"},
    {"lang": "SQL", "topic": "CASE WHEN과 조건 분기", "difficulty": "중급"},
    {"lang": "SQL", "topic": "집계 함수와 NULL 처리 (COALESCE, IFNULL)", "difficulty": "초급"},
    {"lang": "SQL", "topic": "CTE와 재귀 쿼리", "difficulty": "고급"},
    {"lang": "SQL", "topic": "인덱스 활용과 실행 계획", "difficulty": "고급"},
    {"lang": "SQL", "topic": "트랜잭션과 락 (FOR UPDATE)", "difficulty": "고급"},
    # Kotlin
    {"lang": "Kotlin", "topic": "data class와 sealed class", "difficulty": "초급"},
    {"lang": "Kotlin", "topic": "null safety (?., ?:, let, also)", "difficulty": "초급"},
    {"lang": "Kotlin", "topic": "확장 함수와 스코프 함수", "difficulty": "중급"},
    {"lang": "Kotlin", "topic": "코루틴 (launch, async, suspend)", "difficulty": "중급"},
    {"lang": "Kotlin", "topic": "Flow와 StateFlow", "difficulty": "고급"},
    {"lang": "Kotlin", "topic": "제네릭과 reified", "difficulty": "고급"},
    # Python 추가
    {"lang": "Python", "topic": "FastAPI 엔드포인트와 Pydantic 모델", "difficulty": "중급"},
    {"lang": "Python", "topic": "asyncio와 코루틴", "difficulty": "고급"},
    {"lang": "Python", "topic": "단위 테스트 (pytest, fixture, parametrize)", "difficulty": "중급"},
    {"lang": "Python", "topic": "컨텍스트 매니저 (__enter__, __exit__)", "difficulty": "중급"},
    # Go
    {"lang": "Go", "topic": "고루틴과 채널 (goroutine, channel)", "difficulty": "중급"},
    {"lang": "Go", "topic": "인터페이스와 구조체", "difficulty": "초급"},
    {"lang": "Go", "topic": "에러 핸들링 (error, defer, panic, recover)", "difficulty": "중급"},
    {"lang": "Go", "topic": "슬라이스와 맵 활용", "difficulty": "초급"},
    # C/C++
    {"lang": "C", "topic": "포인터와 메모리 관리 (malloc, free)", "difficulty": "중급"},
    {"lang": "C", "topic": "구조체와 함수 포인터", "difficulty": "중급"},
    {"lang": "C", "topic": "파일 입출력 (fopen, fread, fwrite)", "difficulty": "초급"},
    {"lang": "C++", "topic": "STL 컨테이너 (vector, map, set)", "difficulty": "중급"},
    {"lang": "C++", "topic": "스마트 포인터 (unique_ptr, shared_ptr)", "difficulty": "고급"},
    {"lang": "C++", "topic": "템플릿과 제네릭 프로그래밍", "difficulty": "고급"},
    # Bash/Docker
    {"lang": "Bash", "topic": "셸 스크립트 기본 (변수, 조건문, 반복문)", "difficulty": "초급"},
    {"lang": "Bash", "topic": "파이프와 리다이렉션 (|, >, >>, 2>&1)", "difficulty": "중급"},
    {"lang": "Bash", "topic": "sed와 awk 텍스트 처리", "difficulty": "고급"},
    {"lang": "Bash", "topic": "cron과 자동화 스크립트", "difficulty": "중급"},
    {"lang": "Dockerfile", "topic": "Dockerfile 작성 (FROM, COPY, RUN, CMD)", "difficulty": "중급"},
    {"lang": "Dockerfile", "topic": "멀티스테이지 빌드와 레이어 최적화", "difficulty": "고급"},
    {"lang": "YAML", "topic": "docker-compose.yml 작성", "difficulty": "중급"},
    {"lang": "YAML", "topic": "GitHub Actions 워크플로우 작성", "difficulty": "중급"},
    {"lang": "YAML", "topic": "Kubernetes 매니페스트 (Pod, Deployment, Service)", "difficulty": "고급"},
    {"lang": "HCL", "topic": "Terraform 리소스 정의 (AWS EC2, S3)", "difficulty": "중급"},
]

# 코드 토픽 → quiz_course 매핑
CODE_TOPIC_COURSE_MAP = {
    "Python": {"course_id": 15, "section": 1},    # Python
    "Java": {"course_id": 5, "section": 1},        # Java/Spring
    "JavaScript": {"course_id": 6, "section": 1},  # 프론트엔드
    "TypeScript": {"course_id": 6, "section": 3},  # 프론트엔드
    "SQL": {"course_id": 3, "section": 1},          # DB
    "Kotlin": {"course_id": 19, "section": 1},     # Kotlin
    "Go": {"course_id": 16, "section": 3},          # Node.js (서버 범주)
    "C": {"course_id": 22, "section": 1},            # 컴퓨터 구조
    "C++": {"course_id": 22, "section": 1},          # 컴퓨터 구조
    "Bash": {"course_id": 23, "section": 3},        # Linux
    "Dockerfile": {"course_id": 7, "section": 1},   # DevOps
    "YAML": {"course_id": 7, "section": 2},         # DevOps
    "HCL": {"course_id": 7, "section": 3},           # DevOps
}


def get_client(api_key):
    from openai import OpenAI
    return OpenAI(api_key=api_key)


def load_existing_summaries():
    """기존 요약 학습 데이터에서 요약 부분만 추출"""
    summaries = []
    for filename in ["training_data_train.json", "training_data_val.json"]:
        filepath = os.path.join(OUTPUT_DIR, filename)
        if not os.path.exists(filepath):
            continue
        with open(filepath, "r", encoding="utf-8") as f:
            data = json.load(f)
        for item in data:
            text = item.get("text", "")
            topic = item.get("topic", "기타")
            if "<|im_start|>assistant" in text:
                summary_part = text.split("<|im_start|>assistant\n")[-1]
                summary_part = summary_part.replace("<|im_end|>", "").strip()
                if len(summary_part) > 50:
                    summaries.append({"summary": summary_part, "topic": topic})
    return summaries


def parse_json_response(raw):
    """GPT 응답에서 JSON 파싱"""
    json_text = raw
    if "```json" in json_text:
        json_text = json_text.split("```json")[1].split("```")[0].strip()
    elif "```" in json_text:
        json_text = json_text.split("```")[1].split("```")[0].strip()

    parsed = json.loads(json_text)
    questions = parsed.get("questions", [])

    # 검증
    valid_questions = []
    for q in questions:
        if not q.get("question_text") or not q.get("question_type"):
            continue
        qtype = q["question_type"]
        if qtype not in ("MULTIPLE_CHOICE", "MULTIPLE_CHOICE_MULTIPLE", "SHORT_ANSWER"):
            continue
        if qtype in ("MULTIPLE_CHOICE", "MULTIPLE_CHOICE_MULTIPLE") and not q.get("options"):
            continue
        if not q.get("correct_answer"):
            continue
        valid_questions.append(q)

    return valid_questions


def questions_to_text(questions):
    """구조화된 JSON 질문들을 읽기 좋은 텍스트로 변환 (ChatML assistant 응답용)"""
    return json.dumps({"questions": questions}, ensure_ascii=False)


def format_chatml(summary_text, questions_json_str, num_questions=5):
    """ChatML 포맷으로 변환"""
    user_msg = f"다음 스터디 요약 내용을 바탕으로 복습 퀴즈 {num_questions}문제를 JSON으로 생성해주세요.\n\n스터디 요약:\n{summary_text}"
    text = (
        f"<|im_start|>system\n{SYSTEM_PROMPT}<|im_end|>\n"
        f"<|im_start|>user\n{user_msg}<|im_end|>\n"
        f"<|im_start|>assistant\n{questions_json_str}<|im_end|>"
    )
    return text


def generate_quiz_from_summary(client, summary, topic, num_questions=5):
    """기존 요약 기반 퀴즈 생성 (JSON 출력)"""
    prompt = f"""다음은 '{topic}' IT 스터디의 요약 내용입니다. 이 내용을 바탕으로 복습 퀴즈 {num_questions}문제를 JSON으로 생성하세요.

규칙:
1. MULTIPLE_CHOICE 3문제, MULTIPLE_CHOICE_MULTIPLE 1문제, SHORT_ANSWER 1문제 혼합
2. 각 문제에 정답과 해설을 반드시 포함
3. 스터디에서 다룬 핵심 개념을 물어보기
4. 해설은 간결하되 핵심을 짚기

스터디 요약:
{summary}"""

    try:
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": prompt},
            ],
            temperature=0.8,
            max_tokens=2048,
        )
        raw = response.choices[0].message.content.strip()
        questions = parse_json_response(raw)
        if len(questions) >= 3:
            return questions
        return None
    except (json.JSONDecodeError, KeyError, IndexError):
        return None
    except Exception as e:
        print(f"    [API ERROR] {e}")
        return None


def generate_quiz_from_keywords(client, topic_name, topic_info, num_questions=5):
    """주제별 키워드 기반 퀴즈 생성 (요약 + JSON 퀴즈)"""
    keywords = random.sample(topic_info["keywords"], min(8, len(topic_info["keywords"])))
    context = topic_info["context"]

    prompt = f"""당신은 IT 스터디에서 '{topic_name}' 주제를 다루고 있습니다.
{context}하는 상황입니다.

아래 키워드들을 중심으로 먼저 짧은 스터디 요약(5~8문장)을 작성하고,
그 요약을 바탕으로 복습 퀴즈 {num_questions}문제를 JSON으로 생성하세요.

키워드: {', '.join(keywords)}

규칙:
1. 요약은 실제 스터디에서 나올 법한 자연스러운 내용
2. MULTIPLE_CHOICE 3문제, MULTIPLE_CHOICE_MULTIPLE 1문제, SHORT_ANSWER 1문제 혼합
3. 정답과 해설 필수
4. 난이도 초급~고급 혼합

출력 형식 (반드시 JSON만):
먼저 --- 구분자 위에 요약을 작성하고, 아래에 JSON을 작성하세요.

[요약 내용 (5~8문장)]
---
{{"questions": [...]}}"""

    try:
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.85,
            max_tokens=2048,
        )
        content = response.choices[0].message.content.strip()

        # 요약과 JSON 분리
        summary_part = None
        questions = None

        if "---" in content:
            parts = content.split("---", 1)
            summary_part = parts[0].strip()
            json_part = parts[1].strip()
        else:
            # --- 없이 JSON만 온 경우
            json_part = content
            summary_part = f"{topic_name} 주제 스터디 요약"

        questions = parse_json_response(json_part)
        if questions and len(questions) >= 3:
            return summary_part, questions
        return None, None
    except (json.JSONDecodeError, KeyError, IndexError):
        return None, None
    except Exception as e:
        print(f"    [API ERROR] {e}")
        return None, None


def generate_code_quiz(client, code_topic, num_questions=5):
    """코드 기반 퀴즈 생성 (JSON 출력)"""
    prompt = f"""당신은 IT 스터디에서 '{code_topic["lang"]}' 프로그래밍의 '{code_topic["topic"]}' 주제를 학습하고 있습니다.
난이도: {code_topic["difficulty"]}

해당 주제에 대한 짧은 스터디 요약(코드 예시 포함, 5~8문장)을 작성하고,
그 내용을 바탕으로 복습 퀴즈 {num_questions}문제를 JSON으로 생성하세요.

규칙:
1. 코드 관련 질문 포함 (코드 출력 예측, 빈칸 채우기 등)
2. MULTIPLE_CHOICE 3문제, MULTIPLE_CHOICE_MULTIPLE 1문제, SHORT_ANSWER 1문제 혼합
3. 코드는 question_text 안에 포함
4. 정답과 해설 필수

출력 형식:
[요약 내용 (코드 예시 포함)]
---
{{"questions": [...]}}"""

    try:
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": prompt},
            ],
            temperature=0.85,
            max_tokens=2048,
        )
        content = response.choices[0].message.content.strip()

        summary_part = None
        if "---" in content:
            parts = content.split("---", 1)
            summary_part = parts[0].strip()
            json_part = parts[1].strip()
        else:
            json_part = content
            summary_part = f"{code_topic['lang']} {code_topic['topic']} 학습 요약"

        questions = parse_json_response(json_part)
        if questions and len(questions) >= 3:
            return summary_part, questions
        return None, None
    except (json.JSONDecodeError, KeyError, IndexError):
        return None, None
    except Exception as e:
        print(f"    [API ERROR] {e}")
        return None, None


def save_checkpoint(data, generated_counts, sql_questions):
    """체크포인트 저장"""
    checkpoint = {
        "data": data,
        "counts": generated_counts,
        "sql_questions": sql_questions,
    }
    with open(CHECKPOINT_PATH, "w", encoding="utf-8") as f:
        json.dump(checkpoint, f, ensure_ascii=False, indent=2)


def load_checkpoint():
    """체크포인트 로드"""
    if os.path.exists(CHECKPOINT_PATH):
        with open(CHECKPOINT_PATH, "r", encoding="utf-8") as f:
            return json.load(f)
    return None


def export_quiz_course_sql(sql_questions):
    """quiz_course SQL INSERT 파일 생성"""
    # 기존 OS(1), NETWORK(2)가 있으므로 id=3부터
    # 기존 question id 1~60이 있으므로 id=61부터

    lines = []
    lines.append("-- =============================================================================")
    lines.append("-- ModuStudy Quiz Course - GPT-4o-mini 생성 데이터")
    lines.append(f"-- 총 {len(sql_questions)}개 문제")
    lines.append("-- =============================================================================")
    lines.append("")

    # 1. quiz_course INSERT
    lines.append("-- =============================================================================")
    lines.append("-- 1. 퀴즈 코스 (Quiz Courses)")
    lines.append("-- =============================================================================")
    lines.append("INSERT INTO `quiz_course` (`id`, `code`, `name`, `description`, `badge_code`, `total_sections`, `is_active`, `sort_order`)")
    lines.append("VALUES")
    course_values = []
    for cid in sorted(QUIZ_COURSES.keys()):
        c = QUIZ_COURSES[cid]
        desc_escaped = c["description"].replace("'", "''")
        course_values.append(
            f"    ({cid}, '{c['code']}', '{c['name']}', '{desc_escaped}', '{c['badge_code']}', {len(c['sections'])}, TRUE, {cid})"
        )
    lines.append(",\n".join(course_values) + " AS new_values")
    lines.append("ON DUPLICATE KEY UPDATE `name` = new_values.`name`;")
    lines.append("")

    # 2. quiz_course_section INSERT
    lines.append("-- =============================================================================")
    lines.append("-- 2. 퀴즈 코스 섹션 (Quiz Course Sections)")
    lines.append("-- =============================================================================")
    lines.append("INSERT INTO `quiz_course_section` (`quiz_course_id`, `section_number`, `name`, `description`, `total_questions`, `pass_score`)")
    lines.append("VALUES")
    section_values = []

    # 각 코스/섹션별 문제 수 계산
    section_question_counts = {}
    for q in sql_questions:
        key = (q["course_id"], q["section"])
        section_question_counts[key] = section_question_counts.get(key, 0) + 1

    for cid in sorted(QUIZ_COURSES.keys()):
        c = QUIZ_COURSES[cid]
        for snum in sorted(c["sections"].keys()):
            sname, sdesc = c["sections"][snum]
            total_q = section_question_counts.get((cid, snum), 10)
            sdesc_escaped = sdesc.replace("'", "''")
            section_values.append(
                f"    ({cid}, {snum}, '{sname}', '{sdesc_escaped}', {total_q}, 70)"
            )
    lines.append(",\n".join(section_values) + " AS new_values")
    lines.append("ON DUPLICATE KEY UPDATE `name` = new_values.`name`;")
    lines.append("")

    # 3. quiz_course_question INSERT (코스/섹션별로 그룹)
    lines.append("-- =============================================================================")
    lines.append("-- 3. 퀴즈 문제 (Quiz Course Questions)")
    lines.append("-- =============================================================================")

    # 코스/섹션별로 그룹화
    grouped = {}
    for q in sql_questions:
        key = (q["course_id"], q["section"])
        if key not in grouped:
            grouped[key] = []
        grouped[key].append(q)

    question_id = 61  # 기존 60개 이후부터
    for (cid, snum) in sorted(grouped.keys()):
        questions = grouped[(cid, snum)]
        course_name = QUIZ_COURSES.get(cid, {}).get("name", f"코스{cid}")
        section_name = QUIZ_COURSES.get(cid, {}).get("sections", {}).get(snum, (f"섹션{snum}", ""))[0]

        lines.append(f"\n-- {course_name} > {section_name} ({len(questions)}문제)")
        lines.append("INSERT INTO `quiz_course_question` (`id`, `quiz_course_id`, `section_number`, `question_number`, `question_text`, `question_type`, `options`, `correct_answer`, `explanation`)")
        lines.append("VALUES")

        q_values = []
        for qnum, q in enumerate(questions, 1):
            qtext = q["question_text"].replace("'", "''").replace("\\", "\\\\")
            qtype = q["question_type"]

            if q.get("options"):
                opts_json = json.dumps(q["options"], ensure_ascii=False).replace("'", "''")
            else:
                opts_json = None

            if isinstance(q["correct_answer"], list):
                answer = json.dumps(q["correct_answer"], ensure_ascii=False).replace("'", "''")
            else:
                answer = str(q["correct_answer"]).replace("'", "''")

            explanation = (q.get("explanation") or "").replace("'", "''").replace("\\", "\\\\")

            opts_str = f"'{opts_json}'" if opts_json else "NULL"
            q_values.append(
                f"    ({question_id}, {cid}, {snum}, {qnum}, '{qtext}', '{qtype}', "
                f"{opts_str}, '{answer}', '{explanation}')"
            )
            question_id += 1

        lines.append(",\n".join(q_values) + " AS new_values")
        lines.append("ON DUPLICATE KEY UPDATE `question_text` = new_values.`question_text`;")

    lines.append("\n-- =============================================================================")
    lines.append("-- END")
    lines.append("-- =============================================================================")

    sql_path = os.path.normpath(SQL_OUTPUT_PATH)
    os.makedirs(os.path.dirname(sql_path), exist_ok=True)
    with open(sql_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))

    return sql_path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--api-key", required=True, help="OpenAI API key")
    parser.add_argument("--resume", action="store_true", help="체크포인트에서 재개")
    args = parser.parse_args()

    client = get_client(args.api_key)

    all_data = []
    counts = {"summary": 0, "keyword": 0, "code": 0}
    sql_questions = []  # quiz_course용 구조화된 문제 수집

    if args.resume:
        checkpoint = load_checkpoint()
        if checkpoint:
            all_data = checkpoint["data"]
            counts = checkpoint["counts"]
            sql_questions = checkpoint.get("sql_questions", [])
            print(f"체크포인트에서 재개: {len(all_data)}개 (요약:{counts['summary']}, 키워드:{counts['keyword']}, 코드:{counts['code']})")
            print(f"  SQL 문제: {len(sql_questions)}개")

    print("=" * 70)
    print(f"퀴즈 학습 데이터 + quiz_course DB 데이터 생성")
    print(f"목표: 요약기반 {SUMMARY_BASED_COUNT} + 키워드기반 {KEYWORD_BASED_COUNT} + 코드기반 {CODE_BASED_COUNT} = {TOTAL_TARGET}")
    print("=" * 70)

    # ===== 1단계: 기존 요약 기반 퀴즈 =====
    if counts["summary"] < SUMMARY_BASED_COUNT:
        print(f"\n[1/3] 기존 요약 기반 퀴즈 생성 ({counts['summary']}/{SUMMARY_BASED_COUNT})")
        summaries = load_existing_summaries()
        print(f"  기존 요약 데이터 로드: {len(summaries)}개")

        if not summaries:
            print("  [WARNING] 기존 요약 데이터가 없습니다. training_data_train.json 확인 필요")
        else:
            random.shuffle(summaries)
            idx = counts["summary"]
            failed = 0
            while counts["summary"] < SUMMARY_BASED_COUNT and idx < len(summaries) * 2:
                item = summaries[idx % len(summaries)]
                questions = generate_quiz_from_summary(client, item["summary"], item["topic"])

                if questions:
                    questions_json = questions_to_text(questions)
                    chatml = format_chatml(item["summary"], questions_json)
                    all_data.append({
                        "text": chatml,
                        "type": "quiz_from_summary",
                        "topic": item["topic"],
                    })

                    # SQL 용 문제 수집 (토픽 매핑 시도)
                    topic_key = item["topic"]
                    if topic_key in QUIZ_COURSE_MAP:
                        mapping = QUIZ_COURSE_MAP[topic_key]
                        for q in questions:
                            sql_questions.append({**q, **mapping})

                    counts["summary"] += 1
                    failed = 0
                else:
                    failed += 1
                    if failed > 10:
                        print(f"  [WARNING] 연속 실패 {failed}회, 잠시 대기...")
                        time.sleep(5)
                        failed = 0

                idx += 1
                if counts["summary"] % 10 == 0:
                    print(f"  진행: {counts['summary']}/{SUMMARY_BASED_COUNT} (SQL: {len(sql_questions)}개)")
                    save_checkpoint(all_data, counts, sql_questions)

                time.sleep(0.3)

    # ===== 2단계: 키워드 기반 퀴즈 =====
    if counts["keyword"] < KEYWORD_BASED_COUNT:
        print(f"\n[2/3] 키워드 기반 퀴즈 생성 ({counts['keyword']}/{KEYWORD_BASED_COUNT})")
        topics = list(TOPIC_KEYWORDS.keys())
        failed = 0

        while counts["keyword"] < KEYWORD_BASED_COUNT:
            topic_name = random.choice(topics)
            topic_info = TOPIC_KEYWORDS[topic_name]

            summary, questions = generate_quiz_from_keywords(client, topic_name, topic_info)

            if summary and questions:
                questions_json = questions_to_text(questions)
                chatml = format_chatml(summary, questions_json)
                all_data.append({
                    "text": chatml,
                    "type": "quiz_from_keywords",
                    "topic": topic_name,
                })

                # SQL 용 문제 수집
                if topic_name in QUIZ_COURSE_MAP:
                    mapping = QUIZ_COURSE_MAP[topic_name]
                    for q in questions:
                        sql_questions.append({**q, **mapping})

                counts["keyword"] += 1
                failed = 0
            else:
                failed += 1
                if failed > 10:
                    print(f"  [WARNING] 연속 실패 {failed}회, 잠시 대기...")
                    time.sleep(5)
                    failed = 0

            if counts["keyword"] % 10 == 0:
                print(f"  진행: {counts['keyword']}/{KEYWORD_BASED_COUNT} (SQL: {len(sql_questions)}개)")
                save_checkpoint(all_data, counts, sql_questions)

            time.sleep(0.3)

    # ===== 3단계: 코드 기반 퀴즈 =====
    if counts["code"] < CODE_BASED_COUNT:
        print(f"\n[3/3] 코드 기반 퀴즈 생성 ({counts['code']}/{CODE_BASED_COUNT})")
        failed = 0

        while counts["code"] < CODE_BASED_COUNT:
            code_topic = random.choice(CODE_TOPICS)

            summary, questions = generate_code_quiz(client, code_topic)

            if summary and questions:
                questions_json = questions_to_text(questions)
                chatml = format_chatml(summary, questions_json)
                all_data.append({
                    "text": chatml,
                    "type": "quiz_from_code",
                    "topic": f"{code_topic['lang']}/{code_topic['topic']}",
                })

                # SQL 용 문제 수집
                lang = code_topic["lang"]
                if lang in CODE_TOPIC_COURSE_MAP:
                    mapping = CODE_TOPIC_COURSE_MAP[lang]
                    for q in questions:
                        sql_questions.append({**q, **mapping})

                counts["code"] += 1
                failed = 0
            else:
                failed += 1
                if failed > 10:
                    print(f"  [WARNING] 연속 실패 {failed}회, 잠시 대기...")
                    time.sleep(5)
                    failed = 0

            if counts["code"] % 10 == 0:
                print(f"  진행: {counts['code']}/{CODE_BASED_COUNT} (SQL: {len(sql_questions)}개)")
                save_checkpoint(all_data, counts, sql_questions)

            time.sleep(0.3)

    # ===== ChatML 학습 데이터 저장 =====
    random.shuffle(all_data)
    split_idx = int(len(all_data) * 0.9)
    train_data = all_data[:split_idx]
    val_data = all_data[split_idx:]

    with open(OUTPUT_PATH, "w", encoding="utf-8") as f:
        json.dump(all_data, f, ensure_ascii=False, indent=2)

    train_path = os.path.join(OUTPUT_DIR, "quiz_training_data_train.json")
    val_path = os.path.join(OUTPUT_DIR, "quiz_training_data_val.json")

    with open(train_path, "w", encoding="utf-8") as f:
        json.dump(train_data, f, ensure_ascii=False, indent=2)
    with open(val_path, "w", encoding="utf-8") as f:
        json.dump(val_data, f, ensure_ascii=False, indent=2)

    # ===== quiz_course SQL 저장 =====
    sql_path = export_quiz_course_sql(sql_questions)

    # ===== 통계 =====
    type_counts = {}
    topic_counts = {}
    for item in all_data:
        t = item.get("type", "unknown")
        type_counts[t] = type_counts.get(t, 0) + 1
        tp = item.get("topic", "unknown")
        topic_counts[tp] = topic_counts.get(tp, 0) + 1

    # SQL 통계
    sql_course_counts = {}
    for q in sql_questions:
        cid = q["course_id"]
        name = QUIZ_COURSES.get(cid, {}).get("name", f"코스{cid}")
        sql_course_counts[name] = sql_course_counts.get(name, 0) + 1

    print(f"\n{'='*70}")
    print(f"퀴즈 학습 데이터 + DB 데이터 생성 완료!")
    print(f"{'='*70}")
    print(f"\n  [ChatML 학습 데이터]")
    print(f"  총 데이터:  {len(all_data)}개")
    print(f"  학습:       {len(train_data)}개")
    print(f"  검증:       {len(val_data)}개")
    print(f"\n  유형별:")
    for t, c in sorted(type_counts.items()):
        print(f"    {t}: {c}개")
    print(f"\n  [quiz_course SQL 데이터]")
    print(f"  총 문제:    {len(sql_questions)}개")
    print(f"  코스별:")
    for name, c in sorted(sql_course_counts.items()):
        print(f"    {name}: {c}개")
    print(f"\n  저장 파일:")
    print(f"    학습데이터: {OUTPUT_PATH}")
    print(f"    SQL:        {sql_path}")
    print(f"{'='*70}")

    # 체크포인트 삭제
    if os.path.exists(CHECKPOINT_PATH):
        os.remove(CHECKPOINT_PATH)


if __name__ == "__main__":
    main()
