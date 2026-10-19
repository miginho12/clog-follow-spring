package com.clog.follow.follow.exception;

public class CannotFollowSelfException extends RuntimeException {

    public CannotFollowSelfException() {
        super("자기 자신은 팔로우할 수 없습니다");
    }
}
