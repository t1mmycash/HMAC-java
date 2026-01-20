package ru.yandex.practicum.model;

public class VerifyResponse {
    private boolean ok;

    public VerifyResponse() {
    }

    public VerifyResponse(boolean ok) {
        this.ok = ok;
    }

    public boolean getOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public boolean isOk() {
        return ok;
    }

    @Override
    public String toString() {
        return "VerifyResponse{" +
                "ok=" + ok +
                '}';
    }
}
