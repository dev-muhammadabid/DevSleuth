"use client";

import { Suspense, useEffect, useRef, useState } from "react";
import { useSearchParams } from "next/navigation";

export default function AuthCallbackPage() {
  return (
    <Suspense fallback={<p style={{ padding: "2rem", textAlign: "center" }}>Loading...</p>}>
      <CallbackHandler />
    </Suspense>
  );
}

function CallbackHandler() {
  const searchParams = useSearchParams();
  const [error, setError] = useState<string | null>(null);
  // The OAuth code is single-use; guard against React's dev double-invoke firing twice.
  const startedRef = useRef(false);

  useEffect(() => {
    if (startedRef.current) return;
    startedRef.current = true;

    const code = searchParams.get("code");
    if (!code) {
      setError("No authorization code received from GitHub.");
      return;
    }

    (async () => {
      try {
        const res = await fetch(`/api/auth/github/callback?code=${encodeURIComponent(code)}`, {
          credentials: "include",
        });
        if (!res.ok) {
          let message = "Authentication failed.";
          try {
            const body = await res.json();
            if (body?.error) message = body.error;
          } catch {
            /* non-JSON error body */
          }
          throw new Error(message);
        }
        window.location.href = "/";
      } catch (e) {
        setError(e instanceof Error ? e.message : "Authentication failed.");
      }
    })();
  }, [searchParams]);

  if (error) {
    return (
      <div className="card fade-in" style={{ maxWidth: 420, margin: "3rem auto", textAlign: "center" }}>
        <p className="alert alert-error" style={{ marginBottom: "1rem" }}>{error}</p>
        <a href="/login" className="btn btn-primary">Try again</a>
      </div>
    );
  }

  return (
    <div className="loading-center">
      <span className="spinner spinner-lg" />
      <p>Completing sign in…</p>
    </div>
  );
}
