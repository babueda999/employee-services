import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import MessageContent from "@/app/agent/MessageContent";

describe("MessageContent", () => {
  it("renders a markdown table as an HTML table", () => {
    const content = [
      "| ID | Name | Department |",
      "|---:|------|------------|",
      "| 1 | John Doe | Engineering |",
      "| 2 | Jane Smith | Marketing |",
    ].join("\n");

    render(<MessageContent content={content} />);

    const table = screen.getByRole("table");
    expect(table).toBeInTheDocument();

    const rows = screen.getAllByRole("row");
    expect(rows).toHaveLength(3); // header + 2 data rows

    expect(screen.getByRole("columnheader", { name: "Name" })).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: "John Doe" })).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: "Jane Smith" })).toBeInTheDocument();
  });

  it("renders a bullet list with bold text", () => {
    const content = [
      "Employees in **Engineering**:",
      "",
      "- **1 - John Doe** (john.doe@example.com)",
      "- **4 - Emily Davis** (emily.davis@example.com)",
    ].join("\n");

    render(<MessageContent content={content} />);

    expect(screen.getByText("Engineering")).toBeInTheDocument();
    expect(screen.getByRole("list")).toBeInTheDocument();
    expect(screen.getAllByRole("listitem")).toHaveLength(2);
    expect(screen.getByText(/john\.doe@example\.com/)).toBeInTheDocument();
  });

  it("renders plain text with no table or list markup", () => {
    render(<MessageContent content="There are 10 employees." />);

    expect(screen.getByText("There are 10 employees.")).toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
    expect(screen.queryByRole("list")).not.toBeInTheDocument();
  });
});
