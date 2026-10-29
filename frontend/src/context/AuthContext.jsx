import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import * as authService from '../services/authService';
import { setSessionExpiredHandler } from '../services/api';
import { getAccessToken, getStoredUser, saveUser } from '../utils/storage';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const navigate = useNavigate();
  const [user, setUser] = useState(() => (getAccessToken() ? getStoredUser() : null));

  // Appelé par l'interceptor Axios lorsque le backend répond 401 et que la session ne peut plus être renouvelée
  useEffect(() => {
    setSessionExpiredHandler(() => {
      setUser(null);
      navigate('/login?expired=1', { replace: true });
    });
    return () => setSessionExpiredHandler(null);
  }, [navigate]);

  const login = useCallback(async (email, password) => {
    const data = await authService.login(email, password);
    setUser({ role: data.role, fullName: data.fullName, email: data.email });
    return data;
  }, []);

  const logout = useCallback(async () => {
    await authService.logout();
    setUser(null);
    navigate('/login', { replace: true });
  }, [navigate]);

  /** Met à jour le nom / l'email affichés après une modification du profil. */
  const updateUser = useCallback((changes) => {
    setUser((current) => {
      const next = { ...current, ...changes };
      saveUser(next);
      return next;
    });
  }, []);

  const value = useMemo(
    () => ({ user, isAuthenticated: Boolean(user), login, logout, updateUser }),
    [user, login, logout, updateUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth doit être utilisé dans un AuthProvider');
  }
  return context;
}
