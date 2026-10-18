package com.checkers.controller;

import com.checkers.model.entity.Game;
import com.checkers.model.enums.GameMode;
import com.checkers.model.enums.GameStatus;
import com.checkers.repository.GameRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.jpa.show-sql=false", "logging.level.com.checkers=INFO"})
@AutoConfigureMockMvc
class SiteExperienceTest {
    @Autowired MockMvc mvc;
    @Autowired GameRepository games;

    @Test
    void developmentPagesAndSitemapCannotBeIndexed() throws Exception {
        mvc.perform(get("/").param("lang", "ru"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Robots-Tag", "noindex, follow"))
                .andExpect(content().string(containsString("content=\"noindex,follow\"")));
        mvc.perform(get("/robots.txt"))
                .andExpect(content().string("User-agent: *\nDisallow: /\n"));
        mvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<loc>"))));
    }

    @Test
    void rendersEveryScreenInAllThreeLanguages() throws Exception {
        for (String lang : new String[]{"en", "ru", "hy"}) {
            for (String path : new String[]{"/", "/play", "/play?id=1", "/tasks", "/rating", "/history", "/faq", "/login", "/register"}) {
                mvc.perform(get(path).param("lang", lang))
                        .andExpect(status().isOk())
                        .andExpect(content().string(containsString("lang=\"" + lang + "\"")))
                        .andExpect(content().string(not(containsString("??"))))
                        .andExpect(content().string(containsString("/css/theme.css")));
            }
        }
    }

    @Test
    @Transactional
    void publicStatsCountCompletedGamesOnceAndPresenceDeduplicatesCookies() throws Exception {
        long before = games.countByStatus(GameStatus.FINISHED);
        games.save(Game.builder().mode(GameMode.BOT).status(GameStatus.FINISHED).build());
        games.save(Game.builder().mode(GameMode.FRIEND).status(GameStatus.WAITING).build());
        games.save(Game.builder().mode(GameMode.MATCHMAKING).status(GameStatus.IN_PROGRESS).build());
        games.save(Game.builder().mode(GameMode.FRIEND).status(GameStatus.ABORTED).build());
        Cookie visitor = new Cookie("checkers_visitor", "00000000-0000-0000-0000-000000000001");
        mvc.perform(post("/api/site/presence").cookie(visitor)).andExpect(status().isNoContent());
        mvc.perform(post("/api/site/presence").cookie(visitor)).andExpect(status().isNoContent());
        mvc.perform(get("/api/site/stats"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.online").value(1))
                .andExpect(jsonPath("$.completedGames").value(before + 1));
        mvc.perform(post("/api/site/presence"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().httpOnly("checkers_visitor", true))
                .andExpect(cookie().sameSite("checkers_visitor", "Lax"));
    }

    @Test
    void dailyPuzzleIsPublicAndDoesNotExposeASolutionOrChangeGameTotals() throws Exception {
        long before = games.countByStatus(GameStatus.FINISHED);
        mvc.perform(get("/api/tasks/daily"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.maxPlayerMoves", isIn(new Integer[]{3, 4})))
                .andExpect(jsonPath("$.legalMoves", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.playerMoves", empty()))
                .andExpect(jsonPath("$.solution").doesNotExist())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        org.junit.jupiter.api.Assertions.assertEquals(before, games.countByStatus(GameStatus.FINISHED));
    }

    @Test
    void puzzleRequestsAreValidatedAndUnknownPuzzlesReturn404() throws Exception {
        for (String body : new String[]{"{}", "{\"moves\":[]}", "{\"moves\":[null]}",
                "{\"moves\":[[\"z9\",\"b2\"]]}", "{\"moves\":[[null,\"b2\"]]}"}) {
            mvc.perform(post("/api/tasks/2026-10-18-tactic-01/moves")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
        }
        mvc.perform(post("/api/tasks/2026-10-18-does-not-exist/moves")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"moves\":[[\"a1\",\"b2\"]]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("TASK_NOT_FOUND"));
    }
}
