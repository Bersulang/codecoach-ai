package com.codecoach.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class NdjsonStreamWriter {

    private final OutputStream outputStream;

    private final ObjectMapper objectMapper;

    public NdjsonStreamWriter(OutputStream outputStream, ObjectMapper objectMapper) {
        this.outputStream = outputStream;
        this.objectMapper = objectMapper;
    }

    public void start(String message) throws IOException {
        write(Map.of("type", "start", "message", message));
    }

    public void stage(String message) throws IOException {
        write(Map.of("type", "stage", "message", message));
    }

    public void delta(String content) throws IOException {
        write(Map.of("type", "delta", "content", content == null ? "" : content));
    }

    public void done(Object payload) throws IOException {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "done");
        event.put("payload", payload);
        write(event);
    }

    public void error(String message) throws IOException {
        write(Map.of("type", "error", "message", message));
    }

    private void write(Map<String, Object> event) throws IOException {
        String line = objectMapper.writeValueAsString(event) + "\n";
        outputStream.write(line.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }
}
