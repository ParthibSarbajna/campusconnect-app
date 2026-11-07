import { useEffect, useState } from 'react';
import { listDepartmentUsers } from '../api/accounts.js';
import { useAuth } from '../auth/AuthContext.jsx';
import { ROLE_LABELS } from '../auth/roles.js';

export default function SpocHome() {
  const { user } = useAuth();
  const [state, setState] = useState({ loading: true, error: '', data: null });

  useEffect(() => {
    let alive = true;
    listDepartmentUsers(user.departmentId)
      .then((data) => alive && setState({ loading: false, error: '', data }))
      .catch((e) => alive && setState({ loading: false, error: e.message, data: null }));
    return () => { alive = false; };
  }, [user.departmentId]);

  return (
    <main className="mx-auto max-w-4xl px-4 py-8">
      <h1 className="text-3xl font-extrabold">Department</h1>
      <p className="mt-1 text-mute">Department #{user.departmentId}. You can only see accounts assigned to your own department.</p>
      <section className="card mt-4" aria-labelledby="accts">
        <h2 id="accts" className="text-lg">Department accounts</h2>
        {state.loading && <p role="status" className="mt-2 text-mute">Loading…</p>}
        {state.error && <p role="alert" className="mt-2 text-bad">{state.error}</p>}
        {state.data && state.data.items.length === 0 && <p className="mt-2 text-mute">No accounts are assigned to this department yet.</p>}
        {state.data && state.data.items.length > 0 && (
          <div className="mt-2 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead><tr className="text-mute"><th className="py-2">Name</th><th>Email</th><th>Role</th><th>Status</th></tr></thead>
              <tbody>
                {state.data.items.map((u) => (
                  <tr key={u.id} className="border-t border-line">
                    <td className="py-2">{u.fullName}</td><td>{u.email}</td><td>{ROLE_LABELS[u.role]}</td><td>{u.enabled ? 'Active' : 'Disabled'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
      <p className="card mt-4 text-sm text-mute">Student directory, eligibility monitoring and placement reports are planned for later phases. They do not exist yet.</p>
    </main>
  );
}
