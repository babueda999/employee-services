"use client";

import { useEffect, useRef, useState } from "react";
import type { AgentRole, ChatMessage } from "@/types/agent";
import MessageContent from "./MessageContent";
import styles from "./agent.module.css";

const ROLES: AgentRole[] = ["USER", "MANAGER", "ADMIN"];

// Mirrors the backend's role model (AuthorizationGuardrail).
const ROLE_LABELS: Record<AgentRole, string> = {
  USER: "view",
  MANAGER: "update (incl. salary changes)",
  ADMIN: "view, update, delete",
};

const ROLE_DESCRIPTIONS: Record<AgentRole, string> = {
  USER: "USER can only view. Other requests will be denied.",
  MANAGER:
    "MANAGER can only update, including salary changes. View and delete requests will be denied.",
  ADMIN:
    "ADMIN can view, update, and delete — except salary changes, which are MANAGER-only.",
};

function newId(): string {
  return typeof crypto !== "undefined" && "randomUUID" in crypto
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random()}`;
}

export default function AgentChatBox() {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [role, setRole] = useState<AgentRole>("USER");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const chatWindowRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = chatWindowRef.current;
    if (el) {
      el.scrollTop = el.scrollHeight;
    }
  }, [messages, pending]);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();

    const trimmed = input.trim();
    if (trimmed === "" || pending) {
      return;
    }

    const userMessage: ChatMessage = {
      id: newId(),
      role: "user",
      content: trimmed,
    };

    setMessages((prev) => [...prev, userMessage]);
    setInput("");
    setError(null);
    setPending(true);

    try {
      const response = await fetch("/api/agent", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ message: trimmed, role }),
      });

      const data = (await response.json()) as { reply?: string; message?: string };

      if (!response.ok) {
        throw new Error(data.message ?? "The employee agent could not respond.");
      }

      setMessages((prev) => [
        ...prev,
        { id: newId(), role: "assistant", content: data.reply ?? "" },
      ]);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "The employee agent could not respond.",
      );
    } finally {
      setPending(false);
    }
  }

  return (
    <div>
      <div className={styles.chatWindow} ref={chatWindowRef}>
        {messages.length === 0 && (
          <p className={styles.empty}>
            Ask about an employee — e.g. &quot;List all employees&quot; or
            &quot;Find employees in Engineering&quot;.
          </p>
        )}

        {messages.map((message) => (
          <div
            key={message.id}
            className={`${styles.bubbleRow} ${
              message.role === "user"
                ? styles.bubbleRowUser
                : styles.bubbleRowAssistant
            }`}
          >
            <div
              className={`${styles.bubble} ${
                message.role === "user"
                  ? styles.bubbleUser
                  : styles.bubbleAssistant
              }`}
            >
              {message.role === "assistant" ? (
                <MessageContent content={message.content} />
              ) : (
                message.content
              )}
            </div>
          </div>
        ))}

        {pending && (
          <div className={`${styles.bubbleRow} ${styles.bubbleRowAssistant}`}>
            <div className={`${styles.bubble} ${styles.bubbleAssistant}`}>
              Thinking...
            </div>
          </div>
        )}
      </div>

      {error && <p className={styles.error}>{error}</p>}

      <label className={styles.roleLabel}>
        Acting as:{" "}
        <select
          className={styles.roleSelect}
          value={role}
          onChange={(event) => setRole(event.target.value as AgentRole)}
          disabled={pending}
        >
          {ROLES.map((roleOption) => (
            <option key={roleOption} value={roleOption}>
              {roleOption} — {ROLE_LABELS[roleOption]}
            </option>
          ))}
        </select>
        <span className={styles.rolePermission}>{ROLE_DESCRIPTIONS[role]}</span>
      </label>

      <form className={styles.form} onSubmit={handleSubmit}>
        <input
          type="text"
          className={styles.input}
          placeholder="Ask the employee agent..."
          value={input}
          onChange={(event) => setInput(event.target.value)}
          disabled={pending}
        />
        <button type="submit" className={styles.sendButton} disabled={pending}>
          {pending ? "Sending..." : "Send"}
        </button>
      </form>
    </div>
  );
}
