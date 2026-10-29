import { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { changeUserRole, disableUser, enableUser, getUsers } from '../services/adminService';
import Loader from '../components/Loader';
import ErrorMessage from '../components/ErrorMessage';
import { ROLE_LABELS, formatDate } from '../utils/format';
import { getErrorMessage } from '../utils/errors';

export default function AdminUsers() {
  const { user: me } = useAuth();
  const [users, setUsers] = useState(null);
  const [search, setSearch] = useState('');
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);

  useEffect(() => {
    getUsers().then(setUsers).catch((e) => setError(getErrorMessage(e)));
  }, []);

  const visible = useMemo(() => {
    const term = search.trim().toLowerCase();
    return (users || []).filter((u) => !term || u.fullName.toLowerCase().includes(term) || u.email.toLowerCase().includes(term));
  }, [users, search]);

  async function change(target, action) {
    setError('');
    setBusyId(target.id);
    try {
      const updated = await action();
      setUsers((list) => list.map((u) => (u.id === updated.id ? updated : u)));
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setBusyId(null);
    }
  }

  const isMe = (u) => u.email === me.email;

  return (
    <div className="card">
      <h1>Utilisateurs</h1>
      <ErrorMessage message={error} />
      {!users && !error && <Loader />}
      {users && (
        <>
          <div className="filters">
            <input type="search" placeholder="Rechercher un nom ou un email…" value={search}
              onChange={(e) => setSearch(e.target.value)} aria-label="Rechercher un utilisateur" />
          </div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>#</th><th>Nom</th><th>Email</th><th>Rôle</th><th>Compte</th><th>Créé le</th><th>Actions</th></tr>
              </thead>
              <tbody>
                {visible.map((u) => (
                  <tr key={u.id}>
                    <td>{u.id}</td>
                    <td>{u.fullName}</td>
                    <td>{u.email}</td>
                    <td>
                      <select value={u.role} disabled={isMe(u) || busyId === u.id} aria-label={`Rôle de ${u.fullName}`}
                        onChange={(e) => change(u, () => changeUserRole(u.id, e.target.value))}>
                        {Object.entries(ROLE_LABELS).map(([role, label]) => <option key={role} value={role}>{label}</option>)}
                      </select>
                    </td>
                    <td><span className={`badge ${u.enabled ? 'status-RESOLVED' : 'priority-CRITICAL'}`}>{u.enabled ? 'Actif' : 'Désactivé'}</span></td>
                    <td>{formatDate(u.createdAt)}</td>
                    <td>
                      {u.enabled ? (
                        <button type="button" className="btn btn-danger" disabled={isMe(u) || busyId === u.id}
                          onClick={() => change(u, () => disableUser(u.id))}>Désactiver</button>
                      ) : (
                        <button type="button" className="btn btn-secondary" disabled={busyId === u.id}
                          onClick={() => change(u, () => enableUser(u.id))}>Activer</button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}
