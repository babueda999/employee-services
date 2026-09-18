import Link from "next/link";
import { ApiError, getAllEmployees } from "@/lib/employees";
import type { Employee } from "@/types/employee";
import DeleteEmployeeButton from "./DeleteEmployeeButton";
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

      {loadError && <p className={styles.error}>{loadError}</p>}

      {!loadError && employees.length === 0 && (
        <p>No employees found.</p>
      )}

      {employees.length > 0 && (
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Department</th>
              <th>Salary</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {employees.map((employee) => (
              <tr key={employee.id}>
                <td>
                  <Link href={`/employees/${employee.id}`}>
                    {employee.firstName} {employee.lastName}
                  </Link>
                </td>
                <td>{employee.email}</td>
                <td>{employee.department}</td>
                <td>{employee.salary.toLocaleString()}</td>
                <td className={styles.rowActions}>
                  <Link href={`/employees/${employee.id}/edit`}>Edit</Link>
                  <DeleteEmployeeButton
                    id={employee.id}
                    name={`${employee.firstName} ${employee.lastName}`}
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <Link href="/employees/new" className={styles.newLink}>
        + New employee
      </Link>
    </main>
  );
}
