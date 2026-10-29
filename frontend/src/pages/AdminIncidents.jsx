import { useEffect, useState } from 'react';
import { getAllIncidents } from '../services/incidentService';
import IncidentTable from '../components/IncidentTable';
import Loader from '../components/Loader';
import ErrorMessage from '../components/ErrorMessage';
import { getErrorMessage } from '../utils/errors';

export default function AdminIncidents() {
  const [incidents, setIncidents] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    getAllIncidents().then(setIncidents).catch((e) => setError(getErrorMessage(e)));
  }, []);

  return (
    <div className="card">
      <h1>Tous les incidents</h1>
      <p className="muted">Ouvrez un incident pour l'affecter à un technicien ou changer son statut.</p>
      <ErrorMessage message={error} />
      {!incidents && !error && <Loader />}
      {incidents && <IncidentTable incidents={incidents} showCreator showAssignee />}
    </div>
  );
}
