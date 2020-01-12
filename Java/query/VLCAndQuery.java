package bcf.query;



import java.util.BitSet;
import java.util.Iterator;

import bcf.common.VLCActiveBitCol;
import bcf.common.ActiveBitCollection;
import bcf.common.VLCConstants;


public class VLCAndQuery implements PointQuery {

	/** Takes two compressed columns and performs a logical AND
	 * operation on them. The results are returned in a bit vector
	 * 
	 * @param col1 A compressed column for querying 
	 * @param col2 A compressed column for querying
	 * @return  the result of col1 AND col2
	 * */
	public ActiveBitCollection AndQuery(ActiveBitCollection col1, ActiveBitCollection col2) {
		//Find the GCD and use that as the decoding len
		int decodeLen = VLCConstants.GCD_MATRIX[col1.getSeglen()][col2.getSeglen()];
		
		ActiveBitCollection ret=null;
		//make sure we get some benefit out of still encoding it
		if(decodeLen>3){
			//create the result bitcollection 
			ret = new VLCActiveBitCol(decodeLen,"Res_"+col1.getColName()+"_AND_"+col2.getColName());
			Iterator<Long> col1It = col1.getSegmentIterator();
			Iterator<Long> col2It = col2.getSegmentIterator();
			//These decode the segments into the decodeLen
			VLCActiveSegment col1Seg = new VLCActiveSegment(col1It.next(),col1.getSeglen(),decodeLen);
			VLCActiveSegment col2Seg = new VLCActiveSegment(col2It.next(),col2.getSeglen(),decodeLen);
			//need to do this loop at least once even if there is only one segment
			do{
				//See if we need to fetch a new segment from either one of the columns
				if(col1Seg.numOfSegments() == 0){
					col1Seg = new VLCActiveSegment(col1It.next(),col1.getSeglen(),decodeLen);
				}
				if(col2Seg.numOfSegments() == 0){
					col2Seg = new VLCActiveSegment(col2It.next(),col2.getSeglen(),decodeLen);
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
		}else{
			//Total decoding.
				BitSet col1BS = decodeFully(col1);
				BitSet col2BS = decodeFully(col2);
				//ands them together
				col1BS.and(col2BS);
				//have to encode it back into a ActiveBitCollection use 
				//biggest encoding of the two -- it would be better to use the 
				//biggest encoding possible but there is currently no way of finding
				//that out from the info in this class
				int maxSize = Math.max(col1.getSeglen(), col2.getSeglen());
				ret =  new VLCActiveBitCol(maxSize,"Res_"+col1.getColName()+"_AND_"+col2.getColName());
				//Go through the Bitset and translate it into a ActiveBitCollection
				for(int i = 0; i < col1BS.size();){
					//This will hold the value that is added to the ABC
					long value=0;
					//break the bitset in to segements
					for(int z = 0; z<maxSize; z++){
						value= value<<1;
						if(col1BS.get(i)){
							value +=1;
						}
						//this increases the postion of the outer loop
						i++;
					}
					//apped it to the the ABC this will combine runs if needed.
					ret.appendLiteral(value);
				}	
			
		}
		return ret;
	}

	@Override
	public ActiveBitCollection OrQuery(ActiveBitCollection col1, ActiveBitCollection col2) {
		//TODO FILL THIS OUT?
		return null;
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
