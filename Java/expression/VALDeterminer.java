package bcf.compression;

import bcf.common.ActiveBitCollection;
import bcf.common.VLCConstants;

/**
 * This class will find the best segmentation length
 * by trying all seg lens of 7,14,and 28
 * @author Jason
 *
 */
public class VALDeterminer implements SegmentationLengthDeterminer {
	

	@Override
	public int determineSegLen(BitStringRep[] table, int colNum) {
		//keep track of the minimum size and its corresponding seglen
		
		int minSeg = 7;
		SegmentationLengthDeterminer curSDL = new SetLengthDeterminer(7);
		VALCompressor curComp = new VALCompressor(curSDL, table);
		ActiveBitCollection curABC = curComp.compress(colNum);
		int minsize = curABC.getSize();
		
		curSDL = new SetLengthDeterminer(14);
		curComp = new VALCompressor(curSDL, table);
		curABC = curComp.compress(colNum);
		if(minsize>=curABC.getSize()){
			minsize = curABC.getSize();
			minSeg = 14;
		}
		curSDL = new SetLengthDeterminer(28);
		curComp = new VALCompressor(curSDL, table);
		curABC = curComp.compress(colNum);
		
		if(minsize>=curABC.getSize()){
			minsize = curABC.getSize();
			minSeg = 28;
		}
		
		return minSeg;
	}

}
