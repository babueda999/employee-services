import { NextResponse } from "next/server";
import { ApiError, getAllEmployees } from "@/lib/employees";

export async function GET() {
  try {
    const employees = await getAllEmployees();
    return NextResponse.json(employees);
  } catch (error) {
    const status = error instanceof ApiError ? error.status : 502;
    const message =
      error instanceof ApiError
        ? error.message
        : "Unable to reach the employee service.";

    return NextResponse.json({ message }, { status });
  }
}
