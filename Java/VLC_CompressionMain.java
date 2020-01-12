package bcf;
import java.io.*;
import java.util.ArrayList;

import bcf.common.*;
import bcf.compression.*;
import bcf.query.*;

public class VLC_CompressionMain {
	public static void main(String[] args){
		ArrayList<ActiveBitCollection> out = new ArrayList<ActiveBitCollection>(); //TESTING ONLY
		ArrayList<ActiveBitCollection> in = new ArrayList<ActiveBitCollection>();
		BitStringRep w[] = RawBitmapReader.readFileColumnFormat("C:\\Jason\\Research\\Bitmap\\TestDataSet\\test3.txt");
		VLCCompressor c = new VLCCompressor(new VLCBaseDeterminer(7),w);
		//Compressor c = new Compressor(new TestMultSegDet(),w);
		int sizeInBits = 0;
		int i =0;
		try{
			WriteCompressedColumn wcc = new VLCBinaryColumnWriter("C:\\Jason\\Research\\Bitmap\\TestDataSet\\Compressed\\", "test3");
			//go through the row file and compress the columns
			for(BitStringRep z: w){
				ActiveBitCollection a = c.compress(i);
				out.add(a);  //TESTING USE ONLY
				//add the size of the compressed column to the total size 
				sizeInBits += a.getSize();
				wcc.writeColumn(a);
				i++;
			}
			
			///////////////////////////TESTING USE ONLY ONLY /////////////////////
			CompressedBitmapReader cbr = new VLCCompressedReader();
			for(int z = 0; z<i; z++){
				File inf = new File("C:\\Jason\\Research\\Bitmap\\TestDataSet\\Compressed\\test3_col_"+z+".dat");
				FileInputStream file_input = new FileInputStream (inf);
				DataInputStream data_in    = new DataInputStream (file_input);
				
				//The cbr.readColumn reads the data in the file and returns an ActiveBit collection
				in.add(cbr.readColumn(data_in));
			}
			
		
			for(int q = 0; q < out.size(); q++){
				//System.out.println(q+"OUT: "+out.get(q));
				System.out.println(q+"IN:  "+in.get(q));
				
			}


		}catch(Exception e){
			e.printStackTrace();
		}
		System.out.println(i+" columns were compressed.  Their combined size is "+sizeInBits+" bits");
	}

}
