"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";

/**
 * Login page: fetches the GitHub OAuth URL from backend and redirects.
 */
export default function LoginPage() {
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.auth
      .getAuthUrl()
      .then(({ url }) => {
        window.location.href = url;
      })
      .catch(() => {
        setError(
          "Couldn't start GitHub sign in. The server may be missing GitHub OAuth configuration (GITHUB_CLIENT_ID / GITHUB_CLIENT_SECRET)."
        );
      });
  }, []);

  if (error) {
    return (
      <div className="card fade-in" style={{ maxWidth: 460, margin: "3rem auto", textAlign: "center" }}>
        <p className="alert alert-error" style={{ marginBottom: "1rem" }}>{error}</p>
        <button onClick={() => window.location.reload()} className="btn btn-primary">Try again</button>
      </div>
    );
  }

  return (
    <div className="loading-center">
      <span className="spinner spinner-lg" />
      <p>Redirecting to GitHub…</p>
    </div>
  );
}
