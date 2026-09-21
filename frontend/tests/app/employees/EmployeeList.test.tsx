import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import type { Employee } from "@/types/employee";

vi.mock("@/app/employees/actions", () => ({
  deleteEmployeeAction: vi.fn(),
}));

import EmployeeList from "@/app/employees/EmployeeList";

const POLL_INTERVAL_MS = 5000;

const jane: Employee = {
  id: 1,
  firstName: "Jane",
  lastName: "Doe",
  email: "jane.doe@example.com",
  department: "Engineering",
  salary: 75000,
};

const john: Employee = {
  id: 2,
  firstName: "John",
  lastName: "Smith",
  email: "john.smith@example.com",
  department: "Sales",
  salary: 62000,
};

function jsonResponse(body: unknown, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  } as Response;
}

describe("EmployeeList", () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ["setInterval", "clearInterval"] });
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it("renders the employee count and rows from the initial props", () => {
    render(
      <EmployeeList initialEmployees={[jane]} initialError={null} />,
    );

    expect(screen.getByText("1 employee")).toBeInTheDocument();
    expect(screen.getByText("Jane Doe")).toBeInTheDocument();
    expect(fetch).not.toHaveBeenCalled();
  });

  it("updates the employee count and rows after polling picks up new records", async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse([jane, john]));

    render(
      <EmployeeList initialEmployees={[jane]} initialError={null} />,
    );

    expect(screen.getByText("1 employee")).toBeInTheDocument();

    await vi.advanceTimersByTimeAsync(POLL_INTERVAL_MS);

    expect(fetch).toHaveBeenCalledWith("/api/employees", { cache: "no-store" });
    expect(await screen.findByText("2 employees")).toBeInTheDocument();
    expect(screen.getByText("John Smith")).toBeInTheDocument();
  });

  it("keeps showing the last known employees alongside an error when a poll fails", async () => {
    vi.mocked(fetch).mockResolvedValue(
      jsonResponse({ message: "Unable to reach the employee service." }, 502),
    );

    render(
      <EmployeeList initialEmployees={[jane]} initialError={null} />,
    );

    await vi.advanceTimersByTimeAsync(POLL_INTERVAL_MS);

    expect(
      await screen.findByText("Unable to reach the employee service."),
    ).toBeInTheDocument();
    expect(screen.getByText("Jane Doe")).toBeInTheDocument();
    expect(screen.queryByText("1 employee")).not.toBeInTheDocument();
  });

  it("clears a prior error once a subsequent poll succeeds", async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(jsonResponse({ message: "boom" }, 500))
      .mockResolvedValueOnce(jsonResponse([jane, john]));

    render(
      <EmployeeList initialEmployees={[jane]} initialError={null} />,
    );

    await vi.advanceTimersByTimeAsync(POLL_INTERVAL_MS);
    expect(await screen.findByText("boom")).toBeInTheDocument();

    await vi.advanceTimersByTimeAsync(POLL_INTERVAL_MS);

    expect(await screen.findByText("2 employees")).toBeInTheDocument();
    expect(screen.queryByText("boom")).not.toBeInTheDocument();
  });
});
