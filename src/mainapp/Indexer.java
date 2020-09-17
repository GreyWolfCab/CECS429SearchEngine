package mainapp;

import cecs429.documents.DirectoryCorpus;
import cecs429.documents.Document;
import cecs429.documents.DocumentCorpus;

import java.nio.file.*;
import java.util.Scanner;

public class Indexer {

    public static void main(String args[]) {

        DocumentCorpus corpus = requestDirectory();//collect all documents from a directory

        Iterable<Document> docs = corpus.getDocuments();
        for (Document doc : docs) {
            System.out.println(doc.getTitle());
        }

    }

    /**
     * incorporates directory-selection and loads whatever json files found there into the corpus
     * @return a corpus of all documents found at the user specified directory
     */
    private static DocumentCorpus requestDirectory() {

        DocumentCorpus corpus = null;

        //user input handler
        Scanner in = new Scanner(System.in);
        String input = "";

        System.out.print("\nEnter the full directory with all the files to index: ");

        try {//store the input from the user
            input = in.nextLine();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //set the path to collect all the documents
        Path currentWorkingPath = Paths.get(input);//Sample: "C:\\Users\\rcthe\\Downloads\\School\\CECS 429 SEO\\testing"

        //generate corpus based on files found at the directory
        corpus = DirectoryCorpus.loadJsonDirectory(currentWorkingPath, ".json");

        in.close();

        return corpus;

    }

}
