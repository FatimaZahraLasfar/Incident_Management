import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getMyIncidents } from '../services/incidentService';
import IncidentTable from '../components/IncidentTable';
import Loader from '../components/Loader';
import ErrorMessage from '../components/ErrorMessage';
import { getErrorMessage } from '../utils/errors';

export default function MyIncidents() {
  const [incidents, setIncidents] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    getMyIncidents().then(setIncidents).catch((e) => setError(getErrorMessage(e)));
  }, []);

  return (
    <div className="card">
      <div className="page-head">
        <h1>Mes incidents</h1>
        <Link className="btn" to="/incidents/new">Déclarer un incident</Link>
      </div>
      <ErrorMessage message={error} />
      {!incidents && !error && <Loader />}
      {incidents && <IncidentTable incidents={incidents} showAssignee />}
    </div>
  );
}
