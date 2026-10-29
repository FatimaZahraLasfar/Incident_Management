import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import * as authService from '../services/authService';
import ErrorMessage from '../components/ErrorMessage';
import { getErrorMessage, getFieldErrors } from '../utils/errors';

export default function Register() {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: '', email: '', password: '', confirm: '' });
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading] = useState(false);

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setFieldErrors({});
    const errors = {};
    if (form.fullName.trim().length < 2) errors.fullName = 'Le nom doit contenir au moins 2 caractères';
    if (!/^\S+@\S+\.\S+$/.test(form.email.trim())) errors.email = 'Email invalide';
    if (form.password.length < 8) errors.password = 'Le mot de passe doit contenir au moins 8 caractères';
    if (form.password !== form.confirm) errors.confirm = 'Les mots de passe ne correspondent pas';
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }
    setLoading(true);
    try {
      await authService.register(form.fullName.trim(), form.email.trim(), form.password);
      navigate('/login', { replace: true, state: { registered: true } });
    } catch (e) {
      setError(getErrorMessage(e));
      setFieldErrors(getFieldErrors(e));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="card narrow">
      <h1>Créer un compte</h1>
      <p className="muted">Les nouveaux comptes sont des comptes employé.</p>
      <ErrorMessage message={error} />
      <form onSubmit={handleSubmit} noValidate>
        <label>
          Nom complet
          <input value={form.fullName} onChange={update('fullName')} autoComplete="name" />
          {fieldErrors.fullName && <span className="field-error">{fieldErrors.fullName}</span>}
        </label>
        <label>
          Email
          <input type="email" value={form.email} onChange={update('email')} autoComplete="email" />
          {fieldErrors.email && <span className="field-error">{fieldErrors.email}</span>}
        </label>
        <label>
          Mot de passe (8 caractères minimum)
          <input type="password" value={form.password} onChange={update('password')} autoComplete="new-password" />
          {fieldErrors.password && <span className="field-error">{fieldErrors.password}</span>}
        </label>
        <label>
          Confirmation du mot de passe
          <input type="password" value={form.confirm} onChange={update('confirm')} autoComplete="new-password" />
          {fieldErrors.confirm && <span className="field-error">{fieldErrors.confirm}</span>}
        </label>
        <button className="btn" type="submit" disabled={loading}>
          {loading ? 'Création…' : 'Créer le compte'}
        </button>
      </form>
      <p className="muted">Déjà inscrit ? <Link to="/login">Se connecter</Link></p>
    </div>
  );
}
