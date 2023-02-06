package com.example.tictactoe.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GameServiceTest {

    @Nested
    class GameTest {
        GameService.Game game;

        @BeforeEach
        void setUp() {
            game = new GameService.Game();
        }

        @Test
        void test() {
            game.setBoard(new GameService.Player[][] {
                    {null, null, null, },
                    {null, null, null, },
                    {null, null, null, },
            });

            Assertions.assertTrue(game.canMove(new GameService.BoardMove(1, 2)));
        }

        @Test
        void test1() {
            game.setBoard(new GameService.Player[][] {
                    {null, null, null, },
                    {null, null, null, },
                    {null, null, null, },
            });

            Assertions.assertTrue(game.canMove(new GameService.BoardMove(1, 2)));
        }
    }

}
