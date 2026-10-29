import api from './api';

export const getUsers = () => api.get('/api/admin/users').then((r) => r.data);

export const enableUser = (id) => api.put(`/api/admin/users/${id}/enable`).then((r) => r.data);

export const disableUser = (id) => api.put(`/api/admin/users/${id}/disable`).then((r) => r.data);

export const changeUserRole = (id, role) =>
  api.put(`/api/admin/users/${id}/role`, { role }).then((r) => r.data);

export const getStatistics = () => api.get('/api/admin/statistics').then((r) => r.data);
