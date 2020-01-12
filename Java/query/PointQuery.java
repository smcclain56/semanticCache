package bcf.query;

import bcf.common.ActiveBitCollection;

public interface PointQuery {
	/**
	 * Takes two compressed columns and performs an AND
	 * operation on them. The results are returned in a bit vector
	 * 
	 * @param col1 A compressed column for querying 
	 * @param col2 A compressed column for querying
	 * @return  the result of col1 AND col2
	 * */
	public ActiveBitCollection AndQuery(ActiveBitCollection col1, ActiveBitCollection col2);

	/**
	 * Takes two compressed columns and performs an OR
	 * operation on them. The results are returned in a bit vector
	 *
	 * @param col1 A compressed column for querying
	 * @param col2 A compressed column for querying
	 * @return  the result of col1 OR col2
	 * */
	public ActiveBitCollection OrQuery(ActiveBitCollection col1, ActiveBitCollection col2);


}
