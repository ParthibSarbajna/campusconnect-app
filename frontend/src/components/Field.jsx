export default function Field({ id, label, error, value, onChange, type = 'text', ...rest }) {
  return (
    <div className="mt-3">
      <label htmlFor={id} className="mb-1 block text-sm font-bold">{label}</label>
      <input
        id={id}
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={error ? `${id}-error` : undefined}
        className="w-full rounded-lg border border-line bg-surf px-3 py-2"
        {...rest}
      />
      {error && <p id={`${id}-error`} role="alert" className="mt-1 text-sm text-bad">{error}</p>}
    </div>
  );
}
