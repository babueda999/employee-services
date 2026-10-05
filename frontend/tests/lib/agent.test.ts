import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError, askAgent, confirmAgentAction, getTokenUsage } from "@/lib/agent";

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

const sampleErrorBody = {
  timestamp: "2026-01-01T00:00:00",
  status: 500,
  error: "Internal Server Error",
  message: "An unexpected error occurred",
  path: "/api/agent",
};

describe("lib/agent", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  describe("askAgent", () => {
    it("returns the full agent response on success", async () => {
      vi.mocked(fetch).mockResolvedValue(
        jsonResponse({
          reply: "Employee 1 is John Doe, Engineering.",
          conversationId: "conv-1",
          confirmationRequired: false,
          confirmationToken: null,
          toolsUsed: ["get_employee"],
        }),
      );

      const response = await askAgent("Find employee 1");

      expect(response.reply).toBe("Employee 1 is John Doe, Engineering.");
      expect(response.conversationId).toBe("conv-1");
      expect(response.confirmationRequired).toBe(false);
      expect(response.toolsUsed).toEqual(["get_employee"]);
      expect(fetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/agent",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({ message: "Find employee 1" }),
        }),
      );
    });

    it("includes the role in the request body when provided", async () => {
      vi.mocked(fetch).mockResolvedValue(
        jsonResponse({
          reply: "Here are all employees.",
          conversationId: "conv-1",
          confirmationRequired: false,
          confirmationToken: null,
        }),
      );

      await askAgent("List all employees", "ADMIN");

      expect(fetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/agent",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({ message: "List all employees", role: "ADMIN" }),
        }),
      );
    });

    it("includes the conversationId in the request body when provided", async () => {
      vi.mocked(fetch).mockResolvedValue(
        jsonResponse({
          reply: "Updated their department.",
          conversationId: "conv-1",
          confirmationRequired: false,
          confirmationToken: null,
        }),
      );

      await askAgent("Update their department to Sales", "ADMIN", "conv-1");

      expect(fetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/agent",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({
            message: "Update their department to Sales",
            role: "ADMIN",
            conversationId: "conv-1",
          }),
        }),
      );
    });

    it("surfaces a pending confirmation in the response", async () => {
      vi.mocked(fetch).mockResolvedValue(
        jsonResponse({
          reply: "Deleting employee 1 requires confirmation.",
          conversationId: "conv-1",
          confirmationRequired: true,
          confirmationToken: "token-abc",
        }),
      );

      const response = await askAgent("Delete employee 1", "ADMIN");

      expect(response.confirmationRequired).toBe(true);
      expect(response.confirmationToken).toBe("token-abc");
    });

    it("throws ApiError with the backend message when the request fails", async () => {
      vi.mocked(fetch).mockResolvedValue(jsonResponse(sampleErrorBody, 500));

      await expect(askAgent("List all employees")).rejects.toMatchObject({
        status: 500,
        message: "An unexpected error occurred",
      });
      await expect(askAgent("List all employees")).rejects.toBeInstanceOf(
        ApiError,
      );
    });
  });

  describe("confirmAgentAction", () => {
    it("sends the token and approval, and returns the response", async () => {
      vi.mocked(fetch).mockResolvedValue(
        jsonResponse({
          reply: "Employee deleted",
          conversationId: "conv-1",
          confirmationRequired: false,
          confirmationToken: null,
        }),
      );

      const response = await confirmAgentAction("token-abc", true);

      expect(response.reply).toBe("Employee deleted");
      expect(fetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/agent/confirm",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({ confirmationToken: "token-abc", approve: true }),
        }),
      );
    });

    it("includes the approver's role in the request body when provided", async () => {
      vi.mocked(fetch).mockResolvedValue(
        jsonResponse({
          reply: "Employee deleted",
          conversationId: "conv-1",
          confirmationRequired: false,
          confirmationToken: null,
        }),
      );

      await confirmAgentAction("token-abc", true, "MANAGER");

      expect(fetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/agent/confirm",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({
            confirmationToken: "token-abc",
            approve: true,
            role: "MANAGER",
          }),
        }),
      );
    });

    it("throws ApiError when the approver role is not MANAGER", async () => {
      vi.mocked(fetch).mockResolvedValue(
        jsonResponse(
          { message: "Only managers can approve or deny a pending confirmation." },
          403,
        ),
      );

      await expect(confirmAgentAction("token-abc", true, "ADMIN")).rejects.toMatchObject({
        status: 403,
      });
    });

    it("throws ApiError when the token is unknown or expired", async () => {
      vi.mocked(fetch).mockResolvedValue(
        jsonResponse(
          {
            timestamp: "2026-01-01T00:00:00",
            status: 404,
            error: "Pending Confirmation Not Found",
            message: "No pending confirmation found for that token.",
            path: "/api/agent/confirm",
          },
          404,
        ),
      );

      await expect(confirmAgentAction("missing", true)).rejects.toMatchObject({
        status: 404,
      });
    });
  });

  describe("getTokenUsage", () => {
    it("returns the usage snapshot on success", async () => {
      vi.mocked(fetch).mockResolvedValue(
        jsonResponse({
          model: "gpt-5.2",
          requestCount: 3,
          inputTokens: 120,
          outputTokens: 45,
          totalTokens: 165,
        }),
      );

      const usage = await getTokenUsage();

      expect(usage).toEqual({
        model: "gpt-5.2",
        requestCount: 3,
        inputTokens: 120,
        outputTokens: 45,
        totalTokens: 165,
      });
      expect(fetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/agent/usage",
        expect.objectContaining({ cache: "no-store" }),
      );
    });

    it("throws ApiError when the backend is unreachable", async () => {
      vi.mocked(fetch).mockResolvedValue(jsonResponse(sampleErrorBody, 500));

      await expect(getTokenUsage()).rejects.toBeInstanceOf(ApiError);
    });
  });
});
