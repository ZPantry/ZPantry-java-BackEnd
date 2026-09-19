package com.zpantry.user.service;

public final class OwnerAuthorizationException extends RuntimeException {
    public OwnerAuthorizationException() { super("Bạn chỉ được phép cập nhật thông tin của chính tài khoản mình."); }
}
