package com.checkers.service;

import com.checkers.model.enums.GameStatus;
import com.checkers.repository.GameRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class SiteStatsServiceTest {
    private final GameRepository games = mock(GameRepository.class);
    private final Clock clock = mock(Clock.class);
    private final Instant now = Instant.parse("2026-10-18T10:00:00Z");

    @Test
    void countsDistinctPeopleAndUsesCompletedDatabaseGames() {
        when(clock.instant()).thenReturn(now);
        when(games.countByStatus(GameStatus.FINISHED)).thenReturn(127L);
        SiteStatsService service = new SiteStatsService(games, clock);
        service.heartbeat("tab-a", 7L);
        service.heartbeat("tab-b", 7L);
        service.heartbeat("anonymous", null);
        service.heartbeat("anonymous", null);
        assertEquals(new SiteStatsService.Stats(2, 127), service.stats());
        verify(games).countByStatus(GameStatus.FINISHED);
    }

    @Test
    void expiresAbsentVisitorsAndReplacesAnonymousIdentityOnLogin() {
        when(clock.instant()).thenReturn(now);
        SiteStatsService service = new SiteStatsService(games, clock);
        service.heartbeat("browser", null);
        service.heartbeat("browser", 4L);
        service.heartbeat("other-browser", 4L);
        assertEquals(1, service.stats().online());
        when(clock.instant()).thenReturn(now.plusSeconds(45));
        assertEquals(0, service.stats().online());
        service.heartbeat("browser", 4L);
        assertEquals(1, service.stats().online());
    }

    @Test
    void newSiteHasZeroActivity() {
        SiteStatsService service = new SiteStatsService(games, Clock.fixed(now, ZoneOffset.UTC));
        assertEquals(new SiteStatsService.Stats(0, 0), service.stats());
    }
}
