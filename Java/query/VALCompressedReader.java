package bcf.query;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

import bcf.common.VALActiveBitCollection;
import bcf.common.VLCActiveBitCol;
import bcf.common.ActiveBitCollection;
import bcf.common.HexHolder;
import bcf.common.VLCConstants;


public class VALCompressedReader implements CompressedBitmapReader {
	/**
	 * Assumes that the column is prefaced with a byte that indicates
	 * the segmentation length used to encode it. It also assumes
	 * that segments were packed in 32 bit words
	 * 
	 * @return the ActiveBitCollection representation of the column
	 */
	@Override
	public ActiveBitCollection readColumn(DataInputStream columnIn) {
		try{
			
			//read in segment length
			int seglen = columnIn.readByte();
			//add one to account for the flag bit
			//seglen++;
			//Create a bitVector with the name of the column id
			ActiveBitCollection column = new VALActiveBitCollection(seglen,""+columnIn);
			//need to calculate some hex values for parsing
			//HexHolder curHex = HexHolder.getHexHolder(seglen);
			//read in the data file until you get an EOF exception then break
			//not the best way to end the loop but DataInputStreams doesn't have
			//and end of file check
			//number of total bits per segment (includes flag)
			//int segLenPlusFlag = seglen+1;
			while (true)
			{
			
				try
				{	//Read a word in at a time: for seglens longer than 32 we'll need a long
					int temp = columnIn.readInt();
					//damn sign extension is removed by the following couple of lines
					long val = (long)temp;
					val = val &  0xFFFFFFFFL;
					
					column.appendWord(val);
					
				}
				//Thrown only after all the data is read from the file
				catch (EOFException eof)
				{
					break;
				}
			}
			return column;
		}catch(IOException ex){
			ex.printStackTrace();
		}
		return null;
	}

}