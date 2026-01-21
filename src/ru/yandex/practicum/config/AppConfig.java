package ru.yandex.practicum.config;

import java.util.Base64;

public class AppConfig {
    private String hmacAlg;
    private String secret;
    private int listenPort;
    private int maxMsgSizeBytes;

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

        if (listenPort == 0) {
            listenPort = 8080;
        }

        if (maxMsgSizeBytes == 0) {
            maxMsgSizeBytes = 1048576;
        }

        if (listenPort < 1 || listenPort > 65535) {
            throw new IllegalArgumentException(
                    "Config error: 'listenPort' must be between 1 and 65535");
        }

        if (maxMsgSizeBytes <= 0) {
            throw new IllegalArgumentException(
                    "Config error: 'maxMsgSizeBytes' must be positive");
        }

        if (hmacAlg == null) {
            hmacAlg = "HmacSHA256";
        } else {
            if (!"HmacSHA256".equals(hmacAlg.trim())) {
                throw new IllegalArgumentException(
                        "Config error: 'hmacAlg' must be exactly 'HmacSHA256'");
            }
            this.hmacAlg = hmacAlg.trim();
        }
    }

    @Override
    public String toString() {
        return String.format(
                "AppConfig{hmacAlg='%s', listenPort=%d, maxMsgSizeBytes=%d}",
                hmacAlg, listenPort, maxMsgSizeBytes
        );
    }
}