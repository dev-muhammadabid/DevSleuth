"use client";

import type { ReactNode } from "react";
import { useAuth } from "@/components/AuthProvider";

const NAV = [
  { href: "/dashboard", label: "Dashboard" },
  { href: "/repositories", label: "Repositories" },
  { href: "/pull-requests", label: "Pull Requests" },
  { href: "/experiments", label: "Experiments" },
];

export function Layout({ children }: { children: ReactNode }) {
  const { user, logout } = useAuth();

  return (
    <div style={{ minHeight: "100vh" }}>
      <header className="topbar">
        <div className="row" style={{ gap: "1.75rem" }}>
          <a href="/" className="brand">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src="/devsleuth.png" alt="" width={24} height={24} className="brand-logo" />
            DevSleuth
          </a>
          {user && (
            <nav className="nav">
              {NAV.map((item) => (
                <a key={item.href} href={item.href} className="nav-link">
                  {item.label}
                </a>
              ))}
            </nav>
          )}
        </div>

        <div className="row" style={{ gap: "0.75rem" }}>
          {user ? (
            <>
              <div className="row" style={{ gap: "0.5rem" }}>
                <Avatar user={user} />
                <span style={{ fontSize: "0.875rem", color: "#374151" }}>{user.username}</span>
              </div>
              <button onClick={logout} className="btn btn-sm">
                Logout
              </button>
            </>
          ) : (
            <a href="/login" className="btn btn-primary btn-sm">Sign in</a>
          )}
        </div>
      </header>
      <main className="app-main">{children}</main>
    </div>
  );
}

function Avatar({ user }: { user: { username: string; avatarUrl?: string | null } }) {
  if (user.avatarUrl) {
    // eslint-disable-next-line @next/next/no-img-element
    return (
      <img
        src={user.avatarUrl}
        alt={user.username}
        width={28}
        height={28}
        style={{ borderRadius: "50%", border: "1px solid var(--border)" }}
      />
    );
  }
  const initial = user.username?.charAt(0).toUpperCase() ?? "?";
  return (
    <span
      aria-hidden
      style={{
        width: 28,
        height: 28,
        borderRadius: "50%",
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        fontSize: "0.8rem",
        fontWeight: 700,
        color: "#fff",
        background: "linear-gradient(135deg, var(--brand), #22d3ee)",
      }}
    >
      {initial}
    </span>
  );
}
