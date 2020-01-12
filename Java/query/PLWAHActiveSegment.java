package bcf.query;

import bcf.common.ActiveSegment;

public class PLWAHActiveSegment implements ActiveSegment {
	private long litRepOfFill=0;
	private boolean isFill;
	private boolean hasDirty = false;
	private long dirtyWord;
	private byte fillValue = 0;
	private long numSegments; 
	//it is an array because the literal value might need to be chunked if decodeLen < encodeLen
	private long litValue;
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
	private final int numShiftDirtyBits = 25;
	public PLWAHActiveSegment(long value){
		//it a literal
		if(value<=this.maxPLWAHLargestLit){
			this.litValue = value;
			this.isFill = false;
			this.hasDirty = false;
			this.numSegments = 1;
		//its a run of 1's
		}else{
			//since we have to do it for all runs might as well do it first
			this.numSegments = value & this.maxPLWAHFillLen;
			int posDirty = (int)(value & this.dirtyBits);
			posDirty = posDirty >>> this.numShiftDirtyBits;
			if(posDirty != 0){
				//it has a dirty word
				this.hasDirty = true;
				//until we learn different assume the dirty bit is a one and we just have to shift it to the
				//proper position
				this.dirtyWord = 1<<posDirty-1;
			}
			if(value<this.maxPLWAHOneFill){
				this.isFill = true;
				this.fillValue = 0;
				this.litRepOfFill=0;
				
				
				
			}else{
				//run of ones
				this.isFill = true;
				this.fillValue =1;
				this.litRepOfFill = this.maxPLWAHLargestLit;
				if(this.hasDirty){
					//we assumed wrong that it was a run of zeros with a 1
					//easy fix.  Just invert all the bits.
					this.dirtyWord = this.dirtyWord ^ this.maxPLWAHLargestLit;
				}
				
			}
			
			
		}
		
	}
	
	
	/**
	 * JS: Helper function that will turn the fill into a
	 * Literal of the dirty word when it is fill is exhausted
	 * make sure to call it anywhere that a fill value is used 
	 * and the numSegments is decreased. 
	 * */
	private void accountForDirtyWord(){
		if(this.numSegments==0 && this.hasDirty){
			this.isFill = false;
			this.litValue = this.dirtyWord;
			this.numSegments = 1;
			this.hasDirty = false;
			
		}
		
	}
	
	public boolean hasDirtyWord(){
		return this.hasDirty;
	}
	
	public long getDirtyWord(){
		return this.dirtyWord;
	}
	
	
	@Override
	public byte getFillValue() {
		
		return this.fillValue;
	}

	@Override
	public long getLiteralRepOfFill() {
		this.numSegments--;
		this.accountForDirtyWord();
		return this.litRepOfFill;
	}

	@Override
	public long getLiteralValue() {
		this.numSegments--;
		return this.litValue;
	}

	@Override
	public boolean isFill() {
		
		return this.isFill;
	}

	@Override
	public long numOfSegments() {
		return this.numSegments;
	}

	@Override
	public void usedNumWords(long numUsed) {
		this.numSegments-=numUsed;
		this.accountForDirtyWord();
	}

}
