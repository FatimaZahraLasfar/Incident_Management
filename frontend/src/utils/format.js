export const STATUSES = ['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
export const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
export const CATEGORIES = [
  'NETWORK',
  'MALWARE',
  'PHISHING',
  'ACCESS',
  'SOFTWARE',
  'HARDWARE',
  'DATA_LEAK',
  'OTHER',
];

export const STATUS_LABELS = {
  OPEN: 'Ouvert',
  ASSIGNED: 'Affecté',
  IN_PROGRESS: 'En cours',
  RESOLVED: 'Résolu',
  CLOSED: 'Clôturé',
};

export const PRIORITY_LABELS = {
  LOW: 'Faible',
  MEDIUM: 'Moyenne',
  HIGH: 'Élevée',
  CRITICAL: 'Critique',
};

export const CATEGORY_LABELS = {
  NETWORK: 'Réseau',
  MALWARE: 'Malware',
  PHISHING: 'Phishing',
  ACCESS: 'Accès au compte',
  SOFTWARE: 'Logiciel',
  HARDWARE: 'Matériel',
  DATA_LEAK: 'Fuite de données',
  OTHER: 'Autre',
};

export const ROLE_LABELS = {
  EMPLOYEE: 'Employé',
  TECHNICIAN: 'Technicien',
  ADMIN: 'Administrateur',
};

export function formatDate(value) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' });
}

/**
 * Transitions de statut proposées dans l'interface. C'est un confort d'usage uniquement :
 * le backend reste seul juge des transitions autorisées.
 */
const TECHNICIAN_TRANSITIONS = {
  OPEN: ['IN_PROGRESS'],
  ASSIGNED: ['IN_PROGRESS'],
  IN_PROGRESS: ['RESOLVED'],
};

const ADMIN_TRANSITIONS = {
  ...TECHNICIAN_TRANSITIONS,
  RESOLVED: ['CLOSED', 'IN_PROGRESS'],
  CLOSED: ['IN_PROGRESS'],
};

export function allowedTransitions(role, status) {
  const table = role === 'ADMIN' ? ADMIN_TRANSITIONS : role === 'TECHNICIAN' ? TECHNICIAN_TRANSITIONS : {};
  return table[status] || [];
}
