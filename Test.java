import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Test extends Bot {
    class Player {
        String name;
        HashSet<String> possibilities;

        Player(String name, String[] possible) {
            this.name = name;
            this.possibilities = new HashSet<>();
            for (String guestname : possible) {
                possibilities.add(guestname);
            }
        }
    }

    class Piece {
        Room room;
        String pieceName;

        Piece(String name) {
            this.pieceName = name;
        }
    }

    class Room {
        public ArrayList<Piece> pieces;
        public int[] gems;
        public int col;
        public int row;

        Room(int col, int row, String gems, String pieces) {
            this.gems = new int[3];
        }
    }

    class Board {
        Room[][] rooms;

        Board(String players, String gems) {
            this.rooms = new Room[3][4]; // Row is horizontal, col is vertical.
        }

        public ArrayList<Piece> piecesInRoom(int col, int row) {
            return this.rooms[col][row].pieces;
        }

        public ArrayList<Piece> piecesInRow(int row) {
            ArrayList<Piece> rval = new ArrayList<>();
            for (int i = 0; i < 4; i++)
                rval.addAll(this.rooms[row][i].pieces);
            return rval;
        }

        public ArrayList<Piece> piecesThatCanSeePiece(Piece p) {
            ArrayList<Piece> visiblePieces = new ArrayList<>();
            visiblePieces.addAll(this.piecesInCol(p.room.col));
            visiblePieces.addAll(this.piecesInRow(p.room.row));
            return visiblePieces;
        }

        public ArrayList<Piece> piecesInCol(int col) {
            ArrayList<Piece> rval = new ArrayList<>();
            for (int i = 0; i < 3; i++)
                rval.addAll(this.rooms[i][col].pieces);
            return rval;
        }

        public ArrayList<Piece> piecesThatCanTakeGem(String gem) {
            ArrayList<Piece> rval = new ArrayList<>();

            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 4; j++) {
                    Room r = this.rooms[i][j];
                    if (gem == "red") {
                        if (r.gems[0] > 0) {
                            rval.addAll(r.pieces);
                        }
                    } else if (gem == "blue") {
                        if (r.gems[1] > 0) {
                            rval.addAll(r.pieces);
                        }
                    } else if (gem == "green") {
                        if (r.gems[2] > 0) {
                            rval.addAll(r.pieces);
                        }
                    }
                }
            }

            return rval;
        }

    }

    Board board;
    String guestName;
    String playerName;
    HashMap<Integer, Integer> gems;

    public Test(String playerName, String guestName, int numStartingGems, String gemLocations, String[] playerNames,
            String[] guestNames) {
        super(playerName, guestName, numStartingGems, gemLocations, playerNames, guestNames);
    }

    @Override
    public String getPlayerActions(String d1, String d2, String card1, String card2, String board)
            throws Suspicion.BadActionException {
        throw new UnsupportedOperationException("Unimplemented method 'getPlayerActions'");
    }

    @Override
    public void reportPlayerActions(String player, String d1, String d2, String cardPlayed, String board,
            String actions) {
        // The player played these actions.
        // On the board, check if the piece was heavily occluded or slighly occluded.
        // If it was heavily occluded and they moved it to less occluded, the probability of them being it is low.
        // Otherwise, they might be that piece (moved from low occlusion to high occlusion).
    }

    @Override
    public void answerAsk(String guest, String player, String board, boolean canSee) {
    }

    @Override
    public void answerViewDeck(String player) {
    }

    @Override
    public String reportGuesses() {
        String x = "";
        return x;
    }

}
