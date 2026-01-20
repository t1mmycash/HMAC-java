package ru.yandex.practicum.config;

import java.util.Base64;

public class AppConfig {
    private String hmacAlg = "SHA256";
    private String secret;
    private int listenPort = 8080;
    private int maxMsgSizeBytes = 1048576;

    public AppConfig() {
    }

    public String getHmacAlg() {
        return hmacAlg;
    }

    public String getSecret() {
        return secret;
    }

    public int getListenPort() {
        return listenPort;
    }

    public int getMaxMsgSizeBytes() {
        return maxMsgSizeBytes;
    }

    public void setHmacAlg(String hmacAlg) {
        this.hmacAlg = hmacAlg;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    public void setMaxMsgSizeBytes(int maxMsgSizeBytes) {
        this.maxMsgSizeBytes = maxMsgSizeBytes;
    }

    public byte[] getSecretBytes() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("Secret is not configured");
        }
        return Base64.getDecoder().decode(secret);
    }

    public void validate() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalArgumentException("Config error: 'secret' field is required");
        }

        try {
            Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Config error: 'secret' must be valid base64", e);
        }

        if (listenPort < 1 || listenPort > 65535) {
            throw new IllegalArgumentException(
                    "Config error: 'listenPort' must be between 1 and 65535");
        }

        if (maxMsgSizeBytes <= 0) {
            throw new IllegalArgumentException(
                    "Config error: 'maxMsgSizeBytes' must be positive");
        }

        String normalizedHmacAlg = normalizeHmacAlg(hmacAlg);

        if (!"SHA256".equals(normalizedHmacAlg)) {
            throw new IllegalArgumentException(
                    "Config error: 'hmacAlg' must be 'SHA256' (only SHA256 supported)");
        }

        this.hmacAlg = normalizedHmacAlg;
    }

    private String normalizeHmacAlg(String alg) {
        if (alg == null) {
            return "SHA256";
        }
        return alg.trim().toUpperCase();
    }

    @Override
    public String toString() {
        return String.format(
                "AppConfig{hmacAlg='%s', listenPort=%d, maxMsgSizeBytes=%d}",
                hmacAlg, listenPort, maxMsgSizeBytes
        );
    }
}
