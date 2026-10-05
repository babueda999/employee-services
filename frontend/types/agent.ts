export type AgentRole = "USER" | "MANAGER" | "ADMIN";

export interface AgentRequest {
  message: string;
  role?: AgentRole;
  conversationId?: string;
}

export interface AgentResponse {
  reply: string;
  conversationId: string;
  confirmationRequired: boolean;
  confirmationToken: string | null;
  /** Tool names of the sub-agent(s) that handled this turn, e.g. ["search_employees"]. */
  toolsUsed: string[];
}

export interface AgentConfirmRequest {
  confirmationToken: string;
  approve: boolean;
  /** Role of the approver — two-person control: must be MANAGER, regardless of who requested the action. */
  role?: AgentRole;
}

/**
 * Lifecycle of a confirmation attached to one assistant message — "pending"
 * until the user clicks Approve/Deny, then locked to the outcome so the
 * buttons can't be used twice for the same token.
 */
export type ConfirmationStatus = "pending" | "approved" | "denied";

export interface TokenUsage {
  model: string;
  requestCount: number;
  inputTokens: number;
  outputTokens: number;
  totalTokens: number;
}

export interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  confirmationToken?: string | null;
  confirmationStatus?: ConfirmationStatus;
  /** Which sub-agent(s) handled this message, for display as a badge. */
  toolsUsed?: string[];
}
