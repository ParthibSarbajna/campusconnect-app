import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { homePathFor } from '../auth/roles.js';

export default function Forbidden() {
  const { user } = useAuth();
  return (
    <main className="mx-auto max-w-xl px-4 py-16 text-center">
      <h1 className="text-3xl font-extrabold">Access denied</h1>
      <p className="mt-2 text-mute">Your account does not have permission to open that page.</p>
      <Link to={user ? homePathFor(user.role) : '/'} className="btn mt-6">Go to my area</Link>
    </main>
  );
}
