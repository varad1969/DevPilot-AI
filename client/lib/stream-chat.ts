import { getApiBaseUrl, ApiError, type ChatMessage } from "@/lib/api";

export type StreamChatHandlers = {
  onUserMessage?: (message: ChatMessage) => void;
  onToken?: (token: string) => void;
  onAssistantMessage?: (message: ChatMessage) => void;
  onDone?: () => void;
  onError?: (error: Error) => void;
  signal?: AbortSignal;
};

export async function streamChatMessage(
  sessionId: string,
  content: string,
  handlers: StreamChatHandlers = {}
): Promise<void> {
  const res = await fetch(
    `${getApiBaseUrl()}/api/chat/sessions/${sessionId}/messages`,
    {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ content }),
      signal: handlers.signal,
    }
  );

  if (!res.ok) {
    let message = res.statusText;
    try {
      const data = await res.json();
      message = data.message ?? data.error ?? message;
    } catch {
      // ignore
    }
    throw new ApiError(res.status, message);
  }

  if (!res.body) {
    throw new Error("No response body for SSE stream");
  }

  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  const processBlock = (block: string) => {
    if (!block.trim()) return;

    const lines = block.split(/\r?\n/);
    let event = "message";
    const dataLines: string[] = [];

    for (const line of lines) {
      if (line.startsWith("event:")) {
        event = line.slice(6).trim();
      } else if (line.startsWith("data:")) {
        // SSE spec: line is "data:" or "data: " + content
        const dataContent = line.startsWith("data: ")
          ? line.slice(6)
          : line.slice(5);
        dataLines.push(dataContent);
      }
    }

    const data = dataLines.join("\n");
    if (!data && event !== "done") return;

    try {
      if (event === "token") {
        let token = data;
        try {
          const parsed = JSON.parse(data);
          if (typeof parsed === "string") {
            token = parsed;
          }
        } catch {
          // If not valid JSON string, use the raw data directly
          token = data;
        }
        handlers.onToken?.(token);
      } else if (event === "user_message") {
        handlers.onUserMessage?.(JSON.parse(data) as ChatMessage);
      } else if (event === "assistant_message") {
        handlers.onAssistantMessage?.(JSON.parse(data) as ChatMessage);
      } else if (event === "done") {
        handlers.onDone?.();
      } else if (event === "error") {
        handlers.onError?.(new Error(data));
      }
    } catch (err) {
      handlers.onError?.(
        err instanceof Error ? err : new Error("Failed to parse SSE event")
      );
    }
  };

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    // Normalize \r\n to \n to reliably split double newlines
    buffer = buffer.replace(/\r\n/g, "\n");
    const parts = buffer.split("\n\n");
    buffer = parts.pop() ?? "";

    for (const part of parts) {
      processBlock(part);
    }
  }

  // Flush any remaining content in the buffer
  if (buffer.trim()) {
    processBlock(buffer);
  }

  handlers.onDone?.();
}
