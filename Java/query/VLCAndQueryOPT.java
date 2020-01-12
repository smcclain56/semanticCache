package bcf.query;



import java.util.BitSet;
import java.util.Iterator;

import bcf.common.ActiveSegment;
import bcf.common.VLCActiveBitCol;
import bcf.common.ActiveBitCollection;
import bcf.common.VLCConstants;


public class VLCAndQueryOPT implements PointQuery {
	private ActiveBitCollection col1;
	private ActiveBitCollection col2;
	int decodeLen;
	/** Takes two compressed columns and performs a logical AND
	 * operation on them. The results are returned in a bit vector
	 * 
	 * @param col1 A compressed column for querying 
	 * @param col2 A compressed column for querying
	 * @return  the result of col1 AND col2
	 * */
	public ActiveBitCollection AndQuery(ActiveBitCollection col1, ActiveBitCollection col2) {
		//Find the GCD and use that as the decoding len
		
		ActiveBitCollection ret = null;
		int col1seglen = col1.getSeglen();
		int col2seglen = col2.getSeglen();
		
	
		if(col1seglen == col2seglen){
			this.col1=col1;
			this.col2 = col2;
			
			decodeLen=col1seglen;
			VLCActiveSegment_Opt_Same col1Seg = new VLCActiveSegment_Opt_Same(col1seglen);
			VLCActiveSegment_Opt_Same col2Seg = new VLCActiveSegment_Opt_Same(col1seglen);
			ret = this.sameLen(col1Seg, col2Seg);
		}else{
			decodeLen = VLCConstants.GCD_MATRIX[col1seglen][col2seglen];
			if(col1seglen == decodeLen){
				this.col1=col1;
				this.col2 = col2;
				
				//System.out.println("Got Here");
				VLCActiveSegment_Opt_Same col1Seg = new VLCActiveSegment_Opt_Same(col1seglen);
				VLCActiveSegment_Opt_Diff col2Seg = new VLCActiveSegment_Opt_Diff(col2seglen,decodeLen);
				ret = oneDiff(col1Seg,col2Seg);
			}else{
				this.col1=col2;
				this.col2 = col1;
				if(col2seglen == decodeLen){
					
					VLCActiveSegment_Opt_Same col2Seg = new VLCActiveSegment_Opt_Same(col2seglen);
					VLCActiveSegment_Opt_Diff col1Seg = new VLCActiveSegment_Opt_Diff(col1seglen,decodeLen);
					ret = oneDiff(col2Seg,col1Seg);
				}else{
					this.col1=col1;
					this.col2 = col2;
					
					VLCActiveSegment_Opt_Diff col1Seg = new VLCActiveSegment_Opt_Diff(col1seglen,decodeLen);
					VLCActiveSegment_Opt_Diff col2Seg = new VLCActiveSegment_Opt_Diff(col2seglen,decodeLen);
					ret = bothDiff(col1Seg,col2Seg);
				}
			}
		}
		return ret;
	}

	@Override
	public ActiveBitCollection OrQuery(ActiveBitCollection col1, ActiveBitCollection col2) {
		//TODO FILL THIS OUT??
		return null;
	}

	private ActiveBitCollection sameLen(VLCActiveSegment_Opt_Same col1Seg, VLCActiveSegment_Opt_Same col2Seg){
		ActiveBitCollection ret = new VLCActiveBitCol(decodeLen,"Res_"+col1.getColName()+"_AND_"+col2.getColName());
		Iterator<Long> col1It = col1.getSegmentIterator();
		Iterator<Long> col2It = col2.getSegmentIterator();
		
	
		//need to do this loop at least once even if there is only one segment
		do{
			//See if we need to fetch a new segment from either one of the columns
			if(col1Seg.numOfSegments() == 0){
				col1Seg.translateValue(col1It.next());
			}
			if(col2Seg.numOfSegments() == 0){
				col2Seg.translateValue(col2It.next());
			}
			
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
		//JS: this has to be overly complicated due to the fact that one column could pad that last word with zeros meaning that it has
		//empty more values than the other.  Luckily these last values can be ignored.  (TEST!!!!!!!!!!!!!)
		}while((col1It.hasNext() && col2It.hasNext())||(col1It.hasNext() && col2Seg.numOfSegments() != 0)||(col1Seg.numOfSegments()!=0 && col2It.hasNext()));	
	
	return ret;
		
	}
	
	private ActiveBitCollection oneDiff(VLCActiveSegment_Opt_Same col1Seg,VLCActiveSegment_Opt_Diff col2Seg){
		ActiveBitCollection ret = new VLCActiveBitCol(decodeLen,"Res_"+col1.getColName()+"_AND_"+col2.getColName());
		Iterator<Long> col1It = col1.getSegmentIterator();
		Iterator<Long> col2It = col2.getSegmentIterator();
		
	
		//need to do this loop at least once even if there is only one segment
		do{
			//See if we need to fetch a new segment from either one of the columns
			if(col1Seg.numOfSegments() == 0){
				col1Seg.translateValue(col1It.next());
			}
			if(col2Seg.numOfSegments() == 0){
				col2Seg.translateValue(col2It.next());
			}
			
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
		//JS: this has to be overly complicated due to the fact that one column could pad that last word with zeros meaning that it has
		//empty more values than the other.  Luckily these last values can be ignored.  (TEST!!!!!!!!!!!!!)
		}while((col1It.hasNext() && col2It.hasNext())||(col1It.hasNext() && col2Seg.numOfSegments() != 0)||(col1Seg.numOfSegments()!=0 && col2It.hasNext()));	
	
	return ret;
	}
	
	private ActiveBitCollection bothDiff(VLCActiveSegment_Opt_Diff col1Seg,VLCActiveSegment_Opt_Diff col2Seg){
		ActiveBitCollection ret = new VLCActiveBitCol(decodeLen,"Res_"+col1.getColName()+"_AND_"+col2.getColName());
		Iterator<Long> col1It = col1.getSegmentIterator();
		Iterator<Long> col2It = col2.getSegmentIterator();
		
	
		//need to do this loop at least once even if there is only one segment
		do{
			//See if we need to fetch a new segment from either one of the columns
			if(col1Seg.numOfSegments() == 0){
				col1Seg.translateValue(col1It.next());
			}
			if(col2Seg.numOfSegments() == 0){
				col2Seg.translateValue(col2It.next());
			}
			
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
		//JS: this has to be overly complicated due to the fact that one column could pad that last word with zeros meaning that it has
		//empty more values than the other.  Luckily these last values can be ignored.  (TEST!!!!!!!!!!!!!)
		}while((col1It.hasNext() && col2It.hasNext())||(col1It.hasNext() && col2Seg.numOfSegments() != 0)||(col1Seg.numOfSegments()!=0 && col2It.hasNext()));	
	
	return ret;
	}
	
	
	/**
	 * Helper function that returns a decompressed BitSet of the ActiveBitCollection passed in
	 * 
	 * @param The compressed column to be decompressed
	 * @return The decompressed column
	 * */
	private BitSet decodeFully(ActiveBitCollection col){
		BitSet res = new BitSet();
		//keeps track of the first bit that is not set in the set
		int end = 0;
		//Get an iterator for all the segments in the collection
		Iterator<Long> colIt = col.getSegmentIterator();
		//Get the first segment  this will blow up with the iterator is empty (which it probably should)
		VLCActiveSegment seg = new VLCActiveSegment(colIt.next(), col.getSeglen(), col.getSeglen());
		//using do while because it is possible it only contains the one segment and we wont this to happen
		do{
			//if we have exhausted the current segment get the next one
			if(seg.numOfSegments() == 0){
				seg = new VLCActiveSegment(colIt.next(), col.getSeglen(), col.getSeglen());
			}
			//while the segment isn't exhausted
			while(seg.numOfSegments()>0){
				
				if(seg.isFill()){
					//Fill is easy just set the next numSegs*segSize bits to the fill value 
					res.set(end, (int)((end+seg.numOfSegments()*col.getSeglen())), seg.getFillValue()==1);
					end = end+(int)((end+seg.numOfSegments()*col.getSeglen()));
					//don't forget to count the segments as used
					seg.usedNumWords(seg.numOfSegments());
				}else{ 
					//it is a literal have to do more work
					long curLit = seg.getLiteralValue();
					//need this to set the bits in the right order since we are taking the SLB first
					//we have to add it to end+offset
					int offset = col.getSeglen();
					//indexes into the bitset init the last pos
					//we are going to test the last bit and shift until curLit is 0.
					while(offset != 0){
						if(curLit % 2 != 0 ){
							res.set(end+offset-1);
						}
						offset--;
						//this is unsigned shift shifts a zero in to the MSB
						curLit = curLit >>> 1;
					}
					end+=col.getSeglen();
				}
			}
			
		}while(colIt.hasNext());
		
		return res;
	
	}
	
}
