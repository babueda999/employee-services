import { NextRequest, NextResponse } from "next/server";
import { ApiError, askAgent } from "@/lib/agent";

export async function POST(request: NextRequest) {
  let message: unknown;

  try {
    const body = (await request.json()) as { message?: unknown };
    message = body.message;
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

  try {
    const reply = await askAgent(message);
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
