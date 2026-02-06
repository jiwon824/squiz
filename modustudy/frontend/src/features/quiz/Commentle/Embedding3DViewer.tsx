/**
 * 3D 임베딩 시각화 컴포넌트
 * 정답 단어를 중심으로 사용자 입력들이 얼마나 가까운지 3D 공간에서 보여줌
 */

import React, { useState, useRef, useMemo } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import { OrbitControls, Text, Line } from '@react-three/drei';
import * as THREE from 'three';
import { Guess } from './hooks/useCommentleGame';

// 색상 상수 - 6단계 구역 (Google Design System 기반)
const COLORS = {
    answer: '#34A853',      // 초록색 - 정답 (중심)
    user: '#4285F4',        // 파란색 - 최신 입력
    history: '#FBBC04',     // 노란색 - 이전 시도들
    // 6단계 거리 색상
    perfect: '#34A853',     // 95+ 정답 영역
    veryClose: '#4ade80',   // 85-95 매우 가까움
    close: '#4285F4',       // 70-85 가까움
    medium: '#a855f7',      // 50-70 중간 (보라색 유지)
    far: '#FBBC04',         // 30-50 멀리
    veryFar: '#EA4335',     // 0-30 매우 멀리
};

/**
 * 점수에 따른 색상 반환 (6단계)
 */
const getColorByScore = (score: number): string => {
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
interface CenterSphereProps {
    label: string;
}

function CenterSphere({ label }: CenterSphereProps) {
    const meshRef = useRef<THREE.Mesh>(null);

    useFrame(() => {
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
                {label}
            </Text>
        </group>
    );
}

/**
 * 사용자 입력 점
 */
interface UserPointProps {
    position: [number, number, number];
    label: string;
    score: number;
    isLatest?: boolean;
    order: number;
}

function UserPoint({ position, label, score, isLatest = false, order }: UserPointProps) {
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
interface ConnectionLineProps {
    end: [number, number, number];
    score: number;
}

function ConnectionLine({ end, score }: ConnectionLineProps) {
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
            {/* 🎯 정답 영역 (score 95+) */}
            <mesh>
                <sphereGeometry args={[0.1, 32, 32]} />
                <meshBasicMaterial color="#22c55e" transparent opacity={0.15} />
            </mesh>

            {/* 🔥 매우 가까움 (score 85-94) */}
            <mesh>
                <sphereGeometry args={[0.2, 32, 32]} />
                <meshBasicMaterial color="#4ade80" transparent opacity={0.08} wireframe />
            </mesh>

            {/* 👍 가까움 (score 70-84) */}
            <mesh>
                <sphereGeometry args={[0.4, 32, 32]} />
                <meshBasicMaterial color="#60a5fa" transparent opacity={0.06} wireframe />
            </mesh>

            {/* 🤔 중간 (score 50-69) */}
            <mesh>
                <sphereGeometry args={[0.6, 32, 32]} />
                <meshBasicMaterial color="#a78bfa" transparent opacity={0.04} wireframe />
            </mesh>

            {/* ❄️ 멀리 (score 30-49) */}
            <mesh>
                <sphereGeometry args={[0.8, 32, 32]} />
                <meshBasicMaterial color="#fbbf24" transparent opacity={0.03} wireframe />
            </mesh>

            {/* 🌌 매우 멀리 (score 0-29) */}
            <mesh>
                <sphereGeometry args={[1, 32, 32]} />
                <meshBasicMaterial color="#f87171" transparent opacity={0.02} wireframe />
            </mesh>
        </>
    );
}

/**
 * 카메라 진동 애니메이션 컴포넌트
 * 정면 기준 좌우로 부드럽게 왔다갔다
 */
interface CameraOscillationProps {
    controlsRef: React.RefObject<any>;
}

function CameraOscillation({ controlsRef }: CameraOscillationProps) {
    const isUserInteracting = React.useRef(false);
    const idleTimer = React.useRef<ReturnType<typeof setTimeout> | null>(null);

    React.useEffect(() => {
        const controls = controlsRef.current;
        if (!controls) return;

        const onStart = () => {
            isUserInteracting.current = true;
            if (idleTimer.current) clearTimeout(idleTimer.current);
        };
        const onEnd = () => {
            // 사용자 조작 끝난 후 3초 뒤 자동 진동 재개
            idleTimer.current = setTimeout(() => {
                isUserInteracting.current = false;
            }, 3000);
        };

        controls.addEventListener('start', onStart);
        controls.addEventListener('end', onEnd);
        return () => {
            controls.removeEventListener('start', onStart);
            controls.removeEventListener('end', onEnd);
            if (idleTimer.current) clearTimeout(idleTimer.current);
        };
    }, [controlsRef]);

    useFrame(({ clock }) => {
        if (controlsRef.current && !isUserInteracting.current) {
            const t = clock.getElapsedTime();
            // 좌우로 -45도 ~ +45도 사이를 부드럽게 왔다갔다 (0.2 = 느린 속도)
            const angle = Math.sin(t * 0.2) * (Math.PI / 4);
            controlsRef.current.setAzimuthalAngle(angle);
            controlsRef.current.update();
        }
    });
    return null;
}

/**
 * 3D 씬 컴포넌트
 */
interface SceneProps {
    guesses: Guess[];
}

function Scene({ guesses }: SceneProps) {
    const controlsRef = useRef<any>(null);

    return (
        <>
            <ambientLight intensity={0.6} />
            <pointLight position={[10, 10, 10]} intensity={1} />
            <pointLight position={[-10, -10, -10]} intensity={0.5} />

            <DistanceSpheres />
            <CenterSphere label="🎯 정답" />

            {guesses.map((guess, index) => {
                let distance: number;
                const score = guess.score;

                if (score >= 95) {
                    distance = 0.1 + (100 - score) / 5 * 0.1;
                } else if (score >= 85) {
                    distance = 0.2 + (95 - score) / 10 * 0.2;
                } else if (score >= 70) {
                    distance = 0.4 + (85 - score) / 15 * 0.2;
                } else if (score >= 50) {
                    distance = 0.6 + (70 - score) / 20 * 0.2;
                } else if (score >= 30) {
                    distance = 0.8 + (50 - score) / 20 * 0.2;
                } else {
                    distance = 1.0 + (30 - score) / 30 * 0.2;
                }

                const phi = Math.acos(1 - 2 * (index + 1) / (guesses.length + 2));
                const theta = Math.PI * (1 + Math.sqrt(5)) * (index + 1);

                const x = distance * Math.sin(phi) * Math.cos(theta);
                const y = distance * Math.sin(phi) * Math.sin(theta);
                const z = distance * Math.cos(phi);

                const position: [number, number, number] = [x, y, z];
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

            {/* 카메라 진동 애니메이션 */}
            <CameraOscillation controlsRef={controlsRef} />

            <OrbitControls
                ref={controlsRef}
                enablePan={true}
                enableZoom={true}
                enableRotate={true}
                autoRotate={false}
                minDistance={0.5}
                maxDistance={3}
            />
        </>
    );
}

/**
 * 메인 3D 시각화 컴포넌트
 */
interface Embedding3DViewerProps {
    guesses?: Guess[];
}

const Embedding3DViewer: React.FC<Embedding3DViewerProps> = ({ guesses = [] }) => {
    const latestGuess = guesses[0];
    const proximityMessage = latestGuess ? getProximityMessage(latestGuess.score) : '';
    const hasGuesses = guesses.length > 0;

    return (
        <div className="w-full">
            <div className="w-full h-[300px] lg:h-[340px] bg-slate-900 rounded-2xl lg:rounded-3xl overflow-hidden relative">
                {/* Canvas는 항상 렌더링 - Three.js 미리 초기화 */}
                <Canvas camera={{ position: [1.5, 1.5, 1.5], fov: 50 }}>
                    <Scene guesses={guesses} />
                </Canvas>

                {/* 데이터 없을 때 안내 오버레이 */}
                {!hasGuesses && (
                    <div className="absolute inset-0 flex items-center justify-center bg-slate-900/70 backdrop-blur-sm">
                        <p className="text-base lg:text-xl font-semibold text-white leading-relaxed text-center px-6">
                            단어를 입력하면 3D 공간에서<br />정답과의 거리를 확인할 수 있어요!
                        </p>
                    </div>
                )}

                {/* 최신 시도 정보 오버레이 */}
                {hasGuesses && (
                    <div className="absolute bottom-3 lg:bottom-4 left-3 lg:left-4 bg-black/60 backdrop-blur-sm px-3 lg:px-4 py-2 lg:py-3 rounded-lg lg:rounded-xl text-white">
                        <div className="flex items-center gap-2 lg:gap-3 text-sm lg:text-base">
                            <span className="font-bold">{latestGuess?.word}</span>
                            <span className="text-slate-300">→</span>
                            <span className="font-semibold" style={{ color: getColorByScore(latestGuess?.score || 0) }}>
                                {latestGuess?.score?.toFixed(1)}점
                            </span>
                        </div>
                        <div className="text-xs lg:text-sm text-slate-400 mt-1">{proximityMessage}</div>
                    </div>
                )}

                {/* 범례 */}
                {hasGuesses && (
                    <div className="absolute top-3 lg:top-4 right-3 lg:right-4 bg-black/60 backdrop-blur-sm px-3 lg:px-4 py-2 lg:py-3 rounded-lg lg:rounded-xl text-white">
                        <div className="flex items-center gap-2 mb-1.5 text-xs lg:text-sm">
                            <span className="w-2.5 h-2.5 lg:w-3 lg:h-3 rounded-full" style={{ background: COLORS.answer }}></span>
                            <span>정답 (중심)</span>
                        </div>
                        <div className="flex items-center gap-2 mb-1.5 text-xs lg:text-sm">
                            <span className="w-2.5 h-2.5 lg:w-3 lg:h-3 rounded-full" style={{ background: COLORS.user }}></span>
                            <span>최신 입력</span>
                        </div>
                        <div className="flex items-center gap-2 text-xs lg:text-sm">
                            <span className="w-2.5 h-2.5 lg:w-3 lg:h-3 rounded-full" style={{ background: COLORS.history }}></span>
                            <span>이전 시도</span>
                        </div>
                    </div>
                )}
            </div>

            {/* 거리 가이드 - 6단계 */}
            {hasGuesses && (
                <div className="flex flex-wrap justify-center gap-3 lg:gap-4 mt-3 lg:mt-4 text-xs lg:text-sm text-slate-500">
                    <span><span className="inline-block w-2.5 h-2.5 lg:w-3 lg:h-3 rounded-full mr-1.5" style={{ background: COLORS.perfect }}></span>해커 (95+)</span>
                    <span><span className="inline-block w-2.5 h-2.5 lg:w-3 lg:h-3 rounded-full mr-1.5" style={{ background: COLORS.veryClose }}></span>매우 가까움 (85-94)</span>
                    <span><span className="inline-block w-2.5 h-2.5 lg:w-3 lg:h-3 rounded-full mr-1.5" style={{ background: COLORS.close }}></span>가까움 (70-84)</span>
                    <span><span className="inline-block w-2.5 h-2.5 lg:w-3 lg:h-3 rounded-full mr-1.5" style={{ background: COLORS.medium }}></span>중간 (50-69)</span>
                    <span><span className="inline-block w-2.5 h-2.5 lg:w-3 lg:h-3 rounded-full mr-1.5" style={{ background: COLORS.far }}></span>멀리 (30-49)</span>
                    <span><span className="inline-block w-2.5 h-2.5 lg:w-3 lg:h-3 rounded-full mr-1.5" style={{ background: COLORS.veryFar }}></span>매우 멀리 (0-29)</span>
                </div>
            )}
        </div>
    );
};

/**
 * 점수에 따른 메시지
 */
function getProximityMessage(score: number): string {
    if (score >= 100) return "🎉 축하합니다! 정답이에요!";
    if (score >= 95) return "🎯 거의 정답이에요!";
    if (score >= 85) return "🔥 아주 가까워요!";
    if (score >= 70) return "👍 꽤 가까워요!";
    if (score >= 50) return "🤔 조금 멀어요...";
    if (score >= 30) return "❄️ 많이 멀어요...";
    return " 완전히 다른 방향이에요!";
}

export default Embedding3DViewer;
