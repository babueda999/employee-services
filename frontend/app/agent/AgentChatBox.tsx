"use client";

import { useEffect, useRef, useState } from "react";
import type { AgentRole, ChatMessage } from "@/types/agent";
import MessageContent from "./MessageContent";
import SuggestedPrompts from "./SuggestedPrompts";
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

// Mirrors agent.subagents — the backend tool name each one owns.
const AGENT_LABELS: Record<string, string> = {
  get_employee: "Get Employee Agent",
  list_employees: "List Employees Agent",
  search_employees: "Search Employee Agent",
  update_employee: "Update Employee Agent",
  adjust_salary: "Adjust Salary Agent",
  delete_employee: "Delete Employee Agent",
};

function agentLabel(toolName: string): string {
  return AGENT_LABELS[toolName] ?? toolName;
}

type ToolCategory = "read" | "write" | "delete";

function toolCategory(tool: string): ToolCategory {
  if (tool === "delete_employee") {
    return "delete";
  }
  if (tool === "update_employee" || tool === "adjust_salary") {
    return "write";
  }
  return "read";
}

const TOOL_BADGE_CLASS: Record<ToolCategory, string> = {
  read: styles.agentBadgeRead,
  write: styles.agentBadgeWrite,
  delete: styles.agentBadgeDelete,
};

const ROLE_PERMISSION_CLASS: Record<AgentRole, string> = {
  USER: styles.rolePermissionUser,
  MANAGER: styles.rolePermissionManager,
  ADMIN: styles.rolePermissionAdmin,
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
  const [conversationId, setConversationId] = useState<string | undefined>(undefined);
  const [pending, setPending] = useState(false);
  const [confirmingId, setConfirmingId] = useState<string | null>(null);
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
        body: JSON.stringify({ message: trimmed, role, conversationId }),
      });

      const data = (await response.json()) as {
        reply?: string;
        message?: string;
        conversationId?: string;
        confirmationRequired?: boolean;
        confirmationToken?: string | null;
        toolsUsed?: string[];
      };

      if (!response.ok) {
        throw new Error(data.message ?? "The employee agent could not respond.");
      }

      if (data.conversationId) {
        setConversationId(data.conversationId);
      }

      setMessages((prev) => [
        ...prev,
        {
          id: newId(),
          role: "assistant",
          content: data.reply ?? "",
          confirmationToken: data.confirmationRequired ? data.confirmationToken : undefined,
          confirmationStatus: data.confirmationRequired ? "pending" : undefined,
          toolsUsed: data.toolsUsed,
        },
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

  async function handleConfirm(message: ChatMessage, approve: boolean) {
    if (!message.confirmationToken || confirmingId) {
      return;
    }

    setConfirmingId(message.id);
    setError(null);

    try {
      const response = await fetch("/api/agent/confirm", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          confirmationToken: message.confirmationToken,
          approve,
          role,
        }),
      });

      const data = (await response.json()) as {
        reply?: string;
        message?: string;
        conversationId?: string;
        toolsUsed?: string[];
      };

      if (!response.ok) {
        throw new Error(data.message ?? "Could not resolve that confirmation.");
      }

      setMessages((prev) =>
        prev.map((item) =>
          item.id === message.id
            ? { ...item, confirmationStatus: approve ? "approved" : "denied" }
            : item,
        ),
      );

      setMessages((prev) => [
        ...prev,
        {
          id: newId(),
          role: "assistant",
          content: data.reply ?? "",
          toolsUsed: data.toolsUsed,
        },
      ]);

      if (data.conversationId) {
        setConversationId(data.conversationId);
      }
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Could not resolve that confirmation.",
      );
    } finally {
      setConfirmingId(null);
    }
  }

  function handleReset() {
    setMessages([]);
    setConversationId(undefined);
    setError(null);
    setInput("");
    setConfirmingId(null);
  }

  return (
    <div>
      <div className={styles.toolbar}>
        <button
          type="button"
          className={styles.resetButton}
          onClick={handleReset}
          disabled={pending || messages.length === 0}
        >
          New conversation
        </button>
      </div>

      <div className={styles.chatWindow} ref={chatWindowRef}>
        {messages.length === 0 && (
          <div className={styles.empty}>
            <p>
              Ask about an employee — e.g. &quot;List all employees&quot; or
              &quot;Find employees in Engineering&quot;.
            </p>
            <SuggestedPrompts onSelect={setInput} disabled={pending} />
          </div>
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
              {message.role === "assistant" && message.toolsUsed && message.toolsUsed.length > 0 && (
                <div className={styles.agentBadgeRow}>
                  {message.toolsUsed.map((tool) => (
                    <span
                      key={tool}
                      className={`${styles.agentBadge} ${TOOL_BADGE_CLASS[toolCategory(tool)]}`}
                    >
                      {agentLabel(tool)}
                    </span>
                  ))}
                </div>
              )}

              {message.role === "assistant" ? (
                <MessageContent content={message.content} />
              ) : (
                message.content
              )}

              {message.confirmationStatus === "pending" && (
                <>
                  {role !== "MANAGER" && (
                    <p className={styles.confirmHint}>
                      Switch &quot;Acting as&quot; to MANAGER to approve or deny this —
                      whoever requested it can&apos;t also approve it.
                    </p>
                  )}
                  <div className={styles.confirmRow}>
                    <button
                      type="button"
                      className={styles.approveButton}
                      disabled={confirmingId !== null}
                      onClick={() => handleConfirm(message, true)}
                    >
                      {confirmingId === message.id ? "Confirming..." : "Approve"}
                    </button>
                    <button
                      type="button"
                      className={styles.denyButton}
                      disabled={confirmingId !== null}
                      onClick={() => handleConfirm(message, false)}
                    >
                      Deny
                    </button>
                  </div>
                </>
              )}

              {message.confirmationStatus === "approved" && (
                <p className={styles.confirmResolved}>✓ Approved</p>
              )}

              {message.confirmationStatus === "denied" && (
                <p className={styles.confirmResolved}>✗ Denied</p>
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

      <div className={styles.rolePanel}>
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
          <span className={`${styles.rolePermission} ${ROLE_PERMISSION_CLASS[role]}`}>
            {ROLE_DESCRIPTIONS[role]}
          </span>
        </label>
      </div>

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
