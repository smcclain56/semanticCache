package bcf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import bcf.common.*;
import bcf.query.CompressedBitmapReader;
import bcf.query.VLCAndQueryOPT;
import bcf.query.VLCCompressedReader;

public class WLD_QueryMain {
	
    /** Holds the total index size after optimized */
    //public static double CurFSize = 0;
    public static int recordCount = 1;
    public static HashMap<String, ActiveBitCollection> alreadyLoaded = new HashMap<String, ActiveBitCollection>();
    private static boolean createLog = false;
    
	public static void main(String[] args) {
		
		VLCConstants.QHistoryFreq.clear();
		VLCConstants.QRel.clear();

		try {
			// Total time to address all queries
			double totalTime = 0.0;
			// the query engine
			VLCAndQueryOPT queryEng = new VLCAndQueryOPT();
//////////////////////////////////////////////////////////////////////////////////////////////////////////
			//THERE MUST BE SOME STATIC THINGS THAT THE JVM DOESN'T INITIALIZE UNTIL QUERY IS CALLED
			//WITHOUT THIS START UP RUN THE QUERY TIMES TAKE A BIG INITIAL HIT
			ActiveBitCollection delme = new VLCActiveBitCol(28,"-9");
			delme.appendLiteral(35);
			queryEng.AndQuery(delme,delme);
///////////////////////////////////////////////////////////////////////////////////////////////////////
			// map that will hold all the read in query columns might get too
			// big
			
			// create object used to read compressed bitmap files
			CompressedBitmapReader cbr = new VLCCompressedReader();
			// Set up some variables
			String qhf, query, preFix;
			// Open a reader to the query files
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
			
			FileWriter logSum = null;
	    	BufferedWriter writer = null;
			Pattern p = Pattern.compile("(\\w+|\\w+[-]\\w+)[\\.]txt$");
			Matcher m = p.matcher(qhf);
			String logFile, output;
			if (m.find()) {
				createLog = true;
				logFile = m.find(1)+"";
				logFile = VLCConstants.baseDir + "log/"+ logFile + "_algo_"+VLCConstants.algoOption+"_K_"+VLCConstants.OPT_SIZE + "_lambda_" + VLCConstants.LAMBDA+"_query.log";
				logSum = new FileWriter(logFile);
				writer = new BufferedWriter(logSum);
		    	writer.write("query,querytime,totalquerytime,opttime,indexsize,colsrecompressed,avgseglen" + VLCConstants.newLine);
		    	writer.flush();
			} else {
				System.out.println("----> Failed to create log file name, skip logging <----");
			}

			System.out.println("query file:"+qhf);
			FileReader qfr = new FileReader(qhf);
			BufferedReader qbr = new BufferedReader(qfr);
			
			// Get the first line query file
			query = qbr.readLine();
			int nline = 0;
			//alreadyLoaded.clear();

			if (createLog) {
				output = nline + ",";	//query
				output += totalTime + ",";	//querytime
				output += 0 + ",";	//opttime
				output += VLCConstants.totalCurFsize + ",";	//indexsize
				output += VLCConstants.colCompressed + ",";	//colsrecompressed
				double sumSeglen = 0.0;
            	for (int i=0; i<VLCConstants.numCols; i++)
            	{
            		sumSeglen += VLCConstants.ColEncode.get(""+i);
            	}
            	output += sumSeglen/VLCConstants.numCols + ",";
            	//output += VLCConstants.ColEncode.get(qs[1]) + "," + VLCConstants.ColEncode.get(qs[2]);
	    		writer.write(output + VLCConstants.newLine);
	    		writer.flush();
			}

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

				if (nline % VLCConstants.OPT_SIZE == 0 && nline > 0) {

					long start_opt = System.nanoTime();
					// run optimizer
					if (VLCConstants.algoOption == 1) {
						WLDAlgo1.runQoptimize(); 
					} else if (VLCConstants.algoOption == 2) {
						WLDAlgo2.runQoptimize();  
					} else {
						WLDAlgo3.runQoptimize(); 
					}
					double opt_time = (System.nanoTime()-start_opt)/1000000.0;

					if (createLog) {
						output = nline + ",";	//query
						output += totalTime + ",";	//querytime
						output += opt_time + ",";	//opttime
						output += VLCConstants.totalCurFsize + ",";	//indexsize
						output += VLCConstants.colCompressed + ",";	//colsrecompressed
						double sumSeglen = 0.0;
	                	for (int i=0; i<VLCConstants.numCols; i++)
	                	{
	                		sumSeglen += VLCConstants.ColEncode.get(""+i);
	                	}
	                	output += sumSeglen/VLCConstants.numCols + ",";
	                	//output += VLCConstants.ColEncode.get(qs[1]) + "," + VLCConstants.ColEncode.get(qs[2]);
	    	    		writer.write(output + VLCConstants.newLine);
	    	    		writer.flush();
					}

					System.out.println("Optimization time: "+opt_time+" millisecond"+", new index size: "+VLCConstants.totalCurFsize+", total query time: "+totalTime);
				} else {

					// Single point query
					if (qs[0].equals(qs[1])) {
			        	ArrayList<String> l1, l2;
			        	
			        	// create query frequency records for first column
			        	if (VLCConstants.QHistoryFreq.containsKey(qs[0])) {  
			        		VLCConstants.QHistoryFreq.put(qs[0],VLCConstants.QHistoryFreq.get(qs[0])+1.0);
			        	} else {
			        		VLCConstants.QHistoryFreq.put(qs[0],1.0);
			        	}
			        	
			        	// create relationship table
			            if (VLCConstants.QRel.containsKey(qs[0])) {
			                l1 = VLCConstants.QRel.get(qs[0]);
			                l1.add(qs[2]);
			            } else {
			                l1 = new ArrayList<String>();
			                l1.add(qs[2]);
			                VLCConstants.QRel.put(qs[0], l1);
			            }
			            
			         	if (!qs[0].equals(qs[2])) {  // record only if the two cols are different
			         		                         // frequency for 2nd column
			        		if (VLCConstants.QHistoryFreq.containsKey(qs[2])) {
			        			VLCConstants.QHistoryFreq.put(qs[2],VLCConstants.QHistoryFreq.get(qs[2])+1.0);
			        		} else {
			        			VLCConstants.QHistoryFreq.put(qs[2],1.0);
			        		}
			        		
			                if (VLCConstants.QRel.containsKey(qs[2])) {
			                    l2 = VLCConstants.QRel.get(qs[2]);
			                    l2.add(qs[0]);
			                } else {
			                    l2 = new ArrayList<String>();
			                    l2.add(qs[0]);
			                    VLCConstants.QRel.put(qs[2], l2);
			                }
			        	}
			        	
			        	if (VLCConstants.QHistoryFreq.containsKey("total")) {
			        		VLCConstants.QHistoryFreq.put("total",VLCConstants.QHistoryFreq.get("total")+1);
			        	} else {
			        		VLCConstants.QHistoryFreq.put("total",1.0);
			        	}

						// Take two columns and AND them together.
						// Start timer
						if (!alreadyLoaded.containsKey(qs[0])) {
							File inf = new File( preFix + qs[0] + ".dat");

							FileInputStream file_input = new FileInputStream(inf);
							DataInputStream data_in = new DataInputStream(file_input);

							// The cbr.readColumn reads the data in the file and
							// returns an ActiveBit collection
							ActiveBitCollection a = cbr.readColumn(data_in);
							alreadyLoaded.put(qs[0], a);


						}
						if (!alreadyLoaded.containsKey(qs[2])
								&& !qs[0].equals(qs[2])) {

							File inf = new File(preFix + qs[2] + ".dat");

							FileInputStream file_input = new FileInputStream(inf);
							DataInputStream data_in = new DataInputStream(file_input);

							// The cbr.readColumn reads the data in the file and
							// returns an ActiveBit collection
							ActiveBitCollection a = cbr.readColumn(data_in);
							alreadyLoaded.put(qs[2], a);

						}
						ActiveBitCollection ans =null;

						/////// actual query processing ///////
						long start = System.nanoTime();
						if (!qs[0].equals(qs[2])) {
							ans = queryEng.AndQuery(alreadyLoaded
									.get(qs[0]), alreadyLoaded.get(qs[2]));

						}else{
							ans= alreadyLoaded.get(qs[0]);
						}

						totalTime += (System.nanoTime() - start) / 1000000.0;
						//System.out.println("Result of "+alreadyLoaded.get(qs[0])+" AND "+alreadyLoaded.get(qs[2])+" : "+ans);
						//System.out.println("\tTotal time thus far = "+totalTime);
					}
				}
                nline++;
				query = qbr.readLine();
			}

			/// last optimization ///
			long start_opt = System.nanoTime();
			// run optimizer
			if (VLCConstants.algoOption == 1) {
				WLDAlgo1.runQoptimize(); 
			} else if (VLCConstants.algoOption == 2) {
				WLDAlgo2.runQoptimize();  
			} else {
				WLDAlgo3.runQoptimize(); 
			}
			double opt_time = (System.nanoTime()-start_opt)/1000000.0;
			
			if (createLog) {
				output = nline + ",";	//query
				output += totalTime + ",";	//querytime
				output += opt_time + ",";	//opttime
				output += VLCConstants.totalCurFsize + ",";	//indexsize
				output += VLCConstants.colCompressed + ",";	//colsrecompressed
				double sumSeglen = 0.0;
            	for (int i=0; i<VLCConstants.numCols; i++)
            	{
            		sumSeglen += VLCConstants.ColEncode.get(""+i);
            	}
            	output += sumSeglen/VLCConstants.numCols + ",";
            	//output += VLCConstants.ColEncode.get(qs[1]) + "," + VLCConstants.ColEncode.get(qs[2]);
	    		writer.write(output + VLCConstants.newLine);
	    		writer.flush();

		    	writer.close();
		    	logSum.close();				
			}
			
			String myAlgo = "algo_"+VLCConstants.algoOption;
            VLCConstants.expResults.put(recordCount+","+VLCConstants.LAMBDA+","+VLCConstants.OPT_SIZE+","+myAlgo,
            		nline+","+VLCConstants.totalCurFsize+","+totalTime);
            recordCount++;
            System.out.println("Last optimization time: "+opt_time+" millisecond"+", new index size: "+VLCConstants.totalCurFsize+", total query time: "+totalTime);
            totalTime = 0;

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
