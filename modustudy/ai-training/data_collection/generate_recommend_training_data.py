"""
스터디 계획 생성 LoRA 학습 데이터 합성 스크립트 (v2)
- 사용자 자유 주제 + 프로필(기술스택/일정) → 스터디 전체 폼 자동 생성
- GPT-4o-mini로 구조화된 JSON 학습 데이터 생성
- 출력: ChatML 포맷 학습 데이터 (LoRA 파인튜닝용)

입력 형식:
  - topic: 사용자가 자유 텍스트로 입력한 스터디 주제
  - techStack: 사용자 프로필 기술스택 (배열)
  - schedule: 사용자 가용 일정 (배열)

출력 형식 (JSON):
  - name: 스터디 제목
  - intro: 한줄 소개
  - description: 상세 설명
  - topic: 매칭된 토픽명
  - format: 스터디 형식
  - difficulty: 난이도 (BEGINNER|INTERMEDIATE|ADVANCED)
  - goal: 학습 목표
  - textbook: 교재/자료
  - prerequisites: 선행 조건
  - processDetail: 진행 방식 상세
  - durationWeeks: 스터디 기간 (2~8주)
  - scheduleSuggestion: {"days": ["요일"], "time": "시간대"}

사용법:
  python generate_recommend_training_data.py --api-key YOUR_KEY
  python generate_recommend_training_data.py --api-key YOUR_KEY --resume
"""

import os
import json
import time
import random
import argparse
from pathlib import Path


# ===== 설정 =====
TOTAL_TARGET = 2000
BATCH_SIZE = 5
OUTPUT_DIR = os.path.dirname(os.path.abspath(__file__))
OUTPUT_PATH = os.path.join(OUTPUT_DIR, "recommend_training_data.json")
CHECKPOINT_PATH = os.path.join(OUTPUT_DIR, "recommend_checkpoint.json")

# ===== 시스템 프롬프트 =====
SYSTEM_PROMPT = """당신은 IT 스터디 계획을 자동 생성하는 전문가입니다.
사용자가 원하는 스터디 주제와 기술 스택, 가용 일정을 분석하여 완성된 스터디 계획을 JSON으로 생성합니다.

반드시 아래 JSON 형식으로만 응답하세요:
{"name": "스터디 제목", "intro": "한줄 소개 (30자 내외)", "description": "상세 설명 (3~5문장)", "topic": "매칭된 세부주제명", "format": "스터디 형식", "difficulty": "BEGINNER|INTERMEDIATE|ADVANCED", "goal": "구체적 학습 목표", "textbook": "교재 또는 학습 자료", "prerequisites": "선행 조건 (없으면 '없음')", "processDetail": "주차별 또는 회차별 진행 방식 상세", "durationWeeks": 4, "scheduleSuggestion": {"days": ["월", "수"], "time": "19:00-21:00"}}

규칙:
- name은 구체적이고 매력적인 스터디 제목 (15~30자)
- intro는 스터디 카드 썸네일에 표시될 한줄 소개
- description은 참여자가 읽고 참가 결정할 수 있는 상세 설명
- topic은 아래 토픽 트리의 세부주제 중 가장 적합한 것 선택
- format은: 문제 풀이, 독서/책 스터디, 강의 수강, 프로젝트, 모의 면접, 코드 리뷰, 발표/세미나, 토론 중 택 1
- durationWeeks는 스터디 총 기간 (2~8주). 주제 난이도와 범위에 따라 적절히 설정
- processDetail은 durationWeeks에 맞는 주차별 구체적 진행 계획
- scheduleSuggestion의 days는 사용자 가용 일정을 고려하여 배정
- difficulty 설정 기준:
  - BEGINNER: 해당 분야 경험이 없는 사용자
  - INTERMEDIATE: 기초 지식이 있고 실전 경험을 쌓고 싶은 사용자
  - ADVANCED: 실무 경험이 있거나 기술 스택에 해당 기술을 이미 보유한 사용자
  - 사용자가 이미 해당 주제의 관련 기술을 보유하고 있으면 INTERMEDIATE 이상으로 설정"""


# =============================================
# 토픽 트리 (DataInitializer.java와 1:1 매칭)
# =============================================

TOPIC_TREE = {
    "알고리즘/코딩테스트": [
        "백준", "프로그래머스", "SWEA", "LeetCode", "코딩테스트 대비",
        # 세부 알고리즘 유형
        "DP/동적프로그래밍", "그래프 탐색", "그리디 알고리즘", "정렬 알고리즘",
        "이분탐색", "투포인터/슬라이딩윈도우", "백트래킹/완전탐색",
        "문자열 알고리즘", "수학/정수론", "시간복잡도 분석",
        "세그먼트 트리/펜윅 트리", "유니온 파인드", "최단경로 알고리즘",
    ],
    "CS 기초": [
        "자료구조", "알고리즘 이론", "운영체제", "네트워크", "데이터베이스",
        "컴퓨터구조", "디자인패턴", "시스템 설계",
        # 세부 CS 주제
        "프로세스/스레드", "동기화/데드락", "메모리 관리", "CPU 스케줄링",
        "TCP/IP", "HTTP/웹 프로토콜", "DNS/라우팅", "OSI 7계층",
        "SQL 활용", "정규화/인덱스", "트랜잭션/동시성", "분산 데이터베이스",
        "캐시/메모리 계층", "논리회로", "보안 기초",
        "소프트웨어 공학", "이산수학", "SOLID 원칙",
        "생성 패턴", "구조 패턴", "행위 패턴",
    ],
    "프론트엔드": [
        "HTML/CSS", "JavaScript", "TypeScript", "React", "Vue", "Next.js", "웹 접근성/성능",
        # 세부 프론트엔드 주제
        "React Hooks 심화", "상태관리 라이브러리", "CSS 레이아웃/반응형",
        "웹 애니메이션", "프론트엔드 테스트", "웹소켓/실시간 통신",
        "PWA", "GraphQL 클라이언트", "Svelte", "Angular",
        "Storybook/컴포넌트 문서화", "번들러/빌드 도구",
    ],
    "백엔드": [
        "Java/Spring", "Python/Django", "Python/FastAPI", "Node.js/Express", "Go", "Kotlin", "API 설계",
        # 세부 백엔드 주제
        "JPA/ORM", "Spring Security", "Spring Batch", "Redis 활용",
        "메시지 큐 (Kafka/RabbitMQ)", "gRPC", "WebFlux/리액티브",
        "TDD/테스트", "클린 아키텍처", "DDD/도메인 주도 설계",
        "OAuth2/인증", "멀티모듈 프로젝트", "NestJS",
    ],
    "인프라/DevOps": [
        "Docker", "Kubernetes", "CI/CD", "AWS", "GCP", "Linux", "모니터링",
        # 세부 인프라 주제
        "Docker Compose", "Helm/ArgoCD", "Terraform/IaC",
        "GitHub Actions", "Jenkins", "Nginx/로드밸런서",
        "ELK 스택", "Prometheus/Grafana", "서버리스 (Lambda)",
        "네트워크 보안", "Blue-Green 배포", "컨테이너 보안",
    ],
    "AI/ML": [
        "머신러닝 기초", "딥러닝", "NLP", "컴퓨터 비전", "MLOps", "논문 리뷰",
        # 세부 AI 주제
        "PyTorch 실습", "TensorFlow", "Transformer 아키텍처",
        "LLM/대규모 언어모델", "RAG 시스템", "LoRA/경량 학습",
        "데이터 분석 (Pandas)", "추천 시스템", "강화학습",
        "Kaggle 대회", "프롬프트 엔지니어링", "모델 서빙/최적화",
        "생성형 AI 활용", "벡터 DB/임베딩",
    ],
    "모바일": [
        "Android (Kotlin)", "Android (Java)", "iOS (Swift)", "Flutter", "React Native",
        # 세부 모바일 주제
        "Jetpack Compose", "SwiftUI", "MVVM 아키텍처",
        "Coroutines/비동기", "Combine/리액티브", "Flutter 상태관리",
        "Firebase 연동", "모바일 테스트", "앱 배포 CI/CD",
        "모바일 성능 최적화", "Room/Core Data 로컬 저장소",
    ],
    "자격증": [
        "정보처리기사", "SQLD/SQLP", "리눅스마스터", "네트워크관리사", "AWS 자격증", "Azure 자격증", "CKAD/CKA",
        # 세부 자격증 주제
        "정보처리기사 실기", "정보처리기사 필기",
        "AWS SAA", "AWS SAP", "AWS Developer",
        "빅데이터분석기사", "정보보안기사", "컴퓨터활용능력",
    ],
    "취업 준비": [
        "기술 면접", "코딩테스트 대비", "포트폴리오", "이력서/자소서", "모의 면접",
        # 세부 취업 주제
        "시스템 디자인 면접", "행동 면접 (STAR)", "라이브 코딩",
        "프론트엔드 면접", "백엔드 면접", "CS 면접 총정리",
        "연봉 협상", "기술 블로그 운영",
    ],
    "프로젝트": [
        "사이드 프로젝트", "클론 코딩", "오픈소스 기여", "해커톤 준비",
        # 세부 프로젝트 주제
        "풀스택 프로젝트", "MSA 기반 프로젝트", "실시간 채팅 앱",
        "이커머스 플랫폼", "블로그/CMS 플랫폼", "REST API 서버",
        "관리자 대시보드", "포트폴리오 프로젝트",
    ],
    # ===== 새로운 카테고리 =====
    "보안": [
        "웹 보안 (OWASP)", "네트워크 보안", "암호학",
        "인증/인가 설계", "보안 코딩", "침투 테스트 이해",
        "클라우드 보안", "컨테이너 보안",
    ],
    "데이터 엔지니어링": [
        "ETL 파이프라인", "데이터 웨어하우스", "Spark/Hadoop",
        "Airflow", "데이터 레이크", "실시간 스트리밍 (Kafka)",
        "dbt/데이터 모델링", "데이터 거버넌스",
    ],
    "Git/협업": [
        "Git 기본", "브랜치 전략", "코드 리뷰",
        "Git Flow/GitHub Flow", "모노레포 관리", "커밋 컨벤션",
    ],
}

# 형식 목록 (DataInitializer와 동일)
FORMATS = ["문제 풀이", "독서/책 스터디", "강의 수강", "프로젝트", "모의 면접", "코드 리뷰", "발표/세미나", "토론"]


# =============================================
# 자유 텍스트 주제 풀 (사용자가 입력할 법한 다양한 표현)
# 각 카테고리별 수십 가지 자연어 변형
# =============================================

FREE_TOPIC_INPUTS = {
    # ===== 알고리즘/코딩테스트 =====
    "알고리즘/코딩테스트": [
        "코딩테스트 준비", "알고리즘 스터디", "백준 문제풀이",
        "프로그래머스 코테 준비", "LeetCode 영어 문제풀이",
        "삼성 SW역량테스트 대비", "카카오 코테 준비", "네이버 코딩테스트",
        "DP 집중 학습", "그래프 알고리즘 마스터", "그리디 + DP 정복",
        "코테 실전 연습", "자료구조 기초부터", "알고리즘 이론 정리",
        "SWEA A형 대비", "코딩테스트 문제 매일 1문제",
        "투포인터 슬라이딩윈도우 연습", "BFS DFS 완벽 정리",
        "골드 티어 도전", "플래티넘 도전 스터디",
        "정렬 알고리즘 비교 학습", "이분탐색 응용 문제 풀이",
        "문자열 알고리즘 KMP 트라이", "최단경로 알고리즘 정리",
        "세그먼트 트리 펜윅 트리", "유니온 파인드 활용",
        "백트래킹 완전탐색 연습", "코테 시간복잡도 분석 훈련",
    ],

    # ===== CS 기초 =====
    "CS 기초": [
        "운영체제 공부", "네트워크 기초", "데이터베이스 학습",
        "OS 스터디", "컴퓨터 네트워크 정리", "DB SQL 기초",
        "자료구조 이론", "컴퓨터 구조 학습", "디자인패턴 공부",
        "시스템 설계 입문", "면접을 위한 CS 정리",
        "프로세스 스레드 동기화", "TCP/IP 프로토콜 스택",
        "정규화 인덱스 트랜잭션", "캐시 메모리 가상메모리",
        "CS 전공 기초 총정리", "운영체제 공룡책 스터디",
        "네트워크 하향식 접근 스터디", "데이터베이스 설계 실습",
        "RESTful API 설계 원칙", "마이크로서비스 아키텍처 이해",
        "SOLID 원칙과 클린 아키텍처", "GoF 디자인 패턴 정리",
        "소프트웨어 설계 면접 대비", "분산 시스템 기초",
    ],

    # ===== 프론트엔드 =====
    "프론트엔드": [
        "React 기초 학습", "Next.js 앱라우터 스터디", "TypeScript 입문",
        "프론트엔드 면접 준비", "React Hooks 심화", "상태관리 라이브러리 비교",
        "CSS 레이아웃 마스터", "반응형 웹 디자인", "웹 접근성 학습",
        "Vue.js 3 학습", "JavaScript 딥다이브", "모던 자바스크립트 학습",
        "프론트엔드 성능 최적화", "React 컴포넌트 설계 패턴",
        "웹 애니메이션 구현", "Next.js SSR SSG ISR 이해",
        "Zustand Redux Recoil 비교", "TanStack Query 실전",
        "Tailwind CSS 활용", "Storybook 컴포넌트 문서화",
        "프론트엔드 테스트 Jest Cypress", "웹소켓 실시간 통신",
        "PWA 프로그레시브 웹 앱", "GraphQL 프론트 통합",
    ],

    # ===== 백엔드 =====
    "백엔드": [
        "Spring Boot 학습", "JPA 실전 활용", "Spring Security 인증",
        "Java 심화 학습", "Node.js Express 서버 개발",
        "Python Django 웹개발", "FastAPI 백엔드",
        "Go 언어 입문", "Kotlin 서버 개발",
        "REST API 설계와 구현", "백엔드 아키텍처 설계",
        "Spring Boot 3 마이그레이션", "JPA N+1 문제 해결",
        "Spring Batch 대용량 처리", "Redis 캐싱 전략",
        "메시지 큐 Kafka 학습", "gRPC 마이크로서비스",
        "Spring WebFlux 리액티브", "테스트 주도 개발 TDD",
        "클린 아키텍처 헥사고날", "DDD 도메인 주도 설계",
        "OAuth2 소셜 로그인 구현", "멀티모듈 프로젝트 구성",
        "로깅 모니터링 체계 구축", "API 버전 관리 전략",
    ],

    # ===== 인프라/DevOps =====
    "인프라/DevOps": [
        "Docker 기초 학습", "Kubernetes 입문", "CI/CD 파이프라인 구축",
        "AWS 클라우드 학습", "GCP 입문", "Linux 서버 관리",
        "모니터링 시스템 구축", "Terraform IaC",
        "Docker Compose 멀티컨테이너", "K8s 오케스트레이션",
        "Jenkins GitHub Actions 비교", "AWS EC2 S3 RDS 활용",
        "Prometheus Grafana 모니터링", "Nginx 로드밸런서",
        "Blue-Green 무중단 배포", "ArgoCD GitOps",
        "ELK 스택 로그 분석", "AWS Lambda 서버리스",
        "네트워크 보안 방화벽", "컨테이너 보안 최적화",
        "클라우드 비용 최적화", "멀티 클라우드 전략",
    ],

    # ===== AI/ML =====
    "AI/ML": [
        "머신러닝 기초", "딥러닝 입문", "자연어 처리 NLP",
        "컴퓨터 비전 학습", "MLOps 파이프라인",
        "논문 리뷰 스터디", "PyTorch 실습",
        "데이터 분석 Pandas", "추천 시스템 구현",
        "Transformer 아키텍처 이해", "LLM 파인튜닝",
        "생성형 AI 활용", "RAG 시스템 구축",
        "강화학습 기초", "시계열 데이터 분석",
        "Kaggle 대회 참여", "OpenCV 이미지 처리",
        "Hugging Face 모델 활용", "벡터 DB 임베딩 검색",
        "AI 윤리와 편향성", "프롬프트 엔지니어링",
        "LoRA QLoRA 경량 학습", "모델 서빙 최적화",
    ],

    # ===== 모바일 =====
    "모바일": [
        "Android Kotlin 개발", "iOS Swift 개발",
        "Flutter 크로스플랫폼", "React Native 앱개발",
        "Jetpack Compose UI", "SwiftUI 학습",
        "모바일 앱 아키텍처 MVVM", "Android Coroutines",
        "iOS Combine 리액티브", "Flutter 상태관리 Riverpod",
        "Firebase 백엔드 연동", "모바일 테스트 자동화",
        "앱 배포 CI/CD", "푸시 알림 구현",
        "모바일 성능 최적화", "크로스플랫폼 비교 분석",
        "Android Room DB 로컬 저장소", "iOS Core Data",
    ],

    # ===== 자격증 =====
    "자격증": [
        "정보처리기사 실기", "정보처리기사 필기",
        "SQLD 자격증 준비", "SQLP 고급 자격증",
        "리눅스마스터 2급", "리눅스마스터 1급",
        "네트워크관리사 준비", "AWS SAA 자격증",
        "AWS SAP 자격증", "Azure 자격증 AZ-900",
        "CKA 쿠버네티스 자격증", "CKAD 자격증",
        "정보보안기사 준비", "빅데이터분석기사",
    ],

    # ===== 취업 준비 =====
    "취업 준비": [
        "기술 면접 준비", "코딩테스트 + 면접 종합",
        "포트폴리오 정리", "이력서 자소서 첨삭",
        "모의 면접 연습", "시스템 디자인 면접",
        "행동 면접 STAR 기법", "연봉 협상 준비",
        "신입 개발자 취업 준비", "경력 이직 면접",
        "프론트엔드 면접 빈출 질문", "백엔드 면접 빈출 질문",
        "CS 면접 총정리", "라이브 코딩 테스트 대비",
    ],

    # ===== 프로젝트 =====
    "프로젝트": [
        "사이드 프로젝트 기획", "클론 코딩 Netflix",
        "오픈소스 기여 시작", "해커톤 준비",
        "풀스택 토이 프로젝트", "클론 코딩 당근마켓",
        "포트폴리오 프로젝트", "팀 프로젝트 협업",
        "MSA 기반 프로젝트", "실시간 채팅 앱 개발",
        "이커머스 플랫폼 구축", "블로그 플랫폼 개발",
        "관리자 대시보드 개발", "REST API 서버 구축",
        "Todo 앱 풀스택", "클론 코딩 인스타그램",
        "실시간 주식 트래커", "날씨 대시보드 앱",
    ],

    # ===== 보안 =====
    "보안": [
        "웹 보안 OWASP Top 10", "XSS CSRF 방어 학습",
        "SQL Injection 방어", "보안 코딩 가이드",
        "네트워크 보안 기초", "암호학 기초 대칭키 비대칭키",
        "OAuth JWT 인증 설계", "SSL TLS HTTPS 이해",
        "침투 테스트 기초", "클라우드 보안 AWS",
        "컨테이너 보안 Docker", "보안 헤더 CSP 설정",
        "API 보안 인증 인가", "보안 취약점 분석",
    ],

    # ===== 데이터 엔지니어링 =====
    "데이터 엔지니어링": [
        "ETL 파이프라인 구축", "데이터 웨어하우스 설계",
        "Apache Spark 학습", "Hadoop 에코시스템",
        "Airflow DAG 작성", "실시간 스트리밍 Kafka",
        "데이터 레이크 아키텍처", "dbt 데이터 모델링",
        "데이터 거버넌스 정책", "BigQuery 활용",
        "데이터 품질 관리", "Snowflake 학습",
        "CDC 변경 데이터 캡처", "배치 vs 스트리밍 처리",
    ],

    # ===== Git/협업 =====
    "Git/협업": [
        "Git 기본 명령어 학습", "브랜치 전략 Git Flow",
        "코드 리뷰 문화 만들기", "GitHub Flow 학습",
        "모노레포 관리 전략", "커밋 컨벤션 정리",
        "Git 충돌 해결 연습", "PR 리뷰 실전",
        "Git 고급 rebase cherry-pick", "팀 협업 워크플로우",
        "오픈소스 PR 작성법", "Git Hooks 활용",
    ],
}


# =============================================
# 기술스택 풀 (역할 × 수준별)
# 퀴즈 스크립트처럼 상세하게 분류
# =============================================

TECH_STACKS = {
    # ===== 백엔드 =====
    "backend_junior": {
        "tech": ["Java", "Spring Boot", "MySQL", "Git"],
        "related_topics": ["백엔드", "CS 기초", "알고리즘/코딩테스트"],
    },
    "backend_mid": {
        "tech": ["Java", "Spring Boot", "JPA", "MySQL", "Redis", "Docker", "Git"],
        "related_topics": ["백엔드", "인프라/DevOps", "CS 기초"],
    },
    "backend_senior": {
        "tech": ["Java", "Spring Boot", "JPA", "Kafka", "Redis", "Kubernetes", "AWS", "Docker", "Git"],
        "related_topics": ["백엔드", "인프라/DevOps", "프로젝트"],
    },
    "backend_java_mid": {
        "tech": ["Java", "Spring Boot", "Spring Security", "JPA", "QueryDSL", "MySQL", "Redis"],
        "related_topics": ["백엔드", "CS 기초"],
    },
    "backend_python_junior": {
        "tech": ["Python", "Django", "PostgreSQL", "Git"],
        "related_topics": ["백엔드", "CS 기초", "알고리즘/코딩테스트"],
    },
    "backend_python_mid": {
        "tech": ["Python", "FastAPI", "SQLAlchemy", "PostgreSQL", "Redis", "Docker"],
        "related_topics": ["백엔드", "인프라/DevOps"],
    },
    "backend_node_junior": {
        "tech": ["JavaScript", "Node.js", "Express", "MongoDB", "Git"],
        "related_topics": ["백엔드", "프론트엔드"],
    },
    "backend_node_mid": {
        "tech": ["TypeScript", "NestJS", "TypeORM", "PostgreSQL", "Redis", "Docker"],
        "related_topics": ["백엔드", "인프라/DevOps"],
    },
    "backend_go_mid": {
        "tech": ["Go", "Gin", "PostgreSQL", "gRPC", "Docker"],
        "related_topics": ["백엔드", "인프라/DevOps"],
    },
    "backend_kotlin_mid": {
        "tech": ["Kotlin", "Spring Boot", "JPA", "MySQL", "Docker"],
        "related_topics": ["백엔드", "모바일"],
    },

    # ===== 프론트엔드 =====
    "frontend_junior": {
        "tech": ["JavaScript", "React", "HTML", "CSS", "Git"],
        "related_topics": ["프론트엔드", "CS 기초"],
    },
    "frontend_mid": {
        "tech": ["TypeScript", "React", "Next.js", "TailwindCSS", "Zustand", "Git"],
        "related_topics": ["프론트엔드", "백엔드"],
    },
    "frontend_senior": {
        "tech": ["TypeScript", "React", "Next.js", "Redux", "GraphQL", "Storybook", "Jest", "Cypress"],
        "related_topics": ["프론트엔드", "프로젝트"],
    },
    "frontend_vue_mid": {
        "tech": ["TypeScript", "Vue 3", "Nuxt.js", "Pinia", "TailwindCSS"],
        "related_topics": ["프론트엔드"],
    },
    "frontend_ts_focus": {
        "tech": ["TypeScript", "React", "Next.js", "TanStack Query", "Zod"],
        "related_topics": ["프론트엔드", "백엔드"],
    },

    # ===== 풀스택 =====
    "fullstack_junior": {
        "tech": ["JavaScript", "React", "Node.js", "Express", "MongoDB", "Git"],
        "related_topics": ["프론트엔드", "백엔드", "프로젝트"],
    },
    "fullstack_mid": {
        "tech": ["TypeScript", "React", "Next.js", "Node.js", "PostgreSQL", "Docker", "AWS"],
        "related_topics": ["프론트엔드", "백엔드", "인프라/DevOps"],
    },
    "fullstack_senior": {
        "tech": ["TypeScript", "React", "Next.js", "NestJS", "PostgreSQL", "Redis", "Docker", "Kubernetes", "AWS"],
        "related_topics": ["프론트엔드", "백엔드", "인프라/DevOps", "프로젝트"],
    },

    # ===== 모바일 =====
    "mobile_android_junior": {
        "tech": ["Kotlin", "Android", "XML", "Git"],
        "related_topics": ["모바일", "CS 기초"],
    },
    "mobile_android_mid": {
        "tech": ["Kotlin", "Jetpack Compose", "Room", "Hilt", "Coroutines", "Retrofit"],
        "related_topics": ["모바일", "백엔드"],
    },
    "mobile_ios_junior": {
        "tech": ["Swift", "UIKit", "Xcode"],
        "related_topics": ["모바일", "CS 기초"],
    },
    "mobile_ios_mid": {
        "tech": ["Swift", "SwiftUI", "Combine", "Core Data", "Firebase"],
        "related_topics": ["모바일"],
    },
    "mobile_flutter_mid": {
        "tech": ["Dart", "Flutter", "Firebase", "Riverpod"],
        "related_topics": ["모바일", "프론트엔드"],
    },
    "mobile_rn_mid": {
        "tech": ["TypeScript", "React Native", "Expo", "Redux"],
        "related_topics": ["모바일", "프론트엔드"],
    },

    # ===== DevOps =====
    "devops_junior": {
        "tech": ["Linux", "Docker", "Git", "Bash"],
        "related_topics": ["인프라/DevOps", "CS 기초"],
    },
    "devops_mid": {
        "tech": ["Docker", "Kubernetes", "Jenkins", "AWS", "Terraform", "Ansible"],
        "related_topics": ["인프라/DevOps", "백엔드"],
    },
    "devops_senior": {
        "tech": ["Kubernetes", "AWS", "Terraform", "ArgoCD", "Prometheus", "Grafana", "ELK"],
        "related_topics": ["인프라/DevOps", "프로젝트"],
    },

    # ===== AI/데이터 =====
    "ai_junior": {
        "tech": ["Python", "Pandas", "NumPy", "Matplotlib"],
        "related_topics": ["AI/ML", "CS 기초"],
    },
    "ai_mid": {
        "tech": ["Python", "PyTorch", "Scikit-learn", "Pandas", "Jupyter", "SQL"],
        "related_topics": ["AI/ML", "CS 기초"],
    },
    "ai_senior": {
        "tech": ["Python", "PyTorch", "TensorFlow", "Hugging Face", "MLflow", "Airflow", "Docker"],
        "related_topics": ["AI/ML", "인프라/DevOps"],
    },
    "ai_nlp_mid": {
        "tech": ["Python", "PyTorch", "Transformers", "LangChain", "FAISS"],
        "related_topics": ["AI/ML"],
    },
    "ai_cv_mid": {
        "tech": ["Python", "PyTorch", "OpenCV", "YOLO", "Albumentations"],
        "related_topics": ["AI/ML"],
    },

    # ===== 입문 =====
    "beginner_zero": {
        "tech": ["Python"],
        "related_topics": ["CS 기초", "알고리즘/코딩테스트"],
    },
    "beginner_basic": {
        "tech": ["Python", "Git", "HTML", "CSS"],
        "related_topics": ["프론트엔드", "CS 기초"],
    },
    "beginner_cs": {
        "tech": ["C", "Python", "Git"],
        "related_topics": ["CS 기초", "알고리즘/코딩테스트"],
    },

    # ===== 자격증 =====
    "cert_eip": {
        "tech": ["Java", "Python", "SQL", "Git"],
        "related_topics": ["자격증", "CS 기초"],
    },
    "cert_sqld": {
        "tech": ["SQL", "MySQL", "Oracle"],
        "related_topics": ["자격증", "CS 기초"],
    },
    "cert_aws": {
        "tech": ["AWS", "Docker", "Linux", "Terraform"],
        "related_topics": ["자격증", "인프라/DevOps"],
    },
    "cert_cka": {
        "tech": ["Kubernetes", "Docker", "Linux", "Helm"],
        "related_topics": ["자격증", "인프라/DevOps"],
    },

    # ===== 보안 =====
    "security_junior": {
        "tech": ["Python", "Linux", "Git", "Burp Suite"],
        "related_topics": ["보안", "CS 기초"],
    },
    "security_mid": {
        "tech": ["Python", "Linux", "Docker", "Wireshark", "Nmap", "AWS"],
        "related_topics": ["보안", "인프라/DevOps"],
    },
    "security_web": {
        "tech": ["JavaScript", "Python", "Node.js", "OWASP ZAP"],
        "related_topics": ["보안", "백엔드", "프론트엔드"],
    },

    # ===== 데이터 엔지니어링 =====
    "data_eng_junior": {
        "tech": ["Python", "SQL", "Pandas", "Git"],
        "related_topics": ["데이터 엔지니어링", "CS 기초"],
    },
    "data_eng_mid": {
        "tech": ["Python", "Spark", "Airflow", "SQL", "Docker", "AWS"],
        "related_topics": ["데이터 엔지니어링", "인프라/DevOps"],
    },
    "data_eng_senior": {
        "tech": ["Python", "Spark", "Kafka", "Airflow", "dbt", "Snowflake", "Docker", "Kubernetes"],
        "related_topics": ["데이터 엔지니어링", "인프라/DevOps", "AI/ML"],
    },
    "data_eng_cloud": {
        "tech": ["Python", "BigQuery", "Dataflow", "GCP", "Airflow", "SQL"],
        "related_topics": ["데이터 엔지니어링", "인프라/DevOps"],
    },

    # ===== 취업 준비 특화 =====
    "job_frontend": {
        "tech": ["TypeScript", "React", "Next.js", "HTML", "CSS", "Git"],
        "related_topics": ["취업 준비", "프론트엔드", "CS 기초"],
    },
    "job_backend": {
        "tech": ["Java", "Spring Boot", "JPA", "MySQL", "Git"],
        "related_topics": ["취업 준비", "백엔드", "CS 기초"],
    },
    "job_newbie": {
        "tech": ["Python", "Git"],
        "related_topics": ["취업 준비", "CS 기초", "알고리즘/코딩테스트"],
    },

    # ===== 프로젝트 특화 =====
    "project_web_fullstack": {
        "tech": ["TypeScript", "React", "Next.js", "Node.js", "PostgreSQL", "Docker"],
        "related_topics": ["프로젝트", "프론트엔드", "백엔드"],
    },
    "project_msa": {
        "tech": ["Java", "Spring Boot", "Docker", "Kubernetes", "Kafka", "Redis"],
        "related_topics": ["프로젝트", "백엔드", "인프라/DevOps"],
    },

    # ===== Git/협업 =====
    "collab_junior": {
        "tech": ["Git", "GitHub", "VS Code"],
        "related_topics": ["Git/협업", "프론트엔드"],
    },
    "collab_mid": {
        "tech": ["Git", "GitHub", "Docker", "Jira", "Notion"],
        "related_topics": ["Git/협업", "프로젝트"],
    },
}


# =============================================
# 기술스택 → 관련 토픽 매핑 (난이도 자동 설정용)
# 사용자가 이미 해당 기술을 보유하면 난이도를 높게 설정
# =============================================

TECH_TOPIC_OVERLAP = {
    "Docker": ["Docker", "Docker Compose", "컨테이너 보안"],
    "Kubernetes": ["Kubernetes", "Helm/ArgoCD", "CKAD/CKA"],
    "React": ["React", "React Hooks 심화", "상태관리 라이브러리"],
    "Next.js": ["Next.js"],
    "TypeScript": ["TypeScript"],
    "Vue": ["Vue", "Vue 3"],
    "Java": ["Java/Spring", "JPA/ORM"],
    "Spring Boot": ["Java/Spring", "Spring Security", "Spring Batch"],
    "Spring Security": ["Spring Security", "OAuth2/인증"],
    "JPA": ["JPA/ORM"],
    "Python": ["Python/Django", "Python/FastAPI"],
    "Django": ["Python/Django"],
    "FastAPI": ["Python/FastAPI"],
    "Node.js": ["Node.js/Express", "NestJS"],
    "Express": ["Node.js/Express"],
    "Go": ["Go"],
    "Kotlin": ["Kotlin", "Android (Kotlin)"],
    "AWS": ["AWS", "AWS 자격증", "AWS SAA", "서버리스 (Lambda)"],
    "GCP": ["GCP", "BigQuery 활용"],
    "Redis": ["Redis 활용"],
    "Kafka": ["메시지 큐 (Kafka/RabbitMQ)", "실시간 스트리밍 (Kafka)"],
    "Linux": ["Linux", "리눅스마스터"],
    "Jenkins": ["CI/CD", "Jenkins"],
    "Terraform": ["Terraform/IaC"],
    "Prometheus": ["Prometheus/Grafana", "모니터링"],
    "Grafana": ["Prometheus/Grafana", "모니터링"],
    "PyTorch": ["딥러닝", "PyTorch 실습"],
    "TensorFlow": ["딥러닝", "TensorFlow"],
    "Pandas": ["데이터 분석 (Pandas)"],
    "Spark": ["Apache Spark 학습"],
    "Airflow": ["Airflow", "Airflow DAG 작성"],
    "Swift": ["iOS (Swift)", "SwiftUI"],
    "Flutter": ["Flutter", "Flutter 상태관리"],
    "React Native": ["React Native"],
    "SQL": ["SQL 활용", "SQLD/SQLP"],
    "MySQL": ["데이터베이스", "정규화/인덱스"],
    "PostgreSQL": ["데이터베이스"],
    "MongoDB": ["데이터베이스"],
    "Git": ["Git 기본", "브랜치 전략"],
}


# =============================================
# 일정 패턴 (다양한 직장인/학생 패턴)
# =============================================

SCHEDULE_PATTERNS = [
    # 평일 저녁 패턴 (직장인)
    {"desc": "월수 저녁", "days": ["월", "수"], "slots": {"월": "19:00-22:00", "수": "19:00-22:00"}},
    {"desc": "화목 저녁", "days": ["화", "목"], "slots": {"화": "20:00-22:00", "목": "20:00-22:00"}},
    {"desc": "월수금 저녁", "days": ["월", "수", "금"], "slots": {"월": "19:00-21:00", "수": "19:00-21:00", "금": "19:00-21:00"}},
    {"desc": "월 저녁", "days": ["월"], "slots": {"월": "21:00-23:00"}},
    {"desc": "수금 저녁", "days": ["수", "금"], "slots": {"수": "18:00-20:00", "금": "18:00-20:00"}},
    {"desc": "화목 늦은저녁", "days": ["화", "목"], "slots": {"화": "21:00-23:00", "목": "21:00-23:00"}},

    # 아침 패턴 (미라클모닝)
    {"desc": "월수 아침", "days": ["월", "수"], "slots": {"월": "07:00-09:00", "수": "07:00-09:00"}},
    {"desc": "화목토 아침", "days": ["화", "목", "토"], "slots": {"화": "06:30-08:00", "목": "06:30-08:00", "토": "08:00-10:00"}},

    # 주말 패턴 (학생)
    {"desc": "토요일 오전", "days": ["토"], "slots": {"토": "10:00-13:00"}},
    {"desc": "토요일 오후", "days": ["토"], "slots": {"토": "14:00-18:00"}},
    {"desc": "일요일 오후", "days": ["일"], "slots": {"일": "14:00-18:00"}},
    {"desc": "토일 오전", "days": ["토", "일"], "slots": {"토": "10:00-12:00", "일": "10:00-12:00"}},

    # 하이브리드 패턴
    {"desc": "화 저녁 + 토 오전", "days": ["화", "토"], "slots": {"화": "19:00-21:00", "토": "10:00-12:00"}},
    {"desc": "목 저녁 + 일 오후", "days": ["목", "일"], "slots": {"목": "20:00-22:00", "일": "14:00-16:00"}},
    {"desc": "화목 저녁 + 토 오전", "days": ["화", "목", "토"], "slots": {"화": "19:00-22:00", "목": "19:00-22:00", "토": "10:00-13:00"}},
    {"desc": "월수금 점심", "days": ["월", "수", "금"], "slots": {"월": "12:00-13:00", "수": "12:00-13:00", "금": "12:00-13:00"}},

    # 미지정
    {"desc": "미지정", "days": [], "slots": {}},
]


# =============================================
# 주제별 상세 정보 풀 (교재, 목표, 선행조건 등)
# 퀴즈 스크립트처럼 수백 개 상세 항목
# =============================================

TOPIC_DETAILS = {
    # ===== 알고리즘/코딩테스트 =====
    "백준": {
        "textbooks": ["백준 단계별 문제", "백준 골드 필수 문제 100선", "백준 그래프 이론 문제집"],
        "goals": ["백준 골드 티어 달성", "백준 실버→골드 승급", "매일 1문제 100일 연속 풀기"],
        "prerequisites": ["기본 자료구조 이해 (배열, 스택, 큐)", "Python 또는 Java 기초 문법", "없음"],
    },
    "프로그래머스": {
        "textbooks": ["프로그래머스 코딩테스트 연습 레벨2~3", "프로그래머스 SQL 고득점 Kit", "카카오 기출문제 모음"],
        "goals": ["프로그래머스 레벨3 안정적 풀기", "카카오 기출 문제 완전 정복", "코딩테스트 합격률 80% 이상"],
        "prerequisites": ["기본 알고리즘 개념", "Python/Java/JavaScript 중 하나 숙지", "없음"],
    },
    "SWEA": {
        "textbooks": ["SWEA D4~D5 문제", "삼성 SW역량테스트 기출", "삼성 A형 대비 문제집"],
        "goals": ["삼성 SW역량테스트 A형 합격", "SWEA D5 문제 풀이 능력", "BFS/DFS/시뮬레이션 마스터"],
        "prerequisites": ["C++ 또는 Java 기본 문법", "기본 자료구조 이해", "없음"],
    },
    "LeetCode": {
        "textbooks": ["LeetCode Top 100 Liked", "Blind 75", "NeetCode 150", "LeetCode SQL 50"],
        "goals": ["LeetCode Medium 안정적 풀이", "FAANG 면접 코딩테스트 대비", "영문 문제 해석 능력 향상"],
        "prerequisites": ["영어 문제 독해 가능", "기본 자료구조와 알고리즘 지식", "없음"],
    },
    "코딩테스트 대비": {
        "textbooks": ["이것이 코딩테스트다", "Do it! 알고리즘", "알고리즘 문제해결 전략", "코딩 인터뷰 완전 분석"],
        "goals": ["주요 IT기업 코딩테스트 합격", "알고리즘 유형별 완전 정복", "시간 내 문제 해결 능력 향상"],
        "prerequisites": ["프로그래밍 언어 1개 이상 숙지", "없음"],
    },

    # ===== CS 기초 =====
    "자료구조": {
        "textbooks": ["윤성우의 열혈 자료구조", "자료구조와 함께 배우는 알고리즘 입문", "Introduction to Algorithms (CLRS)"],
        "goals": ["핵심 자료구조 구현 능력 확보", "면접에서 자료구조 질문 완벽 대응", "각 자료구조의 시간복잡도 체화"],
        "prerequisites": ["C/C++ 또는 Java 기초", "없음"],
    },
    "알고리즘 이론": {
        "textbooks": ["알고리즘 문제해결 전략", "Introduction to Algorithms", "알고리즘 개론"],
        "goals": ["알고리즘 이론적 기반 확립", "복잡도 분석 능력 향상", "증명 기반 이해"],
        "prerequisites": ["자료구조 기본 이해", "없음"],
    },
    "운영체제": {
        "textbooks": ["운영체제 공룡책", "혼자 공부하는 컴퓨터 구조+운영체제", "OSTEP (Three Easy Pieces)"],
        "goals": ["프로세스/스레드/동기화 완벽 이해", "면접 OS 질문 대비", "커널 동작 원리 파악"],
        "prerequisites": ["C언어 기초", "컴퓨터 구조 기본", "없음"],
    },
    "네트워크": {
        "textbooks": ["컴퓨터 네트워킹: 하향식 접근", "그림으로 배우는 네트워크", "HTTP 완벽 가이드"],
        "goals": ["TCP/IP 4계층 완벽 이해", "HTTP/HTTPS 동작 원리 설명 가능", "네트워크 면접 질문 대비"],
        "prerequisites": ["없음"],
    },
    "데이터베이스": {
        "textbooks": ["데이터베이스 개론", "Real MySQL 8.0", "SQL 첫걸음", "SQL 레벨업"],
        "goals": ["정규화/인덱스/트랜잭션 이해", "복잡한 SQL 쿼리 작성 능력", "DB 설계 능력 확보"],
        "prerequisites": ["SQL 기본 문법", "없음"],
    },
    "컴퓨터구조": {
        "textbooks": ["혼자 공부하는 컴퓨터 구조+운영체제", "컴퓨터 구조 및 설계 (패터슨)", "CSAPP"],
        "goals": ["CPU/메모리/캐시 동작 이해", "면접 컴퓨터 구조 질문 대비", "성능 최적화 기반 이해"],
        "prerequisites": ["논리 회로 기본", "없음"],
    },
    "디자인패턴": {
        "textbooks": ["Head First 디자인 패턴", "GoF 디자인 패턴", "리팩터링"],
        "goals": ["GoF 23가지 패턴 이해 및 적용", "코드 품질 향상", "면접 설계 질문 대비"],
        "prerequisites": ["OOP 기본 이해", "Java 또는 Python 기초", "없음"],
    },
    "시스템 설계": {
        "textbooks": ["가상 면접 사례로 배우는 대규모 시스템 설계 기초", "시스템 디자인 인터뷰", "Designing Data-Intensive Applications"],
        "goals": ["시스템 디자인 면접 대비", "확장 가능한 아키텍처 설계 능력", "트레이드오프 분석 능력"],
        "prerequisites": ["백엔드 개발 경험", "DB/네트워크 기본", "없음"],
    },

    # ===== 프론트엔드 =====
    "HTML/CSS": {
        "textbooks": ["MDN Web Docs", "CSS in Depth", "모던 웹을 위한 HTML5+CSS3 바이블"],
        "goals": ["시맨틱 마크업 작성 능력", "Flexbox/Grid 레이아웃 마스터", "반응형 웹 구현"],
        "prerequisites": ["없음"],
    },
    "JavaScript": {
        "textbooks": ["모던 JavaScript 튜토리얼", "JavaScript Deep Dive", "You Don't Know JS"],
        "goals": ["JS 핵심 개념 완벽 이해", "클로저/프로토타입/비동기 설명 가능", "면접 JS 질문 대비"],
        "prerequisites": ["HTML/CSS 기초", "없음"],
    },
    "TypeScript": {
        "textbooks": ["TypeScript 핸드북", "이펙티브 타입스크립트", "타입스크립트 프로그래밍"],
        "goals": ["타입 시스템 활용 능력", "제네릭/유틸리티 타입 자유자재 사용", "타입 안전한 코드 작성"],
        "prerequisites": ["JavaScript 기본", "없음"],
    },
    "React": {
        "textbooks": ["React 공식 문서", "리액트를 다루는 기술", "모던 리액트 Deep Dive"],
        "goals": ["React 컴포넌트 설계 패턴 습득", "Hooks 완벽 이해", "성능 최적화 기법 적용"],
        "prerequisites": ["JavaScript 기초", "HTML/CSS 기본", "없음"],
    },
    "Vue": {
        "textbooks": ["Vue.js 3 공식 가이드", "Vue.js 프로젝트 투입 일주일 전", "Do it! Vue.js"],
        "goals": ["Vue 3 Composition API 숙달", "Pinia 상태관리 활용", "Vue 생태계 이해"],
        "prerequisites": ["JavaScript 기초", "HTML/CSS 기본", "없음"],
    },
    "Next.js": {
        "textbooks": ["Next.js 공식 문서", "Real World Next.js", "Next.js 실전 프로젝트"],
        "goals": ["App Router 완벽 이해", "SSR/SSG/ISR 적절한 사용", "Next.js 배포 최적화"],
        "prerequisites": ["React 기본", "TypeScript 기초", "없음"],
    },
    "웹 접근성/성능": {
        "textbooks": ["웹 접근성 가이드 WCAG 2.1", "Web Performance in Action", "Core Web Vitals 가이드"],
        "goals": ["WCAG 2.1 AA 준수 웹 개발", "Lighthouse 90점 이상", "Core Web Vitals 최적화"],
        "prerequisites": ["HTML/CSS/JS 기본", "React 또는 Vue 경험", "없음"],
    },

    # ===== 백엔드 =====
    "Java/Spring": {
        "textbooks": ["스프링 부트와 AWS로 혼자 구현하는 웹 서비스", "토비의 스프링", "자바 ORM 표준 JPA 프로그래밍"],
        "goals": ["Spring Boot 기반 REST API 구현", "JPA 활용 데이터 접근 계층 설계", "Spring Security 인증/인가 구현"],
        "prerequisites": ["Java 기초 문법", "SQL 기본", "없음"],
    },
    "Python/Django": {
        "textbooks": ["Django로 배우는 파이썬 웹 프로그래밍", "Two Scoops of Django", "Django REST Framework 공식 문서"],
        "goals": ["Django 기반 웹 서비스 구현", "DRF REST API 설계", "Django ORM 활용"],
        "prerequisites": ["Python 기초", "SQL 기본", "없음"],
    },
    "Python/FastAPI": {
        "textbooks": ["FastAPI 공식 문서", "Building Python Web APIs with FastAPI", "FastAPI + SQLAlchemy 실전"],
        "goals": ["FastAPI 비동기 API 서버 구축", "Pydantic 데이터 검증 활용", "OpenAPI 자동 문서화"],
        "prerequisites": ["Python 기초", "비동기 프로그래밍 이해", "없음"],
    },
    "Node.js/Express": {
        "textbooks": ["Node.js 교과서", "Express 공식 가이드", "Node.js 디자인 패턴"],
        "goals": ["Express 기반 REST API 구현", "미들웨어 패턴 이해", "Node.js 비동기 처리 숙달"],
        "prerequisites": ["JavaScript 기초", "없음"],
    },
    "Go": {
        "textbooks": ["Go 언어 공식 투어", "Tucker의 Go 프로그래밍", "Go Web Programming"],
        "goals": ["Go 언어 기본 문법 숙달", "goroutine/channel 동시성 이해", "Go 기반 웹 서버 구현"],
        "prerequisites": ["프로그래밍 언어 1개 이상 숙지", "없음"],
    },
    "Kotlin": {
        "textbooks": ["Kotlin in Action", "코틀린 프로그래밍", "Kotlin 공식 문서"],
        "goals": ["Kotlin 언어 특성 숙달", "코루틴 비동기 처리", "Spring Boot + Kotlin 활용"],
        "prerequisites": ["Java 또는 다른 OOP 언어 경험", "없음"],
    },
    "API 설계": {
        "textbooks": ["RESTful Web APIs", "API 디자인 패턴", "마이크로서비스 패턴"],
        "goals": ["RESTful API 설계 원칙 습득", "API 버전 관리와 문서화", "GraphQL/gRPC 이해"],
        "prerequisites": ["웹 개발 기본 경험", "HTTP 프로토콜 이해", "없음"],
    },

    # ===== 인프라/DevOps =====
    "Docker": {
        "textbooks": ["Docker 공식 문서", "시작하세요! 도커/쿠버네티스", "Docker Deep Dive"],
        "goals": ["Dockerfile 작성 능력", "Docker Compose 멀티컨테이너 운영", "컨테이너 이미지 최적화"],
        "prerequisites": ["Linux 기본 명령어", "없음"],
    },
    "Kubernetes": {
        "textbooks": ["쿠버네티스 인 액션", "쿠버네티스 완벽 가이드", "Kubernetes 공식 문서"],
        "goals": ["K8s 핵심 리소스 이해", "Helm 차트 작성", "클러스터 운영 능력"],
        "prerequisites": ["Docker 기본", "Linux 명령어", "없음"],
    },
    "CI/CD": {
        "textbooks": ["GitHub Actions 공식 문서", "Jenkins 실전 가이드", "GitLab CI/CD 실습"],
        "goals": ["CI/CD 파이프라인 자동화", "Blue-Green/카나리 배포 이해", "자동 테스트 통합"],
        "prerequisites": ["Git 기본", "Docker 기초", "없음"],
    },
    "AWS": {
        "textbooks": ["AWS 공인 솔루션스 아키텍트 스터디 가이드", "아마존 웹 서비스 인 액션", "AWS Well-Architected Framework"],
        "goals": ["EC2/S3/RDS 핵심 서비스 활용", "VPC 네트워크 설계", "AWS 자격증 취득"],
        "prerequisites": ["Linux 기본", "네트워크 기초", "없음"],
    },
    "GCP": {
        "textbooks": ["GCP 공식 문서", "Google Cloud 실전 가이드", "GCP Associate 시험 가이드"],
        "goals": ["GCP 핵심 서비스 이해", "BigQuery 데이터 분석", "GKE 컨테이너 운영"],
        "prerequisites": ["클라우드 기본 개념", "없음"],
    },
    "Linux": {
        "textbooks": ["리눅스 커맨드라인 완벽 입문서", "이것이 리눅스다", "리눅스 바이블"],
        "goals": ["Linux 서버 관리 능력", "Shell 스크립트 작성", "시스템 모니터링 및 장애 대응"],
        "prerequisites": ["없음"],
    },
    "모니터링": {
        "textbooks": ["Prometheus Up & Running", "Grafana 실전 가이드", "ELK 스택 완벽 가이드"],
        "goals": ["모니터링 시스템 구축", "알림 체계 설계", "로그 수집/분석 파이프라인"],
        "prerequisites": ["Docker 기본", "Linux 명령어", "없음"],
    },

    # ===== AI/ML =====
    "머신러닝 기초": {
        "textbooks": ["핸즈온 머신러닝", "밑바닥부터 시작하는 딥러닝", "파이썬 머신러닝 완벽 가이드"],
        "goals": ["지도/비지도학습 핵심 알고리즘 이해", "Scikit-learn 실전 활용", "데이터 전처리 및 특성 공학"],
        "prerequisites": ["Python 기초", "고등 수학 (선형대수, 확률/통계)", "없음"],
    },
    "딥러닝": {
        "textbooks": ["밑바닥부터 시작하는 딥러닝", "PyTorch로 시작하는 딥러닝", "Deep Learning (Goodfellow)"],
        "goals": ["CNN/RNN/Transformer 구현 능력", "PyTorch 실전 활용", "논문 구현 및 재현"],
        "prerequisites": ["머신러닝 기초", "Python 중급", "없음"],
    },
    "NLP": {
        "textbooks": ["딥러닝을 이용한 자연어 처리 입문", "Hugging Face 공식 튜토리얼", "Speech and Language Processing"],
        "goals": ["BERT/GPT 아키텍처 이해", "텍스트 분류/생성/요약 구현", "LLM 파인튜닝 능력"],
        "prerequisites": ["딥러닝 기초", "Python 중급", "없음"],
    },
    "컴퓨터 비전": {
        "textbooks": ["OpenCV 4로 배우는 컴퓨터 비전", "딥러닝 컴퓨터 비전", "CS231n 강의 자료"],
        "goals": ["이미지 분류/탐지/세그멘테이션 구현", "YOLO/ResNet 등 모델 활용", "실시간 영상 처리"],
        "prerequisites": ["딥러닝 기초", "Python 중급", "없음"],
    },
    "MLOps": {
        "textbooks": ["MLOps 실전 가이드", "Designing Machine Learning Systems", "MLflow 공식 문서"],
        "goals": ["ML 파이프라인 자동화", "모델 버전 관리 및 배포", "모니터링 및 재학습 체계"],
        "prerequisites": ["머신러닝 경험", "Docker 기본", "없음"],
    },
    "논문 리뷰": {
        "textbooks": ["arXiv 최신 논문", "Papers with Code", "Stanford CS229/CS231n/CS224n"],
        "goals": ["주 1편 논문 리뷰 및 발표", "논문 구현 능력 확보", "최신 AI 트렌드 파악"],
        "prerequisites": ["딥러닝 기초", "영어 논문 독해 가능", "없음"],
    },

    # ===== 모바일 =====
    "Android (Kotlin)": {
        "textbooks": ["Android Developers 공식 문서", "깡쌤의 안드로이드 프로그래밍", "이것이 안드로이드다"],
        "goals": ["Jetpack Compose UI 구현", "MVVM 아키텍처 적용", "앱 스토어 배포"],
        "prerequisites": ["Kotlin 기초", "없음"],
    },
    "Android (Java)": {
        "textbooks": ["Do it! 안드로이드 앱 프로그래밍", "Android 공식 문서"],
        "goals": ["Android 앱 개발 기초 습득", "Activity 생명주기 이해", "기본 UI 구현"],
        "prerequisites": ["Java 기초", "없음"],
    },
    "iOS (Swift)": {
        "textbooks": ["Swift 공식 문서", "iOS Programming (Big Nerd Ranch)", "SwiftUI 튜토리얼"],
        "goals": ["SwiftUI 기반 앱 개발", "Combine 리액티브 프로그래밍", "앱 스토어 배포"],
        "prerequisites": ["Swift 기초", "없음"],
    },
    "Flutter": {
        "textbooks": ["Flutter 공식 문서", "Flutter in Action", "코드팩토리의 Flutter"],
        "goals": ["Flutter 크로스플랫폼 앱 개발", "상태관리 (Riverpod/Bloc)", "Firebase 연동"],
        "prerequisites": ["Dart 기초", "없음"],
    },
    "React Native": {
        "textbooks": ["React Native 공식 문서", "React Native 실전 프로젝트", "Expo 가이드"],
        "goals": ["React Native 앱 개발", "네이티브 모듈 연동", "앱 배포 자동화"],
        "prerequisites": ["React 경험", "JavaScript/TypeScript 기초", "없음"],
    },

    # ===== 자격증 =====
    "정보처리기사": {
        "textbooks": ["시나공 정보처리기사 실기", "수제비 정보처리기사", "이기적 정보처리기사"],
        "goals": ["정보처리기사 실기 합격", "SW 공학 핵심 개념 정리", "프로그래밍 언어 코드 해석"],
        "prerequisites": ["기본 프로그래밍 지식", "없음"],
    },
    "SQLD/SQLP": {
        "textbooks": ["SQL 자격검정 실전문제", "SQL 전문가 가이드", "SQLD 기출문제 해설"],
        "goals": ["SQLD 자격증 취득", "SQL 실무 능력 향상", "데이터 모델링 이해"],
        "prerequisites": ["SQL 기초 문법", "없음"],
    },
    "리눅스마스터": {
        "textbooks": ["리눅스마스터 2급 기출문제집", "이기적 리눅스마스터", "리눅스 실습 교재"],
        "goals": ["리눅스마스터 2급 합격", "Linux 서버 관리 기본기", "Shell 명령어 숙달"],
        "prerequisites": ["없음"],
    },
    "네트워크관리사": {
        "textbooks": ["네트워크관리사 2급 기출문제", "네트워크 기초 교재"],
        "goals": ["네트워크관리사 자격증 취득", "네트워크 기본 개념 정리"],
        "prerequisites": ["없음"],
    },
    "AWS 자격증": {
        "textbooks": ["AWS SAA 스터디 가이드", "Tutorials Dojo 모의고사", "AWS 공식 교육 자료"],
        "goals": ["AWS SAA 자격증 취득", "AWS 핵심 서비스 50개 이해", "아키텍처 설계 능력"],
        "prerequisites": ["클라우드 기본 개념", "없음"],
    },
    "Azure 자격증": {
        "textbooks": ["AZ-900 스터디 가이드", "Microsoft Learn", "Azure 공식 교육 자료"],
        "goals": ["Azure 자격증 취득", "클라우드 기본 이해"],
        "prerequisites": ["없음"],
    },
    "CKAD/CKA": {
        "textbooks": ["CKA/CKAD 공식 커리큘럼", "Kubernetes the Hard Way", "killer.sh 모의시험"],
        "goals": ["CKA/CKAD 자격증 취득", "K8s 클러스터 운영 능력", "kubectl 숙달"],
        "prerequisites": ["Docker 기본", "Linux 명령어", "없음"],
    },

    # ===== 취업 준비 =====
    "기술 면접": {
        "textbooks": ["면접을 위한 CS 전공지식 노트", "자바 면접 질문 모음", "프론트엔드 면접 가이드", "Tech Interview for Developer"],
        "goals": ["CS 면접 질문 100개 완벽 대비", "프로젝트 경험 설명 능력", "기술적 깊이 있는 답변 준비"],
        "prerequisites": ["CS 기초 학습 경험", "개발 프로젝트 경험", "없음"],
    },
    "코딩테스트 대비": {
        "textbooks": ["이것이 코딩테스트다", "코딩 인터뷰 완전 분석", "프로그래머스 실전 문제"],
        "goals": ["주요 기업 코딩테스트 합격", "알고리즘 유형별 풀이 전략 확립", "시간 제한 내 문제 해결"],
        "prerequisites": ["프로그래밍 언어 1개 숙지", "없음"],
    },
    "포트폴리오": {
        "textbooks": ["GitHub 프로필 가이드", "포트폴리오 작성 사례집", "기술 블로그 운영 가이드"],
        "goals": ["포트폴리오 프로젝트 3개 이상 완성", "GitHub 프로필 최적화", "기술 블로그 10편 이상 작성"],
        "prerequisites": ["개발 프로젝트 경험", "없음"],
    },
    "이력서/자소서": {
        "textbooks": ["개발자 이력서 작성 가이드", "합격하는 자소서 사례집"],
        "goals": ["서류 합격률 향상", "프로젝트 경험 효과적 어필", "기술 역량 정량적 표현"],
        "prerequisites": ["개발 경험", "없음"],
    },
    "모의 면접": {
        "textbooks": ["면접의 정석", "STAR 기법 가이드", "기술 면접 모의 질문집"],
        "goals": ["면접 긴장감 극복", "구조화된 답변 능력", "기술+인성 면접 동시 대비"],
        "prerequisites": ["CS 기초", "프로젝트 경험", "없음"],
    },

    # ===== 프로젝트 =====
    "사이드 프로젝트": {
        "textbooks": ["사이드 프로젝트 기획서", "MVP 개발 가이드", "린 스타트업"],
        "goals": ["아이디어→배포까지 완주", "풀스택 개발 경험", "포트폴리오 프로젝트 완성"],
        "prerequisites": ["웹 개발 기초", "없음"],
    },
    "클론 코딩": {
        "textbooks": ["유튜브/인프런 클론 코딩 강의", "GitHub 오픈소스 레퍼런스"],
        "goals": ["실제 서비스 아키텍처 이해", "풀스택 개발 경험 축적", "코드 품질 향상"],
        "prerequisites": ["프론트엔드 또는 백엔드 기초", "없음"],
    },
    "오픈소스 기여": {
        "textbooks": ["오픈소스 기여 가이드", "GitHub Good First Issues", "오픈소스 프로젝트 목록"],
        "goals": ["오픈소스 PR 3건 이상 머지", "코드 리뷰 경험", "글로벌 개발자 커뮤니티 참여"],
        "prerequisites": ["Git 활용 능력", "영어 커뮤니케이션", "없음"],
    },
    "해커톤 준비": {
        "textbooks": ["해커톤 참가 가이드", "MVP 빠른 개발 방법론", "팀 협업 도구 활용"],
        "goals": ["해커톤 입상", "48시간 내 프로토타입 완성", "팀 협업 능력 향상"],
        "prerequisites": ["개발 기본기", "팀 프로젝트 경험", "없음"],
    },

    # ===== 알고리즘 세부 유형 =====
    "DP/동적프로그래밍": {
        "textbooks": ["이것이 코딩테스트다 DP 파트", "백준 DP 문제 모음", "알고리즘 문제해결 전략"],
        "goals": ["DP 유형별 풀이 패턴 체화", "탑다운/바텀업 자유자재 전환", "트리DP/비트마스크DP 이해"],
        "prerequisites": ["재귀함수 이해", "기본 자료구조 지식", "없음"],
    },
    "그래프 탐색": {
        "textbooks": ["백준 그래프 이론 문제집", "알고리즘 문제해결 전략 그래프 파트", "LeetCode Graph Problems"],
        "goals": ["BFS/DFS 완벽 구현", "최단경로 알고리즘 3종 숙달", "위상 정렬 활용"],
        "prerequisites": ["스택/큐 이해", "재귀함수 기초", "없음"],
    },
    "그리디 알고리즘": {
        "textbooks": ["이것이 코딩테스트다 그리디 파트", "백준 그리디 문제 모음"],
        "goals": ["그리디 vs DP 판별 능력", "그리디 정당성 증명 이해", "MST 알고리즘 구현"],
        "prerequisites": ["정렬 알고리즘 이해", "없음"],
    },
    "투포인터/슬라이딩윈도우": {
        "textbooks": ["백준 투포인터 문제집", "LeetCode Two Pointer Problems"],
        "goals": ["투포인터 O(n) 풀이 체화", "슬라이딩 윈도우 패턴 숙달", "구간 합/최대값 문제 해결"],
        "prerequisites": ["배열/리스트 기본", "없음"],
    },
    "문자열 알고리즘": {
        "textbooks": ["KMP 알고리즘 튜토리얼", "트라이 자료구조 가이드", "LeetCode String Problems"],
        "goals": ["KMP/라빈카프 구현", "트라이 자료구조 활용", "정규표현식 실전 활용"],
        "prerequisites": ["문자열 기본 처리", "없음"],
    },
    "최단경로 알고리즘": {
        "textbooks": ["다익스트라/벨만포드/플로이드 비교 가이드", "백준 최단경로 문제집"],
        "goals": ["3대 최단경로 알고리즘 구현", "음수 가중치 처리 이해", "실전 문제 적용 능력"],
        "prerequisites": ["그래프 기본 표현", "우선순위 큐 이해", "없음"],
    },

    # ===== CS 세부 주제 =====
    "프로세스/스레드": {
        "textbooks": ["운영체제 공룡책 프로세스 파트", "OSTEP 프로세스 챕터"],
        "goals": ["프로세스/스레드 차이 완벽 설명", "IPC 메커니즘 이해", "컨텍스트 스위칭 비용 분석"],
        "prerequisites": ["C언어 기초", "없음"],
    },
    "동기화/데드락": {
        "textbooks": ["운영체제 공룡책 동기화 파트", "세마포어/뮤텍스 가이드"],
        "goals": ["데드락 4조건 설명 가능", "세마포어/뮤텍스 구현 이해", "생산자-소비자 문제 해결"],
        "prerequisites": ["프로세스/스레드 기본", "없음"],
    },
    "TCP/IP": {
        "textbooks": ["컴퓨터 네트워킹: 하향식 접근 TCP 파트", "TCP/IP 완벽 가이드"],
        "goals": ["3-way/4-way handshake 설명", "흐름제어/혼잡제어 이해", "TCP vs UDP 비교"],
        "prerequisites": ["네트워크 기초 개념", "없음"],
    },
    "HTTP/웹 프로토콜": {
        "textbooks": ["HTTP 완벽 가이드", "MDN HTTP 문서", "그림으로 배우는 HTTP"],
        "goals": ["HTTP 메서드/상태코드 숙지", "REST API 설계 원칙", "HTTP/2, HTTP/3 이해"],
        "prerequisites": ["없음"],
    },
    "정규화/인덱스": {
        "textbooks": ["Real MySQL 8.0 인덱스 파트", "데이터베이스 개론 정규화 파트"],
        "goals": ["1NF~BCNF 정규화 과정 설명", "인덱스 종류와 동작 원리 이해", "쿼리 최적화 기법 습득"],
        "prerequisites": ["SQL 기본 문법", "없음"],
    },
    "트랜잭션/동시성": {
        "textbooks": ["Real MySQL 8.0 트랜잭션 파트", "데이터베이스 시스템 트랜잭션 챕터"],
        "goals": ["ACID 완벽 이해", "격리 수준 4가지 비교", "MVCC 동작 원리 설명"],
        "prerequisites": ["SQL 기본", "DB 기초", "없음"],
    },
    "SOLID 원칙": {
        "textbooks": ["클린 아키텍처", "오브젝트", "SOLID 원칙 가이드"],
        "goals": ["5가지 원칙 코드로 설명 가능", "위반 사례 식별 능력", "리팩토링 적용 능력"],
        "prerequisites": ["OOP 기본", "없음"],
    },
    "분산 데이터베이스": {
        "textbooks": ["Designing Data-Intensive Applications", "분산 시스템 기초"],
        "goals": ["CAP 정리 이해", "샤딩/파티셔닝 설계", "레플리케이션 전략 수립"],
        "prerequisites": ["DB 기초", "네트워크 기본", "없음"],
    },
    "보안 기초": {
        "textbooks": ["OWASP Top 10 가이드", "웹 보안 입문", "암호학 기초"],
        "goals": ["주요 웹 취약점 이해", "안전한 코딩 습관", "인증/인가 설계 능력"],
        "prerequisites": ["웹 개발 기본", "없음"],
    },

    # ===== 프론트엔드 세부 =====
    "React Hooks 심화": {
        "textbooks": ["React 공식 문서 Hooks", "모던 리액트 Deep Dive"],
        "goals": ["커스텀 훅 설계 패턴", "메모이제이션 최적화", "훅 규칙과 클로저 이해"],
        "prerequisites": ["React 기초", "JavaScript 중급", "없음"],
    },
    "상태관리 라이브러리": {
        "textbooks": ["Redux Toolkit 공식 문서", "Zustand/Jotai 가이드", "TanStack Query 문서"],
        "goals": ["전역 vs 서버 상태 설계", "Redux/Zustand/Recoil 비교", "적절한 상태관리 선택 능력"],
        "prerequisites": ["React Hooks 기본", "없음"],
    },
    "프론트엔드 테스트": {
        "textbooks": ["Jest 공식 문서", "Testing Library 가이드", "Cypress E2E 테스트"],
        "goals": ["유닛/통합/E2E 테스트 작성", "테스트 커버리지 80%+", "TDD 프론트엔드 적용"],
        "prerequisites": ["React 기본", "JavaScript 중급", "없음"],
    },

    # ===== 백엔드 세부 =====
    "JPA/ORM": {
        "textbooks": ["자바 ORM 표준 JPA 프로그래밍", "토비의 스프링 JPA 파트"],
        "goals": ["영속성 컨텍스트 완벽 이해", "N+1 문제 해결", "QueryDSL 활용"],
        "prerequisites": ["Java 기초", "SQL 기본", "없음"],
    },
    "Spring Security": {
        "textbooks": ["Spring Security in Action", "Spring Security 공식 문서"],
        "goals": ["SecurityFilterChain 설계", "JWT/OAuth2 인증 구현", "권한 체계 설계"],
        "prerequisites": ["Spring Boot 기본", "HTTP 이해", "없음"],
    },
    "Redis 활용": {
        "textbooks": ["Redis 핵심정리", "Redis in Action", "Redis 공식 문서"],
        "goals": ["캐싱 전략 수립", "Pub/Sub 활용", "세션 관리 및 분산 락 구현"],
        "prerequisites": ["백엔드 기본", "없음"],
    },
    "메시지 큐 (Kafka/RabbitMQ)": {
        "textbooks": ["카프카 핵심 가이드", "RabbitMQ in Action", "이벤트 기반 마이크로서비스"],
        "goals": ["메시지 브로커 설계", "이벤트 기반 아키텍처 구현", "Kafka 프로듀서/컨슈머 구현"],
        "prerequisites": ["백엔드 개발 경험", "Docker 기본", "없음"],
    },
    "클린 아키텍처": {
        "textbooks": ["클린 아키텍처", "만들면서 배우는 클린 아키텍처", "헥사고날 아키텍처 가이드"],
        "goals": ["의존성 역전 원칙 적용", "포트와 어댑터 패턴 구현", "테스트 가능한 설계"],
        "prerequisites": ["Spring Boot 경험", "OOP 이해", "없음"],
    },
    "DDD/도메인 주도 설계": {
        "textbooks": ["도메인 주도 설계", "도메인 주도 설계 첫걸음", "이벤트 소싱과 CQRS"],
        "goals": ["바운디드 컨텍스트 설계", "애그리거트 패턴 적용", "이벤트 스토밍 진행 능력"],
        "prerequisites": ["백엔드 실무 경험", "클린 아키텍처 이해", "없음"],
    },

    # ===== 인프라 세부 =====
    "Docker Compose": {
        "textbooks": ["Docker Compose 공식 문서", "시작하세요! 도커/쿠버네티스"],
        "goals": ["멀티 컨테이너 환경 구성", "네트워크/볼륨 설계", "개발 환경 컨테이너화"],
        "prerequisites": ["Docker 기본", "없음"],
    },
    "Terraform/IaC": {
        "textbooks": ["Terraform Up & Running", "Terraform 공식 문서"],
        "goals": ["인프라 코드화", "상태 관리 이해", "모듈 재사용 설계"],
        "prerequisites": ["AWS 또는 GCP 기본", "없음"],
    },
    "GitHub Actions": {
        "textbooks": ["GitHub Actions 공식 문서", "CI/CD 파이프라인 가이드"],
        "goals": ["워크플로우 YAML 작성", "테스트/빌드/배포 자동화", "시크릿 관리 및 환경 설정"],
        "prerequisites": ["Git 기본", "없음"],
    },
    "ELK 스택": {
        "textbooks": ["ELK 스택 완벽 가이드", "Elasticsearch 가이드"],
        "goals": ["로그 수집/분석 파이프라인 구축", "Kibana 대시보드 설계", "검색 엔진 활용"],
        "prerequisites": ["Docker 기본", "Linux 명령어", "없음"],
    },

    # ===== AI 세부 =====
    "Transformer 아키텍처": {
        "textbooks": ["Attention Is All You Need 논문", "Hugging Face 공식 튜토리얼", "The Illustrated Transformer"],
        "goals": ["Self-Attention 메커니즘 이해", "BERT/GPT 구조 비교", "Transformer 직접 구현"],
        "prerequisites": ["딥러닝 기초", "Python 중급", "없음"],
    },
    "LLM/대규모 언어모델": {
        "textbooks": ["LangChain 문서", "OpenAI Cookbook", "LLM 애플리케이션 개발 가이드"],
        "goals": ["LLM API 활용 애플리케이션 개발", "프롬프트 엔지니어링 숙달", "RAG 파이프라인 구축"],
        "prerequisites": ["Python 기초", "API 호출 경험", "없음"],
    },
    "RAG 시스템": {
        "textbooks": ["LangChain RAG 튜토리얼", "벡터 DB 가이드 (Chroma/Pinecone)", "Retrieval-Augmented Generation 논문"],
        "goals": ["벡터 임베딩 검색 구현", "문서 기반 QA 시스템 구축", "청킹/인덱싱 전략 설계"],
        "prerequisites": ["LLM 기본 이해", "Python 중급", "없음"],
    },
    "LoRA/경량 학습": {
        "textbooks": ["LoRA 논문", "Hugging Face PEFT 문서", "QLoRA 가이드"],
        "goals": ["LoRA 파인튜닝 실행", "학습 데이터 구성 능력", "경량 모델 최적화"],
        "prerequisites": ["딥러닝 경험", "PyTorch 기본", "없음"],
    },
    "데이터 분석 (Pandas)": {
        "textbooks": ["파이썬 데이터 사이언스 핸드북", "Pandas 공식 문서", "데이터 분석을 위한 파이썬"],
        "goals": ["EDA 분석 능력", "데이터 시각화 (Matplotlib/Seaborn)", "전처리 파이프라인 구축"],
        "prerequisites": ["Python 기초", "없음"],
    },

    # ===== 모바일 세부 =====
    "Jetpack Compose": {
        "textbooks": ["Android Developers Compose 가이드", "Compose 공식 코드랩"],
        "goals": ["Compose UI 구현", "상태 관리 패턴", "애니메이션 구현"],
        "prerequisites": ["Kotlin 기초", "Android 기본", "없음"],
    },
    "SwiftUI": {
        "textbooks": ["SwiftUI 공식 튜토리얼", "SwiftUI Thinking"],
        "goals": ["SwiftUI 선언형 UI 구현", "Combine 연동", "iOS 앱 출시"],
        "prerequisites": ["Swift 기초", "없음"],
    },

    # ===== 보안 =====
    "웹 보안 (OWASP)": {
        "textbooks": ["OWASP Top 10 가이드", "웹 해킹과 보안 입문", "Bug Bounty Bootcamp"],
        "goals": ["XSS/CSRF/SQLi 방어 구현", "보안 헤더 설정", "OWASP Top 10 완벽 이해"],
        "prerequisites": ["웹 개발 기본", "HTTP 이해", "없음"],
    },
    "암호학": {
        "textbooks": ["암호학 기초", "Serious Cryptography", "SSL/TLS 완벽 가이드"],
        "goals": ["대칭키/비대칭키 이해", "해싱/솔팅 구현", "PKI 인증서 체계 이해"],
        "prerequisites": ["수학 기초", "없음"],
    },
    "인증/인가 설계": {
        "textbooks": ["OAuth 2.0 in Action", "JWT 핸드북", "인증 시스템 설계 가이드"],
        "goals": ["OAuth2/OIDC 플로우 설계", "JWT 기반 인증 구현", "RBAC/ABAC 권한 체계 설계"],
        "prerequisites": ["웹 개발 경험", "HTTP 기본", "없음"],
    },

    # ===== 데이터 엔지니어링 =====
    "ETL 파이프라인": {
        "textbooks": ["데이터 엔지니어링 기초", "ETL 설계 패턴", "Apache Airflow 가이드"],
        "goals": ["ETL 파이프라인 설계/구현", "데이터 품질 모니터링", "오류 처리 및 재시도 전략"],
        "prerequisites": ["Python/SQL 기본", "없음"],
    },
    "Spark/Hadoop": {
        "textbooks": ["Learning Spark", "Hadoop 완벽 가이드", "Spark in Action"],
        "goals": ["분산 처리 개념 이해", "Spark DataFrame/SQL 활용", "성능 튜닝"],
        "prerequisites": ["Python/Scala 기본", "SQL 중급", "없음"],
    },
    "Airflow": {
        "textbooks": ["Apache Airflow 공식 문서", "Data Pipelines with Airflow"],
        "goals": ["DAG 작성 능력", "오케스트레이션 설계", "커스텀 오퍼레이터 구현"],
        "prerequisites": ["Python 기본", "없음"],
    },
    "데이터 웨어하우스": {
        "textbooks": ["The Data Warehouse Toolkit", "Snowflake 공식 가이드", "BigQuery 실전"],
        "goals": ["스타/스노우플레이크 스키마 설계", "차원 모델링", "분석 쿼리 최적화"],
        "prerequisites": ["SQL 중급", "데이터 모델링 기초", "없음"],
    },

    # ===== Git/협업 =====
    "Git 기본": {
        "textbooks": ["Pro Git", "Git 공식 문서", "누구나 쉽게 배우는 Git"],
        "goals": ["commit/branch/merge 숙달", "충돌 해결 능력", "rebase/cherry-pick 활용"],
        "prerequisites": ["없음"],
    },
    "브랜치 전략": {
        "textbooks": ["Git Flow 가이드", "GitHub Flow 가이드", "Trunk Based Development"],
        "goals": ["팀에 적합한 브랜치 전략 수립", "PR 리뷰 프로세스 설계", "릴리즈 관리 체계 구축"],
        "prerequisites": ["Git 기본", "팀 프로젝트 경험", "없음"],
    },
    "코드 리뷰": {
        "textbooks": ["구글 코드 리뷰 가이드", "효과적인 코드 리뷰", "Code Review Best Practices"],
        "goals": ["건설적인 리뷰 작성 능력", "코드 품질 기준 수립", "리뷰 문화 정착"],
        "prerequisites": ["Git 기본", "프로그래밍 경험", "없음"],
    },
}


# =============================================
# 프로필 생성 함수
# =============================================

def generate_random_profile():
    """랜덤 사용자 프로필 생성"""
    profile_key = random.choice(list(TECH_STACKS.keys()))
    profile = TECH_STACKS[profile_key]
    schedule = random.choice(SCHEDULE_PATTERNS)
    # 선호 기간: 2~8주 (4주에 가중치)
    preferred_duration = random.choices(
        [2, 3, 4, 5, 6, 7, 8],
        weights=[5, 10, 30, 20, 15, 10, 10],
        k=1
    )[0]

    return {
        "profile_key": profile_key,
        "tech_stack": profile["tech"],
        "related_topics": profile["related_topics"],
        "schedule": schedule,
        "preferred_duration": preferred_duration,
    }


def pick_topic_input(profile):
    """프로필과 연관된 카테고리에서 자유 주제 텍스트 선택"""
    # 70% 확률로 연관 카테고리에서, 30% 확률로 랜덤 카테고리에서 선택
    if random.random() < 0.7 and profile["related_topics"]:
        category = random.choice(profile["related_topics"])
    else:
        category = random.choice(list(FREE_TOPIC_INPUTS.keys()))

    topic_text = random.choice(FREE_TOPIC_INPUTS[category])
    return topic_text, category


def format_schedule_str(schedule):
    """일정 정보를 사용자 입력 형태의 문자열로 변환"""
    if not schedule["slots"]:
        return "미지정"
    parts = []
    for day, time_range in schedule["slots"].items():
        parts.append(f"{day} {time_range}")
    return ", ".join(parts)


# =============================================
# GPT 호출 + ChatML 포맷
# =============================================

def get_client(api_key):
    from openai import OpenAI
    return OpenAI(api_key=api_key)


def get_difficulty_hint(topic_text, profile):
    """사용자 기술스택과 주제의 겹침 정도에 따른 난이도 힌트 생성.
    이미 관련 기술을 보유한 경우 → 고급(ADVANCED) 또는 중급(INTERMEDIATE) 권장."""
    user_techs = set(profile["tech_stack"])
    overlapping_techs = []

    for tech in user_techs:
        if tech in TECH_TOPIC_OVERLAP:
            related_topics = TECH_TOPIC_OVERLAP[tech]
            # 주제 텍스트에 관련 토픽 키워드가 포함되어 있는지 확인
            for rt in related_topics:
                if rt.lower() in topic_text.lower() or topic_text.lower() in rt.lower():
                    overlapping_techs.append(tech)
                    break

    if overlapping_techs:
        techs = ", ".join(overlapping_techs)
        return f"\n참고: 사용자가 이미 [{techs}]을(를) 보유하고 있으므로, 해당 주제의 난이도는 INTERMEDIATE 이상(가급적 ADVANCED)으로 설정해주세요."
    return ""


def build_user_message(topic_text, profile):
    """사용자 메시지 구성"""
    tech_str = ", ".join(profile["tech_stack"])
    schedule_str = format_schedule_str(profile["schedule"])
    difficulty_hint = get_difficulty_hint(topic_text, profile)
    duration_weeks = profile.get("preferred_duration", random.choice([2, 3, 4, 5, 6, 7, 8]))

    msg = f"""스터디 주제: {topic_text}
기술 스택: {tech_str}
가용 일정: {schedule_str}
선호 기간: {duration_weeks}주{difficulty_hint}

위 정보를 바탕으로 완성된 스터디 계획을 JSON으로 생성해주세요."""
    return msg


def generate_study_plan(client, topic_text, profile):
    """GPT-4o-mini로 스터디 계획 JSON 생성"""
    user_msg = build_user_message(topic_text, profile)

    try:
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": user_msg},
            ],
            temperature=0.85,
            max_tokens=1024,
        )
        raw = response.choices[0].message.content.strip()

        # JSON 파싱
        json_text = raw
        if "```json" in json_text:
            json_text = json_text.split("```json")[1].split("```")[0].strip()
        elif "```" in json_text:
            json_text = json_text.split("```")[1].split("```")[0].strip()

        parsed = json.loads(json_text)

        # 필수 필드 검증
        required = ["name", "intro", "description", "topic", "format",
                     "difficulty", "goal", "textbook", "processDetail", "scheduleSuggestion"]
        for field in required:
            if field not in parsed:
                return None, None

        # durationWeeks 기본값 설정 (없으면 4주)
        if "durationWeeks" not in parsed or not isinstance(parsed.get("durationWeeks"), int):
            parsed["durationWeeks"] = random.choice([2, 3, 4, 5, 6, 7, 8])
        else:
            parsed["durationWeeks"] = max(2, min(8, parsed["durationWeeks"]))

        # difficulty 검증
        valid_diff = ["BEGINNER", "INTERMEDIATE", "ADVANCED"]
        if parsed["difficulty"] not in valid_diff:
            return None, None

        # format 검증
        if parsed["format"] not in FORMATS:
            # 유사 매칭 시도
            matched = False
            for f in FORMATS:
                if f in parsed["format"] or parsed["format"] in f:
                    parsed["format"] = f
                    matched = True
                    break
            if not matched:
                return None, None

        # scheduleSuggestion 검증
        if not isinstance(parsed.get("scheduleSuggestion"), dict):
            return None, None
        if "days" not in parsed["scheduleSuggestion"] or "time" not in parsed["scheduleSuggestion"]:
            return None, None

        # prerequisites 기본값
        if "prerequisites" not in parsed:
            parsed["prerequisites"] = "없음"

        return user_msg, json.dumps(parsed, ensure_ascii=False)

    except (json.JSONDecodeError, KeyError, IndexError) as e:
        return None, None
    except Exception as e:
        print(f"    [API ERROR] {e}")
        return None, None


def format_chatml(user_msg, assistant_msg):
    """ChatML 포맷으로 변환"""
    text = (
        f"<|im_start|>system\n{SYSTEM_PROMPT}<|im_end|>\n"
        f"<|im_start|>user\n{user_msg}<|im_end|>\n"
        f"<|im_start|>assistant\n{assistant_msg}<|im_end|>"
    )
    return text


# =============================================
# 체크포인트
# =============================================

def save_checkpoint(data, count, stats):
    with open(CHECKPOINT_PATH, "w", encoding="utf-8") as f:
        json.dump({"data": data, "count": count, "stats": stats}, f, ensure_ascii=False, indent=2)


def load_checkpoint():
    if os.path.exists(CHECKPOINT_PATH):
        with open(CHECKPOINT_PATH, "r", encoding="utf-8") as f:
            return json.load(f)
    return None


# =============================================
# 메인 실행
# =============================================

def main():
    parser = argparse.ArgumentParser(description="스터디 계획 생성 LoRA 학습 데이터 합성")
    parser.add_argument("--api-key", required=True, help="OpenAI API key")
    parser.add_argument("--resume", action="store_true", help="체크포인트에서 재개")
    parser.add_argument("--target", type=int, default=TOTAL_TARGET, help=f"생성 목표 수 (기본: {TOTAL_TARGET})")
    args = parser.parse_args()

    target = args.target
    client = get_client(args.api_key)

    all_data = []
    generated = 0
    stats = {
        "by_profile": {},
        "by_category": {},
        "by_difficulty": {},
        "by_format": {},
    }

    if args.resume:
        checkpoint = load_checkpoint()
        if checkpoint:
            all_data = checkpoint["data"]
            generated = checkpoint["count"]
            stats = checkpoint.get("stats", stats)
            print(f"체크포인트에서 재개: {generated}개")

    print("=" * 70)
    print(f"스터디 계획 생성 학습 데이터 합성 v2")
    print(f"목표: {target}개 | 토픽 카테고리: {len(FREE_TOPIC_INPUTS)}개 | 프로필: {len(TECH_STACKS)}개")
    print(f"자유 주제 텍스트: {sum(len(v) for v in FREE_TOPIC_INPUTS.values())}개 변형")
    print("=" * 70)

    failed = 0
    consecutive_fail = 0

    while generated < target:
        # 1. 랜덤 프로필 생성
        profile = generate_random_profile()

        # 2. 자유 주제 텍스트 선택
        topic_text, category = pick_topic_input(profile)

        # 3. GPT로 스터디 계획 생성
        user_msg, plan_json = generate_study_plan(client, topic_text, profile)

        if user_msg and plan_json:
            # ChatML 포맷
            chatml = format_chatml(user_msg, plan_json)

            # 통계 업데이트
            parsed = json.loads(plan_json)
            stats["by_profile"][profile["profile_key"]] = stats["by_profile"].get(profile["profile_key"], 0) + 1
            stats["by_category"][category] = stats["by_category"].get(category, 0) + 1
            stats["by_difficulty"][parsed.get("difficulty", "UNKNOWN")] = stats["by_difficulty"].get(parsed.get("difficulty", "UNKNOWN"), 0) + 1
            stats["by_format"][parsed.get("format", "UNKNOWN")] = stats["by_format"].get(parsed.get("format", "UNKNOWN"), 0) + 1

            all_data.append({
                "text": chatml,
                "type": "study_plan",
                "topic_input": topic_text,
                "category": category,
                "profile_key": profile["profile_key"],
                "difficulty": parsed.get("difficulty"),
                "format": parsed.get("format"),
            })
            generated += 1
            consecutive_fail = 0
        else:
            failed += 1
            consecutive_fail += 1
            if consecutive_fail > 10:
                print(f"  [WARNING] 연속 실패 {consecutive_fail}회, 5초 대기...")
                time.sleep(5)
                consecutive_fail = 0

        # 진행 상황 표시 + 체크포인트
        if generated > 0 and generated % 20 == 0:
            pct = generated / target * 100
            print(f"  진행: {generated}/{target} ({pct:.1f}%) | 실패: {failed}건")
            save_checkpoint(all_data, generated, stats)

        time.sleep(0.3)

    # ===== 저장 =====
    random.shuffle(all_data)
    split_idx = int(len(all_data) * 0.9)
    train_data = all_data[:split_idx]
    val_data = all_data[split_idx:]

    with open(OUTPUT_PATH, "w", encoding="utf-8") as f:
        json.dump(all_data, f, ensure_ascii=False, indent=2)

    train_path = os.path.join(OUTPUT_DIR, "recommend_training_data_train.json")
    val_path = os.path.join(OUTPUT_DIR, "recommend_training_data_val.json")

    with open(train_path, "w", encoding="utf-8") as f:
        json.dump(train_data, f, ensure_ascii=False, indent=2)
    with open(val_path, "w", encoding="utf-8") as f:
        json.dump(val_data, f, ensure_ascii=False, indent=2)

    # ===== 통계 출력 =====
    print(f"\n{'='*70}")
    print(f"스터디 계획 학습 데이터 생성 완료!")
    print(f"{'='*70}")
    print(f"  총 데이터:  {len(all_data)}개")
    print(f"  학습:       {len(train_data)}개")
    print(f"  검증:       {len(val_data)}개")
    print(f"  총 실패:    {failed}건")

    print(f"\n  [카테고리별]")
    for cat, cnt in sorted(stats["by_category"].items(), key=lambda x: -x[1]):
        print(f"    {cat}: {cnt}개")

    print(f"\n  [프로필별 (상위 10)]")
    for prf, cnt in sorted(stats["by_profile"].items(), key=lambda x: -x[1])[:10]:
        print(f"    {prf}: {cnt}개")

    print(f"\n  [난이도별]")
    for diff, cnt in sorted(stats["by_difficulty"].items()):
        print(f"    {diff}: {cnt}개")

    print(f"\n  [형식별]")
    for fmt, cnt in sorted(stats["by_format"].items(), key=lambda x: -x[1]):
        print(f"    {fmt}: {cnt}개")

    print(f"\n  저장 위치:")
    print(f"    전체: {OUTPUT_PATH}")
    print(f"    학습: {train_path}")
    print(f"    검증: {val_path}")
    print(f"{'='*70}")

    # 체크포인트 정리
    if os.path.exists(CHECKPOINT_PATH):
        os.remove(CHECKPOINT_PATH)


if __name__ == "__main__":
    main()
