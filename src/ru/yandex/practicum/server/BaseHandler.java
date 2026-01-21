package ru.yandex.practicum.server;

import ru.yandex.practicum.config.AppConfig;
import ru.yandex.practicum.crypto.HmacService;
import ru.yandex.practicum.model.ErrorResponse;
import ru.yandex.practicum.util.SafeLogger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public abstract class BaseHandler implements HttpHandler {
    protected final AppConfig config;
    protected final HmacService hmacService;
    protected final SafeLogger logger;
    protected final Gson gson = new Gson();

    public BaseHandler(AppConfig config, SafeLogger logger) {
        this.config = config;
        this.logger = logger;
        this.hmacService = new HmacService(config);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "method_not_allowed",
                        "Only POST method is allowed");
                return;
            }

            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (!isValidContentType(contentType)) {
                sendError(exchange, 415, "unsupported_media_type",
                        "Content-Type must be application/json");
                return;
            }

            String body = readRequestBody(exchange);
            if (body == null) {
                return;
            }

            if (body.isEmpty()) {
                sendError(exchange, 400, "invalid_json", "Empty request body");
                return;
            }

            handlePost(exchange, body);

        } catch (Exception e) {
            logger.error("Unhandled exception: " + e.getMessage());
            sendError(exchange, 500, "internal_error", "Internal server error");
        }
    }

    protected abstract void handlePost(HttpExchange exchange, String requestBody)
            throws IOException;

    protected String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        byte[] buffer = new byte[Math.min(1024, config.getMaxMsgSizeBytes())];
        StringBuilder body = new StringBuilder();
        int bytesRead;
        int totalBytes = 0;

        while ((bytesRead = is.read(buffer)) != -1) {
            totalBytes += bytesRead;
            if (totalBytes > config.getMaxMsgSizeBytes()) {
                sendError(exchange, 413, "request_too_large",
                        "Request body size (" + totalBytes +
                                ") exceeds maximum size of " + config.getMaxMsgSizeBytes() + " bytes");
                return null;
            }
            body.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
        }

        return body.toString();
    }

    protected boolean isValidContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        return contentType.toLowerCase().contains("application/json");
    }

    protected void sendResponse(HttpExchange exchange, int statusCode, String responseBody)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
            os.flush();
        }
    }

    protected void sendError(HttpExchange exchange, int statusCode, String errorCode,
                             String message) throws IOException {
        logger.error("HTTP " + statusCode + " - " + errorCode + ": " + message);

        ErrorResponse error = new ErrorResponse(errorCode);
        String responseBody = gson.toJson(error);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
            os.flush();
        }
    }
}