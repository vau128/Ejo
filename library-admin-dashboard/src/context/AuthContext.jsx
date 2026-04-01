import { createContext, useContext, useMemo, useState } from 'react';
import { loginAdmin } from '../api/authApi';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => window.localStorage.getItem('admin_token'));
  const [user, setUser] = useState(() => {
    const saved = window.localStorage.getItem('admin_user');
    return saved ? JSON.parse(saved) : null;
  });

  const login = async (credentials) => {
    const response = await loginAdmin(credentials);
    window.localStorage.setItem('admin_token', response.token);
    window.localStorage.setItem('admin_user', JSON.stringify(response.user));
    setToken(response.token);
    setUser(response.user);
    return response;
  };

  const logout = () => {
    window.localStorage.removeItem('admin_token');
    window.localStorage.removeItem('admin_user');
    setToken(null);
    setUser(null);
  };

  const value = useMemo(
    () => ({
      token,
      user,
      isAuthenticated: Boolean(token),
      login,
      logout,
    }),
    [token, user]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
