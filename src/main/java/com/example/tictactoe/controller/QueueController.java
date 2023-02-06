package com.example.tictactoe.controller;

import com.example.tictactoe.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/queue")
public class QueueController {
    private final QueueService queueService;

    @GetMapping
    Flux<ServerSentEvent<?>> getInLine() {
        UUID id = UUID.randomUUID();
        return Flux.concat(
                Mono.just(ServerSentEvent.builder(Map.of("id", (Object) id)).event("id").build()),
                queueService.match(id).map(this::toMatchSse)
        );
    }

    private <T> ServerSentEvent<T> toMatchSse(T m) {
        return ServerSentEvent.builder(m).event("gameId").build();
    }

    record Match(int gameId, GameService.Player player) {
    }

    @RequiredArgsConstructor
    @Service
    static class QueueService {
        final GameService gameService;
        final QueueRepository queueRepository;

        private static Throwable mapTimeoutException(TimeoutException t) {
            return new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "please try again - no matches available");
        }

        public Mono<Match> match(UUID id) {
            return queueRepository.first()
                    .flatMap(this::setupMatch)
                    .map(i -> new Match(i, GameService.Player.TWO))
                    .switchIfEmpty(waitForMatch(id)
                            .map(i -> new Match(i, GameService.Player.ONE)));
        }

        private Mono<Integer> waitForMatch(UUID id) {
            return queueRepository.getInLine(id)
                    .then(Mono.delay(Duration.ofSeconds(1)).repeat()
                            .flatMap(l -> queueRepository.findMatch(id))
                            .next()
                            .timeout(Duration.ofDays(30))
                            .onErrorMap(TimeoutException.class, QueueService::mapTimeoutException)
                    );
        }

        private Mono<Integer> setupMatch(UUID uuid) {
            int game = gameService.createGame();

            return queueRepository.saveMatch(uuid, game)
                    .thenReturn(game);
        }
    }

    @Service
    static class QueueRepository {
        private final LinkedList<UUID> list = new LinkedList<>();
        private final LinkedList<Map.Entry<UUID, Integer>> matches = new LinkedList<>();

        /**
         * see who is in line
         */
        public synchronized Mono<UUID> first() {
            if (list.isEmpty()) return Mono.empty();
            return Mono.just(list.pop());
        }

        /**
         * set up a game if someone is in line
         */
        public synchronized Mono<Void> saveMatch(UUID uuid, int game) {
            matches.push(Map.entry(uuid, game));
            return Mono.empty();
        }

        /**
         * get in line if we can't set up a game (no one is in line)
         */
        public Mono<Void> getInLine(UUID id) {
            list.push(id);
            return Mono.empty();
        }

        /**
         * poll for if someone shows up later
         */
        public Mono<Integer> findMatch(UUID id) {
            var e = doFindMatch(id);
            if (e != null) {
                matches.remove(e);
                return Mono.just(e.getValue());
            }

            return Mono.empty();
        }

        private Map.Entry<UUID, Integer> doFindMatch(UUID id) {
            for (Map.Entry<UUID, Integer> match : matches) {
                if (match.getKey().equals(id)) {
                    return match;
                }
            }
            return null;
        }
    }
}
