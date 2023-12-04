import java.util.*;

public class SandBot extends Bot
{
    State state = new State();

    public float simulate(ArrayList<String> cardActionsList) {
        return 0.0F;
    }

    public String returnBestActions(ArrayList<ArrayList<String>> simulations) {
        // Run the simulations and return the best scoring card's cardactions.
        String actions = "";
        return actions;
    }

    @Override
    public String getPlayerActions(String d1, String d2, String card1, String card2, String board) throws Suspicion.BadActionException
    {
        this.state.board = new State.Board(board, this.state.pieces, this.state.gemLocations);
        String actions = "";

        String[] possibleMove1;
        if (d1.equals("?")) {
            possibleMove1 = guestNames.clone();
        } else {
            possibleMove1 = new String[] {d1};
        }

        String[] possibleMove2;
        if (d2.equals("?")) {
            possibleMove2 = guestNames.clone();
        } else {
            possibleMove2 = new String[] {d2};
        }

        System.out.println(Arrays.toString(possibleMove1));
        System.out.println(Arrays.toString(possibleMove2));
        String[] cards = new String[]{ card1, card2};

        ArrayList<ArrayList<String>> simulations = new ArrayList<ArrayList<String>>();
        for (String movepiece1: possibleMove1) {
            for (String movepiece2: possibleMove2) {
                System.out.println("1 " + movepiece1);
                System.out.println("2" + movepiece2);
                for (String move1: state.getPossibleMoves(state.pieces.get(movepiece1))) {
                    for (String move2: state.getPossibleMoves(state.pieces.get(movepiece2))) {
                        for (String card: cards) {
                            ArrayList<String> simulActions = new ArrayList<String>();
                            // Push the move piece action immediately
                            simulActions.add("move," + movepiece1 + ","+move1);                            
                            simulActions.add("move," + movepiece2 + ","+move2);                            

                            // List of simulate-able actions.
                            for (String cardAction: card.split(":")) {
                                if (cardAction.startsWith("move")) {
                                    // Bring our move piece to our column on a random row anywhere.
                                    int row = state.r.nextInt(3);
                                    int col = state.me.col;
                                    simulActions.add("move,"+state.pieceToMove+row+","+col);

                                } else if (cardAction.startsWith("viewDeck")) {
                                    simulActions.add("viewDeck");

                                } else if (cardAction.startsWith("get")) {
                                    simulActions.add("get,"+state.bestGemToGet(board));
                                    
                                } else if (cardAction.startsWith("ask")) {
                                    String guestName = cardAction.split(",")[1];
                                    simulActions.add("ask," + guestName +"," + state.pickPlayerToQuestion(guestName));
                                }
                            }

                            simulations.add(simulActions);
                        }
                    }
                }
            }
        }

        System.out.println("I have total " + simulations.size() + " simulations to run");

        // Random move for dice1
        if(d1.equals("?")) d1 = guestNames[state.r.nextInt(guestNames.length)];
        State.Piece piece = state.pieces.get(d1);
        String[] moves = state.getPossibleMoves(piece);
        int movei = state.r.nextInt(moves.length);
        actions += "move," + d1 + "," + moves[movei];
        state.board.movePlayer(piece, Integer.parseInt(moves[movei].split(",")[0]), Integer.parseInt(moves[movei].split(",")[1])); // Perform the move on my board

        // Random move for dice2
        if(d2.equals("?")) d2 = guestNames[state.r.nextInt(guestNames.length)];
        piece = state.pieces.get(d2);
        moves = state.getPossibleMoves(piece);
        movei = state.r.nextInt(moves.length);
        actions += ":move," + d2 + "," + moves[movei];
        state.board.movePlayer(piece, Integer.parseInt(moves[movei].split(",")[0]), Integer.parseInt(moves[movei].split(",")[1])); // Perform the move on my board

        // which card
        int i = state.r.nextInt(2);
        actions += ":play,card"+(i+1);

        String card = i==0?card1:card2;


        for(String cardAction: card.split(":")) // just go ahead and do them in this order
        {
            if(cardAction.startsWith("move")) 
            {
                String guest = state.pieceToMove;
                guest = guestNames[state.r.nextInt(guestNames.length)];
                System.out.println("@SandBot: Moving our favorite " + guest + " piece");
                actions += ":move," + guest + "," + state.r.nextInt(3) + "," + state.r.nextInt(4);
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
                    gemToGrab = state.bestGemToGet(board);
                    actions += ":get," + gemToGrab;
                }
                else 
                {
                    actions += ":" + cardAction;
                    gemToGrab=cardAction.trim().split(",")[1];
                }
                if(gemToGrab.equals("red")) state.gemCounts[Suspicion.RED]++;
                else if(gemToGrab.equals("green")) state.gemCounts[Suspicion.GREEN]++;
                else state.gemCounts[Suspicion.YELLOW]++;
            }
            else if(cardAction.startsWith("ask")) 
            {
                // Ask a random player
                // actions += ":" + cardAction + otherPlayerNames[r.nextInt(otherPlayerNames.length)]; 

                String guestName = cardAction.split(",")[1];
                
                String picked = state.pickPlayerToQuestion(guestName);
                System.out.println("@SandBot: Playing the 'ask' card. Picked " + picked + " to ask about " + guestName);
                actions += ":" + cardAction + picked;
            }
        }
        return actions;
    }
    

    @Override
    public void answerAsk(String guest, String player, String board, boolean canSee)
    {
        State.Board b = new State.Board(board, state.pieces, gemLocations);
        ArrayList<String> possibleGuests = new ArrayList<String>();
        State.Piece p1 = state.pieces.get(guest);  // retrieve the guest
        for(String k : state.pieces.keySet())
        {
            State.Piece p2 = state.pieces.get(k);
            if((canSee && state.canSee(p1,p2)) || (!canSee && !state.canSee(p1,p2))) possibleGuests.add(p2.name);
        }
        state.players.get(player).adjustKnowledge(possibleGuests);
    }

    @Override
    public void answerViewDeck(String player)
    {
        for(String k:state.players.keySet())
        {
            state.players.get(k).adjustKnowledge(player);
        }
    }

    @Override
    public String reportGuesses()
    {
        ArrayList<ArrayList<String>> combinations = new ArrayList<>();
        ArrayList<ArrayList<String>> valid_options = new ArrayList<>();

        for (String player: state.players.keySet()){
            ArrayList<String> inner = new ArrayList<String>(state.players.get(player).possibleGuestNames);
            valid_options.add(inner);
        }

        state.gen_combinations(0, new ArrayList<>(), valid_options, combinations);

        HashMap<String, HashMap<String, Integer>> stats = new HashMap<>();
        for (int playerId = 0; playerId < state.otherPlayerNames.length; playerId++) {
            for (ArrayList<String> entry: combinations) {
                String selected = entry.get(playerId);
                HashMap<String, Integer> row = stats.get(state.otherPlayerNames[playerId]) == null ? new HashMap<String, Integer>() : stats.get(state.otherPlayerNames[playerId]);
                int temp = row.getOrDefault(selected, 0);
                temp += 1;
                row.put(selected, temp);
                stats.put(state.otherPlayerNames[playerId], row);
            }
        }

        for (String entry: stats.keySet()) {
            HashMap<String, Integer> row = stats.get(entry);
            System.err.format("row: %s\n", row);
        }

        String rval = "";
        for (int id = 0; id < state.otherPlayerNames.length; id++) {
            String player = state.otherPlayerNames[id];
            HashMap<String, Integer> possible = stats.get(player);
            int max_value = -1000;
            String selection = null;
            for (String guest: possible.keySet()) {
                if (possible.get(guest) > max_value) {
                    max_value = possible.get(guest);
                    selection = guest;
                }
            }
            rval += player + "," + selection + ":";
        }

        rval = rval.substring(0, rval.length() - 1);
        System.out.println("@SandBot: Guesses are: " + rval);
        return rval;
    }

    @Override
    public void reportPlayerActions(String player, String d1, String d2, String cardPlayed, String board, String actions)
    {
    }

    @Override
    public void reportPlayerActions(String player, String d1, String d2, String cardPlayed, String board[], String actions)
    {
        if(player.equals(this.playerName)) return; // If player is me, return
        // Check for a get action and use the info to update player knowledge
        if(cardPlayed.split(":")[0].equals("get,") || cardPlayed.split(":")[1].equals("get,"))
        {
            int splitindex;
            String[] split = actions.split(":");
            String get;
            if(split[3].indexOf("get")>=0) splitindex=3;
            else splitindex=4;
            get=split[splitindex];
            String gem = get.split(",")[1];
            // board[splitIndex+1] will have the state of the board when the gem was taken
            if(board[splitindex]!=null) // This would indicate an error in the action
            {
                ArrayList<String> possibleGuests = state.getGuestsInRoomWithGem(board[splitindex],gem);
                state.players.get(player).adjustKnowledge(possibleGuests);
            }
        }
    }


    public SandBot(String playerName, String guestName, int numStartingGems, String gemLocations, String[] playerNames, String[] guestNames)
    {
        super(playerName, guestName, numStartingGems, gemLocations, playerNames, guestNames);
        state.gemLocations = gemLocations;
        state.display = new TextDisplay(gemLocations);
        state.pieces = new HashMap<String, State.Piece>();
        ArrayList<String> possibleGuests = new ArrayList<String>();
        for(String name:guestNames)
        {
            state.pieces.put(name, new State.Piece(name));
            if(!name.equals(guestName)) possibleGuests.add(name);
        }
        state.me = state.pieces.get(guestName);

        int pieceIndex = new Random().nextInt(guestNames.length);
        state.pieceToMove = guestNames[pieceIndex];

        state.players = new HashMap<String, State.Player>();
        for(String str: playerNames)
        {
            if(!str.equals(playerName)) state.players.put(str, new State.Player(str, possibleGuests.toArray(new String[possibleGuests.size()])));
        }

        state.otherPlayerNames = state.players.keySet().toArray(new String[state.players.size()]);
    }
}