package com.clog.follow.follow;

import com.clog.follow.user.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    long countByFollowingIdAndStatus(Long followingId, FollowStatus status);

    @Query("""
            select f.follower from Follow f
            where f.following.id = :userId and f.status = :status
            order by f.createdAt desc
            """)
    List<User> findFollowersByStatus(@Param("userId") Long userId, @Param("status") FollowStatus status);
}
