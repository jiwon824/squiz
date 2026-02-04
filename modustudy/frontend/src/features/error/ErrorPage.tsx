import { useParams, useNavigate } from 'react-router-dom';
import { cn } from '@/shared/utils/cn';

// 에러 코드별 메시지 정의
const ERROR_CONFIG: Record<string, { title: string; message: string }> = {
  '404': {
    title: '페이지를 찾을 수 없어요',
    message: '요청하신 페이지가 존재하지 않거나, 이동되었을 수 있어요.',

  },
  '403': {
    title: '접근이 제한되었어요',
    message: '이 페이지에 접근할 권한이 없어요.',

  },
  '500': {
    title: '서버에 문제가 생겼어요',
    message: '잠시 후 다시 시도해 주세요. 문제가 계속되면 관리자에게 문의해 주세요.',

  },
  '503': {
    title: '서비스 점검 중이에요',
    message: '더 나은 서비스를 위해 점검 중이에요. 잠시만 기다려 주세요.',

  },
};

const DEFAULT_ERROR = {
  title: '알 수 없는 오류가 발생했어요',
  message: '예상치 못한 문제가 발생했어요. 잠시 후 다시 시도해 주세요.',
};

export const ErrorPage = () => {
  const { code } = useParams<{ code: string }>();
  const navigate = useNavigate();

  const errorCode = code || '404';
  const config = ERROR_CONFIG[errorCode] || DEFAULT_ERROR;

  return (
    <div
      className={cn(
        'min-h-screen flex flex-col items-center justify-center',
        'bg-gradient-to-b from-sky-50 via-blue-50 to-cyan-50',
        'relative overflow-hidden'
      )}
    >
      {/* 배경 물방울 애니메이션 */}
      <div className="absolute inset-0 pointer-events-none">
        {[...Array(12)].map((_, i) => {
          const size = 30 + Math.random() * 60;
          return (
            <div
              key={i}
              className="absolute rounded-full bg-blue-300/50 animate-bubble"
              style={{
                width: `${size}px`,
                height: `${size}px`,
                left: `${5 + i * 8}%`,
                animationDelay: `-${i * 0.6}s`,
                animationDuration: `${4 + Math.random() * 3}s`,
              }}
            />
          );
        })}
      </div>

      {/* 메인 콘텐츠 */}
      <div className="relative z-10 flex flex-col items-center px-6 text-center">
        {/* 돌고래 캐릭터 */}
        <div className="animate-float-3d -mb-4" style={{ perspective: '800px' }}>
          <img
            src="/images/Gemini_Error_dolphin.png"
            alt="에러 돌고래"
            className={cn(
              'w-64 h-64 md:w-96 md:h-96 object-contain',
              'drop-shadow-xl'
            )}
            style={{ transformStyle: 'preserve-3d' }}
            draggable={false}
          />
        </div>

        {/* 에러 코드 */}
        <div
          className={cn(
            'text-7xl md:text-9xl font-black',
            'bg-gradient-to-r from-blue-500 via-cyan-500 to-blue-400',
            'bg-clip-text text-transparent',
            'animate-pulse-slow select-none'
          )}
        >
          {errorCode}
        </div>

        {/* 에러 메시지 */}
        <h1 className="mt-4 text-xl md:text-2xl font-bold text-gray-800">
          {config.title}
        </h1>
        <p className="mt-2 text-sm md:text-base text-gray-500 max-w-md">
          {config.message}
        </p>

        {/* 액션 버튼 */}
        <div className="mt-8 flex gap-4">
          <button
            onClick={() => navigate(-1)}
            className={cn(
              'px-5 py-2 rounded-lg text-sm font-medium',
              'text-gray-500 hover:text-gray-700',
              'hover:bg-gray-100/60',
              'transition-colors duration-150'
            )}
          >
            ← 이전 페이지
          </button>
          <button
            onClick={() => navigate('/')}
            className={cn(
              'px-5 py-2 rounded-lg text-sm font-medium',
              'bg-gray-800 text-white',
              'hover:bg-gray-700',
              'transition-colors duration-150'
            )}
          >
            홈으로 가기
          </button>
        </div>
      </div>

      {/* CSS 애니메이션 */}
      <style>{`
        @keyframes float-3d {
          0% {
            transform: translateY(0px) rotateX(0deg) rotateY(0deg) scale(1);
          }
          25% {
            transform: translateY(-14px) rotateX(5deg) rotateY(-8deg) scale(1.03);
          }
          50% {
            transform: translateY(-4px) rotateX(-3deg) rotateY(5deg) scale(0.98);
          }
          75% {
            transform: translateY(-10px) rotateX(4deg) rotateY(-4deg) scale(1.02);
          }
          100% {
            transform: translateY(0px) rotateX(0deg) rotateY(0deg) scale(1);
          }
        }
        .animate-float-3d {
          animation: float-3d 4s ease-in-out infinite;
        }

        @keyframes bubble {
          0% {
            transform: translateY(100vh) scale(0);
            opacity: 0;
          }
          50% {
            opacity: 0.8;
          }
          100% {
            transform: translateY(-10vh) scale(1);
            opacity: 0;
          }
        }
        .animate-bubble {
          animation: bubble 4s ease-in-out infinite;
        }

        @keyframes pulse-slow {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.8; }
        }
        .animate-pulse-slow {
          animation: pulse-slow 2.5s ease-in-out infinite;
        }

      `}</style>
    </div>
  );
};
