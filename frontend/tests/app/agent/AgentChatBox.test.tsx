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

async function sendMessage(user: ReturnType<typeof userEvent.setup>, message: string) {
  await user.type(
    screen.getByPlaceholderText("Ask the employee agent..."),
    message,
  );
  await user.click(screen.getByRole("button", { name: "Send" }));
}

describe("AgentChatBox", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  it("sends the message and renders the agent's reply", async () => {
    vi.mocked(fetch).mockResolvedValue(
      jsonResponse({
        reply: "There are 10 employees.",
        conversationId: "conv-1",
        confirmationRequired: false,
        confirmationToken: null,
      }),
    );
    const user = userEvent.setup();
    render(<AgentChatBox />);

    await sendMessage(user, "List all employees");

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

    await sendMessage(user, "List all employees");

    expect(
      await screen.findByText("An unexpected error occurred"),
    ).toBeInTheDocument();
  });

  it("does not submit an empty or whitespace-only message", async () => {
    const user = userEvent.setup();
    render(<AgentChatBox />);

    await sendMessage(user, "   ");

    expect(fetch).not.toHaveBeenCalled();
  });

  it("sends the conversationId from a prior reply on the next message", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse({
        reply: "Found Test Agentic.",
        conversationId: "conv-1",
        confirmationRequired: false,
        confirmationToken: null,
      }),
    );
    const user = userEvent.setup();
    render(<AgentChatBox />);

    await sendMessage(user, "Search for Agentic");
    expect(await screen.findByText("Found Test Agentic.")).toBeInTheDocument();

    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse({
        reply: "Updated their department.",
        conversationId: "conv-1",
        confirmationRequired: false,
        confirmationToken: null,
      }),
    );

    await sendMessage(user, "Update their department to Sales");

    expect(fetch).toHaveBeenLastCalledWith(
      "/api/agent",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({
          message: "Update their department to Sales",
          role: "USER",
          conversationId: "conv-1",
        }),
      }),
    );
  });

  it("shows a hint to switch to MANAGER while the current role can't approve", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse({
        reply: "Deleting employee 1 requires confirmation.",
        conversationId: "conv-1",
        confirmationRequired: true,
        confirmationToken: "token-abc",
      }),
    );
    const user = userEvent.setup();
    render(<AgentChatBox />);

    await sendMessage(user, "Delete employee 1");

    expect(
      await screen.findByText(/Switch "Acting as" to MANAGER/),
    ).toBeInTheDocument();
  });

  it("rejects the approval when the current role is not MANAGER", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse({
        reply: "Deleting employee 1 requires confirmation.",
        conversationId: "conv-1",
        confirmationRequired: true,
        confirmationToken: "token-abc",
      }),
    );
    const user = userEvent.setup();
    render(<AgentChatBox />);

    await user.selectOptions(screen.getByRole("combobox"), "ADMIN");
    await sendMessage(user, "Delete employee 1");
    const approveButton = await screen.findByRole("button", { name: "Approve" });

    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse(
        { message: "Only managers can approve or deny a pending confirmation." },
        403,
      ),
    );

    await user.click(approveButton);

    expect(
      await screen.findByText("Only managers can approve or deny a pending confirmation."),
    ).toBeInTheDocument();
    expect(fetch).toHaveBeenLastCalledWith(
      "/api/agent/confirm",
      expect.objectContaining({
        body: JSON.stringify({ confirmationToken: "token-abc", approve: true, role: "ADMIN" }),
      }),
    );
  });

  it("shows Approve/Deny buttons when the agent requests confirmation, and resolves on Approve by a MANAGER", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse({
        reply: "Deleting employee 1 requires confirmation.",
        conversationId: "conv-1",
        confirmationRequired: true,
        confirmationToken: "token-abc",
      }),
    );
    const user = userEvent.setup();
    render(<AgentChatBox />);

    await sendMessage(user, "Delete employee 1");

    expect(
      await screen.findByText("Deleting employee 1 requires confirmation."),
    ).toBeInTheDocument();
    const approveButton = screen.getByRole("button", { name: "Approve" });
    expect(approveButton).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Deny" })).toBeInTheDocument();

    await user.selectOptions(screen.getByRole("combobox"), "MANAGER");

    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse({
        reply: "Employee deleted",
        conversationId: "conv-1",
        confirmationRequired: false,
        confirmationToken: null,
      }),
    );

    await user.click(approveButton);

    expect(await screen.findByText("Employee deleted")).toBeInTheDocument();
    expect(screen.getByText("✓ Approved")).toBeInTheDocument();
    expect(fetch).toHaveBeenLastCalledWith(
      "/api/agent/confirm",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ confirmationToken: "token-abc", approve: true, role: "MANAGER" }),
      }),
    );
  });

  it("shows which sub-agent handled the reply as a badge", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse({
        reply: "There are 10 employees.",
        conversationId: "conv-1",
        confirmationRequired: false,
        confirmationToken: null,
        toolsUsed: ["list_employees"],
      }),
    );
    const user = userEvent.setup();
    render(<AgentChatBox />);

    await sendMessage(user, "List all employees");

    expect(await screen.findByText("List Employees Agent")).toBeInTheDocument();
  });

  it("shows a badge per sub-agent when a turn used more than one", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse({
        reply: "Updated Jane's department to Sales.",
        conversationId: "conv-1",
        confirmationRequired: false,
        confirmationToken: null,
        toolsUsed: ["search_employees", "update_employee"],
      }),
    );
    const user = userEvent.setup();
    render(<AgentChatBox />);

    await sendMessage(user, "Find Jane and update her department to Sales");

    expect(await screen.findByText("Search Employee Agent")).toBeInTheDocument();
    expect(screen.getByText("Update Employee Agent")).toBeInTheDocument();
  });

  it("shows no agent badge when the model answers without calling a tool", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse({
        reply: "I can help with employee lookups, updates, and more.",
        conversationId: "conv-1",
        confirmationRequired: false,
        confirmationToken: null,
        toolsUsed: [],
      }),
    );
    const user = userEvent.setup();
    render(<AgentChatBox />);

    await sendMessage(user, "What can you do?");

    expect(
      await screen.findByText("I can help with employee lookups, updates, and more."),
    ).toBeInTheDocument();
    expect(screen.queryByText(/Agent$/)).not.toBeInTheDocument();
  });

  it("resolves as denied on Deny without calling the agent again for that action", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse({
        reply: "Deleting employee 1 requires confirmation.",
        conversationId: "conv-1",
        confirmationRequired: true,
        confirmationToken: "token-abc",
      }),
    );
    const user = userEvent.setup();
    render(<AgentChatBox />);

    await sendMessage(user, "Delete employee 1");
    const denyButton = await screen.findByRole("button", { name: "Deny" });

    await user.selectOptions(screen.getByRole("combobox"), "MANAGER");

    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse({
        reply: "Okay, I won't go ahead with that.",
        conversationId: "conv-1",
        confirmationRequired: false,
        confirmationToken: null,
      }),
    );

    await user.click(denyButton);

    expect(
      await screen.findByText("Okay, I won't go ahead with that."),
    ).toBeInTheDocument();
    expect(screen.getByText("✗ Denied")).toBeInTheDocument();
    expect(fetch).toHaveBeenLastCalledWith(
      "/api/agent/confirm",
      expect.objectContaining({
        body: JSON.stringify({ confirmationToken: "token-abc", approve: false, role: "MANAGER" }),
      }),
    );
  });

  it("shows suggested prompt chips before the first message, and clicking one fills the input without sending", async () => {
    const user = userEvent.setup();
    render(<AgentChatBox />);

    const chip = screen.getByRole("button", { name: "List all employees" });
    await user.click(chip);

    expect(
      screen.getByPlaceholderText("Ask the employee agent..."),
    ).toHaveValue("List all employees");
    expect(fetch).not.toHaveBeenCalled();
  });

  it("hides the suggested prompts once a conversation has started", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse({
        reply: "There are 10 employees.",
        conversationId: "conv-1",
        confirmationRequired: false,
        confirmationToken: null,
      }),
    );
    const user = userEvent.setup();
    render(<AgentChatBox />);

    await sendMessage(user, "List all employees");

    expect(await screen.findByText("There are 10 employees.")).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Find employees in Engineering" }),
    ).not.toBeInTheDocument();
  });

  it("starts a new conversation, clearing messages and the conversation id", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse({
        reply: "There are 10 employees.",
        conversationId: "conv-1",
        confirmationRequired: false,
        confirmationToken: null,
      }),
    );
    const user = userEvent.setup();
    render(<AgentChatBox />);

    await sendMessage(user, "List all employees");
    expect(await screen.findByText("There are 10 employees.")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "New conversation" }));

    expect(screen.queryByText("There are 10 employees.")).not.toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "List all employees" }),
    ).toBeInTheDocument();

    vi.mocked(fetch).mockResolvedValueOnce(
      jsonResponse({
        reply: "Here are all employees.",
        conversationId: "conv-2",
        confirmationRequired: false,
        confirmationToken: null,
      }),
    );

    await sendMessage(user, "List all employees");

    expect(fetch).toHaveBeenLastCalledWith(
      "/api/agent",
      expect.objectContaining({
        body: JSON.stringify({ message: "List all employees", role: "USER" }),
      }),
    );
  });
});
