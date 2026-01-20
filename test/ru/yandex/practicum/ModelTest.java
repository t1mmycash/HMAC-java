package ru.yandex.practicum;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import ru.yandex.practicum.model.*;

import static org.junit.jupiter.api.Assertions.*;

public class ModelTest {

    @Test
    @DisplayName("SignRequest: создание с конструктором")
    void testSignRequestConstructor() {
        SignRequest request = new SignRequest("Hello, World!");

        assertNotNull(request);
        assertEquals("Hello, World!", request.getMsg());
    }

    @Test
    @DisplayName("SignRequest: создание пустого объекта + сеттер")
    void testSignRequestSetter() {
        SignRequest request = new SignRequest();

        assertNotNull(request);
        assertNull(request.getMsg());

        request.setMsg("Test message");
        assertEquals("Test message", request.getMsg());
    }

    @Test
    @DisplayName("SignRequest: toString() безопасный вывод")
    void testSignRequestToString() {
        SignRequest request = new SignRequest("Secret message");
        String str = request.toString();

        assertNotNull(str);
        assertTrue(str.contains("SignRequest"));
        assertTrue(str.contains("msg="));
        assertTrue(str.contains("Secret message"));
    }

    @Test
    @DisplayName("SignRequest: toString() с null сообщением")
    void testSignRequestToStringWithNull() {
        SignRequest request = new SignRequest();
        String str = request.toString();

        assertNotNull(str);
        assertTrue(str.contains("null"));
    }

    @Test
    @DisplayName("SignResponse: создание с конструктором")
    void testSignResponseConstructor() {
        SignResponse response = new SignResponse("abc123DEF456");

        assertNotNull(response);
        assertEquals("abc123DEF456", response.getSignature());
    }

    @Test
    @DisplayName("SignResponse: сеттер")
    void testSignResponseSetter() {
        SignResponse response = new SignResponse();

        assertNotNull(response);
        assertNull(response.getSignature());

        response.setSignature("new_signature");
        assertEquals("new_signature", response.getSignature());
    }

    @Test
    @DisplayName("SignResponse: toString() обрезает подпись")
    void testSignResponseToString() {
        String longSignature = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        SignResponse response = new SignResponse(longSignature);
        String str = response.toString();

        assertNotNull(str);
        assertTrue(str.contains("SignResponse"));
        assertTrue(str.contains("signature="));
        assertTrue(str.contains("abcdefghij..."));
        assertFalse(str.contains(longSignature));
    }

    @Test
    @DisplayName("SignResponse: toString() с короткой подписью")
    void testSignResponseToStringShort() {
        SignResponse response = new SignResponse("short");
        String str = response.toString();

        assertTrue(str.contains("short..."));
    }

    @Test
    @DisplayName("SignResponse: toString() с null подписью")
    void testSignResponseToStringNull() {
        SignResponse response = new SignResponse();
        String str = response.toString();

        assertTrue(str.contains("null"));
    }

    @Test
    @DisplayName("VerifyRequest: создание с конструктором")
    void testVerifyRequestConstructor() {
        VerifyRequest request = new VerifyRequest("message", "signature123");

        assertNotNull(request);
        assertEquals("message", request.getMsg());
        assertEquals("signature123", request.getSignature());
    }

    @Test
    @DisplayName("VerifyRequest: сеттеры")
    void testVerifyRequestSetters() {
        VerifyRequest request = new VerifyRequest();

        assertNull(request.getMsg());
        assertNull(request.getSignature());

        request.setMsg("test msg");
        request.setSignature("test sig");

        assertEquals("test msg", request.getMsg());
        assertEquals("test sig", request.getSignature());
    }

    @Test
    @DisplayName("VerifyRequest: toString() обрезает подпись")
    void testVerifyRequestToString() {
        VerifyRequest request = new VerifyRequest(
                "Important message",
                "very_long_signature_abcdefghijklmnop"
        );
        String str = request.toString();

        assertTrue(str.contains("VerifyRequest"));
        assertTrue(str.contains("msg='Important message'"));
        assertTrue(str.contains("signature='very_long_..."));
        assertFalse(str.contains("very_long_signature_abcdefghijklmnop"));
    }

    @Test
    @DisplayName("VerifyResponse: создание с конструктором")
    void testVerifyResponseConstructor() {
        VerifyResponse response = new VerifyResponse(true);

        assertNotNull(response);
        assertTrue(response.isOk());
        assertTrue(response.getOk());
    }

    @Test
    @DisplayName("VerifyResponse: false значение")
    void testVerifyResponseFalse() {
        VerifyResponse response = new VerifyResponse(false);

        assertFalse(response.isOk());
        assertFalse(response.getOk());
    }

    @Test
    @DisplayName("VerifyResponse: сеттеры")
    void testVerifyResponseSetters() {
        VerifyResponse response = new VerifyResponse();

        assertFalse(response.isOk());

        response.setOk(true);
        assertTrue(response.isOk());

        response.setOk(false);
        assertFalse(response.isOk());
    }

    @Test
    @DisplayName("VerifyResponse: toString()")
    void testVerifyResponseToString() {
        VerifyResponse response1 = new VerifyResponse(true);
        VerifyResponse response2 = new VerifyResponse(false);

        assertTrue(response1.toString().contains("ok=true"));
        assertTrue(response2.toString().contains("ok=false"));
    }


    @Test
    @DisplayName("ErrorResponse: создание с конструктором")
    void testErrorResponseConstructor() {
        ErrorResponse response = new ErrorResponse("invalid_json");

        assertNotNull(response);
        assertEquals("invalid_json", response.getError());
    }

    @Test
    @DisplayName("ErrorResponse: сеттер")
    void testErrorResponseSetter() {
        ErrorResponse response = new ErrorResponse();

        assertNull(response.getError());

        response.setError("config_error");
        assertEquals("config_error", response.getError());
    }

    @Test
    @DisplayName("ErrorResponse: toString()")
    void testErrorResponseToString() {
        ErrorResponse response = new ErrorResponse("test_error");
        String str = response.toString();

        assertTrue(str.contains("ErrorResponse"));
        assertTrue(str.contains("error='test_error'"));
    }

    @Test
    @DisplayName("ErrorResponse: toString() с null ошибкой")
    void testErrorResponseToStringNull() {
        ErrorResponse response = new ErrorResponse();
        String str = response.toString();

        assertTrue(str.contains("null"));
    }

    @Test
    @DisplayName("Интеграция: полный сценарий подписи")
    void testFullSignScenario() {
        SignRequest signRequest = new SignRequest("Hello, HMAC!");

        String simulatedSignature = "abc123_" + System.currentTimeMillis();
        SignResponse signResponse = new SignResponse(simulatedSignature);

        VerifyRequest verifyRequest = new VerifyRequest(
                signRequest.getMsg(),
                signResponse.getSignature()
        );

        VerifyResponse verifyResponse = new VerifyResponse(true);

        assertEquals("Hello, HMAC!", signRequest.getMsg());
        assertEquals(simulatedSignature, signResponse.getSignature());
        assertEquals(signRequest.getMsg(), verifyRequest.getMsg());
        assertEquals(signResponse.getSignature(), verifyRequest.getSignature());
        assertTrue(verifyResponse.isOk());
    }

    @Test
    @DisplayName("Интеграция: сценарий с ошибкой")
    void testErrorScenario() {
        ErrorResponse error = new ErrorResponse("invalid_signature_format");

        assertEquals("invalid_signature_format", error.getError());
        assertTrue(error.toString().contains("invalid_signature_format"));
    }
}