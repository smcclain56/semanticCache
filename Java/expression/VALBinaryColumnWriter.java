package bcf.compression;

import java.io.*;
import java.util.Iterator;

import bcf.common.ActiveBitCollection;
import bcf.common.VLCConstants;


public class VALBinaryColumnWriter implements WriteCompressedColumn {
	
	/**Holds the string that writeColumn will append the column number to and write to.*/
	private String filePath;
	
	/**
	 * Constructor takes the path to the output folder and the 
	 * prefix for the file name.  writeColumn will append they column number 
	 * to the back of the fileprefix.  So if outfolder = C:\dataout\
	 * and filePrefix = Hep.  Writecolumn would create a new files
	 * C:\dataout\Hep_1.dat for the first column.  Column number is specified in
	 * ActiveBitCollection and so is seglen.
	 * 
	 * @param outFolder Fully qualified path to the output folder (it must be 
	 * created before this call)
	 * 
	 * @param filePrefix The name of column that the number will be appended to 
	 * when it is written. 
	 * */
	public VALBinaryColumnWriter(String outFolder, String filePrefix)throws IOException{
		//Make sure the file path ends with a file seperator
		if(!outFolder.endsWith(VLCConstants.fsp)){
			outFolder+=VLCConstants.fsp;
		}
		this.filePath = outFolder+filePrefix;
		//Test and make sure the folder exists
		File f = new File(outFolder);
		if(!f.exists()){
			throw new IOException(outFolder+" is not accessible. Please check file path.");
		}
		
	}

	/**
	 * Write the column to a dat file.  It will first write the seglen used
	 * to compress this column then the actual column.
	 * 
	 * @param col The compressed column to write.  The implementation must set the name of the col
	 * and have the segmentation len save in it.
	 * */
	@Override
	public void writeColumn(ActiveBitCollection col) throws IOException {
			DataOutputStream writer = this.getWriter(col.getColName());
			//first write the seglen to the file
			int seglen = col.getSeglen();
			writer.writeByte(seglen);
			//need to acount for the flag bit 
			seglen++;
			//now pack each segment into a word
			for(Iterator<Long> it = col.getSegmentIterator(); it.hasNext();){
				
				if(VLCConstants.WORD_LEN > 32){
					
					writer.writeLong(it.next());
				}else{
					Long temp = it.next();
					//System.out.println("Writing: "+col.getColName()+"  "+Long.toBinaryString(temp));
					writer.writeInt(temp.intValue());
				}
			}
			

	}

	/**
	 * Helper method that opens a data writer to the
	 * file that column  is to be written to.
	 * 
	 * @param colName the name of the column this is appended to the
	 * filePath above and is tyically the column number
	 * */
	
	private DataOutputStream getWriter(String colName) throws IOException{
		DataOutputStream writer = new DataOutputStream( new FileOutputStream( new File(this.filePath+"_col_"+colName+VLCConstants.fext)));
   
		return writer;
	}
}
