import { useEffect, useState } from 'react';
import { getAssignedIncidents } from '../services/incidentService';
import IncidentTable from '../components/IncidentTable';
import Loader from '../components/Loader';
import ErrorMessage from '../components/ErrorMessage';
import { getErrorMessage } from '../utils/errors';

export default function AssignedIncidents() {
  const [incidents, setIncidents] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    getAssignedIncidents().then(setIncidents).catch((e) => setError(getErrorMessage(e)));
  }, []);

  return (
    <div className="card">
      <h1>Incidents qui me sont affectés</h1>
      <ErrorMessage message={error} />
      {!incidents && !error && <Loader />}
      {incidents && <IncidentTable incidents={incidents} showCreator />}
    </div>
  );
}
