import Link from "next/link";
import { ApiError, getAllEmployees } from "@/lib/employees";
import type { Employee } from "@/types/employee";
import EmployeeList from "./EmployeeList";
import styles from "./employees.module.css";

export default async function EmployeesPage() {
  let employees: Employee[];
  let loadError: string | null = null;

  try {
    employees = await getAllEmployees();
  } catch (error) {
    employees = [];
    loadError =
      error instanceof ApiError
        ? error.message
        : "Unable to reach the employee service.";
  }

  return (
    <main className={styles.main}>
      <h1 className={styles.brand}>Sesha LLC</h1>
      <h2>Employees</h2>

      <EmployeeList initialEmployees={employees} initialError={loadError} />

      <Link href="/employees/new" className={styles.newLink}>
        + New employee
      </Link>
    </main>
  );
}
