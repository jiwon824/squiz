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
from pathlib import Path
from typing import Optional
from fastapi import FastAPI, HTTPException, UploadFile, File, BackgroundTasks
from pydantic import BaseModel
from contextlib import asynccontextmanager
import uvicorn

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

        # large-v3 모델은 128 mel bins 사용 (기본값 80과 다름)
        # CT2 변환된 파인튜닝 모델의 경우 num_mels=128 필요
        is_large_v3 = "large-v3" in WHISPER_MODEL or "whisper-it" in WHISPER_MODEL

        whisper_model = WhisperModel(
            WHISPER_MODEL,
            device=WHISPER_DEVICE,
            compute_type=compute_type,
            num_workers=2,
            feature_size=128 if is_large_v3 else 80,  # large-v3: 128, 기타: 80
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

    try:
        # Whisper 추론
        segments, info = whisper_model.transcribe(
            tmp_path,
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
    try:
        jobs[job_id]["status"] = "processing"

        segments, info = whisper_model.transcribe(
            file_path,
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
    user_tech = request.user_tech or []
    user_schedule = request.user_schedule or {}

    # 스케줄 문자열 생성
    schedule_str = ""
    if user_schedule:
        days = list(user_schedule.keys())
        schedule_str = f"선호 요일: {', '.join(days)}"

    tech_str = ", ".join(user_tech) if user_tech else "없음"

    prompt = f"""<|im_start|>system
당신은 IT 스터디 계획을 설계하는 전문가입니다. 사용자가 입력한 주제를 기반으로 스터디 계획을 생성합니다.
반드시 JSON 형식으로만 응답하세요.<|im_end|>
<|im_start|>user
다음 조건으로 스터디 계획을 생성해주세요:

**사용자 입력 주제**: {topic_input}
**기간**: {duration_weeks}주
**사용자 기술스택**: {tech_str}
**{schedule_str}**

아래 JSON 형식으로 응답해주세요:
{{
  "name": "스터디 제목 (사용자 입력 주제 반영)",
  "intro": "한줄 소개",
  "description": "상세 설명 (2-3문장)",
  "topic": "주제 카테고리 (예: Python, React, Docker, 알고리즘 등)",
  "format": "스터디 형식 (예: 프로젝트, 강의 학습, 문제 풀이, 책 스터디)",
  "difficulty": "BEGINNER 또는 INTERMEDIATE 또는 ADVANCED",
  "goal": "스터디 목표",
  "textbook": "예: 공식 문서, 추천 강의, 교재명",
  "prerequisites": "선수 지식",
  "process_detail": "진행 방식 상세",
  "curriculum": [
    {{"week": 1, "title": "1주차 제목", "description": "학습 내용", "learning_goals": ["목표1"], "assignments": ["과제1"]}},
    {{"week": 2, "title": "2주차 제목", "description": "학습 내용", "learning_goals": ["목표1"], "assignments": ["과제1"]}}
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
    try:
        jobs[job_id]["status"] = "processing"

        # 1. STT
        segments, info = whisper_model.transcribe(
            file_path,
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
        if os.path.exists(file_path):
            os.unlink(file_path)


if __name__ == "__main__":
    print("=" * 60)
    print("Squiz AI 추론 서버")
    print("=" * 60)
    print(f"LLM Model: {MODEL_PATH}")
    print(f"Whisper Model: {WHISPER_MODEL}")
    print(f"GPU Layers: {N_GPU_LAYERS}")
    print(f"Context Length: {N_CTX}")
    print(f"Server: http://{HOST}:{PORT}")
    print("=" * 60)

    uvicorn.run(app, host=HOST, port=PORT)
