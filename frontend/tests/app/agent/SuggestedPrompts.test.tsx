import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import SuggestedPrompts, { SUGGESTED_PROMPTS } from "@/app/agent/SuggestedPrompts";

describe("SuggestedPrompts", () => {
  it("renders a button for each suggested prompt", () => {
    render(<SuggestedPrompts onSelect={vi.fn()} disabled={false} />);

    for (const prompt of SUGGESTED_PROMPTS) {
      expect(screen.getByRole("button", { name: prompt })).toBeInTheDocument();
    }
  });

  it("calls onSelect with the exact prompt text when clicked", async () => {
    const onSelect = vi.fn();
    const user = userEvent.setup();
    render(<SuggestedPrompts onSelect={onSelect} disabled={false} />);

    await user.click(screen.getByRole("button", { name: "List all employees" }));

    expect(onSelect).toHaveBeenCalledTimes(1);
    expect(onSelect).toHaveBeenCalledWith("List all employees");
  });

  it("disables every chip when disabled is true", () => {
    render(<SuggestedPrompts onSelect={vi.fn()} disabled={true} />);

    for (const prompt of SUGGESTED_PROMPTS) {
      expect(screen.getByRole("button", { name: prompt })).toBeDisabled();
    }
  });
});
