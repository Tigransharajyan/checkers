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

/** Live visitors on this application instance; completed games across the database. */
@Service
public class SiteStatsService {
    static final Duration PRESENCE_TTL = Duration.ofSeconds(45);
    private final GameRepository games;
    private final Clock clock;
    private final Map<String, Visit> visits = new HashMap<>();

    @Autowired
    public SiteStatsService(GameRepository games) {
        this(games, Clock.systemUTC());
    }

    SiteStatsService(GameRepository games, Clock clock) {
        this.games = games;
        this.clock = clock;
    }

    public synchronized void heartbeat(String visitorId, Long userId) {
        removeExpired();
        visits.put(visitorId, new Visit(userId == null ? "visitor:" + visitorId : "user:" + userId,
                clock.instant()));
    }

    public Stats stats() {
        return new Stats(onlineCount(), games.countByStatus(GameStatus.FINISHED));
    }

    private synchronized long onlineCount() {
        removeExpired();
        return visits.values().stream().map(Visit::identity).distinct().count();
    }

    private void removeExpired() {
        Instant cutoff = clock.instant().minus(PRESENCE_TTL);
        visits.values().removeIf(visit -> !visit.lastSeen().isAfter(cutoff));
    }

    private record Visit(String identity, Instant lastSeen) {}
    public record Stats(long online, long completedGames) {}
}
