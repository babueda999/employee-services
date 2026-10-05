import AgentChatBox from "./AgentChatBox";
import styles from "./agent.module.css";

export default function AgentPage() {
  return (
    <main className={styles.main}>
      <h1 className={styles.title}>Employee Agent</h1>
      <p className={styles.subtitle}>
        Ask questions, update records, or request a change — the assistant picks
        the right tool and asks for approval on sensitive actions.
      </p>

      <AgentChatBox />
    </main>
  );
}
