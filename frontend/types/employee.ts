export interface Employee {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  department: string;
  salary: number;
  remoteWorkEligible: boolean | null;
}

export interface CreateEmployeeRequest {
  firstName: string;
  lastName: string;
  email: string;
  department: string;
  salary: number;
  remoteWorkEligible: boolean;
}

export type UpdateEmployeeRequest = CreateEmployeeRequest;

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
