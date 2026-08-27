"use client";

import { useEffect, useRef, useState } from "react";

/**
 * Polls a fetcher every `intervalMs` until `shouldStop` returns true.
 */
export function usePolling<T>(
  fetcher: () => Promise<T>,
  shouldStop: (data: T) => boolean,
  intervalMs: number = 3000
) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    let active = true;

    const poll = async () => {
      try {
        const result = await fetcher();
        if (!active) return;
        setData(result);
        setLoading(false);
        if (shouldStop(result) && timerRef.current) {
          clearInterval(timerRef.current);
          timerRef.current = null;
        }
      } catch {
        if (active) setLoading(false);
      }
    };

    poll();
    timerRef.current = setInterval(poll, intervalMs);

    return () => {
      active = false;
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, []);

  return { data, loading };
}
