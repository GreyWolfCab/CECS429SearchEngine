package cecs429.text;

import org.tartarus.snowball.ext.englishStemmer;

import java.util.ArrayList;
import java.util.List;

public class AdvancedTokenProcesser implements TokenProcessor{


    @Override
    public List<String> processToken(String token) {

        int beginIndex = 0;
        int endIndex = 0;
        String currentToken;
        String stringArray[];//a hyphenated word will return multiple term
        List<String> result = new ArrayList<String>();//every term derived from the token

        //disqualify beginning characters that are not alphanumeric
        for (int i = 0; i < token.length(); i++) {
            if (isAlphanumeric(token.charAt(i))) {
                beginIndex = i;
                break;
            }
        }

        //disqualify ending characters that are not alphanumeric
        for (int i = token.length()-1; i >= 0; i--) {
            if (isAlphanumeric(token.charAt(i))) {
                endIndex = i;
                break;
            }
        }

        //remove excess characters
        currentToken = token.substring(beginIndex, endIndex+1);

        // Remove all apostrophes or quotation marks (single or double) from anywhere in the string
        currentToken = currentToken.replaceAll("\'","");
        currentToken = currentToken.replaceAll("\"","");

        // Remove hyphens and split up the original hyphenated token into multiple tokens (returns combined, and separated strings)
        stringArray = currentToken.split("-",-1);

        // Convert token to lowercase and add to result array
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

    public static boolean isAlphanumeric(char c) {
        //checks chars exist within the range of letters and numbers via ascii
        if (c < 0x30 || (c >= 0x3a && c <= 0x40) || (c > 0x5a && c <= 0x60) || c > 0x7a){
            return false;
        } else {
            return true;
        }
    }
}
