package com.bridgeflow.api.common;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Email hoặc mật khẩu không đúng.");
    }
}
