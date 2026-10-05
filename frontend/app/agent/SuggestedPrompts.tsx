"use client";

import styles from "./agent.module.css";

export const SUGGESTED_PROMPTS = [
  "List all employees",
  "Find employees in Engineering",
  "Show me the highest-paid employee",
  "Update an employee's department",
] as const;

interface SuggestedPromptsProps {
  onSelect: (prompt: string) => void;
  disabled: boolean;
}

export default function SuggestedPrompts({ onSelect, disabled }: SuggestedPromptsProps) {
  return (
    <div className={styles.suggestedPrompts} role="group" aria-label="Suggested prompts">
      {SUGGESTED_PROMPTS.map((prompt) => (
        <button
          key={prompt}
          type="button"
          className={styles.promptChip}
          disabled={disabled}
          onClick={() => onSelect(prompt)}
        >
          {prompt}
        </button>
      ))}
    </div>
  );
}
