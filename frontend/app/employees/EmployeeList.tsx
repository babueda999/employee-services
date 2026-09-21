"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import type { Employee } from "@/types/employee";
import DeleteEmployeeButton from "./DeleteEmployeeButton";
import styles from "./employees.module.css";

const POLL_INTERVAL_MS = 5000;

interface EmployeeListProps {
  initialEmployees: Employee[];
  initialError: string | null;
}

export default function EmployeeList({
  initialEmployees,
  initialError,
}: EmployeeListProps) {
  const [employees, setEmployees] = useState(initialEmployees);
  const [loadError, setLoadError] = useState(initialError);

  useEffect(() => {
    let cancelled = false;

    const poll = async () => {
      try {
        const response = await fetch("/api/employees", { cache: "no-store" });

        if (!response.ok) {
          const body = (await response.json().catch(() => null)) as
            | { message?: string }
            | null;
          throw new Error(body?.message ?? "Unable to reach the employee service.");
        }

        const data = (await response.json()) as Employee[];

        if (!cancelled) {
          setEmployees(data);
          setLoadError(null);
        }
      } catch (error) {
        if (!cancelled) {
          setLoadError(
            error instanceof Error
              ? error.message
              : "Unable to reach the employee service.",
          );
        }
      }
    };

    const intervalId = setInterval(poll, POLL_INTERVAL_MS);

    return () => {
      cancelled = true;
      clearInterval(intervalId);
    };
  }, []);

  return (
    <>
      {loadError && <p className={styles.error}>{loadError}</p>}

      {!loadError && (
        <p className={styles.count}>
          {employees.length} {employees.length === 1 ? "employee" : "employees"}
        </p>
      )}

      {!loadError && employees.length === 0 && <p>No employees found.</p>}

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
    </>
  );
}
