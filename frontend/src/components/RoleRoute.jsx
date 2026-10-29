import { Link, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/** Réserve une page à certains rôles, ex. <RoleRoute roles={['ADMIN']} /> pour /admin/users. */
export default function RoleRoute({ roles }) {
  const { user } = useAuth();

  if (!user || !roles.includes(user.role)) {
    return (
      <div className="card center">
        <h2>Accès refusé</h2>
        <p>Vous n'avez pas les droits nécessaires pour consulter cette page.</p>
        <Link className="btn" to="/">Retour au tableau de bord</Link>
      </div>
    );
  }
  return <Outlet />;
}
