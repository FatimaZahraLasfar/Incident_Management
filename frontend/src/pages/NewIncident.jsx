import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createIncident } from '../services/incidentService';
import ErrorMessage from '../components/ErrorMessage';
import { CATEGORIES, CATEGORY_LABELS, PRIORITIES, PRIORITY_LABELS } from '../utils/format';
import { getErrorMessage, getFieldErrors } from '../utils/errors';

export default function NewIncident() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ title: '', description: '', category: 'OTHER', priority: 'MEDIUM' });
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading] = useState(false);

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    const errors = {};
    if (!form.title.trim()) errors.title = 'Le titre est obligatoire';
    else if (form.title.length > 150) errors.title = 'Le titre ne doit pas dépasser 150 caractères';
    if (!form.description.trim()) errors.description = 'La description est obligatoire';
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    setLoading(true);
    try {
      const created = await createIncident({ ...form, title: form.title.trim(), description: form.description.trim() });
      navigate(`/incidents/${created.id}`, { replace: true });
    } catch (e) {
      setError(getErrorMessage(e));
      setFieldErrors(getFieldErrors(e));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="card narrow-wide">
      <h1>Déclarer un incident</h1>
      <ErrorMessage message={error} />
      <form onSubmit={handleSubmit} noValidate>
        <label>
          Titre
          <input value={form.title} onChange={update('title')} maxLength={150} />
          {fieldErrors.title && <span className="field-error">{fieldErrors.title}</span>}
        </label>
        <label>
          Description
          <textarea rows={6} value={form.description} onChange={update('description')} maxLength={4000} />
          {fieldErrors.description && <span className="field-error">{fieldErrors.description}</span>}
        </label>
        <div className="row">
          <label>
            Catégorie
            <select value={form.category} onChange={update('category')}>
              {CATEGORIES.map((c) => <option key={c} value={c}>{CATEGORY_LABELS[c]}</option>)}
            </select>
            {fieldErrors.category && <span className="field-error">{fieldErrors.category}</span>}
          </label>
          <label>
            Priorité
            <select value={form.priority} onChange={update('priority')}>
              {PRIORITIES.map((p) => <option key={p} value={p}>{PRIORITY_LABELS[p]}</option>)}
            </select>
            {fieldErrors.priority && <span className="field-error">{fieldErrors.priority}</span>}
          </label>
        </div>
        <button className="btn" type="submit" disabled={loading}>
          {loading ? 'Envoi…' : "Envoyer l'incident"}
        </button>
      </form>
    </div>
  );
}
