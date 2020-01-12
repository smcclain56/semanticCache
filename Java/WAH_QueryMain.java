package bcf;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import bcf.common.ActiveBitCollection;
import bcf.common.PLWAHActiveBitCol;
import bcf.query.*;
import javax.management.Query;

import static java.lang.System.exit;

public class WAH_QueryMain {
	public static void main(String[] args) {
			try {
				// Total time to address all queries
				long diskTimeStart =0;
				long diskTimeEnd =0;

				//For running different tests
				long lookupTime = 0;
				long diskTime = 0;
				long overheadTime =0;
				long avgCoverage =0;
				long avgNumRanges =0;
				int numQueries=0;

                int coverage = 0;
                int numRanges = 0;

//////////////////////////////////////////////////////////////////////////////////////////////////////////
				//THERE MUST BE SOME STATIC THINGS THAT THE JVM DOESN'T INITIALIZE UNTIL QUERY IS CALLED
				//WITHOUT THIS START UP RUN THE QUERY TIMES TAKE A BIG INITIAL HIT
				ActiveBitCollection delme = new PLWAHActiveBitCol();
				delme.appendLiteral(35);
				//queryEng.query(delme,delme); //TODO WHAT IS THIS LINE FOR
///////////////////////////////////////////////////////////////////////////////////////////////////////
				// map that will hold all the read in query columns might get too
				// big
				HashMap<Integer, ActiveBitCollection> columns = new HashMap<Integer, ActiveBitCollection>();
				SemanticCache cache = new SemanticCache();
				HashMap<Integer, ArrayList<Integer>> coverageRanges = new HashMap<Integer, ArrayList<Integer>>();
				HashMap<Integer, ArrayList<Double>> coverageTimes = new HashMap<Integer, ArrayList<Double>>();

				// create object used to read compressed bitmap files
				CompressedBitmapReader cbr = new VLCCompressedReader();
				// Set up some variables
				String qhf, query, output, preFix;
				// Open a reader to the query files
//			qhf = "/Users/wsuvgradstudent/Documents/workspace/general/record_linkage/skew2_queries_linkage.txt";
//			preFix = "/Users/wsuvgradstudent/Documents/workspace/general/record_linkage/bitmap_output/record_linkage_col_";
				//qhf = "/Users/wsuvgradstudent/Documents/workspace/general/bitmap_hep/ZipfQuery_skew0_hep.txt";
				//preFix = "/Users/wsuvgradstudent/Documents/workspace/general/bitmap_hep/bitmap_output/bitmap_hep_col_";

				// 1000 row bitmap
				qhf = "/Users/mccla/IdeaProjects/Bitmap/BitmapWorkloadGenerator/src/1,000Rows/1,000Q-0-100/query_out.txt";
				preFix = "/Users/mccla/IdeaProjects/Bitmap/BitmapWorkloadGenerator/src/1,000Rows/1,000Q-20-80/bitmap_out_gc.txt_UNSTRIPED_1_COMPRESSED/col_";

				//qhf = "/Users/mccla/IdeaProjects/Bitmap/BitmapWorkloadGenerator/query_out.txt"; //only ~50 queries

                // 1 million row bitmap
//				qhf="/Users/mccla/IdeaProjects/Bitmap/BitmapWorkloadGenerator/src/100,000Q-0-100/query_out.txt";
//				preFix = "/Users/mccla/IdeaProjects/Bitmap/BitmapWorkloadGenerator/src/1,000Q-20-80/bitmap_out_gc.txt_UNSTRIPED_1_COMPRESSED/col_";


				WAHCompoundQuery cmpQueryEng;
				FileReader qfr = new FileReader(qhf);
				BufferedReader qbr = new BufferedReader(qfr);
				// Get the first line query file
				query = qbr.readLine();
				int line = 0;

				long start = System.nanoTime();
				while (query != null && !query.equals("")) {
					if (query.startsWith("#")) { // skips the comments denoted by #
						query = qbr.readLine();
						continue;
					}
					line++;

					// Split the query by the comma's and brackets in the file
					//System.out.println("\nQUERY " + query);
					numQueries++;
					StringTokenizer strtoken = new StringTokenizer(query, "[//]//,");

					// Get the number of tokens
					int toke = strtoken.countTokens();

					// Set an array for the columns to be read in.
					String[] qs = new String[toke];

					// Grab each token and stick it in our array.
					for (int i = 0; i < toke; i++) {
						qs[i] = strtoken.nextToken();
					}

					int i = 0;
					while (i < toke) {
						// loop through the query and load all the columns
						diskTimeStart = System.nanoTime();
						for (int k = Integer.parseInt(qs[i]); k <= Integer.parseInt(qs[i + 1]); k++) {
							if (!columns.containsKey(k)) { //if column is not loaded
								File inf = new File(preFix + k + ".dat");
								FileInputStream file_input = new FileInputStream(inf);
								DataInputStream data_in = new DataInputStream(file_input);

								// The cbr.readColumn reads the data in the file and
								// returns an ActiveBit collection
								ActiveBitCollection a = cbr.readColumn(data_in);
								columns.put(k, a);
							}
						}
						i += 3;
						diskTimeEnd = System.nanoTime();
					}

					cmpQueryEng = new WAHCompoundQuery(cache, columns);
					cmpQueryEng.query(cache, columns, qs);

					// Add time components - for graphs
                    diskTime += (diskTimeEnd - diskTimeStart);
                    lookupTime += cmpQueryEng.getLookupTime();
                    overheadTime += cmpQueryEng.getOverheadTime();

                    // add coverage and ranges to coverage hashmap
//					coverage = (int) Math.floor(cmpQueryEng.getCoverage());
//					numRanges = cmpQueryEng.getNumRanges();
//					addToCoverageRanges(coverageRanges, coverage, numRanges);
//					double cacheTime = (lookupTime/1000000.0);
//					//double cacheTime = (lookupTime+overheadTime)/1000000.0;
//					addToCoverageTimes(coverageTimes, coverage, cacheTime); // not including remainder time
					//addToCoverageTimes(coverageTimes, coverage, lookupTime+overheadTime); // including remainder time
					query = qbr.readLine();
				}
				long end = System.nanoTime();
				double totalTime = ((double) (end - start) / 1000000000.0);
				// UNCOMMENT TO SEE BREAKUP OF WHERE THE TIME IS SPENT
				System.out.println("Time = " + totalTime + " sec");
				//findBreakdown(diskTime, overheadTime, lookupTime, totalTime);

				// UNCOMMENT TO SEE THE COVERAGE / AVG NUM RANGES DATA
				//printRangesData(coverageRanges);
				//printTimedData(coverageTimes);

			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	/*********************************************************
	 * Private Methods
	 *********************************************************/
	private static void findBreakdown(long diskTime, long overheadTime, long lookupTime, double totalTime){
		double totalDiskTime =  (diskTime/1000000.0);
		double totalCacheTime =  (lookupTime/1000000.0);
		double totalOverheadTime = (overheadTime/1000000.0);
		double totalTimeMS = totalTime*1000.0;

		double overheadPercent = (totalOverheadTime/totalTimeMS)*100;
		double diskPercent = (totalDiskTime/totalTimeMS)*100;
		double cachePercent = (totalCacheTime/totalTimeMS)*100;
		double processingPercent = 100 - (diskPercent+cachePercent+overheadPercent);

		double processingTime = totalTimeMS - (totalDiskTime + totalCacheTime + totalOverheadTime);
		System.out.println("\t Disk Time = " + totalDiskTime+ " ms");
		System.out.println("\t Cache Time = " + totalCacheTime + " ms");
		System.out.println("\t Overhead Time = " + totalOverheadTime + " ms");
		System.out.println("\t Processing Time = " + processingTime + " ms");

		System.out.println("BREAKDOWN PERCENTAGES");
		System.out.println("\t Disk = " + diskPercent+ "%");
		System.out.println("\t Cache = " + cachePercent + "%");
		System.out.println("\t Overhead  = " + overheadPercent + "%");
		System.out.println("\t Processing = " + processingPercent + "%");

	}

	private static void addToCoverageRanges(HashMap<Integer, ArrayList<Integer>> coverageRange, int newCoverage, int newRange){
		ArrayList<Integer> list;
		if(coverageRange.containsKey(newCoverage)){ // add to list
			list = coverageRange.get(newCoverage);
			list.add(newRange);
		}else{ //make a new list and add to it
			list = new ArrayList<Integer>();
			coverageRange.put(newCoverage, list);
			list.add(newRange);
		}
	}

	private static void addToCoverageTimes(HashMap<Integer, ArrayList<Double>> coverageTimes, int newCoverage, double newTime){
		ArrayList<Double> list;
		if(coverageTimes.containsKey(newCoverage)){ // add to list
			list = coverageTimes.get(newCoverage);
			list.add(newTime);
		}else{ //make a new list and add to it
			list = new ArrayList<Double>();
			coverageTimes.put(newCoverage, list);
			list.add(newTime);
		}
	}


	private static double findAvgRange(HashMap<Integer, ArrayList<Integer>> coverage, int coverageVal){
		ArrayList<Integer> list = coverage.get(coverageVal);
		int sum = 0;
		for(int i =0; i < list.size(); i++){
			sum += list.get(i);
		}

		double average = sum / (double) list.size();
		return average;
	}

	private static double findAvgTimes(HashMap<Integer, ArrayList<Double>> coverage, int coverageVal){
		ArrayList<Double> list = coverage.get(coverageVal);
		double sum = 0;
		for(int i =0; i < list.size(); i++){
			sum += list.get(i);
		}

		double average = sum / (double) list.size();
		return average;
	}

	private static void printRangesData(HashMap<Integer, ArrayList<Integer>> coverageRanges){
		ArrayList<Integer> list;
		double averageNumRanges =0;
		for(Integer key : coverageRanges.keySet()){
			list = coverageRanges.get(key);
			averageNumRanges = findAvgRange(coverageRanges, key);
			System.out.println(key + "\t" + averageNumRanges);
		}

		//printCoverageRangeHash(coverage);
	}

	private static void printTimedData(HashMap<Integer, ArrayList<Double>> coverageTimes){
		ArrayList<Double> list;
		double averageTime =0;
		for(Integer key : coverageTimes.keySet()){
			list = coverageTimes.get(key);
			averageTime = findAvgTimes(coverageTimes, key);
			System.out.println(key + "\t" + averageTime);
		}
		//printCoverageTimeHash(coverageTimes);
	}


	private static void printCoverageRangeHash(HashMap<Integer, ArrayList<Integer>> coverage){
		ArrayList<Integer> list;
		for(Integer key : coverage.keySet()){
			list = coverage.get(key);
			System.out.println(key + ":");
			for(int i =0; i< list.size(); i++){
				//System.out.print("\t" + list.get(i));
			}
			System.out.println();
		}
	}

	private static void printCoverageTimeHash(HashMap<Integer, ArrayList<Double>> coverageTimes){
		ArrayList<Double> list;
		for(Integer key : coverageTimes.keySet()){
			list = coverageTimes.get(key);
			System.out.println(key + ":");
			for(int i =0; i< list.size(); i++){
				System.out.print("\t" + list.get(i));
			}
			System.out.println();
		}
	}
}
