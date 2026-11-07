// Mirrors the backend rules. The server re-validates everything; this is only for fast feedback.
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function validateName(v) {
  const t = (v || '').trim();
  if (!t) return 'Enter your full name.';
  if (t.length < 2 || t.length > 120) return 'Name must be 2 to 120 characters.';
  return '';
}

export function validateEmail(v) {
  const t = (v || '').trim();
  if (!t) return 'Enter your email address.';
  if (t.length > 254 || !EMAIL_RE.test(t)) return 'Enter a valid email address.';
  return '';
}

export function validatePassword(v) {
  if (!v) return 'Enter a password.';
  if (v.length < 10 || v.length > 72) return 'Use 10 to 72 characters.';
  if (!/[A-Za-z]/.test(v) || !/\d/.test(v)) return 'Include at least one letter and one digit.';
  return '';
}
