/** Colors map to the CSS variables carried over from the original demo (light and dark). */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        bg: 'var(--bg)', surf: 'var(--surf)', ink: 'var(--ink)', mute: 'var(--mute)', line: 'var(--line)',
        pri: 'var(--pri)', prit: 'var(--prit)', onpri: 'var(--on-pri)',
        ok: 'var(--ok)', okt: 'var(--okt)', warn: 'var(--warn)', warnt: 'var(--warnt)',
        bad: 'var(--bad)', badt: 'var(--badt)', board: 'var(--board)', amber: 'var(--amber)',
      },
      fontFamily: {
        display: ['"Bricolage Grotesque"', '"DM Sans"', 'system-ui', 'sans-serif'],
        sans: ['"DM Sans"', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
