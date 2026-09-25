import Link from "next/link";
import { getAllEmployees } from "@/lib/employees";
import EmployeeCountStat from "./EmployeeCountStat";
import styles from "./page.module.css";

export default async function Home() {
  let employeeCount: number | null = null;

  try {
    employeeCount = (await getAllEmployees()).length;
  } catch {
    employeeCount = null;
  }

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <span className={styles.eyebrow}>Sesha LLC Employee Service</span>
        <h1 className={styles.title}>Manage your team, simply.</h1>
        <p className={styles.subtitle}>
          A clean front end for the Spring Boot Employee Service API — view,
          add, edit, and remove employees in a few clicks.
        </p>

        <EmployeeCountStat initialCount={employeeCount} />

        <div className={styles.actions}>
          <Link href="/employees" className={styles.primary}>
            View employees
          </Link>
          <Link href="/employees/new" className={styles.secondary}>
            Add employee
          </Link>
          <Link href="/agent" className={styles.secondary}>
            Chat with the agent
          </Link>
        </div>
      </div>
    </div>
  );
}
