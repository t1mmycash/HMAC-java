package ru.yandex.practicum;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import ru.yandex.practicum.crypto.Codec;
import ru.yandex.practicum.crypto.HmacService;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;

public class HmacServiceTest {

    private HmacService hmacService;
    private byte[] testSecretKey;

    @BeforeEach
    void setUp() {
        testSecretKey = "test-secret-key-1234567890".getBytes(StandardCharsets.UTF_8);
        hmacService = new HmacService(testSecretKey);
    }

    @Test
    @DisplayName("Создание сервиса с валидным ключом")
    void testConstructorWithValidKey() {
        assertNotNull(hmacService);
        assertTrue(hmacService.isInitialized());
        assertEquals(testSecretKey.length, hmacService.getSecretKeyLength());
    }

    @Test
    @DisplayName("Создание сервиса с null ключом должно бросать исключение")
    void testConstructorWithNullKey() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new HmacService(null));
        assertTrue(exception.getMessage().contains("Secret key"));
    }

    @Test
    @DisplayName("Создание сервиса с пустым ключом должно бросать исключение")
    void testConstructorWithEmptyKey() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new HmacService(new byte[0]));
        assertTrue(exception.getMessage().contains("Secret key"));
    }

    @Test
    @DisplayName("Подпись null сообщения должна бросать исключение")
    void testSignNullMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> hmacService.sign(null));
        assertTrue(exception.getMessage().contains("Message"));
    }

    @Test
    @DisplayName("Проверка null сообщения должна бросать исключение")
    void testVerifyNullMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> hmacService.verify(null, "signature"));
        assertTrue(exception.getMessage().contains("Message"));
    }

    @Test
    @DisplayName("Проверка null подписи должна бросать исключение")
    void testVerifyNullSignature() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> hmacService.verify("message", null));
        assertTrue(exception.getMessage().contains("Signature"));
    }

    @Test
    @DisplayName("Подпись пустого сообщения")
    void testSignEmptyMessage() {
        String signature = hmacService.sign("");

        assertNotNull(signature);
        assertFalse(signature.isEmpty());
        assertTrue(Codec.isValidBase64Url(signature));

        assertTrue(hmacService.verify("", signature));
    }

    @Test
    @DisplayName("Подпись обычного текста")
    void testSignRegularText() {
        String message = "Hello, HMAC World!";
        String signature = hmacService.sign(message);

        assertNotNull(signature);
        assertTrue(Codec.isValidBase64Url(signature));

        assertTrue(hmacService.verify(message, signature));
    }

    @Test
    @DisplayName("Подпись длинного текста")
    void testSignLongText() {
        String message = "A".repeat(1000);
        String signature = hmacService.sign(message);

        assertNotNull(signature);
        assertTrue(Codec.isValidBase64Url(signature));
        assertTrue(hmacService.verify(message, signature));
    }

    @Test
    @DisplayName("Подпись текста с спецсимволами и кириллицей")
    void testSignSpecialCharacters() {
        String message = "Привет! Hello! @#$%^&*()\n\t\r";
        String signature = hmacService.sign(message);

        assertNotNull(signature);
        assertTrue(Codec.isValidBase64Url(signature));
        assertTrue(hmacService.verify(message, signature));
    }

    @Test
    @DisplayName("Детерминированность: одинаковые сообщения → одинаковые подписи")
    void testDeterministicSignatures() {
        String message = "Test message";

        String signature1 = hmacService.sign(message);
        String signature2 = hmacService.sign(message);
        String signature3 = hmacService.sign(message);

        assertEquals(signature1, signature2);
        assertEquals(signature2, signature3);
        assertTrue(hmacService.verify(message, signature1));
    }

    @Test
    @DisplayName("Разные сообщения → разные подписи")
    void testDifferentMessagesDifferentSignatures() {
        String message1 = "Message 1";
        String message2 = "Message 2";

        String signature1 = hmacService.sign(message1);
        String signature2 = hmacService.sign(message2);

        assertNotEquals(signature1, signature2);
        assertTrue(hmacService.verify(message1, signature1));
        assertTrue(hmacService.verify(message2, signature2));
        assertFalse(hmacService.verify(message1, signature2));
        assertFalse(hmacService.verify(message2, signature1));
    }

    @Test
    @DisplayName("Проверка верной подписи")
    void testVerifyValidSignature() {
        String message = "Test verification";
        String signature = hmacService.sign(message);

        assertTrue(hmacService.verify(message, signature));
    }

    @Test
    @DisplayName("Проверка неверной подписи (изменен 1 байт)")
    void testVerifyInvalidSignatureOneByteChanged() {
        String message = "Test message";
        String originalSignature = hmacService.sign(message);

        byte[] signatureBytes = Codec.decodeFromBase64Url(originalSignature);
        signatureBytes[0] ^= (byte) 0xFF; // Инвертируем первый байт
        String corruptedSignature = Codec.encodeToBase64Url(signatureBytes);

        assertFalse(hmacService.verify(message, corruptedSignature));
    }

    @Test
    @DisplayName("Проверка неверной подписи (другая подпись)")
    void testVerifyInvalidSignatureDifferent() {
        String message = "Test message";
        String signatureForOtherMessage = hmacService.sign("Other message");

        assertFalse(hmacService.verify(message, signatureForOtherMessage));
    }

    @Test
    @DisplayName("Проверка неверной подписи (пустая подпись)")
    void testVerifyEmptySignature() {
        assertFalse(hmacService.verify("message", ""));
    }

    @Test
    @DisplayName("Проверка неверной подписи (не base64url)")
    void testVerifyInvalidBase64UrlSignature() {
        assertFalse(hmacService.verify("message", "@@@invalid@@@"));
    }

    @Test
    @DisplayName("Проверка неверной подписи (неправильная длина для HMAC-SHA256)")
    void testVerifyWrongLengthSignature() {
        byte[] wrongLengthSignature = new byte[31];
        String encodedSignature = Codec.encodeToBase64Url(wrongLengthSignature);

        assertFalse(hmacService.verify("message", encodedSignature));
    }

    @Test
    @DisplayName("Разные ключи → разные подписи для одного сообщения")
    void testDifferentKeysDifferentSignatures() {
        String message = "Same message";

        byte[] key1 = "key-one-1234567890".getBytes(StandardCharsets.UTF_8);
        HmacService service1 = new HmacService(key1);
        String signature1 = service1.sign(message);

        byte[] key2 = "key-two-0987654321".getBytes(StandardCharsets.UTF_8);
        HmacService service2 = new HmacService(key2);
        String signature2 = service2.sign(message);

        assertNotEquals(signature1, signature2);

        assertTrue(service1.verify(message, signature1));
        assertFalse(service1.verify(message, signature2));

        assertTrue(service2.verify(message, signature2));
        assertFalse(service2.verify(message, signature1));
    }

    @Test
    @DisplayName("Измененное сообщение → не проходит проверку")
    void testModifiedMessageFailsVerification() {
        String originalMessage = "Original message";
        String signature = hmacService.sign(originalMessage);

        String modifiedMessage = originalMessage + "!";

        assertFalse(hmacService.verify(modifiedMessage, signature));
    }

    @Test
    @DisplayName("Регистр в сообщении важен")
    void testMessageCaseSensitive() {
        String message1 = "Hello";
        String message2 = "hello";

        String signature1 = hmacService.sign(message1);
        String signature2 = hmacService.sign(message2);

        assertNotEquals(signature1, signature2);
        assertTrue(hmacService.verify(message1, signature1));
        assertTrue(hmacService.verify(message2, signature2));
        assertFalse(hmacService.verify(message1, signature2));
        assertFalse(hmacService.verify(message2, signature1));
    }

    @Test
    @DisplayName("Подпись в base64url формате (без +, /, =)")
    void testSignatureBase64UrlFormat() {
        String message = "Test base64url";
        String signature = hmacService.sign(message);

        assertFalse(signature.contains("+"));
        assertFalse(signature.contains("/"));
        assertFalse(signature.contains("="));

        assertTrue(Codec.isValidBase64Url(signature));

        byte[] decoded = Codec.decodeFromBase64Url(signature);
        assertEquals(32, decoded.length);
    }

    @Test
    @DisplayName("Проверка timing-safe сравнения (косвенная проверка)")
    void testTimingSafeComparison() {
        String message = "Test timing";
        String correctSignature = hmacService.sign(message);

        byte[] wrongBytes = Codec.decodeFromBase64Url(correctSignature);
        wrongBytes[0] ^= (byte) 0xFF;
        String wrongSignature = Codec.encodeToBase64Url(wrongBytes);

        assertTrue(hmacService.verify(message, correctSignature));
        assertFalse(hmacService.verify(message, wrongSignature));

        assertFalse(hmacService.verify(message, "invalid_signature_base64"));
    }

    @Test
    @DisplayName("Интеграционный тест: полный цикл подписи-проверки")
    void testFullSignVerifyCycle() {
        String[] testMessages = {
                "Simple",
                "",
                "With spaces and\ttabs",
                "With спецсимволы: !@#$%^&*()",
                "Кириллица и русский текст",
                "A".repeat(500),
                "Multi\nline\nmessage",
                "Ends with space ",
                " Starts with space"
        };

        for (String message : testMessages) {
            String signature = hmacService.sign(message);

            assertNotNull(signature);
            assertFalse(signature.isEmpty());
            assertTrue(Codec.isValidBase64Url(signature));

            assertTrue(hmacService.verify(message, signature),
                    "Failed for message: '" + message + "'");

            String modifiedMessage = message + "X";
            assertFalse(hmacService.verify(modifiedMessage, signature),
                    "Should fail for modified message: '" + modifiedMessage + "'");
        }
    }

    @Test
    @DisplayName("Проверка что сервис использует Codec для кодирования")
    void testUsesCodecForEncoding() {
        String message = "Test codec usage";
        String signature = hmacService.sign(message);

        byte[] decoded = Codec.decodeFromBase64Url(signature);
        String reencoded = Codec.encodeToBase64Url(decoded);

        assertEquals(signature, reencoded);
    }

    @Test
    @DisplayName("Проверка длины HMAC-SHA256 подписи")
    void testHmacSignatureLength() {
        String message = "Test length";
        String signature = hmacService.sign(message);

        byte[] decoded = Codec.decodeFromBase64Url(signature);
        assertEquals(32, decoded.length);

        assertTrue(Codec.isValidHmacSignatureLength(signature));
    }

    @Test
    @DisplayName("Проверка с очень длинным секретным ключом")
    void testWithVeryLongSecretKey() {
        byte[] longKey = new byte[1024];
        for (int i = 0; i < longKey.length; i++) {
            longKey[i] = (byte) (i % 256);
        }

        HmacService serviceWithLongKey = new HmacService(longKey);
        String message = "Test with long key";
        String signature = serviceWithLongKey.sign(message);

        assertNotNull(signature);
        assertTrue(Codec.isValidBase64Url(signature));
        assertTrue(serviceWithLongKey.verify(message, signature));
    }

    @Test
    @DisplayName("Проверка с однобайтовым секретным ключом")
    void testWithOneByteSecretKey() {
        byte[] singleByteKey = new byte[] { 0x7F };
        HmacService service = new HmacService(singleByteKey);

        String message = "Test";
        String signature = service.sign(message);

        assertNotNull(signature);
        assertTrue(service.verify(message, signature));
    }
}