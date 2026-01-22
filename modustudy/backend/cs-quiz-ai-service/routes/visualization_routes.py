"""
임베딩 시각화 관련 라우트
3D 공간에서 단어들의 위치를 시각화하기 위한 좌표 반환
"""
from flask import Blueprint, request, jsonify
from services import AIService
from sklearn.decomposition import PCA
import numpy as np

visualization_bp = Blueprint('visualization', __name__, url_prefix='/api')

ai_service = AIService()


@visualization_bp.route('/embedding-3d', methods=['POST'])
def get_embedding_3d():
    """
    단어들의 임베딩을 3D 좌표로 변환하여 반환
    
    Request Body:
    {
        "userWord": "사용자 입력 단어",
        "answerWord": "정답 단어",
        "referenceWords": ["참조용 단어들"] (선택사항)
    }
    
    Response:
    {
        "points": [
            {"word": "단어", "x": 0.5, "y": 0.3, "z": 0.1, "type": "answer"},
            {"word": "단어2", "x": 0.2, "y": 0.4, "z": 0.6, "type": "user"},
            ...
        ],
        "similarity": 0.85,
        "distance": 0.15
    }
    """
    try:
        data = request.json
        
        if not data:
            return jsonify({"error": "JSON 데이터가 필요합니다"}), 400
        
        user_word = data.get('userWord', '').strip()
        answer_word = data.get('answerWord', '').strip()
        reference_words = data.get('referenceWords', [])
        
        if not user_word or not answer_word:
            return jsonify({"error": "userWord와 answerWord가 필요합니다"}), 400
        
        # 모든 단어 수집
        all_words = [answer_word, user_word]
        word_types = ['answer', 'user']
        
        # 참조 단어 추가 (있는 경우)
        for ref_word in reference_words:
            ref_word = ref_word.strip()
            if ref_word and ref_word not in all_words:
                all_words.append(ref_word)
                word_types.append('reference')
        
        # 임베딩 생성
        model = ai_service.get_model()
        embeddings = model.encode(all_words)
        
        # PCA로 3D 좌표로 축소
        # 최소 3개의 샘플이 필요하므로 더미 포인트 추가
        if len(embeddings) < 3:
            # 원점과 축 방향 더미 포인트 추가
            dummy_embeddings = np.zeros((3 - len(embeddings), embeddings.shape[1]))
            embeddings_extended = np.vstack([embeddings, dummy_embeddings])
        else:
            embeddings_extended = embeddings
        
        pca = PCA(n_components=3)
        coords_3d = pca.fit_transform(embeddings_extended)
        
        # 실제 단어들의 좌표만 사용
        coords_3d = coords_3d[:len(all_words)]
        
        # 좌표를 -1 ~ 1 범위로 정규화
        max_abs = np.max(np.abs(coords_3d)) if np.max(np.abs(coords_3d)) > 0 else 1
        coords_normalized = coords_3d / max_abs
        
        # 유사도 계산
        similarity = ai_service.calculate_similarity(user_word, answer_word)
        
        # 3D 공간에서의 거리 계산 (정답-사용자 입력)
        distance = np.linalg.norm(coords_normalized[0] - coords_normalized[1])
        
        # 결과 포인트 생성
        points = []
        for i, (word, word_type) in enumerate(zip(all_words, word_types)):
            points.append({
                "word": word,
                "x": float(coords_normalized[i][0]),
                "y": float(coords_normalized[i][1]),
                "z": float(coords_normalized[i][2]),
                "type": word_type
            })
        
        return jsonify({
            "points": points,
            "similarity": float(similarity),
            "score": round(similarity * 100, 2),
            "distance3d": float(distance),
            "isCorrect": user_word == answer_word,
            "variance_explained": [float(v) for v in pca.explained_variance_ratio_]
        })
        
    except Exception as e:
        print(f"❌ 오류 발생: {str(e)}")
        return jsonify({"error": str(e)}), 500


@visualization_bp.route('/embedding-3d-batch', methods=['POST'])
def get_embedding_3d_batch():
    """
    여러 단어들의 임베딩을 3D 좌표로 변환하여 반환
    히스토리 시각화에 유용
    
    Request Body:
    {
        "answerWord": "정답 단어",
        "attemptWords": ["시도1", "시도2", "시도3", ...]
    }
    
    Response:
    {
        "points": [...],
        "attempts": [
            {"word": "시도1", "similarity": 0.5, "order": 1},
            ...
        ]
    }
    """
    try:
        data = request.json
        
        if not data:
            return jsonify({"error": "JSON 데이터가 필요합니다"}), 400
        
        answer_word = data.get('answerWord', '').strip()
        attempt_words = data.get('attemptWords', [])
        
        if not answer_word:
            return jsonify({"error": "answerWord가 필요합니다"}), 400
        
        if not isinstance(attempt_words, list) or len(attempt_words) == 0:
            return jsonify({"error": "attemptWords 배열이 필요합니다"}), 400
        
        # 정답을 첫 번째로, 시도들을 순서대로
        all_words = [answer_word]
        word_types = ['answer']
        
        for word in attempt_words:
            word = word.strip()
            if word:
                all_words.append(word)
                word_types.append('attempt')
        
        # 임베딩 생성
        model = ai_service.get_model()
        embeddings = model.encode(all_words)
        
        # 충분한 샘플 확보
        if len(embeddings) < 3:
            dummy_embeddings = np.zeros((3 - len(embeddings), embeddings.shape[1]))
            embeddings_extended = np.vstack([embeddings, dummy_embeddings])
        else:
            embeddings_extended = embeddings
        
        # PCA로 3D 좌표로 축소
        pca = PCA(n_components=3)
        coords_3d = pca.fit_transform(embeddings_extended)
        coords_3d = coords_3d[:len(all_words)]
        
        # 정규화
        max_abs = np.max(np.abs(coords_3d)) if np.max(np.abs(coords_3d)) > 0 else 1
        coords_normalized = coords_3d / max_abs
        
        # 결과 생성
        points = []
        attempts = []
        
        for i, (word, word_type) in enumerate(zip(all_words, word_types)):
            point = {
                "word": word,
                "x": float(coords_normalized[i][0]),
                "y": float(coords_normalized[i][1]),
                "z": float(coords_normalized[i][2]),
                "type": word_type
            }
            
            if word_type == 'attempt':
                similarity = ai_service.calculate_similarity(word, answer_word)
                point["similarity"] = float(similarity)
                point["order"] = len(attempts) + 1
                attempts.append({
                    "word": word,
                    "similarity": float(similarity),
                    "score": round(similarity * 100, 2),
                    "order": len(attempts) + 1,
                    "isCorrect": word == answer_word
                })
            
            points.append(point)
        
        return jsonify({
            "answerWord": answer_word,
            "points": points,
            "attempts": attempts,
            "totalAttempts": len(attempts),
            "variance_explained": [float(v) for v in pca.explained_variance_ratio_]
        })
        
    except Exception as e:
        print(f"❌ 오류 발생: {str(e)}")
        return jsonify({"error": str(e)}), 500


@visualization_bp.route('/embedding-sphere', methods=['POST'])
def get_embedding_sphere():
    """
    정답을 중심으로 한 구체 위에 사용자 입력을 배치
    유사도가 높을수록 중심에 가깝게 표시
    
    Request Body:
    {
        "answerWord": "정답",
        "userWord": "사용자입력"
    }
    
    Response:
    {
        "center": {"x": 0, "y": 0, "z": 0, "word": "정답"},
        "userPoint": {"x": 0.3, "y": 0.5, "z": 0.2, "word": "입력", "radius": 0.7},
        "similarity": 0.85,
        "radius": 0.15  // 중심으로부터의 거리 (1-similarity)
    }
    """
    try:
        data = request.json
        
        if not data:
            return jsonify({"error": "JSON 데이터가 필요합니다"}), 400
        
        user_word = data.get('userWord', '').strip()
        answer_word = data.get('answerWord', '').strip()
        
        if not user_word or not answer_word:
            return jsonify({"error": "userWord와 answerWord가 필요합니다"}), 400
        
        # 유사도 계산
        similarity = ai_service.calculate_similarity(user_word, answer_word)
        
        # 임베딩 생성
        model = ai_service.get_model()
        embeddings = model.encode([answer_word, user_word])
        
        # 임베딩 차이 벡터에서 방향 추출
        diff_vector = embeddings[1] - embeddings[0]
        
        # 3D로 축소 (처음 3개 차원 또는 PCA)
        if len(diff_vector) >= 3:
            # 간단히 처음 3개 차원 사용하고 정규화
            direction = diff_vector[:3]
            direction = direction / (np.linalg.norm(direction) + 1e-8)
        else:
            direction = np.array([1, 0, 0])
        
        # 반지름: 유사도가 높을수록 작음 (중심에 가까움)
        radius = 1 - similarity
        
        # 구면 좌표 계산
        user_point = {
            "x": float(direction[0] * radius),
            "y": float(direction[1] * radius),
            "z": float(direction[2] * radius),
            "word": user_word,
            "radius": float(radius)
        }
        
        return jsonify({
            "center": {
                "x": 0,
                "y": 0,
                "z": 0,
                "word": answer_word
            },
            "userPoint": user_point,
            "similarity": float(similarity),
            "score": round(similarity * 100, 2),
            "radius": float(radius),
            "isCorrect": user_word == answer_word,
            "message": get_proximity_message(similarity)
        })
        
    except Exception as e:
        print(f"❌ 오류 발생: {str(e)}")
        return jsonify({"error": str(e)}), 500


def get_proximity_message(similarity: float) -> str:
    """유사도에 따른 메시지 반환"""
    if similarity >= 0.95:
        return "🎯 거의 정답이에요!"
    elif similarity >= 0.85:
        return "🔥 아주 가까워요!"
    elif similarity >= 0.70:
        return "👍 꽤 가까워요!"
    elif similarity >= 0.50:
        return "🤔 조금 멀어요..."
    elif similarity >= 0.30:
        return "❄️ 많이 멀어요..."
    else:
        return "🌌 완전히 다른 방향이에요!"
