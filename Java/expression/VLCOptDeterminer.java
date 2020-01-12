package bcf.compression;

import bcf.common.ActiveBitCollection;
import bcf.common.VLCConstants;

/**
 * This class will find the best segmentation length
 * by trying all seg lens from 4 to word length
 * @author Jason
 *
 */
public class VLCOptDeterminer implements SegmentationLengthDeterminer {
	

	@Override
	public int determineSegLen(BitStringRep[] table, int colNum) {
		//keep track of the minimum size and its corresponding seglen
		int minsize = Integer.MAX_VALUE;
		int minSeg = 4;
		//loop that runs though all the factors of the base
		int currentSeg = 4;
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
			currentSeg++;
		}
		return minSeg;
	}

}
