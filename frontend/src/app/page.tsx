import { HealthBadge } from "@/components/HealthBadge";

export default function Home() {
  return (
    <main className="min-h-screen bg-zinc-50 dark:bg-zinc-950 text-zinc-900 dark:text-zinc-100 font-sans">
      <div className="mx-auto max-w-2xl px-6 py-24">
        <p className="text-xs uppercase tracking-widest text-violet-600 dark:text-violet-400 font-semibold">
          Daily · v0.0.1
        </p>
        <h1 className="mt-3 text-5xl font-bold tracking-tight">
          Daily
        </h1>
        <p className="mt-4 text-lg text-zinc-600 dark:text-zinc-400 max-w-lg leading-relaxed">
          A personal daily check-in app for habits, with built-in capture for
          things you don&apos;t want to forget.
        </p>

        <div className="mt-10 rounded-xl border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 p-6 shadow-sm">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-zinc-500 dark:text-zinc-400">
            Stack status
          </h2>
          <HealthBadge />
        </div>

        <div className="mt-8 text-sm text-zinc-500 dark:text-zinc-400 space-y-1">
          <p>
            📄 <a className="underline hover:text-zinc-700 dark:hover:text-zinc-200" href="https://github.com/vbhapkar77/daily">github.com/vbhapkar77/daily</a>
          </p>
          <p>
            🏗 Phase 2 (Foundation) — scaffold verified
          </p>
          <p>
            ➡️ Next: feature 001 — auth signup &amp; login
          </p>
        </div>
      </div>
    </main>
  );
}
