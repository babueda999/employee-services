import type {
  ApiErrorResponse,
  CreateEmployeeRequest,
  Employee,
  UpdateEmployeeRequest,
} from "@/types/employee";

const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  readonly status: number;
  readonly body: ApiErrorResponse | null;

  constructor(status: number, body: ApiErrorResponse | null) {
    super(body?.message ?? `Request failed with status ${status}`);
    this.status = status;
    this.body = body;
  }
}

async function parseErrorBody(response: Response): Promise<ApiErrorResponse | null> {
  try {
    return (await response.json()) as ApiErrorResponse;
  } catch {
    return null;
  }
}

export async function getAllEmployees(): Promise<Employee[]> {
  const response = await fetch(`${API_BASE_URL}/api/employees`, {
    cache: "no-store",
  });

  if (!response.ok) {
    throw new ApiError(response.status, await parseErrorBody(response));
  }

  const employees = (await response.json()) as Employee[];

  if (employees.length === 0) {
    console.info("No employee records are available");
  }

  return employees;
}

export async function getEmployeeById(id: string): Promise<Employee> {
  const response = await fetch(`${API_BASE_URL}/api/employees/${id}`, {
    cache: "no-store",
  });

  if (!response.ok) {
    throw new ApiError(response.status, await parseErrorBody(response));
  }

  return (await response.json()) as Employee;
}

export async function createEmployee(
  request: CreateEmployeeRequest,
): Promise<Employee> {
  const response = await fetch(`${API_BASE_URL}/api/employees`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw new ApiError(response.status, await parseErrorBody(response));
  }

  return (await response.json()) as Employee;
}

export async function updateEmployee(
  id: string,
  request: UpdateEmployeeRequest,
): Promise<Employee> {
  const response = await fetch(`${API_BASE_URL}/api/employees/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw new ApiError(response.status, await parseErrorBody(response));
  }

  return (await response.json()) as Employee;
}

export async function deleteEmployee(id: string): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/api/employees/${id}`, {
    method: "DELETE",
  });

  if (!response.ok) {
    throw new ApiError(response.status, await parseErrorBody(response));
  }
}
