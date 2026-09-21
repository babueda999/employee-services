import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiError,
  createEmployee,
  deleteEmployee,
  getAllEmployees,
  getEmployeeById,
  updateEmployee,
} from "@/lib/employees";

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

const sampleEmployee = {
  id: 1,
  firstName: "John",
  lastName: "Doe",
  email: "john.doe@example.com",
  department: "Engineering",
  salary: 75000,
};

const sampleErrorBody = {
  timestamp: "2026-01-01T00:00:00",
  status: 404,
  error: "Employee Not Found",
  message: "Employee not found with id: 1",
  path: "/api/employees/1",
};

describe("lib/employees", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  describe("getAllEmployees", () => {
    it("returns the parsed list on success", async () => {
      vi.mocked(fetch).mockResolvedValue(jsonResponse([sampleEmployee]));

      const result = await getAllEmployees();

      expect(result).toEqual([sampleEmployee]);
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining("/api/employees"),
        expect.objectContaining({ cache: "no-store" }),
      );
    });

    it("throws ApiError with the backend error body on failure", async () => {
      vi.mocked(fetch).mockResolvedValue(jsonResponse(sampleErrorBody, 500));

      await expect(getAllEmployees()).rejects.toMatchObject({
        status: 500,
        message: sampleErrorBody.message,
      });
    });
  });

  describe("getEmployeeById", () => {
    it("returns the employee on success", async () => {
      vi.mocked(fetch).mockResolvedValue(jsonResponse(sampleEmployee));

      const result = await getEmployeeById("1");

      expect(result).toEqual(sampleEmployee);
    });

    it("throws ApiError with status 404 when not found", async () => {
      vi.mocked(fetch).mockResolvedValue(jsonResponse(sampleErrorBody, 404));

      const error = await getEmployeeById("1").catch((e) => e);

      expect(error).toBeInstanceOf(ApiError);
      expect(error.status).toBe(404);
      expect(error.message).toBe(sampleErrorBody.message);
    });

    it("falls back to a generic message when the error body is not JSON", async () => {
      vi.mocked(fetch).mockResolvedValue(
        new Response("not json", { status: 500 }),
      );

      const error = await getEmployeeById("1").catch((e) => e);

      expect(error).toBeInstanceOf(ApiError);
      expect(error.status).toBe(500);
      expect(error.message).toBe("Request failed with status 500");
    });
  });

  describe("createEmployee", () => {
    it("POSTs the request body and returns the created employee", async () => {
      vi.mocked(fetch).mockResolvedValue(jsonResponse(sampleEmployee, 201));

      const result = await createEmployee({
        firstName: "John",
        lastName: "Doe",
        email: "john.doe@example.com",
        department: "Engineering",
        salary: 75000,
      });

      expect(result).toEqual(sampleEmployee);
      const [, init] = vi.mocked(fetch).mock.calls[0];
      expect(init).toMatchObject({ method: "POST" });
      expect(JSON.parse(init!.body as string)).toMatchObject({
        email: "john.doe@example.com",
      });
    });

    it("throws ApiError on duplicate email (409)", async () => {
      vi.mocked(fetch).mockResolvedValue(
        jsonResponse(
          {
            ...sampleErrorBody,
            status: 409,
            error: "Duplicate Employee",
            message: "Employee already exists with email: john.doe@example.com",
          },
          409,
        ),
      );

      const error = await createEmployee({
        firstName: "John",
        lastName: "Doe",
        email: "john.doe@example.com",
        department: "Engineering",
        salary: 75000,
      }).catch((e) => e);

      expect(error).toBeInstanceOf(ApiError);
      expect(error.status).toBe(409);
    });
  });

  describe("updateEmployee", () => {
    it("PUTs to the employee's URL and returns the updated employee", async () => {
      vi.mocked(fetch).mockResolvedValue(
        jsonResponse({ ...sampleEmployee, department: "Sales" }),
      );

      const result = await updateEmployee("1", {
        firstName: "John",
        lastName: "Doe",
        email: "john.doe@example.com",
        department: "Sales",
        salary: 75000,
      });

      expect(result.department).toBe("Sales");
      const [url, init] = vi.mocked(fetch).mock.calls[0];
      expect(url).toContain("/api/employees/1");
      expect(init).toMatchObject({ method: "PUT" });
    });
  });

  describe("deleteEmployee", () => {
    it("DELETEs the employee and resolves with no value on success", async () => {
      vi.mocked(fetch).mockResolvedValue(new Response(null, { status: 204 }));

      await expect(deleteEmployee("1")).resolves.toBeUndefined();
      const [url, init] = vi.mocked(fetch).mock.calls[0];
      expect(url).toContain("/api/employees/1");
      expect(init).toMatchObject({ method: "DELETE" });
    });

    it("throws ApiError when the employee does not exist", async () => {
      vi.mocked(fetch).mockResolvedValue(jsonResponse(sampleErrorBody, 404));

      await expect(deleteEmployee("1")).rejects.toBeInstanceOf(ApiError);
    });
  });
});
