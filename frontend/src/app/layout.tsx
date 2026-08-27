import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "DevSleuth",
  description: "AI-assisted hybrid code review platform",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
