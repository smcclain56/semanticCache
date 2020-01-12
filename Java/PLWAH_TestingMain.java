package bcf;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

import bcf.common.ActiveBitCollection;
import bcf.compression.BitStringRep;
import bcf.compression.VLCCompressor;
import bcf.compression.PLWAHBinaryColumnWriter;
import bcf.compression.PLWAHCompressor;
import bcf.compression.RawBitmapReader;
import bcf.compression.SetLengthDeterminer;
import bcf.compression.VLCBinaryColumnWriter;
import bcf.compression.VLCOptDeterminer;
import bcf.compression.WriteCompressedColumn;
import bcf.query.CompressedBitmapReader;
import bcf.query.PLWAHAndQuery;
import bcf.query.PLWAHCompressedReader;
import bcf.query.VLCAndQuery;
import bcf.query.VLCCompressedReader;


public class PLWAH_TestingMain {
	public static void main(String[] args){
		//This reads in the raw bitmap 
		BitStringRep w[] = RawBitmapReader.readFileColumnFormat("C:\\Jason\\Research\\Bitmap\\TestDataSet\\test1.txt");
		//Set up the object that will be compressing the files
		PLWAHCompressor c = new PLWAHCompressor(w);
		
		int i = 0;
		try{
			//probably want to remove these they are just for testing purposes (saving the columns would be very costly)(
			ArrayList<ActiveBitCollection> out = new ArrayList<ActiveBitCollection>();
			ArrayList<ActiveBitCollection> in = new ArrayList<ActiveBitCollection>();
			
			//This will write the compressed files in binary form
			WriteCompressedColumn wcc = new PLWAHBinaryColumnWriter("C:\\Jason\\Research\\Bitmap\\TestDataSet\\Compressed\\", "test1");
			//go through the row file and compress the columns
			for(BitStringRep z: w){
				ActiveBitCollection a = c.compress(i);
////////////////////////////////////////////////////////////////////////////////////////////////
				//REMOVE THIS AFTER TESTING 
				out.add(a);
////////////////////////////////////////////////////////////////////////////////////////////////
				wcc.writeColumn(a);
				
				i++;
			}
			
////////////////////////////////////////////////////////////////////////////
			//READING THE FILES BACK IN ONLY FOR TESTING
///////////////////////////////////////////////////////////////////////////
			//This reader reads compress PLWAH Binary files
			CompressedBitmapReader cbr = new PLWAHCompressedReader();
			for(int z = 0; z<i; z++){
				File inf = new File("C:\\Jason\\Research\\Bitmap\\TestDataSet\\Compressed\\test1_col_"+z+".dat");
				FileInputStream file_input = new FileInputStream (inf);
				DataInputStream data_in    = new DataInputStream (file_input);
				
				//The cbr.readColumn reads the data in the file and returns an ActiveBit collection
				in.add(cbr.readColumn(data_in));
			}
			
			///////////////////////////TESTING ONLY/////////////////////
			for(int q = 0; q < out.size(); q++){
				System.out.println(q+"OUT: "+out.get(q));
				System.out.println(q+"IN:  "+in.get(q));
				
			}
			//////////////////////////////////////////////////////
			
			
			//Query eng can only query 1 columns together
			PLWAHAndQuery queryEng = new PLWAHAndQuery();
			System.out.println("0 : "+in.get(0));
			System.out.println("4 : "+in.get(4));
			ActiveBitCollection ans = queryEng.AndQuery(in.get(0),in.get(4));
			System.out.println("Ans="+ans);
		
		}catch(Exception e){
			System.out.println("BAD NEWS");
			e.printStackTrace();
		}

	}

}
