import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import * as authApi from '../api/auth.js';
import { resetCsrf, setUnauthorizedHandler } from '../api/client.js';

export const AuthContext = createContext(null);

/**
 * Holds who the server says is signed in. This state is for display and routing only:
 * every protected API call is re-checked by the backend.
 */
export function AuthProvider({ children }) {
  const [state, setState] = useState({ status: 'loading', user: null });

  useEffect(() => {
    let alive = true;
    const signedOut = () => { if (alive) setState({ status: 'unauthenticated', user: null }); };
    setUnauthorizedHandler(() => { resetCsrf(); signedOut(); }); // session expired mid-use
    authApi.fetchMe()
      .then((user) => { if (alive) setState({ status: 'authenticated', user }); })
      .catch(signedOut);
    return () => { alive = false; setUnauthorizedHandler(null); };
  }, []);

  const login = useCallback(async (email, password) => {
    const user = await authApi.login(email, password);
    resetCsrf();
    setState({ status: 'authenticated', user });
    return user;
  }, []);

  const register = useCallback((body) => authApi.register(body), []);

  const logout = useCallback(async () => {
    try { await authApi.logout(); } catch { /* the session may already be gone */ }
    resetCsrf();
    setState({ status: 'unauthenticated', user: null });
  }, []);

  const refresh = useCallback(async () => {
    const user = await authApi.fetchMe();
    setState({ status: 'authenticated', user });
    return user;
  }, []);

  const value = useMemo(() => ({ ...state, login, register, logout, refresh }), [state, login, register, logout, refresh]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
