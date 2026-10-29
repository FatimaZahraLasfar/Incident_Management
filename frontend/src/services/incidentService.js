import api from './api';

export const createIncident = (payload) => api.post('/api/incidents', payload).then((r) => r.data);

export const getMyIncidents = () => api.get('/api/incidents/my').then((r) => r.data);

export const getAssignedIncidents = () => api.get('/api/incidents/assigned').then((r) => r.data);

export const getAllIncidents = () => api.get('/api/incidents').then((r) => r.data);

export const getIncident = (id) => api.get(`/api/incidents/${id}`).then((r) => r.data);

export const updateStatus = (id, status) =>
  api.put(`/api/incidents/${id}/status`, { status }).then((r) => r.data);

export const assignIncident = (id, technicianId) =>
  api.put(`/api/incidents/${id}/assign/${technicianId}`).then((r) => r.data);

export const addComment = (id, content) =>
  api.post(`/api/incidents/${id}/comments`, { content }).then((r) => r.data);

/** Tableau de bord adapté au rôle : { role, stats: {...}, recentIncidents: [...] }. */
export const getDashboard = () => api.get('/api/dashboard').then((r) => r.data);
