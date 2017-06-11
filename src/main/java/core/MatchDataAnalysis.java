package core;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import util.BobUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class MatchDataAnalysis {
    private static final Map<String, String> idToChampNameMap = LoLAPI.getChampIdToNameMap();
    private static final HashMap<String, Double> winRates = new HashMap<>();
    private static final List<String> team1 = new ArrayList<>();
    private static final List<String> team2 = new ArrayList<>();
    private static final List<String> results = new ArrayList<>();


    public static void main(String[] args) throws IOException {
        loadData();
        fillWinRateMap();
        printWinrates();
        //printTeamWinrates();
        //printChampionPopularity();
        //predictOutcomeUsingNaiveBayes();
    }


    private static void predictOutcomeUsingNaiveBayes() throws IOException {
        int[] guesses = new int[2];

        List<String> team1Test = Files.readAllLines(Paths.get("Input/Test/team1.txt"));
        List<String> team2Test = Files.readAllLines(Paths.get("Input/Test/team2.txt"));
        List<String> resultsTest = Files.readAllLines(Paths.get("Input/Test/result.txt"));

        IntStream.range(0, team1Test.size()).forEach(i -> {
            List<String> team1Ids = List.of(team1Test.get(i).split(","));
            List<String> team2Ids = List.of(team2Test.get(i).split(","));
            int result = Integer.parseInt(resultsTest.get(i));

            double team1WinProb = team1Ids.stream().mapToDouble(winRates::get).reduce(1.0, (d1, d2) -> d1*d2);
            double team2WinProb = team2Ids.stream().mapToDouble(winRates::get).reduce(1.0, (d1, d2) -> d1*d2);

            if ((team1WinProb > team2WinProb && result == 1) || (team1WinProb <= team2WinProb && result == 2)) {
                guesses[0]++;
            } else {
                guesses[1]++;
            }

            String printOut = team1Ids.stream().map(idToChampNameMap::get).collect(Collectors.joining(", "));
            printOut += " (" + String.format("%.1f", 100*team1WinProb/(team1WinProb+team2WinProb)) + "%) vs. ";

            printOut += "(" + String.format("%.1f", 100*team2WinProb/(team1WinProb+team2WinProb)) + "%) ";
            printOut += team2Ids.stream().map(idToChampNameMap::get).collect(Collectors.joining(", "));

            printOut += " >> Accuracy: " + String.format("%.1f",(double)100*guesses[0] / (guesses[0]+guesses[1])) + "%";
            System.out.println(printOut);
        });
    }

    private static void printWinrates() {
        winRates.entrySet().stream()
                .sorted(Comparator.comparingDouble(Map.Entry<String, Double>::getValue).reversed())
                .forEachOrdered(entry -> System.out.println(idToChampNameMap.get(entry.getKey()) + ": " + String.format("%.2f",entry.getValue() * 100)));
    }

    private static void printTeamWinrates() {
        Multiset<String> teamWins = HashMultiset.create(2);
        teamWins.addAll(results);
        System.out.println("Bottom side wins " +  String.format("%.2f",100*(float)teamWins.count("1")/teamWins.size()) + "%");
        System.out.println("Top side wins " +  String.format("%.2f",100*(float)teamWins.count("2")/teamWins.size()) + "%");
    }

    private static void printChampionPopularity() {
        Multiset<String> playRates = HashMultiset.create();
        team1.forEach(rawLine -> playRates.addAll(List.of(rawLine.split(","))));
        team2.forEach(rawLine -> playRates.addAll(List.of(rawLine.split(","))));

        playRates.elementSet().stream()
                .sorted(Comparator.comparingInt(playRates::count).reversed())
                .forEachOrdered(champID -> System.out.println(BobUtil.strictFill(idToChampNameMap.get(champID), 13) + BobUtil.strictFill(""+playRates.count(champID), 7) + String.format("%.2f",100*(float)playRates.count(champID)/(playRates.size()/10)) + "%"));
    }

    private static void fillWinRateMap() throws IOException {
        Multiset<String> wins = HashMultiset.create();
        Multiset<String> losses = HashMultiset.create();

        IntStream.range(0, team1.size()).forEach(i -> {
            List<String> team1Ids = List.of(team1.get(i).split(","));
            List<String> team2Ids = List.of(team2.get(i).split(","));
            int result = Integer.parseInt(results.get(i));

            if (result == 1) {
                wins.addAll(team1Ids);
                losses.addAll(team2Ids);
            } else {
                wins.addAll(team2Ids);
                losses.addAll(team1Ids);
            }
        });
        assert (wins.size() == losses.size());
        assert (wins.elementSet().size() == idToChampNameMap.size());

        wins.elementSet().forEach(champ -> {
            int win = wins.count(champ);
            int loss = losses.count(champ);
            winRates.put(champ, (double)win/(win+loss));
        });
    }

    private static void loadData() throws IOException {
        team1.addAll(Files.readAllLines(Paths.get("Input/Train/team1.txt")));
        team2.addAll(Files.readAllLines(Paths.get("Input/Train/team2.txt")));
        results.addAll(Files.readAllLines(Paths.get("Input/Train/result.txt")));
        assert (team1.size() == team2.size()); assert (team1.size() == results.size());
    }
}