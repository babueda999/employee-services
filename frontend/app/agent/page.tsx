import Link from "next/link";
import AgentChatBox from "./AgentChatBox";
import styles from "./agent.module.css";

export default function AgentPage() {
  return (
    <main className={styles.main}>
      <Link href="/" className={styles.backLink}>
        &larr; Back home
      </Link>

      <h1 className={styles.brand}>Employee Agent</h1>

      <AgentChatBox />
    </main>
  );
}
