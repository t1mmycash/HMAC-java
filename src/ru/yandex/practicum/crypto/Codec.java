package ru.yandex.practicum.crypto;

import java.util.Base64;
import java.util.regex.Pattern;

public class Codec {

    private static final Pattern BASE64URL_PATTERN = Pattern.compile("^[A-Za-z0-9_-]*$");

    public static String encodeToBase64Url(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(data);
    }

    public static byte[] decodeFromBase64Url(String base64Url) {
        if (base64Url == null) {
            throw new IllegalArgumentException("Base64url string cannot be null");
        }
        if (base64Url.isEmpty()) {
            throw new IllegalArgumentException("Base64url string cannot be empty");
        }

        try {
            return Base64.getUrlDecoder().decode(base64Url);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid base64url format: " + base64Url,
                    e
            );
        }
    }

    public static boolean isValidBase64Url(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        if (!BASE64URL_PATTERN.matcher(input).matches()) {
            return false;
        }

        try {
            decodeFromBase64Url(input);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean isValidHmacSignatureLength(String base64Url) {
        if (!isValidBase64Url(base64Url)) {
            return false;
        }

        try {
            byte[] decoded = decodeFromBase64Url(base64Url);
            return decoded.length == 32;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}