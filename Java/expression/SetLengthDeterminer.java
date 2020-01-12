package bcf.compression;

public class SetLengthDeterminer implements SegmentationLengthDeterminer {
	/**The length that will always be returned by this class when determineSegLen is called*/
	private int seglen;
	
	/**
	 * @param the length that this determiner will always return
	 * */
	public SetLengthDeterminer(int seglen){
		this.seglen = seglen;
	}

	@Override
	public int determineSegLen(BitStringRep[] table, int colNum) {
		return seglen;
	}

}
