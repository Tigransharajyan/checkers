package com.checkers.dto.request;

import com.checkers.model.enums.BotDifficulty;
import com.checkers.model.enums.GameMode;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Incoming game-creation DTO containing mode and bot difficulty. GameService requires difficulty
 * for bot mode; Lombok generates accessors.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGameRequest {

    @NotNull
    private GameMode mode;

    /** Difficulty used when mode is BOT. */
    private BotDifficulty botDifficulty;
}
