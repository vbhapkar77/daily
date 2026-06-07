"use client";

import { useEffect, useState } from "react";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

type HealthState = "loading" | "up" | "down";

export function HealthBadge() {
  const [state, setState] = useState<HealthState>("loading");
  const [detail, setDetail] = useState<string>("");

  useEffect(() => {
    const controller = new AbortController();
    fetch(`${API_URL}/actuator/health`, { signal: controller.signal })
      .then(async (res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const json = (await res.json()) as { status: string };
        setState(json.status === "UP" ? "up" : "down");
        setDetail(json.status);
      })
      .catch((e) => {
        setState("down");
        setDetail(e instanceof Error ? e.message : "Unknown error");
      });
    return () => controller.abort();
  }, []);

  const dot =
    state === "loading"
      ? "bg-zinc-400 animate-pulse"
      : state === "up"
        ? "bg-green-500"
        : "bg-red-500";

  const text =
    state === "loading"
      ? "Checking backend…"
      : state === "up"
        ? "Backend healthy"
        : `Backend unreachable (${detail})`;

  return (
    <div className="mt-3 space-y-2 text-sm">
      <div className="flex items-center gap-3">
        <span className={`size-2.5 rounded-full ${dot}`} aria-hidden />
        <span>{text}</span>
      </div>
      <div className="text-xs text-zinc-500 dark:text-zinc-400 font-mono">
        API: {API_URL}
      </div>
    </div>
  );
}
