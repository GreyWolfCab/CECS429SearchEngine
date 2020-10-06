package mainapp;

import cecs429.documents.Document;
import cecs429.documents.DocumentCorpus;
import cecs429.index.Index;
import cecs429.index.KGramIndex;
import cecs429.index.Posting;

import spark.ModelAndView;
import spark.Spark;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class WebUI {
    public static Indexer indexer = new Indexer();
    public static Index index = null;
    public static KGramIndex kGramIndex = new KGramIndex();
    public static String dir = "";
    public static ArrayList<Posting> postings = new ArrayList();
    public static DocumentCorpus corpus = null;

    public static void main(String args[]) {

        Spark.staticFileLocation("public_html");
        /** testing environment: http://localhost:4567/ **/
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

        // posts directory
        Spark.post("/", (request, response) -> {
            String directoryValue = request.queryParams("directoryValue");
            dir = directoryValue;
            corpus = indexer.requestDirectory(dir);
            index = indexer.timeIndexBuild(corpus, kGramIndex);
            return "<div style=\"font-size: 12px; position:\">Files Indexed From: " + directoryValue + " Time to Index:"+ indexer.getTimeToBuildIndex() +  " seconds</div>";
        });

        // posts query values based on query inputs from client (outputs as html table)
        Spark.post("/search", (request, response) -> {
            String queryValue = request.queryParams("queryValue");

            postings = (ArrayList<Posting>) indexer.userQueryInput(corpus, index, queryValue);

            return "<div><b>Query: </b>" + queryValue +
                    "<table id=\"document-table\" style=\"width:100%\">\n" +
                    "    <tr>\n" +
                    "        <th>Document ID</th>\n" +
                    "        <th>Document Title</th>\n" +
                    "        <th>Positions</th>\n" +
                    "    </tr>\n" +
                         postings.toString() +
                    "</table>" +
                    "<div>Total Documents: " + postings.size() + "</div></div>" ;
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
            return "</br><div style=\"\"> " + content.toString() + " </div>";
        });

        // handles special queries from client (posts as a div to client)
        Spark.post("/squery", (request, response) -> {
            String squeryValue = request.queryParams("squeryValue");
            String stemmedTerm;
            if (squeryValue.length() >= 5 && squeryValue.substring(1, 5).equals("stem")) {
                stemmedTerm = indexer.userSQueryStem(squeryValue);
                System.out.printf("%s stemmed to: %s", squeryValue.substring(6), stemmedTerm);
                System.out.println();
                return "<div style=\"font-size: 12px;\">"+ squeryValue + " stemmed to: " + stemmedTerm + "</div>";
                //build a new index from the given directory
            } else if (squeryValue.length() >= 6 && squeryValue.substring(1, 6).equals("index")) {
                System.out.println("Resetting the directory...");
                dir = squeryValue.substring(7);
                corpus = indexer.requestDirectory(dir);
                index = indexer.timeIndexBuild(corpus, kGramIndex);
                return "<div style=\"font-size: 12px\">New Files Indexed From: " + dir + "</div> <div style=\"font-size: 10px\">Time to Index:"+ indexer.getTimeToBuildIndex() +  " seconds</div>";
                //print the first 1000 terms in the vocabulary
            } else if (squeryValue.length() == 6 && squeryValue.substring(1, 6).equals("vocab")) {
                List<String> vocabList = indexer.userSQueryVocab();
                return "<div style=\"font-size: 12px;\">"+ vocabList +" </br style=\"font-size: 15;\"># of vocab terms: " + vocabList.size() + "</div>";
            } else {
                return "<div style=\"font-size: 12px;\">Not Valid Special Query</div>";
            }
        });

    }
}
