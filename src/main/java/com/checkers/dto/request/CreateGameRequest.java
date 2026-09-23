package com.checkers.dto.request;

import com.checkers.model.enums.BotDifficulty;
import com.checkers.model.enums.GameMode;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGameRequest {

    @NotNull
    private GameMode mode;

    /** Required when {@code mode == BOT}. */
    private BotDifficulty botDifficulty;
}
