import { useCallback, useEffect, useState } from 'react';
import { myApplications, withdrawApplication } from '../api/applications.js';
import StatusPill from '../components/StatusPill.jsx';
import { formatDateTime } from '../lib/format.js';

export default function StudentApplications() {
  const [data, setData] = useState(null);
  const [page, setPage] = useState(0);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const load = useCallback(async () => {
    try { setData(await myApplications(page)); setError(''); }
    catch (e) { setError(e.message); setData({ items: [], page: 0, size: 20, total: 0 }); }
  }, [page]);
  useEffect(() => { load(); }, [load]);

  async function withdraw(a) {
    if (!window.confirm(`Withdraw your application to ${a.companyName}?`)) return;
    setNotice('');
    try { await withdrawApplication(a.id); setNotice('Application withdrawn.'); load(); }
    catch (e) { setError(e.message); }
  }

  const pages = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1;
  return (
    <main className="mx-auto grid max-w-4xl gap-4 px-4 py-8">
      <h1 className="text-3xl font-extrabold">My applications</h1>
      {notice && <p role="status" className="rounded-lg bg-okt p-3 text-sm text-ok">{notice}</p>}
      {error && <p role="alert" className="rounded-lg bg-badt p-3 text-sm text-bad">{error}</p>}
      {data === null && <p role="status" className="text-mute">Loading…</p>}
      {data && data.items.length === 0 && !error && <p className="card text-mute">You have not applied to any drive yet.</p>}
      {data && data.items.length > 0 && (
        <div className="card overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead><tr className="text-mute"><th className="py-2">Company</th><th>Role</th><th>Applied</th><th>Status</th><th /></tr></thead>
            <tbody>
              {data.items.map((a) => (
                <tr key={a.id} className="border-t border-line">
                  <td className="py-2">{a.companyName}</td>
                  <td>{a.driveTitle}</td>
                  <td>{formatDateTime(a.appliedAt)}</td>
                  <td><StatusPill status={a.status} /></td>
                  <td>
                    {(a.status === 'APPLIED' || a.status === 'SHORTLISTED') && (
                      <button type="button" className="btn-ghost" onClick={() => withdraw(a)}>Withdraw<span className="sr-only"> from {a.companyName}</span></button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {data && data.total > data.size && (
        <div className="flex items-center gap-3 text-sm">
          <button type="button" className="btn-ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
          <span>Page {page + 1} of {pages}</span>
          <button type="button" className="btn-ghost" disabled={page + 1 >= pages} onClick={() => setPage(page + 1)}>Next</button>
        </div>
      )}
    </main>
  );
}
