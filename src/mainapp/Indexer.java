package mainapp;

import cecs429.documents.DirectoryCorpus;
import cecs429.documents.Document;
import cecs429.documents.DocumentCorpus;
import cecs429.index.Index;
import cecs429.index.PositionalInvertedIndex;
import cecs429.index.Posting;
import cecs429.query.BooleanQueryParser;
import cecs429.text.AdvancedTokenProcesser;
import cecs429.text.EnglishTokenStream;

import java.nio.file.*;
import java.util.List;
import java.util.Scanner;
import static spark.Spark.*;


public class Indexer {

    public static void main(String args[]) {

        DocumentCorpus corpus = requestDirectory();// collect all documents from a directory

        Iterable<Document> docs = corpus.getDocuments();
        for (Document doc : docs) {// print each document associated with its id
            System.out.println(doc.getId() + ": " + doc.getTitle());
        }

        Index index = indexCorpus(corpus);

        BooleanQueryParser query = new BooleanQueryParser();
        System.out.println("\nTesting boolean parser:");

        // AND query test
        // search for the terms: bird AND seed AND science
        String searchText1 = "bird seed science";

        System.out.println("Searching for: " + searchText1);
        for (Posting posting : query.parseQuery(searchText1).getPostings(index)) {
            System.out.print("Document ID: " + posting.getDocumentId() + " Positions: ");
            for (Integer positions : posting.getPositions()) {
                System.out.print(positions + ", ");
            }
            System.out.println();
        }

        // OR query test
        // search for the terms: hawaii OR manoa OR goose
        String searchText2 = "hawaii + manoa + goose";

        System.out.println("\nSearching for: " + searchText2);
        for (Posting posting : query.parseQuery(searchText2).getPostings(index)) {
            System.out.print("Document ID: " + posting.getDocumentId() + " Positions: ");
            for (Integer positions : posting.getPositions()) {
                System.out.print(positions + ", ");
            }
            System.out.println();
        }

        // PHRASE literal test
        // search for the phrase: "about a"
        String searchText3 = "\"about a\"";// Phrase test "about a" is in ch 3 & 1 .txt files

        System.out.println("\nSearching for: " + searchText3);
        for (Posting posting : query.parseQuery(searchText3).getPostings(index)) {
            System.out.print("Document ID: " + posting.getDocumentId() + " Positions: ");
            for (Integer positions : posting.getPositions()) {
                System.out.print(positions + ", ");
            }
            System.out.println();
        }

        // PHRASE literal test
        // search for the phrase: "learn about the"
        String searchText4 = "\"learn about the\"";// Phrase test "learn about the" showed up 5 times for me

        System.out.println("\nSearching for: " + searchText4);
        for (Posting posting : query.parseQuery(searchText4).getPostings(index)) {
            System.out.print("Document ID: " + posting.getDocumentId() + " Positions: ");
            for (Integer positions : posting.getPositions()) {
                System.out.print(positions + ", ");
            }
            System.out.println();
        }

        // //search for the term: manoa
        // System.out.println("\nSearching for: manoa");
        // //basic test for the positional inverted index
        // for (Posting posting : query.parseQuery("manoa").getPostings(index)) {
        // System.out.print("Document ID: " + posting.getDocumentId() + " Positions: ");
        // for (Integer positions : posting.getPositions()) {
        // System.out.print(positions + ", ");
        // }
        // //failed test for getting content
        // System.out.println("\nContent" +
        // corpus.getDocument(posting.getDocumentId()).toString());
        // }
        //
        // //search for the term: and
        // System.out.println("\nSearching for: and");
        // //basic test for the positional inverted index
        // for (Posting posting : query.parseQuery("and").getPostings(index)) {
        // System.out.print("Document ID: " + posting.getDocumentId() + " Positions: ");
        // for (Integer positions : posting.getPositions()) {
        // System.out.print(positions + ", ");
        // }
        // System.out.println();
        // }
        //
        // //vocab in the index test
        // System.out.println("\n" + index.getVocabulary());

    }

    private static Index indexCorpus(DocumentCorpus corpus) {

        PositionalInvertedIndex index = new PositionalInvertedIndex();// create positional index
        AdvancedTokenProcesser processor = new AdvancedTokenProcesser();// create token processor

        // Get all the documents in the corpus by calling GetDocuments().
        Iterable<Document> documents = corpus.getDocuments();

        for (Document docs : documents) {// iterate through every valid document found in the corpus

            // Tokenize the document's content by constructing an EnglishTokenStream around
            // the document's content.
            EnglishTokenStream stream = new EnglishTokenStream(docs.getContent());
            Iterable<String> tokens = stream.getTokens();// convert read data into tokens
            int wordPosition = 1;// maintain the position of the word throughout the document

            // Iterate through the tokens in the document, processing them using a
            // BasicTokenProcessor,
            for (String token : tokens) {

                List<String> word = processor.processToken(token);// convert a token to indexable terms
                index.addTerm(word, docs.getId(), wordPosition);// add word data to index
                wordPosition++;// increment word position

            }

        }

        return index;

    }

    /**
     * incorporates directory-selection and loads whatever json files found there
     * into the corpus
     *
     * @return a corpus of all documents found at the user specified directory
     */
    private static DocumentCorpus requestDirectory() {

        DocumentCorpus corpus = null;

        // user input handler
        Scanner in = new Scanner(System.in);
        String input = "";

        System.out.print("\nEnter the full directory with all the files to index: ");

        try {// store the input from the user
            input = in.nextLine();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // set the path to collect all the documents
        Path currentWorkingPath = Paths.get(input);// Sample: "C:\\Users\\rcthe\\Downloads\\School\\CECS 429
                                                   // SEO\\testing"

        // generate corpus based on files found at the directory
        corpus = DirectoryCorpus.loadTextDirectory(currentWorkingPath);

        in.close();

        return corpus;

    }

}
