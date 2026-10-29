import { formatDate } from '../utils/format';

export default function IncidentHistoryList({ history }) {
  if (!history || history.length === 0) {
    return <p className="muted">Aucun historique.</p>;
  }
  return (
    <ul className="timeline">
      {history.map((entry) => (
        <li key={entry.id}>
          <time>{formatDate(entry.createdAt)}</time>
          <span>{entry.message}</span>
          {entry.performedBy && <small className="muted"> — {entry.performedBy.fullName}</small>}
        </li>
      ))}
    </ul>
  );
}
