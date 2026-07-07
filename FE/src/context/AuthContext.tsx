/* eslint-disable react-refresh/only-export-components */
import React, { createContext, useContext, useState, useEffect } from 'react';

export interface User {
  username: string;
  fullName: string;
  role: 'ADMIN' | 'VIEWER';
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (token: string, user: User) => void;
  logout: () => void;
  loading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Load auth info from localStorage on mount
    const savedToken = localStorage.getItem('gis_token');
    const savedUser = localStorage.getItem('gis_user');

    if (savedToken && savedUser) {
      try {
        const parsedUser = JSON.parse(savedUser);
        // Defer states to avoid synchronous set-state-in-effect warning
        Promise.resolve().then(() => {
          setToken(savedToken);
          setUser(parsedUser);
          setLoading(false);
        });
        return;
      } catch (e) {
        console.error('Failed to parse saved user', e);
        localStorage.removeItem('gis_token');
        localStorage.removeItem('gis_user');
      }
    }
    // Defer loading update to avoid synchronous set-state-in-effect warning
    Promise.resolve().then(() => {
      setLoading(false);
    });
  }, []);

  const login = (newToken: string, newUser: User) => {
    setToken(newToken);
    setUser(newUser);
    localStorage.setItem('gis_token', newToken);
    localStorage.setItem('gis_user', JSON.stringify(newUser));
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem('gis_token');
    localStorage.removeItem('gis_user');
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token,
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
