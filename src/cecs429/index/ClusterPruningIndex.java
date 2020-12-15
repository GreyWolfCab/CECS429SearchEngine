package cecs429.index;

import cecs429.documents.DocumentCorpus;

import java.util.*;

public class ClusterPruningIndex {

    private HashMap<Integer, double[]> leaders = new HashMap<>();


    // create sqrt(N) leaders (randomly)
    public int[] chooseLeaders(DocumentCorpus corpus, double[][] termVectorSpace){
        int n = corpus.getCorpusSize();
        int numOfLeaders = (int)Math.sqrt(n);
        Random rand = new Random();
        int randomID;

        // create list of document leader ids
        for(int i = 0; i < numOfLeaders; i++) {
            randomID = rand.nextInt(n + 1);
            if (!leaders.containsKey(randomID)) {//prevent duplicate leaders
                leaders.put(randomID, termVectorSpace[randomID]);
            } else {
                i--;
            }
        }

        return computeNearestLeader(termVectorSpace);
    }
    // compute the nearest leader for every document in the corpus (saving information on-disk)
    public int[] computeNearestLeader(double[][] termVectorSpace){

        int[] docsToLeaders = new int[termVectorSpace.length];//-1 means it's a leader
        for (int i = 0; i < termVectorSpace.length; i++) {

            if (leaders.containsKey(i)) {
                docsToLeaders[i] = -1;
            } else {
                docsToLeaders[i] = findNearestLeader(termVectorSpace[i]);
            }

        }

        return docsToLeaders;

    }

    public int findNearestLeader(double[] vectorSpace) {

        double cos_theta = Double.MIN_VALUE;//closer to 1 means it is more simlar
        int bestLeader = 0;
        for (Map.Entry<Integer, double[]> entry : leaders.entrySet()) {//find the nearest leader
            double temp_cos = calculateSimilarity(entry.getValue(), vectorSpace);
            if (cos_theta < temp_cos) {
                cos_theta = temp_cos;
                bestLeader = entry.getKey();
            }
        }

        return bestLeader;

    }

    public double calculateSimilarity(double[] lead, double[] doc) {
        //dot product
        double dotProduct = dotProduct(lead, doc);
        // product of magnitudes
        double magnitudeProduct = findMagnitude(lead) * findMagnitude(doc);

        return dotProduct / magnitudeProduct;
    }

    public double dotProduct(double[] lead, double[] doc) {

        double sum = 0;
        for (int i = 0; i < lead.length; i++)
        {
            sum += lead[i] * doc[i];
        }
        return sum;

    }

    public double findMagnitude(double[] vector) {

        double magnitude = 0;
        for (int i = 0; i < vector.length; i++) {
            magnitude += Math.pow(vector[i], 2);
        }
        return Math.sqrt(magnitude);

    }
}
