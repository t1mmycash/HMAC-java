package ru.yandex.practicum.server;

import ru.yandex.practicum.config.AppConfig;
import ru.yandex.practicum.crypto.Codec;
import ru.yandex.practicum.model.VerifyRequest;
import ru.yandex.practicum.model.VerifyResponse;
import ru.yandex.practicum.util.SafeLogger;
import com.sun.net.httpserver.HttpExchange;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;

public class VerifyHandler extends BaseHandler {

    public VerifyHandler(AppConfig config, SafeLogger logger) {
        super(config, logger);
    }

    @Override
    protected void handlePost(HttpExchange exchange, String requestBody) throws IOException {
        VerifyRequest request;
        try {
            request = gson.fromJson(requestBody, VerifyRequest.class);
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
}
