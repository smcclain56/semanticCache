package bcf.common;

import java.util.ArrayList;
import java.util.HashMap;
import bcf.compression.*;


/**
 * VLC Public Utilities Class for all constants
 * Non-instantiable
 *
 * @author fred.
 *         Created Jan 28, 2012.
 */

public class VLCConstants {
	
	private VLCConstants() { } /* not to instantiate */
	
	/**
	 * table name, folder & out file name for running queries after processing index file
	 */
	public static String tableName = null; 
	public static String baseDir = null;
	public static String outDir = null;
	public static String outFileExtension = null;

	/**
	 * System file separator character. ("\" for windows, etc.).
	 */
	public static final String fsp = System.getProperty("file.separator"); 
	
	/**
	 * System new line character.
	 */
	public static final String newLine = System.getProperty("line.separator"); 
	
	/**
	 * Bitmap index file extension.
	 */
	public final static String fext = ".dat"; 
	
	/**
	 * Minimum encoding size that needs to be represented with an Int.
	 */
	public final static int repwint = 15; 
	
	/**
	 * Minimum encoding size that needs to be represented with a Short.
	 */
	public final static int repwshort = 7; 

	/**
	 * This is the common base that should be used to determine the segmentation length for 
	 * column compression.  This value will only be used if use_abs is set to true.  A value of 
	 * 1 means that all length from 4 to 32 will be tested.  A value of 4 means that 4,8,12.. will be 
	 * tested and so on.
	 */
	public final static int COL_BASE = 7;
	
	/** Number of rows in bitmap */
	public static int numRows = 0;
	
	/** Number of columns in bitmap */
	public static int numCols = 0;
	
	/** Array consisting of all rows, in RowVec form*/
	public static BitStringRep RAW_TABLE[];

	/** bitmap uncompressed size*/
	public static long USZ_Bytes;
	
	/** Option to run compression algorithm 1, 2, or 3 */
	public static int algoOption = 3;
	
	/** Word-size x numCols */
	public static int WQ_Deno;

	/** Lambda */
	public static double LAMBDA = 0.05;
	
	/** Alpha for column query prediction */
	public static double ALPHA = 0.7;
	
	/** Constant: running optimization after this number of queries */
	public static int OPT_SIZE = 100;
	
	/** loop query file again */
	public static int loopQuery = 1;
	
	/** Reset Queried History at K sample interval or not */
	static boolean resetQueryHistoryAtK = false;

	/** Weight ratio (1-WCR / WQ) */
	public static double Coeff = 0;

	/** Constant: number of bits in a word on the system being compressed on (note: not sure that its not hard coded in places)*/
	public static final int WORD_LEN = 32;
    
	/**
	 * Hex constant used to check for filled or literal word
	 * of 32 or 64.
	 */
	public static long IF_FILL = 0x7FFFFFFFL;             // 32 bit constant
	//public static long IF_FILL = 0x7FFFFFFFFFFFFFFFL;   // 64 bit constant
	
	/**
	 * Hex constant used to 'AND' to get number of run
	 * of 32 or 64.
	 */
	public static long RUN_COUNT = 0x3FFFFFFFL;           // 32 bit constant
	//public static long RUN_COUNT = 0x3FFFFFFFFFFFFFFFL; // 64 bit constant
	
	/** Constant: Optimization Time */
	public static double OPT_TIME = 0;
    
	/** Constant: Sample Query Time */
	public static double S_TIME = 0;
    
	/** Constant: total Query Time */
	public static double TTQ_TIME = 0;
    
	/** Constant: total bitmap index size */
	public static double bitmapSize = 0;

	/** Number of columns compressed */
	public static int colCompressed = 0;

	/** current total file size, use for comparison after each optimization */
	public static int totalCurFsize = 0;

	/** pre condition file size weight table Wo for all possible segment length for each column  */
	public static HashMap<String,Double> Wo_Table = new HashMap<String,Double>();

	/** pre compute file ratio table for all possible segment length for each column */
	public static HashMap<String,Double> WCR_Table = new HashMap<String,Double>();

	/** Column Current Encoding Length */
	public static HashMap<String,Integer> ColEncode = new HashMap<String,Integer>();

	/** Query frequency History table - access and update by WLD_QueryMain.java */
	public static HashMap<String,Double> QHistoryFreq = new HashMap<String,Double>();

	/** recomputed segment length table after runQoptimize */
	public static HashMap<String,Integer> optSeglen = new HashMap<String,Integer>();

	/** Query relationship table - access and update by WLD_QueryMain.java*/
	public static HashMap<String,ArrayList<String>> QRel = new HashMap<String,ArrayList<String>>();

	/** original optimized segment length table for file size */
	public static HashMap<String,Integer> orgSeglen = new HashMap<String,Integer>();
	
	/** original optimized segment length table for file size */
	public static HashMap<String,Double> colSizeInBytes = new HashMap<String,Double>();
	
	/** History records of each column  */
	public static HashMap<Integer,Double> colHistory = new HashMap<Integer,Double>();
	
	/** used for loop runs of lambda & K */
	public static HashMap<String,String> expResults = new HashMap<String,String>();
	
	/** user input seglen for Algo-1 */
	public static Integer userInputSegLen;
	
	
	public static int GCD_MATRIX[][]= new int[WORD_LEN][WORD_LEN];
	static{
		
        for (int i = 1; i < VLCConstants.WORD_LEN; i++) {
            for (int j = 1; j < VLCConstants.WORD_LEN; j++) {
            	int r = i;
                if (i != j) {
                    r = findGCD(i,j);
                } 
               GCD_MATRIX[i][j]=r;
               GCD_MATRIX[j][i]=r;
              
            }	
        }	
		
	}
	private static int findGCD(final int x, final int y) {
		int a, b;
		if (x > y) {
			a = y;
			b = x;
		} else {
			a = x;
			b = y;
		}

		while (b != 0)
		{
			int temp = a % b;
			a = b;
			b = temp;
		}

		return a;

	}

	/**An array holding the powers of 2 for use in hex calculations.*/
	public static long[] powers = new long[VLCConstants.WORD_LEN];

	static{
		for(int i =0;i<VLCConstants.WORD_LEN;i++){
			powers[i] = (long) Math.pow(2,i);
		}
	}
	
}
