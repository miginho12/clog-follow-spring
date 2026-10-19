package com.clog.follow.follow.exception;

import com.clog.follow.follow.FollowStatus;

public class InvalidFollowTransitionException extends RuntimeException {

    public InvalidFollowTransitionException(FollowStatus from, FollowStatus to) {
        super("허용되지 않은 상태 전이: " + from + " -> " + to);
    }
}
