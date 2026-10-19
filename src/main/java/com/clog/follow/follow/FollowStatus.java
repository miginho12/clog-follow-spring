package com.clog.follow.follow;

/**
 * 팔로우 상태.
 *
 * <pre>
 * (없음) --follow(공개)--> ACCEPTED
 * (없음) --follow(비공개)--> PENDING --accept--> ACCEPTED
 *                            PENDING --reject--> (삭제)
 * PENDING | ACCEPTED --unfollow / removeFollower--> (삭제)
 * </pre>
 */
public enum FollowStatus {
    PENDING,
    ACCEPTED
}
