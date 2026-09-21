"use client";

import { useActionState } from "react";
import type { Employee } from "@/types/employee";
import type { FormState } from "./actions";
import styles from "./employees.module.css";

interface EmployeeFormProps {
  action: (prevState: FormState, formData: FormData) => Promise<FormState>;
  initialValues?: Employee;
  submitLabel: string;
}

const initialState: FormState = { error: null };

export default function EmployeeForm({
  action,
  initialValues,
  submitLabel,
}: EmployeeFormProps) {
  const [state, formAction, pending] = useActionState(action, initialState);

  return (
    <form action={formAction} className={styles.form}>
      {state.error && <p className={styles.error}>{state.error}</p>}

      <label className={styles.formField}>
        First name
        <input
          name="firstName"
          type="text"
          required
          defaultValue={initialValues?.firstName}
        />
      </label>

      <label className={styles.formField}>
        Last name
        <input
          name="lastName"
          type="text"
          required
          defaultValue={initialValues?.lastName}
        />
      </label>

      <label className={styles.formField}>
        Email
        <input
          name="email"
          type="email"
          required
          defaultValue={initialValues?.email}
        />
      </label>

      <label className={styles.formField}>
        Department
        <input
          name="department"
          type="text"
          required
          defaultValue={initialValues?.department}
        />
      </label>

      <label className={styles.formField}>
        Salary
        <input
          name="salary"
          type="number"
          step="0.01"
          min="0.01"
          required
          defaultValue={initialValues?.salary}
        />
      </label>

      <button type="submit" disabled={pending}>
        {pending ? "Saving..." : submitLabel}
      </button>
    </form>
  );
}
