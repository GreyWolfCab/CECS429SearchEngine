package mainapp;

import cecs429.documents.DocumentCorpus;
import cecs429.index.Index;
import cecs429.index.KGram;
import cecs429.query.Search;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class TestQueries {

    private static Search search = new Search();

    public static void runTestQueries(String indexLocation, DocumentCorpus corpus, Index index, KGram kGramIndex, ArrayList<Integer> leaders, Boolean isBooleanQuery, Boolean testThroughput) {

        ArrayList<String> allQueries = new ArrayList<>();
        try {
            File queries = new File(indexLocation + "\\relevance\\queries");
            Scanner read = new Scanner(queries);
            while (read.hasNextLine()) {
                allQueries.add(read.nextLine());
            }
            read.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        ArrayList<int[]> relDocs = new ArrayList<>();
        try {
            File relevantDocs = new File(indexLocation + "\\relevance\\qrel");
            Scanner read = new Scanner(relevantDocs);
            int i = 0;
            while (read.hasNextLine()) {
                String data = read.nextLine();
                String[] stringDocIds = data.split(" ");
                int[] intDocIds = new int[stringDocIds.length];
                for (int j = 0; j < intDocIds.length; j++) {
                    intDocIds[j] = Integer.parseInt(stringDocIds[j]);
                }
                relDocs.add(intDocIds);
            }
            read.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        double sumAvgPrecision = 0;
        for (int i = 0; i < allQueries.size(); i++) {
            sumAvgPrecision += search.testSearch(corpus, index, kGramIndex, allQueries.get(i), null, false, false, relDocs.get(i));
        }

        double meanAvgPrecision = ((double)1/allQueries.size()) * sumAvgPrecision;

        System.out.println("MAP: " + meanAvgPrecision);


    }

}
