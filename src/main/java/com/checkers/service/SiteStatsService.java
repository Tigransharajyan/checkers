package com.checkers.service;

import com.checkers.model.enums.GameStatus;
import com.checkers.repository.GameRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Keeps recent visitor activity in this instance's memory and reads completed-game totals from the
 * database. Reports observed activity rather than fabricated counts.
 */
@Service
public class SiteStatsService {
    static final Duration PRESENCE_TTL = Duration.ofSeconds(45);
    private final GameRepository games;
    private final Clock clock;
    private final Map<String, Visit> visits = new HashMap<>();

    /**
     * Creates statistics with a game repository and clock. The production constructor uses UTC; the
     * test overload accepts a controllable Clock.
     */
    @Autowired
    public SiteStatsService(GameRepository games) {
        this(games, Clock.systemUTC());
    }

    /**
     * Creates statistics with a game repository and clock. The production constructor uses UTC; the
     * test overload accepts a controllable Clock.
     */
    SiteStatsService(GameRepository games, Clock clock) {
        this.games = games;
        this.clock = clock;
    }

    /**
     * Removes expired visits and updates cookie-based activity under synchronization. Authenticated
     * browser tabs share the identity user:id.
     */
    public synchronized void heartbeat(String visitorId, Long userId) {
        removeExpired();
        visits.put(visitorId, new Visit(userId == null ? "visitor:" + visitorId : "user:" + userId,
                clock.instant()));
    }

    /**
     * Returns active visitors on this application instance and completed games counted in the
     * database.
     */
    public Stats stats() {
        return new Stats(onlineCount(), games.countByStatus(GameStatus.FINISHED));
    }

    /**
     * Counts distinct live identities after expiring inactive visits. Multiple tabs belonging to
     * one user do not inflate the total.
     */
    private synchronized long onlineCount() {
        removeExpired();
        return visits.values().stream().map(Visit::identity).distinct().count();
    }

    /**
     * Removes visits with no heartbeat during the last forty-five seconds.
     */
    private void removeExpired() {
        Instant cutoff = clock.instant().minus(PRESENCE_TTL);
        visits.values().removeIf(visit -> !visit.lastSeen().isAfter(cutoff));
    }

    /**
     * Internal activity record for a visitor cookie, containing its deduplicated identity and last
     * heartbeat time.
     */
    private record Visit(String identity, Instant lastSeen) {}
    /**
     * Immutable public totals: currently active visitors and completed games.
     */
    public record Stats(long online, long completedGames) {}
}
