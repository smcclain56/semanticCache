package bcf.compression;

import bcf.common.ActiveBitCollection;
import bcf.common.VLCConstants;

/**
 * This class will find the best segmentation length
 * for a given column using a user defined base.  For example,
 * if the user set the base to be 7 this class would compress
 * the column using seg lengths of 7 17 21 28 and return the one that 
 * gives the best compression
 * @author Jason
 *
 */
public class VLCBaseDeterminer implements SegmentationLengthDeterminer {
	
	//this is the base that will be used to determine the segments
	private int base;
	
	/**
	 * @param base the base that will be used to determine all segment lengths for this SLD
	 */
	public VLCBaseDeterminer (int base){
		this.base = base;
	}

	@Override
	public int determineSegLen(BitStringRep[] table, int colNum) {
		//keep track of the minimum size and its corresponding seglen
		int minsize = Integer.MAX_VALUE;
		int minSeg = base;
		//loop that runs though all the factors of the base
		int currentSeg = base;
		int i = 1;
		//-1 because we need to save room for the flag bit
		while(currentSeg <= VLCConstants.WORD_LEN-1){
			//This is ugly and slow,  could think about changing it so new objects aren't always created
			//create a SDL that is fixed at the currentSeg Length
			SegmentationLengthDeterminer curSDL = new SetLengthDeterminer(currentSeg);
			VLCCompressor curComp = new VLCCompressor(curSDL, table);
			ActiveBitCollection curABC = curComp.compress(colNum);
			//testing the size using >= since we start at smallest seglen
			//if there is a tie we want to take the large length since it is 
			//less parsing.
			if(minsize >= curABC.getNumberOfWords()){
				minsize = curABC.getNumberOfWords();
				minSeg = currentSeg;
				
			}
			//move to the next seg length
			i++;
			currentSeg = base*i;
		}
		return minSeg;
	}

}
