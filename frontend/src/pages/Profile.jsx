import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { changePassword, getProfile, updateProfile } from '../services/profileService';
import ErrorMessage from '../components/ErrorMessage';
import Loader from '../components/Loader';
import { ROLE_LABELS, formatDate } from '../utils/format';
import { getErrorMessage, getFieldErrors } from '../utils/errors';

export default function Profile() {
  const { updateUser } = useAuth();
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState({ fullName: '', email: '' });
  const [profileMsg, setProfileMsg] = useState({ error: '', ok: '', fields: {} });
  const [pwd, setPwd] = useState({ oldPassword: '', newPassword: '', confirmPassword: '' });
  const [pwdMsg, setPwdMsg] = useState({ error: '', ok: '', fields: {} });
  const [loadError, setLoadError] = useState('');

  useEffect(() => {
    getProfile()
      .then((p) => {
        setProfile(p);
        setForm({ fullName: p.fullName, email: p.email });
      })
      .catch((e) => setLoadError(getErrorMessage(e)));
  }, []);

  async function saveProfile(event) {
    event.preventDefault();
    setProfileMsg({ error: '', ok: '', fields: {} });
    if (form.fullName.trim().length < 2 || !/^\S+@\S+\.\S+$/.test(form.email.trim())) {
      setProfileMsg({ error: 'Renseignez un nom (2 caractères minimum) et un email valide.', ok: '', fields: {} });
      return;
    }
    try {
      const updated = await updateProfile(form.fullName.trim(), form.email.trim());
      setProfile(updated);
      updateUser({ fullName: updated.fullName, email: updated.email });
      setProfileMsg({ error: '', ok: 'Profil mis à jour.', fields: {} });
    } catch (e) {
      setProfileMsg({ error: getErrorMessage(e), ok: '', fields: getFieldErrors(e) });
    }
  }

  async function savePassword(event) {
    event.preventDefault();
    setPwdMsg({ error: '', ok: '', fields: {} });
    if (pwd.newPassword.length < 8) {
      setPwdMsg({ error: 'Le nouveau mot de passe doit contenir au moins 8 caractères.', ok: '', fields: {} });
      return;
    }
    if (pwd.newPassword !== pwd.confirmPassword) {
      setPwdMsg({ error: 'La confirmation ne correspond pas au nouveau mot de passe.', ok: '', fields: {} });
      return;
    }
    try {
      await changePassword(pwd.oldPassword, pwd.newPassword, pwd.confirmPassword);
      setPwd({ oldPassword: '', newPassword: '', confirmPassword: '' });
      setPwdMsg({ error: '', ok: 'Mot de passe modifié. Vos autres sessions ont été déconnectées.', fields: {} });
    } catch (e) {
      setPwdMsg({ error: getErrorMessage(e), ok: '', fields: getFieldErrors(e) });
    }
  }

  if (loadError) return <ErrorMessage message={loadError} />;
  if (!profile) return <Loader />;

  return (
    <div className="two-cols">
      <div className="card">
        <h1>Mon profil</h1>
        <p className="muted">
          {ROLE_LABELS[profile.role]} — membre depuis le {formatDate(profile.createdAt)}
        </p>
        <ErrorMessage message={profileMsg.error} />
        {profileMsg.ok && <div className="alert alert-success">{profileMsg.ok}</div>}
        <form onSubmit={saveProfile} noValidate>
          <label>
            Nom complet
            <input value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
            {profileMsg.fields.fullName && <span className="field-error">{profileMsg.fields.fullName}</span>}
          </label>
          <label>
            Email
            <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            {profileMsg.fields.email && <span className="field-error">{profileMsg.fields.email}</span>}
          </label>
          <button className="btn" type="submit">Enregistrer</button>
        </form>
      </div>

      <div className="card">
        <h2>Changer le mot de passe</h2>
        <ErrorMessage message={pwdMsg.error} />
        {pwdMsg.ok && <div className="alert alert-success">{pwdMsg.ok}</div>}
        <form onSubmit={savePassword} noValidate>
          <label>
            Ancien mot de passe
            <input type="password" autoComplete="current-password" value={pwd.oldPassword}
              onChange={(e) => setPwd({ ...pwd, oldPassword: e.target.value })} />
          </label>
          <label>
            Nouveau mot de passe
            <input type="password" autoComplete="new-password" value={pwd.newPassword}
              onChange={(e) => setPwd({ ...pwd, newPassword: e.target.value })} />
            {pwdMsg.fields.newPassword && <span className="field-error">{pwdMsg.fields.newPassword}</span>}
          </label>
          <label>
            Confirmation du nouveau mot de passe
            <input type="password" autoComplete="new-password" value={pwd.confirmPassword}
              onChange={(e) => setPwd({ ...pwd, confirmPassword: e.target.value })} />
          </label>
          <button className="btn" type="submit">Modifier le mot de passe</button>
        </form>
      </div>
    </div>
  );
}
