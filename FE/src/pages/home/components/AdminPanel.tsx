import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../../../context/AuthContext';
import api from '../../../api/axiosInstance';
import { Users, Plus, AlertCircle, Info, Shield, Edit2, Trash2 } from 'lucide-react';
import AddUserModal from './AddUserModal';
import EditUserModal, { type AdminUser } from './EditUserModal';
import DeleteUserModal from './DeleteUserModal';

interface AdminPanelProps {
  setActiveView: (view: 'map' | 'admin') => void;
}

const AdminPanel: React.FC<AdminPanelProps> = ({ setActiveView }) => {
  const { user } = useAuth();
  
  // Local state for admin panel with proper typing
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [usersLoading, setUsersLoading] = useState(false);
  const [adminError, setAdminError] = useState<string | null>(null);
  const [adminSuccess, setAdminSuccess] = useState<string | null>(null);

  // Modals visibility state
  const [showAddModal, setShowAddModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [targetUser, setTargetUser] = useState<AdminUser | null>(null);

  // Fetch Users list from API
  const fetchUsers = useCallback(async () => {
    if (user?.role !== 'ADMIN') return;
    setUsersLoading(true);
    setAdminError(null);
    try {
      const response = await api.get('/api/admin/users');
      setUsers(response.data);
    } catch (err) {
      console.error('Failed to load users list', err);
      setAdminError('Không thể tải danh sách tài khoản từ máy chủ.');
    } finally {
      setUsersLoading(false);
    }
  }, [user]);

  // Load users on mount (deferred to next tick to avoid eslint set-state-in-effect warnings)
  useEffect(() => {
    let mounted = true;
    const deferFetch = async () => {
      await Promise.resolve();
      if (mounted) {
        fetchUsers();
      }
    };
    deferFetch();
    return () => {
      mounted = false;
    };
  }, [fetchUsers]);

  // Alert callbacks passed to modals
  const handleSuccess = useCallback((message: string) => {
    setAdminSuccess(message);
    setAdminError(null);
    fetchUsers();
  }, [fetchUsers]);

  const handleError = useCallback((message: string) => {
    setAdminError(message);
    setAdminSuccess(null);
  }, []);

  return (
    <div className="w-full h-full bg-neutral-50 overflow-y-auto z-20 flex flex-col p-6 sm:p-10 pt-24">
      <div className="w-full max-w-5xl mx-auto bg-white border border-neutral-200 rounded-2xl shadow-sm p-6 sm:p-8">
        
        {/* Header */}
        <div className="flex justify-between items-center mb-8 pb-4 border-b border-neutral-100">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-primary-50 flex items-center justify-center text-primary-500">
              <Users size={20} />
            </div>
            <div>
              <h3 className="text-xl font-bold text-neutral-900">Quản lý người dùng</h3>
              <p className="text-xs text-neutral-400 mt-0.5">
                Danh sách các tài khoản Viewer được phân quyền trong hệ thống
              </p>
            </div>
          </div>
          <div className="flex items-center space-x-3">
            <button
              onClick={() => {
                setAdminError(null);
                setAdminSuccess(null);
                setShowAddModal(true);
              }}
              className="py-2.5 px-4 rounded-xl bg-primary-500 hover:bg-primary-600 active:scale-[0.98] text-white text-xs font-semibold flex items-center space-x-1.5 transition-all shadow-sm shadow-primary-500/10 cursor-pointer"
            >
              <Plus size={16} />
              <span>Thêm tài khoản</span>
            </button>
            <button
              onClick={() => setActiveView('map')}
              className="py-2.5 px-4 rounded-xl bg-neutral-100 hover:bg-neutral-200 text-neutral-700 text-xs font-semibold cursor-pointer transition-all"
            >
              Quay lại bản đồ
            </button>
          </div>
        </div>

        {/* Status Message Banners */}
        {adminError && (
          <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 text-xs sm:text-sm font-semibold flex items-center space-x-2">
            <AlertCircle size={18} className="shrink-0" />
            <span>{adminError}</span>
          </div>
        )}
        {adminSuccess && (
          <div className="mb-6 p-4 rounded-xl bg-green-50 border border-green-200 text-green-700 text-xs sm:text-sm font-semibold flex items-center space-x-2">
            <Info size={18} className="shrink-0" />
            <span>{adminSuccess}</span>
          </div>
        )}

        {/* Users Table */}
        <div className="overflow-x-auto border border-neutral-100 rounded-xl">
          <table className="w-full text-sm text-left text-neutral-500">
            <thead className="text-xs text-neutral-700 uppercase bg-neutral-50 border-b border-neutral-100">
              <tr>
                <th className="px-6 py-4">Tên đăng nhập</th>
                <th className="px-6 py-4">Họ và tên</th>
                <th className="px-6 py-4">Vai trò</th>
                <th className="px-6 py-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-100">
              {usersLoading ? (
                <tr>
                  <td colSpan={4} className="px-6 py-10 text-center text-neutral-400 font-semibold">
                    <div className="flex justify-center items-center space-x-2">
                      <div className="animate-spin rounded-full h-5 w-5 border-t-2 border-b-2 border-primary-500"></div>
                      <span>Đang tải danh sách...</span>
                    </div>
                  </td>
                </tr>
              ) : users.length === 0 ? (
                <tr>
                  <td colSpan={4} className="px-6 py-10 text-center text-neutral-400 font-semibold">
                    Chưa có tài khoản Viewer nào.
                  </td>
                </tr>
              ) : (
                users.map((item) => (
                  <tr key={item.id} className="bg-white hover:bg-neutral-50/50 transition-colors">
                    <td className="px-6 py-4 font-semibold text-neutral-900">@{item.username}</td>
                    <td className="px-6 py-4">{item.fullName}</td>
                    <td className="px-6 py-4">
                      <span className={`inline-flex items-center space-x-1 text-[10px] font-bold px-2.5 py-0.5 rounded-full border ${
                        item.role === 'ADMIN' ? 'bg-red-50 text-red-700 border-red-200' : 'bg-primary-50 text-primary-700 border-primary-100'
                      }`}>
                        <Shield size={10} className="shrink-0" />
                        <span>{item.role}</span>
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right flex justify-end space-x-2">
                      <button
                        onClick={() => {
                          setTargetUser(item);
                          setAdminError(null);
                          setAdminSuccess(null);
                          setShowEditModal(true);
                        }}
                        className="p-2 text-neutral-400 hover:text-neutral-900 rounded-lg hover:bg-neutral-100 cursor-pointer transition-all"
                        title="Chỉnh sửa thông tin"
                      >
                        <Edit2 size={16} />
                      </button>
                      {item.username !== user?.username && (
                        <button
                          onClick={() => {
                            setTargetUser(item);
                            setAdminError(null);
                            setAdminSuccess(null);
                            setShowDeleteModal(true);
                          }}
                          className="p-2 text-neutral-400 hover:text-red-600 rounded-lg hover:bg-red-50 cursor-pointer transition-all"
                          title="Xóa tài khoản"
                        >
                          <Trash2 size={16} />
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* CRUD Modals */}
      <AddUserModal
        isOpen={showAddModal}
        onClose={() => setShowAddModal(false)}
        onSuccess={handleSuccess}
        onError={handleError}
      />

      <EditUserModal
        key={targetUser?.id || 'none'}
        isOpen={showEditModal}
        onClose={() => {
          setShowEditModal(false);
          setTargetUser(null);
        }}
        targetUser={targetUser}
        onSuccess={handleSuccess}
        onError={handleError}
      />

      <DeleteUserModal
        isOpen={showDeleteModal}
        onClose={() => {
          setShowDeleteModal(false);
          setTargetUser(null);
        }}
        targetUser={targetUser}
        onSuccess={handleSuccess}
        onError={handleError}
      />
    </div>
  );
};

export default React.memo(AdminPanel);
