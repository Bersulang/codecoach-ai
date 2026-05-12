export interface NdjsonStreamEvent<TPayload = unknown> {
  type: "start" | "stage" | "delta" | "done" | "error";
  message?: string;
  content?: string;
  payload?: TPayload;
}

export interface NdjsonStreamHandlers<TPayload = unknown> {
  onStart?: (event: NdjsonStreamEvent<TPayload>) => void;
  onStage?: (event: NdjsonStreamEvent<TPayload>) => void;
  onDelta?: (content: string, event: NdjsonStreamEvent<TPayload>) => void;
  onDone?: (payload: TPayload, event: NdjsonStreamEvent<TPayload>) => void;
  onError?: (event: NdjsonStreamEvent<TPayload>) => void;
}

export async function readNdjsonStream<TPayload>(
  response: Response,
  handlers: NdjsonStreamHandlers<TPayload>,
) {
  if (!response.body) {
    throw new Error("Stream body is empty");
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder("utf-8");
  let buffer = "";

  while (true) {
    const { done, value } = await reader.read();
    if (done) {
      break;
    }

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split("\n");
    buffer = lines.pop() ?? "";

    for (const line of lines) {
      handleLine(line, handlers);
    }
  }

  buffer += decoder.decode();
  if (buffer.trim()) {
    handleLine(buffer, handlers);
  }
}

function handleLine<TPayload>(
  line: string,
  handlers: NdjsonStreamHandlers<TPayload>,
) {
  const trimmed = line.trim();
  if (!trimmed) {
    return;
  }

  let event: NdjsonStreamEvent<TPayload>;
  try {
    event = JSON.parse(trimmed) as NdjsonStreamEvent<TPayload>;
  } catch {
    handlers.onError?.({
      type: "error",
      message: "流式响应解析失败",
    });
    return;
  }

  if (event.type === "start") {
    handlers.onStart?.(event);
    return;
  }
  if (event.type === "stage") {
    handlers.onStage?.(event);
    return;
  }
  if (event.type === "delta") {
    handlers.onDelta?.(event.content ?? "", event);
    return;
  }
  if (event.type === "done") {
    handlers.onDone?.(event.payload as TPayload, event);
    return;
  }
  if (event.type === "error") {
    handlers.onError?.(event);
  }
}
