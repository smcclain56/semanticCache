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


public class PLWAH_CompressionMain {
	public static void main(String[] args){
		//This reads in the raw bitmap 			
		//BitStringRep w[] = RawBitmapReader.readFileColumnFormat("/Users/wsuvgradstudent/Documents/workspace/general/bitmap_uniform/bitmap_uniform.txt");
		//BitStringRep w[] = RawBitmapReader.readFileColumnFormat("/Users/wsuvgradstudent/Documents/workspace/general/europe_horizontal/europe_horizontal.txt");
		BitStringRep w[] = RawBitmapReader.readFileColumnFormat("/Users/wsuvgradstudent/Documents/workspace/general/record_linkage/record_linkage_bitmap.txt");
		//Set up the object that will be compressing the files
		PLWAHCompressor c = new PLWAHCompressor(w);
		int sizeInBits =0;
		int i = 0;
		try{
			//This will write the compressed files in binary form
			WriteCompressedColumn wcc = new PLWAHBinaryColumnWriter("/Users/wsuvgradstudent/Documents/workspace/general/record_linkage/bitmap_output", "record_linkage");
			//WriteCompressedColumn wcc = new PLWAHBinaryColumnWriter("/Users/wsuvgradstudent/Documents/workspace/general/bitmap_uniform/bitmap_output", "bitmap_uniform");
//			WriteCompressedColumn wcc = new PLWAHBinaryColumnWriter("/Users/wsuvgradstudent/Documents/workspace/bitmap_skysurvey48/PLWAH", "bitmap_skysurvey48");
			//go through the row file and compress the columns
			for(BitStringRep z: w){
				ActiveBitCollection a = c.compress(i);
				//add the size of the compressed column to the total size 
				sizeInBits += a.getSize();
				wcc.writeColumn(a);
				
				i++;
			}
			
			

		}catch(Exception e){
			e.printStackTrace();
		}
		System.out.println(i+" columns were compressed.  Their combined size is "+sizeInBits+" bits");
		System.out.println(i+" columns were compressed.  Their combined size is "+sizeInBits/8.0+" bytes");
	}

}
