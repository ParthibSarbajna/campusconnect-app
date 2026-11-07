import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { homePathFor } from '../auth/roles.js';
import { validateEmail } from '../auth/validators.js';
import Field from '../components/Field.jsx';

export default function SignIn() {
  const { status, user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [busy, setBusy] = useState(false);

  if (status === 'authenticated') return <Navigate to={homePathFor(user.role)} replace />;

  async function onSubmit(e) {
    e.preventDefault();
    const next = { email: validateEmail(email), password: password ? '' : 'Enter your password.' };
    setErrors(next);
    setFormError('');
    if (next.email || next.password) return;
    setBusy(true);
    try {
      const u = await login(email.trim(), password);
      navigate(homePathFor(u.role), { replace: true });
    } catch (err) {
      setFormError(err.status === 401 ? 'Invalid email or password.' : err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="mx-auto max-w-md px-4 py-10">
      <h1 className="text-3xl font-extrabold">Sign in</h1>
      {location.state?.registered && (
        <p role="status" className="mt-3 rounded-lg bg-okt p-3 text-sm text-ok">Account created. Sign in to continue.</p>
      )}
      <form onSubmit={onSubmit} noValidate className="card mt-4">
        <Field id="email" label="Email" type="email" autoComplete="username" value={email} onChange={setEmail} error={errors.email} />
        <Field id="password" label="Password" type="password" autoComplete="current-password" value={password} onChange={setPassword} error={errors.password} />
        {formError && <p role="alert" className="mt-3 text-sm text-bad">{formError}</p>}
        <button type="submit" className="btn mt-4" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
      </form>
      <p className="mt-4 text-sm text-mute">New student? <Link to="/register" className="font-bold text-pri">Create an account</Link></p>
    </main>
  );
}
