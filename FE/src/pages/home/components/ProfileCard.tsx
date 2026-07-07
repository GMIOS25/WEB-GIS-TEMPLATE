import React, { useState } from 'react';
import { useAuth } from '../../../context/AuthContext';
import { User as UserIcon, LogOut, Shield } from 'lucide-react';

const ProfileCard: React.FC = () => {
  const { user, logout } = useAuth();
  const [showProfileDropdown, setShowProfileDropdown] = useState(false);

  return (
    <div className="absolute top-6 right-6 z-30">
      <button
        onClick={() => setShowProfileDropdown(!showProfileDropdown)}
        aria-label="Menu tài khoản"
        className="bg-white border border-neutral-200 hover:border-neutral-300 rounded-xl px-5 py-3 shadow-sm hover:shadow-md flex items-center justify-between space-x-3 w-[240px] cursor-pointer transition-all duration-200"
      >
        <span className="text-sm font-bold text-neutral-900 truncate">
          {user?.fullName || 'Tên người dùng'}
        </span>
        <div className="w-8 h-8 rounded-full bg-neutral-50 flex items-center justify-center border border-neutral-200 text-neutral-600 shrink-0 shadow-inner">
          <UserIcon size={16} />
        </div>
      </button>

      {/* Profile Dropdown Menu */}
      {showProfileDropdown && (
        <div className="absolute right-0 mt-2 w-[240px] bg-white border border-neutral-200 rounded-xl shadow-lg p-3 z-50 animate-fadeIn">
          <div className="px-3 py-2.5 border-b border-neutral-100 mb-2">
            <p className="text-[10px] font-bold text-neutral-400 uppercase tracking-wider">Tài khoản</p>
            <p className="text-sm font-semibold text-neutral-900 truncate mt-0.5">@{user?.username}</p>
            <div className="flex items-center space-x-1.5 mt-1.5">
              <Shield size={12} className="text-primary-500" />
              <span className="text-[10px] font-bold text-neutral-500 uppercase tracking-wider">{user?.role}</span>
            </div>
          </div>
          <button
            onClick={logout}
            className="w-full text-left px-3 py-2 rounded-lg text-sm text-red-600 hover:bg-red-50 flex items-center space-x-2 transition-all cursor-pointer font-medium"
          >
            <LogOut size={16} />
            <span>Đăng xuất</span>
          </button>
        </div>
      )}
    </div>
  );
};

export default ProfileCard;
