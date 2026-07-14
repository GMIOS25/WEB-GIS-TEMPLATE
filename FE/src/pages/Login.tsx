import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../api/axiosInstance';
import { Lock, User, Eye, EyeOff, Loader2, Map } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const Login: React.FC = () => {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  // Form states
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  
  // UI states
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Redirect if already logged in
  useEffect(() => {
    if (isAuthenticated) {
      navigate('/');
    }
  }, [isAuthenticated, navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password.trim()) {
      setError('Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.');
      return;
    }

    setError(null);
    setIsLoading(true);

    /* ponytail: 
       Basic form control with useState instead of React Hook Form.
       Ceiling: Basic validation. Upgrade path: Integrations with validation libraries if inputs scale.
    */
    try {
      const response = await api.post('/api/auth/login', {
        username,
        password,
      });

      const { username: retUsername, fullName, role } = response.data;

      // JWT đã được BE set qua cookie HttpOnly kèm response này (Set-Cookie).
      // AuthContext chỉ cần lưu thông tin user để hiển thị UI.
      login({ username: retUsername, fullName, role });
      
      // Redirect to homepage
      navigate('/');
    } catch (err: unknown) {
      console.error('Login error', err);
      const errorResponse = err as { response?: { status: number; data?: { message?: string } }; request?: unknown };
      if (errorResponse.response) {
        if (errorResponse.response.status === 401) {
          setError('Tên đăng nhập hoặc mật khẩu không chính xác.');
        } else {
          setError(errorResponse.response.data?.message || 'Có lỗi xảy ra từ máy chủ.');
        }
      } else if (errorResponse.request) {
        setError('Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại kết nối mạng.');
      } else {
        setError('Đã xảy ra lỗi không xác định.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-center p-6 bg-white">
      
      {/* Brand Icon Box (Board [77x77] #10b981) */}
      <div className="w-16 h-16 sm:w-[77px] sm:h-[77px] rounded-2xl bg-primary-500 text-white flex items-center justify-center shadow-sm mb-4">
        <Map className="w-8 h-8 sm:w-10 sm:h-10 stroke-[2]" />
      </div>

      {/* Main Title above Card (Hệ thống bản đồ địa giới GIS. [601x50]) */}
      <h1 className="text-xl sm:text-2xl font-semibold text-neutral-900 text-center tracking-tight mb-8">
        Hệ thống bản đồ địa giới GIS.
      </h1>

      {/* Login Card (Board [601x532] #d6d6d6 at 0.3 opacity) */}
      <div className="w-full max-w-[540px] bg-neutral-200/30 border border-neutral-200 rounded-2xl p-8 sm:p-10 shadow-sm">
        
        {/* Card Title (login [601x50]) */}
        <h2 className="text-2xl font-semibold text-neutral-900 text-center mb-8">
          Đăng nhập
        </h2>

        {error && (
          <div className="mb-6 p-3 rounded-lg bg-red-50/80 border border-red-200 text-red-600 text-xs sm:text-sm font-medium">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Username label & input block */}
          <div className="space-y-2">
            <label className="text-sm font-semibold text-neutral-900 block">
              Tên đăng nhập
            </label>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-neutral-400">
                <User size={18} />
              </span>
              <input
                type="text"
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="Nhập tên đăng nhập"
                disabled={isLoading}
                className="w-full pl-10 pr-4 py-2.5 rounded-lg border border-neutral-300 bg-white text-neutral-900 placeholder-neutral-400 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 transition-all text-sm"
              />
            </div>
          </div>

          {/* Password label & input block */}
          <div className="space-y-2">
            <label className="text-sm font-semibold text-neutral-900 block">
              Mật khẩu
            </label>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-neutral-400">
                <Lock size={18} />
              </span>
              <input
                type={showPassword ? 'text' : 'password'}
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Nhập mật khẩu"
                disabled={isLoading}
                className="w-full pl-10 pr-10 py-2.5 rounded-lg border border-neutral-300 bg-white text-neutral-900 placeholder-neutral-400 focus:outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 transition-all text-sm"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute inset-y-0 right-0 pr-3 flex items-center text-neutral-400 hover:text-neutral-600"
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </div>

          {/* Login button (login_btn [518x59] #10b981) */}
          <button
            type="submit"
            disabled={isLoading}
            className="w-full py-3 px-4 rounded-lg font-semibold text-white bg-primary-500 hover:bg-primary-600 active:scale-[0.99] transition-all flex items-center justify-center space-x-2 text-sm disabled:opacity-50 disabled:pointer-events-none cursor-pointer mt-8"
          >
            {isLoading ? (
              <>
                <Loader2 size={18} className="animate-spin" />
                <span>Đang xử lý...</span>
              </>
            ) : (
              <span>Đăng nhập</span>
            )}
          </button>
        </form>
      </div>

      {/* Footer copyright (@2026 - GPHI) */}
      <div className="mt-12 text-sm font-semibold text-neutral-900 text-center">
        @2026 - GPHI
      </div>
    </div>
  );
};

export default Login;
