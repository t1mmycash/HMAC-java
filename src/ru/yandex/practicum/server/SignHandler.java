package ru.yandex.practicum.server;

import ru.yandex.practicum.config.AppConfig;
import ru.yandex.practicum.model.SignRequest;
import ru.yandex.practicum.model.SignResponse;
import ru.yandex.practicum.util.SafeLogger;
import com.sun.net.httpserver.HttpExchange;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;

public class SignHandler extends BaseHandler {

    public SignHandler(AppConfig config, SafeLogger logger) {
        super(config, logger);
    }

    @Override
    protected void handlePost(HttpExchange exchange, String requestBody) throws IOException {
        SignRequest request;
        try {
            request = gson.fromJson(requestBody, SignRequest.class);
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
}
