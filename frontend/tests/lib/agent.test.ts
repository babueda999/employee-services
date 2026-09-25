import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError, askAgent } from "@/lib/agent";

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
    it("returns the reply on success", async () => {
      vi.mocked(fetch).mockResolvedValue(
        jsonResponse({ reply: "Employee 1 is John Doe, Engineering." }),
      );

      const reply = await askAgent("Find employee 1");

      expect(reply).toBe("Employee 1 is John Doe, Engineering.");
      expect(fetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/agent",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({ message: "Find employee 1" }),
        }),
      );
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
});
