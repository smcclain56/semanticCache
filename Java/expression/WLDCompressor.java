package bcf.compression;
import bcf.common.*;

public class WLDCompressor {

	public WLDCompressor(){	}
	
	/**
	 *Compress the raw data
	 *@param the column that needs to be compressed
	 *@return The ActiveBitCollection that contains the compresses representation of the column
	 * */
	public ActiveBitCollection compress(int seglen, int columnNum){

		//create a new collection to hold this column its name will be the number
		ActiveBitCollection columnRep = new VLCActiveBitCol(seglen,""+columnNum);
		//value of the current seglen in long form
		long val = 0;
		
		//get the number of bits in the column
		int nBits = VLCConstants.RAW_TABLE[columnNum].getNumBits();
		
		//get the number of bits added to the segment, this number is independent of nBits
		//There is an easy way to tie this to Nbits but for clarity I have not done that
		int numSegBits = 0;
		
		//parse all the bits in the column
		for(int bCount = 0; bCount <nBits; bCount++){
			//sift the bits of call over one to make room for the new column value
			val = val<<1;
			//Test to see if the bit is one 
			if(VLCConstants.RAW_TABLE[columnNum].getBit(bCount)){
				//set the last bit to 1
				val=val+1;
			}
			//count the bit we've added to the segment
			numSegBits +=1;
			
			//if we've filled current word we need to append it to the 
			//the collection. 
			if(numSegBits%seglen == 0){
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
			val = val<<seglen-numSegBits;
			//call append this will determine if it is a run or a literal
			columnRep.appendLiteral(val);		
		}
		
		return columnRep;

	}

}