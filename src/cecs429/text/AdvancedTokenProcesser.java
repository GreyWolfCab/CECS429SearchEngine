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
//        currentToken = currentToken.replaceAll("\'","");
//        currentToken = currentToken.replaceAll("\"","");
        char singleQuote = '\'';
        char doubleQuote = '\"';
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < currentToken.length(); i++) {
            char c = currentToken.charAt(i);
            if (c != singleQuote && c != doubleQuote) {
                sb.append(c);
            }

        }
        currentToken = sb.toString();

        //DONE: remove hyphens from tokens
        // Remove hyphens and split up the original hyphenated token into multiple tokens (returns combined, and separated strings)
        stringArray = currentToken.split("-",-1);

        // Convert token to lowercase and add to result array
        if (stringArray.length > 1) {//multiple tokens found
            StringBuilder combinedToken = new StringBuilder();//combination of all subtokens into one
            for(int i = 0; i < stringArray.length;i++){//iterate through all subtokens separately
                combinedToken.append(stringArray[i]);//add to combo token
                result.add(stringArray[i].toLowerCase());//add individual token to result
            }
            result.add(combinedToken.toString());//add combo token to result
        } else {//single token found
            result.add(stringArray[0].toLowerCase());//add individual token to result
        }

        return result;
    }

    /**
     * DONE stemmer
     * stem a single token using the Porter2stemmer method
     * @param token the token to be stemmed
     * @return the stemmed token
     */
    public static String stemToken(String token) {

        String stemmedTerm = "";

        englishStemmer stemmer = new englishStemmer();
        stemmer.setCurrent(token);
        if (stemmer.stem()) {
            stemmedTerm = stemmer.getCurrent();
        }

        return stemmedTerm;

    }

    /**
     * DONE nonalpha characters
     * determine if the character is an alpha-numeric character
     * @param c the character to inspect
     * @return whether the character is alpha-numeric or not
     */
    private static boolean isAlphanumeric(char c) {
        //checks chars exist within the range of letters and numbers via ascii
        if (c < 0x30 || (c >= 0x3a && c <= 0x40) || (c > 0x5a && c <= 0x60) || c > 0x7a){
            return false;
        } else {
            return true;
        }
    }
}
