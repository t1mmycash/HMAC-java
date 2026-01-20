package ru.yandex.practicum.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SafeLogger {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void info(String message) {
        log("INFO", message);
    }

    public void error(String message) {
        log("ERROR", message);
    }

    public void warn(String message) {
        log("WARN", message);
    }

    private void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        System.out.printf("[%s] %s: %s%n", timestamp, level, message);
    }

    public void logOperation(String endpoint, int statusCode, int msgLength) {
        info(String.format("%s - %d - msg_length=%d", endpoint, statusCode, msgLength));
    }

    public void logVerify(String endpoint, int statusCode, int msgLength, boolean result) {
        info(String.format("%s - %d - msg_length=%d - result=%s",
                endpoint, statusCode, msgLength, result));
    }
}
