package bcf.common;

import java.util.*;
//JS:  Right now we don't compress 0111111...1111 (longest lit).  This does limit our compression 
//if the next word is a near word (e.i. a run of 1 with a dirty bit on).  Think of fixing.

public class PLWAHActiveBitCol implements ActiveBitCollection {
	
	//Enumeration type that is used to identifies the type of the last word 
	//added to the collection.  A LITERAL is a literal word, ONERUN is a run of 
	//ones without any dirty bits being set, ZERORUN run of zeros no dirty bits,
	//DIRTYRUN
	
	private enum WordType{
		LITERAL, ONERUN, ZERORUN, DIRTYRUN 
	}
	//Start it out as a literal
	WordType lastWord = WordType.LITERAL;
	/**The length used to encode this column.  This is used in querying.**/
	private int seglen = 31;
	/**List that holds**/
	private List<Long> vec;
    /** Name of this column*/
	private String name = "";
	

	/**The hex value for PLWAH's largest lit   01111111111...111: assumes a 32 bit word*/
	private final long  maxPLWAHLargestLit = 0x7FFFFFFFL;
	/**The hex value for PLWAH's  0 fill       10000000000...000: assumes a 32 bit word*/
	private final long  maxPLWAHZeroFill =  0x80000000L;
	/**The hex value for PLWAH's  1 fill       11000000000...000: assumes a 32 bit word*/
	private final long  maxPLWAHOneFill =  0xC0000000L;
	/**The hex value for PLWAH's max 1 fill    11000001111...111: assumes a 32 bit word*/
	private final long  maxPLWAHOneFillFull =  0xC1FFFFFFL;
	/**The hex value for PLWAH's max 0 fill    10000001111...111: assumes a 32 bit word*/
	private final long maxPLWAHZeroFillFull =  0x81FFFFFFL;
	/**The hex value for PLWAH's max len fill  00000001111...111: assumes a 32 bit word*/
	private final long maxPLWAHFillLen = 0x1FFFFFFL;
	/**The hex value to get PLWAH's dirty bits 001111100000..000: assumes a 32 bit word*/
	private final long dirtyBits = 0x3E000000L;
	
	private static HashMap<Long, Integer> NEARONESMAP = initializeNearOnes();
	//initialize the hashmap of near ones where the key is the long representation of the dirty bit
	//value is the position of the dirty bit
	private static HashMap<Long, Integer> initializeNearOnes(){
		HashMap<Long,Integer> retVals = new HashMap<Long,Integer>();
		//xor a run of ones against a one in each position
		long base = 0x7FFFFFFFL;
		for(int i=1; i<=31; i++){
			retVals.put(base^(1<<i-1),i);
		}
		return retVals;
	}
		
	/**
	 * Constructor that initializes the arraylist
	 * and initial word for this column
	 * */
	public PLWAHActiveBitCol(){
		this.vec = new ArrayList<Long>();
		
		
	}

	
	/**
	 * Constructor that initializes the arraylist
	 * and initial word for this column
	 * 
	 * @param name The name of this column
	 * */
	public PLWAHActiveBitCol(String name){
		this.vec = new ArrayList<Long>();
		this.name = name;
		
	}

	

	/**
	 * Helper method that adds new word(s) to 
	 * the collection.  The word(s) will contain runs
	 * of zeros
	 * 
	 * @param numwords The number of runs to be represented
	 * */
	
	private void addRunOfZerosNewWord(long numRuns){
		//Since it can over fill multiple runs we need a loop
		//each time through the loop we are going to add a full run of 
		//zeros
		while(numRuns > this.maxPLWAHFillLen){
			//Subtract the number runs we can represent in a new element
			numRuns = numRuns - this.maxPLWAHFillLen;
			//add a full run of zeros to the collection
			this.vec.add(new Long((this.maxPLWAHZeroFillFull)));

		}
		//If there is any left over toss it in to a new element
		if(numRuns > 0){
			this.vec.add(this.maxPLWAHZeroFill+ numRuns);	
		}

		
	}

	/**
	 * Helper method that adds new word(s) to 
	 * the collection.  The word(s) will contain runs
	 * of ones
	 * 
	 * @param numwords The number of runs to be represented
	 * */
	
	private void addRunOfOnesNewWord(long numRuns){
		//Since it can over fill multiple runs we need a loop
		//each time through the loop we are going to add a full run of ones
		while(numRuns > this.maxPLWAHFillLen){
			//Subtract the number runs we can represent in a new element
			numRuns = numRuns - this.maxPLWAHFillLen;
			//add a new full run
			this.vec.add(new Long((this.maxPLWAHOneFillFull)));

		}
		//Might have a few runs left toss them in a new word
		if(numRuns > 0){
			this.vec.add(new Long((this.maxPLWAHOneFill+numRuns)));	
		}

		
	}

	@Override
	public void appendFill(long numRuns, byte fillBit) {
		if (numRuns> 1 && !this.isEmpty())
		{

			long lastElementValue = this.vec.get(this.vec.size()-1);
			
			//We are adding a fill of 0s
			if (fillBit == 0)
			{	this.lastWord = WordType.ZERORUN;
				//Last run was a 0, so lets append to it.
				if (lastElementValue >= this.maxPLWAHZeroFill && lastElementValue < this.maxPLWAHOneFill)
				{
					//does adding this run over fill this run (we can't represent it given the seglen)
					if(lastElementValue + numRuns > this.maxPLWAHZeroFillFull){
						//how many runs can we represent
						numRuns= numRuns-(this.maxPLWAHZeroFillFull - lastElementValue);
						//set the last one to max fill
						this.vec.set(this.vec.size()-1, new Long((this.maxPLWAHZeroFillFull)));
						//Now we need to take care of whats left
						//This is a helper function that will add new word or words to accommodate whats left
						this.addRunOfZerosNewWord(numRuns);
					//We can fit this into the last word without over filling it.
					}else{
						this.vec.set(this.vec.size()-1, new Long((lastElementValue+numRuns))); 	
					}
					
				}
				//Last word wasn't a run zeros so we need to start a new word
				else
				{	
					//This is a helper function that will add new word or words to accommodate the runs
					this.addRunOfZerosNewWord(numRuns);
				}
			}
			//Fillbit is 1 so we are adding a run of ones
			//Check last word was a run of ones
			else if (lastElementValue >= this.maxPLWAHOneFill)
			{
				this.lastWord = WordType.ONERUN;
				//if we can't fit the entire run into the last word
				if(lastElementValue + numRuns > this.maxPLWAHOneFillFull){
					numRuns = numRuns- (this.maxPLWAHOneFillFull - lastElementValue);
					//set the last one to max fill
					this.vec.set(this.vec.size()-1, new Long((this.maxPLWAHOneFillFull)));
					//Now we have to take care of what is left.
					this.addRunOfOnesNewWord(numRuns);
					//We can fit them all in the last element
				}else{
					this.vec.set(this.vec.size()-1, new Long((lastElementValue+numRuns))); 

				}
			}
			//Last word wasn't a run of ones just add a new word
			else
			{	this.lastWord = WordType.ONERUN;
				this.addRunOfOnesNewWord(numRuns);

			}
		}
		//First word of the column
		else if (this.isEmpty())
		{	//Take care of the cause when its a run of zeros
			if (fillBit == 0)
			{
				this.lastWord = WordType.ZERORUN;
				this.addRunOfZerosNewWord(numRuns);
				
			}
			//Take care of the cause when its a run of ones
			else
			{	this.lastWord = WordType.ONERUN;
				this.addRunOfOnesNewWord(numRuns);
			}
		}
		//Trying to add a single run treat it like a literal 
		else
		{
			if (fillBit == 0)
			{
				this.appendLiteral(0);
			}
			else
			{
				this.appendLiteral(this.maxPLWAHLargestLit);
			}

			
		}
	}


	

	@Override
	public void appendLiteral(long value) {
		//If this is the first entry add the active word
	//	System.out.println("TEST:  "+Long.toBinaryString(value));
		if (this.isEmpty()){
			//this.vec.add(value);
			if(value == 0){
				this.vec.add(this.maxPLWAHZeroFill+1);
				this.lastWord = WordType.ZERORUN;
			}else if(value ==  this.maxPLWAHLargestLit){
				this.vec.add(this.maxPLWAHOneFill+1);
				this.lastWord = WordType.ONERUN; 
			}else{
				this.vec.add(value);
				this.lastWord = WordType.LITERAL;
			}
		}else{
			//Grab the last element and its value.
			long lastElementValue = this.vec.get(this.vec.size()-1);
			//If the value of 0 means a run of 0s.
			if (value == 0){
				if (this.lastWord == WordType.ZERORUN &&  lastElementValue <this.maxPLWAHZeroFillFull){
					this.vec.set(this.vec.size()-1, lastElementValue+1); // 
				}//There was a change in bits... treat this as a literal
				else{
					this.vec.add(this.maxPLWAHZeroFill+1);
					this.lastWord = WordType.ZERORUN;
				}
			}  //If we have a run of 1s.
			else if (value == this.maxPLWAHLargestLit){

				//If the last was also a run of 1s, we start a new run of 1s.
				/*if (lastElementValue == value){
					//Change the last value to be a run of 1s, 
					//representing 2 runs.
					this.lastWord = WordType.ONERUN;
					this.vec.set(this.vec.size()-1,this.maxPLWAHOneFill+2);
					
				}//Already have a run of 1s setup, so just update that.
				else */
				if (this.lastWord==WordType.ONERUN && (lastElementValue != this.maxPLWAHOneFillFull)){
					this.vec.set(this.vec.size()-1, lastElementValue+1); 

				}//A literal
				else{
				//	System.out.println("Adding run");
					this.lastWord = WordType.ONERUN;
					this.vec.add(this.maxPLWAHOneFill+1); 
				}
			}
			else{
				//Check if we PLWAH this motha 
				
				if(this.lastWord == WordType.ZERORUN  && this.nearZero(value)!=-1 ){
					this.setDirtyBit(this.nearZero(value));
				}else if(this.lastWord == WordType.ONERUN && this.nearOne(value)!=-1){
					this.setDirtyBit(this.nearOne(value));
				}else{
					//nothing to do but add it as a literal
					//System.out.println("ADDING "+Long.toBinaryString(value));
					this.lastWord = WordType.LITERAL;
					this.vec.add(value); 
				}
				
			}
		}
	}

	
	/**
	 * This is a helper function that sets the dirt bit in a run.  It should only
	 * be called when a near word is found (a word containing only 1 heterogeneous bit) following a run
	 * of dominant value.  
	 * 
	 * @param position of the heterogeneous bit in the current word
	 */
	private void setDirtyBit(int pos){

		long lastElementValue = this.vec.get(this.vec.size()-1);
		//Shift the position number to the correct spot
		pos = pos << 25;
		//or with the shifted position to combine the two
		lastElementValue = lastElementValue | pos;
		 //rewrite the last element
		this.vec.set(this.vec.size()-1, lastElementValue);
		
		//make sure to flag this last word as a dirty word
		this.lastWord = WordType.DIRTYRUN;
	}
	
	
	/**
	 * This is a helper function determines if the value only has 1 flipped bit.
	 * Meaning that if the previous element was a zero fill we can flip the 
	 * correct dirty bit.
	 * 
	 * @param value the value to check if it is a near to zero run
	 * @return the position of the flipped bit or -1 if there are multiple flipped bits
	 * */
    private int nearZero(long value){ 
    	//test value against all the powers of 2.  if it exactly equals a power of 2 then
    	//it is a near zero word
    
    	long test = 1;
    	//CHANGED POS=0 TO POS=1
    	for(int pos=1; pos<=this.seglen; pos++){
    		if(value==test){
    			return pos;
    		}
    		test = test<<1;
    	}
    	
    	return -1;
    
    }
    /**
     * JS: WHY IS THIS RUNNING SO DAMN SLOW???  
     * 
	 * This is a helper function determines if the value only one non-flipped bit.
	 * Meaning that if the previous element was a one fill we can flip the 
	 * correct dirty bit.
	 * 
	 * @param value the value to check if it is a near to one run
	 * @return the position of the non-flipped bit or -1 if there are multiple non-flipped bits
	 * */
   private int nearOne(long value){ 
    	/*long takeWay =1;
    	//use xor to create 111..0..111, move the 0 from right to left testing if the resulting
    	//number is equal to the value, if it is then we have a near 1 word 
    	for(int pos=0; pos<this.seglen; seglen++){
    		long test = this.maxPLWAHLargestLit ^ takeWay;//^ is xor in java
    		if(value==test){
    			return pos;
    		}
    		takeWay = takeWay<<1;
    	}
    
    	return -1;*/
	   if(NEARONESMAP.containsKey(value)){
		   return NEARONESMAP.get(value);
	   }
	   return -1;
    }
	

	@Override
	public void appendWord(long word) {
		this.vec.add(word);

	}

	@Override
	public int getNumberOfWords() {
		//get the number of segments
		int numseg = this.vec.size();
		//How many segments can fit in a word
		int numSegPerWord = VLCConstants.WORD_LEN/this.seglen;
		//total number of words needed to store this column
		int tot = numseg/numSegPerWord;
		if(tot==0){
			tot = 1;
		}
		return tot;
	}

	@Override
	public boolean isEmpty() {
		return this.vec.isEmpty();
	}

	@Override
	public void setSeglen(int seglen) {
		this.seglen = seglen;
		

	}
	
	@Override
	public int getSeglen(){
		return this.seglen; 
	}

	
	@Override
	public String toString(){
		String s = "";
		for(long l : this.vec){
			
			String t = Long.toBinaryString(l);
			while(t.length()<VLCConstants.WORD_LEN){
				t="0"+t;
			}
			s+=t;
			s+=",";
		}
		return s;
	}
	
	@Override
	public String getColName(){
		return name;
	}


	@Override
	public Iterator<Long> getSegmentIterator() {
		return this.vec.iterator();
	}

	@Override
	public int getSize() {
		return this.vec.size()*VLCConstants.WORD_LEN;
	}

	

	
}
