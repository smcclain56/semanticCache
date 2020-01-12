package bcf.common;

import java.util.Iterator;

public interface ActiveBitCollection {
	/***
	 * Appends a fill word representing n runs, with a fillbit
     * of 'fillbit'.
     * 
	 * @param numRuns number of runs to add to the fill
	 * @param bitvalue the value of the fill bit 1 or 0
	 */
	public void appendFill(long numRuns, byte fillBit);
	
	 /**
     * Appends the past in value to the collection of segments,
     * as the appropriate hex value. This method will enter
     * the new segment as a literal, and then see what values have 
     * come before it to determine whether or not it should be
     * appended to a fill or not.
     * 
     * @param value The literal value to be added to the collection
     */
	public void appendLiteral(long value);
	
	/**
     * Add the a word to the vector containing
     * all words.
     * 
     * @param word Word to be appended.
     */
	public void appendWord(long word);
	
	
	/**
     * Set the encoding length to be used
     * by this ActiveBitVector.
     * 
     * @param seglen The new encoding length.
     */
    public void setSeglen(int seglen);
    
    /**
     * Gets the encoding length to be used
     * by this ActiveBitCollection.
     * 
     * @return The new encoding length.
     */
    public int getSeglen();
    
    /**
     * Retrieves the total number of words needed
     * to represent this bit collection. This method is used in method 
     * finding best compression or findBestGCD 
     * 
     * @return Number of words used to represent this ActiveBitCollection.
     */
    public int getNumberOfWords();
    
	
	/** 
     * Checks if the Collection holding words is empty.
     * 
     * @return True if the collection is empty, false otherwise.
     */
	public boolean isEmpty();
	
	/**
	 * Returns a Iterator for words in the bit vector.  right now it returns integer
	 * in the future when we want a 64 bit word with will have to be long
	 * @return iterator for the words stored in this column
	 */
	public Iterator<Long> getSegmentIterator();
	
	
	/**
	 * @return The name assigned to this bit collection typically the column number
	 * */
	public String getColName();
	
	/**
	 * @return The size in bits of the current compressed column
	 */
	public int getSize();
}
