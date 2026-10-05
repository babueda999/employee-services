"use client";

import { useEffect, useState } from "react";
import type { TokenUsage } from "@/types/agent";
import styles from "./page.module.css";

const BASE_INTRO =
  "This is an Employee Management application, developed by Seshagir Babu Edara, " +
  "for generative and agentic AI demo purposes.";

function usageSentence(tokenUsage: TokenUsage | null): string {
  if (!tokenUsage) {
    return "";
  }

  if (tokenUsage.requestCount === 0) {
    return ` The AI assistant is powered by the ${tokenUsage.model} model and hasn't been used yet this session.`;
  }

  return (
    ` So far, the AI assistant has used approximately ${tokenUsage.totalTokens} tokens ` +
    `across ${tokenUsage.requestCount} requests, powered by the ${tokenUsage.model} model.`
  );
}

function introText(tokenUsage: TokenUsage | null): string {
  return BASE_INTRO + usageSentence(tokenUsage);
}

function speak(text: string, onStart: () => void, onEnd: () => void) {
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.rate = 0.98;
  utterance.onstart = onStart;
  utterance.onend = onEnd;
  utterance.onerror = onEnd;

  window.speechSynthesis.cancel();
  window.speechSynthesis.speak(utterance);
}

interface VoiceOverIntroProps {
  tokenUsage: TokenUsage | null;
}

/**
 * Narrates a short spoken introduction on the home page, including a
 * readout of OpenAI token usage and the model name.
 *
 * The `tokenUsage` prop is only the value from the initial server render —
 * which can be stale by the time this actually mounts (e.g. Next's router
 * cache can serve a cached render of "/" from before the visitor ever used
 * the agent, if they navigate back to it later in the same session). So on
 * mount this re-fetches the live figure from `/api/agent/usage` (the same
 * route AgentChatBox's backend talks to) before speaking or rendering the
 * caption, the same "don't trust the server-rendered snapshot, re-check on
 * mount" pattern `EmployeeCountStat` already uses for the employee count.
 */
export default function VoiceOverIntro({ tokenUsage: initialTokenUsage }: VoiceOverIntroProps) {
  const [tokenUsage, setTokenUsage] = useState(initialTokenUsage);
  const [supported, setSupported] = useState(false);
  const [speaking, setSpeaking] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function loadLatestUsage(): Promise<TokenUsage | null> {
      try {
        const response = await fetch("/api/agent/usage", { cache: "no-store" });

        if (!response.ok) {
          return initialTokenUsage;
        }

        return (await response.json()) as TokenUsage;
      } catch {
        return initialTokenUsage;
      }
    }

    loadLatestUsage().then((latest) => {
      if (cancelled) {
        return;
      }

      setTokenUsage(latest);

      if (typeof window === "undefined" || !("speechSynthesis" in window)) {
        return;
      }

      setSupported(true);
      speak(
        introText(latest),
        () => setSpeaking(true),
        () => setSpeaking(false),
      );
    });

    return () => {
      cancelled = true;
      if (typeof window !== "undefined" && "speechSynthesis" in window) {
        window.speechSynthesis.cancel();
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function handleToggle() {
    if (speaking) {
      window.speechSynthesis.cancel();
      setSpeaking(false);
      return;
    }

    speak(
      introText(tokenUsage),
      () => setSpeaking(true),
      () => setSpeaking(false),
    );
  }

  return (
    <div className={styles.voiceOver}>
      <p className={styles.voiceCaption}>{introText(tokenUsage)}</p>

      {supported && (
        <button
          type="button"
          className={styles.voiceButton}
          onClick={handleToggle}
          aria-pressed={speaking}
        >
          {speaking ? "⏹ Stop voice-over" : "🔊 Play voice-over"}
        </button>
      )}
    </div>
  );
}
