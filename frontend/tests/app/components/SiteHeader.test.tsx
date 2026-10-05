import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import SiteHeader from "@/app/components/SiteHeader";

const { usePathnameMock } = vi.hoisted(() => ({
  usePathnameMock: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  usePathname: usePathnameMock,
}));

describe("SiteHeader", () => {
  it("renders links to Home, Employees, and Assistant", () => {
    usePathnameMock.mockReturnValue("/");
    render(<SiteHeader />);

    expect(screen.getByRole("link", { name: "Home" })).toHaveAttribute("href", "/");
    expect(screen.getByRole("link", { name: "Employees" })).toHaveAttribute(
      "href",
      "/employees",
    );
    expect(screen.getByRole("link", { name: "Assistant" })).toHaveAttribute(
      "href",
      "/agent",
    );
  });

  it("marks Employees as the current page for a nested employees route", () => {
    usePathnameMock.mockReturnValue("/employees/3/edit");
    render(<SiteHeader />);

    expect(screen.getByRole("link", { name: "Employees" })).toHaveAttribute(
      "aria-current",
      "page",
    );
    expect(screen.getByRole("link", { name: "Home" })).not.toHaveAttribute(
      "aria-current",
    );
    expect(screen.getByRole("link", { name: "Assistant" })).not.toHaveAttribute(
      "aria-current",
    );
  });

  it("marks Home as the current page only on the exact root path", () => {
    usePathnameMock.mockReturnValue("/");
    render(<SiteHeader />);

    expect(screen.getByRole("link", { name: "Home" })).toHaveAttribute(
      "aria-current",
      "page",
    );
  });

  it("marks Assistant as the current page on the agent route", () => {
    usePathnameMock.mockReturnValue("/agent");
    render(<SiteHeader />);

    expect(screen.getByRole("link", { name: "Assistant" })).toHaveAttribute(
      "aria-current",
      "page",
    );
  });
});
