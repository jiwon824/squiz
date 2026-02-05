import React from 'react';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { SquizLogoNew } from '@/shared/components/SquizLogoNew';

export const LandingHero: React.FC = () => {
  const navigate = useNavigate();

  return (
    <section className="relative w-full h-screen flex flex-col items-center justify-center bg-white px-6 overflow-hidden">
      {/* 배경 장식 */}
      <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-blue-50 rounded-full blur-3xl opacity-50" />
      <div className="absolute bottom-[-10%] right-[-10%] w-[30%] h-[30%] bg-indigo-50 rounded-full blur-3xl opacity-50" />

      {/* 메인 콘텐츠 */}
      <div className="z-10 flex flex-col items-center text-center space-y-8 max-w-4xl">
        {/* 로고 */}
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7 }}
          className="transition-transform duration-700 hover:scale-105"
        >
          <SquizLogoNew width={300} height={300} />
        </motion.div>

        {/* 소개 문구 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7, delay: 0.2 }}
          className="space-y-4"
        >
          <h1 className="text-4xl md:text-6xl font-extrabold text-slate-900 tracking-tight leading-tight">
            당신의 학습을 <br />
            <span className="text-primary">함께 만드는 공간</span>
          </h1>

          <p className="text-lg md:text-xl text-slate-500 font-medium max-w-2xl mx-auto leading-relaxed">
            스터디 참여만으로 자동 요약, AI 퀴즈, 학습 관리까지.
            <br className="hidden md:block" />
            복잡한 학습 관리는 이제 SQuiz에 맡기세요.
          </p>
        </motion.div>

        {/* CTA 버튼 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7, delay: 0.4 }}
        >
          <button
            onClick={() => navigate('/login')}
            className="px-8 py-3 bg-slate-900 text-white font-bold rounded-full shadow-lg hover:bg-primary hover:shadow-blue-200 transition-colors duration-300"
          >
            시작하기
          </button>
        </motion.div>
      </div>

      {/* 스크롤 유도 화살표 */}
      <div className="absolute bottom-10 w-full flex justify-center animate-bounce">
        <svg className="w-6 h-6 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 14l-7 7m0 0l-7-7m7 7V3" />
        </svg>
      </div>
    </section>
  );
};
