package com.clog.follow.follow;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증은 학습 범위 밖이라 X-User-Id 헤더를 현재 사용자로 간주한다.
 */
@RestController
@RequestMapping("/users")
public class FollowController {

    private static final String USER_HEADER = "X-User-Id";

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    public record FollowResponse(FollowStatus followStatus, long followerCount) {
    }

    public record UserItem(Long id, String nickname) {
    }

    @PostMapping("/{userId}/follow")
    public FollowResponse follow(@RequestHeader(USER_HEADER) Long me, @PathVariable Long userId) {
        FollowStatus status = followService.follow(me, userId);
        return new FollowResponse(status, followService.countFollowers(userId));
    }

    @DeleteMapping("/{userId}/follow")
    public ResponseEntity<Void> unfollow(@RequestHeader(USER_HEADER) Long me, @PathVariable Long userId) {
        followService.unfollow(me, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/follow-requests")
    public List<UserItem> myFollowRequests(@RequestHeader(USER_HEADER) Long me) {
        return followService.pendingRequests(me).stream()
                .map(u -> new UserItem(u.getId(), u.getNickname()))
                .toList();
    }

    @PostMapping("/{requesterId}/follow-request/accept")
    public ResponseEntity<Void> accept(@RequestHeader(USER_HEADER) Long me, @PathVariable Long requesterId) {
        followService.accept(me, requesterId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{requesterId}/follow-request/reject")
    public ResponseEntity<Void> reject(@RequestHeader(USER_HEADER) Long me, @PathVariable Long requesterId) {
        followService.reject(me, requesterId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{followerId}/follower")
    public ResponseEntity<Void> removeFollower(@RequestHeader(USER_HEADER) Long me, @PathVariable Long followerId) {
        followService.removeFollower(me, followerId);
        return ResponseEntity.noContent().build();
    }
}
