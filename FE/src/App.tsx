import React, { Suspense, lazy } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';

const Login = lazy(() => import('./pages/Login'));
const Home = lazy(() => import('./pages/Home'));

const AuthLoadingScreen: React.FC = () => (
  <div className="min-h-screen flex items-center justify-center bg-neutral-50">
    <div className="flex flex-col items-center space-y-4">
      <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-primary-500" />
      <span className="text-sm text-neutral-500">Đang kiểm tra phiên đăng nhập...</span>
    </div>
  </div>
);

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();
  if (loading) return <AuthLoadingScreen />;
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
};

const PublicRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();
  if (loading) return <AuthLoadingScreen />;
  return isAuthenticated ? <Navigate to="/" replace /> : <>{children}</>;
};

const App: React.FC = () => {
  return (
    <Suspense fallback={<AuthLoadingScreen />}>
      <Routes>
        {/* Public Login Route */}
        <Route
          path="/login"
          element={
            <PublicRoute>
              <Login />
            </PublicRoute>
          }
        />

        {/* Protected Main Home Map Dashboard Route */}
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <Home />
            </ProtectedRoute>
          }
        />

        {/* Catch-all Route: Redirect to dashboard */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  );
};

export default App;
