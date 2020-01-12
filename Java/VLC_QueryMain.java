package bcf;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;

import bcf.common.ActiveBitCollection;
import bcf.common.PLWAHActiveBitCol;
import bcf.common.VALActiveBitCollection;
import bcf.common.VLCActiveBitCol;
import bcf.query.CompressedBitmapReader;
import bcf.query.PLWAHAndQuery;
import bcf.query.PLWAHCompressedReader;
import bcf.query.VALAndQuery;
import bcf.query.VALCompressedReader;
import bcf.query.VLCAndQuery;
import bcf.query.VLCCompressedReader;



public class VLC_QueryMain {
	public static void main(String[] args) {
		try {
			// Total time to address all queries
			double totalTime = 0.0;
			// the query engine
			VLCAndQuery queryEng = new VLCAndQuery();
//////////////////////////////////////////////////////////////////////////////////////////////////////////
			//THERE MUST BE SOME STATIC THINGS THAT THE JVM DOESN'T INITIALIZE UNTIL QUERY IS CALLED
			//WITHOUT THIS START UP RUN THE QUERY TIMES TAKE A BIG INITIAL HIT
			ActiveBitCollection delme = new VLCActiveBitCol(28,"-9");
			delme.appendLiteral(35);
			queryEng.AndQuery(delme,delme);
///////////////////////////////////////////////////////////////////////////////////////////////////////
			// map that will hold all the read in query columns might get too
			// big
			HashMap<String, ActiveBitCollection> alreadyLoaded = new HashMap<String, ActiveBitCollection>();
			// create object used to read compressed bitmap files
			CompressedBitmapReader cbr = new VLCCompressedReader();
			// Set up some variables
			String qhf, query, output;
			// Open a reader to the query files
			qhf = "C:\\Jason\\Research\\Bitmap\\TestDataSet\\test1Query.txt";

			FileReader qfr = new FileReader(qhf);
			BufferedReader qbr = new BufferedReader(qfr);
			// Get the first line query file
			query = qbr.readLine();

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
				for (int i = 0; i < toke; i++) {
					qs[i] = strtoken.nextToken();
				}

				// Single point query
				if (qs[0].equals(qs[1])) {
					// Take two columns and AND them together.
					// Start timer
					if (!alreadyLoaded.containsKey(qs[0])) {
						File inf = new File(
								"C:\\Jason\\Research\\Bitmap\\TestDataSet\\Compressed\\test3_col_"
										+ qs[0] + ".dat");
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
						File inf = new File(
								"C:\\Jason\\Research\\Bitmap\\TestDataSet\\Compressed\\test3_col_"
										+ qs[2] + ".dat");
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
					System.out.println("Result of "+alreadyLoaded.get(qs[0])+" AND "+alreadyLoaded.get(qs[2])+" : "+ans);
					System.out.println("\tTotal time thus far = "+totalTime);
				}
				query = qbr.readLine();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
