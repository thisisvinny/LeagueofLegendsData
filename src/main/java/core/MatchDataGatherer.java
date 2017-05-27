package core;

import com.fasterxml.jackson.databind.JsonNode;
import util.BobUtil;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Random;
import java.util.stream.Collectors;

public class MatchDataGatherer {
    private static final Instant lastPatch = Instant.now().minus(Duration.ofDays(7));
    private static final ArrayDeque<Long> unVisitedPlayers = new ArrayDeque<>();
    private static final HashSet<Long> knownAccounts = new HashSet<>();
    private static final HashSet<Long> knownGames = new HashSet<>();
    private static final Random random = new Random();


    public static void main(String[] args) throws InterruptedException {
        //add seed players
        unVisitedPlayers.offer(19816915L);
        crawlMatchData();
    }

    private static void crawlMatchData() throws InterruptedException {
        Instant nextRequest = Instant.now();
        while (!unVisitedPlayers.isEmpty()) {
            if (nextRequest.isAfter(Instant.now())) {
                Thread.sleep(nextRequest.toEpochMilli() - Instant.now().toEpochMilli());
            }
            nextRequest = Instant.now().plusMillis(1250);
            writeMatchesToFileForAccount(unVisitedPlayers.pop());
            System.out.println("Known Players " + knownAccounts.size() + ", knownGames " + knownGames.size() + ", unVisitedPlayers " + unVisitedPlayers.size());
        }
    }

    private static void writeMatchesToFileForAccount(long accountId) {
        LoLAPI.getLatestMatches(accountId, "ARAM")
                .filter(node -> !knownGames.contains(node.get("gameId").asLong()))
                .filter(node -> node.get("createDate").asLong() > lastPatch.toEpochMilli())
                .forEach(node -> {
                    writeMatchToFile(node);
                    knownGames.add(node.get("gameId").asLong());
                });
    }

    private static void writeMatchToFile(JsonNode matchNode) {
        HashSet<String> team1Ids = new HashSet<>();
        HashSet<String> team2Ids = new HashSet<>();
        int winningTeam = 2;

        //First Parse Player Data
        if (matchNode.get("teamId").asInt() == 100) {
            team1Ids.add(matchNode.get("championId").asText());
            if (matchNode.get("stats").get("win").asBoolean()) winningTeam = 1;
        } else if (matchNode.get("teamId").asInt() == 200) {
            team2Ids.add(matchNode.get("championId").asText());
            if (!matchNode.get("stats").get("win").asBoolean()) winningTeam = 1;
        } else {
            throw new RuntimeException("PANIC");
        }

        //Now fill in teams
        matchNode.get("fellowPlayers").iterator().forEachRemaining(node -> {
            if (node.get("teamId").asInt() == 100) team1Ids.add(node.get("championId").asText());
            else if (node.get("teamId").asInt() == 200) team2Ids.add(node.get("championId").asText());
            else throw new RuntimeException("PANIC");

            //add new account ids to the list of account IDs to crawl
            if (!knownAccounts.contains(node.get("summonerId").asLong())) {
                knownAccounts.add(node.get("summonerId").asLong());
                unVisitedPlayers.offer(node.get("summonerId").asLong());
            }
        });

        //print teams to file
        String fileLocation = (random.nextInt(5) == 0) ? "Output/Test/" : "Output/Train/";
        BobUtil.writeTextToFile(team1Ids.stream().collect(Collectors.joining(",")),fileLocation + "team1.txt",true);
        BobUtil.writeTextToFile(team2Ids.stream().collect(Collectors.joining(",")),fileLocation + "team2.txt",true);
        BobUtil.writeTextToFile(""+winningTeam, fileLocation + "result.txt", true);
    }
}
