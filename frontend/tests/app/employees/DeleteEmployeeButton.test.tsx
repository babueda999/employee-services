import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

const deleteEmployeeActionMock = vi.fn();
vi.mock("@/app/employees/actions", () => ({
  deleteEmployeeAction: (...args: unknown[]) =>
    deleteEmployeeActionMock(...args),
}));

import DeleteEmployeeButton from "@/app/employees/DeleteEmployeeButton";

describe("DeleteEmployeeButton", () => {
  beforeEach(() => {
    deleteEmployeeActionMock.mockReset();
  });

  it("shows only a Delete button initially", () => {
    render(<DeleteEmployeeButton id={1} name="Jane Doe" />);

    expect(screen.getByRole("button", { name: "Delete" })).toBeInTheDocument();
    expect(screen.queryByText("Delete Jane Doe?")).not.toBeInTheDocument();
  });

  it("shows an inline confirm prompt after clicking Delete", async () => {
    const user = userEvent.setup();
    render(<DeleteEmployeeButton id={1} name="Jane Doe" />);

    await user.click(screen.getByRole("button", { name: "Delete" }));

    expect(screen.getByText("Delete Jane Doe?")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Confirm" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Cancel" })).toBeInTheDocument();
  });

  it("dismisses the confirm prompt on Cancel without calling the action", async () => {
    const user = userEvent.setup();
    render(<DeleteEmployeeButton id={1} name="Jane Doe" />);

    await user.click(screen.getByRole("button", { name: "Delete" }));
    await user.click(screen.getByRole("button", { name: "Cancel" }));

    expect(screen.queryByText("Delete Jane Doe?")).not.toBeInTheDocument();
    expect(deleteEmployeeActionMock).not.toHaveBeenCalled();
  });

  it("calls deleteEmployeeAction bound to the employee id on Confirm", async () => {
    deleteEmployeeActionMock.mockResolvedValue({ error: null });
    const user = userEvent.setup();
    render(<DeleteEmployeeButton id={7} name="Jane Doe" />);

    await user.click(screen.getByRole("button", { name: "Delete" }));
    await user.click(screen.getByRole("button", { name: "Confirm" }));

    expect(deleteEmployeeActionMock).toHaveBeenCalledTimes(1);
    const [id] = deleteEmployeeActionMock.mock.calls[0];
    expect(id).toBe("7");
  });

  it("shows the returned error message when deletion fails", async () => {
    deleteEmployeeActionMock.mockResolvedValue({
      error: "Employee not found with id: 7",
    });
    const user = userEvent.setup();
    render(<DeleteEmployeeButton id={7} name="Jane Doe" />);

    await user.click(screen.getByRole("button", { name: "Delete" }));
    await user.click(screen.getByRole("button", { name: "Confirm" }));

    expect(
      await screen.findByText("Employee not found with id: 7"),
    ).toBeInTheDocument();
  });
});
