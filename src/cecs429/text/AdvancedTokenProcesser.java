package cecs429.text;

import org.tartarus.snowball.ext.englishStemmer;

import java.util.ArrayList;
import java.util.Arrays;

public class AdvancedTokenProcesser implements TokenProcessor{


    @Override
    public ArrayList<String> processToken(String token) {

        int beginIndex = 0;
        int endIndex = 0;
        String currentToken;
        ArrayList<String> result = new ArrayList<String>();

        // Remove all non-alphanumeric characters from the beginning and end of token
        String[] stringArray = token.split("");

            // Parse through string array to find beginning index
        for(int i = 0; i < stringArray.length;i++){
            if(stringArray[i].matches("^[a-zA-Z0-9]*$")){
                beginIndex = i;
                break;
            }
        }

            // Parse through string array to find ending index
        for(int i = stringArray.length - 1; i > 0;i--){
            if(stringArray[i].matches("^[a-zA-Z0-9]*$")){
                endIndex = i;
                break;
            }
        }

            // Change array to only include selected indices
        stringArray = Arrays.copyOfRange(stringArray, beginIndex, endIndex + 1);
        currentToken = String.join("", stringArray);

        // Remove all apostrophes or quotation marks (single or double) from anywhere in the string
        currentToken = currentToken.replaceAll("\'","");
        currentToken = currentToken.replaceAll("\"","");

        // Remove hyphens and split up the original hyphenated token into multiple tokens (returns combined, and separated strings)
        stringArray = currentToken.split("-",-1);
        currentToken = currentToken.replaceAll("-","");

        // Convert token to lowercase and add to result array
        result.add(currentToken.toLowerCase());
        for(int i = 0; i < stringArray.length;i++){
            result.add(stringArray[i].toLowerCase());
        }

        // Stem token using implementation of the Porter2stemmer

        englishStemmer stemmer = new englishStemmer();
        for(int i = 0; i < result.size(); i++){
            stemmer.setCurrent(result.get(i));
            if(stemmer.stem()){
                result.set(i, stemmer.getCurrent());
            }
        }

        return result;
    }
}
