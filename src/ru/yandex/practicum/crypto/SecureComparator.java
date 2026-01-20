package ru.yandex.practicum.crypto;

public class SecureComparator {

    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) {
            return false;
        }

        if (a.length != b.length) {
            return false;
        }

        int result = 0;

        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    public static boolean constantTimeEquals(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return false;
        }

        byte[] bytes1 = s1.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] bytes2 = s2.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        return constantTimeEquals(bytes1, bytes2);
    }
}
