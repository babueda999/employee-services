export interface AgentRequest {
  message: string;
}

export interface AgentResponse {
  reply: string;
}

export interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
}
