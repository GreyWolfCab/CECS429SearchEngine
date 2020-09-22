package cecs429.text;

import org.tartarus.snowball.ext.englishStemmer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdvancedTokenProcesser implements TokenProcessor{


    @Override
    public List<String> processToken(String token) {

        int beginIndex = 0;
        int endIndex = 0;
        String currentToken;
        String stringArray[];
        List<String> stringList = new ArrayList<String>();
        List<String> result = new ArrayList<String>();

        // Remove all non-alphanumeric characters from the beginning and end of token
        stringArray = token.split("");
        stringList = Arrays.asList(stringArray);

        // Parse through string array to find beginning index
        for(int i = 0; i < stringList.size();i++){
            if(isAlphanumeric(stringList.get(i))){
                beginIndex = i;
                break;
            }
        }

        // Parse through string array to find ending index
        for(int i = stringList.size() - 1; i > 0;i--){
            if(isAlphanumeric(stringList.get(i))){
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

        // Print result in console
        //System.out.println(result);

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

    public static boolean isAlphanumeric(String str) {
        char c = str.charAt(0);
        if (c < 0x30 || (c >= 0x3a && c <= 0x40) || (c > 0x5a && c <= 0x60) || c > 0x7a){
            return false;
        } else {
            return true;
        }
    }
}
