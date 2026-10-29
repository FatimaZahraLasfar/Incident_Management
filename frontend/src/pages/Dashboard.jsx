import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { getDashboard } from '../services/incidentService';
import { getStatistics } from '../services/adminService';
import StatCard from '../components/StatCard';
import IncidentTable from '../components/IncidentTable';
import Loader from '../components/Loader';
import ErrorMessage from '../components/ErrorMessage';
import { CATEGORY_LABELS, PRIORITY_LABELS } from '../utils/format';
import { getErrorMessage } from '../utils/errors';

// [clé renvoyée par /api/dashboard, libellé, couleur]
const CARDS = {
  EMPLOYEE: [
    ['total', 'Mes incidents', 'default'],
    ['open', 'Ouverts', 'warning'],
    ['assigned', 'Affectés', 'default'],
    ['inProgress', 'En cours', 'info'],
    ['resolvedOrClosed', 'Résolus ou clôturés', 'success'],
  ],
  TECHNICIAN: [
    ['assigned', 'Incidents affectés', 'default'],
    ['critical', 'Critiques en attente', 'danger'],
    ['inProgress', 'En cours', 'info'],
    ['resolved', 'Résolus', 'success'],
  ],
  ADMIN: [
    ['totalUsers', 'Utilisateurs', 'default'],
    ['employees', 'Employés', 'default'],
    ['technicians', 'Techniciens', 'default'],
    ['totalIncidents', 'Incidents', 'default'],
    ['open', 'Ouverts', 'warning'],
    ['inProgress', 'En cours', 'info'],
    ['closed', 'Clôturés', 'success'],
  ],
};

function Breakdown({ title, data, labels }) {
  return (
    <div className="card">
      <h3>{title}</h3>
      <ul className="breakdown">
        {Object.entries(data).map(([key, count]) => (
          <li key={key}><span>{labels[key] || key}</span><strong>{count}</strong></li>
        ))}
      </ul>
    </div>
  );
}

export default function Dashboard() {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [stats, setStats] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const dashboard = await getDashboard();
        if (cancelled) return;
        setData(dashboard);
        if (dashboard.role === 'ADMIN') {
          const statistics = await getStatistics();
          if (!cancelled) setStats(statistics);
        }
      } catch (e) {
        if (!cancelled) setError(getErrorMessage(e));
      }
    }
    load();
    return () => { cancelled = true; };
  }, []);

  return (
    <div>
      <h1>Bonjour {user.fullName}</h1>
      <ErrorMessage message={error} />
      {!data && !error && <Loader />}
      {data && (
        <>
          <div className="stats-grid">
            {(CARDS[data.role] || []).map(([key, label, tone]) => (
              <StatCard key={key} label={label} value={data.stats[key] ?? 0} tone={tone} />
            ))}
          </div>

          {stats && (
            <div className="two-cols">
              <Breakdown title="Incidents par catégorie" data={stats.byCategory} labels={CATEGORY_LABELS} />
              <Breakdown title="Incidents par priorité" data={stats.byPriority} labels={PRIORITY_LABELS} />
            </div>
          )}

          <div className="card">
            <h2>{data.role === 'TECHNICIAN' ? 'Derniers incidents affectés' : 'Derniers incidents'}</h2>
            <IncidentTable
              incidents={data.recentIncidents}
              filters={false}
              showCreator={data.role !== 'EMPLOYEE'}
              showAssignee={data.role !== 'TECHNICIAN'}
            />
          </div>
        </>
      )}
    </div>
  );
}
