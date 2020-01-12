package bcf.query;

import bcf.common.ActiveSegment;
import bcf.common.HexHolder;

public class VLCActiveSegment_Opt_Same implements ActiveSegment{

	
	private long litRepOfFill=0;
	private boolean isFill;
	private byte fillValue = 0;
	private long numSegments; 
	//it is an array because the literal value might need to be chunked if decodeLen < encodeLen
	private long litValue;
	private final long encodeOnesNoFlag;
	private final long encodeOneRun;
	private final long encodeRunLen;

	
	
	/**
	 *The constructor just sets up the values for this segment
	 *
	 *@param value The long value of this segment
	 *@param encodedLen The segment length used to encode this value
	 *@param decodeLen The segment length to use while decoding, decodeLen has to be a 
	 *factor of encodedLen.
	 * */
	public VLCActiveSegment_Opt_Same(int encodeLen){
		
		HexHolder encodeHex = HexHolder.getHexHolder(encodeLen);
		//get the needed Hex values for encoding length
		this.encodeOneRun = encodeHex.getOneRun();
		this.encodeOnesNoFlag = encodeHex.getOnesNoFlag();
		this.encodeRunLen = encodeHex.getRunLen();
		
		//get the needed values for decoding Length
	}
	
	public void translateValue(long value){
	
		
		
		this.isFill = value > encodeOnesNoFlag;
		
		if(isFill){
			//Determine if it is a run of ones or zeros
			if(value >= encodeOneRun){
				this.fillValue = 1;
				this.litRepOfFill = encodeOnesNoFlag;
			}else{
				this.fillValue = 0;
				this.litRepOfFill=0;
			}
				
			//how many words does the fill represent in the decodeLen
			this.numSegments = (value & encodeRunLen);
		}else{
			this.litValue = value & encodeOnesNoFlag;
			this.numSegments = 1;
			
		}
		
	}
	
	@Override
	public byte getFillValue() {
		return this.fillValue;
	}

	@Override
	public long getLiteralValue() {
		this.numSegments--;
		//remember ++ evaluates first then increments
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
		
	}

	@Override
	public long getLiteralRepOfFill() {
		this.numSegments--;
		return this.litRepOfFill;
	}

}
