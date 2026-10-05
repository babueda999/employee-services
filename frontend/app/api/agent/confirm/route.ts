import { NextRequest, NextResponse } from "next/server";
import { ApiError, confirmAgentAction } from "@/lib/agent";
import type { AgentRole } from "@/types/agent";

const VALID_ROLES: AgentRole[] = ["USER", "MANAGER", "ADMIN"];

function isAgentRole(value: unknown): value is AgentRole {
  return typeof value === "string" && (VALID_ROLES as string[]).includes(value);
}

export async function POST(request: NextRequest) {
  let confirmationToken: unknown;
  let approve: unknown;
  let role: unknown;

  try {
    const body = (await request.json()) as {
      confirmationToken?: unknown;
      approve?: unknown;
      role?: unknown;
    };
    confirmationToken = body.confirmationToken;
    approve = body.approve;
    role = body.role;
  } catch {
    return NextResponse.json(
      { message: "Request body must be valid JSON." },
      { status: 400 },
    );
  }

  if (typeof confirmationToken !== "string" || confirmationToken.trim() === "") {
    return NextResponse.json(
      { message: "confirmationToken is required." },
      { status: 400 },
    );
  }

  if (typeof approve !== "boolean") {
    return NextResponse.json(
      { message: "approve is required." },
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
    const agentResponse = await confirmAgentAction(confirmationToken, approve, role);
    return NextResponse.json(agentResponse);
  } catch (error) {
    const status = error instanceof ApiError ? error.status : 502;
    const errorMessage =
      error instanceof ApiError
        ? error.message
        : "Unable to reach the employee agent.";

    return NextResponse.json({ message: errorMessage }, { status });
  }
}
