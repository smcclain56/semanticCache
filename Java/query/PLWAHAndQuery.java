package bcf.query;



import java.util.BitSet;
import java.util.Iterator;

import bcf.common.VLCActiveBitCol;
import bcf.common.ActiveBitCollection;
import bcf.common.PLWAHActiveBitCol;
import bcf.common.VLCConstants;


public class PLWAHAndQuery implements PointQuery {
	/** Takes two compressed columns and performs a logical AND
	 * operation on them. The results are returned in a bit vector
	 * 
	 * @param col1 A compressed column for querying 
	 * @param col2 A compressed column for querying
	 * @return  the result of col1 AND col2
	 * */
	public ActiveBitCollection AndQuery(ActiveBitCollection col1, ActiveBitCollection col2) {
		
		ActiveBitCollection ret=null;
		
			//create the result bitcollection 
			ret = new PLWAHActiveBitCol("Res_"+col1.getColName()+"_AND_"+col2.getColName());
			Iterator<Long> col1It = col1.getSegmentIterator();
			Iterator<Long> col2It = col2.getSegmentIterator();
			//These decode the segments into the decodeLen
			PLWAHActiveSegment col1Seg = new PLWAHActiveSegment(col1It.next());
			PLWAHActiveSegment col2Seg = new PLWAHActiveSegment(col2It.next());
			//need to do this loop at least once even if there is only one segment
			do{
				//See if we need to fetch a new segment from either one of the columns
				if(col1Seg.numOfSegments() == 0){
					col1Seg = new PLWAHActiveSegment(col1It.next());
				}
				if(col2Seg.numOfSegments() == 0){
					col2Seg = new PLWAHActiveSegment(col2It.next());
				}
				//System.out.println("col1 "+col1Seg.numOfSegments()+ "  col2 "+col2Seg.numOfSegments());
				//process the decoded segments
				while(col1Seg.numOfSegments()!=0 && col2Seg.numOfSegments() != 0){
					
					if(col1Seg.isFill()){
						if(col2Seg.isFill()){//They are both fills
							//find the shortest run
							long minSegs = Math.min(col1Seg.numOfSegments(), col2Seg.numOfSegments());
							//append a run of that length the return value 
							ret.appendFill(minSegs, (byte)(col1Seg.getFillValue()&col2Seg.getFillValue()));
							//mark those words as being used
							col1Seg.usedNumWords(minSegs);
							col2Seg.usedNumWords(minSegs);
						}else{//col1 is a fill col2 is a literal
							ret.appendLiteral((col1Seg.getLiteralRepOfFill()&col2Seg.getLiteralValue()));
						}
						
					}else{//col1Seg is a literal
						if(col2Seg.isFill()){
							ret.appendLiteral((col2Seg.getLiteralRepOfFill()&col1Seg.getLiteralValue()));
						}else{//both are literals
							ret.appendLiteral((col2Seg.getLiteralValue()&col1Seg.getLiteralValue()));
						}
						
					}
				}
				//need to use && here because of the way the columns are read from disk
				//This solves a problem when one column slops over and writes a single segment into
				//the last word.  The remainder of that word is filled with zeros.  If the other column
				//does not have the same slop the columns won't have the same number of bits.  Luckily all the 
				//extra bits can be disregarded (since they are just extra padding) and so we can stop the loop when
				//one of the columns is exhausted.
			}while(col1It.hasNext()   || col2It.hasNext());	
		
		return ret;
	}

	@Override
	public ActiveBitCollection OrQuery(ActiveBitCollection col1, ActiveBitCollection col2) {
		// TODO FILL THIS OUT???
		return null;
	}


}
