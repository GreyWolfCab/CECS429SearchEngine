package mainapp;

import cecs429.documents.DocumentCorpus;
import cecs429.index.Index;
import cecs429.index.KGramIndex;


import cecs429.index.Posting;
import spark.ModelAndView;
import spark.Spark;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class WebUI {



    public static void main(String args[]) {
        Spark.staticFileLocation("public_html");
        Indexer indexer = new Indexer();
        KGramIndex kGramIndex = new KGramIndex();
        AtomicReference<String> dir = new AtomicReference<>("");
        AtomicReference<ArrayList<Posting>> postings = new AtomicReference<>(new ArrayList());

        Spark.get("/", (req, res) -> {
            HashMap<String, Object> model =  new HashMap<>();
            return new ThymeleafTemplateEngine().render(new ModelAndView(model, "index"));
        });


        Spark.get("/search", (req, res) -> {
            HashMap<String, Object> model = new HashMap<>();
            return new ThymeleafTemplateEngine().render(new ModelAndView(model, "search-window"));
        });

        Spark.post("/", (request, response) -> {
            String directoryValue = request.queryParams("directoryValue");
            dir.set(directoryValue);
            indexer.runIndexer(directoryValue);
            return "<div style=\"font-size: 12px; position:\">Files Indexed From: " + directoryValue + " Time to Index:"+ indexer.getTimeToBuildIndex() +  " seconds</div>";
        });

        Spark.post("/search", (request, response) -> {
            String queryValue = request.queryParams("queryValue");
            DocumentCorpus corpus = indexer.requestDirectory(dir.get());//collect all documents from a directory
            Index index = indexer.timeIndexBuild(corpus, kGramIndex);
            postings.set((ArrayList<Posting>) indexer.userQueryInput(corpus, index, queryValue));
            //System.out.println(response.body());

            return "<div><b>Query: </b>" + queryValue +
                    "<table id=\"document-table\" style=\"width:100%\">\n" +
                    "    <tr>\n" +
                    "        <th>Document ID</th>\n" +
                    "        <th>Document Title</th>\n" +
                    "        <th>Positions</th>\n" +
                    "    </tr>\n" +
                         postings.get().toString() +
                    "</table>" +
                    "Open Doc ID: <input id=\"open-input\"type=\"text\" name=\"open-doc\" value=\"\" autocomplete=\"off\"/>" +
                    "<a id=\"open-button\" >open</a>" +
                    "</div>" ;
        });

        Spark.post("/squery", (request, response) -> {
//            index.set(indexer.timeIndexBuild(corpus, kGramIndex));
            String squeryValue = request.queryParams("squeryValue");
            String stemmedTerm;
            if (squeryValue.length() >= 5 && squeryValue.substring(1, 5).equals("stem")) {
                stemmedTerm = indexer.userSQueryStem(squeryValue);
                System.out.printf("%s stemmed to: %s", squeryValue.substring(6), stemmedTerm);
                System.out.println();
                return "<div style=\"font-size: 10px;\">"+ squeryValue + " stemmed to: " + stemmedTerm + "</div>";
                //build a new index from the given directory
            } else if (squeryValue.length() >= 6 && squeryValue.substring(1, 6).equals("index")) {
                System.out.println("Resetting the directory...");
                dir.set(squeryValue.substring(7));
                indexer.runIndexer(squeryValue.substring(7));
                return "<div style=\"font-size: 10px\">New Files Indexed From: " + dir.get() + "</div> <div style=\"font-size: 10px\">Time to Index:"+ indexer.getTimeToBuildIndex() +  " seconds</div>";
                //print the first 1000 terms in the vocabulary
            } else if (squeryValue.length() == 6 && squeryValue.substring(1, 6).equals("vocab")) {
                List<String> vocabList = indexer.userSQueryVocab();
                return "<div style=\"font-size: 8px;\">"+ vocabList +"</div>";
            } else {
                return "<div style=\"font-size: 10px;\">Not Valid Special Query</div>";
            }
        });

    }


}
