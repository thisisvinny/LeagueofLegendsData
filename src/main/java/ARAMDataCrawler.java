import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

/**
 * Created by Dons on 12-08-2016.
 */
public class ARAMDataCrawler {
    private static final long TIMESINCELASTPATCH  = 1000*60*60*24*8;
    private static final HashMap<Integer, Integer> champIDTranslationMap = new HashMap<>();
    private static final ArrayList<Long> playerIDList = new ArrayList<>();
    private static final HashSet<Long> gamesIDsSeen = new HashSet<>();
    private static final Random random = new Random();

    private static final int requestDelayInMS = 1330;
    private static long latestGameTimeInMS = 0;

    public static void main(String[] args) {
        fillChampIDTranslationMap();
        playerIDList.add(20590302L);
        crawlARAMGames("euw");
    }


    private static void crawlARAMGames(String region) {
        int currentPlayerIndex = 0;

        while (playerIDList.size() > currentPlayerIndex) {
            long currentPlayerID = playerIDList.get(currentPlayerIndex);
            long lastLookupTime = System.currentTimeMillis();
            int newGamesFound = 0;

            JsonNode recentGamesNode = LeagueAPI.getRecentGames(currentPlayerID);

            //Sanity checks that we recieved a gameNode and that the game no contains a game list.
            if (recentGamesNode == null ) System.out.println("*** Recent game node is null for " + + currentPlayerID);
            else if (!recentGamesNode.has("games")) {
                System.out.println("Encountered problem looking up player with ID: " + currentPlayerID);
                System.out.println(recentGamesNode);
            } else {
                Iterator<JsonNode> gameNodes = recentGamesNode.get("games").elements();
                while (gameNodes.hasNext()) {
                    JsonNode gameNode = gameNodes.next();
                    boolean isInvalid = gameNode.get("invalid").asBoolean();
                    long gameTimestamp = gameNode.get("createDate").asLong();
                    long gameID = gameNode.get("gameId").asLong();
                    String gameMode = gameNode.get("gameMode").asText();
                    String gameType = gameNode.get("gameType").asText();

                    // Test to see if any games return invalid == true, and investigate the cause if so, possibly used for loss forgiven?
                    if (isInvalid) System.out.println("*** Found invalid game?! .. GameID: " + gameID);

                    //If we are playing an ARAM MatchMade game on the current Patch
                    if (gameMode.equalsIgnoreCase("ARAM") && gameType.equalsIgnoreCase("MATCHED_GAME") && (latestGameTimeInMS - gameTimestamp) < TIMESINCELASTPATCH) {
                        //if we found a new game
                        if (!gamesIDsSeen.contains(gameID)) {
                            parseAndPrintGame(gameNode, region);
                            gamesIDsSeen.add(gameID);
                            newGamesFound++;
                        }
                    }
                }

                long sleepTime = (lastLookupTime + requestDelayInMS) - System.currentTimeMillis();
                if (sleepTime > 0) try { Thread.sleep(requestDelayInMS); } catch (InterruptedException e) { e.printStackTrace(); }
                currentPlayerIndex++;
                System.out.println("Crawled player number " + currentPlayerIndex + ", Players left: " + (playerIDList.size() - currentPlayerIndex) + ", Games found: " + gamesIDsSeen.size() + " (+" + newGamesFound + ")");
            }
        }
    }

    private static void parseAndPrintGame(JsonNode gameNode, String region) {
        HashSet<Integer> team100 = getChampIDsAndAddPlayerIDs(gameNode, 100);
        HashSet<Integer> team200 = getChampIDsAndAddPlayerIDs(gameNode, 200);
        boolean playerWon = gameNode.get("stats").get("win").asBoolean();
        int playerTeam = gameNode.get("teamId").asInt();
        String output = "";

        output += buildNeuralVector(team100, team200) + ",";

        if (playerTeam == 100 && playerWon) {
            output += "1";
        } else if (playerTeam == 200 && !playerWon) {
            output += "1";
        } else {
            output += "0";
        }

        String dataMode = (random.nextInt(5) == 0) ? "evaluation" : "training";
        BobsUtility.writeTextToFile(output, "Output/" + region + "/" + dataMode + ".csv", true);
        BobsUtility.writeTextToFile(output, "Output/Combined/" + dataMode + ".csv", true);
    }

    private static HashSet<Integer> getChampIDsAndAddPlayerIDs(JsonNode gameNode, int teamID) {
        if (gameNode.get("createDate").asLong() > latestGameTimeInMS) latestGameTimeInMS = gameNode.get("createDate").asLong();
        HashSet<Integer> returnSet = new HashSet<>();

        //if the current player is on the same add his champID to the returnSetm after translating it
        if (gameNode.get("teamId").asInt() == teamID) {
            returnSet.add(champIDTranslationMap.get(gameNode.get("championId").asInt()));
        }

        Iterator<JsonNode> playerNodes = gameNode.get("fellowPlayers").elements();
        while (playerNodes.hasNext()) {
            JsonNode playerNode = playerNodes.next();
            int playerChampID = playerNode.get("championId").asInt();
            int playerTeamID = playerNode.get("teamId").asInt();
            long playerID = playerNode.get("summonerId").asLong();
            playerChampID = champIDTranslationMap.get(playerChampID);

            if (playerTeamID == teamID) {
                returnSet.add(playerChampID);
                if (!playerIDList.contains(playerID)) playerIDList.add(playerID);
            }
        }

        if (returnSet.size() != 5) {
            System.out.println("Something went wrong, did not find 5 players for team " + teamID + " in gameID: " + gameNode.get("gameId").asLong() + " team size: " + returnSet.size());
        }
        return returnSet;
    }

    /**
     * Outputs string of the form 0,0,0,0,1,1,0,0,0 where each 1 represents a champion of that ID
     * team 1 is represented by the first 1 to MAXCHAMPID 'neurons' and team by the next MAXCHAMPID 'neurons'
     * @param team100 Champ Id's of players in the first team.
     * @param team200 Champ Id's of players in the second team.
     * @return
     */
    private static String buildNeuralVector(HashSet<Integer> team100, HashSet<Integer> team200) {
        int[] neurons = new int[champIDTranslationMap.size() * 2];
        for (Integer i : team100) neurons[i - 1] = 1;
        for (Integer i : team200) neurons[i + champIDTranslationMap.size() - 1] = 1;

        // Return the string representation of the array minus the brackets
        String returnString = Arrays.toString(neurons).replaceAll(" ", "");
        return returnString.substring(1, returnString.length() - 1);
    }

    /**
     * Fills out a map translating from current champion ID's to ID 1->x where x is the current number of active champions.
     */
    private static void fillChampIDTranslationMap() {
        TreeSet<Integer> sortedChampIDs = new TreeSet<>();

        JsonNode allChampionNodes = LeagueAPI.getChampions("euw");
        Iterator<JsonNode> championNodes = allChampionNodes.get("champions").elements();
        while (championNodes.hasNext()) {
            JsonNode championNode = championNodes.next();
            sortedChampIDs.add(championNode.get("id").asInt());
        }

        int mappingID = 1;
        for (int id : sortedChampIDs) {
            champIDTranslationMap.put(id, mappingID);
            mappingID++;
        }

        System.out.println(champIDTranslationMap.toString());
        System.out.println(champIDTranslationMap.size() + " Different ChampionID's found.");
    }
}
