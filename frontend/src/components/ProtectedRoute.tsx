"use client";

import { useAuth } from "@/components/AuthProvider";
import type { ReactNode } from "react";

/**
 * Wraps pages that require authentication.
 * Shows loading while checking auth, redirects to login if not authenticated.
 */
export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="loading-center">
        <span className="spinner spinner-lg" />
        <span>Checking your session…</span>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="card fade-in" style={{ maxWidth: 420, margin: "3rem auto", textAlign: "center" }}>
        <h2 style={{ marginBottom: "0.5rem" }}>Sign in to DevSleuth</h2>
        <p className="muted" style={{ marginBottom: "1.25rem" }}>
          Connect your GitHub account to start reviewing pull requests.
        </p>
        <a href="/login" className="btn btn-primary">Sign in with GitHub</a>
      </div>
    );
  }

  return <>{children}</>;
}
