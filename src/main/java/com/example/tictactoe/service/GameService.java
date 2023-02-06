package com.example.tictactoe.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class GameService {

    private final ArrayList<Game> games = new ArrayList<>();

    @PostConstruct
    void init() {
        createGame();
    }

    public Game move(int id, Move move, Player player) {
        BoardMove boardMove = new BoardMove(move);
        Game game = getGame(id);
        if (game.getLast() == player)
            throw new PlayerNotCurrentException();

        return game.move(player, boardMove);
    }

    public Game getGame(int id) {
        try {
            return games.get(id);
        } catch (Exception e) {
            throw new GameNotFoundException();
        }
    }

    public int createGame() {
        games.add(new Game());
        return games.size() - 1;
    }

    public enum Player {
        ONE,
        TWO,
    }

    @Data
    public static class Game {
        static List<Map.Entry<Integer, Integer>> POSITIONS_TO_CHECK = List.of(
                Map.entry(0, 0),
                Map.entry(0, 1),
                Map.entry(0, 2),
                Map.entry(1, 0),
                Map.entry(2, 0)
        );

        static List<Map.Entry<Integer, Integer>> DIRECTIONS_TO_CHECK = List.of(
                Map.entry(1, 0),
                Map.entry(1, -1),
                Map.entry(0, -1),
                Map.entry(-1, -1),
                Map.entry(-1, 0),
                Map.entry(-1, 1),
                Map.entry(0, 1),
                Map.entry(1, 1)
        );

        Player[][] board = new Player[3][3];
        Player last = Player.TWO;
        boolean over;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Player getWinner() {
            if (!over) return null;
            return last;
        }

        @SuppressWarnings("ForLoopReplaceableByForEach")
        public long toLong() {
            ByteBuffer byteBuffer = ByteBuffer.allocate(8);
            byteBuffer.put((byte) (last == Player.ONE ? 1 : 2));
            byteBuffer.put((byte) (over ? 1 : 0));
            byteBuffer.putShort((short) 0);
            int boardData = 0;
            for (int i = 0; i < board.length; i++) {
                Player[] players = board[i];
                for (int j = 0; j < players.length; j++) {
                    Player player = players[j];
                    boardData |= (player == Player.ONE ? 1 : 2);
                    boardData <<= 2;
                }
            }
            byteBuffer.putInt(boardData);
            return byteBuffer.flip().getLong();
        }

        public Game fromLong(long data) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(8).putLong(data).flip();
            int boardData = byteBuffer.getInt();
            for (int j = board.length - 1; j >= 0; j--) {
                Player[] players = board[j];
                for (int i = players.length - 1; i >= 0; i--) {
                    players[i] = ((boardData & 0b11) == 1 ? Player.ONE : Player.TWO);
                    boardData >>= 2;
                }
            }
            byteBuffer.getShort();
            setOver(byteBuffer.get() == 1);
            setLast(byteBuffer.get() == 1 ? Player.ONE : Player.TWO);
            return this;
        }

        boolean canMove(BoardMove move) {
            return board[move.getRow()][move.getCol()] == null;
        }

        public Game move(Player player, BoardMove move) {
            if (canMove(move)) {
                board[move.getRow()][move.getCol()] = player;
            } else {
                throw new GameMoveNotValidException(player, Move.of(move));
            }

            if (won()) {
                this.over = true;
            }

            return this;
        }

        boolean won() {
            // loop over positions
            for (var position : POSITIONS_TO_CHECK) {
                Player player = board[position.getKey()][position.getValue()];

                if (player == null) continue;

                // loop over directions
                for (var direction : DIRECTIONS_TO_CHECK) {
                    boolean directionWins = false;

                    // loop over steps - should be line of length 3 to win
                    for (int i = 1; i < 2; i++) {
                        try {
                            if (board[direction.getKey() * i][direction.getValue() * i] != player) {
                                directionWins = true;
                                break;
                            }
                        } catch (ArrayIndexOutOfBoundsException ignored) {
                            continue;
                        }
                    }

                    // after you are done checking both "steps" feel free to report
                    if (directionWins) return true;
                }
            }

            // no win yet
            return false;
        }
    }

    static abstract class GameException extends RuntimeException {
        public GameException(String message) {
            super(message);
        }

        public GameException() {
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    static class GameMoveNotValidException extends GameException {

        public GameMoveNotValidException(Player player, Move move) {
            super("player %s can't move to %s".formatted(player, move));
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    static class PlayerNotCurrentException extends GameException {
        public PlayerNotCurrentException() {
        }
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    static class GameNotFoundException extends GameException {
        public GameNotFoundException() {
        }
    }

    /**
     * 1-indexed - for external api
     */
    @Accessors(chain = true)
    @Data
    public static class Move {
        @Min(1) @Max(3)
        Integer row;
        @Min(1) @Max(3)
        Integer col;

        static Move of(BoardMove boardMove) {
            return new Move()
                    .setRow(boardMove.getRow() + 1)
                    .setCol(boardMove.getCol() + 1);
        }
    }

    /**
     * like {@link Move} but 0-indexed
     */
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class BoardMove {
        @Min(1) @Max(3)
        Integer row;
        @Min(1) @Max(3)
        Integer col;

        public BoardMove(Move move) {
            row = move.getRow() - 1;
            col = move.getCol() - 1;
        }
    }
}
