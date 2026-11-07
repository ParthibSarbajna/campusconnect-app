import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <main className="mx-auto max-w-xl px-4 py-16 text-center">
      <h1 className="text-3xl font-extrabold">Page not found</h1>
      <p className="mt-2 text-mute">The page you asked for does not exist.</p>
      <Link to="/" className="btn mt-6">Back to home</Link>
    </main>
  );
}
