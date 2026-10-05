import { NextResponse } from "next/server";
import { ApiError, getTokenUsage } from "@/lib/agent";

export async function GET() {
  try {
    const usage = await getTokenUsage();
    return NextResponse.json(usage);
  } catch (error) {
    const status = error instanceof ApiError ? error.status : 502;
    const errorMessage =
      error instanceof ApiError
        ? error.message
        : "Unable to reach the employee agent.";

    return NextResponse.json({ message: errorMessage }, { status });
  }
}
