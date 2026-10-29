import axios from 'axios';
import { clearSession, getAccessToken, getRefreshToken, saveTokens } from '../utils/storage';

export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: API_URL,
  headers: { 'Content-Type': 'application/json' },
});

/** Les endpoints /auth/** sont publics : on n'y joint pas de JWT et un 401 n'y signifie pas "session expirée". */
function isAuthEndpoint(url = '') {
  return url.startsWith('/auth/');
}

// --- Interceptor de requête : ajoute "Authorization: Bearer <JWT>" aux requêtes protégées ---------------
api.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token && !isAuthEndpoint(config.url)) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// --- Interceptor de réponse : gestion du 401 (expiration du JWT) ----------------------------------------
let sessionExpiredHandler = null;

/** Enregistré par AuthContext : met à jour l'état React puis redirige vers /login. */
export function setSessionExpiredHandler(handler) {
  sessionExpiredHandler = handler;
}

function handleSessionExpired() {
  clearSession(); // 1. supprimer le JWT
  if (sessionExpiredHandler) {
    sessionExpiredHandler(); // 2. et 3. rediriger vers Login avec le message "session expirée"
  } else {
    window.location.assign('/login?expired=1');
  }
}

let refreshPromise = null;

/** Un seul renouvellement à la fois : les requêtes qui échouent en parallèle partagent la même promesse
 *  (les refresh tokens sont à usage unique, un deuxième appel simultané provoquerait une déconnexion). */
function refreshAccessToken() {
  if (!refreshPromise) {
    const refreshToken = getRefreshToken();
    // axios "nu" pour ne pas repasser par les interceptors
    refreshPromise = axios
      .post(`${API_URL}/auth/refresh`, { refreshToken })
      .then((response) => {
        saveTokens(response.data);
        return response.data.token;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    const status = error.response?.status;

    if (status === 401 && original && !isAuthEndpoint(original.url)) {
      // Première tentative : renouveler l'access token avec le refresh token, puis rejouer la requête
      if (!original._retried && getRefreshToken()) {
        original._retried = true;
        try {
          const newToken = await refreshAccessToken();
          original.headers.Authorization = `Bearer ${newToken}`;
          return api(original);
        } catch {
          // refresh impossible (expiré, révoqué, compte désactivé) : on termine la session ci-dessous
        }
      }
      handleSessionExpired();
    }
    return Promise.reject(error);
  },
);

export default api;
