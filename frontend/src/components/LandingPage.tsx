"use client";

import { useEffect, useRef, useState } from "react";
import { useAuth } from "@/components/AuthProvider";

export function LandingPage() {
  return (
    <div className="landing">
      <HeroSection />
      <StatsBar />
      <FeaturesSection />
      <WorkflowSection />
      <DemoSection />
      <CTASection />
    </div>
  );
}

/* ------------------------------------------------------------------ Hero -- */

function HeroSection() {
  const { user } = useAuth();
  const ctaHref = user ? "/dashboard" : "/login";

  return (
    <section className="landing-hero">
      <div className="landing-hero-bg" aria-hidden>
        <div className="hero-orb hero-orb--1" />
        <div className="hero-orb hero-orb--2" />
        <div className="hero-orb hero-orb--3" />
      </div>
      <div className="landing-hero-content">
        <span className="landing-pill">
          <span className="landing-pill-dot" />
          AI-Powered Code Review
        </span>
        <h1 className="landing-title">
          Catch bugs before<br />they ship.{" "}
          <span className="landing-title-accent">Automatically.</span>
        </h1>
        <p className="landing-subtitle">
          DevSleuth combines static analysis, AI reasoning, and your team&apos;s
          context to surface real issues in every pull request — not noise.
        </p>
        <div className="landing-hero-actions">
          <a href={ctaHref} className="btn btn-primary btn-lg landing-btn-glow">
            Get Started
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden>
              <path d="M3 8h10M9 4l4 4-4 4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
          </a>
          <a href="#how-it-works" className="btn btn-ghost btn-lg">
            See How It Works
          </a>
        </div>
      </div>
      <div className="landing-hero-visual" aria-hidden>
        <HeroCodeBlock />
      </div>
    </section>
  );
}

function HeroCodeBlock() {
  return (
    <div className="hero-code">
      <div className="hero-code-bar">
        <div className="hero-code-dots">
          <span className="hero-dot" style={{ background: "#f87171" }} />
          <span className="hero-dot" style={{ background: "#fbbf24" }} />
          <span className="hero-dot" style={{ background: "#34d399" }} />
        </div>
        <span className="hero-code-filename">TransferService.java</span>
      </div>
      <pre className="hero-code-body">
        <code>
          <Line n={1} text='public void transfer(Account from, Account to,' />
          <Line n={2} text='                     BigDecimal amount) {' />
          <Line n={3} text='    if (amount.compareTo(BigDecimal.ZERO) <= 0)' highlight="warn" />
          <Line n={4} text='        throw new IllegalArgumentException(' />
          <Line n={5} text='            "amount must be positive");' />
          <Line n={6} text='    from.debit(amount);' />
          <Line n={7} text='    to.credit(amount);' highlight="issue" />
          <Line n={8} text='}' />
        </code>
      </pre>
      <div className="hero-finding-wrap">
        <div className="hero-finding slide-in">
          <div className="hero-finding-badge">AI</div>
          <div className="hero-finding-content">
            <strong>Race condition detected</strong>
            <p>debit + credit are not atomic. Wrap in <code>@Transactional</code> to prevent partial transfers.</p>
          </div>
          <span className="hero-finding-severity">Critical</span>
        </div>
      </div>
    </div>
  );
}

function Line({ n, text, highlight }: { n: number; text: string; highlight?: string }) {
  return (
    <span className={`hero-line ${highlight ? `hero-line--${highlight}` : ""}`}>
      <span className="hero-line-n">{n}</span>
      {text}
      {"\n"}
    </span>
  );
}

/* ------------------------------------------------------------ Stats bar --- */

function StatsBar() {
  const ref = useRef<HTMLDivElement>(null);
  useReveal(ref);

  return (
    <section className="landing-stats" ref={ref}>
      <AnimatedStat value={3} label="Analysis Engines" suffix="" />
      <AnimatedStat value={95} label="Accuracy Rate" suffix="%" />
      <AnimatedStat value={2} label="Minute Setup" suffix=" min" />
      <AnimatedStat value={0} label="Config Needed" suffix="" />
    </section>
  );
}

function AnimatedStat({ value, label, suffix }: { value: number; label: string; suffix: string }) {
  const [count, setCount] = useState(0);
  const ref = useRef<HTMLDivElement>(null);
  const started = useRef(false);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting && !started.current) {
        started.current = true;
        let frame = 0;
        const totalFrames = 40;
        const step = () => {
          frame++;
          setCount(Math.min(Math.round((frame / totalFrames) * value), value));
          if (frame < totalFrames) requestAnimationFrame(step);
        };
        requestAnimationFrame(step);
        observer.disconnect();
      }
    }, { threshold: 0.5 });
    observer.observe(el);
    return () => observer.disconnect();
  }, [value]);

  return (
    <div className="landing-stat-item" ref={ref}>
      <span className="landing-stat-value">{count}{suffix}</span>
      <span className="landing-stat-label">{label}</span>
    </div>
  );
}

/* --------------------------------------------------------------- Features -- */

function FeaturesSection() {
  const ref = useRef<HTMLDivElement>(null);
  useReveal(ref);

  return (
    <section className="landing-section" ref={ref}>
      <span className="landing-section-tag">Features</span>
      <h2 className="landing-section-title">Three engines. Zero blind spots.</h2>
      <p className="landing-section-sub">
        Each engine catches what the others miss. Together, they cover style, safety, and semantics.
      </p>
      <div className="landing-features">
        <FeatureCard
          icon={<IconShield />}
          title="Static Analysis"
          desc="Checkstyle, SpotBugs, and Semgrep run on every diff. Catches null dereferences, style violations, and security anti-patterns before review."
          color="var(--brand)"
        />
        <FeatureCard
          icon={<IconBrain />}
          title="AI Reasoning"
          desc="An LLM reads the full PR context — not just the diff — to find logic errors, race conditions, and missing edge-case handling."
          color="#22d3ee"
        />
        <FeatureCard
          icon={<IconFlask />}
          title="Experiments"
          desc="A/B test review strategies across repositories. Compare accuracy, time-to-merge, and false-positive rates with real data."
          color="#a78bfa"
        />
      </div>
    </section>
  );
}

function FeatureCard({ icon, title, desc, color }: { icon: React.ReactNode; title: string; desc: string; color: string }) {
  return (
    <div className="landing-feature-card" style={{ ["--card-accent" as string]: color }}>
      <div className="landing-feature-icon-wrap">{icon}</div>
      <h3>{title}</h3>
      <p>{desc}</p>
    </div>
  );
}

/* --------------------------------------------------------------- Workflow -- */

function WorkflowSection() {
  const ref = useRef<HTMLDivElement>(null);
  useReveal(ref);

  return (
    <section className="landing-section landing-section--dark" id="how-it-works" ref={ref}>
      <span className="landing-section-tag">Workflow</span>
      <h2 className="landing-section-title">From push to feedback in seconds</h2>
      <p className="landing-section-sub">Every PR triggers an automated pipeline — no manual steps.</p>
      <div className="landing-workflow">
        <WorkflowStep
          step={1}
          title="Connect Repository"
          desc="Link your GitHub repos via OAuth. One click, zero config files."
          visual={<WfVisualConnect />}
        />
        <WorkflowStep
          step={2}
          title="Diff Extraction & Analysis"
          desc="DevSleuth parses the PR diff, runs three static analyzers in parallel, and feeds the code context to the AI engine."
          visual={<WfVisualAnalyze />}
        />
        <WorkflowStep
          step={3}
          title="Smart Findings"
          desc="Results are ranked by severity, deduplicated across engines, and mapped to exact lines in your diff. No noise."
          visual={<WfVisualFindings />}
        />
        <WorkflowStep
          step={4}
          title="Ship with Confidence"
          desc="Address real issues, dismiss false positives with one click. Track team improvement over time with built-in metrics."
          visual={<WfVisualShip />}
        />
      </div>
    </section>
  );
}

function WorkflowStep({ step, title, desc, visual }: { step: number; title: string; desc: string; visual: React.ReactNode }) {
  const ref = useRef<HTMLDivElement>(null);
  useReveal(ref);

  return (
    <div className="landing-wf-step" ref={ref}>
      <div className="landing-wf-left">
        <div className="landing-wf-num">{step}</div>
        <div className="landing-wf-text">
          <h3 className="landing-wf-title">{title}</h3>
          <p className="landing-wf-desc">{desc}</p>
        </div>
      </div>
      <div className="landing-wf-visual">{visual}</div>
    </div>
  );
}

/* Workflow mini-visuals */

function WfVisualConnect() {
  return (
    <div className="wf-mini wf-mini--connect">
      <div className="wf-mini-icon">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none"><path d="M12 2C6.48 2 2 6.48 2 12c0 4.42 2.87 8.17 6.84 9.49.5.09.66-.22.66-.48v-1.7c-2.78.6-3.37-1.34-3.37-1.34-.45-1.16-1.11-1.47-1.11-1.47-.91-.62.07-.61.07-.61 1 .07 1.53 1.03 1.53 1.03.89 1.52 2.34 1.08 2.91.83.09-.65.35-1.08.63-1.33-2.22-.25-4.56-1.11-4.56-4.95 0-1.09.39-1.98 1.03-2.68-.1-.25-.45-1.27.1-2.64 0 0 .84-.27 2.75 1.02A9.56 9.56 0 0112 6.8c.85.004 1.71.115 2.51.337 1.91-1.29 2.75-1.02 2.75-1.02.55 1.37.2 2.39.1 2.64.64.7 1.03 1.59 1.03 2.68 0 3.85-2.34 4.7-4.57 4.94.36.31.68.92.68 1.85v2.75c0 .26.16.57.67.48A10.01 10.01 0 0022 12c0-5.52-4.48-10-10-10z" fill="currentColor"/></svg>
      </div>
      <div className="wf-mini-line wf-mini-line--animated" />
      <div className="wf-mini-badge">Connected</div>
    </div>
  );
}

function WfVisualAnalyze() {
  return (
    <div className="wf-mini wf-mini--analyze">
      <div className="wf-mini-engines">
        <span className="wf-engine wf-engine--1">Checkstyle</span>
        <span className="wf-engine wf-engine--2">SpotBugs</span>
        <span className="wf-engine wf-engine--3">Semgrep</span>
      </div>
      <div className="wf-mini-progress">
        <div className="wf-mini-progress-fill" />
      </div>
      <span className="wf-engine wf-engine--ai">AI Engine</span>
    </div>
  );
}

function WfVisualFindings() {
  return (
    <div className="wf-mini wf-mini--findings">
      <div className="wf-finding-row wf-finding-row--critical">
        <span className="wf-sev-dot" />Critical: Race condition
      </div>
      <div className="wf-finding-row wf-finding-row--high">
        <span className="wf-sev-dot" />High: Unchecked null
      </div>
      <div className="wf-finding-row wf-finding-row--medium">
        <span className="wf-sev-dot" />Medium: Missing validation
      </div>
    </div>
  );
}

function WfVisualShip() {
  return (
    <div className="wf-mini wf-mini--ship">
      <div className="wf-merge-icon">
        <svg width="20" height="20" viewBox="0 0 16 16" fill="none"><circle cx="4" cy="4" r="2" stroke="currentColor" strokeWidth="1.5"/><circle cx="4" cy="12" r="2" stroke="currentColor" strokeWidth="1.5"/><circle cx="12" cy="12" r="2" stroke="currentColor" strokeWidth="1.5"/><path d="M4 6v4M4 8c0-2 4-2 8 2" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/></svg>
      </div>
      <span className="wf-merge-label">Merged with confidence</span>
    </div>
  );
}

/* ---------------------------------------------------------------- Demo ---- */

function DemoSection() {
  const ref = useRef<HTMLDivElement>(null);
  useReveal(ref);

  return (
    <section className="landing-section" ref={ref}>
      <span className="landing-section-tag">Live Preview</span>
      <h2 className="landing-section-title">See it in action</h2>
      <p className="landing-section-sub">A typical review finds issues across multiple categories.</p>
      <div className="landing-demo">
        <div className="landing-demo-sidebar">
          <DemoFindingItem severity="critical" title="Race condition in transfer()" file="TransferService.java:7" />
          <DemoFindingItem severity="high" title="Null pointer dereference" file="UserService.java:42" />
          <DemoFindingItem severity="medium" title="Missing input validation" file="ApiController.java:18" />
          <DemoFindingItem severity="low" title="Unused import statement" file="Config.java:3" />
        </div>
        <div className="landing-demo-main">
          <div className="landing-demo-header">
            <span className="landing-demo-pr">PR #247</span>
            <span className="landing-demo-title">Add fund transfer feature</span>
          </div>
          <div className="landing-demo-metrics">
            <DemoMetric label="Issues Found" value="4" />
            <DemoMetric label="Critical" value="1" accent="var(--critical)" />
            <DemoMetric label="AI Suggestions" value="3" accent="var(--brand)" />
            <DemoMetric label="Review Time" value="8s" />
          </div>
          <div className="landing-demo-chart">
            <div className="demo-chart-bar" style={{ ["--bar-h" as string]: "85%", ["--bar-color" as string]: "var(--critical)" }} />
            <div className="demo-chart-bar" style={{ ["--bar-h" as string]: "65%", ["--bar-color" as string]: "var(--high)" }} />
            <div className="demo-chart-bar" style={{ ["--bar-h" as string]: "45%", ["--bar-color" as string]: "var(--medium)" }} />
            <div className="demo-chart-bar" style={{ ["--bar-h" as string]: "25%", ["--bar-color" as string]: "var(--low)" }} />
          </div>
        </div>
      </div>
    </section>
  );
}

function DemoFindingItem({ severity, title, file }: { severity: string; title: string; file: string }) {
  return (
    <div className={`landing-demo-finding landing-demo-finding--${severity}`}>
      <span className="landing-demo-sev-dot" />
      <div>
        <div className="landing-demo-finding-title">{title}</div>
        <div className="landing-demo-finding-file">{file}</div>
      </div>
    </div>
  );
}

function DemoMetric({ label, value, accent }: { label: string; value: string; accent?: string }) {
  return (
    <div className="landing-demo-metric">
      <span className="landing-demo-metric-value" style={{ color: accent }}>{value}</span>
      <span className="landing-demo-metric-label">{label}</span>
    </div>
  );
}

/* ------------------------------------------------------------------ CTA --- */

function CTASection() {
  const ref = useRef<HTMLDivElement>(null);
  const { user } = useAuth();
  const ctaHref = user ? "/dashboard" : "/login";
  useReveal(ref);

  return (
    <section className="landing-cta" ref={ref}>
      <div className="landing-cta-glow" aria-hidden />
      <h2 className="landing-cta-title">Ready to ship better code?</h2>
      <p className="landing-cta-sub">
        Sign in with GitHub and run your first automated review in under two minutes. Free for open source.
      </p>
      <a href={ctaHref} className="btn btn-primary btn-lg landing-btn-glow" style={{ marginTop: "1.5rem" }}>
        Get Started Free →
      </a>
    </section>
  );
}

/* ------------------------------------------------------------ Scroll reveal */

function useReveal(ref: React.RefObject<HTMLElement | null>) {
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          el.classList.add("revealed");
          observer.disconnect();
        }
      },
      { threshold: 0.1 }
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, [ref]);
}

/* ------------------------------------------------------------------ Icons -- */

function IconShield() {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
      <path d="M9 12l2 2 4-4"/>
    </svg>
  );
}

function IconBrain() {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M9.5 2A5.5 5.5 0 004 7.5c0 1.13.34 2.18.93 3.06A4.5 4.5 0 003 14.5 4.5 4.5 0 007.5 19h1v3h7v-3h1a4.5 4.5 0 004.5-4.5 4.5 4.5 0 00-1.93-3.94A5.5 5.5 0 0020 7.5 5.5 5.5 0 0014.5 2h-5z"/>
      <path d="M12 2v20"/>
    </svg>
  );
}

function IconFlask() {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M9 3h6M10 3v6.5L4 20h16l-6-10.5V3"/>
      <path d="M7 17h10"/>
    </svg>
  );
}
