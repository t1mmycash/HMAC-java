package ru.yandex.practicum.server;

import ru.yandex.practicum.config.AppConfig;
import ru.yandex.practicum.crypto.Codec;
import ru.yandex.practicum.crypto.HmacService;
import ru.yandex.practicum.model.*;
import ru.yandex.practicum.util.SafeLogger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class RequestHandler implements HttpHandler {
    private final AppConfig config;
    private final HmacService hmacService;
    private final SafeLogger logger;
    private final Gson gson = new Gson();

    public RequestHandler(AppConfig config, SafeLogger logger) {
        this.config = config;
        this.logger = logger;
        this.hmacService = new HmacService(config.getSecretBytes());
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            switch (path) {
                case "/sign":
                    if ("POST".equals(method)) {
                        handleSign(exchange);
                    } else {
                        sendError(exchange, 405, "method_not_allowed",
                                "Only POST method is allowed for /sign");
                    }
                    break;

                case "/verify":
                    if ("POST".equals(method)) {
                        handleVerify(exchange);
                    } else {
                        sendError(exchange, 405, "method_not_allowed",
                                "Only POST method is allowed for /verify");
                    }
                    break;

                default:
                    sendError(exchange, 404, "not_found",
                            "Endpoint not found. Available: POST /sign, POST /verify");
                    break;
            }
        } catch (Exception e) {
            logger.error("Unhandled exception in request handler: " + e.getMessage());
            sendError(exchange, 500, "internal_error", "Internal server error");
        }
    }

    private void handleSign(HttpExchange exchange) throws IOException {
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

        SignRequest request;
        try {
            request = gson.fromJson(body, SignRequest.class);
        } catch (JsonSyntaxException e) {
            sendError(exchange, 400, "invalid_json", "Invalid JSON format");
            return;
        }

        if (request.getMsg() == null) {
            sendError(exchange, 400, "invalid_msg", "Missing 'msg' field");
            return;
        }

        if (request.getMsg().isEmpty()) {
            sendError(exchange, 400, "invalid_msg", "Message cannot be empty");
            return;
        }

        if (request.getMsg().length() > config.getMaxMsgSizeBytes()) {
            sendError(exchange, 413, "message_too_large",
                    "Message length (" + request.getMsg().length() +
                            ") exceeds maximum size of " + config.getMaxMsgSizeBytes() + " bytes");
            return;
        }

        String signature = hmacService.sign(request.getMsg());
        SignResponse response = new SignResponse(signature);

        sendResponse(exchange, 200, gson.toJson(response));

        logger.logOperation("POST /sign", 200, request.getMsg().length());
    }

    private void handleVerify(HttpExchange exchange) throws IOException {
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

        VerifyRequest request;
        try {
            request = gson.fromJson(body, VerifyRequest.class);
        } catch (JsonSyntaxException e) {
            sendError(exchange, 400, "invalid_json", "Invalid JSON format");
            return;
        }

        if (request.getMsg() == null) {
            sendError(exchange, 400, "invalid_msg", "Missing 'msg' field");
            return;
        }

        if (request.getMsg().isEmpty()) {
            sendError(exchange, 400, "invalid_msg", "Message cannot be empty");
            return;
        }

        if (request.getSignature() == null) {
            sendError(exchange, 400, "invalid_signature", "Missing 'signature' field");
            return;
        }

        if (request.getMsg().length() > config.getMaxMsgSizeBytes()) {
            sendError(exchange, 413, "message_too_large",
                    "Message length (" + request.getMsg().length() +
                            ") exceeds maximum size of " + config.getMaxMsgSizeBytes() + " bytes");
            return;
        }

        if (!Codec.isValidBase64Url(request.getSignature())) {
            sendError(exchange, 400, "invalid_signature_format",
                    "Signature must be valid base64url format");
            return;
        }

        if (!Codec.isValidHmacSignatureLength(request.getSignature())) {
            sendError(exchange, 400, "invalid_signature_length",
                    "Invalid signature length. HMAC-SHA256 must be 32 bytes after decoding");
            return;
        }

        boolean isValid = hmacService.verify(request.getMsg(), request.getSignature());
        VerifyResponse response = new VerifyResponse(isValid);

        sendResponse(exchange, 200, gson.toJson(response));

        logger.logVerify("POST /verify", 200, request.getMsg().length(), isValid);
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
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
                return null; // Ошибка уже отправлена
            }
            body.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
        }

        return body.toString();
    }

    private boolean isValidContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        return contentType.toLowerCase().contains("application/json");
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String responseBody)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
            os.flush();
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String errorCode,
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