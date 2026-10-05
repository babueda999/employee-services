import Link from "next/link";
import { notFound } from "next/navigation";
import BackLink from "@/app/components/BackLink";
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
      <BackLink href="/employees">Back to employees</BackLink>

      <div className={styles.pageHeader}>
        <h2>
          {employee.firstName} {employee.lastName}
        </h2>
      </div>

      <div className={styles.detailCard}>
        <div className={styles.detailGrid}>
          <span className={styles.detailLabel}>Email</span>
          <span className={styles.detailValue}>{employee.email}</span>

          <span className={styles.detailLabel}>Department</span>
          <span className={styles.detailValue}>
            <span className={styles.deptPill}>{employee.department}</span>
          </span>

          <span className={styles.detailLabel}>Salary</span>
          <span className={`${styles.detailValue} ${styles.salaryCell}`}>
            {employee.salary.toLocaleString()}
          </span>

          <span className={styles.detailLabel}>Remote work eligible</span>
          <span className={styles.detailValue}>
            {employee.remoteWorkEligible === null
              ? "Not specified"
              : employee.remoteWorkEligible
                ? "Yes"
                : "No"}
          </span>
        </div>

        <div className={styles.actions}>
          <Link href={`/employees/${employee.id}/edit`} className={styles.editLink}>
            Edit
          </Link>
          <DeleteEmployeeButton
            id={employee.id}
            name={`${employee.firstName} ${employee.lastName}`}
          />
        </div>
      </div>
    </main>
  );
}
