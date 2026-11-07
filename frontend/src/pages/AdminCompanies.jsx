import { useCallback, useEffect, useState } from 'react';
import { createCompany, listCompanies, setCompanyActive } from '../api/companies.js';
import Field from '../components/Field.jsx';

const EMPTY = { name: '', website: '', industry: '', description: '' };

export default function AdminCompanies() {
  const [companies, setCompanies] = useState(null);
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const load = useCallback(async () => {
    try { setCompanies(await listCompanies()); setError(''); } catch (e) { setError(e.message); setCompanies([]); }
  }, []);
  useEffect(() => { load(); }, [load]);

  async function add(e) {
    e.preventDefault();
    setNotice('');
    const name = form.name.trim();
    if (!name) { setErrors({ name: 'Enter the company name.' }); return; }
    if (form.website.trim() && !/^https?:\/\/\S+$/.test(form.website.trim())) {
      setErrors({ website: 'Use a full http:// or https:// address.' });
      return;
    }
    setErrors({});
    try {
      await createCompany({
        name,
        website: form.website.trim() || null,
        industry: form.industry.trim() || null,
        description: form.description.trim() || null,
      });
      setForm(EMPTY);
      setNotice('Company added.');
      load();
    } catch (ex) {
      if (ex.status === 409) setErrors({ name: ex.message });
      else setError(ex.message);
    }
  }

  async function toggle(c) {
    setNotice('');
    try {
      await setCompanyActive(c.id, !c.active);
      setNotice(`${c.name} ${c.active ? 'deactivated' : 'activated'}.`);
      load();
    } catch (ex) { setError(ex.message); }
  }

  return (
    <main className="mx-auto grid max-w-5xl gap-4 px-4 py-8">
      <h1 className="text-3xl font-extrabold">Companies</h1>
      {notice && <p role="status" className="rounded-lg bg-okt p-3 text-sm text-ok">{notice}</p>}
      {error && <p role="alert" className="rounded-lg bg-badt p-3 text-sm text-bad">{error}</p>}

      <section className="card" aria-labelledby="add-company">
        <h2 id="add-company" className="text-lg">Add a company</h2>
        <form onSubmit={add} noValidate className="grid gap-x-4 sm:grid-cols-2">
          <Field id="cname" label="Name" value={form.name} onChange={(v) => setForm({ ...form, name: v })} error={errors.name} />
          <Field id="cweb" label="Website" value={form.website} onChange={(v) => setForm({ ...form, website: v })} error={errors.website} />
          <Field id="cind" label="Industry" value={form.industry} onChange={(v) => setForm({ ...form, industry: v })} />
          <Field id="cdesc" label="Description" value={form.description} onChange={(v) => setForm({ ...form, description: v })} />
          <div className="sm:col-span-2"><button type="submit" className="btn mt-4">Add company</button></div>
        </form>
      </section>

      <section className="card" aria-labelledby="company-list">
        <h2 id="company-list" className="text-lg">All companies</h2>
        {companies === null && <p role="status" className="mt-2 text-mute">Loading…</p>}
        {companies && companies.length === 0 && <p className="mt-2 text-mute">No companies yet. Add one above before creating a drive.</p>}
        {companies && companies.length > 0 && (
          <div className="mt-2 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead><tr className="text-mute"><th className="py-2">Name</th><th>Industry</th><th>Status</th><th /></tr></thead>
              <tbody>
                {companies.map((c) => (
                  <tr key={c.id} className="border-t border-line">
                    <td className="py-2">{c.name}</td>
                    <td>{c.industry || '—'}</td>
                    <td>{c.active ? 'Active' : 'Inactive'}</td>
                    <td><button type="button" className="btn-ghost" onClick={() => toggle(c)}>{c.active ? 'Deactivate' : 'Activate'}<span className="sr-only"> {c.name}</span></button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </main>
  );
}
