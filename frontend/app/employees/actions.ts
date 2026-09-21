"use server";

import { redirect } from "next/navigation";
import { revalidatePath } from "next/cache";
import {
  ApiError,
  createEmployee,
  deleteEmployee,
  updateEmployee,
} from "@/lib/employees";
import type { CreateEmployeeRequest } from "@/types/employee";

export interface FormState {
  error: string | null;
}

function parseFormData(formData: FormData): CreateEmployeeRequest {
  return {
    firstName: String(formData.get("firstName") ?? ""),
    lastName: String(formData.get("lastName") ?? ""),
    email: String(formData.get("email") ?? ""),
    department: String(formData.get("department") ?? ""),
    salary: Number(formData.get("salary")),
  };
}

export async function createEmployeeAction(
  _prevState: FormState,
  formData: FormData,
): Promise<FormState> {
  let created;

  try {
    created = await createEmployee(parseFormData(formData));
  } catch (error) {
    if (error instanceof ApiError) {
      return { error: error.message };
    }

    return { error: "Unable to reach the employee service." };
  }

  revalidatePath("/employees");
  redirect(`/employees/${created.id}`);
}

export async function updateEmployeeAction(
  id: string,
  _prevState: FormState,
  formData: FormData,
): Promise<FormState> {
  try {
    await updateEmployee(id, parseFormData(formData));
  } catch (error) {
    if (error instanceof ApiError) {
      return { error: error.message };
    }

    return { error: "Unable to reach the employee service." };
  }

  revalidatePath("/employees");
  revalidatePath(`/employees/${id}`);
  redirect(`/employees/${id}`);
}

export async function deleteEmployeeAction(
  id: string,
  _prevState: FormState,
  _formData: FormData,
): Promise<FormState> {
  try {
    await deleteEmployee(id);
  } catch (error) {
    if (error instanceof ApiError) {
      return { error: error.message };
    }

    return { error: "Unable to reach the employee service." };
  }

  revalidatePath("/employees");
  redirect("/employees");
}
