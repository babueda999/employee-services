import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import AgentChatBox from "@/app/agent/AgentChatBox";

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("AgentChatBox", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  it("sends the message and renders the agent's reply", async () => {
    vi.mocked(fetch).mockResolvedValue(
      jsonResponse({ reply: "There are 10 employees." }),
    );
    const user = userEvent.setup();
    render(<AgentChatBox />);

    await user.type(
      screen.getByPlaceholderText("Ask the employee agent..."),
      "List all employees",
    );
    await user.click(screen.getByRole("button", { name: "Send" }));

    expect(await screen.findByText("There are 10 employees.")).toBeInTheDocument();
    expect(screen.getByText("List all employees")).toBeInTheDocument();
    expect(fetch).toHaveBeenCalledWith(
      "/api/agent",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ message: "List all employees", role: "USER" }),
      }),
    );
  });

  it("shows an error message when the agent request fails", async () => {
    vi.mocked(fetch).mockResolvedValue(
      jsonResponse({ message: "An unexpected error occurred" }, 500),
    );
    const user = userEvent.setup();
    render(<AgentChatBox />);

    await user.type(
      screen.getByPlaceholderText("Ask the employee agent..."),
      "List all employees",
    );
    await user.click(screen.getByRole("button", { name: "Send" }));

    expect(
      await screen.findByText("An unexpected error occurred"),
    ).toBeInTheDocument();
  });

  it("does not submit an empty or whitespace-only message", async () => {
    const user = userEvent.setup();
    render(<AgentChatBox />);

    await user.type(
      screen.getByPlaceholderText("Ask the employee agent..."),
      "   ",
    );
    await user.click(screen.getByRole("button", { name: "Send" }));

    expect(fetch).not.toHaveBeenCalled();
  });
});
