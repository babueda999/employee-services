import BackLink from "@/app/components/BackLink";
import { createEmployeeAction } from "../actions";
import EmployeeForm from "../EmployeeForm";
import styles from "../employees.module.css";

export default function NewEmployeePage() {
  return (
    <main className={styles.main}>
      <BackLink href="/employees">Back to employees</BackLink>

      <div className={styles.pageHeader}>
        <h2>New Employee</h2>
      </div>

      <EmployeeForm action={createEmployeeAction} submitLabel="Create" />
    </main>
  );
}
