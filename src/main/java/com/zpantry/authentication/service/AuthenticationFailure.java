package com.zpantry.authentication.service;

public final class AuthenticationFailure extends RuntimeException {
    public AuthenticationFailure(String message) { super(message); }
}
