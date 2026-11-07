import { CheckCircle2, CircleAlert, RefreshCw } from 'lucide-react';
import { useHealth } from '../hooks/useHealth.js';

function Badge({ up, children }) {
  return <span className={`pill ${up ? 'bg-okt text-ok' : 'bg-badt text-bad'}`}>{children}</span>;
}

export default function StatusCard() {
  const { state, data, error, refresh } = useHealth();

  return (
    <section className="card" aria-labelledby="status-heading">
      <div className="flex items-center gap-2">
        <h2 id="status-heading" className="text-lg">System status</h2>
        <span className="flex-1" />
        <button type="button" className="btn-ghost" onClick={refresh} disabled={state === 'loading'}>
          <RefreshCw size={14} aria-hidden="true" /> Refresh
        </button>
      </div>

      {state === 'loading' && <p className="mt-3 text-mute" role="status">Checking the backend…</p>}

      {state === 'error' && (
        <div className="mt-3 flex gap-2 text-bad" role="alert">
          <CircleAlert size={18} aria-hidden="true" />
          <div>
            <p className="font-bold">Backend not reachable</p>
            <p className="text-sm text-mute">{error?.message} Start the database and backend, then refresh.</p>
          </div>
        </div>
      )}

      {state === 'ready' && data && (
        <dl className="mt-3 grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm">
          <dt className="text-mute">Backend</dt>
          <dd><Badge up={data.status === 'UP'}>{data.status}</Badge></dd>
          <dt className="text-mute">Database</dt>
          <dd><Badge up={data.database === 'UP'}>{data.database}</Badge></dd>
          <dt className="text-mute">Schema version</dt>
          <dd>{data.schemaVersion ?? 'none applied'}</dd>
          <dt className="text-mute">App version</dt>
          <dd>{data.version}</dd>
          <dt className="text-mute">Checked at</dt>
          <dd>{new Date(data.timestamp).toLocaleString()}</dd>
          {data.status === 'UP' && (
            <dd className="col-span-2 mt-1 flex items-center gap-1 text-ok"><CheckCircle2 size={14} aria-hidden="true" /> Frontend, backend and database are connected.</dd>
          )}
        </dl>
      )}
    </section>
  );
}
