import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import org.apache.hc.client5.http.impl.sync.HttpClientBuilder;
import org.apache.hc.client5.http.methods.HttpGet;
import org.apache.hc.client5.http.sync.HttpClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Created by Dons on 12-08-2016.
 */
public class LeagueAPI {
    private static final String BASEAPILOCATION = "https://SERVER.api.pvp.net/api/lol/SERVER/";
    private static final String APIKey;
    private static final HttpClient client = HttpClientBuilder.create().build();

    static {
        APIKey = loadAPIKey("Input/apikey.txt");
    }


    public static void main(String[] args) {
        System.out.println(getRecentGames(51666047, "euw"));
    }

    public static JsonNode getRecentGames(long playerID, String server) {
        HttpGet getRequest = new HttpGet(BASEAPILOCATION.replaceAll("SERVER", server) + "v1.3/game/by-summoner/" + playerID + "/recent?api_key=" + APIKey);
        return executeGet(getRequest);
    }

    public static JsonNode getChampions(String server) {
        HttpGet getRequest = new HttpGet(BASEAPILOCATION.replaceAll("SERVER", server) + "v1.2/champion?api_key=" + APIKey);
        return executeGet(getRequest);
    }

    private static JsonNode executeGet(HttpGet getRequest) {
        JsonNode returnNode = null;

        try (InputStream input = client.execute(getRequest).getEntity().getContent()) {
            returnNode = new ObjectMapper().readTree(input);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Something went wrong requesting from the riot API, this should probably be handled better");
        }
        getRequest.releaseConnection();
        return returnNode;
    }

    private static String loadAPIKey(String fileLocation) {
        String returnKey = "";
        File apiKeyFile = new File(fileLocation);
        if (apiKeyFile.exists()) {
            try {
                returnKey = Files.readFirstLine(apiKeyFile, Charset.defaultCharset());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Could not find API key file at location: " + fileLocation);
        }
        return returnKey;
    }
}
