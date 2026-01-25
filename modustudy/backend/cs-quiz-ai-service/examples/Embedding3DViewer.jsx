/**
 * 3D 임베딩 시각화 컴포넌트
 * Three.js + React Three Fiber를 사용하여 단어 임베딩을 3D 공간에 시각화
 * 
 * 필요한 패키지:
 * npm install three @react-three/fiber @react-three/drei
 */

import { Canvas } from '@react-three/fiber';
import { OrbitControls, Text, Sphere, Line } from '@react-three/drei';
import { useState, useEffect, useRef } from 'react';

// 색상 상수
const COLORS = {
  answer: '#22c55e',      // 초록색 - 정답
  user: '#3b82f6',        // 파란색 - 사용자 입력
  reference: '#a855f7',   // 보라색 - 참조 단어
  attempt: '#f59e0b',     // 주황색 - 시도 히스토리
  line: '#ffffff',        // 흰색 - 연결선
  grid: '#374151',        // 회색 - 그리드
};

/**
 * 3D 점 컴포넌트
 */
function Point3D({ position, color, label, size = 0.08, showLabel = true }) {
  const [hovered, setHovered] = useState(false);
  
  return (
    <group position={position}>
      <Sphere
        args={[size, 32, 32]}
        onPointerOver={() => setHovered(true)}
        onPointerOut={() => setHovered(false)}
      >
        <meshStandardMaterial
          color={hovered ? '#ffffff' : color}
          emissive={color}
          emissiveIntensity={0.5}
        />
      </Sphere>
      {showLabel && (
        <Text
          position={[0, size + 0.1, 0]}
          fontSize={0.12}
          color="white"
          anchorX="center"
          anchorY="bottom"
        >
          {label}
        </Text>
      )}
    </group>
  );
}

/**
 * 두 점 사이의 연결선
 */
function ConnectionLine({ start, end, opacity = 0.5 }) {
  return (
    <Line
      points={[start, end]}
      color={COLORS.line}
      lineWidth={1}
      opacity={opacity}
      transparent
    />
  );
}

/**
 * 3D 그리드/축
 */
function AxisHelper() {
  return (
    <group>
      {/* X축 - 빨강 */}
      <Line points={[[-1.5, 0, 0], [1.5, 0, 0]]} color="#ef4444" lineWidth={1} />
      {/* Y축 - 초록 */}
      <Line points={[[0, -1.5, 0], [0, 1.5, 0]]} color="#22c55e" lineWidth={1} />
      {/* Z축 - 파랑 */}
      <Line points={[[0, 0, -1.5], [0, 0, 1.5]]} color="#3b82f6" lineWidth={1} />
    </group>
  );
}

/**
 * 메인 3D 시각화 컴포넌트
 */
export function Embedding3DViewer({ points = [], showConnections = true }) {
  // 정답 포인트 찾기
  const answerPoint = points.find(p => p.type === 'answer');
  
  return (
    <div className="w-full h-96 bg-gray-900 rounded-lg overflow-hidden">
      <Canvas camera={{ position: [2, 2, 2], fov: 50 }}>
        {/* 조명 */}
        <ambientLight intensity={0.5} />
        <pointLight position={[10, 10, 10]} intensity={1} />
        
        {/* 축 표시 */}
        <AxisHelper />
        
        {/* 포인트들 렌더링 */}
        {points.map((point, index) => (
          <Point3D
            key={index}
            position={[point.x, point.y, point.z]}
            color={COLORS[point.type] || COLORS.reference}
            label={point.word}
            size={point.type === 'answer' ? 0.12 : 0.08}
          />
        ))}
        
        {/* 정답과 다른 포인트들 연결선 */}
        {showConnections && answerPoint && points
          .filter(p => p.type !== 'answer')
          .map((point, index) => (
            <ConnectionLine
              key={index}
              start={[answerPoint.x, answerPoint.y, answerPoint.z]}
              end={[point.x, point.y, point.z]}
              opacity={0.3}
            />
          ))
        }
        
        {/* 카메라 컨트롤 */}
        <OrbitControls
          enablePan={true}
          enableZoom={true}
          enableRotate={true}
          autoRotate={true}
          autoRotateSpeed={0.5}
        />
      </Canvas>
    </div>
  );
}

/**
 * 구체 시각화 컴포넌트 (정답 중심)
 */
export function EmbeddingSphereViewer({ center, userPoint, similarity }) {
  return (
    <div className="w-full h-96 bg-gray-900 rounded-lg overflow-hidden">
      <Canvas camera={{ position: [2, 2, 2], fov: 50 }}>
        <ambientLight intensity={0.5} />
        <pointLight position={[10, 10, 10]} intensity={1} />
        
        {/* 중심점 (정답) */}
        {center && (
          <Point3D
            position={[center.x, center.y, center.z]}
            color={COLORS.answer}
            label={center.word}
            size={0.15}
          />
        )}
        
        {/* 사용자 입력 */}
        {userPoint && (
          <>
            <Point3D
              position={[userPoint.x, userPoint.y, userPoint.z]}
              color={COLORS.user}
              label={userPoint.word}
              size={0.1}
            />
            
            {/* 중심과 연결선 */}
            <ConnectionLine
              start={[center.x, center.y, center.z]}
              end={[userPoint.x, userPoint.y, userPoint.z]}
            />
          </>
        )}
        
        {/* 반투명 구체 (유사도 범위 표시) */}
        <Sphere args={[1, 32, 32]} position={[0, 0, 0]}>
          <meshStandardMaterial
            color="#3b82f6"
            transparent
            opacity={0.1}
            wireframe
          />
        </Sphere>
        
        <OrbitControls autoRotate autoRotateSpeed={0.5} />
      </Canvas>
      
      {/* 유사도 표시 */}
      {similarity !== undefined && (
        <div className="absolute bottom-4 left-4 bg-black/50 px-3 py-2 rounded text-white">
          유사도: {(similarity * 100).toFixed(1)}%
        </div>
      )}
    </div>
  );
}

/**
 * API 연동 훅
 */
export function useEmbedding3D(apiBaseUrl = 'http://localhost:5001') {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [data, setData] = useState(null);

  const fetchEmbedding3D = async (userWord, answerWord, referenceWords = []) => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await fetch(`${apiBaseUrl}/api/embedding-3d`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userWord, answerWord, referenceWords })
      });
      
      if (!response.ok) throw new Error('API 요청 실패');
      
      const result = await response.json();
      setData(result);
      return result;
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const fetchEmbeddingSphere = async (userWord, answerWord) => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await fetch(`${apiBaseUrl}/api/embedding-sphere`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userWord, answerWord })
      });
      
      if (!response.ok) throw new Error('API 요청 실패');
      
      const result = await response.json();
      setData(result);
      return result;
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const fetchBatchEmbedding3D = async (answerWord, attemptWords) => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await fetch(`${apiBaseUrl}/api/embedding-3d-batch`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ answerWord, attemptWords })
      });
      
      if (!response.ok) throw new Error('API 요청 실패');
      
      const result = await response.json();
      setData(result);
      return result;
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  return {
    loading,
    error,
    data,
    fetchEmbedding3D,
    fetchEmbeddingSphere,
    fetchBatchEmbedding3D
  };
}

/**
 * 사용 예시 컴포넌트
 */
export default function EmbeddingVisualization() {
  const { loading, data, fetchEmbedding3D } = useEmbedding3D();
  const [userWord, setUserWord] = useState('');
  const [answerWord, setAnswerWord] = useState('배열');

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (userWord.trim()) {
      await fetchEmbedding3D(userWord, answerWord);
    }
  };

  return (
    <div className="p-6 space-y-4">
      <h2 className="text-xl font-bold">임베딩 3D 시각화</h2>
      
      <form onSubmit={handleSubmit} className="flex gap-2">
        <input
          type="text"
          value={userWord}
          onChange={(e) => setUserWord(e.target.value)}
          placeholder="단어를 입력하세요"
          className="px-3 py-2 border rounded flex-1"
        />
        <button
          type="submit"
          disabled={loading}
          className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 disabled:opacity-50"
        >
          {loading ? '로딩...' : '확인'}
        </button>
      </form>

      {data && (
        <>
          <div className="text-sm text-gray-600">
            정답: {data.answerWord || answerWord} | 
            유사도: {(data.similarity * 100).toFixed(1)}% |
            점수: {data.score}점
          </div>
          
          <Embedding3DViewer 
            points={data.points} 
            showConnections={true}
          />
        </>
      )}
    </div>
  );
}
