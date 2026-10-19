package com.clog.follow.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nickname;

    /** 공개 계정이면 팔로우 즉시 수락, 비공개면 승인 대기. */
    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    protected User() {
    }

    public User(String nickname, boolean isPublic) {
        this.nickname = nickname;
        this.isPublic = isPublic;
    }

    public boolean isSameUser(User other) {
        return this == other || (id != null && id.equals(other.id));
    }

    public Long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean isPublic() {
        return isPublic;
    }
}
