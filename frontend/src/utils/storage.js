// Stockage de la session dans le navigateur (localStorage).
// Limite connue : localStorage est lisible par tout script de la page (risque XSS). Le cahier des charges
// impose ce stockage ; React échappe par défaut le contenu affiché, ce qui limite ce risque.

const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';
const USER_KEY = 'user';

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function getStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null');
  } catch {
    return null;
  }
}

/** Enregistre la réponse de /auth/login : { token, refreshToken, role, fullName, email }. */
export function saveSession({ token, refreshToken, role, fullName, email }) {
  localStorage.setItem(ACCESS_TOKEN_KEY, token);
  if (refreshToken) {
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  }
  localStorage.setItem(USER_KEY, JSON.stringify({ role, fullName, email }));
}

/** Remplace les jetons après un /auth/refresh (le rôle et le nom restent inchangés). */
export function saveTokens({ token, refreshToken }) {
  localStorage.setItem(ACCESS_TOKEN_KEY, token);
  if (refreshToken) {
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  }
}

export function saveAccessToken(token) {
  localStorage.setItem(ACCESS_TOKEN_KEY, token);
}

export function saveUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearSession() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}
