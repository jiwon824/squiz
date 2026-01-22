/**
 * 3D 임베딩 시각화 컴포넌트
 * 정답 단어를 중심으로 사용자 입력들이 얼마나 가까운지 3D 공간에서 보여줌
 */

import { Canvas, useFrame } from '@react-three/fiber';
import { OrbitControls, Text, Sphere, Line } from '@react-three/drei';
import { useState, useRef, useMemo } from 'react';
import * as THREE from 'three';

// 색상 상수 - 6단계 구역
const COLORS = {
    answer: '#22c55e',      // 초록색 - 정답 (중심)
    user: '#3b82f6',        // 파란색 - 최신 입력
    history: '#f59e0b',     // 주황색 - 이전 시도들
    // 6단계 거리 색상
    perfect: '#22c55e',     // 95+ 정답 영역
    veryClose: '#4ade80',   // 85-95 매우 가까움
    close: '#60a5fa',       // 70-85 가까움
    medium: '#a78bfa',      // 50-70 중간
    far: '#fbbf24',         // 30-50 멀리
    veryFar: '#f87171',     // 0-30 매우 멀리
};

/**
 * 점수에 따른 색상 반환 (6단계)
 */
const getColorByScore = (score) => {
    if (score >= 95) return COLORS.perfect;
    if (score >= 85) return COLORS.veryClose;
    if (score >= 70) return COLORS.close;
    if (score >= 50) return COLORS.medium;
    if (score >= 30) return COLORS.far;
    return COLORS.veryFar;
};

/**
 * 회전하는 중심 구체 (정답)
 */
function CenterSphere({ label }) {
    const meshRef = useRef();

    useFrame((state) => {
        if (meshRef.current) {
            meshRef.current.rotation.y += 0.005;
        }
    });

    return (
        <group position={[0, 0, 0]}>
            <mesh ref={meshRef}>
                <sphereGeometry args={[0.15, 32, 32]} />
                <meshStandardMaterial
                    color={COLORS.answer}
                    emissive={COLORS.answer}
                    emissiveIntensity={0.3}
                />
            </mesh>
            <Text
                position={[0, 0.3, 0]}
                fontSize={0.12}
                color="white"
                anchorX="center"
                anchorY="bottom"
            >
                🎯 정답
            </Text>
        </group>
    );
}

/**
 * 사용자 입력 점
 */
function UserPoint({ position, label, score, isLatest = false, order }) {
    const [hovered, setHovered] = useState(false);
    const color = isLatest ? COLORS.user : getColorByScore(score);
    const size = isLatest ? 0.1 : 0.07;

    return (
        <group position={position}>
            <mesh
                onPointerOver={() => setHovered(true)}
                onPointerOut={() => setHovered(false)}
            >
                <sphereGeometry args={[hovered ? size * 1.3 : size, 32, 32]} />
                <meshStandardMaterial
                    color={hovered ? '#ffffff' : color}
                    emissive={color}
                    emissiveIntensity={0.4}
                />
            </mesh>
            {(hovered || isLatest) && (
                <Text
                    position={[0, size + 0.12, 0]}
                    fontSize={0.08}
                    color="white"
                    anchorX="center"
                    anchorY="bottom"
                >
                    {`${label} (${score.toFixed(1)})`}
                </Text>
            )}
            {/* 순서 번호 */}
            <Text
                position={[0, -size - 0.05, 0]}
                fontSize={0.05}
                color="#888"
                anchorX="center"
            >
                #{order}
            </Text>
        </group>
    );
}

/**
 * 중심과 점을 연결하는 선
 */
function ConnectionLine({ end, score }) {
    const color = getColorByScore(score);
    const points = useMemo(() => [
        new THREE.Vector3(0, 0, 0),
        new THREE.Vector3(...end)
    ], [end]);

    return (
        <Line
            points={points}
            color={color}
            lineWidth={1}
            opacity={0.4}
            transparent
        />
    );
}

/**
 * 반투명 거리 구체들 (가이드) - 5단계로 세분화
 */
function DistanceSpheres() {
    return (
        <>
            {/* 🎯 정답 영역 (score 95+) - 거의 정답! */}
            <mesh>
                <sphereGeometry args={[0.1, 32, 32]} />
                <meshBasicMaterial color="#22c55e" transparent opacity={0.15} />
            </mesh>

            {/* 🔥 매우 가까움 (score 85-95) */}
            <mesh>
                <sphereGeometry args={[0.2, 32, 32]} />
                <meshBasicMaterial color="#4ade80" transparent opacity={0.08} wireframe />
            </mesh>

            {/* 👍 가까움 (score 70-85) */}
            <mesh>
                <sphereGeometry args={[0.4, 32, 32]} />
                <meshBasicMaterial color="#60a5fa" transparent opacity={0.06} wireframe />
            </mesh>

            {/* 🤔 중간 (score 50-70) */}
            <mesh>
                <sphereGeometry args={[0.6, 32, 32]} />
                <meshBasicMaterial color="#a78bfa" transparent opacity={0.04} wireframe />
            </mesh>

            {/* ❄️ 멀리 (score 30-50) */}
            <mesh>
                <sphereGeometry args={[0.8, 32, 32]} />
                <meshBasicMaterial color="#fbbf24" transparent opacity={0.03} wireframe />
            </mesh>

            {/* 🌌 매우 멀리 (score 0-30) */}
            <mesh>
                <sphereGeometry args={[1, 32, 32]} />
                <meshBasicMaterial color="#f87171" transparent opacity={0.02} wireframe />
            </mesh>
        </>
    );
}

/**
 * 3D 씬 컴포넌트
 */
function Scene({ guesses }) {
    return (
        <>
            {/* 조명 */}
            <ambientLight intensity={0.6} />
            <pointLight position={[10, 10, 10]} intensity={1} />
            <pointLight position={[-10, -10, -10]} intensity={0.5} />

            {/* 거리 가이드 구체 */}
            <DistanceSpheres />

            {/* 중심점 (정답) */}
            <CenterSphere label="정답" />

            {/* 사용자 입력들 */}
            {guesses.map((guess, index) => {
                // 점수를 기반으로 거리 계산 (score가 높을수록 가까움)
                // 최소 거리(0.25)를 두어 중심 구체(0.15)와 겹치지 않도록 함
                const minDistance = 0.25;
                const maxDistance = 1.0;
                const rawDistance = (100 - guess.score) / 100;
                const distance = minDistance + rawDistance * (maxDistance - minDistance);

                // 각 시도마다 다른 방향으로 배치 (황금각 사용)
                const phi = Math.acos(1 - 2 * (index + 1) / (guesses.length + 2));
                const theta = Math.PI * (1 + Math.sqrt(5)) * (index + 1);

                const x = distance * Math.sin(phi) * Math.cos(theta);
                const y = distance * Math.sin(phi) * Math.sin(theta);
                const z = distance * Math.cos(phi);

                const position = [x, y, z];
                const isLatest = index === 0;

                return (
                    <group key={guess.id || index}>
                        <ConnectionLine end={position} score={guess.score} />
                        <UserPoint
                            position={position}
                            label={guess.word}
                            score={guess.score}
                            isLatest={isLatest}
                            order={guesses.length - index}
                        />
                    </group>
                );
            })}

            {/* 카메라 컨트롤 */}
            <OrbitControls
                enablePan={true}
                enableZoom={true}
                enableRotate={true}
                autoRotate={true}
                autoRotateSpeed={0.3}
                minDistance={0.5}
                maxDistance={3}
            />
        </>
    );
}

/**
 * 메인 3D 시각화 컴포넌트
 */
const Embedding3DViewer = ({ guesses = [] }) => {
    if (guesses.length === 0) {
        return (
            <div className="w-full h-[500px] bg-slate-900 rounded-xl flex items-center justify-center text-slate-400">
                <div className="text-center">
                    <span className="text-4xl">🌌</span>
                    <p className="mt-2 text-sm">단어를 입력하면 3D 공간에서<br />정답과의 거리를 확인할 수 있어요!</p>
                </div>
            </div>
        );
    }

    const latestGuess = guesses[0];
    const proximityMessage = getProximityMessage(latestGuess?.score || 0);

    return (
        <div className="w-full">
            {/* 3D 캔버스 */}
            <div className="w-full h-[500px] bg-slate-900 rounded-xl overflow-hidden relative">
                <Canvas camera={{ position: [1.5, 1.5, 1.5], fov: 50 }}>
                    <Scene guesses={guesses} />
                </Canvas>

                {/* 최신 시도 정보 오버레이 */}
                <div className="absolute bottom-3 left-3 bg-black/60 backdrop-blur-sm px-3 py-2 rounded-lg text-white text-sm">
                    <div className="flex items-center gap-2">
                        <span className="font-bold">{latestGuess?.word}</span>
                        <span className="text-slate-300">→</span>
                        <span style={{ color: getColorByScore(latestGuess?.score || 0) }}>
                            {latestGuess?.score?.toFixed(1)}점
                        </span>
                    </div>
                    <div className="text-xs text-slate-400 mt-1">{proximityMessage}</div>
                </div>

                {/* 범례 */}
                <div className="absolute top-3 right-3 bg-black/60 backdrop-blur-sm px-2 py-1.5 rounded-lg text-xs text-white">
                    <div className="flex items-center gap-1.5 mb-1">
                        <span className="w-2 h-2 rounded-full" style={{ background: COLORS.answer }}></span>
                        <span>정답 (중심)</span>
                    </div>
                    <div className="flex items-center gap-1.5 mb-1">
                        <span className="w-2 h-2 rounded-full" style={{ background: COLORS.user }}></span>
                        <span>최신 입력</span>
                    </div>
                    <div className="flex items-center gap-1.5">
                        <span className="w-2 h-2 rounded-full" style={{ background: COLORS.history }}></span>
                        <span>이전 시도</span>
                    </div>
                </div>
            </div>

            {/* 거리 가이드 - 6단계 */}
            <div className="flex flex-wrap justify-center gap-3 mt-2 text-xs text-slate-500">
                <span><span className="inline-block w-2 h-2 rounded-full mr-1" style={{ background: COLORS.perfect }}></span>해케르 (95+)</span>
                <span><span className="inline-block w-2 h-2 rounded-full mr-1" style={{ background: COLORS.veryClose }}></span>매우 가까움 (85-94)</span>
                <span><span className="inline-block w-2 h-2 rounded-full mr-1" style={{ background: COLORS.close }}></span>가까움 (70-84)</span>
                <span><span className="inline-block w-2 h-2 rounded-full mr-1" style={{ background: COLORS.medium }}></span>중간 (50-69)</span>
                <span><span className="inline-block w-2 h-2 rounded-full mr-1" style={{ background: COLORS.far }}></span>멀리 (30-49)</span>
                <span><span className="inline-block w-2 h-2 rounded-full mr-1" style={{ background: COLORS.veryFar }}></span>매우 멀리 (0-29)</span>
            </div>
        </div>
    );
};

/**
 * 점수에 따른 메시지
 */
function getProximityMessage(score) {
    if (score >= 100) return "🎉 축하합니다! 정답이에요!";
    if (score >= 95) return "🎯 거의 정답이에요!";
    if (score >= 85) return "🔥 아주 가까워요!";
    if (score >= 70) return "👍 꽤 가까워요!";
    if (score >= 50) return "🤔 조금 멀어요...";
    if (score >= 30) return "❄️ 많이 멀어요...";
    return "🌌 완전히 다른 방향이에요!";
}

export default Embedding3DViewer;
