/* eslint-disable react-refresh/only-export-components */
import React, { createContext, useContext, useState, useEffect } from 'react';
import api from '../api/axiosInstance';

export interface User {
  username: string;
  fullName: string;
  role: 'ADMIN' | 'VIEWER';
}

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  login: (user: User) => void;
  logout: () => void;
  loading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    /*
     * Bảo mật JWT: token nằm trong cookie HttpOnly nên JS không đọc được,
     * do đó không thể tự khôi phục phiên đăng nhập bằng cách đọc localStorage
     * như trước. Thay vào đó, gọi /api/auth/me — trình duyệt sẽ tự đính kèm
     * cookie (nhờ withCredentials: true trong axiosInstance), BE xác thực JWT
     * và trả về thông tin user hiện tại nếu cookie còn hợp lệ.
     */
    let cancelled = false;

    api
      .get('/api/auth/me')
      .then((response) => {
        if (!cancelled) {
          setUser(response.data);
        }
      })
      .catch(() => {
        // Không có cookie hợp lệ (chưa đăng nhập hoặc token hết hạn) -> giữ user = null
        if (!cancelled) {
          setUser(null);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const login = (newUser: User) => {
    // Token đã được BE set qua Set-Cookie (HttpOnly) ngay trong response của
    // /api/auth/login — FE chỉ cần lưu thông tin user (không nhạy cảm) để
    // hiển thị UI, không còn giữ token ở đây nữa.
    setUser(newUser);
  };

  const logout = () => {
    // Chờ BE xóa cookie HttpOnly (Max-Age=0) rồi mới clear state, tránh
    // trường hợp cookie cũ vẫn còn hiệu lực do request logout thất bại.
    api
      .post('/api/auth/logout')
      .catch(() => {})
      .finally(() => {
        setUser(null);
      });
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        login,
        logout,
        loading,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
