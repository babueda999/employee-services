"use client";

import { useActionState, useState } from "react";
import { deleteEmployeeAction } from "./actions";
import styles from "./employees.module.css";

interface DeleteEmployeeButtonProps {
  id: number;
  name: string;
}

const initialState = { error: null as string | null };

export default function DeleteEmployeeButton({
  id,
  name,
}: DeleteEmployeeButtonProps) {
  const [confirming, setConfirming] = useState(false);
  const boundAction = deleteEmployeeAction.bind(null, String(id));
  const [state, formAction, pending] = useActionState(
    boundAction,
    initialState,
  );

  if (!confirming) {
    return (
      <button
        type="button"
        className={styles.deleteButton}
        onClick={() => setConfirming(true)}
      >
        Delete
      </button>
    );
  }

  return (
    <form action={formAction} className={styles.deleteConfirm}>
      <span>Delete {name}?</span>
      <button type="submit" className={styles.deleteButton} disabled={pending}>
        {pending ? "Deleting..." : "Confirm"}
      </button>
      <button
        type="button"
        onClick={() => setConfirming(false)}
        disabled={pending}
      >
        Cancel
      </button>
      {state.error && <p className={styles.error}>{state.error}</p>}
    </form>
  );
}
