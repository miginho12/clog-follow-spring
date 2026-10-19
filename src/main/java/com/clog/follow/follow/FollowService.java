package com.clog.follow.follow;

import com.clog.follow.follow.exception.CannotFollowSelfException;
import com.clog.follow.follow.exception.FollowRequestNotFoundException;
import com.clog.follow.follow.exception.UserNotFoundException;
import com.clog.follow.user.User;
import com.clog.follow.user.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public FollowService(FollowRepository followRepository, UserRepository userRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    /** 팔로우 (idempotent). 이미 관계가 있으면 기존 상태를 그대로 반환. */
    @Transactional
    public FollowStatus follow(Long followerId, Long targetId) {
        if (followerId.equals(targetId)) {
            throw new CannotFollowSelfException();
        }
        return followRepository.findByFollowerIdAndFollowingId(followerId, targetId)
                .map(Follow::getStatus)
                .orElseGet(() -> {
                    Follow follow = Follow.request(getUser(followerId), getUser(targetId));
                    return followRepository.save(follow).getStatus();
                });
    }

    /** 언팔로우 (idempotent). PENDING 요청 취소도 같은 경로. */
    @Transactional
    public void unfollow(Long followerId, Long targetId) {
        followRepository.findByFollowerIdAndFollowingId(followerId, targetId)
                .ifPresent(followRepository::delete);
    }

    /** owner 가 requester 의 PENDING 요청을 수락. save() 호출 없이 dirty checking 으로 반영. */
    @Transactional
    public void accept(Long ownerId, Long requesterId) {
        getPendingRequest(ownerId, requesterId).accept();
    }

    /** owner 가 requester 의 PENDING 요청을 거절 (row 삭제). */
    @Transactional
    public void reject(Long ownerId, Long requesterId) {
        followRepository.delete(getPendingRequest(ownerId, requesterId));
    }

    /** 내 팔로워 끊어내기 (idempotent). unfollow 와 방향만 반대. */
    @Transactional
    public void removeFollower(Long ownerId, Long followerId) {
        unfollow(followerId, ownerId);
    }

    public long countFollowers(Long userId) {
        return followRepository.countByFollowingIdAndStatus(userId, FollowStatus.ACCEPTED);
    }

    public List<User> pendingRequests(Long ownerId) {
        return followRepository.findFollowersByStatus(ownerId, FollowStatus.PENDING);
    }

    private Follow getPendingRequest(Long ownerId, Long requesterId) {
        return followRepository.findByFollowerIdAndFollowingId(requesterId, ownerId)
                .filter(Follow::isPending)
                .orElseThrow(() -> new FollowRequestNotFoundException(requesterId));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
