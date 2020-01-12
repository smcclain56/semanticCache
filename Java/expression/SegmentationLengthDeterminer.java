package bcf.compression;

public interface SegmentationLengthDeterminer {

	/**
	 * For the give column determines the segmentation length
	 * that should be used for encoding
	 * 
	 * @param table A column oriented representation of the bitmap
	 * @param colNum The id of the column that is to be compressed
	 * */
	public int determineSegLen(BitStringRep[] table, int colNum);
	
}
