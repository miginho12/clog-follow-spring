package com.clog.follow.follow;

import com.clog.follow.follow.exception.CannotFollowSelfException;
import com.clog.follow.follow.exception.InvalidFollowTransitionException;
import com.clog.follow.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * 팔로우 관계 (follower → following).
 *
 * <p>상태 전이 규칙은 이 엔티티가 소유한다. 서비스는 전이를 "요청"만 하고,
 * 허용되지 않은 전이는 엔티티가 예외로 막는다.
 */
@Entity
@Table(
        name = "follows",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_follows_follower_following",
                columnNames = {"follower_id", "following_id"}))
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 팔로우 하는 사람 (구독자). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id")
    private User follower;

    /** 팔로우 받는 사람 (대상). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "following_id")
    private User following;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FollowStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Follow() {
    }

    private Follow(User follower, User following, FollowStatus status) {
        this.follower = follower;
        this.following = following;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    /** 팔로우 요청 생성. 대상이 공개 계정이면 즉시 ACCEPTED, 비공개면 PENDING. */
    public static Follow request(User follower, User target) {
        if (follower.isSameUser(target)) {
            throw new CannotFollowSelfException();
        }
        FollowStatus initial = target.isPublic() ? FollowStatus.ACCEPTED : FollowStatus.PENDING;
        return new Follow(follower, target, initial);
    }

    /** PENDING → ACCEPTED. 트랜잭션 안에서 호출하면 dirty checking 으로 UPDATE 된다. */
    public void accept() {
        if (status != FollowStatus.PENDING) {
            throw new InvalidFollowTransitionException(status, FollowStatus.ACCEPTED);
        }
        this.status = FollowStatus.ACCEPTED;
    }

    public boolean isPending() {
        return status == FollowStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public User getFollower() {
        return follower;
    }

    public User getFollowing() {
        return following;
    }

    public FollowStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
