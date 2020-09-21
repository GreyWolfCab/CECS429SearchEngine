package cecs429.text;
import org.tartarus.snowball.ext.englishStemmer;

public class AdvancedTokenProcesser implements TokenProcessor{


    @Override
    public String processToken(String token) {

        //remove all non-alphanumeric characters from the beginning and end



        //remove all apostrophes or quotation marks (single or double) from anywhere in the string



        //remove hyphens and split up the original hyphenated token into multiple tokens (returns combined, and separated strings)


        //convert token to lowercase


        //stem token using implementation of the Porter2stemmer

        englishStemmer stemmer = new englishStemmer();

        return null;
    }
}
