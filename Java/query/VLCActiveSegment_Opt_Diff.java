package bcf.query;

import bcf.common.ActiveSegment;
import bcf.common.HexHolder;

public class VLCActiveSegment_Opt_Diff implements ActiveSegment {

	
	private long litRepOfFill=0;
	private boolean isFill;
	private byte fillValue = 0;
	private long numSegments; 
	//it is an array because the literal value might need to be chunked if decodeLen < encodeLen
	private long litValues[];
	private int curLitPos = 0;
	private final long encodeOnesNoFlag;
	private final long decodeOnesNoFlag;
	private final long encodeOneRun;
	private final int diffBase;
	private final long encodeRunLen;

	private int decodeLen;
	
	/**
	 *The constructor just sets up the values for this segment
	 *
	 *@param value The long value of this segment
	 *@param encodedLen The segment length used to encode this value
	 *@param decodeLen The segment length to use while decoding, decodeLen has to be a 
	 *factor of encodedLen.
	 * */
	public VLCActiveSegment_Opt_Diff(int encodeLen, int decodeLen){
	
		this.decodeLen = decodeLen;
		
		HexHolder decodeHex = HexHolder.getHexHolder(decodeLen);
		HexHolder encodeHex = HexHolder.getHexHolder(encodeLen);
		//get the needed Hex values for encoding length
		this.encodeOneRun = encodeHex.getOneRun();
		this.encodeOnesNoFlag = encodeHex.getOnesNoFlag();
		this.encodeRunLen = encodeHex.getRunLen();
		
		//get the needed values for decoding Length
		this.decodeOnesNoFlag = decodeHex.getOnesNoFlag();
		
		this.diffBase = encodeLen/decodeLen;
		this.litValues = new long[diffBase];
	}
	
	public void translateValue(long value){
		this.curLitPos=0;
	
		
		this.isFill = value > encodeOnesNoFlag;
		
		if(isFill){
			//Determine if it is a run of ones or zeros
			if(value >= encodeOneRun){
				this.fillValue = 1;
				this.litRepOfFill = decodeOnesNoFlag;
			}else{
				this.fillValue = 0;
				this.litRepOfFill=0;
			}
				
			//how many words does the fill represent in the decodeLen
			this.numSegments = (value & encodeRunLen)*(this.diffBase);
		}else{
			
				
				
				int moveover = 0;
				//Have to go from MSB to LSB because when querying you match MSB's first.
				for(int i = (diffBase); i > 0; i--){
					this.litValues[i-1] = (value >> (decodeLen*moveover) & decodeOnesNoFlag );
					moveover++;
				}
				this.numSegments = diffBase;
			
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
		return this.litValues[this.curLitPos++];
		
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
