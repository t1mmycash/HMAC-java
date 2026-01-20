package ru.yandex.practicum;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import ru.yandex.practicum.crypto.Codec;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;

public class CodecTest {

    @Test
    @DisplayName("Кодирование и декодирование обычного текста")
    void testEncodeDecode() {
        byte[] original = "Test message for HMAC".getBytes(StandardCharsets.UTF_8);

        String encoded = Codec.encodeToBase64Url(original);
        assertNotNull(encoded);
        assertFalse(encoded.contains("+"));
        assertFalse(encoded.contains("/"));
        assertFalse(encoded.contains("="));

        byte[] decoded = Codec.decodeFromBase64Url(encoded);
        assertArrayEquals(original, decoded);
    }

    @Test
    @DisplayName("Кодирование пустого массива")
    void testEncodeEmpty() {
        byte[] empty = new byte[0];
        String encoded = Codec.encodeToBase64Url(empty);

        assertNotNull(encoded);
        assertTrue(encoded.isEmpty());

        assertFalse(Codec.isValidBase64Url(encoded));
    }

    @Test
    @DisplayName("Кодирование null должно бросать исключение")
    void testEncodeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Codec.encodeToBase64Url(null));
    }

    @Test
    @DisplayName("Декодирование null должно бросать исключение")
    void testDecodeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Codec.decodeFromBase64Url(null));
    }

    @Test
    @DisplayName("Декодирование пустой строки должно бросать исключение")
    void testDecodeEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> Codec.decodeFromBase64Url(""));
    }

    @Test
    @DisplayName("Проверка валидных base64url строк (из задания)")
    void testIsValidBase64UrlValid() {
        assertTrue(Codec.isValidBase64Url("SGVsbG8"));  // "Hello"
        assertTrue(Codec.isValidBase64Url("YWJjMTIz"));  // "abc123"

        assertTrue(Codec.isValidBase64Url("abc-def_123"));

        String validHmac = "AABlc2RAZ21haWwuY29tOjE3MDc0MjczNzg1Nzc6NjI";
        assertTrue(Codec.isValidBase64Url(validHmac));
    }

    @Test
    @DisplayName("Проверка невалидных base64url строк (из задания)")
    void testIsValidBase64UrlInvalid() {
        // Запрещенные символы
        assertFalse(Codec.isValidBase64Url("abc+def"));
        assertFalse(Codec.isValidBase64Url("abc/def"));
        assertFalse(Codec.isValidBase64Url("abc=def"));
        assertFalse(Codec.isValidBase64Url("abc def"));
        assertFalse(Codec.isValidBase64Url("abc@def"));

        assertFalse(Codec.isValidBase64Url(null));
        assertFalse(Codec.isValidBase64Url(""));
    }

    @Test
    @DisplayName("Короткие строки с валидными символами")
    void testShortValidStrings() {
        assertFalse(Codec.isValidBase64Url("a"));
        try {
            byte[] result = java.util.Base64.getUrlDecoder().decode("ab");
            assertTrue(Codec.isValidBase64Url("ab"));
        } catch (IllegalArgumentException e) {
            assertFalse(Codec.isValidBase64Url("ab"));
        }

        try {
            byte[] result = java.util.Base64.getUrlDecoder().decode("abc");
            assertTrue(Codec.isValidBase64Url("abc"));
        } catch (IllegalArgumentException e) {
            assertFalse(Codec.isValidBase64Url("abc"));
        }

        assertTrue(Codec.isValidBase64Url("abcd"));
    }

    @Test
    @DisplayName("Декодирование невалидной строки должно бросать исключение")
    void testDecodeInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> Codec.decodeFromBase64Url("@@@"));

        assertThrows(IllegalArgumentException.class,
                () -> Codec.decodeFromBase64Url("abc def"));

        assertThrows(IllegalArgumentException.class,
                () -> Codec.decodeFromBase64Url("абв"));

        assertThrows(IllegalArgumentException.class,
                () -> Codec.decodeFromBase64Url("abc@def"));

        assertThrows(IllegalArgumentException.class,
                () -> Codec.decodeFromBase64Url("abc(def"));
    }

    @Test
    @DisplayName("Проверка длины подписи HMAC-SHA256")
    void testIsValidHmacSignatureLength() {

        byte[] hmacSha256Output = new byte[32];
        for (int i = 0; i < 32; i++) {
            hmacSha256Output[i] = (byte) i;
        }

        String encoded32Bytes = Codec.encodeToBase64Url(hmacSha256Output);
        assertEquals(43, encoded32Bytes.length());
        assertTrue(Codec.isValidHmacSignatureLength(encoded32Bytes));

        byte[] wrongLength = new byte[31];
        String encoded31Bytes = Codec.encodeToBase64Url(wrongLength);
        assertFalse(Codec.isValidHmacSignatureLength(encoded31Bytes));

        byte[] longer = new byte[33];
        String encoded33Bytes = Codec.encodeToBase64Url(longer);
        assertFalse(Codec.isValidHmacSignatureLength(encoded33Bytes));
    }

    @Test
    @DisplayName("Символьная проверка base64url")
    void testBase64UrlAllCharacters() {
        String allChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
        assertTrue(Codec.isValidBase64Url(allChars));

        assertTrue(Codec.isValidBase64Url("ABCDEFGH"));      // заглавные
        assertTrue(Codec.isValidBase64Url("ijklmnop"));      // строчные
        assertTrue(Codec.isValidBase64Url("01234567"));      // цифры
        assertTrue(Codec.isValidBase64Url("-_"));            // специальные
    }

    @Test
    @DisplayName("Работа с кириллицей через UTF-8")
    void testCyrillicText() {
        String cyrillicText = "Привет, мир!";
        byte[] bytes = cyrillicText.getBytes(StandardCharsets.UTF_8);

        String encoded = Codec.encodeToBase64Url(bytes);
        assertTrue(Codec.isValidBase64Url(encoded));

        byte[] decoded = Codec.decodeFromBase64Url(encoded);
        String decodedText = new String(decoded, StandardCharsets.UTF_8);

        assertEquals(cyrillicText, decodedText);
    }

    @Test
    @DisplayName("Специальные символы в тексте")
    void testSpecialCharacters() {
        String text = "Line1\nLine2\tTab!@#$%^&*()";
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        String encoded = Codec.encodeToBase64Url(bytes);
        assertTrue(Codec.isValidBase64Url(encoded));

        byte[] decoded = Codec.decodeFromBase64Url(encoded);
        String decodedText = new String(decoded, StandardCharsets.UTF_8);

        assertEquals(text, decodedText);
    }

    @Test
    @DisplayName("Пограничный случай: строка с валидными символами, но некорректная base64")
    void testBorderlineCases() {

        String notBase64 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

        try {
            java.util.Base64.getUrlDecoder().decode(notBase64);
            assertTrue(Codec.isValidBase64Url(notBase64));
        } catch (IllegalArgumentException e) {
            assertFalse(Codec.isValidBase64Url(notBase64));
        }
    }

    @Test
    @DisplayName("Интеграционный тест: полный цикл как в задании")
    void testFullCycle() {
        String[] testMessages = {
                "hello",
                "Test message",
                "12345",
                "Special chars: !@#$%^&*()",
                "Кириллица",
                "A".repeat(1000),
        };

        for (String message : testMessages) {
            byte[] original = message.getBytes(StandardCharsets.UTF_8);

            String encoded = Codec.encodeToBase64Url(original);
            assertNotNull(encoded);

            if (!encoded.isEmpty()) {
                assertTrue(Codec.isValidBase64Url(encoded));
            }

            byte[] decoded = Codec.decodeFromBase64Url(encoded);

            assertArrayEquals(original, decoded,
                    "Failed for message: '" + message + "'");
        }
    }

    @Test
    @DisplayName("Тест из задания: base64url без паддинга")
    void testWithoutPadding() {
        byte[] testData = "Hello".getBytes(StandardCharsets.UTF_8);

        String encoded = java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(testData);

        String ourEncoded = Codec.encodeToBase64Url(testData);
        assertEquals(encoded, ourEncoded);

        assertFalse(encoded.contains("="));
        assertFalse(ourEncoded.contains("="));
    }
}