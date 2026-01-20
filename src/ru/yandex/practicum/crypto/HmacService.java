package ru.yandex.practicum.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class HmacService {
    private final byte[] secretKey;
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    public HmacService(byte[] secretKey) {
        if (secretKey == null || secretKey.length == 0) {
            throw new IllegalArgumentException("Secret key cannot be null or empty");
        }
        this.secretKey = secretKey.clone();
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
            throw new RuntimeException("HMAC algorithm not available: " + HMAC_ALGORITHM, e);
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

        Mac mac = Mac.getInstance(HMAC_ALGORITHM);

        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, HMAC_ALGORITHM);
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