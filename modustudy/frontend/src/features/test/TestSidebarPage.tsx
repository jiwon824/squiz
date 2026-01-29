
import { UserLayoutV2 } from '@/layouts/UserLayoutV2';

export const TestSidebarPage = () => {
    return (
        <UserLayoutV2>
            <div className="space-y-6">
                <h2 className="text-3xl font-bold text-study-text">
                    사이드바 테스트 페이지
                </h2>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    {/* 테스트 카드 1 */}
                    <div className="bg-white p-6 rounded-google shadow-sm border border-study-blue/20">
                        <h3 className="text-xl font-semibold text-study-blue mb-2">
                            퀴즈 통계
                        </h3>
                        <p className="text-study-text">
                            오늘 풀은 퀴즈: 5개
                        </p>
                    </div>

                    {/* 테스트 카드 2 */}
                    <div className="bg-white p-6 rounded-google shadow-sm border border-study-teal/20">
                        <h3 className="text-xl font-semibold text-study-teal mb-2">
                            스터디 현황
                        </h3>
                        <p className="text-study-text">
                            진행 중인 스터디: 3개
                        </p>
                    </div>

                    {/* 테스트 카드 3 */}
                    <div className="bg-white p-6 rounded-google shadow-sm border border-study-green/20">
                        <h3 className="text-xl font-semibold text-study-green mb-2">
                            완료율
                        </h3>
                        <p className="text-study-text">
                            이번 주: 85%
                        </p>
                    </div>
                </div>

                <div className="bg-white p-8 rounded-google shadow-sm">
                    <h3 className="text-2xl font-bold text-study-text mb-4">
                        사이드바 기능 테스트
                    </h3>
                    <ul className="space-y-2 text-study-text">
                        <li>✅ 사이드바 열기/닫기 토글</li>
                        <li>✅ Framer Motion 애니메이션</li>
                        <li>✅ 파스텔 컬러 테마 적용</li>
                        <li>✅ Material Icons 아이콘</li>
                        <li>✅ 배지 및 상태 표시</li>
                    </ul>
                </div>
            </div>
        </UserLayoutV2>
    );
};
