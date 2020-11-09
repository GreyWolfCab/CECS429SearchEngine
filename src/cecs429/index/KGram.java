package cecs429.index;

import java.util.List;
import java.util.Set;

public interface KGram {

    /**
     * gets all terms associated with the specified gram, null if the gram doesn't exist
     * @param gram
     * @return
     */
    public Set<String> getTerms(String gram);

    /**
     * get every gram stored in the K-Gram Index
     * @return
     */
    public List<String> getGrams();

    /**
     * get every gram that maybe derived from the given term following the given gram limit
     * @param gramLimit
     * @param term
     * @return
     */
    public List<String> getGrams(int gramLimit, String term);

    /**
     * For in-memory k-gram adds the gram to the Index, for on-disk it does nothing
     * @param gramLimit
     * @param term
     */
    public void addGram(int gramLimit, String term);

}
