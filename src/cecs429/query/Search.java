package cecs429.query;

import cecs429.documents.DocumentCorpus;
import cecs429.index.Index;
import cecs429.index.KGram;
import cecs429.index.Posting;
import cecs429.rankedretrieval.SpellingCorrection;
import cecs429.text.AdvancedTokenProcesser;
import testing.Accumulator;

import java.util.*;

public class Search {

    private static final int RANKED_RETURN = 50;
    private static final double VOCAB_ELIMINATION_THRESHOLD = 0;// 3 because it is the best

    public String performSearch(DocumentCorpus corpus, Index index, KGram kGramIndex, String queryValue, Boolean isBooleanQuery) {

        StringBuilder postingsRows = new StringBuilder();
        String result = "";

        int[] relevantDocsCran = {52, 185, 487, 13, 880, 14, 877, 103, 57, 876, 379};

        System.out.println("Starting Query...");//calculate how long it takes to execute
        double queryRuntime;
        long startTime = System.nanoTime();

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

            PriorityQueue<Accumulator> pq = userRankedQueryInput(corpus, index, kGramIndex, queryValue);
            String suggestedQuery = SpellingCorrection.getSuggestedQuery();

            int pqSize = pq.size();
            while(!pq.isEmpty()){
                Accumulator currAcc = pq.poll();
                String title = corpus.getDocument(currAcc.getDocId()).getTitle();
                int docId = currAcc.getDocId();
                for (int i = 0; i < relevantDocsCran.length; i++) {
                    if (docId == relevantDocsCran[i]-1) {
                        System.out.println(relevantDocsCran[i] + " was included.");
                    }
                }
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
        System.out.println("Query Time: " + queryRuntime + " seconds\n");

        return result;

    }

    public static PriorityQueue<Accumulator> userRankedQueryInput(DocumentCorpus corpus, Index index, KGram kGramIndex, String queryInput){
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
