import React, { useState } from 'react';
import { X, Trash2 } from 'lucide-react';
import api from '../../../api/axiosInstance';
import type { AdminUser } from './EditUserModal';

interface DeleteUserModalProps {
  isOpen: boolean;
  onClose: () => void;
  targetUser: AdminUser | null;
  onSuccess: (msg: string) => void;
  onError: (msg: string) => void;
}

const DeleteUserModal: React.FC<DeleteUserModalProps> = ({
  isOpen,
  onClose,
  targetUser,
  onSuccess,
  onError,
}) => {
  const [loading, setLoading] = useState(false);

  if (!isOpen || !targetUser) return null;

  const handleDelete = async () => {
    setLoading(true);
    try {
      await api.delete(`/api/admin/users/${targetUser.id}`);
      onSuccess(`Đã xóa tài khoản "${targetUser.username}" thành công.`);
      onClose();
    } catch (err: unknown) {
      console.error(err);
      const errorMsg = err && typeof err === 'object' && 'response' in err
        ? ((err as { response?: { data?: string } }).response?.data)
        : null;
      onError(errorMsg || 'Không thể xóa tài khoản này.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-neutral-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-white border border-neutral-200 rounded-2xl shadow-xl max-w-sm w-full p-6 relative animate-zoomIn">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-1 text-neutral-400 hover:text-neutral-600 rounded-full hover:bg-neutral-100 cursor-pointer transition-all"
          disabled={loading}
        >
          <X size={18} />
        </button>
        <div className="flex flex-col items-center text-center space-y-4 mb-6">
          <div className="w-12 h-12 rounded-full bg-red-50 flex items-center justify-center text-red-600">
            <Trash2 size={24} />
          </div>
          <div>
            <h3 className="text-lg font-bold text-neutral-900">Xác nhận xóa tài khoản</h3>
            <p className="text-xs text-neutral-500 mt-2">
              Bạn có chắc chắn muốn xóa vĩnh viễn tài khoản <strong className="text-neutral-950">@{targetUser.username}</strong> ({targetUser.fullName})? Hành động này không thể hoàn tác.
            </p>
          </div>
        </div>

        <div className="flex justify-stretch space-x-3">
          <button
            onClick={onClose}
            className="flex-1 py-2.5 rounded-xl text-xs font-bold text-neutral-700 bg-neutral-100 hover:bg-neutral-200 cursor-pointer transition-all"
            disabled={loading}
          >
            Hủy bỏ
          </button>
          <button
            onClick={handleDelete}
            className="flex-1 py-2.5 rounded-xl text-xs font-bold text-white bg-red-600 hover:bg-red-700 active:scale-[0.98] cursor-pointer transition-all disabled:opacity-50"
            disabled={loading}
          >
            {loading ? 'Đang xóa...' : 'Đồng ý xóa'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default React.memo(DeleteUserModal);
