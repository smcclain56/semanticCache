package bcf.compression;

public class WAHDeterminer implements SegmentationLengthDeterminer {

	@Override
	public int determineSegLen(BitStringRep[] table, int colNum) {
		return 31;
	}

}
