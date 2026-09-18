import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import EmployeeForm from "@/app/employees/EmployeeForm";
import type { FormState } from "@/app/employees/actions";

describe("EmployeeForm", () => {
  it("renders empty required fields for create mode", () => {
    render(<EmployeeForm action={vi.fn()} submitLabel="Create" />);

    expect(screen.getByLabelText("First name")).toHaveValue("");
    expect(screen.getByLabelText("Last name")).toHaveValue("");
    expect(screen.getByLabelText("Email")).toHaveValue("");
    expect(screen.getByLabelText("Department")).toHaveValue("");
    expect(screen.getByRole("button", { name: "Create" })).toBeInTheDocument();
  });

  it("prefills fields from initialValues for edit mode", () => {
    render(
      <EmployeeForm
        action={vi.fn()}
        submitLabel="Save"
        initialValues={{
          id: 1,
          firstName: "Jane",
          lastName: "Doe",
          email: "jane.doe@example.com",
          department: "Sales",
          salary: 60000,
        }}
      />,
    );

    expect(screen.getByLabelText("First name")).toHaveValue("Jane");
    expect(screen.getByLabelText("Last name")).toHaveValue("Doe");
    expect(screen.getByLabelText("Email")).toHaveValue("jane.doe@example.com");
    expect(screen.getByLabelText("Department")).toHaveValue("Sales");
    expect(screen.getByLabelText("Salary")).toHaveValue(60000);
  });

  it("submits the entered values as FormData to the action", async () => {
    const user = userEvent.setup();
    const action = vi.fn<
      (prevState: FormState, formData: FormData) => Promise<FormState>
    >(async () => ({ error: null }));

    render(<EmployeeForm action={action} submitLabel="Create" />);

    await user.type(screen.getByLabelText("First name"), "Jane");
    await user.type(screen.getByLabelText("Last name"), "Doe");
    await user.type(screen.getByLabelText("Email"), "jane.doe@example.com");
    await user.type(screen.getByLabelText("Department"), "Sales");
    await user.type(screen.getByLabelText("Salary"), "60000");
    await user.click(screen.getByRole("button", { name: "Create" }));

    expect(action).toHaveBeenCalledTimes(1);
    const [, formData] = action.mock.calls[0];
    expect(formData.get("firstName")).toBe("Jane");
    expect(formData.get("lastName")).toBe("Doe");
    expect(formData.get("email")).toBe("jane.doe@example.com");
    expect(formData.get("department")).toBe("Sales");
    expect(formData.get("salary")).toBe("60000");
  });

  it("shows the error returned by the action instead of navigating away", async () => {
    const user = userEvent.setup();
    const action = vi.fn<
      (prevState: FormState, formData: FormData) => Promise<FormState>
    >(async () => ({
      error: "Employee already exists with email: jane.doe@example.com",
    }));

    render(<EmployeeForm action={action} submitLabel="Create" />);

    await user.type(screen.getByLabelText("First name"), "Jane");
    await user.type(screen.getByLabelText("Last name"), "Doe");
    await user.type(screen.getByLabelText("Email"), "jane.doe@example.com");
    await user.type(screen.getByLabelText("Department"), "Sales");
    await user.type(screen.getByLabelText("Salary"), "60000");
    await user.click(screen.getByRole("button", { name: "Create" }));

    expect(
      await screen.findByText(
        "Employee already exists with email: jane.doe@example.com",
      ),
    ).toBeInTheDocument();
  });
});
