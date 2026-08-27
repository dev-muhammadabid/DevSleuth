import type { Metadata } from "next";
import "@/styles/globals.css";
import { Layout } from "@/components/Layout";
import { AuthProvider } from "@/components/AuthProvider";

export const metadata: Metadata = {
  title: "DevSleuth",
  description: "AI-assisted hybrid code review platform",
  icons: {
    icon: "/devsleuth.png",
    apple: "/devsleuth.png",
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <AuthProvider>
          <Layout>{children}</Layout>
        </AuthProvider>
      </body>
    </html>
  );
}
