package ru.yandex.practicum.model;

public class SignResponse {
    private String signature;

    public SignResponse() {
    }

    public SignResponse(String signature) {
        this.signature = signature;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public String toString() {
        String sigPreview = "null";
        if (signature != null && !signature.isEmpty()) {
            int previewLength = Math.min(10, signature.length());
            sigPreview = signature.substring(0, previewLength) + "...";
        }

        return "SignResponse{" +
                "signature='" + sigPreview + "'" +
                '}';
    }
}
