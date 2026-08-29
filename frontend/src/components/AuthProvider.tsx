"use client";

import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { api } from "@/lib/api";
import type { User } from "@/types";

interface AuthContextType {
  user: User | null;
  loading: boolean;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType>({
  user: null,
  loading: true,
  logout: async () => {},
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    api.auth
      .me()
      .then((u) => {
        if (active) setUser(u);
      })
      .catch(() => {
        // 401 (not signed in) or network error -> treat as logged out.
        if (active) setUser(null);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  const logout = async () => {
    try {
      await api.auth.logout();
    } finally {
      setUser(null);
      // Go to the public landing page, NOT /login: the login page immediately
      // re-initiates GitHub OAuth, and since GitHub still has the app authorized it
      // would silently sign the user back in — making logout appear to do nothing.
      window.location.href = "/";
    }
  };

  return (
    <AuthContext.Provider value={{ user, loading, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
