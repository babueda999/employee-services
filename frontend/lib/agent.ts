import type {
  AgentConfirmRequest,
  AgentRequest,
  AgentResponse,
  AgentRole,
  TokenUsage,
} from "@/types/agent";
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

/**
 * Pass back the `conversationId` from a prior response to continue that
 * conversation (the backend replays its recent history as context);
 * omit it to start a new one.
 */
export async function askAgent(
  message: string,
  role?: AgentRole,
  conversationId?: string,
): Promise<AgentResponse> {
  const request: AgentRequest = { message, role, conversationId };

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

  return data;
}

/**
 * Approves or denies a CRITICAL-risk tool call (currently just
 * delete_employee) that the agent held back pending human confirmation.
 * Two-person control: `role` is the *approver's* role, independent of
 * whoever originally requested the action, and must be MANAGER.
 */
export async function confirmAgentAction(
  confirmationToken: string,
  approve: boolean,
  role?: AgentRole,
): Promise<AgentResponse> {
  const request: AgentConfirmRequest = { confirmationToken, approve, role };

  const response = await fetch(`${API_BASE_URL}/api/agent/confirm`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw new ApiError(response.status, await parseErrorBody(response));
  }

  return (await response.json()) as AgentResponse;
}

/**
 * Running total of OpenAI token usage and the model name the agent uses —
 * a simple "how much has this demo cost so far" readout. In-memory on the
 * backend, so it resets whenever that server restarts.
 */
export async function getTokenUsage(): Promise<TokenUsage> {
  const response = await fetch(`${API_BASE_URL}/api/agent/usage`, {
    cache: "no-store",
  });

  if (!response.ok) {
    throw new ApiError(response.status, await parseErrorBody(response));
  }

  return (await response.json()) as TokenUsage;
}
