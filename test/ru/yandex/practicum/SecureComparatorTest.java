package ru.yandex.practicum;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import ru.yandex.practicum.crypto.Codec;
import ru.yandex.practicum.crypto.SecureComparator;

import static org.junit.jupiter.api.Assertions.*;

public class SecureComparatorTest {

    @Test
    @DisplayName("Сравнение одинаковых массивов")
    void testEqualArrays() {
        byte[] arr1 = {1, 2, 3, 4, 5};
        byte[] arr2 = {1, 2, 3, 4, 5};

        assertTrue(SecureComparator.constantTimeEquals(arr1, arr2));
    }

    @Test
    @DisplayName("Сравнение разных массивов (первый байт отличается)")
    void testDifferentFirstByte() {
        byte[] arr1 = {1, 2, 3, 4, 5};
        byte[] arr2 = {9, 2, 3, 4, 5};

        assertFalse(SecureComparator.constantTimeEquals(arr1, arr2));
    }

    @Test
    @DisplayName("Сравнение разных массивов (последний байт отличается)")
    void testDifferentLastByte() {
        byte[] arr1 = {1, 2, 3, 4, 5};
        byte[] arr2 = {1, 2, 3, 4, 9};

        assertFalse(SecureComparator.constantTimeEquals(arr1, arr2));
    }

    @Test
    @DisplayName("Сравнение массивов разной длины")
    void testDifferentLength() {
        byte[] arr1 = {1, 2, 3};
        byte[] arr2 = {1, 2, 3, 4};

        assertFalse(SecureComparator.constantTimeEquals(arr1, arr2));
    }

    @Test
    @DisplayName("Сравнение пустых массивов")
    void testEmptyArrays() {
        byte[] empty1 = new byte[0];
        byte[] empty2 = new byte[0];

        assertTrue(SecureComparator.constantTimeEquals(empty1, empty2));
    }

    @Test
    @DisplayName("Сравнение длинных массивов")
    void testLongArrays() {
        byte[] long1 = new byte[1024];
        byte[] long2 = new byte[1024];

        for (int i = 0; i < 1024; i++) {
            long1[i] = (byte) (i % 256);
            long2[i] = (byte) (i % 256);
        }

        assertTrue(SecureComparator.constantTimeEquals(long1, long2));

        long2[500] = 99;
        assertFalse(SecureComparator.constantTimeEquals(long1, long2));
    }

    @Test
    @DisplayName("Сравнение строк")
    void testStringComparison() {
        String s1 = "Hello, HMAC!";
        String s2 = "Hello, HMAC!";
        String s3 = "Hello, HMAC?";

        assertTrue(SecureComparator.constantTimeEquals(s1, s2));
        assertFalse(SecureComparator.constantTimeEquals(s1, s3));
    }

    @Test
    @DisplayName("Строки с null")
    void testStringWithNull() {
        String s = "test";

        assertFalse(SecureComparator.constantTimeEquals(s, null));
        assertFalse(SecureComparator.constantTimeEquals(null, s));
    }

    @Test
    @DisplayName("Демонстрация timing-безопасности")
    void testTimingSafety() {
        byte[] secret = "my_secret_key".getBytes();

        byte[] guess1 = "xy_secret_key".getBytes();  // Первый байт отличается

        byte[] guess2 = "myXsecret_key".getBytes();  // Второй байт отличается

        assertFalse(SecureComparator.constantTimeEquals(secret, guess1));
        assertFalse(SecureComparator.constantTimeEquals(secret, guess2));
    }

    @Test
    @DisplayName("Использование в контексте HMAC проверки")
    void testHmacContext() {
        byte[] realSignature = Codec.decodeFromBase64Url("SGVsbG8");  // "Hello"
        byte[] fakeSignature = Codec.decodeFromBase64Url("SGVsbG9f"); // "Hello_"

        byte[] expectedSignature = Codec.decodeFromBase64Url("SGVsbG8");

        assertTrue(SecureComparator.constantTimeEquals(realSignature, expectedSignature));
        assertFalse(SecureComparator.constantTimeEquals(fakeSignature, expectedSignature));
    }
}