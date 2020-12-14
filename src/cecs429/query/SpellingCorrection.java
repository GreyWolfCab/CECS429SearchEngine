package cecs429.query;

import cecs429.index.Index;
import cecs429.index.KGram;
import cecs429.index.Posting;
import cecs429.query.TermLiteral;
import cecs429.text.AdvancedTokenProcesser;
import mainapp.Indexer;

import java.util.*;

public class SpellingCorrection {

    public static String suggestedQuery = "";
    public static boolean discardSuggested = false;

    public static void performSpellCheck(Index index, KGram kGramIndex, String stemmedTerm, String term) {

        if (!index.getVocabulary().contains(stemmedTerm)) {
            discardSuggested = true;
            // find alterative terms to use (spelling correction)
            List<String> kgrams = kGramIndex.getGrams(Indexer.K_GRAM_LIMIT, term);
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
            if (getSuggestedQuery().equals("")) {
                setSuggestedQuery(lowestEditDistanceTerm);
            } else {
                setSuggestedQuery(getSuggestedQuery() + " " + lowestEditDistanceTerm);
            }
        } else {
            if (getSuggestedQuery().equals("")) {
                setSuggestedQuery(term);
            } else {
                setSuggestedQuery(getSuggestedQuery() + " " + term);
            }
        }

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

    public static String getSuggestedQuery() {
        return suggestedQuery;
    }

    public static void setSuggestedQuery(String query) {
        suggestedQuery = query;
    }

}
