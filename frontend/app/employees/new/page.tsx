import Link from "next/link";
import { createEmployeeAction } from "../actions";
import EmployeeForm from "../EmployeeForm";
import styles from "../employees.module.css";

export default function NewEmployeePage() {
  return (
    <main className={styles.main}>
      <Link href="/employees" className={styles.backLink}>
        &larr; Back to employees
      </Link>

      <h1 className={styles.brand}>Sesha LLC</h1>
      <h2>New Employee</h2>

      <EmployeeForm action={createEmployeeAction} submitLabel="Create" />
    </main>
  );
}
