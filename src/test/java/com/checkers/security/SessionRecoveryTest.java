package com.checkers.security;

import com.checkers.model.entity.User;
import com.checkers.repository.UserRepository;
import com.checkers.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.jpa.show-sql=false", "logging.level.com.checkers=INFO"})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
class SessionRecoveryTest {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @Autowired UserRepository users;

    @Test
    void missingAccountRequiresLoginWhilePublicRequestsAndRefreshStillWork() throws Exception {
        String header = "Bearer " + jwt.createAccessToken(User.builder()
                .id(Long.MAX_VALUE).username("previous_guest").guest(true).build());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, header))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
        mvc.perform(get("/api/site/stats").header(HttpHeaders.AUTHORIZATION, header))
                .andExpect(status().isOk());
        mvc.perform(post("/api/site/presence").header(HttpHeaders.AUTHORIZATION, header))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/refresh").header(HttpHeaders.AUTHORIZATION, header)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"old-session\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void reusedDatabaseIdCannotAuthenticateThePreviousAccount() throws Exception {
        User current = users.save(User.builder().username("new_guest").guest(true).build());
        String stale = jwt.createAccessToken(User.builder()
                .id(current.getId()).username("previous_guest").guest(true).build());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + stale))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.createAccessToken(current)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new_guest"));
    }

    @Test
    void anonymousAndMalformedSessionsReturn401OnProtectedRequests() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }
}
