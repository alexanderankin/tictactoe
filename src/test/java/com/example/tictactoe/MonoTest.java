package com.example.tictactoe;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

public class MonoTest {
    public static void main(String[] args) {
        AtomicReference<Long> atomicLong = new AtomicReference<>(1L);
        Mono.delay(Duration.ofSeconds(3)).doOnNext(l -> atomicLong.set(0L)).subscribe();
        Long block = Mono.delay(Duration.ofSeconds(1))
                .repeat()
                .filter(e -> atomicLong.get().equals(e))
                .next()
                .timeout(Duration.ofSeconds(2))
                .block();
        System.out.println(block);
    }
}
