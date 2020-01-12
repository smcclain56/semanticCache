package bcf.common;

import bcf.compression.BitStringRep;
import bcf.compression.SegmentationLengthDeterminer;

public class TestMultSegDet implements SegmentationLengthDeterminer {
	static int count = -1;
	@Override
	public int determineSegLen(BitStringRep[] table, int colNum) {
		count++;
		if(count % 3 == 0){return 17;}
		else if(count % 3 == 1){return 14;}
		else{return 27;}
	}

}
