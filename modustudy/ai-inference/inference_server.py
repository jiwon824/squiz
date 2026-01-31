"""
AI 추론 서버 (FastAPI + llama-cpp-python + Whisper)
- Qwen3-8B GGUF 모델 로드
- Whisper STT (faster-whisper)
- Redis Queue 비동기 작업 처리

실행:
  pip install -r requirements.txt
  python inference_server.py
"""

import os
import uuid
import tempfile
import subprocess
from pathlib import Path
from typing import Optional, List
from fastapi import FastAPI, HTTPException, UploadFile, File, BackgroundTasks
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from contextlib import asynccontextmanager
import uvicorn
import json

# 설정
MODEL_PATH = os.getenv("MODEL_PATH", "./models/qwen3-8b-summarizer-q4km.gguf")
WHISPER_MODEL = os.getenv("WHISPER_MODEL", "medium")  # tiny, base, small, medium, large-v3
WHISPER_DEVICE = os.getenv("WHISPER_DEVICE", "cpu")  # cpu or cuda
N_CTX = int(os.getenv("N_CTX", "4096"))
N_GPU_LAYERS = int(os.getenv("N_GPU_LAYERS", "0"))  # 0 = CPU만, -1 = 전체 GPU
N_THREADS = int(os.getenv("N_THREADS", "4"))
HOST = os.getenv("HOST", "0.0.0.0")
PORT = int(os.getenv("PORT", "8000"))
REDIS_URL = os.getenv("REDIS_URL", "redis://localhost:6379")

# 전역 모델
llm = None
whisper_model = None


# ===== 음성 전처리 =====

def preprocess_audio(input_path: str) -> str:
    """
    음성 전처리 (노이즈 제거 + 정규화)
    - highpass: 75Hz 이하 저주파 제거 (남성 저음 보존)
    - lowpass: 8kHz 이상 고주파 제거 (하울링)
    - afftdn: FFT 기반 노이즈 감쇠 (loudnorm 전에 배치)
    - loudnorm: 볼륨 정규화
    - 16kHz mono PCM으로 변환 (Whisper 최적)
    
    Returns: 전처리된 파일 경로 (실패 시 원본 경로)
    """
    try:
        # 출력 파일 경로 (UUID로 충돌 방지)
        output_path = input_path.rsplit(".", 1)[0] + f"_preprocessed_{uuid.uuid4().hex[:8]}.wav"
        
        # ffmpeg 명령어 (최적화)
        cmd = [
            "ffmpeg", "-i", input_path,
            "-af", "highpass=f=75,lowpass=f=8000,afftdn=nf=-25,loudnorm",
            "-c:a", "pcm_s16le",  # 무압축 PCM (Whisper 최적)
            "-ar", "16000",       # 16kHz 샘플레이트
            "-ac", "1",           # 모노
            "-threads", "0",      # CPU 자동 최적화
            "-y",                 # 덮어쓰기
            output_path
        ]
        
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=300  # 5분 타임아웃
        )
        
        if result.returncode == 0:
            print(f"[PREPROCESS] 전처리 완료: {input_path} -> {output_path}")
            return output_path
        else:
            print(f"[PREPROCESS] ffmpeg 실패: {result.stderr[:200]}")
            return input_path  # 원본 사용 (fallback)
            
    except subprocess.TimeoutExpired:
        print(f"[PREPROCESS] 타임아웃 (5분 초과): {input_path}")
        return input_path
    except Exception as e:
        print(f"[PREPROCESS] 에러: {e}")
        return input_path


def cleanup_preprocessed(original_path: str, preprocessed_path: str):
    """전처리된 임시 파일 삭제"""
    if preprocessed_path != original_path and os.path.exists(preprocessed_path):
        try:
            os.unlink(preprocessed_path)
        except Exception:
            pass



def load_llm():
    """LLM 모델 로드"""
    global llm
    if not os.path.exists(MODEL_PATH):
        print(f"[WARNING] LLM 모델 파일 없음: {MODEL_PATH}")
        return None

    from llama_cpp import Llama
    print(f"LLM 모델 로드 중: {MODEL_PATH}")
    llm = Llama(
        model_path=MODEL_PATH,
        n_ctx=N_CTX,
        n_gpu_layers=N_GPU_LAYERS,
        n_threads=N_THREADS,
        verbose=False,
    )
    print("LLM 모델 로드 완료!")
    return llm


def load_whisper():
    """Whisper 모델 로드"""
    global whisper_model
    try:
        from faster_whisper import WhisperModel
        print(f"Whisper 모델 로드 중: {WHISPER_MODEL} ({WHISPER_DEVICE})")

        compute_type = "float32" if WHISPER_DEVICE == "cpu" else "float16"

        whisper_model = WhisperModel(
            WHISPER_MODEL,
            device=WHISPER_DEVICE,
            compute_type=compute_type,
            num_workers=2,
        )
        print("Whisper 모델 로드 완료!")
        return whisper_model
    except Exception as e:
        print(f"[WARNING] Whisper 로드 실패: {e}")
        return None


# 추천 전용 LLM (14B 모델)
recommend_llm = None
RECOMMEND_MODEL_PATH = os.getenv("RECOMMEND_MODEL_PATH", "./models/qwen3-14b-recommend-q4km.gguf")


def load_recommend_llm():
    """추천용 LLM 모델 로드"""
    global recommend_llm
    if not os.path.exists(RECOMMEND_MODEL_PATH):
        print(f"[WARNING] 추천 모델 파일 없음: {RECOMMEND_MODEL_PATH}")
        return None

    from llama_cpp import Llama
    print(f"추천 모델 로드 중: {RECOMMEND_MODEL_PATH}")
    recommend_llm = Llama(
        model_path=RECOMMEND_MODEL_PATH,
        n_ctx=N_CTX,
        n_gpu_layers=N_GPU_LAYERS,
        n_threads=N_THREADS,
        verbose=False,
    )
    print("추천 모델 로드 완료!")
    return recommend_llm


@asynccontextmanager
async def lifespan(app: FastAPI):
    """서버 시작/종료 시 모델 로드/언로드"""
    print("=" * 60)
    print("Squiz AI 추론 서버 시작")
    print("=" * 60)

    load_llm()
    load_recommend_llm()
    load_whisper()

    print("=" * 60)
    yield
    print("서버 종료")


app = FastAPI(
    title="Squiz AI 추론 서버",
    description="IT 스터디 회의록 STT + 요약 API",
    version="1.0.0",
    lifespan=lifespan,
)


# ===== Request/Response 모델 =====

class SummarizeRequest(BaseModel):
    transcript: str
    max_tokens: int = 512
    temperature: float = 0.7

    class Config:
        json_schema_extra = {
            "example": {
                "transcript": "김민수: 오늘은 Docker 기초에 대해 공부해봤어요.\n이지은: 네, 컨테이너랑 이미지 개념이 처음엔 헷갈렸는데 이제 좀 이해됐어요.",
                "max_tokens": 512,
                "temperature": 0.7
            }
        }


class SummarizeResponse(BaseModel):
    summary: str
    tokens_used: int


class QuizRequest(BaseModel):
    summary: str
    num_questions: int = 5
    max_tokens: int = 1024
    temperature: float = 0.7


class QuizResponse(BaseModel):
    quiz: str
    tokens_used: int


class STTResponse(BaseModel):
    text: str
    segments: list
    language: str
    duration: float


class JobResponse(BaseModel):
    job_id: str
    status: str
    message: str


class JobStatusResponse(BaseModel):
    job_id: str
    status: str  # pending, processing, completed, failed
    result: Optional[dict] = None
    error: Optional[str] = None


class HealthResponse(BaseModel):
    status: str
    llm_loaded: bool
    whisper_loaded: bool
    model_path: str
    whisper_model: str


# ===== 작업 저장소 (간단한 인메모리, 프로덕션에서는 Redis 사용) =====
jobs = {}


# ===== API 엔드포인트 =====

@app.get("/health", response_model=HealthResponse)
async def health_check():
    """헬스체크"""
    return HealthResponse(
        status="ok",
        llm_loaded=llm is not None,
        whisper_loaded=whisper_model is not None,
        model_path=MODEL_PATH,
        whisper_model=WHISPER_MODEL,
    )


@app.post("/api/stt", response_model=STTResponse)
async def speech_to_text(file: UploadFile = File(...)):
    """
    음성 파일을 텍스트로 변환 (동기)
    - 지원 형식: wav, mp3, m4a, webm 등
    """
    if whisper_model is None:
        raise HTTPException(status_code=503, detail="Whisper 모델이 로드되지 않았습니다")

    # 임시 파일로 저장
    suffix = Path(file.filename).suffix if file.filename else ".wav"
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        content = await file.read()
        tmp.write(content)
        tmp_path = tmp.name

    preprocessed_path = None
    try:
        # 음성 전처리 (노이즈 제거)
        preprocessed_path = preprocess_audio(tmp_path)
        
        # Whisper 추론
        segments, info = whisper_model.transcribe(
            preprocessed_path,
            language="ko",
            beam_size=5,
            vad_filter=True,
        )

        # 결과 조합
        segment_list = []
        full_text = []
        for seg in segments:
            segment_list.append({
                "start": seg.start,
                "end": seg.end,
                "text": seg.text.strip(),
            })
            full_text.append(seg.text.strip())

        return STTResponse(
            text=" ".join(full_text),
            segments=segment_list,
            language=info.language,
            duration=info.duration,
        )

    finally:
        # 임시 파일 삭제
        if preprocessed_path:
            cleanup_preprocessed(tmp_path, preprocessed_path)
        os.unlink(tmp_path)


@app.post("/api/stt/async", response_model=JobResponse)
async def speech_to_text_async(
    background_tasks: BackgroundTasks,
    file: UploadFile = File(...),
):
    """
    음성 파일을 텍스트로 변환 (비동기)
    - job_id로 상태 조회 가능
    """
    if whisper_model is None:
        raise HTTPException(status_code=503, detail="Whisper 모델이 로드되지 않았습니다")

    # 임시 파일로 저장
    suffix = Path(file.filename).suffix if file.filename else ".wav"
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        content = await file.read()
        tmp.write(content)
        tmp_path = tmp.name

    # Job 생성
    job_id = str(uuid.uuid4())
    jobs[job_id] = {"status": "pending", "result": None, "error": None}

    # 백그라운드 작업 등록
    background_tasks.add_task(process_stt_job, job_id, tmp_path)

    return JobResponse(
        job_id=job_id,
        status="pending",
        message="STT 작업이 등록되었습니다. /api/jobs/{job_id}로 상태를 확인하세요.",
    )


def process_stt_job(job_id: str, file_path: str):
    """STT 백그라운드 작업"""
    preprocessed_path = None
    try:
        jobs[job_id]["status"] = "processing"
        
        # 음성 전처리
        preprocessed_path = preprocess_audio(file_path)

        segments, info = whisper_model.transcribe(
            preprocessed_path,
            language="ko",
            beam_size=5,
            vad_filter=True,
        )

        segment_list = []
        full_text = []
        for seg in segments:
            segment_list.append({
                "start": seg.start,
                "end": seg.end,
                "text": seg.text.strip(),
            })
            full_text.append(seg.text.strip())

        jobs[job_id]["status"] = "completed"
        jobs[job_id]["result"] = {
            "text": " ".join(full_text),
            "segments": segment_list,
            "language": info.language,
            "duration": info.duration,
        }

    except Exception as e:
        jobs[job_id]["status"] = "failed"
        jobs[job_id]["error"] = str(e)

    finally:
        # 임시 파일 삭제
        if preprocessed_path:
            cleanup_preprocessed(file_path, preprocessed_path)
        if os.path.exists(file_path):
            os.unlink(file_path)


@app.get("/api/jobs/{job_id}", response_model=JobStatusResponse)
async def get_job_status(job_id: str):
    """작업 상태 조회"""
    if job_id not in jobs:
        raise HTTPException(status_code=404, detail="Job not found")

    job = jobs[job_id]
    return JobStatusResponse(
        job_id=job_id,
        status=job["status"],
        result=job["result"],
        error=job["error"],
    )


@app.post("/api/summarize", response_model=SummarizeResponse)
async def summarize_meeting(request: SummarizeRequest):
    """회의록 요약 API"""
    if llm is None:
        raise HTTPException(status_code=503, detail="LLM 모델이 로드되지 않았습니다")

    prompt = f"""<|im_start|>system
당신은 IT 스터디 회의록을 요약하는 전문가입니다. 핵심 내용을 정확하게 정리합니다.<|im_end|>
<|im_start|>user
다음 IT 스터디 회의 내용을 요약해주세요.

회의 내용:
{request.transcript}<|im_end|>
<|im_start|>assistant
"""

    try:
        output = llm(
            prompt,
            max_tokens=request.max_tokens,
            temperature=request.temperature,
            stop=["<|im_end|>", "<|im_start|>"],
            echo=False,
        )

        summary = output["choices"][0]["text"].strip()
        tokens_used = output["usage"]["total_tokens"]

        return SummarizeResponse(
            summary=summary,
            tokens_used=tokens_used,
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/quiz", response_model=QuizResponse)
async def generate_quiz(request: QuizRequest):
    """복습 퀴즈 생성 API"""
    if llm is None:
        raise HTTPException(status_code=503, detail="LLM 모델이 로드되지 않았습니다")

    prompt = f"""<|im_start|>system
당신은 IT 스터디 내용을 바탕으로 복습 퀴즈를 생성하는 전문가입니다.
객관식, OX, 단답형을 적절히 혼합하여 출제합니다.<|im_end|>
<|im_start|>user
다음 스터디 요약 내용을 바탕으로 복습 퀴즈 {request.num_questions}문제를 생성해주세요.

스터디 요약:
{request.summary}<|im_end|>
<|im_start|>assistant
"""

    try:
        output = llm(
            prompt,
            max_tokens=request.max_tokens,
            temperature=request.temperature,
            stop=["<|im_end|>", "<|im_start|>"],
            echo=False,
        )

        quiz = output["choices"][0]["text"].strip()
        tokens_used = output["usage"]["total_tokens"]

        return QuizResponse(
            quiz=quiz,
            tokens_used=tokens_used,
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# ===== 스터디 템플릿 추천 =====

class TemplateRecommendRequest(BaseModel):
    topic_input: Optional[str] = None  # 사용자 입력 주제 (필수!)
    duration_weeks: Optional[int] = 4
    total_sessions: Optional[int] = None  # 총 회차 (요일수 × 주수)
    user_tech: Optional[list] = []
    user_schedule: Optional[dict] = {}
    study_type: Optional[str] = None
    difficulty_preference: Optional[str] = None


class TemplateRecommendResponse(BaseModel):
    name: str
    intro: str
    description: str
    topic: str
    format: str
    difficulty: str
    goal: str
    textbook: str
    prerequisites: str
    process_detail: str
    schedule_suggestion: Optional[dict] = None
    curriculum: Optional[list] = None
    reason: Optional[str] = None
    tokens_used: int = 0


@app.post("/api/recommend-template")
async def recommend_template(request: TemplateRecommendRequest):
    """
    사용자 입력 주제와 선호 설정 기반 스터디 템플릿 생성
    """
    global recommend_llm
    if recommend_llm is None:
        load_recommend_llm()
    if recommend_llm is None:
        raise HTTPException(status_code=503, detail="추천 모델이 로드되지 않았습니다")

    # 사용자 입력 주제 (필수)
    topic_input = request.topic_input or "IT 스터디"
    duration_weeks = request.duration_weeks or 4
    # 총 회차: 전달받은 값 사용, 없으면 주수 기본값
    total_sessions = request.total_sessions or duration_weeks
    user_tech = request.user_tech or []
    user_schedule = request.user_schedule or {}

    # 스케줄 문자열 생성
    schedule_str = ""
    if user_schedule:
        days = list(user_schedule.keys())
        schedule_str = f"선호 요일: {', '.join(days)}"

    tech_str = ", ".join(user_tech) if user_tech else "없음"

    # 정확한 topic/format 목록 (DB와 일치 - 반드시 이 목록에서만 선택해야 함)
    topic_list = """
[알고리즘/코딩테스트]
- 백준
- 프로그래머스
- SWEA
- LeetCode
- 코딩테스트 대비
- 자료구조
- 알고리즘 이론

[CS 기초]
- 운영체제
- 네트워크
- 데이터베이스
- 컴퓨터구조
- 디자인패턴
- 시스템 설계

[프론트엔드]
- HTML/CSS
- JavaScript
- TypeScript
- React
- Vue
- Next.js
- 웹 접근성/성능

[백엔드]
- Java/Spring
- Python/Django
- Python/FastAPI
- Node.js/Express
- Go
- Kotlin
- API 설계

[DevOps/인프라]
- Docker
- Kubernetes
- CI/CD
- AWS
- GCP
- Linux
- 모니터링

[AI/데이터]
- 머신러닝 기초
- 딥러닝
- NLP
- 컴퓨터 비전
- MLOps
- 논문 리뷰

[모바일]
- Android (Kotlin)
- Android (Java)
- iOS (Swift)
- Flutter
- React Native
"""

    format_list = """
- 문제 풀이: 알고리즘/코딩테스트 문제를 함께 풀어보는 형식
- 독서/책 스터디: 기술 서적을 함께 읽고 토론하는 형식
- 강의 수강: 온라인 강의를 함께 수강하고 토론하는 형식
- 프로젝트: 실제 프로젝트를 함께 개발하는 형식
- 모의 면접: 기술 면접 대비 연습을 하는 형식
- 코드 리뷰: 서로의 코드를 리뷰하고 피드백하는 형식
- 발표/세미나: 각자 학습한 내용을 발표하는 형식
- 토론: 특정 주제에 대해 토론하는 형식
"""

    prompt = f"""<|im_start|>system
당신은 IT 스터디 계획을 작성하는 전문가입니다.
사용자의 학습 주제를 분석하여 체계적인 스터디 계획을 JSON 형식으로 작성합니다.

중요 규칙:
1. topic 필드는 반드시 아래 목록에서 정확히 일치하는 값을 선택하세요.
2. format 필드는 반드시 아래 목록에서 정확히 일치하는 값을 선택하세요.
3. 목록에 없는 값을 임의로 만들지 마세요.
4. JSON만 출력하고, 설명이나 부가 텍스트를 추가하지 마세요.
5. curriculum 배열은 반드시 요청된 총 회차 수만큼 생성하세요.

선택 가능한 topic 목록:
{topic_list}

선택 가능한 format 목록:
{format_list}
<|im_end|>
<|im_start|>user
사용자 입력 주제: {topic_input}
총 회차: {total_sessions}회
사용자 기술 스택: {tech_str}
{schedule_str}

위 정보를 바탕으로 스터디 계획을 JSON으로 작성해주세요.
topic은 위 목록에서 가장 적합한 것을 정확히 선택하고,
format도 주제에 맞는 것을 위 목록에서 정확히 선택해주세요.
curriculum은 반드시 {total_sessions}개의 회차를 생성해주세요.

예를 들어:
- "스프링 학습" → topic: "Java/Spring"
- "리액트 공부" → topic: "React"
- "알고리즘 문제풀이" → topic: "알고리즘 이론" 또는 "백준", format: "문제 풀이"

출력할 JSON 형식:
{{
  "name": "스터디 제목",
  "intro": "한 줄 소개 (15자 내외)",
  "description": "스터디 상세 설명 (2-3문장)",
  "topic": "목록에서 정확히 선택",
  "format": "목록에서 정확히 선택",
  "difficulty": "BEGINNER 또는 INTERMEDIATE 또는 ADVANCED",
  "goal": "스터디 목표",
  "textbook": "추천 교재 또는 참고 자료",
  "prerequisites": "선수 지식 요건",
  "process_detail": "스터디 진행 방식 설명",
  "curriculum": [
    {{"session": 1, "title": "1회차 제목", "description": "학습 내용"}},
    {{"session": 2, "title": "2회차 제목", "description": "학습 내용"}},
    ...
    {{"session": {total_sessions}, "title": "{total_sessions}회차 제목", "description": "학습 내용"}}
  ]
}}
<|im_end|>
<|im_start|>assistant
"""

    try:
        output = recommend_llm(
            prompt,
            max_tokens=2048,
            temperature=0.7,
            stop=["<|im_end|>", "<|im_start|>"],
            echo=False,
        )

        response_text = output["choices"][0]["text"].strip()
        tokens_used = output["usage"]["total_tokens"]

        # JSON 파싱 시도
        import json
        import re

        # JSON 블록 추출
        json_match = re.search(r'\{[\s\S]*\}', response_text)
        if json_match:
            try:
                result = json.loads(json_match.group())
                return {
                    "name": result.get("name", f"{topic_input} 스터디"),
                    "intro": result.get("intro", f"{topic_input} 학습을 위한 스터디"),
                    "description": result.get("description", ""),
                    "topic": result.get("topic", topic_input),
                    "format": result.get("format", "강의 학습"),
                    "difficulty": result.get("difficulty", "INTERMEDIATE"),
                    "goal": result.get("goal", ""),
                    "textbook": result.get("textbook", ""),
                    "prerequisites": result.get("prerequisites", ""),
                    "process_detail": result.get("process_detail", ""),
                    "schedule_suggestion": None,
                    "curriculum": result.get("curriculum", []),
                    "reason": None,
                    "tokens_used": tokens_used,
                }
            except json.JSONDecodeError:
                pass

        # JSON 파싱 실패 시 기본값 반환
        return {
            "name": f"{topic_input} 스터디",
            "intro": f"{topic_input} 학습을 위한 스터디입니다.",
            "description": f"{topic_input}에 대해 {duration_weeks}주간 함께 학습합니다.",
            "topic": topic_input,
            "format": "강의 학습",
            "difficulty": "INTERMEDIATE",
            "goal": f"{topic_input} 마스터하기",
            "textbook": "공식 문서 및 온라인 강의",
            "prerequisites": "기초 프로그래밍 지식",
            "process_detail": "매주 학습 후 토론 및 과제 수행",
            "schedule_suggestion": None,
            "curriculum": [],
            "reason": "JSON 파싱 실패로 기본값 반환",
            "tokens_used": tokens_used,
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/recommend-template/stream")
async def recommend_template_stream(request: TemplateRecommendRequest):
    """
    스트리밍 방식 스터디 템플릿 생성
    - SSE(Server-Sent Events) 형식으로 토큰 단위 전송
    - 완료 시 파싱된 JSON 결과 전송
    """
    global recommend_llm
    if recommend_llm is None:
        load_recommend_llm()
    if recommend_llm is None:
        raise HTTPException(status_code=503, detail="추천 모델이 로드되지 않았습니다")

    # 요청 파라미터 처리 (기존과 동일)
    topic_input = request.topic_input or "IT 스터디"
    duration_weeks = request.duration_weeks or 4
    total_sessions = request.total_sessions or duration_weeks
    user_tech = request.user_tech or []
    user_schedule = request.user_schedule or {}

    schedule_str = ""
    if user_schedule:
        days = list(user_schedule.keys())
        schedule_str = f"선호 요일: {', '.join(days)}"

    tech_str = ", ".join(user_tech) if user_tech else "없음"

    # topic/format 목록 (기존과 동일)
    topic_list = """
[알고리즘/코딩테스트]
- 백준
- 프로그래머스
- SWEA
- LeetCode
- 코딩테스트 대비
- 자료구조
- 알고리즘 이론

[CS 기초]
- 운영체제
- 네트워크
- 데이터베이스
- 컴퓨터구조
- 디자인패턴
- 시스템 설계

[프론트엔드]
- HTML/CSS
- JavaScript
- TypeScript
- React
- Vue
- Next.js
- 웹 접근성/성능

[백엔드]
- Java/Spring
- Python/Django
- Python/FastAPI
- Node.js/Express
- Go
- Kotlin
- API 설계

[DevOps/인프라]
- Docker
- Kubernetes
- CI/CD
- AWS
- GCP
- Linux
- 모니터링

[AI/데이터]
- 머신러닝 기초
- 딥러닝
- NLP
- 컴퓨터 비전
- MLOps
- 논문 리뷰

[모바일]
- Android (Kotlin)
- Android (Java)
- iOS (Swift)
- Flutter
- React Native
"""

    format_list = """
- 문제 풀이: 알고리즘/코딩테스트 문제를 함께 풀어보는 형식
- 독서/책 스터디: 기술 서적을 함께 읽고 토론하는 형식
- 강의 수강: 온라인 강의를 함께 수강하고 토론하는 형식
- 프로젝트: 실제 프로젝트를 함께 개발하는 형식
- 모의 면접: 기술 면접 대비 연습을 하는 형식
- 코드 리뷰: 서로의 코드를 리뷰하고 피드백하는 형식
- 발표/세미나: 각자 학습한 내용을 발표하는 형식
- 토론: 특정 주제에 대해 토론하는 형식
"""

    prompt = f"""<|im_start|>system
당신은 IT 스터디 계획을 작성하는 전문가입니다.
사용자의 학습 주제를 분석하여 체계적인 스터디 계획을 JSON 형식으로 작성합니다.

중요 규칙:
1. topic 필드는 반드시 아래 목록에서 정확히 일치하는 값을 선택하세요.
2. format 필드는 반드시 아래 목록에서 정확히 일치하는 값을 선택하세요.
3. 목록에 없는 값을 임의로 만들지 마세요.
4. JSON만 출력하고, 설명이나 부가 텍스트를 추가하지 마세요.
5. curriculum 배열은 반드시 요청된 총 회차 수만큼 생성하세요.

선택 가능한 topic 목록:
{topic_list}

선택 가능한 format 목록:
{format_list}
<|im_end|>
<|im_start|>user
사용자 입력 주제: {topic_input}
총 회차: {total_sessions}회
사용자 기술 스택: {tech_str}
{schedule_str}

위 정보를 바탕으로 스터디 계획을 JSON으로 작성해주세요.
topic은 위 목록에서 가장 적합한 것을 정확히 선택하고,
format도 주제에 맞는 것을 위 목록에서 정확히 선택해주세요.
curriculum은 반드시 {total_sessions}개의 회차를 생성해주세요.

예를 들어:
- "스프링 학습" → topic: "Java/Spring"
- "리액트 공부" → topic: "React"
- "알고리즘 문제풀이" → topic: "알고리즘 이론" 또는 "백준", format: "문제 풀이"

출력할 JSON 형식:
{{
  "name": "스터디 제목",
  "intro": "한 줄 소개 (15자 내외)",
  "description": "스터디 상세 설명 (2-3문장)",
  "topic": "목록에서 정확히 선택",
  "format": "목록에서 정확히 선택",
  "difficulty": "BEGINNER 또는 INTERMEDIATE 또는 ADVANCED",
  "goal": "스터디 목표",
  "textbook": "추천 교재 또는 참고 자료",
  "prerequisites": "선수 지식 요건",
  "process_detail": "스터디 진행 방식 설명",
  "curriculum": [
    {{"session": 1, "title": "1회차 제목", "description": "학습 내용"}},
    {{"session": 2, "title": "2회차 제목", "description": "학습 내용"}},
    ...
    {{"session": {total_sessions}, "title": "{total_sessions}회차 제목", "description": "학습 내용"}}
  ]
}}
<|im_end|>
<|im_start|>assistant
"""

    import re

    async def generate_stream():
        """토큰 단위 스트리밍 생성기"""
        full_response = ""
        tokens_used = 0

        try:
            # stream=True로 토큰 단위 생성
            for chunk in recommend_llm(
                prompt,
                max_tokens=2048,
                temperature=0.7,
                stop=["<|im_end|>", "<|im_start|>"],
                echo=False,
                stream=True,
            ):
                token = chunk["choices"][0]["text"]
                full_response += token

                # SSE 형식으로 토큰 전송
                yield f"event: token\ndata: {json.dumps({'token': token}, ensure_ascii=False)}\n\n"

            # JSON 파싱 시도
            json_match = re.search(r'\{[\s\S]*\}', full_response)
            if json_match:
                try:
                    result = json.loads(json_match.group())
                    final_result = {
                        "name": result.get("name", f"{topic_input} 스터디"),
                        "intro": result.get("intro", f"{topic_input} 학습을 위한 스터디"),
                        "description": result.get("description", ""),
                        "topic": result.get("topic", topic_input),
                        "format": result.get("format", "강의 수강"),
                        "difficulty": result.get("difficulty", "INTERMEDIATE"),
                        "goal": result.get("goal", ""),
                        "textbook": result.get("textbook", ""),
                        "prerequisites": result.get("prerequisites", ""),
                        "process_detail": result.get("process_detail", ""),
                        "schedule_suggestion": None,
                        "curriculum": result.get("curriculum", []),
                        "reason": None,
                        "tokens_used": 0,
                    }
                    yield f"event: complete\ndata: {json.dumps(final_result, ensure_ascii=False)}\n\n"
                except json.JSONDecodeError:
                    # JSON 파싱 실패 시 기본값
                    fallback = {
                        "name": f"{topic_input} 스터디",
                        "intro": f"{topic_input} 학습을 위한 스터디입니다.",
                        "description": f"{topic_input}에 대해 {duration_weeks}주간 함께 학습합니다.",
                        "topic": topic_input,
                        "format": "강의 수강",
                        "difficulty": "INTERMEDIATE",
                        "goal": f"{topic_input} 마스터하기",
                        "textbook": "공식 문서 및 온라인 강의",
                        "prerequisites": "기초 프로그래밍 지식",
                        "process_detail": "매주 학습 후 토론 및 과제 수행",
                        "schedule_suggestion": None,
                        "curriculum": [],
                        "reason": "JSON 파싱 실패",
                        "tokens_used": 0,
                    }
                    yield f"event: complete\ndata: {json.dumps(fallback, ensure_ascii=False)}\n\n"
            else:
                # JSON 블록 없음
                fallback = {
                    "name": f"{topic_input} 스터디",
                    "error": "JSON 형식 응답 없음",
                    "raw_response": full_response[:500],
                }
                yield f"event: complete\ndata: {json.dumps(fallback, ensure_ascii=False)}\n\n"

        except Exception as e:
            yield f"event: error\ndata: {json.dumps({'error': str(e)}, ensure_ascii=False)}\n\n"

    return StreamingResponse(
        generate_stream(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",  # Nginx 버퍼링 비활성화
        }
    )


@app.post("/api/action-items")
async def extract_action_items(request: SummarizeRequest):
    """액션 아이템 추출 API"""
    if llm is None:
        raise HTTPException(status_code=503, detail="LLM 모델이 로드되지 않았습니다")

    prompt = f"""<|im_start|>system
당신은 IT 스터디 회의록에서 액션 아이템(할 일)을 추출하는 전문가입니다.<|im_end|>
<|im_start|>user
다음 회의 내용에서 액션 아이템(할 일, 다음 과제)을 추출해주세요.

회의 내용:
{request.transcript}<|im_end|>
<|im_start|>assistant
"""

    try:
        output = llm(
            prompt,
            max_tokens=request.max_tokens,
            temperature=request.temperature,
            stop=["<|im_end|>", "<|im_start|>"],
            echo=False,
        )

        return {
            "action_items": output["choices"][0]["text"].strip(),
            "tokens_used": output["usage"]["total_tokens"],
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/process-meeting")
async def process_meeting(
    background_tasks: BackgroundTasks,
    file: UploadFile = File(...),
    generate_quiz: bool = True,
):
    """
    회의 전체 처리 파이프라인 (비동기)
    1. STT (음성 → 텍스트)
    2. 요약 생성
    3. 퀴즈 생성 (선택)
    """
    if whisper_model is None:
        raise HTTPException(status_code=503, detail="Whisper 모델이 로드되지 않았습니다")
    if llm is None:
        raise HTTPException(status_code=503, detail="LLM 모델이 로드되지 않았습니다")

    # 임시 파일로 저장
    suffix = Path(file.filename).suffix if file.filename else ".wav"
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        content = await file.read()
        tmp.write(content)
        tmp_path = tmp.name

    # Job 생성
    job_id = str(uuid.uuid4())
    jobs[job_id] = {
        "status": "pending",
        "result": None,
        "error": None,
        "generate_quiz": generate_quiz,
    }

    # 백그라운드 작업 등록
    background_tasks.add_task(process_meeting_job, job_id, tmp_path, generate_quiz)

    return JobResponse(
        job_id=job_id,
        status="pending",
        message="회의 처리 작업이 등록되었습니다.",
    )


def process_meeting_job(job_id: str, file_path: str, generate_quiz_flag: bool):
    """회의 전체 처리 백그라운드 작업"""
    preprocessed_path = None
    try:
        jobs[job_id]["status"] = "processing"
        
        # 0. 음성 전처리
        preprocessed_path = preprocess_audio(file_path)

        # 1. STT
        segments, info = whisper_model.transcribe(
            preprocessed_path,
            language="ko",
            beam_size=5,
            vad_filter=True,
        )

        segment_list = []
        full_text = []
        for seg in segments:
            segment_list.append({
                "start": seg.start,
                "end": seg.end,
                "text": seg.text.strip(),
            })
            full_text.append(seg.text.strip())

        transcript = " ".join(full_text)

        # 2. 요약
        summary_prompt = f"""<|im_start|>system
당신은 IT 스터디 회의록을 요약하는 전문가입니다.<|im_end|>
<|im_start|>user
다음 IT 스터디 회의 내용을 요약해주세요.

회의 내용:
{transcript}<|im_end|>
<|im_start|>assistant
"""

        summary_output = llm(
            summary_prompt,
            max_tokens=512,
            temperature=0.7,
            stop=["<|im_end|>", "<|im_start|>"],
            echo=False,
        )
        summary = summary_output["choices"][0]["text"].strip()

        result = {
            "transcript": transcript,
            "segments": segment_list,
            "duration": info.duration,
            "summary": summary,
        }

        # 3. 퀴즈 생성 (선택)
        if generate_quiz_flag:
            quiz_prompt = f"""<|im_start|>system
당신은 IT 스터디 내용을 바탕으로 복습 퀴즈를 생성하는 전문가입니다.<|im_end|>
<|im_start|>user
다음 스터디 요약 내용을 바탕으로 복습 퀴즈 5문제를 생성해주세요.

스터디 요약:
{summary}<|im_end|>
<|im_start|>assistant
"""

            quiz_output = llm(
                quiz_prompt,
                max_tokens=1024,
                temperature=0.7,
                stop=["<|im_end|>", "<|im_start|>"],
                echo=False,
            )
            result["quiz"] = quiz_output["choices"][0]["text"].strip()

        jobs[job_id]["status"] = "completed"
        jobs[job_id]["result"] = result

    except Exception as e:
        jobs[job_id]["status"] = "failed"
        jobs[job_id]["error"] = str(e)

    finally:
        if preprocessed_path:
            cleanup_preprocessed(file_path, preprocessed_path)
        if os.path.exists(file_path):
            os.unlink(file_path)


# ===== Claude API 설정 =====
CLAUDE_API_KEY = os.getenv("CLAUDE_API_KEY", "")
CLAUDE_MODEL = os.getenv("CLAUDE_MODEL", "claude-3-haiku-20240307")


def call_claude(prompt: str, max_tokens: int = 2048) -> str:
    """Claude API 호출"""
    if not CLAUDE_API_KEY:
        return None

    import httpx

    response = httpx.post(
        "https://api.anthropic.com/v1/messages",
        headers={
            "x-api-key": CLAUDE_API_KEY,
            "anthropic-version": "2023-06-01",
            "content-type": "application/json",
        },
        json={
            "model": CLAUDE_MODEL,
            "max_tokens": max_tokens,
            "messages": [{"role": "user", "content": prompt}],
        },
        timeout=120.0,
    )

    if response.status_code == 200:
        return response.json()["content"][0]["text"]
    else:
        print(f"[WARNING] Claude API 오류: {response.status_code} - {response.text}")
        return None


# ===== 회의 전체 처리 (Claude 검증 포함) =====

class MeetingFullRequest(BaseModel):
    meeting_id: int
    user_ids: Optional[list] = []  # 화자별 음성의 user_id 목록


class MeetingFullResponse(BaseModel):
    transcript: str
    summary: str
    keywords: list
    action_items: list  # [{"user_id": 1, "content": "..."}, ...]
    quiz: Optional[str] = None


@app.post("/api/process-meeting-full")
async def process_meeting_full(
    background_tasks: BackgroundTasks,
    mixed_audio: UploadFile = File(...),
    individual_audios: list[UploadFile] = File(default=[]),
    user_ids: str = "",  # comma-separated user IDs
    generate_quiz: bool = True,
):
    """
    회의 전체 처리 파이프라인 (Claude 검증 포함)
    1. 전체 음성 STT
    2. 로컬 LLM 요약
    3. Claude로 요약 검증/보완 + 키워드 추출
    4. 화자별 STT + 액션아이템 추천
    5. 복습 퀴즈 생성
    """
    if whisper_model is None:
        raise HTTPException(status_code=503, detail="Whisper 모델이 로드되지 않았습니다")
    if llm is None:
        raise HTTPException(status_code=503, detail="LLM 모델이 로드되지 않았습니다")

    # user_ids 파싱
    user_id_list = [int(x.strip()) for x in user_ids.split(",") if x.strip().isdigit()]

    # 임시 파일로 저장 - 전체 음성
    suffix = Path(mixed_audio.filename).suffix if mixed_audio.filename else ".webm"
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        content = await mixed_audio.read()
        tmp.write(content)
        mixed_path = tmp.name

    # 화자별 음성 저장
    individual_paths = []
    for i, audio in enumerate(individual_audios):
        suffix = Path(audio.filename).suffix if audio.filename else ".webm"
        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
            content = await audio.read()
            tmp.write(content)
            individual_paths.append({
                "path": tmp.name,
                "user_id": user_id_list[i] if i < len(user_id_list) else None
            })

    # Job 생성
    job_id = str(uuid.uuid4())
    jobs[job_id] = {
        "status": "pending",
        "result": None,
        "error": None,
    }

    # 백그라운드 작업 등록
    background_tasks.add_task(
        process_meeting_full_job,
        job_id,
        mixed_path,
        individual_paths,
        generate_quiz
    )

    return JobResponse(
        job_id=job_id,
        status="pending",
        message="회의 전체 처리 작업이 등록되었습니다.",
    )


def process_meeting_full_job(job_id: str, mixed_path: str, individual_paths: list, generate_quiz: bool):
    """회의 전체 처리 백그라운드 작업 (Claude 1회 통합 호출)"""
    preprocessed_mixed = None
    preprocessed_individuals = []
    try:
        jobs[job_id]["status"] = "processing"

        import json
        import re

        # 0. 음성 전처리 (전체 + 화자별)
        preprocessed_mixed = preprocess_audio(mixed_path)
        for item in individual_paths:
            preprocessed_path = preprocess_audio(item["path"])
            preprocessed_individuals.append({
                "original": item["path"],
                "preprocessed": preprocessed_path,
                "user_id": item.get("user_id")
            })

        # 1. 전체 음성 STT
        segments, info = whisper_model.transcribe(
            preprocessed_mixed,
            language="ko",
            beam_size=5,
            vad_filter=True,
        )
        full_text = " ".join([seg.text.strip() for seg in segments])

        # 2. 화자별 STT (먼저 모두 수행)
        speaker_texts = []
        for item in preprocessed_individuals:
            if not item["user_id"]:
                continue
            try:
                segments, _ = whisper_model.transcribe(
                    item["preprocessed"],
                    language="ko",
                    beam_size=5,
                    vad_filter=True,
                )
                user_text = " ".join([seg.text.strip() for seg in segments])
                if user_text.strip():
                    speaker_texts.append({
                        "user_id": item["user_id"],
                        "text": user_text[:1500]
                    })
            except Exception as e:
                print(f"[WARNING] 화자별 STT 실패: {e}")

        # 3. 로컬 LLM 요약 (파인튜닝 형식) - 원본 대신 요약본만 Claude에 전달
        summary_prompt = f"""<|im_start|>system
당신은 IT 스터디 회의록을 요약하는 전문가입니다. 다음 형식으로 정리해주세요:

## 요약
[2-3문장 핵심 요약]

## 다룬 내용
- [토픽별 요점]

## 액션 아이템
- [할 일]

## 키워드
[쉼표로 구분]
<|im_end|>
<|im_start|>user
다음 회의 내용을 분석하여 정리해주세요:

{full_text[:4000]}
<|im_end|>
<|im_start|>assistant
"""
        summary_output = llm(summary_prompt, max_tokens=1200, temperature=0.3,
                             stop=["<|im_end|>", "<|im_start|>"], echo=False)
        local_summary = summary_output["choices"][0]["text"].strip()

        # 4. Claude 통합 호출 (8B 요약본만 전달, 원본 미전송)
        speaker_info = ""
        if speaker_texts:
            # 화자 ID 목록만 전달 (발언 원본은 전달하지 않음)
            speaker_ids_list = [s['user_id'] for s in speaker_texts]
            speaker_info = f"\n\n## 참여자 ID 목록: {speaker_ids_list}"

        quiz_instruction = ""
        if generate_quiz:
            quiz_instruction = """
5. **퀴즈**: 스터디 내용 복습을 위한 퀴즈를 5문제 이상 생성해주세요.
   - MULTIPLE_CHOICE(객관식)와 SHORT_ANSWER(단답형) 혼합
   - 난이도: EASY, MEDIUM, HARD 섞어서
   - 각 문제에 정답과 해설 포함"""

        claude_prompt = f"""다음은 IT 스터디 회의 분석 결과입니다. 검토하고 보완해주세요.

## 회의 요약:
{local_summary}
{speaker_info}

---

위 분석을 바탕으로 다음을 수행해주세요:

1. **요약 검토 및 보완**: 위 요약을 검토하고, 더 풍부하고 명확하게 보완해주세요 (5-7문장).
   - 핵심 논의 사항을 빠짐없이 포함
   - 결론이나 합의된 사항 강조

2. **학습 피드백**: 회의에서 다룬 기술/개념에 대해 학습에 도움이 되는 피드백을 제공해주세요.
   - 핵심 개념의 배경 지식 및 원리 설명
   - 심화 학습을 위한 관련 주제 추천
   - 실무 적용 팁이나 주의사항

3. **키워드**: 핵심 키워드 5-7개를 추출해주세요.
{quiz_instruction}

---

반드시 아래 JSON 형식으로만 응답해주세요:
{{
  "summary": "보완된 풍부한 요약 (5-7문장)",
  "feedback": "학습 피드백 (배경지식, 심화주제, 실무팁 등을 풍부하게)",
  "keywords": ["키워드1", "키워드2", ...],
  "quiz": [
    {{
      "question": "문제 내용",
      "type": "객관식",
      "options": ["A. 보기1", "B. 보기2", "C. 보기3", "D. 보기4"],
      "answer": "A",
      "explanation": "해설"
    }}
  ]
}}
"""

        claude_response = call_claude(claude_prompt, max_tokens=4096)

        # 기본값: 8B 요약 결과 사용
        final_summary = local_summary
        feedback = ""
        keywords = []
        quiz = None
        action_items = []  # 액션아이템은 8B 요약본에 포함됨

        if claude_response:
            json_match = re.search(r'\{[\s\S]*\}', claude_response)
            if json_match:
                try:
                    result = json.loads(json_match.group())
                    final_summary = result.get("summary", local_summary)
                    feedback = result.get("feedback", "")
                    keywords = result.get("keywords", [])
                    if generate_quiz and result.get("quiz"):
                        quiz = json.dumps(result["quiz"], ensure_ascii=False)
                except Exception as e:
                    print(f"[WARNING] Claude 응답 파싱 실패: {e}")

        # 요약에 학습 피드백 추가
        if feedback:
            final_summary = f"{final_summary}\n\n📚 학습 피드백:\n{feedback}"

        jobs[job_id]["status"] = "completed"
        jobs[job_id]["result"] = {
            "transcript": full_text,
            "summary": final_summary,
            "keywords": keywords,
            "action_items": action_items,
            "quiz": quiz,
        }

    except Exception as e:
        jobs[job_id]["status"] = "failed"
        jobs[job_id]["error"] = str(e)
        print(f"[ERROR] 회의 처리 실패: {e}")

    finally:
        # 임시 파일 정리 (전처리 파일 포함)
        if preprocessed_mixed:
            cleanup_preprocessed(mixed_path, preprocessed_mixed)
        if os.path.exists(mixed_path):
            os.unlink(mixed_path)
        for item in preprocessed_individuals:
            cleanup_preprocessed(item["original"], item["preprocessed"])
        for item in individual_paths:
            if os.path.exists(item["path"]):
                os.unlink(item["path"])


# ===== 실시간 발화 세그먼트 처리 (SFU → AI → 백엔드) =====

class SpeechSegmentRequest(BaseModel):
    meeting_id: int
    user_id: str  # 발화자 ID (socket.id 또는 displayName)
    timestamp: float  # 발화 시작 시간 (Unix timestamp ms)
    duration_ms: int  # 발화 길이 (ms)
    file_path: str  # 세그먼트 파일 경로


class SpeechSegmentResponse(BaseModel):
    status: str
    text: str = ""
    message: str = ""


@app.post("/api/process-speech-segment", response_model=SpeechSegmentResponse)
async def process_speech_segment(request: SpeechSegmentRequest, background_tasks: BackgroundTasks):
    """
    실시간 발화 세그먼트 처리
    1. 오디오 전처리 (경량화)
    2. Whisper STT
    3. 백엔드로 결과 전송 (WebSocket 브로드캐스트)

    Note: 빠른 응답을 위해 백엔드 전송은 백그라운드로 처리
    """
    if whisper_model is None:
        raise HTTPException(status_code=503, detail="Whisper 모델이 로드되지 않았습니다")

    file_path = request.file_path
    meeting_id = request.meeting_id
    user_id = request.user_id
    timestamp = request.timestamp
    duration_ms = request.duration_ms

    # 경로 보안 검증
    SFU_UPLOADS_PATH = os.getenv("SFU_UPLOADS_PATH", "/app/uploads")
    abs_path = os.path.abspath(file_path)
    if not abs_path.startswith(os.path.abspath(SFU_UPLOADS_PATH)):
        raise HTTPException(status_code=400, detail=f"Invalid file path")

    if not os.path.exists(abs_path):
        raise HTTPException(status_code=404, detail=f"File not found: {file_path}")

    print(f"[SPEECH] 발화 세그먼트 처리 시작: meetingId={meeting_id}, userId={user_id}, duration={duration_ms}ms")

    preprocessed_path = None
    try:
        # 1. 경량 오디오 전처리 (실시간 최적화)
        preprocessed_path = preprocess_audio_realtime(abs_path)
        process_path = preprocessed_path if preprocessed_path != abs_path else abs_path

        # 2. Whisper STT (빠른 설정)
        segments, info = whisper_model.transcribe(
            process_path,
            language="ko",
            beam_size=3,  # 속도를 위해 낮춤 (기본 5)
            vad_filter=True,
            condition_on_previous_text=False,  # 독립 세그먼트이므로 비활성화
        )

        # 결과 조합
        full_text = " ".join([seg.text.strip() for seg in segments])

        print(f"[SPEECH] STT 완료: meetingId={meeting_id}, userId={user_id}, text='{full_text[:50]}...'")

        # 3. 백엔드로 결과 전송 (비동기)
        if full_text.strip():
            background_tasks.add_task(
                send_speech_to_backend,
                meeting_id=meeting_id,
                user_id=user_id,
                timestamp=timestamp,
                duration_ms=duration_ms,
                text=full_text
            )

        return SpeechSegmentResponse(
            status="success",
            text=full_text,
            message="STT 처리 완료"
        )

    except Exception as e:
        print(f"[SPEECH] STT 처리 실패: {e}")
        return SpeechSegmentResponse(
            status="error",
            text="",
            message=str(e)
        )

    finally:
        # 임시 파일 정리
        if preprocessed_path:
            cleanup_preprocessed(abs_path, preprocessed_path)
        # 원본 세그먼트 파일도 삭제 (SFU에서 전송 후 불필요)
        try:
            if os.path.exists(abs_path):
                os.unlink(abs_path)
        except Exception:
            pass


def preprocess_audio_realtime(input_path: str) -> str:
    """
    실시간 세그먼트용 경량 전처리
    - 전체 전처리보다 빠른 버전
    - 기본 노이즈 제거 + 정규화만 수행
    """
    try:
        output_path = input_path.rsplit(".", 1)[0] + f"_rt_{uuid.uuid4().hex[:6]}.wav"

        cmd = [
            "ffmpeg", "-i", input_path,
            "-af", "highpass=f=100,lowpass=f=7000,loudnorm",  # 경량 필터
            "-c:a", "pcm_s16le",
            "-ar", "16000",
            "-ac", "1",
            "-y",
            output_path
        ]

        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=30  # 실시간이므로 타임아웃 짧게
        )

        if result.returncode == 0:
            return output_path
        else:
            print(f"[PREPROCESS-RT] ffmpeg 실패: {result.stderr[:100]}")
            return input_path

    except Exception as e:
        print(f"[PREPROCESS-RT] 에러: {e}")
        return input_path


async def send_speech_to_backend(meeting_id: int, user_id: str, timestamp: float, duration_ms: int, text: str):
    """
    백엔드로 STT 결과 전송
    - 백엔드에서 WebSocket으로 클라이언트에 브로드캐스트
    """
    import httpx

    BACKEND_URL = os.getenv("BACKEND_URL", "http://squiz-backend:8080")
    url = f"{BACKEND_URL}/api/internal/meetings/{meeting_id}/speech-segments"

    payload = {
        "userId": user_id,
        "timestamp": int(timestamp),
        "durationMs": duration_ms,
        "text": text
    }

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.post(url, json=payload)

        if response.status_code in (200, 201):
            print(f"[SPEECH] 백엔드 전송 완료: meetingId={meeting_id}")
        else:
            print(f"[SPEECH] 백엔드 전송 실패: status={response.status_code}")

    except Exception as e:
        print(f"[SPEECH] 백엔드 전송 오류: {e}")


# ===== 녹음 파일 업로드 (SFU → AI → 백엔드) =====

BACKEND_URL = os.getenv("BACKEND_URL", "http://squiz-backend:8080")
SFU_UPLOADS_PATH = os.getenv("SFU_UPLOADS_PATH", "/app/uploads")


class RecordingUploadRequest(BaseModel):
    meeting_id: int
    file_path: str  # SFU 서버 내 파일 경로


class RecordingUploadResponse(BaseModel):
    status: str
    message: str
    recording_url: Optional[str] = None


@app.post("/api/upload-recording", response_model=RecordingUploadResponse)
async def upload_recording(request: RecordingUploadRequest):
    """
    SFU 녹음 파일을 전처리 후 백엔드로 업로드
    1. 파일 경로 검증
    2. 오디오 전처리 (노이즈 제거, 정규화)
    3. 백엔드 내부 API로 업로드
    """
    import httpx

    meeting_id = request.meeting_id
    file_path = request.file_path

    # 경로 보안 검증 (SFU uploads 디렉토리 내부만 허용)
    abs_path = os.path.abspath(file_path)
    if not abs_path.startswith(os.path.abspath(SFU_UPLOADS_PATH)):
        raise HTTPException(status_code=400, detail=f"Invalid file path: must be within {SFU_UPLOADS_PATH}")

    if not os.path.exists(abs_path):
        raise HTTPException(status_code=404, detail=f"File not found: {file_path}")

    print(f"[UPLOAD] 녹음 파일 업로드 시작: meetingId={meeting_id}, path={file_path}")

    preprocessed_path = None
    try:
        # 1. 오디오 전처리
        preprocessed_path = preprocess_audio(abs_path)
        upload_path = preprocessed_path if preprocessed_path != abs_path else abs_path

        print(f"[UPLOAD] 전처리 완료: {upload_path}")

        # 2. 백엔드로 업로드
        upload_url = f"{BACKEND_URL}/api/internal/meetings/{meeting_id}/recording/video"

        with open(upload_path, "rb") as f:
            files = {"video": ("voice.webm", f, "video/webm")}

            async with httpx.AsyncClient(timeout=120.0) as client:
                response = await client.post(upload_url, files=files)

        if response.status_code in (200, 201):
            result = response.json()
            recording_url = result.get("data", {}).get("recordingUrl")
            print(f"[UPLOAD] 업로드 성공: meetingId={meeting_id}, url={recording_url}")
            return RecordingUploadResponse(
                status="success",
                message="녹음 파일 업로드 완료",
                recording_url=recording_url
            )
        else:
            print(f"[UPLOAD] 업로드 실패: status={response.status_code}, body={response.text}")
            raise HTTPException(
                status_code=response.status_code,
                detail=f"Backend upload failed: {response.text}"
            )

    except httpx.RequestError as e:
        print(f"[UPLOAD] 네트워크 오류: {e}")
        raise HTTPException(status_code=502, detail=f"Backend connection failed: {str(e)}")

    finally:
        # 전처리 임시 파일 정리
        if preprocessed_path:
            cleanup_preprocessed(abs_path, preprocessed_path)


# ===== 실시간 STT transcript 기반 요약 (STT 스킵) =====

class TranscriptSummarizeRequest(BaseModel):
    transcript: str  # 이미 변환된 transcript 텍스트 (화자: 텍스트 형식)
    speaker_ids: Optional[List[int]] = None  # 화자 ID 목록 (액션아이템용)
    generate_quiz: bool = True


class TranscriptSummarizeResponse(BaseModel):
    status: str
    job_id: Optional[str] = None
    message: str = ""


@app.post("/api/summarize-transcript", response_model=TranscriptSummarizeResponse)
async def summarize_transcript(request: TranscriptSummarizeRequest, background_tasks: BackgroundTasks):
    """
    실시간 STT로 수집된 transcript를 바로 요약
    STT 단계를 건너뛰고 Claude API로 직접 요약 요청
    """
    if not request.transcript or len(request.transcript.strip()) < 50:
        return TranscriptSummarizeResponse(
            status="error",
            message="Transcript too short (minimum 50 characters)"
        )

    job_id = str(uuid.uuid4())
    jobs[job_id] = {"status": "processing", "result": None, "error": None}

    background_tasks.add_task(
        process_transcript_summary,
        job_id,
        request.transcript,
        request.speaker_ids or [],
        request.generate_quiz
    )

    return TranscriptSummarizeResponse(
        status="processing",
        job_id=job_id,
        message="Transcript summarization started"
    )


def process_transcript_summary(job_id: str, transcript: str, speaker_ids: List[int], generate_quiz: bool):
    """
    Transcript 기반 요약 처리 (백그라운드)
    """
    try:
        print(f"[TRANSCRIPT] 요약 시작: job_id={job_id}, length={len(transcript)}")

        # 1. 로컬 8B LLM으로 요약 + 키워드 + 액션아이템 추출 (파인튜닝 형식)
        summary_prompt = f"""<|im_start|>system
당신은 IT 스터디 회의록을 요약하는 전문가입니다. 다음 형식으로 정리해주세요:

## 요약
[2-3문장 핵심 요약]

## 다룬 내용
- [토픽별 요점]

## 액션 아이템
- [할 일]

## 키워드
[쉼표로 구분]
<|im_end|>
<|im_start|>user
다음 회의 내용을 분석하여 정리해주세요:

{transcript[:4000]}
<|im_end|>
<|im_start|>assistant
"""
        summary_output = llm(summary_prompt, max_tokens=1200, temperature=0.3,
                             stop=["<|im_end|>", "<|im_start|>"], echo=False)
        local_summary = summary_output["choices"][0]["text"].strip()

        print(f"[TRANSCRIPT] 8B 요약 완료: {len(local_summary)} chars")

        # 2. Claude 호출 (8B 요약본 기반 검토/보완 + 보충설명 + 퀴즈)
        quiz_instruction = ""
        if generate_quiz:
            quiz_instruction = """
4. **퀴즈**: 스터디 내용 복습을 위한 퀴즈를 5문제 이상 생성해주세요.
   - MULTIPLE_CHOICE(객관식)와 SHORT_ANSWER(단답형) 혼합
   - 난이도: EASY, MEDIUM, HARD 섞어서
   - 각 문제에 정답과 해설 포함"""

        claude_prompt = f"""다음은 IT 스터디 회의 분석 결과입니다. 검토하고 보완해주세요.

{local_summary}

---

위 분석을 바탕으로 다음을 수행해주세요:

1. **요약 검토 및 보완**: 위 요약을 검토하고, 더 풍부하고 명확하게 보완해주세요 (5-7문장).
   - 핵심 논의 사항을 빠짐없이 포함
   - 결론이나 합의된 사항 강조

2. **학습 피드백**: 회의에서 다룬 기술/개념에 대해 학습에 도움이 되는 피드백을 제공해주세요.
   - 핵심 개념의 배경 지식 및 원리 설명
   - 심화 학습을 위한 관련 주제 추천
   - 실무 적용 팁이나 주의사항

3. **키워드 보완**: 위에서 추출된 키워드를 검토하고, 누락된 중요 키워드가 있다면 추가해주세요.
{quiz_instruction}

---

반드시 아래 JSON 형식으로만 응답해주세요:
{{
  "summary": "보완된 풍부한 요약 (5-7문장)",
  "feedback": "학습 피드백 (배경지식, 심화주제, 실무팁 등을 풍부하게)",
  "keywords": ["키워드1", "키워드2", ...],
  "quiz": [
    {{
      "question": "문제 내용",
      "type": "객관식",
      "options": ["A. 보기1", "B. 보기2", "C. 보기3", "D. 보기4"],
      "answer": "A",
      "explanation": "해설"
    }}
  ]
}}
"""

        claude_response = call_claude(claude_prompt, max_tokens=4096)

        # 기본값: 8B 요약 결과 사용
        final_summary = local_summary
        feedback = ""
        keywords = []
        quiz = None
        action_items = []  # 액션아이템은 8B 요약본에 포함됨

        if claude_response:
            json_match = re.search(r'\{[\s\S]*\}', claude_response)
            if json_match:
                try:
                    result = json.loads(json_match.group())
                    # Claude가 보완한 요약 사용
                    final_summary = result.get("summary", local_summary)
                    feedback = result.get("feedback", "")
                    keywords = result.get("keywords", [])
                    if generate_quiz and result.get("quiz"):
                        quiz = json.dumps(result["quiz"], ensure_ascii=False)
                except Exception as e:
                    print(f"[TRANSCRIPT] Claude 응답 파싱 실패: {e}")

        # 요약에 학습 피드백 추가
        if feedback:
            final_summary = f"{final_summary}\n\n📚 학습 피드백:\n{feedback}"

        jobs[job_id]["status"] = "completed"
        jobs[job_id]["result"] = {
            "transcript": transcript,
            "summary": final_summary,
            "keywords": keywords,
            "action_items": action_items,
            "quiz": quiz,
        }
        print(f"[TRANSCRIPT] 요약 완료: job_id={job_id}")

    except Exception as e:
        jobs[job_id]["status"] = "failed"
        jobs[job_id]["error"] = str(e)
        print(f"[TRANSCRIPT] 요약 실패: {e}")


if __name__ == "__main__":
    print("=" * 60)
    print("Squiz AI 추론 서버")
    print("=" * 60)
    print(f"LLM Model: {MODEL_PATH}")
    print(f"Whisper Model: {WHISPER_MODEL}")
    print(f"GPU Layers: {N_GPU_LAYERS}")
    print(f"Context Length: {N_CTX}")
    print(f"Server: http://{HOST}:{PORT}")
    print(f"Backend URL: {BACKEND_URL}")
    print("=" * 60)

    uvicorn.run(app, host=HOST, port=PORT)
