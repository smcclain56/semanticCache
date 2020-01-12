package bcf.common;

import java.util.*;


public class VLCActiveBitCol implements ActiveBitCollection {
	
	/**The length used to encode this column.  This is used in quering.**/
	private int seglen = 0;
	/**List that holds**/
	private List<Long> vec;
    /** Name of this column*/
	private String name = "";
	/** Hexholder instance used to determine proper hexadecmial values*/
	private HexHolder hex;
	
	
	/**
	 * Constructor that initializes the arraylist
	 * and initial word for this column
	 * */
	public VLCActiveBitCol(){
		this.vec = new ArrayList<Long>();
		
	}

	
	/**
	 * Constructor that initializes the arraylist
	 * and initial word for this column
	 * 
	 * @param name The name of this column
	 * */
	public VLCActiveBitCol(String name){
		this.vec = new ArrayList<Long>();
		this.name = name;
	}

	/**
	 * Constructor that initializes the arraylist
	 * and initial word for this column
	 * 
	 * @param seglen segmentation length to be used for this column
	 * @param name The name of this column
	 * */
	public VLCActiveBitCol(int seglen, String name){
		this.seglen = seglen;
		this.hex  = HexHolder.getHexHolder(seglen);
		this.vec = new ArrayList<Long>();
		this.name = name;
	}
	
	/**
	 * Constructor that initializes the arraylist
	 * and initial word for this column
	 * 
	 * @param seglen segmentation length to be used for this column
	 * */
	public VLCActiveBitCol(int seglen){
		this.seglen = seglen;
		this.hex  = HexHolder.getHexHolder(seglen);
		this.vec = new ArrayList<Long>();
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
		while(numRuns > this.hex.getZeroRunFull()){
			//Subtract the number runs we can represent in a new element
			numRuns = numRuns - this.hex.getRunLen();
			//add a full run of zeros to the collection
			this.vec.add(new Long((this.hex.getZeroRunFull())));

		}
		//If there is any left over toss it in to a new element
		if(numRuns > 0){
			this.vec.add(this.hex.getZeroRun()+ numRuns);	
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
		while(numRuns > this.hex.getOnes()){
			//Subtract the number runs we can represent in a new element
			numRuns = numRuns - this.hex.getRunLen();
			//add a new full run
			this.vec.add(new Long((this.hex.getOnes())));

		}
		//Might have a few runs left toss them in a new word
		if(numRuns > 0){
			this.vec.add(new Long((this.hex.getOneRun()+numRuns)));	
		}

		
	}

	@Override
	public void appendFill(long numRuns, byte fillBit) {
		if (numRuns> 1 && !this.isEmpty())
		{

			long lastElementValue = this.vec.get(this.vec.size()-1);
			//We are adding a fill of 0s
			if (fillBit == 0)
			{
				//Last run was a 0, so lets append to it.
				if (lastElementValue >= this.hex.getZeroRun() && lastElementValue < this.hex.getOneRun())
				{
					//does adding this run over fill this run (we can't represent it given the seglen)
					if(lastElementValue + numRuns > this.hex.getZeroRunFull()){
						//how many runs can we represent
						numRuns= numRuns-(this.hex.getZeroRunFull() - lastElementValue);
						//set the last one to max fill
						this.vec.set(this.vec.size()-1, new Long((this.hex.getZeroRunFull())));
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
			else if (lastElementValue >= this.hex.getOneRun())
			{
				//if we can't fit the entire run into the last word
				if(lastElementValue + numRuns > this.hex.getOnes()){
					numRuns = numRuns- (this.hex.getOnes() - lastElementValue);
					//set the last one to max fill
					this.vec.set(this.vec.size()-1, new Long((this.hex.getOnes())));
					//Now we have to take care of what is left.
					this.addRunOfOnesNewWord(numRuns);
					//We can fit them all in the last element
				}else{
					this.vec.set(this.vec.size()-1, new Long((lastElementValue+numRuns))); 

				}
			}
			//Last word wasn't a run of ones just add a new word
			else
			{	this.addRunOfOnesNewWord(numRuns);

			}
		}
		//First word of the column
		else if (this.isEmpty())
		{	//Take care of the cause when its a run of zeros
			if (fillBit == 0)
			{
				this.addRunOfZerosNewWord(numRuns);
				
			}
			//Take care of the cause when its a run of ones
			else
			{
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
				this.appendLiteral(this.hex.getOnesNoFlag());
			}

			
		}
	}


	

	@Override
	public void appendLiteral(long value) {
		//If this is the first entry add the active word
		if (this.isEmpty()){
			this.vec.add(value);
		}else{
			//Grab the last element and its value.
			long lastElementValue = this.vec.get(this.vec.size()-1);
			//If the value of 0 means a run of 0s.
			if (value == 0){
				//If the last one was also a run of 0s, start a new run of 0s
				if (lastElementValue == 0){ 
					//Change the value of the last thing to a run of zeros, with 2 runs
					this.vec.set(this.vec.size()-1,this.hex.getZeroRunWOne());
				} //We already have a run of 0s setup, so we go through appending a run of 0s
				else if (lastElementValue >= this.hex.getZeroRun() && lastElementValue < this.hex.getOneRun() && lastElementValue != this.hex.getZeroRunFull()){
					this.vec.set(this.vec.size()-1, lastElementValue+1); // 
				}//There was a change in bits... treat this as a literal
				else{
					this.vec.add(value); 
				}
			}  //If we have a run of 1s.
			else if (value == this.hex.getOnesNoFlag()){

				//If the last was also a run of 1s, we start a new run of 1s.
				if (lastElementValue == value){
					//Change the last value to be a run of 1s, 
					//representing 2 runs.
					this.vec.set(this.vec.size()-1,this.hex.getOneRunWOne());

				}//Already have a run of 1s setup, so just update that.
				else if (lastElementValue >= this.hex.getOneRun() && (lastElementValue != this.hex.getOnes())){
					this.vec.set(this.vec.size()-1, lastElementValue+1); 

				}//A literal
				else{
					this.vec.add(value); 
				}
			}
			else{
				//A literal
				this.vec.add(value); 

			}
		}
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
		if(numseg%numSegPerWord!=0){
			tot++;
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
		this.hex  =  HexHolder.getHexHolder(seglen);

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
			while(t.length()!=this.seglen+1){
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
		return this.getNumberOfWords()*VLCConstants.WORD_LEN;
	}


	
}
