package mainapp;

import cecs429.documents.DirectoryCorpus;
import cecs429.documents.Document;
import cecs429.documents.DocumentCorpus;
import cecs429.index.*;
import cecs429.text.AdvancedTokenProcesser;
import cecs429.text.EnglishTokenStream;

import java.nio.file.*;
import java.util.*;


public class Indexer {

    private final static double TERM_DOC_FREQ_THRESHOLD = 5.00;
    public final static int K_GRAM_LIMIT = 3;

    //cranfield id's will be 1 over (i.e. in our results it will display id: 12, but it is actually id: 0013.json)

    public static Index indexCorpus(DocumentCorpus corpus, KGram kGramIndex, String indexLocation) {

        PositionalInvertedIndex index = new PositionalInvertedIndex();//create positional index
        AdvancedTokenProcesser processor = new AdvancedTokenProcesser();//create token processor
        ClusterPruningIndex clusterIndex = new ClusterPruningIndex();

        DiskIndexWriter diskIndexWriter = new DiskIndexWriter();
        ArrayList<Double> documentWeight = new ArrayList<>();

        // Get all the documents in the corpus by calling GetDocuments().
        Iterable<Document> documents = corpus.getDocuments();

        HashMap<String, Integer> mostPopularTerms = new HashMap<>();
        int currentDoc = 0;
        String[] vectorTerms = {"flow", "on", "at", "by", "that", "pressur", "an", "be", "number", "boundari", "layer", "from", "as", "result", "this", "it", "effect", "which", "method", "theori", "bodi", "solut", "heat", "wing", "mach", "equat", "shock", "use", "present", "was", "surfac", "distribut", "obtain", "two", "temperatur", "ratio", "been", "problem", "were", "veloc", "approxim", "calcul", "case", "have", "test", "plate", "investig", "given", "condit", "speed", "these", "valu", "transfer", "wave", "or", "has", "angl", "experiment", "superson", "jet", "made", "cylind", "edg", "rang", "measur", "laminar", "found", "load", "can", "stream", "lift", "determin", "coeffici", "analysi", "over", "increas", "general", "reynold", "wall", "free", "base", "high", "point", "turbul", "dimension", "also", "between", "some", "hyperson", "stress", "shown", "than", "buckl", "separ"};
        double[][] termVectorSpace = new double[corpus.getCorpusSize()][vectorTerms.length];

        for (Document docs : documents) {//iterate through every valid document found in the corpus
            currentDoc = docs.getId();
            int totalTerms = 0;
            double[] docVector = new double[vectorTerms.length];
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
                totalTerms = words.size();
            }

            /* Determine popular terms */
            int finalTotalTerms = totalTerms;
            termFrequency.forEach((key, value) -> {

                for (int j = 0; j < vectorTerms.length; j++) {
                    if (key.equals(vectorTerms[j])) {
                        docVector[j] = (double) value / finalTotalTerms;
                    }
                }

                if (mostPopularTerms.containsKey(key)) {
                    int prevFrequency = mostPopularTerms.get(key);
                    mostPopularTerms.put(key, prevFrequency + value);
                } else {
                    mostPopularTerms.put(key, 1);
                }

            });

            for (int j = 0; j < docVector.length; j++) {
                termVectorSpace[currentDoc][j] = docVector[j];
            }

            /* */

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

//        for (int i = 0; i < termVectorSpace.length; i++) {
//            System.out.print(i + ": [");
//            for (int j = 0; j < termVectorSpace[i].length; j++) {
//                System.out.print(termVectorSpace[i][j] + ", ");
//            }
//            System.out.println("]");
//
//        }

        /* Determine popular terms
        // Create a list from elements of HashMap
        List<Map.Entry<String, Integer> > list =
                new LinkedList<Map.Entry<String, Integer> >(mostPopularTerms.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<String, Integer> >() {
            public int compare(Map.Entry<String, Integer> o1,
                               Map.Entry<String, Integer> o2)
            {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        for (int i = 0; i < list.size(); i++) {
            System.out.print("\"" + list.get(i).getKey() + "\", ");//determine the most popular words in the corpus
        }

        /* */

        //write document leaders to disk
        diskIndexWriter.writeLeaderIndex(clusterIndex.chooseLeaders(corpus, termVectorSpace), corpus.getCorpusSize(), indexLocation);
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
