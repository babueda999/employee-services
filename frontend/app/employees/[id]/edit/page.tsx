import { notFound } from "next/navigation";
import BackLink from "@/app/components/BackLink";
import { ApiError, getEmployeeById } from "@/lib/employees";
import { updateEmployeeAction } from "../../actions";
import EmployeeForm from "../../EmployeeForm";
import styles from "../../employees.module.css";

export default async function EditEmployeePage(
  props: PageProps<"/employees/[id]/edit">,
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

  const boundAction = updateEmployeeAction.bind(null, id);

  return (
    <main className={styles.main}>
      <BackLink href={`/employees/${id}`}>Back to employee</BackLink>

      <div className={styles.pageHeader}>
        <h2>Edit Employee</h2>
      </div>

      <EmployeeForm
        action={boundAction}
        initialValues={employee}
        submitLabel="Save"
      />
    </main>
  );
}
