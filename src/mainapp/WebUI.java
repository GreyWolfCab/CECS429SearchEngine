package mainapp;

import cecs429.documents.Document;
import cecs429.documents.DocumentCorpus;
import cecs429.index.*;

import cecs429.query.Search;
import spark.ModelAndView;
import spark.Spark;
import spark.template.thymeleaf.ThymeleafTemplateEngine;
import testing.Accumulator;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

import static java.util.stream.Collectors.joining;

public class WebUI {
    private static Indexer indexer = new Indexer();
    private static Search search = new Search();
    private static Index index = null;
    private static KGram kGramIndex = null;
    private static String dir = "";
    private static DocumentCorpus corpus = null;
    private static DiskIndexWriter diskIndexWriter = new DiskIndexWriter();

    public static void main(String args[]) {

        System.out.println("http://localhost:4567/");
        Spark.staticFileLocation("public_html");
        /* testing environment: http://localhost:4567/ */
        // creates thymeleaf template for index.html at /
        Spark.get("/", (req, res) -> {
            HashMap<String, Object> model =  new HashMap<>();
            return new ThymeleafTemplateEngine().render(new ModelAndView(model, "index"));
        });

        // creates thymeleaf template for search-window.html at /search
        Spark.get("/search", (req, res) -> {
            HashMap<String, Object> model = new HashMap<>();
            return new ThymeleafTemplateEngine().render(new ModelAndView(model, "search-window"));
        });

        // posts directory, builds index
        Spark.post("/", (request, response) -> {
            dir = request.queryParams("directoryValue");
            double buildTime = timeToBuildIndex(dir, false);
            return "<div style=\"font-size: 12px; position:\">Files Indexed From: " + dir + " </br>Time to Index: " + buildTime +  " seconds</div></br>";
        });

        Spark.post("/buildindex", (request, response) -> {
            dir = request.queryParams("directoryValue");
            double buildTime = timeToBuildIndex(dir, true);
            return "<div style=\"font-size: 12px; position:\">Built Disk Index From: " + dir + " </br>Time to Index: " + buildTime + " seconds</div>";
        });

        // posts query values based on query inputs from client (outputs as html table)
        Spark.post("/search", (request, response) -> {

            String queryValue = request.queryParams("queryValue");
            return search.performSearch(corpus, index, kGramIndex, queryValue, true, false);
        });

        // post ranked query values based on query inputs from client (outputs as html table)

        Spark.post("/ranked-search", (request, response) -> {

            String queryValue = request.queryParams("queryValue");

            return search.performSearch(corpus, index, kGramIndex, queryValue, false, false);

        });

        Spark.post("/ranked-search-test", (request, response) -> {

            String queryValue = request.queryParams("queryValue");
            search.setQueryTime(0.0);
            search.performSearch(corpus, index, kGramIndex, queryValue, false, true);
            double time = search.getQueryTime();
            double meanResponseTime = time/search.getTEST_ITERATIONS();
            double throughput = 1/meanResponseTime;
            return "<div style=\"font-size: 12px;\">Total Time To Complete 30 iterations: " + time + " seconds</div>" +
                    "<div style=\"font-size: 12px;\">Mean Response Time: " + meanResponseTime + " seconds</div>" +
                    "<div style=\"font-size: 12px;\">Throughput: " + throughput + " queries/second</div>";

        });

        // posts document contents as a div

        Spark.post("/document", (request, response) -> {
            String docid = request.queryParams("docValue");
            int id = Integer.parseInt(docid);
            corpus = indexer.requestDirectory(dir);
            corpus.getDocuments(); //this line is needed or else corpus has mDocuments = null ???
            Document doc = corpus.getDocument(id);
            Reader reader = doc.getContent();
            StringBuilder content = new StringBuilder();
            int readerCharValue;
            try {
                while ((readerCharValue = reader.read()) != -1) {//read each char from the reader
                    content.append((char)readerCharValue);//convert the value to a char, add to builder
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            return "</br><div style=\"\"> " + content.toString() + " </div></br>";
        });

        // handles special queries from client (posts as a div to client)

        Spark.post("/squery", (request, response) -> {
            String squeryValue = request.queryParams("squeryValue");
            String stemmedTerm;
            if (squeryValue.length() == 2 && squeryValue.substring(1, 2).equals("q")) {
                System.out.println("\nEnding program...");
                System.exit(-1);
            } if (squeryValue.length() >= 5 && squeryValue.substring(1, 5).equals("stem")) {
                stemmedTerm = indexer.userSQueryStem(squeryValue);
                System.out.printf("%s stemmed to: %s", squeryValue.substring(6), stemmedTerm);
                System.out.println();
                return "</br><div style=\"font-size: 12px;\">"+ squeryValue + " stemmed to: " + stemmedTerm + "</div></br>";
                //build a new index from the given directory
            } else if (squeryValue.length() >= 6 && squeryValue.substring(1, 6).equals("index")) {
                System.out.println("Resetting the directory...");//re-build an in-memory index
                dir = squeryValue.substring(7);
                double buildTime = timeToBuildIndex(dir, false);
                return "<div style=\"font-size: 12px\">New Files Indexed From: " + dir + "</div> </br> <div style=\"font-size: 10px\">Time to Index:"+ buildTime +  " seconds</div>";
                //print the first 1000 terms in the vocabulary
            } else if (squeryValue.length() == 6 && squeryValue.substring(1, 6).equals("vocab")) {
                List<String> vocabList = indexer.userSQueryVocab(index);//gather vocab list from any index
                List<String> subVocab = null;
                if (vocabList.size() >= 1000) { subVocab = vocabList.subList(0, 999); }
                else { subVocab = vocabList.subList(0, vocabList.size() - 1); }
                String formattedVocabList = subVocab.stream().map(item -> "" + item + "</br>").collect(joining(" "));
                return "<b style=\"font-size: 15px;\"># of vocab terms: " + vocabList.size() + "</b></div></br>" + " </br> <div style=\"font-size: 12px;\">"+ formattedVocabList + "</br>";
            } else {
                return "<div style=\"font-size: 12px;\">Not Valid Special Query</div></br>";
            }
        });

    }

    private static double timeToBuildIndex(String dir, boolean isDiskIndex) throws IOException {

        System.out.println("Starting to build index...");
        //measure how long it takes to build the index
        long startTime = System.nanoTime();

        if (isDiskIndex) {//create index from disk
            corpus = indexer.requestDirectory(dir);
            kGramIndex = indexer.buildDiskKGramIndex(dir);
            index = indexer.buildDiskPositionalIndex(dir);//builds positional index and k-gram index
        } else {//create in memory index
            corpus = indexer.requestDirectory(dir);
            kGramIndex = new KGramIndex();
            index = indexer.timeIndexBuild(corpus, kGramIndex, dir);
            diskIndexWriter.writeIndex(index, dir);//calls the writer of index to disk
        }

        long stopTime = System.nanoTime();
        double indexSeconds = (double)(stopTime - startTime) / 1_000_000_000.0;
        System.out.println("Done!\n");
        System.out.println("Time to build index: " + indexSeconds + " seconds");

        return indexSeconds;

    }

}
