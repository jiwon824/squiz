import React, { useState } from 'react';
import { X, User, CheckCircle2 } from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import { userApi } from '@/api/endpoints/userApi';

interface EditProfileModalProps {
    isOpen: boolean;
    onClose: () => void;
}

export const EditProfileModal: React.FC<EditProfileModalProps> = ({ isOpen, onClose }) => {
    const { user, updateUser } = useAuthStore();
    const [formData, setFormData] = useState({
        name: user?.name || '',
        nickname: user?.nickname || '',
        bio: user?.bio || '',
    });
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    if (!isOpen) return null;

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        const { name, value } = e.target;
        setFormData((prev) => ({ ...prev, [name]: value }));
        setError(null);
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        setIsLoading(true);
        try {
            const updatedUser = await userApi.updateProfile({
                name: formData.name,
                nickname: formData.nickname,
                bio: formData.bio
            });

            // 전역 상태 업데이트
            updateUser({
                name: updatedUser.name,
                nickname: updatedUser.nickname || undefined,
                bio: formData.bio // API 결과에 bio가 없다면 폼 데이터 사용
            });

            alert('프로필 정보가 수정되었습니다.');
            onClose();
        } catch (err: any) {
            console.error('Profile update error:', err);
            setError(err.response?.data?.message || '프로필 수정 중 오류가 발생했습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
            {/* Backdrop */}
            <div
                className="absolute inset-0 bg-black/40 backdrop-blur-sm transition-opacity"
                onClick={onClose}
            />

            {/* Modal */}
            <div className="relative w-full max-w-md bg-white rounded-3xl shadow-2xl overflow-hidden animate-in fade-in zoom-in duration-200">
                <div className="p-6">
                    <div className="flex items-center justify-between mb-6">
                        <h2 className="text-xl font-bold text-gray-900 flex items-center gap-2">
                            <User className="text-study-blue" size={24} />
                            프로필 편집
                        </h2>
                        <button
                            onClick={onClose}
                            className="p-2 hover:bg-gray-100 rounded-full transition-colors text-gray-400 hover:text-gray-600"
                        >
                            <X size={20} />
                        </button>
                    </div>

                    <form onSubmit={handleSubmit} className="space-y-4">
                        <div className="space-y-2">
                            <label className="text-sm font-bold text-gray-700 ml-1">실명 (고정)</label>
                            <input
                                type="text"
                                name="name"
                                value={formData.name}
                                readOnly
                                className="w-full p-3.5 bg-gray-100 border border-gray-200 rounded-2xl text-gray-500 cursor-not-allowed outline-none"
                                title="실명은 수정할 수 없습니다."
                            />
                            <p className="text-[11px] text-gray-400 ml-2">* 소셜 인증된 이름으로 고정됩니다.</p>
                        </div>

                        <div className="space-y-2">
                            <label className="text-sm font-bold text-gray-700 ml-1">닉네임</label>
                            <input
                                type="text"
                                name="nickname"
                                value={formData.nickname}
                                onChange={handleChange}
                                placeholder="사용하실 닉네임을 입력해주세요"
                                className="w-full p-3.5 bg-gray-50 border border-gray-200 rounded-2xl focus:ring-2 focus:ring-study-blue/20 focus:border-study-blue transition-all outline-none"
                            />
                        </div>

                        <div className="space-y-2">
                            <label className="text-sm font-bold text-gray-700 ml-1">한 줄 소개</label>
                            <textarea
                                name="bio"
                                value={formData.bio}
                                onChange={handleChange as any}
                                placeholder="자신을 한 줄로 표현해 보세요"
                                rows={2}
                                className="w-full p-3.5 bg-gray-50 border border-gray-200 rounded-2xl focus:ring-2 focus:ring-study-blue/20 focus:border-study-blue transition-all outline-none resize-none"
                            />
                        </div>

                        {error && (
                            <p className="text-sm font-medium text-red-500 ml-1 flex items-center gap-1">
                                <span>⚠️</span> {error}
                            </p>
                        )}

                        <div className="pt-4 flex gap-3">
                            <button
                                type="button"
                                onClick={onClose}
                                className="flex-1 p-3.5 bg-gray-100 hover:bg-gray-200 text-gray-700 font-bold rounded-2xl transition-all"
                            >
                                취소
                            </button>
                            <button
                                type="submit"
                                disabled={isLoading}
                                className="flex-1 p-3.5 bg-gradient-to-r from-study-blue to-study-blue-dark text-white font-bold rounded-2xl shadow-lg shadow-study-blue/20 hover:shadow-study-blue/30 hover:-translate-y-0.5 transition-all active:translate-y-0 disabled:opacity-50 flex items-center justify-center gap-2"
                            >
                                {isLoading ? (
                                    <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                                ) : (
                                    <>
                                        <CheckCircle2 size={18} />
                                        저장하기
                                    </>
                                )}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};
