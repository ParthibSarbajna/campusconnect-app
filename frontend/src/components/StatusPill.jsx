const TONE = {
  SELECTED: 'bg-okt text-ok',
  OPEN: 'bg-okt text-ok',
  REJECTED: 'bg-badt text-bad',
  CANCELLED: 'bg-badt text-bad',
  WITHDRAWN: 'bg-badt text-bad',
  SHORTLISTED: 'bg-warnt text-warn',
};

export default function StatusPill({ status }) {
  return <span className={`pill ${TONE[status] || 'bg-prit text-pri'}`}>{status}</span>;
}
