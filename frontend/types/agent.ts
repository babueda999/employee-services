export type AgentRole = "USER" | "MANAGER" | "ADMIN";

export interface AgentRequest {
  message: string;
  role?: AgentRole;
}

export interface AgentResponse {
  reply: string;
}

export interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
}
