package bcf.query;

import bcf.common.ActiveSegment;
import bcf.common.HexHolder;

public class VLCActiveSegment implements ActiveSegment {

	
	private long litRepOfFill=0;
	private boolean isFill;
	private byte fillValue = 0;
	private long numSegments; 
	//it is an array because the literal value might need to be chunked if decodeLen < encodeLen
	private long litValues[];
	private int curLitPos = 0;
	/**
	 *The constructor just sets up the values for this segment
	 *
	 *@param value The long value of this segment
	 *@param encodedLen The segment length used to encode this value
	 *@param decodeLen The segment length to use while decoding, decodeLen has to be a 
	 *factor of encodedLen.
	 * */
	public VLCActiveSegment(long value, int encodeLen, int decodeLen){
		HexHolder decodeHex = HexHolder.getHexHolder(decodeLen);
		HexHolder encodeHex = HexHolder.getHexHolder(encodeLen);
		//Determines if the value is a fill 
		this.isFill = value > encodeHex.getOnesNoFlag();
		
		if(isFill){
			//Determine if it is a run of ones or zeros
			if(value >= encodeHex.getOneRun()){
				this.fillValue = 1;
				this.litRepOfFill = decodeHex.getOnesNoFlag();
			}else{
				this.fillValue = 0;
			}
				
			//how many words does the fill represent in the decodeLen
			this.numSegments = (value & encodeHex.getRunLen())*(encodeLen/decodeLen);
		}else{
			//strictly speaking this if isn't necessary (it would work even if they were the same)
			//but to save time in the case where they are the same 
			if(encodeLen!=decodeLen){
				this.litValues = new long[encodeLen/decodeLen];
				
				int moveover = 0;
				//Have to go from MSB to LSB because when querying you match MSB's first.
				for(int i = (encodeLen/decodeLen); i > 0; i--){
					this.litValues[i-1] = (value >> (decodeLen*moveover) & decodeHex.getOnesNoFlag());
					moveover++;
				}
				this.numSegments = encodeLen/decodeLen;
			}else{
				this.litValues = new long[1];
				this.litValues[0] = value & encodeHex.getOnesNoFlag();
				this.numSegments = 1;
			}
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
