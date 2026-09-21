import Link from "next/link";
import { notFound } from "next/navigation";
import { ApiError, getEmployeeById } from "@/lib/employees";
import DeleteEmployeeButton from "../DeleteEmployeeButton";
import styles from "../employees.module.css";

export default async function EmployeeDetailPage(
  props: PageProps<"/employees/[id]">,
) {
  const { id } = await props.params;

  let employee;

  try {
    employee = await getEmployeeById(id);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      notFound();
    }

    throw error;
  }

  return (
    <main className={styles.main}>
      <Link href="/employees" className={styles.backLink}>
        &larr; Back to employees
      </Link>

      <h1 className={styles.brand}>Sesha LLC</h1>
      <h2>
        {employee.firstName} {employee.lastName}
      </h2>

      <div className={styles.field}>
        <span className={styles.label}>Email:</span>
        {employee.email}
      </div>
      <div className={styles.field}>
        <span className={styles.label}>Department:</span>
        {employee.department}
      </div>
      <div className={styles.field}>
        <span className={styles.label}>Salary:</span>
        {employee.salary.toLocaleString()}
      </div>

      <div className={styles.actions}>
        <Link href={`/employees/${employee.id}/edit`}>Edit</Link>
        <DeleteEmployeeButton
          id={employee.id}
          name={`${employee.firstName} ${employee.lastName}`}
        />
      </div>
    </main>
  );
}
