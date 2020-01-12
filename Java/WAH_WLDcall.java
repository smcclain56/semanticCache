package bcf;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import bcf.common.ActiveBitCollection;
import bcf.common.PLWAHActiveBitCol;
import bcf.common.VLCConstants;
import bcf.compression.BitStringRep;
import bcf.compression.PLWAHCompressor;
import bcf.compression.RawBitmapReader;
import bcf.compression.VLCBinaryColumnWriter;
import bcf.compression.VLCCompressor;
import bcf.compression.WAHDeterminer;
import bcf.compression.WriteCompressedColumn;
import bcf.query.CompressedBitmapReader;
import bcf.query.VLCCompressedReader;
import bcf.query.WAHAndQuery;

public class WAH_WLDcall {
	
	private static double sizeInBytes = 0;

	public static void main(String[] args){

		String qhf, query, output, preFix;
		
		if (args[2] != null) {
			qhf = args[2];
		} else {
			qhf = "/Users/wsuvgradstudent/Documents/workspace/general/bitmap_uniform/ZipfQuery_skew2_uniform.txt";
		}
		if (args[0] != null) {
		    preFix = VLCConstants.outDir + "/bitmap_" + args[0] +"_col_";
		} else {
			preFix = "/Users/wsuvgradstudent/Documents/workspace/general/bitmap_uniform/bitmap_out/bitmap_uniform_col_"; 
		}

		VLCCompressor c = new VLCCompressor(new WAHDeterminer(),VLCConstants.RAW_TABLE);

		int sizeInBits = 0;
		int i =0;
		try{
			WriteCompressedColumn wcc = new VLCBinaryColumnWriter(VLCConstants.outDir,VLCConstants.outFileExtension);

			//go through the row file and compress the columns
			for(BitStringRep z: VLCConstants.RAW_TABLE){
				ActiveBitCollection a = c.compress(i);
				sizeInBits += a.getSize();
				wcc.writeColumn(a);
				i++;
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
		sizeInBytes = Math.ceil((double) sizeInBits/8);
		System.out.println(i+" columns were compressed.  Their combined size is "+sizeInBits+" bits or "+sizeInBytes+" bytes.");
		
		// now run the query file
		try {
			// Total time to address all queries
			double totalTime = 0.0;
			// the query engine
			WAHAndQuery queryEng = new WAHAndQuery();
//////////////////////////////////////////////////////////////////////////////////////////////////////////
			//THERE MUST BE SOME STATIC THINGS THAT THE JVM DOESN'T INITIALIZE UNTIL QUERY IS CALLED
			//WITHOUT THIS START UP RUN THE QUERY TIMES TAKE A BIG INITIAL HIT
			ActiveBitCollection delme = new PLWAHActiveBitCol();
			delme.appendLiteral(35);
			queryEng.AndQuery(delme,delme);
///////////////////////////////////////////////////////////////////////////////////////////////////////
			// map that will hold all the read in query columns might get too
			// big
			HashMap<String, ActiveBitCollection> alreadyLoaded = new HashMap<String, ActiveBitCollection>();
			// create object used to read compressed bitmap files
			CompressedBitmapReader cbr = new VLCCompressedReader();
			// Set up some variables

			FileReader qfr = new FileReader(qhf);
			BufferedReader qbr = new BufferedReader(qfr);
			// Get the first line query file
			query = qbr.readLine();
			alreadyLoaded.clear();
			
			while (query != null && !query.equals("")) {

				// Split the query by the comma's in the file
				StringTokenizer strtoken = new StringTokenizer(query, ",");

				// This nextToken is to read the queryID
				strtoken.nextToken();

				// Find the column id of the first column to be queried.
				int value = Integer.parseInt((strtoken.nextToken()).trim());

				// Start a new tokenizer
				strtoken = new StringTokenizer(query, ",");

				// Get the queryid
				String queryid = strtoken.nextToken();

				// Set an array for the 4 columns to be read in.
				String[] qs = new String[4];

				// Get the number of tokens
				int toke = strtoken.countTokens();

				// Grab each token and stick it in our array.
				for (int j = 0; j < toke; j++) {
					qs[j] = strtoken.nextToken();
				}

				// Single point query
				if (qs[0].equals(qs[1])) {
					// Take two columns and AND them together.
					// Start timer
					if (!alreadyLoaded.containsKey(qs[0])) {
						File inf = new File( preFix + qs[0] + ".dat");
						FileInputStream file_input = new FileInputStream(inf);
						DataInputStream data_in = new DataInputStream(
								file_input);

						// The cbr.readColumn reads the data in the file and
						// returns an ActiveBit collection
						ActiveBitCollection a = cbr.readColumn(data_in);
						alreadyLoaded.put(qs[0], a);
						

					}
					if (!alreadyLoaded.containsKey(qs[2])
							&& !qs[0].equals(qs[2])) {
						File inf = new File( preFix + qs[2] + ".dat");
						FileInputStream file_input = new FileInputStream(inf);
						DataInputStream data_in = new DataInputStream(
								file_input);

						// The cbr.readColumn reads the data in the file and
						// returns an ActiveBit collection
						ActiveBitCollection a = cbr.readColumn(data_in);
						alreadyLoaded.put(qs[2], a);

					}
					ActiveBitCollection ans =null;
					long start = System.nanoTime();
					if (!qs[0].equals(qs[2])) {
						ans = queryEng.AndQuery(alreadyLoaded
								.get(qs[0]), alreadyLoaded.get(qs[2]));

					}else{
						ans= alreadyLoaded.get(qs[0]);
					}

					totalTime += (System.nanoTime() - start) / 1000000.0;
					//System.out.println("Result of "+qs[0]+" AND "+qs[2]+" : "+ans);
					//System.out.println("\tTotal time thus far = "+totalTime);
				}
				query = qbr.readLine();
			}
			String myAlgo = "algo_WAH";
            VLCConstants.expResults.put(WLD_QueryMain.recordCount+","+"WAH"+","+VLCConstants.OPT_SIZE+","+myAlgo,
            		"WAH"+","+sizeInBytes+","+totalTime);
            WLD_QueryMain.recordCount++;

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
