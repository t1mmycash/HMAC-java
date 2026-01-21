package ru.yandex.practicum.crypto;

import ru.yandex.practicum.config.AppConfig;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class HmacService {
    private final byte[] secretKey;
    private final String hmacAlgorithm;

    public HmacService(AppConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }

        this.secretKey = config.getSecretBytes();
        this.hmacAlgorithm = config.getHmacAlg();

        if (secretKey == null || secretKey.length == 0) {
            throw new IllegalArgumentException("Secret key cannot be null or empty");
        }
        if (hmacAlgorithm == null || hmacAlgorithm.isEmpty()) {
            throw new IllegalArgumentException("HMAC algorithm cannot be null or empty");
        }
    }

    public String sign(String message) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }

        try {
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            byte[] hmacBytes = calculateHmac(messageBytes);
            return Codec.encodeToBase64Url(hmacBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("HMAC algorithm not available: " + hmacAlgorithm, e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Invalid secret key for HMAC", e);
        }
    }

    public boolean verify(String message, String signature) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        if (signature == null) {
            throw new IllegalArgumentException("Signature cannot be null");
        }

        try {
            String expectedSignature = sign(message);

            byte[] providedSignatureBytes = Codec.decodeFromBase64Url(signature);
            byte[] expectedSignatureBytes = Codec.decodeFromBase64Url(expectedSignature);

            return SecureComparator.constantTimeEquals(
                    providedSignatureBytes,
                    expectedSignatureBytes
            );

        } catch (IllegalArgumentException e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify signature", e);
        }
    }

    private byte[] calculateHmac(byte[] data)
            throws NoSuchAlgorithmException, InvalidKeyException {

        Mac mac = Mac.getInstance(hmacAlgorithm);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, hmacAlgorithm);
        mac.init(secretKeySpec);
        return mac.doFinal(data);
    }

    public int getSecretKeyLength() {
        return secretKey.length;
    }

    public boolean isInitialized() {
        return secretKey != null && secretKey.length > 0;
    }
}