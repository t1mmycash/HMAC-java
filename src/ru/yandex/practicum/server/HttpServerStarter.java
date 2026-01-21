package ru.yandex.practicum.server;

import ru.yandex.practicum.config.AppConfig;
import ru.yandex.practicum.util.SafeLogger;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.io.IOException;

public class HttpServerStarter {
    private final AppConfig config;
    private final SafeLogger logger;
    private HttpServer server;

    public HttpServerStarter(AppConfig config, SafeLogger logger) {
        this.config = config;
        this.logger = logger;
    }

    public void start() throws IOException {
        try {
            server = HttpServer.create(
                    new InetSocketAddress(config.getListenPort()),
                    0
            );

            SignHandler signHandler = new SignHandler(config, logger);
            VerifyHandler verifyHandler = new VerifyHandler(config, logger);

            server.createContext("/sign", signHandler);
            server.createContext("/verify", verifyHandler);

            server.setExecutor(null);
            server.start();

            logger.info("✅ HMAC Service started on http://localhost:" + config.getListenPort());
            logger.info("Available endpoints:");
            logger.info("  POST http://localhost:" + config.getListenPort() + "/sign");
            logger.info("  POST http://localhost:" + config.getListenPort() + "/verify");
            logger.info("Max message size: " + config.getMaxMsgSizeBytes() + " bytes");

        } catch (IOException e) {
            logger.error("❌ Failed to start server on port " + config.getListenPort() + ": " + e.getMessage());
            throw e;
        }
    }

    public void stop() {
        if (server != null) {
            logger.info("Stopping server...");
            server.stop(0);
            logger.info("Server stopped");
        }
    }
}
