import React, { useState } from 'react';
import { X, Key } from 'lucide-react';
import api from '../../../api/axiosInstance';

export interface AdminUser {
  id: number;
  username: string;
  fullName: string;
  role: 'ADMIN' | 'VIEWER';
}

interface EditUserModalProps {
  isOpen: boolean;
  onClose: () => void;
  targetUser: AdminUser | null;
  onSuccess: (msg: string) => void;
  onError: (msg: string) => void;
}

const EditUserModal: React.FC<EditUserModalProps> = ({
  isOpen,
  onClose,
  targetUser,
  onSuccess,
  onError,
}) => {
  // Initialize state directly from targetUser (since parent uses key={targetUser?.id} to re-mount)
  const [fullName, setFullName] = useState(targetUser?.fullName || '');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);

  if (!isOpen || !targetUser) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!fullName.trim()) {
      onError('Họ và tên không được để trống.');
      return;
    }
    if (password && password !== confirmPassword) {
      onError('Mật khẩu nhập lại không khớp.');
      return;
    }

    setLoading(true);
    try {
      await api.put(`/api/admin/users/${targetUser.id}`, {
        fullName: fullName.trim(),
        password: password || undefined,
      });
      onSuccess(`Cập nhật tài khoản "${targetUser.username}" thành công.`);
      onClose();
    } catch (err: unknown) {
      console.error(err);
      const errorMsg = err && typeof err === 'object' && 'response' in err
        ? ((err as { response?: { data?: { message?: string } } }).response?.data?.message)
        : null;
      onError(errorMsg || 'Có lỗi xảy ra khi cập nhật.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-neutral-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-white border border-neutral-200 rounded-2xl shadow-xl max-w-md w-full p-6 relative animate-zoomIn">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-1 text-neutral-400 hover:text-neutral-600 rounded-full hover:bg-neutral-100 cursor-pointer transition-all"
          disabled={loading}
        >
          <X size={18} />
        </button>
        <div className="flex items-center space-x-3 mb-6">
          <div className="w-10 h-10 rounded-xl bg-primary-50 flex items-center justify-center text-primary-500">
            <Key size={20} />
          </div>
          <div>
            <h3 className="text-lg font-bold text-neutral-900">Cập nhật tài khoản</h3>
            <p className="text-xs text-neutral-400 mt-0.5">@{targetUser.username}</p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-bold text-neutral-500 uppercase tracking-wider block">Họ và tên *</label>
            <input
              type="text"
              required
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="Họ và tên"
              className="w-full px-3 py-2.5 rounded-xl border border-neutral-200 bg-white text-sm text-neutral-950 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 transition-all"
              disabled={loading}
            />
          </div>

          <div className="space-y-1.5 pt-2">
            <p className="text-[10px] font-bold text-neutral-400 border-b border-neutral-100 pb-1 mb-2">
              Đổi mật khẩu (bỏ trống nếu giữ nguyên)
            </p>
            <label className="text-xs font-bold text-neutral-500 uppercase tracking-wider block">Mật khẩu mới</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Mật khẩu mới ít nhất 6 ký tự"
              className="w-full px-3 py-2.5 rounded-xl border border-neutral-200 bg-white text-sm text-neutral-950 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 transition-all"
              disabled={loading}
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-bold text-neutral-500 uppercase tracking-wider block">Nhập lại mật khẩu mới</label>
            <input
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="Xác nhận mật khẩu mới"
              className="w-full px-3 py-2.5 rounded-xl border border-neutral-200 bg-white text-sm text-neutral-950 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 transition-all"
              disabled={loading}
            />
          </div>

          <div className="flex justify-end space-x-3 pt-4 border-t border-neutral-100">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2.5 rounded-xl text-xs font-bold text-neutral-600 bg-neutral-100 hover:bg-neutral-200 cursor-pointer transition-all"
              disabled={loading}
            >
              Hủy bỏ
            </button>
            <button
              type="submit"
              className="px-4 py-2.5 rounded-xl text-xs font-bold text-white bg-primary-500 hover:bg-primary-600 active:scale-[0.98] shadow-sm shadow-primary-500/10 cursor-pointer transition-all disabled:opacity-50"
              disabled={loading}
            >
              {loading ? 'Đang lưu...' : 'Lưu thay đổi'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default React.memo(EditUserModal);
