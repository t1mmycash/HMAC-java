package ru.yandex.practicum;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import ru.yandex.practicum.config.AppConfig;
import ru.yandex.practicum.config.ConfigLoader;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.io.File;

public class ConfigTest {

    private Path createTempFile(String content) throws IOException {
        Path tempFile = Files.createTempFile("config-test-", ".json");
        Files.writeString(tempFile, content);
        tempFile.toFile().deleteOnExit();
        return tempFile;
    }

    @Test
    @DisplayName("Загрузка валидной конфигурации со всеми полями")
    void testLoadValidConfig() throws IOException {
        String configJson = """
                {
                  "hmacAlg": "SHA256",
                  "secret": "YQ==",
                  "listenPort": 8080,
                  "maxMsgSizeBytes": 1048576
                }
                """;

        Path configFile = createTempFile(configJson);

        AppConfig config = ConfigLoader.load(configFile.toString());

        assertEquals("SHA256", config.getHmacAlg());
        assertEquals("YQ==", config.getSecret());
        assertEquals(8080, config.getListenPort());
        assertEquals(1048576, config.getMaxMsgSizeBytes());

        byte[] secretBytes = config.getSecretBytes();
        assertEquals("a", new String(secretBytes));
    }

    @Test
    @DisplayName("Загрузка минимальной конфигурации (только обязательный секрет)")
    void testLoadMinimalConfig() throws IOException {
        String configJson = """
                {
                  "secret": "YQ=="
                }
                """;

        Path configFile = createTempFile(configJson);

        AppConfig config = ConfigLoader.load(configFile.toString());

        assertEquals("SHA256", config.getHmacAlg());
        assertEquals(8080, config.getListenPort());
        assertEquals(1048576, config.getMaxMsgSizeBytes());
        assertEquals("YQ==", config.getSecret());
    }

    @Test
    @DisplayName("Ошибка загрузки: отсутствует обязательное поле 'secret'")
    void testLoadMissingSecret() throws IOException {
        String configJson = """
                {
                  "listenPort": 8080,
                  "maxMsgSizeBytes": 1024
                }
                """;

        Path configFile = createTempFile(configJson);

        IOException exception = assertThrows(IOException.class,
                () -> ConfigLoader.load(configFile.toString()));

        assertTrue(exception.getMessage().contains("secret"));
    }

    @Test
    @DisplayName("Ошибка загрузки: секрет не является валидным base64")
    void testLoadInvalidBase64Secret() throws IOException {
        String configJson = """
                {
                  "secret": "не-base64-строка@@@",
                  "listenPort": 8080
                }
                """;

        Path configFile = createTempFile(configJson);

        IOException exception = assertThrows(IOException.class,
                () -> ConfigLoader.load(configFile.toString()));

        assertTrue(exception.getMessage().contains("base64"));
    }

    @Test
    @DisplayName("Ошибка загрузки: некорректный порт (меньше 1)")
    void testLoadInvalidPortLow() throws IOException {
        String configJson = """
                {
                  "secret": "YQ==",
                  "listenPort": 0
                }
                """;

        Path configFile = createTempFile(configJson);

        IOException exception = assertThrows(IOException.class,
                () -> ConfigLoader.load(configFile.toString()));

        assertTrue(exception.getMessage().contains("listenPort"));
    }

    @Test
    @DisplayName("Ошибка загрузки: некорректный порт (больше 65535)")
    void testLoadInvalidPortHigh() throws IOException {
        String configJson = """
                {
                  "secret": "YQ==",
                  "listenPort": 70000
                }
                """;

        Path configFile = createTempFile(configJson);

        IOException exception = assertThrows(IOException.class,
                () -> ConfigLoader.load(configFile.toString()));

        assertTrue(exception.getMessage().contains("listenPort"));
    }

    @Test
    @DisplayName("Ошибка загрузки: неположительный maxMsgSizeBytes")
    void testLoadInvalidMaxSize() throws IOException {
        String configJson = """
                {
                  "secret": "YQ==",
                  "maxMsgSizeBytes": 0
                }
                """;

        Path configFile = createTempFile(configJson);

        IOException exception = assertThrows(IOException.class,
                () -> ConfigLoader.load(configFile.toString()));

        assertTrue(exception.getMessage().contains("maxMsgSizeBytes"));
    }

    @Test
    @DisplayName("Ошибка загрузки: неверный алгоритм HMAC (не SHA256)")
    void testLoadInvalidHmacAlg() throws IOException {
        String configJson = """
                {
                  "secret": "YQ==",
                  "hmacAlg": "SHA512"
                }
                """;

        Path configFile = createTempFile(configJson);

        IOException exception = assertThrows(IOException.class,
                () -> ConfigLoader.load(configFile.toString()));

        assertTrue(exception.getMessage().contains("hmacAlg"));
    }

    @Test
    @DisplayName("Ошибка загрузки: файл конфигурации не найден")
    void testLoadFileNotFound() {
        String nonExistentFile = System.getProperty("java.io.tmpdir") +
                File.separator + "non_existent_config_" +
                System.currentTimeMillis() + ".json";

        IOException exception = assertThrows(IOException.class,
                () -> ConfigLoader.load(nonExistentFile));

        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    @DisplayName("Ошибка загрузки: путь является директорией, а не файлом")
    void testLoadPathIsDirectory() throws IOException {
        Path tempDir = Files.createTempDirectory("config-test-dir-");
        tempDir.toFile().deleteOnExit();

        IOException exception = assertThrows(IOException.class,
                () -> ConfigLoader.load(tempDir.toString()));

        assertTrue(exception.getMessage().contains("file") ||
                exception.getMessage().contains("directory"));
    }

    @Test
    @DisplayName("Ошибка загрузки: некорректный JSON формат")
    void testLoadInvalidJsonFormat() throws IOException {
        String invalidJson = "{ это не json }";

        Path configFile = createTempFile(invalidJson);

        IOException exception = assertThrows(IOException.class,
                () -> ConfigLoader.load(configFile.toString()));

        assertTrue(exception.getMessage().contains("JSON"));
    }

    @Test
    @DisplayName("Ошибка загрузки: пустой файл конфигурации")
    void testLoadEmptyFile() throws IOException {
        Path configFile = createTempFile("");

        IOException exception = assertThrows(IOException.class,
                () -> ConfigLoader.load(configFile.toString()));

        assertTrue(exception.getMessage().contains("empty") ||
                exception.getMessage().contains("JSON") ||
                exception.getMessage().contains("null"));
    }

    @Test
    @DisplayName("Валидация AppConfig: успешная валидация")
    void testAppConfigValidationSuccess() {
        AppConfig config = new AppConfig();
        config.setSecret("YQ==");
        config.setListenPort(8080);
        config.setMaxMsgSizeBytes(1024);
        config.setHmacAlg("SHA256");

        assertDoesNotThrow(config::validate);
    }

    @Test
    @DisplayName("Валидация AppConfig: отсутствует секрет")
    void testAppConfigValidationMissingSecret() {
        AppConfig config = new AppConfig();
        config.setListenPort(8080);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                config::validate);

        assertTrue(exception.getMessage().contains("secret"));
    }

    @Test
    @DisplayName("Валидация AppConfig: пустой секрет")
    void testAppConfigValidationEmptySecret() {
        AppConfig config = new AppConfig();
        config.setSecret("");
        config.setListenPort(8080);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                config::validate);

        assertTrue(exception.getMessage().contains("secret"));
    }

    @Test
    @DisplayName("Валидация AppConfig: только пробелы в секрете")
    void testAppConfigValidationBlankSecret() {
        AppConfig config = new AppConfig();
        config.setSecret("   ");
        config.setListenPort(8080);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                config::validate);

        assertTrue(exception.getMessage().contains("secret"));
    }

    @Test
    @DisplayName("Метод getSecretBytes: корректное декодирование base64")
    void testGetSecretBytesValid() {
        AppConfig config = new AppConfig();
        String originalText = "test-secret-123";
        String base64Text = Base64.getEncoder().encodeToString(originalText.getBytes());
        config.setSecret(base64Text);

        byte[] secretBytes = config.getSecretBytes();
        assertEquals(originalText, new String(secretBytes));
    }

    @Test
    @DisplayName("Метод getSecretBytes: ошибка при отсутствии секрета")
    void testGetSecretBytesWhenNotSet() {
        AppConfig config = new AppConfig();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                config::getSecretBytes);

        assertTrue(exception.getMessage().contains("Secret"));
    }

    @Test
    @DisplayName("Метод getSecretBytes: ошибка при пустом секрете")
    void testGetSecretBytesWhenEmpty() {
        AppConfig config = new AppConfig();
        config.setSecret("");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                config::getSecretBytes);

        assertTrue(exception.getMessage().contains("Secret"));
    }

    @Test
    @DisplayName("Безопасный toString: не отображает секрет")
    void testToStringSafety() {
        AppConfig config = new AppConfig();
        config.setHmacAlg("SHA256");
        config.setSecret("super-secret-base64-string");
        config.setListenPort(8080);
        config.setMaxMsgSizeBytes(1024);

        String str = config.toString();

        assertFalse(str.contains("super-secret-base64-string"));
        assertFalse(str.contains("secret"));

        assertTrue(str.contains("SHA256"));
        assertTrue(str.contains("8080"));
        assertTrue(str.contains("1024"));
        assertTrue(str.contains("AppConfig"));
    }

    @Test
    @DisplayName("Загрузка конфигурации с разными регистрами в hmacAlg")
    void testLoadCaseInsensitiveHmacAlg() throws IOException {
        String[] cases = {"SHA256", "sha256", "Sha256"};

        for (String hmacAlgCase : cases) {
            String configJson = String.format("""
                    {
                      "secret": "YQ==",
                      "hmacAlg": "%s"
                    }
                    """, hmacAlgCase);

            Path configFile = createTempFile(configJson);

            AppConfig config = ConfigLoader.load(configFile.toString());
            assertEquals("SHA256", config.getHmacAlg());
        }
    }

    @Test
    @DisplayName("Геттеры и сеттеры AppConfig")
    void testGettersAndSetters() {
        AppConfig config = new AppConfig();

        assertEquals("SHA256", config.getHmacAlg());
        assertEquals(8080, config.getListenPort());
        assertEquals(1048576, config.getMaxMsgSizeBytes());
        assertNull(config.getSecret());

        config.setHmacAlg("SHA256");
        config.setSecret("test");
        config.setListenPort(9090);
        config.setMaxMsgSizeBytes(2048);

        assertEquals("SHA256", config.getHmacAlg());
        assertEquals("test", config.getSecret());
        assertEquals(9090, config.getListenPort());
        assertEquals(2048, config.getMaxMsgSizeBytes());
    }

    @Test
    @DisplayName("Загрузка конфигурации с дополнительными полями (должна игнорироваться)")
    void testLoadWithExtraFields() throws IOException {
        String configJson = """
                {
                  "secret": "YQ==",
                  "listenPort": 9090,
                  "extraField1": "should be ignored",
                  "extraField2": 12345
                }
                """;

        Path configFile = createTempFile(configJson);

        AppConfig config = ConfigLoader.load(configFile.toString());

        assertEquals("SHA256", config.getHmacAlg());
        assertEquals("YQ==", config.getSecret());
        assertEquals(9090, config.getListenPort());
        assertEquals(1048576, config.getMaxMsgSizeBytes());
    }

    @Test
    @DisplayName("Проверка дефолтных значений при null полях в JSON")
    void testLoadWithNullFields() throws IOException {
        String configJson = """
                {
                  "secret": "YQ==",
                  "hmacAlg": null
                }
                """;

        Path configFile = createTempFile(configJson);

        AppConfig config = ConfigLoader.load(configFile.toString());

        assertEquals("SHA256", config.getHmacAlg());
        assertEquals("YQ==", config.getSecret());
        assertEquals(8080, config.getListenPort());
        assertEquals(1048576, config.getMaxMsgSizeBytes());
    }

    @Test
    @DisplayName("Проверка граничных значений порта")
    void testLoadBoundaryPortValues() throws IOException {
        // Минимальный валидный порт
        String configJson1 = """
                {
                  "secret": "YQ==",
                  "listenPort": 1
                }
                """;

        Path configFile1 = createTempFile(configJson1);
        AppConfig config1 = ConfigLoader.load(configFile1.toString());
        assertEquals(1, config1.getListenPort());

        String configJson2 = """
                {
                  "secret": "YQ==",
                  "listenPort": 65535
                }
                """;

        Path configFile2 = createTempFile(configJson2);
        AppConfig config2 = ConfigLoader.load(configFile2.toString());
        assertEquals(65535, config2.getListenPort());
    }

    @Test
    @DisplayName("Проверка большого maxMsgSizeBytes")
    void testLoadLargeMaxSize() throws IOException {
        String configJson = """
                {
                  "secret": "YQ==",
                  "maxMsgSizeBytes": 1073741824
                }
                """;

        Path configFile = createTempFile(configJson);

        AppConfig config = ConfigLoader.load(configFile.toString());
        assertEquals(1073741824, config.getMaxMsgSizeBytes());
    }

    @Test
    @DisplayName("Валидация: отрицательный maxMsgSizeBytes")
    void testValidationNegativeMaxSize() {
        AppConfig config = new AppConfig();
        config.setSecret("YQ==");
        config.setMaxMsgSizeBytes(-100);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                config::validate);

        assertTrue(exception.getMessage().contains("maxMsgSizeBytes"));
    }

    @Test
    @DisplayName("Валидация: порт равен 0")
    void testValidationPortZero() {
        AppConfig config = new AppConfig();
        config.setSecret("YQ==");
        config.setListenPort(0);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                config::validate);

        assertTrue(exception.getMessage().contains("listenPort"));
    }

    @Test
    @DisplayName("Валидация: порт больше 65535")
    void testValidationPortTooHigh() {
        AppConfig config = new AppConfig();
        config.setSecret("YQ==");
        config.setListenPort(65536);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                config::validate);

        assertTrue(exception.getMessage().contains("listenPort"));
    }
}