import api from './api';
import { clearSession, getRefreshToken, saveSession } from '../utils/storage';

/** POST /auth/login -> { token, refreshToken, role, fullName, email } (enregistré dans localStorage). */
export async function login(email, password) {
  const { data } = await api.post('/auth/login', { email, password });
  saveSession(data);
  return data;
}

/** POST /auth/register : le rôle est attribué par le backend (EMPLOYEE), il n'est jamais envoyé. */
export async function register(fullName, email, password) {
  const { data } = await api.post('/auth/register', { fullName, email, password });
  return data;
}

/** Révoque le refresh token côté serveur puis efface la session locale, même si l'appel échoue. */
export async function logout() {
  const refreshToken = getRefreshToken();
  try {
    if (refreshToken) {
      await api.post('/auth/logout', { refreshToken });
    }
  } catch {
    // la déconnexion locale doit toujours aboutir
  } finally {
    clearSession();
  }
}
