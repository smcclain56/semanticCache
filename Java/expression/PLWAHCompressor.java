package bcf.compression;
import bcf.common.*;


/**
 * This class is responsible for compressing the raw bitmaps used only for PLWAH compression.
 * 
 * JS: REALLY need to make compression generic enough to work with any compression scheme, the only challenge is 
 * how to get the activeBitcollection to work with seglength.  Could use reflection 
 * 
 * */

public class PLWAHCompressor {
	
	/**Raw representation of the bit structures it needs to be in column format*/
	private BitStringRep[] table;
	
	
	/**
	 *@param sld And implementation of SegmentationLength Determiner that will be used to determine the length to use to compress each column
	 *@param table This is a column oriented structure of the raw bitmap
	 * */
	public PLWAHCompressor( BitStringRep[] table){
		this.table = table;
		
	}
	
	/**
	 *Compress the raw data
	 *@param the column that needs to be compressed
	 *@return The ActiveBitCollection that contains the compresses representation of the column
	 * */
	public ActiveBitCollection compress(int columnNum){
		
		ActiveBitCollection columnRep = new PLWAHActiveBitCol(""+columnNum);
		//value of the current seglen in long form
		long val = 0;
		
		//get the number of bits in the column
		int nBits = this.table[columnNum].getNumBits();
		//get the number of bits added to the segment, this number is independent of nBits
		//There is an easy way to tie this to Nbits but for clarity I have not done that
		int numSegBits = 0;
		//parse all the bits in the column
		for(int bCount = 0; bCount <nBits; bCount++){
			//sift the bits of call over one to make room for the new column value
			val = val<<1;
			//Test to see if the bit is one 
			if(this.table[columnNum].getBit(bCount)){
				//set the last bit to 1
				val=val+1;
			}
			//count the bit we've added to the segment
			numSegBits +=1;
			
			//if we've filled current word we need to append it to the 
			//the collection. 
			if(numSegBits%(VLCConstants.WORD_LEN-1) == 0){
				//call append this will determine if it is a run or a literal
				columnRep.appendLiteral(val);
				//reset value for the next segment
				val = 0;
				//reset numSegBits
				numSegBits =0;
			}
		
		}
		
		//Need to take care of the case when there isn't nBits%seglen == 0 num bits
		//That would mean that the very last time through the loop a word wasn't added
		//in those cases numSegBits won't be zero
		//in this case we just add 0's until we fill out the segment
		if(numSegBits!=0){
			//to round out the perfect segment len add zeros
			//to calculate how many is left
			val = val<<(VLCConstants.WORD_LEN-1 )-numSegBits;
			//call append this will determine if it is a run or a literal
			columnRep.appendLiteral(val);		
		}
		
		return columnRep;

	}

}
