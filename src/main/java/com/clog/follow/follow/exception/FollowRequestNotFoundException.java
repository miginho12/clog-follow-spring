package com.clog.follow.follow.exception;

public class FollowRequestNotFoundException extends RuntimeException {

    public FollowRequestNotFoundException(Long requesterId) {
        super("팔로우 요청을 찾을 수 없습니다: " + requesterId);
    }
}
