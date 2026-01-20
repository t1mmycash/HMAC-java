package ru.yandex.practicum.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.Base64;

public class SecretRotator {
    public static void main(String[] args) {
        try {
            String configPath = (args.length > 0) ? args[0] : "config.json";

            System.out.println("🔐 HMAC Secret Rotator");
            System.out.println("======================");

            rotateSecret(configPath);

        } catch (Exception e) {
            System.err.println("❌ Ошибка: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void rotateSecret(String configPath) throws Exception {
        Path configFile = Path.of(configPath);

        if (!Files.exists(configFile)) {
            throw new RuntimeException("Файл конфигурации не найден: " + configPath);
        }

        System.out.println("📄 Чтение конфигурации: " + configPath);

        Gson gson = new Gson();
        JsonObject config;
        try (FileReader reader = new FileReader(configPath)) {
            config = gson.fromJson(reader, JsonObject.class);
        }

        if (config == null) {
            throw new RuntimeException("Неверный формат JSON в файле конфигурации");
        }

        String oldSecret = config.has("secret") ? config.get("secret").getAsString() : null;
        if (oldSecret != null) {
            System.out.println(" Текущий секрет: " + maskSecret(oldSecret));
        } else {
            System.out.println("⚠️  Внимание: секрет не найден в конфиге");
        }

        System.out.println("🔄 Генерация нового секрета...");
        SecureRandom random = new SecureRandom();
        byte[] newSecretBytes = new byte[32];
        random.nextBytes(newSecretBytes);
        String newSecret = Base64.getEncoder().encodeToString(newSecretBytes);

        System.out.println("✅ Новый секрет: " + maskSecret(newSecret));

        String backupPath = createBackup(configPath);
        System.out.println("💾 Резервная копия: " + backupPath);

        config.addProperty("secret", newSecret);

        Gson prettyGson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();

        try (FileWriter writer = new FileWriter(configPath)) {
            prettyGson.toJson(config, writer);
        }

        // 7. Вывод инструкций
        System.out.println("\n🎉 Ротация секрета успешно завершена!");
        System.out.println("=====================================");
        System.out.println("\n⚠️  ВАЖНЫЕ ЗАМЕЧАНИЯ:");
        System.out.println("1. Все ранее созданные подписи станут невалидными");
        System.out.println("2. Все клиенты должны использовать новый секрет");
        System.out.println("3. Необходимо перезапустить сервер:");
        System.out.println("   - Остановить текущий сервер (Ctrl+C)");
        System.out.println("   - Запустить заново: java -cp ... ru.yandex.practicum.ServerHMAC");
        System.out.println("\n🔧 Для отката используйте резервную копию:");
        System.out.println("   cp " + backupPath + " " + configPath);
    }

    private static String createBackup(String originalPath) throws Exception {
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupPath = originalPath + ".backup_" + timestamp;

        Files.copy(Path.of(originalPath), Path.of(backupPath), StandardCopyOption.REPLACE_EXISTING);

        return backupPath;
    }

    private static String maskSecret(String secret) {
        if (secret == null || secret.length() <= 8) {
            return "***";
        }
        return secret.substring(0, 4) + "..." + secret.substring(secret.length() - 4);
    }
}