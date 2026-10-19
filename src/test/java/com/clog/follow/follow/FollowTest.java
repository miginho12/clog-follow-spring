package com.clog.follow.follow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.clog.follow.follow.exception.CannotFollowSelfException;
import com.clog.follow.follow.exception.InvalidFollowTransitionException;
import com.clog.follow.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 순수 도메인 단위 테스트 — 스프링/DB 없이 상태 전이 규칙만 검증. */
class FollowTest {

    private final User me = new User("me", true);
    private final User publicUser = new User("public", true);
    private final User privateUser = new User("private", false);

    @Test
    @DisplayName("공개 계정 팔로우는 즉시 ACCEPTED")
    void requestToPublicAccount() {
        Follow follow = Follow.request(me, publicUser);

        assertThat(follow.getStatus()).isEqualTo(FollowStatus.ACCEPTED);
    }

    @Test
    @DisplayName("비공개 계정 팔로우는 PENDING")
    void requestToPrivateAccount() {
        Follow follow = Follow.request(me, privateUser);

        assertThat(follow.getStatus()).isEqualTo(FollowStatus.PENDING);
        assertThat(follow.isPending()).isTrue();
    }

    @Test
    @DisplayName("자기 자신은 팔로우할 수 없다")
    void cannotFollowSelf() {
        assertThatThrownBy(() -> Follow.request(me, me))
                .isInstanceOf(CannotFollowSelfException.class);
    }

    @Test
    @DisplayName("PENDING -> accept -> ACCEPTED")
    void acceptPending() {
        Follow follow = Follow.request(me, privateUser);

        follow.accept();

        assertThat(follow.getStatus()).isEqualTo(FollowStatus.ACCEPTED);
    }

    @Test
    @DisplayName("ACCEPTED 는 다시 accept 할 수 없다")
    void cannotAcceptTwice() {
        Follow follow = Follow.request(me, publicUser);

        assertThatThrownBy(follow::accept)
                .isInstanceOf(InvalidFollowTransitionException.class)
                .hasMessageContaining("ACCEPTED -> ACCEPTED");
    }
}
