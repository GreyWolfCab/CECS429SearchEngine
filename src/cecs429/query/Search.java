package cecs429.query;

import cecs429.documents.DocumentCorpus;
import cecs429.index.Index;
import cecs429.index.KGram;
import cecs429.index.Posting;
import cecs429.text.AdvancedTokenProcesser;
import testing.Accumulator;

import java.util.*;

public class Search {

    private static final int RANKED_RETURN = 50;
    private static final double VOCAB_ELIMINATION_THRESHOLD = 3;// 3 because it is the best
    private final int TEST_ITERATIONS = 30;
    private double queryTime = 0.0;

    public String performSearch(DocumentCorpus corpus, Index index, KGram kGramIndex, String queryValue, ArrayList<Integer> leaders, Boolean isBooleanQuery, Boolean testThroughput) {

        StringBuilder postingsRows = new StringBuilder();
        String result = "";
        int testIterations = 1;

        System.out.println("Starting Query...");//calculate how long it takes to execute
        double queryRuntime;
        long startTime = System.nanoTime();

        if(testThroughput == true) {
            testIterations = TEST_ITERATIONS;
        }
        for(int iteration = 0; iteration < testIterations; iteration++) {
            if (isBooleanQuery) {//process a boolean query
                List<Posting> postings = userBooleanQueryInput(corpus, index, kGramIndex, queryValue);


                for (Posting post : postings) {//include document titles for each returned posting

                    String title = corpus.getDocument(post.getDocumentId()).getTitle();
                    String row = "    <tr>\n" +
                            "        <td>"+post.getDocumentId()+"</td>\n" +
                            "        <td><button id=\"" + post.getDocumentId() + "\" onClick=\"docClicked(this.id)\" >"+title+"</button></td>\n" +
                            "        <td>"+post.getPositions()+"</td>\n" +
                            "    </tr>\n";
                    postingsRows.append(row);

                }

                result = "<div><b>Query: </b>" + queryValue +
                        "<div>Total Documents: " + postings.size() + "</div></div></br>" +
                        "<table style=\"width:100%\">\n" +
                        "    <tr>\n" +
                        "        <th>Document ID</th>\n" +
                        "        <th>Document Title</th>\n" +
                        "        <th>Positions</th>\n" +
                        "    </tr>\n" +
                        postingsRows.toString() +
                        "</table>";

            } else {

                PriorityQueue<Accumulator> pq;
                if(leaders == null) {
                    pq = userRankedQueryInput(corpus, index, kGramIndex, queryValue);
                } else {
                    pq = userRankedQueryInput(corpus, index, kGramIndex, queryValue, leaders);
                }
                String suggestedQuery = SpellingCorrection.getSuggestedQuery();

                int pqSize = pq.size();
                while(!pq.isEmpty()){
                    Accumulator currAcc = pq.poll();
                    String title = corpus.getDocument(currAcc.getDocId()).getTitle();
                    int docId = currAcc.getDocId() + 1;
                    docId--;
                    double value = currAcc.getA_d();
                    String row = "    <tr>\n" +
                            "        <td>"+docId+"</td>\n" +
                            "        <td><button id=\"" + docId + "\" onClick=\"docClicked(this.id)\" >"+title+"</button></td>\n" +
                            "        <td>"+value+"</td>\n" +
                            "    </tr>\n";
                    postingsRows.insert(0,row);
                }

                result = "<div><b>Top " + RANKED_RETURN + " Results for: </b>" + queryValue +
                        "<div>Suggested Query: <button id=\"spelling-correction-btn\" onClick=\"suggestedQueryClicked(this.value)\">" + suggestedQuery + "</button></div>" +
                        "<div>Total Documents: " + pqSize + "</div></div></br>" +
                        "<table style=\"width:100%\">\n" +
                        "    <tr>\n" +
                        "        <th>Document Id</th>\n" +
                        "        <th>Document Title</th>\n" +
                        "        <th>Score</th>\n" +
                        "    </tr>\n" +
                        postingsRows.toString() +
                        "</table>";

            }

            long stopTime = System.nanoTime();
            queryRuntime = (double)(stopTime - startTime) / 1_000_000_000.0;
            setQueryTime(queryTime + queryRuntime);
            System.out.println("Query Time: " + queryRuntime + " seconds\n");
        }
        return result;

    }

    public double testSearch(DocumentCorpus corpus, Index index, KGram kGramIndex, String queryValue, ArrayList<Integer> leaders, Boolean isBooleanQuery, Boolean testThroughput, int[] relDocs) {

        System.out.println("Starting Query...");//calculate how long it takes to execute
        double queryRuntime;
        long startTime = System.nanoTime();

        PriorityQueue<Accumulator> pq;
        if(leaders == null) {
            pq = userRankedQueryInput(corpus, index, kGramIndex, queryValue);
        } else {
            pq = userRankedQueryInput(corpus, index, kGramIndex, queryValue, leaders);
        }

        System.out.println("Query: " + queryValue.substring(0, queryValue.length()-2));
        System.out.print("Relevant: ");

        double relevantSum = 0;
        int relevantIndex = 0;
        int totalRelevantDocs = 0;
        while(!pq.isEmpty()){
            Accumulator currAcc = pq.poll();
            relevantIndex++;
            String title = corpus.getDocument(currAcc.getDocId()).getTitle();
            int docId = currAcc.getDocId() + 1;
            for (int i = 0; i < relDocs.length; i++) {
                if (relDocs[i] == docId) {
                    System.out.print(docId + ", ");
                    totalRelevantDocs++;
                    relevantSum += (double) totalRelevantDocs / relevantIndex;
                    break;
                }
            }

        }

        double avgPrecision = ((double)1/relDocs.length) * relevantSum;

        System.out.println();

        long stopTime = System.nanoTime();
        queryRuntime = (double)(stopTime - startTime) / 1_000_000_000.0;
        setQueryTime(queryTime + queryRuntime);
        System.out.println("Query Time: " + queryRuntime + " seconds");
        System.out.println("Average Precision: " + avgPrecision + "\n");

        return avgPrecision;

    }

    //ranked query with cluster pruning
    public static PriorityQueue<Accumulator> userRankedQueryInput(DocumentCorpus corpus, Index index, KGram kGramIndex, String queryInput, ArrayList<Integer> leaders) {

        double n = corpus.getCorpusSize();
        List<TermLiteral> termLiterals = new ArrayList<TermLiteral>();
        int counter = 0;
        List<Posting> postings = new ArrayList<Posting>();
        HashMap<Posting, Double> hm = new HashMap<>();
        PriorityQueue<Accumulator> pq = new PriorityQueue<>(RANKED_RETURN);

        String[] terms = queryInput.split(" ");
        SpellingCorrection.setSuggestedQuery("");
        SpellingCorrection.discardSuggested = false;

        for (String term : terms) { // find the leader that fulfills the query
            term = term.toLowerCase();
            String stemmedTerm = AdvancedTokenProcesser.stemToken(term);
            termLiterals.add(new TermLiteral(stemmedTerm));

            int df_t = index.getDocumentFrequencyOfTerm(stemmedTerm);
            double w_qt = Math.log(1 + n/df_t);  // calculate wqt = ln(1 + N/dft)

            postings = termLiterals.get(counter).getPostings(index, kGramIndex);
            counter++;
            double tf_td = (double) index.getTermFrequency(stemmedTerm) / (double) postings.size();
            for(Posting p : postings){ // for each document in postings list
                if (leaders.contains(p.getDocumentId())) {
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
        }

        List<Accumulator> accumulators = new ArrayList<Accumulator>();
        hm.forEach((key,value) -> accumulators.add(new Accumulator(key.getDocumentId(),value)));
        for (Accumulator acc : accumulators){
            // only retain the a certain amount of the top results
            double value = acc.getA_d() / index.getDocumentWeight(acc.getDocId());
            acc.setA_d(value);
            if(pq.size() < RANKED_RETURN || pq.peek().getA_d() < acc.getA_d()){
                if(pq.size() == RANKED_RETURN){
                    pq.remove();
                }
                pq.add(acc);
            }
        }

        int leaderId = 0;
        while(!pq.isEmpty()) {
            leaderId = pq.poll().getDocId();
        }

        ArrayList<Integer> followers = index.getDocumentFollowers(leaderId);
        counter = 0;
        postings = new ArrayList<Posting>();
        termLiterals = new ArrayList<TermLiteral>();

        for (String term : terms) { // order the followers that fulfills the query
            term = term.toLowerCase();
            String stemmedTerm = AdvancedTokenProcesser.stemToken(term);
            termLiterals.add(new TermLiteral(stemmedTerm));

            int df_t = index.getDocumentFrequencyOfTerm(stemmedTerm);
            double w_qt = Math.log(1 + n/df_t);  // calculate wqt = ln(1 + N/dft)

            postings = termLiterals.get(counter).getPostings(index, kGramIndex);
            counter++;
            double tf_td = (double) index.getTermFrequency(stemmedTerm) / (double) postings.size();
            for(Posting p : postings){ // for each document in postings list
                if (followers.contains(p.getDocumentId())) {
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
        }

        List<Accumulator> followersAccumulators = new ArrayList<Accumulator>();
        hm.forEach((key,value) -> accumulators.add(new Accumulator(key.getDocumentId(),value)));
        for (Accumulator acc : accumulators){
            // only retain the a certain amount of the top results
            double value = acc.getA_d() / index.getDocumentWeight(acc.getDocId());
            acc.setA_d(value);
            if(pq.size() < RANKED_RETURN || pq.peek().getA_d() < acc.getA_d()){
                if(pq.size() == RANKED_RETURN){
                    pq.remove();
                }
                pq.add(acc);
            }
        }

        return pq;

    }

    //ranked query with vocab elimination
    public static PriorityQueue<Accumulator> userRankedQueryInput(DocumentCorpus corpus, Index index, KGram kGramIndex, String queryInput) {
        double n = corpus.getCorpusSize();
        List<TermLiteral> termLiterals = new ArrayList<TermLiteral>();
        int counter = 0;
        List<Posting> postings = new ArrayList<Posting>();
        HashMap<Posting, Double> hm = new HashMap<>();
        PriorityQueue<Accumulator> pq = new PriorityQueue<>(RANKED_RETURN);

        String[] terms = queryInput.split(" ");
        SpellingCorrection.setSuggestedQuery("");
        SpellingCorrection.discardSuggested = false;

        for (String term : terms) { // for each term in query
            term = term.toLowerCase();
            String stemmedTerm = AdvancedTokenProcesser.stemToken(term);
            termLiterals.add(new TermLiteral(stemmedTerm));

            SpellingCorrection.performSpellCheck(index, kGramIndex, stemmedTerm, term);

            int df_t = index.getDocumentFrequencyOfTerm(stemmedTerm);
            double w_qt = Math.log(1 + n/df_t);  // calculate wqt = ln(1 + N/dft)
            //not as accurate, but saves us from thousands of disk reads

            if (w_qt < VOCAB_ELIMINATION_THRESHOLD) {//wqt is too small to be included in results
                //skip this term
            } else {
                postings = termLiterals.get(counter).getPostings(index, kGramIndex);
                counter++;
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

        }

        if (!SpellingCorrection.discardSuggested) {
            SpellingCorrection.setSuggestedQuery("");
        }

        List<Accumulator> accumulators = new ArrayList<Accumulator>();
        hm.forEach((key,value) -> accumulators.add(new Accumulator(key.getDocumentId(),value)));
        for (Accumulator acc : accumulators){
            // only retain the a certain amount of the top results
            double value = acc.getA_d() / index.getDocumentWeight(acc.getDocId());
            acc.setA_d(value);
            if(pq.size() < RANKED_RETURN || pq.peek().getA_d() < acc.getA_d()){
                if(pq.size() == RANKED_RETURN){
                    pq.remove();
                }
                pq.add(acc);
            }
        }

        return pq;
    }

    public double getQueryTime() {
        return queryTime;
    }

    public void setQueryTime(double queryTime) {
        this.queryTime = queryTime;
    }

    public int getTEST_ITERATIONS() {
        return TEST_ITERATIONS;
    }

    public static void getAveragePrecision() {
        // 1 / (total relevant docs) * (summation by iterate through all results) isRelevant(i) * (precision of the doc)
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

}
