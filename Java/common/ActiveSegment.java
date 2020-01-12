package bcf.common;

public interface ActiveSegment {
	
	
	
	
	/**
	 * Returns the number of segments represented by this 
	 * encoding.  So it it is a fill of 23 segments of 1's
	 * it will return 23.  If it is a literal it will return 1.
	 * 
	 * @return number of segments represented by this encoding
	 * */
	public long numOfSegments();
	
	/**
	 * @return True if this segment is a run of ones false otherwise
	 * */
	public byte getFillValue();
	
	/**
	 * @return A literal representation of the fill
	 */
	public long getLiteralRepOfFill();
	
	/**
	 * @return True if this segment is a fill false otherwise
	 * */
	public boolean isFill();
	
	
	/**
	 * Returns the literal of this active segment.  If this segment is a run
	 * it returns a 1 segment lengths worth of the fill value.  For example, if the segment
	 * length was 7 and it was a run of 48 1's, getLiteralVaule would return 1111111
	 * @return literal presentation of the segment
	 * */
	public long getLiteralValue();
	
	/**
	 * This method is called when the query has used a certain number of words
	 * of the fill.  The number of used words is taken away from the active words
	 * 
	 * @param numUsed The number of segments used in the query.
	 * */
	public void usedNumWords(long numUsed);

}
