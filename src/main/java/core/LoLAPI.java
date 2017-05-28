package core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jdk.incubator.http.HttpClient;
import jdk.incubator.http.HttpRequest;
import jdk.incubator.http.HttpResponse;
import util.PrettyPrinter;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.UnresolvedAddressException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class LoLAPI {
    private static final String BASEAPILOCATION = "https://euw.api.pvp.net/api/";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static String APIKEY = "";
    static {
        try {
            APIKEY = Files.readAllLines(Paths.get("Input/apikey.txt")).get(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        getLatestMatches(19816915, "ARAM")
                .forEach(PrettyPrinter::prettyPrintJSonNode);
    }

    public static Stream<JsonNode> getLatestMatches(long accountID, String gameMode) { // v1.3/game/by-summoner/
        JsonNode root = getJsonNodeFromString("https://euw.api.riotgames.com/api/lol/EUW/v1.3/game/by-summoner/" + accountID + "/recent?api_key=" +APIKEY);
        if (root == null) return Stream.of();

        return StreamSupport.stream(root.get("games").spliterator(), false)
                .filter(node -> node.get("gameMode").asText().equalsIgnoreCase(gameMode))
                .filter(node -> node.get("gameType").asText().equalsIgnoreCase("MATCHED_GAME"));
    }

    public static Map<String, String> getChampIdToNameMap() {
        JsonNode rootNode  = getJsonNodeFromString("https://euw1.api.riotgames.com/lol/static-data/v3/champions?dataById=true&api_key=" + APIKEY);
        return StreamSupport.stream(rootNode.get("data").spliterator(), false)
                .collect(Collectors.toMap(node -> node.get("id").asText(), node -> node.get("name").asText()));
    }


    private static JsonNode getJsonNodeFromString(String urlString) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(urlString)).GET().build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandler.asString());
            if (response.statusCode() == 200) return new ObjectMapper().readTree(response.body());
            else {
                System.out.println("something went wrong getting " + urlString + " ... " + response.body());
            }
        } catch (IOException | InterruptedException | UnresolvedAddressException e) {
            e.printStackTrace();
        }
        return null;
    }
}
