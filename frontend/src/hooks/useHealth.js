import { useCallback, useEffect, useState } from 'react';
import { getHealth } from '../api/health.js';

export function useHealth() {
  const [result, setResult] = useState({ state: 'loading', data: null, error: null });
  const [tick, setTick] = useState(0);

  useEffect(() => {
    const ctrl = new AbortController();
    setResult({ state: 'loading', data: null, error: null });
    getHealth(ctrl.signal)
      .then((data) => setResult({ state: 'ready', data, error: null }))
      .catch((error) => {
        if (error && error.name === 'AbortError') return;
        setResult({ state: 'error', data: null, error });
      });
    return () => ctrl.abort();
  }, [tick]);

  const refresh = useCallback(() => setTick((t) => t + 1), []);
  return { ...result, refresh };
}
