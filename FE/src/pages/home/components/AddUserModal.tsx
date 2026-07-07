import React, { useState } from 'react';
import { X, Plus } from 'lucide-react';
import api from '../../../api/axiosInstance';

interface AddUserModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (msg: string) => void;
  onError: (msg: string) => void;
}

const AddUserModal: React.FC<AddUserModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  onError,
}) => {
  const [username, setUsername] = useState('');
  const [fullName, setFullName] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !fullName.trim() || !password.trim()) {
      onError('Vui lòng điền đầy đủ các thông tin bắt buộc.');
      return;
    }

    setLoading(true);
    try {
      await api.post('/api/admin/users', {
        username: username.trim(),
        fullName: fullName.trim(),
        password: password.trim(),
      });
      onSuccess(`Đã tạo tài khoản "${username}" thành công.`);
      setUsername('');
      setFullName('');
      setPassword('');
      onClose();
    } catch (err: unknown) {
      console.error(err);
      const errorMsg = err && typeof err === 'object' && 'response' in err
        ? ((err as { response?: { data?: string } }).response?.data)
        : null;
      onError(errorMsg || 'Có lỗi xảy ra khi tạo tài khoản.');
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
            <Plus size={20} />
          </div>
          <h3 className="text-lg font-bold text-neutral-900">Tạo tài khoản mới</h3>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-bold text-neutral-500 uppercase tracking-wider block">Tên đăng nhập *</label>
            <input
              type="text"
              required
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Nhập tên đăng nhập (ví dụ: viewer_le)"
              className="w-full px-3 py-2.5 rounded-xl border border-neutral-200 bg-white text-sm text-neutral-950 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 transition-all"
              disabled={loading}
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-bold text-neutral-500 uppercase tracking-wider block">Họ và tên *</label>
            <input
              type="text"
              required
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="Nhập họ và tên hiển thị"
              className="w-full px-3 py-2.5 rounded-xl border border-neutral-200 bg-white text-sm text-neutral-950 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 transition-all"
              disabled={loading}
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-bold text-neutral-500 uppercase tracking-wider block">Mật khẩu ban đầu *</label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Mật khẩu ít nhất 6 ký tự"
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
              {loading ? 'Đang tạo...' : 'Tạo tài khoản'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default React.memo(AddUserModal);
