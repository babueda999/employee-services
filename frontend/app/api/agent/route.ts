import { NextRequest, NextResponse } from "next/server";
import { ApiError, askAgent } from "@/lib/agent";
import type { AgentRole } from "@/types/agent";

const VALID_ROLES: AgentRole[] = ["USER", "MANAGER", "ADMIN"];

function isAgentRole(value: unknown): value is AgentRole {
  return typeof value === "string" && (VALID_ROLES as string[]).includes(value);
}

export async function POST(request: NextRequest) {
  let message: unknown;
  let role: unknown;

  try {
    const body = (await request.json()) as { message?: unknown; role?: unknown };
    message = body.message;
    role = body.role;
  } catch {
    return NextResponse.json(
      { message: "Request body must be valid JSON." },
      { status: 400 },
    );
  }

  if (typeof message !== "string" || message.trim() === "") {
    return NextResponse.json(
      { message: "message is required." },
      { status: 400 },
    );
  }

  if (role !== undefined && !isAgentRole(role)) {
    return NextResponse.json(
      { message: "role must be one of USER, MANAGER, ADMIN." },
      { status: 400 },
    );
  }

  try {
    const reply = await askAgent(message, role);
    return NextResponse.json({ reply });
  } catch (error) {
    const status = error instanceof ApiError ? error.status : 502;
    const errorMessage =
      error instanceof ApiError
        ? error.message
        : "Unable to reach the employee agent.";

    return NextResponse.json({ message: errorMessage }, { status });
  }
}
