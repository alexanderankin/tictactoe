package com.example.tictactoe.controller;

import com.example.tictactoe.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("/api/game")
public class GameController {
    private final GameService gameService;

    // @PostMapping
    // @ResponseStatus(HttpStatus.CREATED)
    // Map<String, Integer> startGame() {
    //     return Map.of("id", gameService.createGame());
    // }

    // POST /game/{id}?player=ONE {rol:, col:}
    @PostMapping("/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    GameService.Game move(@PathVariable int id,
                          @Valid @RequestBody GameService.Move move,
                          @RequestParam GameService.Player player) {
        return gameService.move(id, move, player);
    }

    // GET /game/{id}
    @GetMapping("/{id}")
    GameService.Game get(@PathVariable int id) {
        return gameService.getGame(id);
    }

    // GET /game/{id}/events
    @GetMapping("/{id}/events")
    Flux<ServerSentEvent<GameService.Game>> move(@PathVariable int id) {
        AtomicReference<Long> last = new AtomicReference<>(gameService.getGame(id).toLong());
        return Mono.delay(Duration.ofSeconds(1))
                .repeat()
                .flatMap(l -> {
                    GameService.Game game = gameService.getGame(id);
                    long next = game.toLong();

                    if (next == last.get()) {
                        return Mono.empty();
                    } else {
                        last.set(next);
                    }

                    return Mono.just(ServerSentEvent.<GameService.Game>builder().event("game").data(game).build());
                });
    }
}
