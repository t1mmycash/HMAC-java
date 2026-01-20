package ru.yandex.practicum.config;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public class ConfigLoader {
    private static final Gson GSON = new Gson();

    public static AppConfig load(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("Configuration file not found: " + filePath);
        }

        if (!Files.isRegularFile(path)) {
            throw new IOException("Path is not a file: " + filePath);
        }

        checkFilePermissions(path);

        try (FileReader reader = new FileReader(filePath)) {
            AppConfig config = GSON.fromJson(reader, AppConfig.class);

            if (config == null) {
                throw new IOException("Config file is empty or invalid JSON");
            }

            try {
                config.validate();
            } catch (IllegalArgumentException e) {
                throw new IOException("Invalid configuration: " + e.getMessage(), e);
            }

            return config;

        } catch (JsonSyntaxException e) {
            throw new IOException("Invalid JSON format in config file", e);
        }
    }

    private static void checkFilePermissions(Path path) throws IOException {
        try {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path);

            if (permissions.contains(PosixFilePermission.OTHERS_WRITE)) {
                throw new IOException(
                        "SECURITY RISK: config.json is world-writable!\n" +
                                "Attackers could modify your secret key.\n" +
                                "Fix with: chmod 600 " + path.toAbsolutePath()
                );
            }

            if (permissions.contains(PosixFilePermission.GROUP_WRITE)) {
                System.err.println("WARNING: config.json is group-writable.");
                System.err.println("Consider: chmod 640 " + path.toAbsolutePath());
            }

            if (permissions.contains(PosixFilePermission.OTHERS_READ)) {
                System.err.println("WARNING: config.json is world-readable.");
                System.err.println("Consider: chmod 600 " + path.toAbsolutePath());
            }

        } catch (UnsupportedOperationException e) {
            System.out.println("NOTE: File permission check skipped (Windows system).");
            System.out.println("On Windows, ensure config.json is not accessible to other users.");
        } catch (IOException e) {
            System.err.println("WARNING: Could not check file permissions: " + e.getMessage());
        }
    }
}
