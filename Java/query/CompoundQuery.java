package bcf.query;

import bcf.common.ActiveBitCollection;

import java.util.ArrayList;
import java.util.HashMap;

public interface CompoundQuery {
    /**
     * Takes two arrays of compressed columns and preforms an OR
     * operations on the ranges. Then preforms an AND operation between
     * the results. The results are returned in a bit vector
     *
     * @param columns A hashmap containing the compressed columns loaded
     * @param tokens An array holding the tokens (start,end,operator) for the sub-queries
     * @return  The result of doing an AND between the two sets of tuples
     * */
    public ActiveBitCollection query(SemanticCache cache, HashMap<Integer, ActiveBitCollection> columns, String[] tokens);

}
