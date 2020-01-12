package bcf;
import java.io.*;
import java.util.ArrayList;

import bcf.common.*;
import bcf.compression.*;
import bcf.query.*;

public class VLC_TestingMain {
	public static void main(String[] args){
		BitStringRep w[] = RawBitmapReader.readFileColumnFormat("C:\\Jason\\Research\\Bitmap\\TestDataSet\\test3.txt");
		VLCCompressor c = new VLCCompressor(new VLCOptDeterminer(),w);
		//Compressor c = new Compressor(new TestMultSegDet(),w);
		int i = 0;
		try{
			ArrayList<ActiveBitCollection> out = new ArrayList<ActiveBitCollection>();
			ArrayList<ActiveBitCollection> in = new ArrayList<ActiveBitCollection>();
			WriteCompressedColumn wcc = new VLCBinaryColumnWriter("C:\\Jason\\Research\\Bitmap\\TestDataSet\\Compressed\\", "test1");
			
			for(BitStringRep z: w){
				//System.out.println(z.getBitString()+"   "+z.getBitString().length());
				ActiveBitCollection a = c.compress(i);
				out.add(a);
				wcc.writeColumn(a);
				//System.out.println("ACTIVE " +a.toString());
				i++;
			}
			
			CompressedBitmapReader cbr = new VLCCompressedReader();
			for(int z = 0; z<i; z++){
				File inf = new File("C:\\Jason\\Research\\Bitmap\\TestDataSet\\Compressed\\VerySmall_col_"+z+".dat");
				FileInputStream file_input = new FileInputStream (inf);
				DataInputStream data_in    = new DataInputStream (file_input);
				in.add(cbr.readColumn(data_in));
			}
		
			for(int q = 0; q < in.size(); q++){
				System.out.println(q+"OUT: "+out.get(q));
				System.out.println(q+" IN: "+in.get(q));
			}
			
			VLCAndQuery queryEng = new VLCAndQuery();
			System.out.println("0 : "+in.get(10));
			System.out.println("6 : "+in.get(11));
			ActiveBitCollection ans = queryEng.AndQuery(in.get(11),in.get(10));
			System.out.println("Answer = "+ans);
		
		}catch(Exception e){
			e.printStackTrace();
		}

	}

}
