import java.util.*;

public class State {
    public Random r = new Random();
    public HashMap<String, Piece> pieces; // Keyed off of guest name
    public Board board;
    public Piece me;
    public String pieceToMove;
    public HashMap<String, Player> players; // Keyed off of player name
    public String otherPlayerNames[];
    public TextDisplay display;
    public int[] gemCounts = new int[] {0, 0, 0};
    public String gemLocations;

    public static class Board
    {
        public Room rooms[][];
        public String gemLocations;

        public class Room
        {
            public final boolean gems[] = new boolean[3];
            public final String[] availableGems;
            public final int row;
            public final int col;
            private HashMap<String, Piece> pieces;

            public void removePlayer(Piece piece)
            {
                removePlayer(piece.name);
                piece.col=-1;
                piece.row=-1;
            }

            public void removePlayer(String name)
            {
                pieces.remove(name);
            }

            public void addPlayer(Piece piece)
            {
                piece.col=this.col;
                piece.row=this.row;
                pieces.put(piece.name, piece);
            }

            public Room(boolean red, boolean green, boolean yellow, int row, int col)
            {
                pieces = new HashMap<String, Piece>();
                this.row = row;
                this.col = col;
                gems[Suspicion.RED]=red;
                gems[Suspicion.GREEN]=green;
                gems[Suspicion.YELLOW]=yellow;
                String temp="";
                if(red) temp += "red,";
                if(green) temp += "green,";
                if(yellow) temp += "yellow,";
                availableGems = (temp.substring(0,temp.length()-1)).split(",");
            }
        }

        public void movePlayer(Piece player, int row, int col)
        {
            rooms[player.row][player.col].removePlayer(player);
            rooms[row][col].addPlayer(player);
        }

        public void clearRooms()
        {
            rooms=new Room[3][4];
            int x=0, y=0;
            boolean red, green, yellow;

            for(String gems:gemLocations.trim().split(":"))
            {
                if(gems.contains("red")) red=true;
                else red=false;
                if(gems.contains("green")) green=true;
                else green=false;
                if(gems.contains("yellow")) yellow=true;
                else yellow=false;
                rooms[x][y] = new Room(red,green,yellow,x,y);
                y++;
                x += y/4;
                y %= 4;
            }
        }

        public Board(String piecePositions, HashMap<String, Piece> pieces, String gemLocations)
        {
            Piece piece;
            this.gemLocations=gemLocations;
            clearRooms();
            int col=0;
            int row=0;
            for(String room:piecePositions.split(":",-1)) // Split out each room
            {
                room = room.trim();
                if(room.length()!=0) for(String guest: room.split(",")) // Split guests out of each room
                {
                    guest = guest.trim();
                    piece = pieces.get(guest);
                    rooms[row][col].addPlayer(piece);
                }
                col++;
                row = row + col/4;
                col = col%4;
            }
        }
    }

    public Piece getPiece(String name)
    {
        return pieces.get(name);
    }

    public static class Player
    {
        public String playerName;
        public ArrayList<String> possibleGuestNames;

        public void adjustKnowledge(ArrayList<String> possibleGuests)
        {
            Iterator<String> it = possibleGuestNames.iterator();
            while(it.hasNext())
            {
                String g;
                if(!possibleGuests.contains(g=it.next()))
                {
                    it.remove();
                }
            }
        }

        public void adjustKnowledge(String notPossibleGuest)
        {
            Iterator<String> it = possibleGuestNames.iterator();
            while(it.hasNext())
            {
                if(it.next().equals(notPossibleGuest))
                {
                    it.remove();
                    break;
                }
            }
        }

        public Player(String name, String[] guests)
        {
            playerName = name;
            possibleGuestNames = new ArrayList<String>();
            for(String g: guests)
            {
                possibleGuestNames.add(g);
            }
        }
    }

    public static class Piece
    {
        public int row, col;
        public String name;

        public Piece(String name)
        {
            this.name = name;
        }
    }

    public String[] getPossibleMoves(Piece p)
    {
        LinkedList<String> moves=new LinkedList<String>();
        if (p == null) {
            System.out.println("ERROR: Piece is null!");
        }

        if(p.row > 0) moves.push((p.row-1) + "," + p.col);
        if(p.row < 2) moves.push((p.row+1) + "," + p.col);
        if(p.col > 0) moves.push((p.row) + "," + (p.col-1));
        if(p.col < 3) moves.push((p.row) + "," + (p.col+1));

        return moves.toArray(new String[moves.size()]);
    }


    // Pick a player to answer the question if they have seen an "other" guest.
    public String pickPlayerToQuestion(String otherGuest) {
        System.out.println("@SandBot: ---------------- pick player to question ------------------");
        // Make a list of viable players.
        HashMap<String, ArrayList<Boolean>> possiblePlayerAnswers = new HashMap<String, ArrayList<Boolean>>();

        for (String p: this.players.keySet()) {
            ArrayList<String> possibleGuestNames = players.get(p).possibleGuestNames;
            ArrayList<Boolean> canSeeGuest = new ArrayList<Boolean>();

            for (String guest: possibleGuestNames){
                Piece p1 = pieces.get(guest);
                Piece other = pieces.get(otherGuest);
                canSeeGuest.add(canSee(p1, other));
            }

            possiblePlayerAnswers.put(p, canSeeGuest);
        }

        // calculate the entropy of each set - select the one with the highest entropy.
        float highestEntropy = -10000;
        String selectedPlayer = null;

        // Print the possible values for the given keyset.
        for (String player_name: possiblePlayerAnswers.keySet()) {
            System.out.print("@SandBot: "+ player_name + " ");
            for (boolean answer: possiblePlayerAnswers.get(player_name)) {
                System.out.print( " -> "+ answer);
            }
            System.out.println();
        }
        // --- End of Print section ---

        for (String p: possiblePlayerAnswers.keySet()) {
            HashMap<Boolean, Integer> check = new HashMap<Boolean, Integer>();
            int total = possiblePlayerAnswers.get(p).size();

            for (boolean b: possiblePlayerAnswers.get(p)) {
                int temp = check.get(b) == null ? 0 : check.get(b);
                temp += 1;
                check.put(b, temp);
            }

            float entropy = 0;
            for (boolean b: check.keySet()) {
                System.out.println("@SandBot: " + check.get(b) + "  " + total);
                float probability = (float) check.get(b) / (float) total;
                entropy -= probability * (Math.log(probability) / Math.log(2));
            }
            System.out.println("@SandBot: Entropy for " + p + " is " + entropy);
            if (entropy > highestEntropy) {
                highestEntropy = entropy;
                selectedPlayer = p;
            }
        }

        System.out.println("@SandBot: Selected " + selectedPlayer + " to ask about " + otherGuest);
        return selectedPlayer;
    }

    private int countGems(String gem)
    {
        if(gem.equals("red")) return gemCounts[Suspicion.RED];
        else if(gem.equals("green")) return gemCounts[Suspicion.GREEN];
        else return gemCounts[Suspicion.YELLOW];
    }

    public ArrayList<String> getGuestsInRoomWithGem(String board, String gemcolor)
    {
        Board b = new Board(board, pieces, gemLocations);
        int gem=-1;
        if(gemcolor.equals("yellow")) gem = Suspicion.YELLOW;
        else if(gemcolor.equals("green")) gem = Suspicion.GREEN;
        else if(gemcolor.equals("red")) gem = Suspicion.RED;
        ArrayList<String> possibleGuests = new ArrayList<String>();

        int y=0,x=0;
        for(String guests: board.trim().split(":"))
        {
            //only get people from rooms with the gem
            if(b.rooms[y][x].gems[gem] && guests.trim().length()>0)
            {
                for(String guest:guests.trim().split(","))
                {
                    possibleGuests.add(guest.trim());
                }
            }
            x++;
            y+=x/4;
            x%=4;
        }

        return possibleGuests;
    }

    public boolean canSee(Piece p1, Piece p2) // returns whether or not these two pieces see each
    {
        return (p1.row==p2.row || p1.col == p2.col);
    }


    // Get the possible gems this player can take.
    public String[] getPossibleGemsList() {
        return this.board.rooms[me.row][me.col].availableGems;
    }

    public String gemFromId(int gemID) {
        if (gemID == Suspicion.RED) return "red";
        else if (gemID == Suspicion.GREEN) return "green";
        else if (gemID == Suspicion.YELLOW) return "yellow";
        else {
            System.out.println("Invalid GEM ID");
            return "";
        }
    }

    public int idFromGem(String gem) {
        switch (gem.toLowerCase()) {
            case "red":
                return Suspicion.RED;
            case "green":
                return Suspicion.GREEN;
            case "yellow":
                return Suspicion.YELLOW;
            default:
                System.out.println("Invalid GEM");
                return -1; // or throw an exception, depending on your requirements
        }
    }

    public String bestGemToGet(String board)
    {
        System.out.println("@SandBot: ---------------------- picking a gem to pick -------------------");
        int[] current = this.gemCounts.clone();
        // Get possible gems this player can take.
        String[] available = getPossibleGemsList();
        int min_gem = 1000;
        for (String gem: available) {
            if (this.gemCounts[idFromGem(gem)] < min_gem) min_gem = this.gemCounts[idFromGem(gem)];
        }

        // Get all gems which have min gem count.
        int num_players = -100;
        String selected_gem = null;
        for (String gem: available) {
            if (this.gemCounts[idFromGem(gem)] == min_gem) {
                System.out.println("@SandBot: Option: "+ gem + " " + this.gemCounts[idFromGem(gem)] + " " + min_gem);
                if (getGuestsInRoomWithGem(board, gem).size() >= num_players)  {
                    num_players = getGuestsInRoomWithGem(board, gem).size();
                    selected_gem = gem;
                }
            }
        }
        System.out.println("@SandBot: Selected: " + selected_gem);

        return selected_gem;
    }

    public void gen_combinations(int slotId, ArrayList<String> currentCombination, ArrayList<ArrayList<String>> valid_values, ArrayList<ArrayList<String>> tableToFill) {
        if (slotId >= valid_values.size()) {
            ArrayList<String> add = new ArrayList<String>(currentCombination);
            tableToFill.add(add);
        } else {
            ArrayList<String> possibleGuestNames = valid_values.get(slotId);
            for (String value: possibleGuestNames) {
                if (!currentCombination.contains(value)) {
                    currentCombination.add(value); // Add this option to the list.
                    gen_combinations(slotId + 1, currentCombination, valid_values, tableToFill);
                    currentCombination.remove(currentCombination.size() - 1);
                }
            }
        }
    }
}
