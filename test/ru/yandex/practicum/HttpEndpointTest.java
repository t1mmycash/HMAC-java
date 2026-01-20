package ru.yandex.practicum;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.*;
import ru.yandex.practicum.config.AppConfig;
import ru.yandex.practicum.config.ConfigLoader;
import ru.yandex.practicum.crypto.Codec;
import ru.yandex.practicum.crypto.HmacService;
import ru.yandex.practicum.server.HttpServerStarter;
import ru.yandex.practicum.util.SafeLogger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class HttpEndpointTest {

    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLTEyMzQ1";
    private static final int TEST_PORT = 18080; // Используем другой порт
    private static final int MAX_MSG_SIZE = 1024; // 1KB для тестов

    private static HttpServerStarter server;
    private static Thread serverThread;
    private static final AtomicBoolean serverStarted = new AtomicBoolean(false);
    private static final Gson gson = new Gson();
    private static final String baseUrl = "http://localhost:" + TEST_PORT;

    @BeforeAll
    static void setUpAll() throws Exception {
        System.out.println("🔄 Настройка тестового окружения...");

        Files.deleteIfExists(Path.of("test_config.json"));

        String configJson = String.format(
                "{\"hmacAlg\":\"SHA256\",\"secret\":\"%s\",\"listenPort\":%d,\"maxMsgSizeBytes\":%d}",
                TEST_SECRET, TEST_PORT, MAX_MSG_SIZE
        );

        Files.writeString(Path.of("test_config.json"), configJson);
        System.out.println("📄 Создан test_config.json");

        AppConfig config = ConfigLoader.load("test_config.json");

        SafeLogger logger = new SafeLogger() {
            @Override
            public void info(String message) {
            }

            @Override
            public void error(String message) {
                System.err.println("[TEST SERVER ERROR] " + message);
            }
        };

        server = new HttpServerStarter(config, logger);

        serverThread = new Thread(() -> {
            try {
                System.out.println("🚀 Запуск тестового сервера на порту " + TEST_PORT + "...");
                server.start();
                serverStarted.set(true);
                System.out.println("✅ Тестовый сервер запущен");

                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Ошибка запуска тестового сервера: " + e.getMessage());
                e.printStackTrace();
            }
        });

        serverThread.setDaemon(true);
        serverThread.start();

        System.out.println("⏳ Ожидание запуска сервера...");
        int maxWaitTime = 10000; // 10 секунд
        int waitInterval = 100; // 100ms
        int totalWaited = 0;

        while (totalWaited < maxWaitTime) {
            if (isServerRunning(TEST_PORT)) {
                serverStarted.set(true);
                System.out.println("✅ Сервер запущен и отвечает на порту " + TEST_PORT);
                break;
            }
            Thread.sleep(waitInterval);
            totalWaited += waitInterval;
        }

        if (!serverStarted.get()) {
            throw new RuntimeException("Не удалось запустить тестовый сервер за " + (maxWaitTime / 1000) + " секунд");
        }

        Thread.sleep(500);
    }

    @AfterAll
    static void tearDownAll() throws Exception {
        System.out.println("🛑 Завершение тестового окружения...");

        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                System.err.println("Ошибка при остановке сервера: " + e.getMessage());
            }
        }

        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
            try {
                serverThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        try {
            Files.deleteIfExists(Path.of("test_config.json"));
            System.out.println("🗑️  test_config.json удален");
        } catch (IOException e) {
            System.err.println("Не удалось удалить test_config.json: " + e.getMessage());
        }

        System.out.println("✅ Тестовое окружение завершено");
    }

    private static boolean isServerRunning(int port) {
        try {
            URL url = new URL("http://localhost:" + port + "/sign");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(500);
            conn.setReadTimeout(500);
            int responseCode = conn.getResponseCode();
            return responseCode > 0;
        } catch (IOException e) {
            return false;
        }
    }

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(serverStarted.get(), "Сервер не запущен, пропускаем тест");
    }

    private String sendPostRequest(String endpoint, String jsonBody) throws IOException {
        URL url = new URL(baseUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        return response.toString();
    }

    private int getResponseCode(String endpoint, String jsonBody) throws IOException {
        URL url = new URL(baseUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        return conn.getResponseCode();
    }

    @Test
    @DisplayName("Тест 1: Подпись/проверка успеха - msg='hello'")
    void testSignAndVerifySuccess() throws IOException {
        System.out.println("🧪 Тест 1: Подпись/проверка успеха");

        String signRequest = "{\"msg\":\"hello\"}";
        String signResponse = sendPostRequest("/sign", signRequest);

        JsonObject signJson = gson.fromJson(signResponse, JsonObject.class);
        assertTrue(signJson.has("signature"));
        String signature = signJson.get("signature").getAsString();

        assertTrue(Codec.isValidBase64Url(signature));

        String verifyRequest = String.format("{\"msg\":\"hello\",\"signature\":\"%s\"}", signature);
        String verifyResponse = sendPostRequest("/verify", verifyRequest);

        JsonObject verifyJson = gson.fromJson(verifyResponse, JsonObject.class);
        assertTrue(verifyJson.has("ok"));
        assertTrue(verifyJson.get("ok").getAsBoolean());

        System.out.println("✅ Тест 1 пройден");
    }

    @Test
    @DisplayName("Тест 2: Неверная подпись - изменить 1 байт → ok=false")
    void testInvalidSignatureOneByteChanged() throws IOException {
        System.out.println("🧪 Тест 2: Неверная подпись");

        String signRequest = "{\"msg\":\"test message\"}";
        String signResponse = sendPostRequest("/sign", signRequest);

        JsonObject signJson = gson.fromJson(signResponse, JsonObject.class);
        String originalSignature = signJson.get("signature").getAsString();

        byte[] signatureBytes = Codec.decodeFromBase64Url(originalSignature);
        signatureBytes[0] ^= 0xFF;
        String corruptedSignature = Codec.encodeToBase64Url(signatureBytes);

        String verifyRequest = String.format("{\"msg\":\"test message\",\"signature\":\"%s\"}", corruptedSignature);
        String verifyResponse = sendPostRequest("/verify", verifyRequest);

        JsonObject verifyJson = gson.fromJson(verifyResponse, JsonObject.class);
        assertTrue(verifyJson.has("ok"));
        assertFalse(verifyJson.get("ok").getAsBoolean());

        System.out.println("✅ Тест 2 пройден");
    }

    @Test
    @DisplayName("Тест 3: Изменённое сообщение → ok=false")
    void testModifiedMessageFailsVerification() throws IOException {
        System.out.println("🧪 Тест 3: Изменённое сообщение");

        String signRequest = "{\"msg\":\"hello\"}";
        String signResponse = sendPostRequest("/sign", signRequest);

        JsonObject signJson = gson.fromJson(signResponse, JsonObject.class);
        String signature = signJson.get("signature").getAsString();

        String verifyRequest = String.format("{\"msg\":\"hello!\",\"signature\":\"%s\"}", signature);
        String verifyResponse = sendPostRequest("/verify", verifyRequest);

        JsonObject verifyJson = gson.fromJson(verifyResponse, JsonObject.class);
        assertTrue(verifyJson.has("ok"));
        assertFalse(verifyJson.get("ok").getAsBoolean());

        System.out.println("✅ Тест 3 пройден");
    }

    @Test
    @DisplayName("Тест 4: Невалидная base64url → 400 invalid_signature_format")
    void testInvalidBase64UrlSignature() throws IOException {
        System.out.println("🧪 Тест 4: Невалидная base64url");

        String verifyRequest = "{\"msg\":\"test\",\"signature\":\"@@@invalid@@@\"}";

        int responseCode = getResponseCode("/verify", verifyRequest);
        assertEquals(400, responseCode, "Ожидался код 400");

        System.out.println("✅ Тест 4 пройден");
    }

    @Test
    @DisplayName("Тест 5: Пустой msg → 400 invalid_msg")
    void testEmptyMessage() throws IOException {
        System.out.println("🧪 Тест 5: Пустой msg");

        String signRequest = "{\"msg\":\"\"}";

        int responseCode = getResponseCode("/sign", signRequest);
        assertEquals(400, responseCode, "Ожидался код 400");

        System.out.println("✅ Тест 5 пройден");
    }

    @Test
    @DisplayName("Тест 6: Большое сообщение (> maxMsgSizeBytes) → 413")
    void testLargeMessage() throws IOException {
        System.out.println("🧪 Тест 6: Большое сообщение");

        StringBuilder largeMessage = new StringBuilder();
        largeMessage.append("A".repeat(MAX_MSG_SIZE + 100));

        String signRequest = String.format("{\"msg\":\"%s\"}", largeMessage.toString());

        int responseCode = getResponseCode("/sign", signRequest);
        assertEquals(413, responseCode, "Ожидался код 413");

        System.out.println("✅ Тест 6 пройден");
    }

    @Test
    @DisplayName("Тест 7: Стабильность кодирования - одинаковый msg → одинаковая signature")
    void testDeterministicSignatures() throws IOException {
        System.out.println("🧪 Тест 7: Стабильность кодирования");

        String message = "test deterministic message";
        String signRequest = String.format("{\"msg\":\"%s\"}", message);

        String signResponse1 = sendPostRequest("/sign", signRequest);
        JsonObject signJson1 = gson.fromJson(signResponse1, JsonObject.class);
        String signature1 = signJson1.get("signature").getAsString();

        String signResponse2 = sendPostRequest("/sign", signRequest);
        JsonObject signJson2 = gson.fromJson(signResponse2, JsonObject.class);
        String signature2 = signJson2.get("signature").getAsString();

        assertEquals(signature1, signature2,
                "Подписи для одинакового сообщения должны быть идентичны");

        System.out.println("✅ Тест 7 пройден");
    }

    @Test
    @DisplayName("Тест 8: Тайминг-стойкое сравнение (косвенная проверка через HmacService)")
    void testTimingSafeComparison() {
        System.out.println("🧪 Тест 8: Тайминг-стойкое сравнение");

        byte[] secretKey = Base64.getDecoder().decode(TEST_SECRET);
        HmacService hmacService = new HmacService(secretKey);

        String message = "test timing safety";
        String signature = hmacService.sign(message);

        byte[] signatureBytes = Codec.decodeFromBase64Url(signature);
        signatureBytes[signatureBytes.length - 1] ^= 0xFF;
        String wrongSignature = Codec.encodeToBase64Url(signatureBytes);

        boolean result = hmacService.verify(message, wrongSignature);
        assertFalse(result, "Невалидная подпись должна возвращать false");

        boolean validResult = hmacService.verify(message, signature);
        assertTrue(validResult, "Валидная подпись должна возвращать true");

        System.out.println("✅ Тест 8 пройден");
    }

    @Test
    @DisplayName("Тест 9: Конфиг-ошибки - некорректный secret → ошибка загрузки")
    void testConfigErrors() throws IOException {
        System.out.println("🧪 Тест 9: Конфиг-ошибки");

        String invalidConfig = "{\"hmacAlg\":\"SHA256\",\"secret\":\"not-valid-base64!!!\",\"listenPort\":18081,\"maxMsgSizeBytes\":1024}";

        Files.writeString(Path.of("invalid_config_test.json"), invalidConfig);

        Exception exception = assertThrows(IOException.class, () -> {
            ConfigLoader.load("invalid_config_test.json");
        });

        assertTrue(exception.getMessage().contains("secret") ||
                        exception.getMessage().contains("base64"),
                "Ожидалось сообщение об ошибке secret/base64, но получили: " + exception.getMessage());

        // Очистка
        Files.deleteIfExists(Path.of("invalid_config_test.json"));

        System.out.println("✅ Тест 9 пройден");
    }

    @Test
    @DisplayName("Тест 10: Невалидный Content-Type → 415")
    void testInvalidContentType() throws IOException {
        System.out.println("🧪 Тест 10: Невалидный Content-Type");

        URL url = new URL(baseUrl + "/sign");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/plain");
        conn.setDoOutput(true);
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);

        String jsonBody = "{\"msg\":\"test\"}";
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        assertEquals(415, conn.getResponseCode(), "Ожидался код 415");

        System.out.println("✅ Тест 10 пройден");
    }

    @Test
    @DisplayName("Тест 11: Неверный HTTP метод → 405")
    void testWrongHttpMethod() throws IOException {
        System.out.println("🧪 Тест 11: Неверный HTTP метод");

        URL url = new URL(baseUrl + "/sign");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);

        assertEquals(405, conn.getResponseCode(), "Ожидался код 405");

        System.out.println("✅ Тест 11 пройден");
    }

    @Test
    @DisplayName("Тест 12: Несуществующий endpoint → 404")
    void testNonExistentEndpoint() throws IOException {
        System.out.println("🧪 Тест 12: Несуществующий endpoint");

        String request = "{\"msg\":\"test\"}";

        int responseCode = getResponseCode("/nonexistent", request);
        assertEquals(404, responseCode, "Ожидался код 404");

        System.out.println("✅ Тест 12 пройден");
    }
}