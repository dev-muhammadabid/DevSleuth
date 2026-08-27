export default function Home() {
  return (
    <main style={{ padding: "2rem", fontFamily: "system-ui, sans-serif" }}>
      <h1>DevSleuth</h1>
      <p>AI-assisted hybrid code review platform</p>
      <nav>
        <ul>
          <li>
            <a href="/pull-requests">Pull Requests</a>
          </li>
        </ul>
      </nav>
    </main>
  );
}
