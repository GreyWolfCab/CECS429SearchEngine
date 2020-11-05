package mainapp;

import cecs429.documents.DirectoryCorpus;
import cecs429.documents.Document;
import cecs429.documents.DocumentCorpus;
import cecs429.index.*;
import cecs429.query.BooleanQueryParser;
import cecs429.text.AdvancedTokenProcesser;
import cecs429.text.EnglishTokenStream;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.io.IOException;
import java.io.Reader;


public class Indexer {

    public final static int K_GRAM_LIMIT = 3;
    public double timeToBuildIndex = 0.00;
    public Index index;

    public void main(String[] args) {

        runMainApp();

        //AND query test
        //search for the terms: bird AND seed AND science
        String searchText1 = "bird seed science";

        //OR query test
        //search for the terms: hawaii OR manoa OR goose
        String searchText2 = "hawaii + manoa + goose";

        //PHRASE literal test
        //search for the phrase: "about a"
        String searchText3 = "\"about a\"";//Phrase test "about a" is in ch 3 & 1 .txt files

        //PHRASE literal test
        //search for the phrase: "learn about the"
        String searchText4 = "\"learn about the\"";//Phrase test "learn about the" showed up 5 times for me

        //TERM literal test
        //search for the term: manoa
        String searchText5 = "manoa";

        /**
         * PHrase terms: cano*
         * $cano$ $canoa$ $canoe$ $canoe-building$ $canoe-men$ $canoe-only$ $canoe-shaped$ $canoe-yard$ $canoe/boat$ $canoe/kayak$ $canoe/kayak/backcountry$ $canoe/kayak/paddleboard/backcountry$ $canoe/kayak/paddleboard/hydrobike/backcountry$ $canoe/kayak/raft$ $canoe/kayaking$ $canoe:visitors$ $canoed$ $canoein$ $canoeing$ $canoeing.Γ$ $canoeing/kayaking$ $canoeist$ $canoeists$ $canoemen$ $canoemobile$ $canoer$ $canoers$ $canoes$ $canoes...they$ $canoes.Γ$ $canoes/kayaks$ $canoes;and$ $canoesΓ$ $canoethere$ $canoeΓÇöthe$ $canola$ $canon$ $canonchet$ $canoncito$ $canonica$ $canonicus$ $canonization$ $canonized$ $canons$ $canonvb$ $canonvb-h41$ $canooe$ $canopi$ $canopied$ $canopies$ $canopy$ $canopy.about$ $canopy.despite$ $canopy;listen$ $canorus$ $canorus)ΓÇöft/csc$ $canot$ $canova$
         */

    }

    /**
     *  used by the console to trigger the start of the application
     */
    private void runMainApp() {

        DocumentCorpus corpus = requestDirectory("");//collect all documents from a directory
        String indexLocation = "Know Where";

        KGramIndex kGramIndex = new KGramIndex();//build k-gram from 1 to limit sized grams
        Index index = timeIndexBuild(corpus, kGramIndex, indexLocation);//build the index and print how long it takes

        userQuery(corpus, index, kGramIndex);//handle user input

    }

    public static Index indexCorpus(DocumentCorpus corpus, KGramIndex kGramIndex, String indexLocation) {

        PositionalInvertedIndex index = new PositionalInvertedIndex();//create positional index
        AdvancedTokenProcesser processor = new AdvancedTokenProcesser();//create token processor

        DiskIndexWriter diskIndexWriter = new DiskIndexWriter();
        ArrayList<Double> documentWeight = new ArrayList<>();

        // Get all the documents in the corpus by calling GetDocuments().
        Iterable<Document> documents = corpus.getDocuments();

        for (Document docs : documents) {//iterate through every valid document found in the corpus

            HashMap<String, Integer> termFrequency = new HashMap<>();//term frequency of every term in a document

            // Tokenize the document's content by constructing an EnglishTokenStream around the document's content.
            EnglishTokenStream stream = new EnglishTokenStream(docs.getContent());
            Iterable<String> tokens = stream.getTokens();//convert read data into tokens
            int wordPosition = 1;//maintain the position of the word throughout the document

            // Iterate through the tokens in the document, processing them using a BasicTokenProcessor,
            for (String token : tokens) {

                List<String> words = processor.processToken(token);//convert a token to indexable terms
                for (int i = 0; i < words.size(); i++) {//iterate through all unstemmed tokens
                    kGramIndex.addGram(K_GRAM_LIMIT, words.get(i));//build k-gram off of un-stemmed tokens
                    words.set(i, AdvancedTokenProcesser.stemToken(words.get(i)));
                    if (termFrequency.containsKey(words.get(i))) {//if term is duplicate
                        int prevFrequency = termFrequency.get(words.get(i));
                        termFrequency.put(words.get(i), prevFrequency + 1);//increment term frequency counter
                    } else {
                        termFrequency.put(words.get(i), 1);//add new term to frequency counter
                    }
                }
                index.addTerm(words, docs.getId(), wordPosition, docs.getTitle());//add word data to index
                wordPosition++;//increment word position

            }

            double sumTermWeights = 0;//sum of term weights
            ArrayList<Integer> tf_d = new ArrayList<>(termFrequency.values());//every term frequency in the document

            for (int i = 0; i < tf_d.size(); i++) {//iterate through all term frequencies
                double w_dt = 1 + Math.log(tf_d.get(i));//weight of specific term in a document
                w_dt = Math.pow(w_dt, 2);
                sumTermWeights += w_dt;//summation of w_dt^2
            }
            //do math to get L_d
            double l_d = Math.sqrt(sumTermWeights);//square root normalized w_dt's
            documentWeight.add(l_d);

        }

        //write document weights to disk
        diskIndexWriter.writeDocumentWeights(documentWeight, indexLocation);

        return index;

    }

    /**
     * incorporates directory-selection and loads whatever json files found there into the corpus
     * @return a corpus of all documents found at the user specified directory
     */
    public static DocumentCorpus requestDirectory(String path) {

        DocumentCorpus corpus;

        if (path.equals("")) {//if the path is empty request it from the user
            //user input handler
            Scanner in = new Scanner(System.in);

            System.out.print("\nEnter the full directory with all the files to index: ");

            try {//store the input from the user
                path = in.nextLine();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //set the path to collect all the documents
        Path currentWorkingPath = Paths.get(path);//Sample: "C:\\Users\\rcthe\\Downloads\\School\\CECS 429 SEO\\testing"

        //generate corpus based on files found at the directory
        corpus = DirectoryCorpus.loadTextDirectory(currentWorkingPath);

        return corpus;

    }


    public String userSQueryStem (String queryInput){
        return AdvancedTokenProcesser.stemToken(queryInput.substring(6));
    }

    public List<String> userSQueryVocab () {
        return index.getVocabulary();
    }

    public static List<Posting> userQueryInput(DocumentCorpus corpus, Index index, KGramIndex kGramIndex, String queryInput) {
        BooleanQueryParser query = new BooleanQueryParser();
        List<Posting> postings = query.parseQuery(queryInput).getPostings(index, kGramIndex);

        if (postings == null) {//term not found
            System.out.println("No such term found...");
        } else {//the term is in the index
            //print each document associated with the query
            for (Posting posting : postings) {
                System.out.printf("Document ID: %-9s Title: %s", posting.getDocumentId(),
                        corpus.getDocument(posting.getDocumentId()).getTitle());
                System.out.println();
            }
            System.out.println("\nTotal Documents: " + postings.size());//print total documents found

        }
        return postings;
    }

    /**
     * method for collecting a users query via the console
     * @param corpus the documents parsed through by the index
     * @param index a collection of terms and documents
     * @param kGramIndex a collection of characters in sequence associated with complete terms
     */
    private void userQuery(DocumentCorpus corpus, Index index, KGramIndex kGramIndex) {

        //collect input from the user for a query
        try (Scanner in = new Scanner(System.in)) {

            BooleanQueryParser query = new BooleanQueryParser();
            String input = "";

            //repeatedly ask the user for a query
            while(!input.equals(":q")) {

                System.out.print("\nEnter a valid query (:q to end): ");
                input = in.nextLine();

                if (input.charAt(0) == ':') {//determine if the query is a special query

                    //special query to quit
                    if (input.length() == 2 && input.substring(1, 2).equals("q")) {
                        System.out.println("\nEnding program...");
                        //stem the given token
                    } else if (input.length() >= 5 && input.substring(1, 5).equals("stem")) {
                        String stemmedTerm = AdvancedTokenProcesser.stemToken(input.substring(6));
                        System.out.printf("%s stemmed to: %s", input.substring(6), stemmedTerm);
                        System.out.println();
                        //build a new index from the given directory
                    } else if (input.length() >= 6 && input.substring(1, 6).equals("index")) {
                        System.out.println("Resetting the directory...");
                        corpus = requestDirectory(input.substring(7));//collect all documents from a directory
                        kGramIndex = new KGramIndex();
                        index = timeIndexBuild(corpus, kGramIndex, input.substring(7));
                        //print the first 1000 terms in the vocabulary
                    } else if (input.length() == 6 && input.substring(1, 6).equals("vocab")) {
                        printIndexVocab(index);
                    } else {
                        System.out.println("\nThis is not a valid special query...");
                    }

                } else {//handle typical term query

                    //collect postings of the query
                    List<Posting> postings = query.parseQuery(input).getPostings(index, kGramIndex);

                    if (postings == null) {//term not found
                        System.out.println("No such term found...");
                    } else {//the term is in the index
                        //print each document associated with the query
                        for (Posting posting : postings) {
                            System.out.printf("Document ID: %-9s Title: %s", posting.getDocumentId(),
                                    corpus.getDocument(posting.getDocumentId()).getTitle());
                            System.out.println();
                        }
                        System.out.println("\nTotal Documents: " + postings.size());//print total documents found

                        int requestId = 0;//handle user request to view document content
                        while (requestId != -1) {//determine if the user wants to view a document
                            System.out.print("\nEnter a Document ID to view that documents' content " +
                                    "(-1 for another query): ");
                            input = in.nextLine();
                            try {//convert user input into a valid integer
                                requestId = Integer.parseInt(input);
                                //if the id is valid give the content
                                if (requestId >= 0 && requestId < corpus.getCorpusSize()) {
                                    printDocument(corpus.getDocument(requestId));//print document title and content
                                }

                                //user entered id is not valid
                            } catch (NumberFormatException nfe) {
                                System.out.println("Invalid Document Id, going back to query\n");
                                requestId = -1;
                            }

                        }

                        input = "";
                    }

                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * print the first 1000 terms in the index
     * @param index the index to grab the vocabulary from
     */
    private static void printIndexVocab(Index index) {

        int i = 0;
        for (String term : index.getVocabulary()) {
            if (i >= 1000) {//print only 1000 words before ending
                System.out.println("Vocabulary Size: " + index.getVocabulary().size());
                break;
            } else {
                System.out.println(term);//print the term on a single line
            }
            i++;
        }

    }

    /**
     * takes a document, prints the title, handles the reader returned by getContent() in Document.java, and
     * prints the content of the document.
     * @param document the document to get the information from to print
     */
    private static void printDocument(Document document) {

        System.out.println("\nTitle: " + document.getTitle());//print the title
        Reader reader = document.getContent();//grab the reader
        StringBuilder content = new StringBuilder();
        int readerCharValue;
        try {
            while ((readerCharValue = reader.read()) != -1) {//read each char from the reader
                content.append((char)readerCharValue);//convert the value to a char, add to builder
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        System.out.println("Content: " + content.toString());//print document information

    }

    /**
     * calculates the time to build an index, also calls the function to build an index
     * @param corpus the corpus to build an index from
     * @return the completed index
     */
    public Index timeIndexBuild(DocumentCorpus corpus, KGramIndex kGramIndex, String indexLocation) {


        System.out.println("Starting to build index...");

        //measure how long it takes to build the index
        long startTime = System.nanoTime();
        index = indexCorpus(corpus, kGramIndex, indexLocation);
        long stopTime = System.nanoTime();
        double indexSeconds = (double)(stopTime - startTime) / 1_000_000_000.0;
        System.out.println("Done!\n");
        System.out.println("Time to build index: " + indexSeconds + " seconds");
        this.setTimeToBuildIndex(indexSeconds);
        return index;

    }

    public double getTimeToBuildIndex() {
        return timeToBuildIndex;
    }

    public void setTimeToBuildIndex(double timeToBuildIndex) {
        this.timeToBuildIndex = timeToBuildIndex;
    }


}
