import java.util.*;

public class SandBotGemLogicPly extends Bot
{
    Random r = new Random();
    HashMap<String, Piece> pieces; // Keyed off of guest name
    Board board;
    Piece me;
    HashMap<String, Player> players; // Keyed off of player name
    String otherPlayerNames[];
    TextDisplay display;

    int[] gemCounts = new int[3];

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

    public class Player
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

    public class Piece
    {
        public int row, col;
        public String name;

        public Piece(String name)
        {
            this.name = name;
        }
    }

    private String[] getPossibleMoves(Piece p)
    {
        LinkedList<String> moves=new LinkedList<String>();
        if(p.row > 0) moves.push((p.row-1) + "," + p.col);
        if(p.row < 2) moves.push((p.row+1) + "," + p.col);
        if(p.col > 0) moves.push((p.row) + "," + (p.col-1));
        if(p.col < 3) moves.push((p.row) + "," + (p.col+1));

        return moves.toArray(new String[moves.size()]);
    }


    public String getPlayerActions(String d1, String d2, String card1, String card2, String board) throws Suspicion.BadActionException
    {
        this.board = new Board(board, pieces, gemLocations);
        String actions = "";

        // Random move for dice1
        if(d1.equals("?")) d1 = guestNames[r.nextInt(guestNames.length)];
        Piece piece = pieces.get(d1);
        String[] moves = getPossibleMoves(piece);
        int movei = r.nextInt(moves.length);
        actions += "move," + d1 + "," + moves[movei];
        this.board.movePlayer(piece, Integer.parseInt(moves[movei].split(",")[0]), Integer.parseInt(moves[movei].split(",")[1])); // Perform the move on my board

        // Random move for dice2
        if(d2.equals("?")) d2 = guestNames[r.nextInt(guestNames.length)];
        piece = pieces.get(d2);
        moves = getPossibleMoves(piece);
        movei = r.nextInt(moves.length);
        actions += ":move," + d2 + "," + moves[movei];
        this.board.movePlayer(piece, Integer.parseInt(moves[movei].split(",")[0]), Integer.parseInt(moves[movei].split(",")[1])); // Perform the move on my board

        // which card
        int i = r.nextInt(2);
        actions += ":play,card"+(i+1);

        String card = i==0?card1:card2;


        for(String cardAction: card.split(":")) // just go ahead and do them in this order
        {
            if(cardAction.startsWith("move")) 
            {
                String guest;
                guest = guestNames[r.nextInt(guestNames.length)];
                piece = pieces.get(guest);
                //moves = getPossibleMoves(piece);
                actions += ":move," + guest + "," + r.nextInt(3) + "," + r.nextInt(4);
            }
            else if(cardAction.startsWith("viewDeck")) 
            {
                actions += ":viewDeck";
            }
            else if(cardAction.startsWith("get")) 
            {
                String gemToGrab;
                int count;
                if(cardAction.equals("get,")) 
                {
                    // Grab a random gem
                    gemToGrab = bestGemToGet(board);
                    actions += ":get," + gemToGrab;
                }
                else 
                {
                    actions += ":" + cardAction;
                    gemToGrab=cardAction.trim().split(",")[1];
                }
                if(gemToGrab.equals("red")) gemCounts[Suspicion.RED]++;
                else if(gemToGrab.equals("green")) gemCounts[Suspicion.GREEN]++;
                else gemCounts[Suspicion.YELLOW]++;
            }
            else if(cardAction.startsWith("ask")) 
            {
                // Ask a random player
                actions += ":" + cardAction + otherPlayerNames[r.nextInt(otherPlayerNames.length)]; 
            }
        }
        return actions;
    }

    private int countGems(String gem)
    {
        if(gem.equals("red")) return gemCounts[Suspicion.RED];
        else if(gem.equals("green")) return gemCounts[Suspicion.GREEN];
        else return gemCounts[Suspicion.YELLOW];
    }

    private ArrayList<String> getGuestsInRoomWithGem(String board, String gemcolor)
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

    private boolean canSee(Piece p1, Piece p2) // returns whether or not these two pieces see each 
    {
        return (p1.row==p2.row || p1.col == p2.col);
    }

    
    public void answerAsk(String guest, String player, String board, boolean canSee)
    {
        Board b = new Board(board, pieces, gemLocations);
        ArrayList<String> possibleGuests = new ArrayList<String>();
        Piece p1 = pieces.get(guest);  // retrieve the guest 
        for(String k : pieces.keySet())
        {
            Piece p2 = pieces.get(k);
            if((canSee && canSee(p1,p2)) || (!canSee && !canSee(p1,p2))) possibleGuests.add(p2.name);
        }
        players.get(player).adjustKnowledge(possibleGuests);
    }

    public void answerViewDeck(String player)
    {
        for(String k:players.keySet())
        {
            players.get(k).adjustKnowledge(player);
        }
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


    private String bestGemToGet(String board){
        // Grab each of the gem one at a time and take the highest utility gain
        int[] currentGemState = this.gemCounts;
        String[] availableGem = this.board.rooms[this.pieces.get(this.me.name).row][this.pieces.get(this.me.name).col].availableGems;
        float gemUtility = -9999999999.0F;
        int gemId = -1;
        int optimum_gem = -1;
        System.out.println(gemCounts[0] + " " + gemCounts[1] + " " + gemCounts[2]);
        for (String gemPick: availableGem){
            if (gemPick.equals("red")){
                gemId = Suspicion.RED;
            }else if (gemPick.equals("green")){
                gemId = Suspicion.GREEN;
            }else{
                gemId = Suspicion.YELLOW;
            }
            currentGemState[gemId]++;
            float currentGemUtility = getGemPoint(currentGemState) + playersThatCanTakeGem(gemPick);
            if (currentGemUtility > gemUtility){
                optimum_gem = gemId;
                gemUtility = currentGemUtility;
            }
            currentGemState[gemId]--;
        }
        System.out.println("Selected gem to pick "+ gemFromId(optimum_gem));
        System.out.println("Sandesh picked " + bestGemToGetSandesh(board));
        if (optimum_gem == Suspicion.RED){
            return "red";
        } else if (optimum_gem == Suspicion.GREEN){
            return "green";
        } else {
            return "yellow";
        }
    }


    private float getGemPoint(int [] gems){
        // [1, 2, 1]
        float gemPoint;
        //clone  this before sorting
        int[] gemCloned = new int[] {gems[0],gems[1],gems[2]};
        //sort the gems value
        Arrays.sort(gemCloned);
        // This is the pair that already created
        int minimumGem = gemCloned[0];  // [0, 1, 2]
        // Now we will check the mid value and max value we will try to minimize the difference between them so that they
        // create a new pair
        gemPoint = minimumGem * 6;
        gemPoint += gems[0] - minimumGem;
        gemPoint += gems[1] - minimumGem;
        gemPoint += gems[2] - minimumGem;
        // gemPoint -= gemCloned[2] - gemCloned[1];
        // gemPoint += (gemCloned[2] - minimumGem) +(gemCloned[1] - minimumGem);
        return gemPoint;
    }


    private int playersThatCanTakeGem(String gemToGrab){
        int PlayerThatCanTakeThisGem = 0;
        for(String character: pieces.keySet()){
            int row = pieces.get(character).row;
            int col = pieces.get(character).col;
            if (Arrays.asList(board.rooms[row][col].availableGems).contains(gemToGrab)){
                PlayerThatCanTakeThisGem += 1F;
            }
        }

        System.out.println(PlayerThatCanTakeThisGem + " can take the gem " + gemToGrab);
        return PlayerThatCanTakeThisGem;
    }

    public String bestGemToGetSandesh(String board)
    {
        int[] current = this.gemCounts.clone();
        // Get possible gems this player can take.
        String[] available = getPossibleGemsList();

        // List of gems that will maximize the gem count.
        int best_score = -10000;
        int best_gem = 0;

        for (String gem: available) {
            int gem_index = idFromGem(gem); 

            current[gem_index] += 1;
            int cantake = playersThatCanTakeGem(gem);
            int cantake2 = getGuestsInRoomWithGem(board, gem).size();
            if (cantake != cantake2) {
                System.out.println("Values do not match: " + cantake + " vs " + cantake2);
            }
            int score = gemscore(current) + cantake;
            if (score > best_score) {
                // Score is given by the gem score
                // Tie broken by number of guests that can grab the gem (occlude information).
                // i.e. if we have 1, 1, 2 gems, we can take either red or green gem,
                // but we pick whichever gem can be picked by most players.
                best_score = score;
                best_gem = gem_index;
            }

            // Restore.
            current[gem_index] -= 1;
        }

        String rval = gemFromId(best_gem);
        return rval;
    }

    // Score of a gem combination.
    int gemscore(int[] gems) {
        int red = gems[Suspicion.RED];
        int green = gems[Suspicion.GREEN];
        int yellow = gems[Suspicion.YELLOW];
        int min_gems = Math.min(Math.min(red, green), yellow);
        return min_gems * 6 + (red - min_gems) + (green - min_gems) + (yellow - min_gems);
    }

    public String reportGuesses()
    {
        String rval="";
        for(String k:players.keySet())
        {
            Player p = players.get(k);
            rval += k;
            Collections.shuffle(p.possibleGuestNames);
            for(String g: p.possibleGuestNames)
            {
                rval += ","+g;
            }
            rval+=":";
        }
        return rval.substring(0,rval.length()-1);
    }

    public void reportPlayerActions(String player, String d1, String d2, String cardPlayed, String board, String actions) {
    }


    public SandBotGemLogicPly(String playerName, String guestName, int numStartingGems, String gemLocations, String[] playerNames, String[] guestNames)
    {
        super(playerName, guestName, numStartingGems, gemLocations, playerNames, guestNames);
        display = new TextDisplay(gemLocations);
        pieces = new HashMap<String, Piece>();
        ArrayList<String> possibleGuests = new ArrayList<String>();
        for(String name:guestNames)
        {
            pieces.put(name, new Piece(name));
            if(!name.equals(guestName)) possibleGuests.add(name);
        }
        me = pieces.get(guestName);

        players = new HashMap<String, Player>();
        for(String str: playerNames)
        {
            if(!str.equals(playerName)) players.put(str, new Player(str, possibleGuests.toArray(new String[possibleGuests.size()])));
        }

        otherPlayerNames = players.keySet().toArray(new String[players.size()]);
    }
}