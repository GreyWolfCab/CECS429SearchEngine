package cecs429.index;

import cecs429.documents.Document;
import cecs429.documents.DocumentCorpus;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ClusterPruningIndex {

    private ArrayList<Integer> leaders;


    // create sqrt(N) leaders (randomly)
    public void chooseLeaders(DocumentCorpus corpus, Index index){
        int n = corpus.getCorpusSize();
        double numOfLeaders = Math.sqrt(n);
        Random rand = new Random();
        int randomID;

        // create list of document leader ids
        for( int i = 0; i < numOfLeaders;  ) {
            randomID = rand.nextInt(n + 1);
            leaders.add(randomID);
        }
    }
    // compute the nearest leader for every document in the corpus (saving information on-disk)
    public int computeNearestLeader(Index index, int docId){
        int nearestLeader = -1;
        double minDistance = Double.POSITIVE_INFINITY;
        if(leaders == null) {
            return -1;
        }
        // currently finding nearest leader based on smallest document weight difference ??
        for (int leaderId : leaders){
            double currentDistance = index.getDocumentWeight(leaderId) - index.getDocumentWeight(docId);
            if (currentDistance < minDistance) {
                minDistance = currentDistance;
                nearestLeader = leaderId;
            }
        }

        return nearestLeader;
    }

    // store nearestleader on disk
    public void ClusterPruningIndexer(DocumentCorpus corpus, Index index) {
        chooseLeaders(corpus, index);
        Iterable<Document> documents = corpus.getDocuments();
        int leaderId = 0;

        for (Document doc : documents){
            int docId = doc.getId();
            leaderId = computeNearestLeader(index, docId);
            // store leaderId on disk..

            // ???
        }

    }
}
