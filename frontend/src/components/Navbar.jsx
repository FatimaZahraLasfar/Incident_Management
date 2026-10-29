import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ROLE_LABELS } from '../utils/format';

const LINKS = {
  EMPLOYEE: [
    ['/incidents/my', 'Mes incidents'],
    ['/incidents/new', 'Déclarer un incident'],
  ],
  TECHNICIAN: [['/incidents/assigned', 'Incidents affectés']],
  ADMIN: [
    ['/admin/incidents', 'Incidents'],
    ['/admin/users', 'Utilisateurs'],
  ],
};

export default function Navbar() {
  const { user, isAuthenticated, logout } = useAuth();

  return (
    <header className="navbar">
      <div className="navbar-inner">
        <NavLink to="/" className="brand">Suivi des incidents</NavLink>
        {isAuthenticated && (
          <>
            <nav className="nav-links">
              <NavLink to="/" end>Tableau de bord</NavLink>
              {(LINKS[user.role] || []).map(([to, label]) => (
                <NavLink key={to} to={to}>{label}</NavLink>
              ))}
              <NavLink to="/profile">Profil</NavLink>
            </nav>
            <div className="nav-user">
              <span>
                {user.fullName} <small className="muted">({ROLE_LABELS[user.role] || user.role})</small>
              </span>
              <button type="button" className="btn btn-secondary" onClick={logout}>Déconnexion</button>
            </div>
          </>
        )}
      </div>
    </header>
  );
}
