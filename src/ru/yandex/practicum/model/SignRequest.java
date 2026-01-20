package ru.yandex.practicum.model;

public class SignRequest {
    private String msg;

    public SignRequest() {
    }

    public SignRequest(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "SignRequest{" +
                "msg='" + (msg != null ? msg : "null") + "'" +
                '}';
    }
}