package bcf.query;

import bcf.common.ActiveBitCollection;

import java.util.HashMap;

public interface RangeQuery {
    /**
     * Takes two compressed columns and performs an AND
     * operation on them. The results are returned in a bit vector
     *
     * @param columns A hashmap containing the compressed columns
     * @param start starting column of the query
     * @param end ending column of the query
     * @return  the result of doing an AND of all the columns from col[start] to col[end]
     * */
    public ActiveBitCollection AndQuery(HashMap<Integer, ActiveBitCollection> columns, int start, int end);

    /**
     * Takes two compressed columns and performs an OR
     * operation on them. The results are returned in a bit vector
     *
     * @param columns A hashmap containing the compressed columns
     * @param start starting column of the query
     * @param end ending column of the query
     * @return  the result of doing an OR of all the columns from col[start] to col[end]
     * */
    public ActiveBitCollection OrQuery(HashMap<Integer, ActiveBitCollection> columns, int start, int end);


}
