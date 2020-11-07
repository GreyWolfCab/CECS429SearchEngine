package testing;

import cecs429.documents.Document;
import cecs429.documents.DocumentCorpus;
import cecs429.index.DiskPositionalIndex;
import cecs429.index.Index;
import cecs429.index.KGramIndex;
import cecs429.index.Posting;
import cecs429.query.TermLiteral;
import cecs429.text.EnglishTokenStream;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Accumulator implements Comparable<Accumulator> {

    private int docId;
    private double A_d;

    public Accumulator(int id, double ad) {
        this.docId = id;
        this.A_d = ad;
    }

    public int getDocId() {
        return docId;
    }

    public void setDocId(int docId) {
        this.docId = docId;
    }

    public double getA_d() {
        return A_d;
    }

    public void setA_d(double a_d) {
        A_d = a_d;
    }

    @Override
    public int compareTo(Accumulator acc) {
        if(this.A_d < acc.getA_d()){
            return -1;
        }
        else if (acc.getA_d() < this.A_d){
            return 1;
        }
        return 0;
    }
}

class test {
    public static void main(String[] args) {
        Queue<Integer> largest = new PriorityQueue<>(10);
        int[] arr = new Random().ints(20, 0, 100).toArray();
        for (int item : arr) {
            if (largest.size() < 10 || largest.peek() < item) {
                if (largest.size() == 10)
                    largest.remove();
                largest.add(item);
            }
        }
        for (int num : arr) {
            System.out.print(num + " ");
        }
        System.out.println(largest);


    }

    /***
    public static Queue<Accumulator> userRankedQueryInput(DocumentCorpus corpus, DiskPositionalIndex index, KGramIndex kGramIndex, String queryInput){
        double n = corpus.getCorpusSize();
        List<TermLiteral> termLiterals = null;
        int counter = 0;
        List<Posting> postings = null;
        List<Accumulator> accumulators = null;
        Queue<Accumulator> pq = new PriorityQueue<>(10);

        List<String> terms = Arrays.asList(queryInput.split(" "));

        for (String term : terms) { // for each term in query
            termLiterals.add(new TermLiteral(term));
            postings = termLiterals.get(counter).getPostings(index, kGramIndex);
            double w_qt = Math.log(n/postings.size());  // calculate wqt = ln(1 + N/dft)

            for(Posting p : postings){ // for each document in postings list
                Document d = corpus.getDocument(p.getDocumentId());
                double w_dt = index.getDocumentWeight(d.getId());
                double a_d = w_dt * w_qt;
                //accumulators[p.getDocumentId()] = accumulators[p.getDocumentId()] + a_d;
                accumulators.add(new Accumulator(d.getId(),a_d));
            }
        }
        for (Accumulator acc : accumulators){
            if(pq.size() < 10 || pq.peek().getA_d() < acc.getA_d()){
                if(pq.size() == 10){
                    pq.remove();
                }
                pq.add(acc);
            }
        }

        return pq;
    }
     **/
}


