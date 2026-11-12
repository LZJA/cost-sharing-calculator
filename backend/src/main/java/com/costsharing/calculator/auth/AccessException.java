package com.costsharing.calculator.auth;

import lombok.Getter;
@Getter
public class AccessException extends RuntimeException {
    private final int status;
    public AccessException(int status, String message) { super(message); this.status = status; }
}
