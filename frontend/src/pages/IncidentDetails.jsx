import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { addComment, assignIncident, getIncident, updateStatus } from '../services/incidentService';
import { getUsers } from '../services/adminService';
import IncidentHistoryList from '../components/IncidentHistoryList';
import { PriorityBadge, StatusBadge } from '../components/StatusBadge';
import Loader from '../components/Loader';
import ErrorMessage from '../components/ErrorMessage';
import { CATEGORY_LABELS, STATUS_LABELS, allowedTransitions, formatDate } from '../utils/format';
import { getErrorMessage } from '../utils/errors';

export default function IncidentDetails() {
  const { id } = useParams();
  const { user } = useAuth();
  const [incident, setIncident] = useState(null);
  const [technicians, setTechnicians] = useState([]);
  const [technicianId, setTechnicianId] = useState('');
  const [note, setNote] = useState('');
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');
  const [busy, setBusy] = useState(false);

  const isAdmin = user.role === 'ADMIN';
  const canWork = user.role === 'ADMIN' || user.role === 'TECHNICIAN';
  const backLink = { EMPLOYEE: '/incidents/my', TECHNICIAN: '/incidents/assigned', ADMIN: '/admin/incidents' }[user.role];

  useEffect(() => {
    let cancelled = false;
    setIncident(null);
    setError('');
    getIncident(id)
      .then((data) => { if (!cancelled) setIncident(data); })
      .catch((e) => { if (!cancelled) setError(getErrorMessage(e)); });
    return () => { cancelled = true; };
  }, [id]);

  // Liste des techniciens actifs, pour l'affectation (réservée à l'admin)
  useEffect(() => {
    if (!isAdmin) return;
    getUsers()
      .then((users) => setTechnicians(users.filter((u) => u.role === 'TECHNICIAN' && u.enabled)))
      .catch(() => setTechnicians([]));
  }, [isAdmin]);

  async function run(action, successMessage) {
    setError('');
    setInfo('');
    setBusy(true);
    try {
      await action();
      setInfo(successMessage);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  const handleStatus = (status) => {
    if (status === 'CLOSED' && !window.confirm('Clôturer cet incident ?')) return;
    run(async () => setIncident(await updateStatus(id, status)), `Statut modifié : ${STATUS_LABELS[status]}`);
  };

  const handleAssign = (event) => {
    event.preventDefault();
    if (!technicianId) {
      setError('Sélectionnez un technicien.');
      return;
    }
    run(async () => {
      setIncident(await assignIncident(id, technicianId));
      setTechnicianId('');
    }, 'Incident affecté.');
  };

  const handleNote = (event) => {
    event.preventDefault();
    if (!note.trim()) {
      setError('La note ne peut pas être vide.');
      return;
    }
    run(async () => {
      const comment = await addComment(id, note.trim());
      setIncident((current) => ({ ...current, comments: [...current.comments, comment] }));
      setNote('');
    }, 'Note ajoutée.');
  };

  if (!incident) {
    return (
      <div className="card">
        <ErrorMessage message={error} />
        {!error && <Loader />}
        <Link to={backLink}>← Retour à la liste</Link>
      </div>
    );
  }

  const transitions = allowedTransitions(user.role, incident.status);
  const closedForTechnician = incident.status === 'CLOSED' && !isAdmin;

  return (
    <div>
      <p><Link to={backLink}>← Retour à la liste</Link></p>
      <div className="card">
        <div className="page-head">
          <h1>#{incident.id} — {incident.title}</h1>
          <StatusBadge status={incident.status} />
        </div>
        <ErrorMessage message={error} />
        {info && <div className="alert alert-success">{info}</div>}

        <dl className="details">
          <dt>Catégorie</dt><dd>{CATEGORY_LABELS[incident.category] || incident.category}</dd>
          <dt>Priorité</dt><dd><PriorityBadge priority={incident.priority} /></dd>
          <dt>Déclaré par</dt><dd>{incident.createdBy?.fullName}</dd>
          <dt>Technicien</dt><dd>{incident.assignedTo?.fullName || <span className="muted">Non affecté</span>}</dd>
          <dt>Créé le</dt><dd>{formatDate(incident.createdAt)}</dd>
          <dt>Mis à jour le</dt><dd>{formatDate(incident.updatedAt)}</dd>
        </dl>
        <h3>Description</h3>
        <p className="description">{incident.description}</p>
      </div>

      {canWork && (
        <div className="card">
          <h2>Actions</h2>
          {isAdmin && incident.status !== 'CLOSED' && (
            <form className="inline-form" onSubmit={handleAssign}>
              <select value={technicianId} onChange={(e) => setTechnicianId(e.target.value)} aria-label="Technicien">
                <option value="">— Choisir un technicien —</option>
                {technicians.map((t) => <option key={t.id} value={t.id}>{t.fullName}</option>)}
              </select>
              <button className="btn" type="submit" disabled={busy}>
                {incident.assignedTo ? 'Réaffecter' : 'Affecter'}
              </button>
            </form>
          )}
          <div className="actions">
            {transitions.length === 0 && (
              <span className="muted">
                {closedForTechnician ? 'Incident clôturé : seul un administrateur peut le modifier.' : 'Aucun changement de statut possible.'}
              </span>
            )}
            {transitions.map((status) => (
              <button key={status} type="button" className="btn btn-secondary" disabled={busy} onClick={() => handleStatus(status)}>
                {status === 'IN_PROGRESS' && incident.status === 'CLOSED' ? 'Rouvrir' : `Passer à « ${STATUS_LABELS[status]} »`}
              </button>
            ))}
          </div>
        </div>
      )}

      {canWork && (
        <div className="card">
          <h2>Notes de traitement</h2>
          {incident.comments.length === 0 && <p className="muted">Aucune note.</p>}
          <ul className="comments">
            {incident.comments.map((c) => (
              <li key={c.id}>
                <div><strong>{c.author?.fullName}</strong> <small className="muted">{formatDate(c.createdAt)}</small></div>
                <p>{c.content}</p>
              </li>
            ))}
          </ul>
          {!closedForTechnician && (
            <form onSubmit={handleNote}>
              <label>
                Ajouter une note
                <textarea rows={3} value={note} onChange={(e) => setNote(e.target.value)} maxLength={2000}
                  placeholder="Ex. : Analyse antivirus effectuée." />
              </label>
              <button className="btn" type="submit" disabled={busy}>Ajouter la note</button>
            </form>
          )}
        </div>
      )}

      <div className="card">
        <h2>Historique</h2>
        <IncidentHistoryList history={incident.history} />
      </div>
    </div>
  );
}
