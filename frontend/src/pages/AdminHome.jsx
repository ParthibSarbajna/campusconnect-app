import { useCallback, useEffect, useState } from 'react';
import { createDepartment, createUser, listDepartments, listUsers, updateUser } from '../api/accounts.js';
import { useAuth } from '../auth/AuthContext.jsx';
import { ROLE_LABELS } from '../auth/roles.js';
import { validateEmail, validateName, validatePassword } from '../auth/validators.js';
import Field from '../components/Field.jsx';

const SELECT = 'rounded-lg border border-line bg-surf px-2 py-1';

function UserRow({ u, departments, isSelf, onSaved, onError }) {
  const [role, setRole] = useState(u.role);
  const [dept, setDept] = useState(u.departmentId ?? '');
  const [enabled, setEnabled] = useState(u.enabled);
  async function save() {
    try {
      await updateUser(u.id, { role, departmentId: dept === '' ? null : Number(dept), enabled });
      onSaved();
    } catch (e) { onError(e.message); }
  }
  return (
    <tr className="border-t border-line">
      <td className="py-2">{u.fullName}<div className="text-xs text-mute">{u.email}</div></td>
      <td>
        <select aria-label={`Role for ${u.email}`} className={SELECT} value={role} disabled={isSelf} onChange={(e) => setRole(e.target.value)}>
          {Object.entries(ROLE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
      </td>
      <td>
        <select aria-label={`Department for ${u.email}`} className={SELECT} value={dept} onChange={(e) => setDept(e.target.value)}>
          <option value="">None</option>
          {departments.map((d) => <option key={d.id} value={d.id}>{d.code}</option>)}
        </select>
      </td>
      <td><input type="checkbox" aria-label={`Enabled for ${u.email}`} checked={enabled} disabled={isSelf} onChange={(e) => setEnabled(e.target.checked)} /></td>
      <td><button type="button" className="btn-ghost" onClick={save}>Save</button></td>
    </tr>
  );
}

export default function AdminHome() {
  const { user } = useAuth();
  const [departments, setDepartments] = useState([]);
  const [users, setUsers] = useState({ items: [], page: 0, size: 25, total: 0 });
  const [page, setPage] = useState(0);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  const [dep, setDep] = useState({ code: '', name: '' });
  const [nu, setNu] = useState({ fullName: '', email: '', password: '', role: 'DEPARTMENT_SPOC', departmentId: '' });
  const [formErrors, setFormErrors] = useState({});

  const load = useCallback(async () => {
    try {
      const [d, u] = await Promise.all([listDepartments(), listUsers(page, 25)]);
      setDepartments(d);
      setUsers(u);
    } catch (e) { setError(e.message); }
  }, [page]);
  useEffect(() => { load(); }, [load]);

  const done = (msg) => { setError(''); setNotice(msg); load(); };

  async function addDepartment(e) {
    e.preventDefault();
    if (!dep.code.trim() || !dep.name.trim()) { setError('Enter a department code and name.'); return; }
    try { await createDepartment(dep.code.trim(), dep.name.trim()); setDep({ code: '', name: '' }); done('Department created.'); }
    catch (ex) { setError(ex.message); }
  }

  async function addUser(e) {
    e.preventDefault();
    const errs = { fullName: validateName(nu.fullName), email: validateEmail(nu.email), password: validatePassword(nu.password) };
    if (nu.role === 'DEPARTMENT_SPOC' && !nu.departmentId) errs.departmentId = 'Choose a department for a SPOC.';
    setFormErrors(errs);
    if (Object.values(errs).some(Boolean)) return;
    try {
      await createUser({ ...nu, email: nu.email.trim(), fullName: nu.fullName.trim(), departmentId: nu.departmentId === '' ? null : Number(nu.departmentId) });
      setNu({ fullName: '', email: '', password: '', role: 'DEPARTMENT_SPOC', departmentId: '' });
      done('Account created.');
    } catch (ex) { setError(ex.message); }
  }

  const pages = Math.max(1, Math.ceil(users.total / users.size));
  return (
    <main className="mx-auto grid max-w-5xl gap-4 px-4 py-8">
      <h1 className="text-3xl font-extrabold">Administration</h1>
      {notice && <p role="status" className="rounded-lg bg-okt p-3 text-sm text-ok">{notice}</p>}
      {error && <p role="alert" className="rounded-lg bg-badt p-3 text-sm text-bad">{error}</p>}

      <section className="card" aria-labelledby="deps">
        <h2 id="deps" className="text-lg">Departments</h2>
        <p className="mt-1 text-sm text-mute">{departments.length ? departments.map((d) => `${d.code} (${d.name})`).join(', ') : 'No departments yet. Create one before adding a Department SPOC.'}</p>
        <form onSubmit={addDepartment} noValidate className="mt-2 grid gap-2 sm:grid-cols-[1fr_2fr_auto] sm:items-end">
          <Field id="dcode" label="Code" value={dep.code} onChange={(v) => setDep({ ...dep, code: v })} />
          <Field id="dname" label="Name" value={dep.name} onChange={(v) => setDep({ ...dep, name: v })} />
          <button type="submit" className="btn">Add</button>
        </form>
      </section>

      <section className="card" aria-labelledby="new-user">
        <h2 id="new-user" className="text-lg">Create an account</h2>
        <form onSubmit={addUser} noValidate className="grid gap-x-4 sm:grid-cols-2">
          <Field id="nfn" label="Full name" value={nu.fullName} onChange={(v) => setNu({ ...nu, fullName: v })} error={formErrors.fullName} />
          <Field id="nem" label="Email" type="email" value={nu.email} onChange={(v) => setNu({ ...nu, email: v })} error={formErrors.email} />
          <Field id="npw" label="Initial password" type="password" autoComplete="new-password" value={nu.password} onChange={(v) => setNu({ ...nu, password: v })} error={formErrors.password} />
          <div className="mt-3">
            <label htmlFor="nrole" className="mb-1 block text-sm font-bold">Role</label>
            <select id="nrole" className={`${SELECT} w-full py-2`} value={nu.role} onChange={(e) => setNu({ ...nu, role: e.target.value })}>
              {Object.entries(ROLE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </select>
            <label htmlFor="ndept" className="mb-1 mt-3 block text-sm font-bold">Department</label>
            <select id="ndept" className={`${SELECT} w-full py-2`} value={nu.departmentId} onChange={(e) => setNu({ ...nu, departmentId: e.target.value })}>
              <option value="">None</option>
              {departments.map((d) => <option key={d.id} value={d.id}>{d.code}</option>)}
            </select>
            {formErrors.departmentId && <p role="alert" className="mt-1 text-sm text-bad">{formErrors.departmentId}</p>}
          </div>
          <div className="sm:col-span-2"><button type="submit" className="btn mt-4">Create account</button></div>
        </form>
      </section>

      <section className="card" aria-labelledby="all-users">
        <h2 id="all-users" className="text-lg">Accounts ({users.total})</h2>
        <div className="mt-2 overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead><tr className="text-mute"><th className="py-2">User</th><th>Role</th><th>Department</th><th>Enabled</th><th /></tr></thead>
            <tbody>
              {users.items.map((u) => (
                <UserRow key={`${u.id}-${u.role}-${u.departmentId}-${u.enabled}`} u={u} departments={departments}
                  isSelf={u.id === user.id} onSaved={() => done('Account updated.')} onError={setError} />
              ))}
            </tbody>
          </table>
        </div>
        <div className="mt-3 flex items-center gap-3 text-sm">
          <button type="button" className="btn-ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
          <span>Page {page + 1} of {pages}</span>
          <button type="button" className="btn-ghost" disabled={page + 1 >= pages} onClick={() => setPage(page + 1)}>Next</button>
        </div>
      </section>
      <p className="card text-sm text-mute">Recruitment rounds, offers, historical imports and analytics are planned for later phases. They do not exist yet.</p>
    </main>
  );
}
