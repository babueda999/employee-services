"use client";

import { useEffect, useState } from "react";
import type { Employee } from "@/types/employee";
import styles from "./page.module.css";

const POLL_INTERVAL_MS = 5000;

interface EmployeeCountStatProps {
  initialCount: number | null;
}

export default function EmployeeCountStat({
  initialCount,
}: EmployeeCountStatProps) {
  const [count, setCount] = useState(initialCount);

  useEffect(() => {
    let cancelled = false;

    const poll = async () => {
      try {
        const response = await fetch("/api/employees", { cache: "no-store" });

        if (!response.ok) {
          return;
        }

        const data = (await response.json()) as Employee[];

        if (!cancelled) {
          setCount(data.length);
        }
      } catch {
        // Keep showing the last known count if a poll fails.
      }
    };

    const intervalId = setInterval(poll, POLL_INTERVAL_MS);

    return () => {
      cancelled = true;
      clearInterval(intervalId);
    };
  }, []);

  if (count === null) {
    return null;
  }

  return (
    <div className={styles.stat}>
      <span className={styles.statNumber}>{count}</span>
      <span>{count === 1 ? "employee" : "employees"} on record</span>
    </div>
  );
}
