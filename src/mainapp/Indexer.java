package mainapp;

import cecs429.documents.DirectoryCorpus;
import cecs429.documents.Document;
import cecs429.documents.DocumentCorpus;
import cecs429.index.*;
import cecs429.query.BooleanQueryParser;
import cecs429.query.TermLiteral;
import cecs429.text.AdvancedTokenProcesser;
import cecs429.text.EnglishTokenStream;
import testing.Accumulator;

import java.nio.file.*;
import java.util.*;


public class Indexer {

    private final static double TERM_DOC_FREQ_THRESHOLD = 5.00;
    public final static int K_GRAM_LIMIT = 3;
    public static String suggestedQuery = "";

    public static Index indexCorpus(DocumentCorpus corpus, KGram kGramIndex, String indexLocation) {

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
        diskIndexWriter.writeKGramIndex(kGramIndex, indexLocation);
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
        Path currentWorkingPath = Paths.get(path);

        //generate corpus based on files found at the directory
        corpus = DirectoryCorpus.loadTextDirectory(currentWorkingPath);

        return corpus;

    }


    public String userSQueryStem (String queryInput){
        return AdvancedTokenProcesser.stemToken(queryInput.substring(6));
    }

    public List<String> userSQueryVocab (Index index) {
        return index.getVocabulary();
    }

    public static List<Posting> userBooleanQueryInput(DocumentCorpus corpus, Index index, KGram kGramIndex, String queryInput) {
        BooleanQueryParser query = new BooleanQueryParser();
        List<Posting> postings = query.parseQuery(queryInput).getPostings(index, kGramIndex);

        corpus.getDocuments();//corpus doesn't exist if we don't include this line. (I have no idea)
        //print each document associated with the query
        for (Posting posting : postings) {
            System.out.printf("Document ID: %-9s Title: %s", posting.getDocumentId(),
                    corpus.getDocument(posting.getDocumentId()).getTitle());
            System.out.println();
        }
        System.out.println("\nTotal Documents: " + postings.size());//print total documents found

        return postings;
    }

    /**
     * Calculate the Levenshtein Edit Distance of two strings
    **/
    public static int calculateEditDistance(String x, String y) {
        int[][] dp = new int[x.length() + 1][y.length() + 1]; // create matrix of length x and y strings (mxn)

        for (int n = 0; n <= x.length(); n++) {
            for (int m = 0; m <= y.length(); m++) {
                if (n == 0) {
                    dp[n][m] = m;
                }
                else if (m == 0) {
                    dp[n][m] = n;
                }
                else {
                    dp[n][m] = min(dp[n - 1][m - 1]
                                    + substitution(x.charAt(n - 1), y.charAt(m - 1)),
                            dp[n - 1][m] + 1,
                            dp[n][m - 1] + 1);
                }
            }
        }

        return dp[x.length()][y.length()];
    }

    public static int substitution(char a, char b) {
        return a == b ? 0 : 1;
    }

    public static int min(int... numbers) {
        return Arrays.stream(numbers)
                .min().orElse(Integer.MAX_VALUE);
    }

    public static double getJaccardCoefficient(String firstTerm, String secondTerm, KGram kGramIndex) {

        //build list of grams for both terms
        List<String> firstGrams = new ArrayList<>();
        List<String> secondGrams = new ArrayList<>();

        for (int i = Indexer.K_GRAM_LIMIT; i > 0; i--) {//get every possible gram for both terms
            firstGrams.addAll(kGramIndex.getGrams(i, firstTerm));
            secondGrams.addAll(kGramIndex.getGrams(i, secondTerm));
        }

        Collections.sort(firstGrams);
        Collections.sort(secondGrams);//sort the gram lists
        int similarGrams = 0;
        int i = 0, j = 0;

        while (i < firstGrams.size() && j < secondGrams.size()) {//iterate through both lists until one ends

            int match = firstGrams.get(i).compareTo(secondGrams.get(j));//compare current grams

            if (match == 0) {//count how many grams match between both terms
                similarGrams++;
                i++;
                j++;
            } else if (match < 0) {
                i++;
            } else {
                j++;
            }

        }

        //Jaccard Coefficient = (A⋂B) / A + B - (A⋂B)
        return ((double)similarGrams / (double)(firstGrams.size() + secondGrams.size() - similarGrams));

    }

    public static PriorityQueue<Accumulator> userRankedQueryInput(DocumentCorpus corpus, DiskPositionalIndex index, KGram kGramIndex, String queryInput){
        double n = corpus.getCorpusSize();
        List<TermLiteral> termLiterals = new ArrayList<TermLiteral>();
        int counter = 0;
        List<Posting> postings = new ArrayList<Posting>();
        HashMap<Posting, Double> hm = new HashMap<>();
        PriorityQueue<Accumulator> pq = new PriorityQueue<>(10);

        String[] terms = queryInput.split(" ");
        setSuggestedQuery("");
        boolean discardSuggested = false;

        for (String term : terms) { // for each term in query
            term = term.toLowerCase();
            String stemmedTerm = AdvancedTokenProcesser.stemToken(term);
            termLiterals.add(new TermLiteral(stemmedTerm));
            postings = termLiterals.get(counter).getPostings(index, kGramIndex);
            counter++;
            if (!index.getVocabulary().contains(stemmedTerm)) {
                discardSuggested = true;
                // find alterative terms to use (spelling correction)
                List<String> kgrams = kGramIndex.getGrams(K_GRAM_LIMIT, term);
                Set<String> relatedTerms = new HashSet<>();//hashset prevents duplicates
                for (String kgram : kgrams) { // add all related terms that have common k-grams
                    Set<String> currRelatedTerms = kGramIndex.getTerms(kgram);
                    if (currRelatedTerms != null) {//prevent non-existing grams null pointer exception

                        relatedTerms.addAll(currRelatedTerms);
                    }
                }

                // jaccard coefficient
                //HashMap<String, Double> termsJaccardCoefficient = new HashMap<>();
                HashMap<String, Double> termsEditDistance = new HashMap<>();
                double threshold = 0.2;//a match would be 1
                for (String relatedTerm : relatedTerms) {
                    double coefficient = getJaccardCoefficient(term, relatedTerm, kGramIndex);
                    if (coefficient >= threshold) {//track terms that surpass the threshold
                        //termsJaccardCoefficient.put(relatedTerm, coefficient);
                        // edit distance
                        double editDistance = calculateEditDistance(term, relatedTerm);
                        termsEditDistance.put(relatedTerm,editDistance);
                    }
                }
                String lowestEditDistanceTerm = "";
                double lowestEditDistanceValue = Double.MAX_VALUE;

                for (Map.Entry<String, Double> entry : termsEditDistance.entrySet()) {
                    if (entry.getValue() < lowestEditDistanceValue) {
                        // iterate and find lowest edit distance term
                        lowestEditDistanceTerm = entry.getKey();
                        lowestEditDistanceValue = entry.getValue();
                    } else if (entry.getValue() == lowestEditDistanceValue) { // if they are equal
                        // check which has highest df_t (when stemmed)
                        // stem current lowest ED term
                        String stemmedLEDT = AdvancedTokenProcesser.stemToken(lowestEditDistanceTerm);
                        TermLiteral lowestTerm = new TermLiteral(stemmedLEDT);
                        // stem current entry key term
                        String stemmedCT = AdvancedTokenProcesser.stemToken(entry.getKey());
                        TermLiteral currTerm = new TermLiteral(stemmedCT);
                        // get postings size (df_t) of each term
                        List<Posting> lowestPostings = lowestTerm.getPostings(index, kGramIndex);
                        List<Posting> currPostings = currTerm.getPostings(index, kGramIndex);
                        // compare sizes (df_t)
                        if (currPostings.size() > lowestPostings.size()){
                            lowestEditDistanceTerm = entry.getKey();
                            lowestEditDistanceValue = entry.getValue();
                        }
                    }
                }
                setSuggestedQuery(getSuggestedQuery() + " " + lowestEditDistanceTerm);
            } else {
                setSuggestedQuery(getSuggestedQuery() + stemmedTerm);
            }
            double w_qt = Math.log(1 + n/postings.size());  // calculate wqt = ln(1 + N/dft)
            //not as accurate, but saves us from thousands of disk reads
            double tf_td = (double) index.getTermFrequency(stemmedTerm) / (double) postings.size();
            for(Posting p : postings){ // for each document in postings list
                //Document d = corpus.getDocument(p.getDocumentId());//very slow
                //double tf_td = index.getTermDocumentFrequency(stemmedTerm, d.getId());//Horribly slow
                double w_dt = 1 + Math.log(tf_td);
                double a_d = (w_dt * w_qt);
                if (hm.get(p) != null) {
                    hm.put(p, hm.get(p) + a_d);
                } else {
                    hm.put(p, a_d);
                }
            }

        }

        if (!discardSuggested) {
            setSuggestedQuery("");
        }

        List<Accumulator> accumulators = new ArrayList<Accumulator>();
        hm.forEach((key,value) -> accumulators.add(new Accumulator(key.getDocumentId(),value)));
        for (Accumulator acc : accumulators){
            // only retain the top 10
            double value = acc.getA_d() / index.getDocumentWeight(acc.getDocId());
            acc.setA_d(value);
            if(pq.size() < 10 || pq.peek().getA_d() < acc.getA_d()){
                if(pq.size() == 10){
                    pq.remove();
                }
                pq.add(acc);
            }
        }

        return pq;
    }

    public static String getSuggestedQuery() {
        return suggestedQuery;
    }

    public static void setSuggestedQuery(String query) {
        suggestedQuery = query;
    }

    /**
     * calculates the time to build an index, also calls the function to build an index
     * @param corpus the corpus to build an index from
     * @return the completed index
     */
    public Index timeIndexBuild(DocumentCorpus corpus, KGram kGramIndex, String indexLocation) {
        return indexCorpus(corpus, kGramIndex, indexLocation);
    }

    public KGram buildDiskKGramIndex(String dir) {
        return new DiskKGramIndex(dir);
    }

    public DiskPositionalIndex buildDiskPositionalIndex(String dir) {
        return new DiskPositionalIndex(dir);
    }

}
