import type { AgentRequest, AgentResponse, AgentRole } from "@/types/agent";
import type { ApiErrorResponse } from "@/types/employee";

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

export async function askAgent(message: string, role?: AgentRole): Promise<string> {
  const request: AgentRequest = { message, role };

  const response = await fetch(`${API_BASE_URL}/api/agent`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw new ApiError(response.status, await parseErrorBody(response));
  }

  const data = (await response.json()) as AgentResponse;

  if (!data.reply || data.reply.trim() === "") {
    console.info("No employee records are available");
  }

  return data.reply;
}
