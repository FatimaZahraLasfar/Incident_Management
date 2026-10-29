import api from './api';
import { saveAccessToken, saveTokens } from '../utils/storage';

export const getProfile = () => api.get('/api/profile').then((r) => r.data);

/** PUT /api/profile -> { user, token } : le nouveau token tient compte d'un éventuel changement d'email. */
export async function updateProfile(fullName, email) {
  const { data } = await api.put('/api/profile', { fullName, email });
  saveAccessToken(data.token);
  return data.user;
}

/** PUT /api/profile/password -> nouvelle session { token, refreshToken, ... } : les anciennes sont révoquées. */
export async function changePassword(oldPassword, newPassword, confirmPassword) {
  const { data } = await api.put('/api/profile/password', { oldPassword, newPassword, confirmPassword });
  saveTokens(data);
  return data;
}
