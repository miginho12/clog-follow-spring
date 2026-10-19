package com.clog.follow.follow;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clog.follow.user.User;
import com.clog.follow.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** HTTP 레벨 시나리오 + 예외 → 상태코드 매핑 검증. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FollowControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository userRepository;

    Long requester;
    Long owner;

    @BeforeEach
    void setUp() {
        requester = userRepository.save(new User("requester", true)).getId();
        owner = userRepository.save(new User("owner", false)).getId();
    }

    @Test
    @DisplayName("비공개 계정: 요청 → 요청 목록 확인 → 수락 → 팔로워 1")
    void approvalFlow() throws Exception {
        mvc.perform(post("/users/{id}/follow", owner).header("X-User-Id", requester))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followStatus").value("PENDING"))
                .andExpect(jsonPath("$.followerCount").value(0));

        mvc.perform(get("/users/me/follow-requests").header("X-User-Id", owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(requester));

        mvc.perform(post("/users/{id}/follow-request/accept", requester).header("X-User-Id", owner))
                .andExpect(status().isNoContent());

        mvc.perform(post("/users/{id}/follow", owner).header("X-User-Id", requester))
                .andExpect(jsonPath("$.followStatus").value("ACCEPTED"))
                .andExpect(jsonPath("$.followerCount").value(1));
    }

    @Test
    @DisplayName("요청 없이 수락 → 404 ProblemDetail")
    void acceptWithoutRequest() throws Exception {
        mvc.perform(post("/users/{id}/follow-request/accept", requester).header("X-User-Id", owner))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("자기 자신 팔로우 → 400")
    void selfFollow() throws Exception {
        mvc.perform(post("/users/{id}/follow", requester).header("X-User-Id", requester))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("거절 후 다시 거절 → 404")
    void rejectTwice() throws Exception {
        mvc.perform(post("/users/{id}/follow", owner).header("X-User-Id", requester));

        mvc.perform(post("/users/{id}/follow-request/reject", requester).header("X-User-Id", owner))
                .andExpect(status().isNoContent());
        mvc.perform(post("/users/{id}/follow-request/reject", requester).header("X-User-Id", owner))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("언팔로우는 관계가 없어도 204")
    void unfollowIdempotent() throws Exception {
        mvc.perform(delete("/users/{id}/follow", owner).header("X-User-Id", requester))
                .andExpect(status().isNoContent());
    }
}
