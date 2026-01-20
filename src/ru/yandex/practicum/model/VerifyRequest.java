package ru.yandex.practicum.model;

public class VerifyRequest {
    private String msg;
    private String signature;

    public VerifyRequest() {
    }

    public VerifyRequest(String msg, String signature) {
        this.msg = msg;
        this.signature = signature;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public String toString() {
        String msgPreview = msg != null ? msg : "null";
        String sigPreview = "null";

        if (signature != null && !signature.isEmpty()) {
            int previewLength = Math.min(10, signature.length());
            sigPreview = signature.substring(0, previewLength) + "...";
        }

        return "VerifyRequest{" +
                "msg='" + msgPreview + "', " +
                "signature='" + sigPreview + "'" +
                '}';
    }
}
