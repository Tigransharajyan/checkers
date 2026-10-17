package com.checkers.service;

import com.checkers.dto.response.GameResponse;
import com.checkers.exception.BusinessException;
import com.checkers.exception.ErrorCode;
import com.checkers.model.entity.Game;
import com.checkers.model.entity.GameInvite;
import com.checkers.model.entity.User;
import com.checkers.model.enums.GameMode;
import com.checkers.model.enums.GameStatus;
import com.checkers.model.enums.InviteStatus;
import com.checkers.repository.GameInviteRepository;
import com.checkers.repository.GameRepository;
import com.checkers.repository.MoveRepository;
import com.checkers.security.SecurityUser;
import com.checkers.websocket.GameEventPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InviteServiceTest {

    @Mock
    private GameInviteRepository gameInviteRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private MoveRepository moveRepository;

    @Mock
    private UserService userService;

    @Mock
    private GameEventPublisher eventPublisher;

    private InviteService inviteService;

    @BeforeEach
    void setUp() {
        inviteService = new InviteService(gameInviteRepository, gameRepository, moveRepository, userService, eventPublisher);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(Long id, String username) {
        User user = User.builder().id(id).username(username).guest(false).build();
        SecurityUser securityUser = new SecurityUser(user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void acceptInvite_creatorReopensInvite_returnsGameIdempotently() {
        authenticateUser(100L, "creatorAlice");

        User creator = User.builder().id(100L).username("creatorAlice").build();
        when(userService.requireEntity(100L)).thenReturn(creator);

        Game game = Game.builder()
                .id(1L)
                .mode(GameMode.FRIEND)
                .status(GameStatus.WAITING)
                .whitePlayer(creator)
                .blackPlayer(null)
                .build();

        GameInvite invite = GameInvite.builder()
                .code("INVITE12")
                .game(game)
                .creator(creator)
                .status(InviteStatus.PENDING)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();

        when(gameInviteRepository.findByCode("INVITE12")).thenReturn(Optional.of(invite));
        when(moveRepository.findByGameIdOrderByMoveNumberAsc(1L)).thenReturn(List.of());

        GameResponse response = inviteService.acceptInvite("INVITE12");

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("creatorAlice", response.getWhitePlayer().getUsername());
    }

    @Test
    void acceptInvite_joinedBlackPlayerRefreshes_returnsGameIdempotently() {
        authenticateUser(200L, "joinerBob");

        User creator = User.builder().id(100L).username("creatorAlice").build();
        User joiner = User.builder().id(200L).username("joinerBob").build();
        when(userService.requireEntity(200L)).thenReturn(joiner);

        Game game = Game.builder()
                .id(2L)
                .mode(GameMode.FRIEND)
                .status(GameStatus.IN_PROGRESS)
                .whitePlayer(creator)
                .blackPlayer(joiner)
                .build();

        GameInvite invite = GameInvite.builder()
                .code("INVITE34")
                .game(game)
                .creator(creator)
                .status(InviteStatus.ACCEPTED)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();

        when(gameInviteRepository.findByCode("INVITE34")).thenReturn(Optional.of(invite));
        when(moveRepository.findByGameIdOrderByMoveNumberAsc(2L)).thenReturn(List.of());

        GameResponse response = inviteService.acceptInvite("INVITE34");

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals("joinerBob", response.getBlackPlayer().getUsername());
    }

    @Test
    void acceptInvite_thirdPartyAttemptsToJoinAcceptedInvite_throwsInviteAlreadyUsed() {
        authenticateUser(300L, "strangerCharlie");

        User creator = User.builder().id(100L).username("creatorAlice").build();
        User joiner = User.builder().id(200L).username("joinerBob").build();
        User stranger = User.builder().id(300L).username("strangerCharlie").build();
        when(userService.requireEntity(300L)).thenReturn(stranger);

        Game game = Game.builder()
                .id(3L)
                .mode(GameMode.FRIEND)
                .status(GameStatus.IN_PROGRESS)
                .whitePlayer(creator)
                .blackPlayer(joiner)
                .build();

        GameInvite invite = GameInvite.builder()
                .code("INVITE56")
                .game(game)
                .creator(creator)
                .status(InviteStatus.ACCEPTED)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();

        when(gameInviteRepository.findByCode("INVITE56")).thenReturn(Optional.of(invite));

        BusinessException ex = assertThrows(BusinessException.class, () -> inviteService.acceptInvite("INVITE56"));
        assertEquals(ErrorCode.INVITE_ALREADY_USED, ex.getErrorCode());
    }
}
