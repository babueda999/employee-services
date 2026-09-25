import type { ReactNode } from "react";
import styles from "./agent.module.css";

type MessageBlock =
  | { type: "table"; headers: string[]; rows: string[][] }
  | { type: "paragraph"; lines: string[] };

function isSeparatorRow(line: string): boolean {
  const trimmed = line.trim();
  return trimmed.includes("-") && /^[\s|:-]+$/.test(trimmed);
}

function splitRow(line: string): string[] {
  return line
    .trim()
    .replace(/^\|/, "")
    .replace(/\|$/, "")
    .split("|")
    .map((cell) => cell.trim());
}

function parseMessageBlocks(content: string): MessageBlock[] {
  const lines = content.split("\n");
  const blocks: MessageBlock[] = [];
  let i = 0;

  const startsTable = (index: number) =>
    lines[index]?.includes("|") &&
    lines[index + 1] !== undefined &&
    isSeparatorRow(lines[index + 1]);

  while (i < lines.length) {
    if (startsTable(i)) {
      const headers = splitRow(lines[i]);
      i += 2;

      const rows: string[][] = [];
      while (i < lines.length && lines[i].includes("|") && lines[i].trim() !== "") {
        rows.push(splitRow(lines[i]));
        i += 1;
      }

      blocks.push({ type: "table", headers, rows });
      continue;
    }

    const paragraphLines: string[] = [];
    while (i < lines.length && !startsTable(i)) {
      paragraphLines.push(lines[i]);
      i += 1;
    }
    blocks.push({ type: "paragraph", lines: paragraphLines });
  }

  return blocks;
}

function renderInline(text: string): ReactNode[] {
  return text
    .split(/(\*\*[^*]+\*\*)/g)
    .filter((part) => part !== "")
    .map((part, index) =>
      part.startsWith("**") && part.endsWith("**") ? (
        <strong key={index}>{part.slice(2, -2)}</strong>
      ) : (
        <span key={index}>{part}</span>
      ),
    );
}

function ParagraphBlock({ lines }: { lines: string[] }) {
  const nodes: ReactNode[] = [];
  let bulletBuffer: string[] = [];

  const flushBullets = () => {
    if (bulletBuffer.length > 0) {
      nodes.push(
        <ul key={`ul-${nodes.length}`} className={styles.list}>
          {bulletBuffer.map((item, index) => (
            <li key={index}>{renderInline(item)}</li>
          ))}
        </ul>,
      );
      bulletBuffer = [];
    }
  };

  lines.forEach((line, index) => {
    const trimmed = line.trim();

    if (trimmed === "") {
      flushBullets();
      return;
    }

    const bulletMatch = trimmed.match(/^[-*]\s+(.*)$/);
    if (bulletMatch) {
      bulletBuffer.push(bulletMatch[1]);
      return;
    }

    flushBullets();
    nodes.push(
      <p key={`p-${index}`} className={styles.paragraphLine}>
        {renderInline(trimmed)}
      </p>,
    );
  });

  flushBullets();
  return <>{nodes}</>;
}

export default function MessageContent({ content }: { content: string }) {
  const blocks = parseMessageBlocks(content);

  return (
    <>
      {blocks.map((block, index) =>
        block.type === "table" ? (
          <div key={index} className={styles.tableWrap}>
            <table className={styles.table}>
              <thead>
                <tr>
                  {block.headers.map((header, headerIndex) => (
                    <th key={headerIndex}>{renderInline(header)}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {block.rows.map((row, rowIndex) => (
                  <tr key={rowIndex}>
                    {row.map((cell, cellIndex) => (
                      <td key={cellIndex}>{renderInline(cell)}</td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <ParagraphBlock key={index} lines={block.lines} />
        ),
      )}
    </>
  );
}
