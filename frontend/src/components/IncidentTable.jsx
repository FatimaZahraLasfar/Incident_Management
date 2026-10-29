import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { CATEGORY_LABELS, PRIORITIES, PRIORITY_LABELS, STATUSES, STATUS_LABELS, formatDate } from '../utils/format';
import { PriorityBadge, StatusBadge } from './StatusBadge';

/** Tableau d'incidents avec recherche (titre) et filtres (statut, priorité) appliqués côté client. */
export default function IncidentTable({ incidents, showCreator = false, showAssignee = false, filters = true }) {
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [priority, setPriority] = useState('');

  const visible = useMemo(() => {
    const term = search.trim().toLowerCase();
    return incidents.filter(
      (i) =>
        (!term || i.title.toLowerCase().includes(term)) &&
        (!status || i.status === status) &&
        (!priority || i.priority === priority),
    );
  }, [incidents, search, status, priority]);

  return (
    <div>
      {filters && (
        <div className="filters">
          <input
            type="search"
            placeholder="Rechercher par titre…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            aria-label="Rechercher par titre"
          />
          <select value={status} onChange={(e) => setStatus(e.target.value)} aria-label="Filtrer par statut">
            <option value="">Tous les statuts</option>
            {STATUSES.map((s) => (
              <option key={s} value={s}>{STATUS_LABELS[s]}</option>
            ))}
          </select>
          <select value={priority} onChange={(e) => setPriority(e.target.value)} aria-label="Filtrer par priorité">
            <option value="">Toutes les priorités</option>
            {PRIORITIES.map((p) => (
              <option key={p} value={p}>{PRIORITY_LABELS[p]}</option>
            ))}
          </select>
        </div>
      )}

      {visible.length === 0 ? (
        <p className="muted">Aucun incident à afficher.</p>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>#</th>
                <th>Titre</th>
                <th>Catégorie</th>
                <th>Priorité</th>
                <th>Statut</th>
                {showCreator && <th>Déclaré par</th>}
                {showAssignee && <th>Technicien</th>}
                <th>Créé le</th>
              </tr>
            </thead>
            <tbody>
              {visible.map((i) => (
                <tr key={i.id}>
                  <td>{i.id}</td>
                  <td><Link to={`/incidents/${i.id}`}>{i.title}</Link></td>
                  <td>{CATEGORY_LABELS[i.category] || i.category}</td>
                  <td><PriorityBadge priority={i.priority} /></td>
                  <td><StatusBadge status={i.status} /></td>
                  {showCreator && <td>{i.createdBy?.fullName}</td>}
                  {showAssignee && <td>{i.assignedTo?.fullName || <span className="muted">Non affecté</span>}</td>}
                  <td>{formatDate(i.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
