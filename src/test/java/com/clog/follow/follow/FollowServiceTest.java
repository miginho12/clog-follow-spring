package com.clog.follow.follow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.clog.follow.follow.exception.CannotFollowSelfException;
import com.clog.follow.follow.exception.FollowRequestNotFoundException;
import com.clog.follow.follow.exception.UserNotFoundException;
import com.clog.follow.user.User;
import com.clog.follow.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** 서비스 + JPA 통합 테스트 (H2). 각 테스트는 롤백된다. */
@SpringBootTest
@Transactional
class FollowServiceTest {

    @Autowired
    FollowService followService;

    @Autowired
    FollowRepository followRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EntityManager em;

    Long requester;
    Long privateOwner;
    Long publicOwner;

    @BeforeEach
    void setUp() {
        requester = userRepository.save(new User("requester", true)).getId();
        privateOwner = userRepository.save(new User("privateOwner", false)).getId();
        publicOwner = userRepository.save(new User("publicOwner", true)).getId();
    }

    /** 1차 캐시를 비워 다음 조회가 실제 DB 에서 오도록 한다. */
    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    private FollowStatus statusInDb(Long followerId, Long followingId) {
        return followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                .map(Follow::getStatus)
                .orElse(null);
    }

    @Test
    @DisplayName("공개 계정 팔로우 → ACCEPTED, 팔로워 수 증가")
    void followPublic() {
        FollowStatus status = followService.follow(requester, publicOwner);

        assertThat(status).isEqualTo(FollowStatus.ACCEPTED);
        assertThat(followService.countFollowers(publicOwner)).isEqualTo(1);
    }

    @Test
    @DisplayName("비공개 계정 팔로우 → PENDING, 팔로워 수에는 미포함, 요청 목록에 노출")
    void followPrivate() {
        FollowStatus status = followService.follow(requester, privateOwner);

        assertThat(status).isEqualTo(FollowStatus.PENDING);
        assertThat(followService.countFollowers(privateOwner)).isZero();
        assertThat(followService.pendingRequests(privateOwner))
                .extracting(User::getId)
                .containsExactly(requester);
    }

    @Test
    @DisplayName("이미 팔로우 중이면 새 row 없이 기존 상태 반환 (idempotent)")
    void followIsIdempotent() {
        followService.follow(requester, privateOwner);

        FollowStatus again = followService.follow(requester, privateOwner);

        assertThat(again).isEqualTo(FollowStatus.PENDING);
        assertThat(followRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("수락하면 save() 없이도 dirty checking 으로 DB 에 ACCEPTED 반영")
    void acceptPersistsViaDirtyChecking() {
        followService.follow(requester, privateOwner);
        flushAndClear();

        followService.accept(privateOwner, requester);
        flushAndClear();

        assertThat(statusInDb(requester, privateOwner)).isEqualTo(FollowStatus.ACCEPTED);
        assertThat(followService.countFollowers(privateOwner)).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 수락된 요청을 다시 수락하면 요청 없음 예외")
    void acceptTwiceFails() {
        followService.follow(requester, privateOwner);
        followService.accept(privateOwner, requester);

        assertThatThrownBy(() -> followService.accept(privateOwner, requester))
                .isInstanceOf(FollowRequestNotFoundException.class);
    }

    @Test
    @DisplayName("요청이 없는데 수락하면 예외")
    void acceptWithoutRequestFails() {
        assertThatThrownBy(() -> followService.accept(privateOwner, requester))
                .isInstanceOf(FollowRequestNotFoundException.class);
    }

    @Test
    @DisplayName("거절하면 PENDING row 삭제")
    void reject() {
        followService.follow(requester, privateOwner);

        followService.reject(privateOwner, requester);
        flushAndClear();

        assertThat(statusInDb(requester, privateOwner)).isNull();
    }

    @Test
    @DisplayName("ACCEPTED 관계는 거절할 수 없다 (끊으려면 removeFollower)")
    void rejectAcceptedFails() {
        followService.follow(requester, publicOwner);

        assertThatThrownBy(() -> followService.reject(publicOwner, requester))
                .isInstanceOf(FollowRequestNotFoundException.class);
        assertThat(statusInDb(requester, publicOwner)).isEqualTo(FollowStatus.ACCEPTED);
    }

    @Test
    @DisplayName("언팔로우는 PENDING 요청 취소도 처리하고, 관계가 없어도 예외 없음")
    void unfollowIsIdempotent() {
        followService.follow(requester, privateOwner);

        followService.unfollow(requester, privateOwner);
        followService.unfollow(requester, privateOwner);
        flushAndClear();

        assertThat(statusInDb(requester, privateOwner)).isNull();
    }

    @Test
    @DisplayName("owner 가 팔로워를 끊어낼 수 있다")
    void removeFollower() {
        followService.follow(requester, publicOwner);

        followService.removeFollower(publicOwner, requester);
        flushAndClear();

        assertThat(statusInDb(requester, publicOwner)).isNull();
    }

    @Test
    @DisplayName("자기 자신 팔로우 거부")
    void cannotFollowSelf() {
        assertThatThrownBy(() -> followService.follow(requester, requester))
                .isInstanceOf(CannotFollowSelfException.class);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 팔로우 거부")
    void targetNotFound() {
        assertThatThrownBy(() -> followService.follow(requester, 999_999L))
                .isInstanceOf(UserNotFoundException.class);
    }
}
