import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { homePathFor } from '../auth/roles.js';
import { validateEmail, validateName, validatePassword } from '../auth/validators.js';
import Field from '../components/Field.jsx';

export default function Register() {
  const { status, user, register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: '', email: '', password: '', confirm: '' });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (v) => setForm((f) => ({ ...f, [k]: v }));

  if (status === 'authenticated') return <Navigate to={homePathFor(user.role)} replace />;

  async function onSubmit(e) {
    e.preventDefault();
    const next = {
      fullName: validateName(form.fullName),
      email: validateEmail(form.email),
      password: validatePassword(form.password),
      confirm: form.confirm === form.password ? '' : 'Passwords do not match.',
    };
    setErrors(next);
    setFormError('');
    if (Object.values(next).some(Boolean)) return;
    setBusy(true);
    try {
      await register({ fullName: form.fullName.trim(), email: form.email.trim(), password: form.password });
      navigate('/signin', { replace: true, state: { registered: true } });
    } catch (err) {
      if (err.status === 409) setErrors({ email: 'An account with this email already exists.' });
      else setFormError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="mx-auto max-w-md px-4 py-10">
      <h1 className="text-3xl font-extrabold">Create a student account</h1>
      <p className="mt-1 text-sm text-mute">Department SPOC and Placement Admin accounts are created by the placement cell.</p>
      <form onSubmit={onSubmit} noValidate className="card mt-4">
        <Field id="fullName" label="Full name" autoComplete="name" value={form.fullName} onChange={set('fullName')} error={errors.fullName} />
        <Field id="email" label="Email" type="email" autoComplete="email" value={form.email} onChange={set('email')} error={errors.email} />
        <Field id="password" label="Password" type="password" autoComplete="new-password" value={form.password} onChange={set('password')} error={errors.password} />
        <p className="mt-1 text-xs text-mute">10 to 72 characters with at least one letter and one digit.</p>
        <Field id="confirm" label="Confirm password" type="password" autoComplete="new-password" value={form.confirm} onChange={set('confirm')} error={errors.confirm} />
        {formError && <p role="alert" className="mt-3 text-sm text-bad">{formError}</p>}
        <button type="submit" className="btn mt-4" disabled={busy}>{busy ? 'Creating account…' : 'Create account'}</button>
      </form>
      <p className="mt-4 text-sm text-mute">Already registered? <Link to="/signin" className="font-bold text-pri">Sign in</Link></p>
    </main>
  );
}
