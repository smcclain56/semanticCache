package bcf.query;

import bcf.common.ActiveSegment;
import bcf.common.HexHolder;

public class WAHActiveSegment implements ActiveSegment {

	
	private long litRepOfFill=0;
	private boolean isFill;
	private byte fillValue = 0;
	private long numSegments; 
	//it is an array because the literal value might need to be chunked if decodeLen < encodeLen
	private long litValues;

	/**
	 *The constructor just sets up the values for this segment
	 * 
	 *@param value
	 *            The long value of this segment
	 * */
	public WAHActiveSegment(long value) {
		HexHolder encodeHex = HexHolder.getHexHolder(31);
		// Determines if the value is a fill
		this.isFill = value > encodeHex.getOnesNoFlag();

		if (isFill) {
			// Determine if it is a run of ones or zeros
			if (value >= encodeHex.getOneRun()) {
				this.fillValue = 1;
				this.litRepOfFill = encodeHex.getOnesNoFlag();
			} else {
				this.fillValue = 0;
			}

			// how many words does the fill represent
			this.numSegments = (value & encodeHex.getRunLen());
		} else {

			this.litValues = value & encodeHex.getOnesNoFlag();
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
		return this.litValues;
		
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
