import { beforeEach, describe, expect, it, vi } from "vitest";

const { createEmployeeMock, updateEmployeeMock, deleteEmployeeMock } = vi.hoisted(() => ({
  createEmployeeMock: vi.fn(),
  updateEmployeeMock: vi.fn(),
  deleteEmployeeMock: vi.fn(),
}));

vi.mock("@/lib/employees", async () => {
  const actual = await vi.importActual<typeof import("@/lib/employees")>(
    "@/lib/employees",
  );
  return {
    ...actual,
    createEmployee: createEmployeeMock,
    updateEmployee: updateEmployeeMock,
    deleteEmployee: deleteEmployeeMock,
  };
});

const { redirectMock, revalidatePathMock } = vi.hoisted(() => ({
  redirectMock: vi.fn((url: string) => {
    throw new Error(`NEXT_REDIRECT:${url}`);
  }),
  revalidatePathMock: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  redirect: redirectMock,
}));

vi.mock("next/cache", () => ({
  revalidatePath: revalidatePathMock,
}));

import { ApiError } from "@/lib/employees";
import {
  createEmployeeAction,
  deleteEmployeeAction,
  updateEmployeeAction,
} from "@/app/employees/actions";

function formDataFor(fields: Record<string, string>) {
  const formData = new FormData();
  for (const [key, value] of Object.entries(fields)) {
    formData.set(key, value);
  }
  return formData;
}

const validFields = {
  firstName: "John",
  lastName: "Doe",
  email: "john.doe@example.com",
  department: "Engineering",
  salary: "75000",
};

describe("employees/actions", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("createEmployeeAction", () => {
    it("creates the employee, revalidates, and redirects to the new record", async () => {
      createEmployeeMock.mockResolvedValue({ id: 42 });

      await expect(
        createEmployeeAction({ error: null }, formDataFor(validFields)),
      ).rejects.toThrow("NEXT_REDIRECT:/employees/42");

      expect(createEmployeeMock).toHaveBeenCalledWith({
        firstName: "John",
        lastName: "Doe",
        email: "john.doe@example.com",
        department: "Engineering",
        salary: 75000,
      });
      expect(revalidatePathMock).toHaveBeenCalledWith("/employees");
      expect(redirectMock).toHaveBeenCalledWith("/employees/42");
    });

    it("returns the ApiError message instead of redirecting on failure", async () => {
      createEmployeeMock.mockRejectedValue(
        new ApiError(409, {
          timestamp: "now",
          status: 409,
          error: "Duplicate Employee",
          message: "Employee already exists with email: john.doe@example.com",
          path: "/api/employees",
        }),
      );

      const result = await createEmployeeAction(
        { error: null },
        formDataFor(validFields),
      );

      expect(result).toEqual({
        error: "Employee already exists with email: john.doe@example.com",
      });
      expect(redirectMock).not.toHaveBeenCalled();
    });

    it("returns a generic error message for a non-ApiError failure", async () => {
      createEmployeeMock.mockRejectedValue(new TypeError("network down"));

      const result = await createEmployeeAction(
        { error: null },
        formDataFor(validFields),
      );

      expect(result).toEqual({
        error: "Unable to reach the employee service.",
      });
    });
  });

  describe("updateEmployeeAction", () => {
    it("updates the employee, revalidates both routes, and redirects", async () => {
      updateEmployeeMock.mockResolvedValue({ id: 1 });

      await expect(
        updateEmployeeAction("1", { error: null }, formDataFor(validFields)),
      ).rejects.toThrow("NEXT_REDIRECT:/employees/1");

      expect(updateEmployeeMock).toHaveBeenCalledWith(
        "1",
        expect.objectContaining({ email: "john.doe@example.com" }),
      );
      expect(revalidatePathMock).toHaveBeenCalledWith("/employees");
      expect(revalidatePathMock).toHaveBeenCalledWith("/employees/1");
    });

    it("returns the ApiError message when the email belongs to another employee", async () => {
      updateEmployeeMock.mockRejectedValue(
        new ApiError(409, {
          timestamp: "now",
          status: 409,
          error: "Duplicate Employee",
          message: "Email already used by another employee: john.doe@example.com",
          path: "/api/employees/1",
        }),
      );

      const result = await updateEmployeeAction(
        "1",
        { error: null },
        formDataFor(validFields),
      );

      expect(result.error).toContain("already used by another employee");
    });
  });

  describe("deleteEmployeeAction", () => {
    it("deletes the employee, revalidates the list, and redirects to it", async () => {
      deleteEmployeeMock.mockResolvedValue(undefined);

      await expect(
        deleteEmployeeAction("1", { error: null }, new FormData()),
      ).rejects.toThrow("NEXT_REDIRECT:/employees");

      expect(deleteEmployeeMock).toHaveBeenCalledWith("1");
      expect(revalidatePathMock).toHaveBeenCalledWith("/employees");
      expect(redirectMock).toHaveBeenCalledWith("/employees");
    });

    it("returns the ApiError message when the employee no longer exists", async () => {
      deleteEmployeeMock.mockRejectedValue(
        new ApiError(404, {
          timestamp: "now",
          status: 404,
          error: "Employee Not Found",
          message: "Employee not found with id: 1",
          path: "/api/employees/1",
        }),
      );

      const result = await deleteEmployeeAction(
        "1",
        { error: null },
        new FormData(),
      );

      expect(result).toEqual({ error: "Employee not found with id: 1" });
      expect(redirectMock).not.toHaveBeenCalled();
    });
  });
});
