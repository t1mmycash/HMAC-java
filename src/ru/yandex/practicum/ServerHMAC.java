package ru.yandex.practicum;

import ru.yandex.practicum.config.AppConfig;
import ru.yandex.practicum.config.ConfigLoader;
import ru.yandex.practicum.server.HttpServerStarter;
import ru.yandex.practicum.util.SafeLogger;

public class ServerHMAC {
    public static void main(String[] args) {
        try {
            AppConfig config = ConfigLoader.load("config.json");

            SafeLogger logger = new SafeLogger();
            logger.info("Starting HMAC Service on port " + config.getListenPort());

            HttpServerStarter server = new HttpServerStarter(config, logger);
            server.start();

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
