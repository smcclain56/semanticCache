package bcf.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import bcf.WLD_QueryMain;
import bcf.compression.VLCBinaryColumnWriter;
import bcf.compression.WLDCompressor;
import bcf.compression.WriteCompressedColumn;

public class WLDAlgo3 {
	private WLDAlgo3() { // static factory class, not to instantiate
	}

	/** temp table */
	private static HashMap<String,Boolean> seenCol = new HashMap<String,Boolean>();
	private static int topSeglen = 1;
	private static boolean setTop = false;
	private static double total = 0.0;

	public static void runQoptimize() throws IOException
	{
		System.out.println("Running Algo 3 Optimization...");

		// clear old records in memory
		WLDAlgo3.seenCol.clear();
		VLCConstants.optSeglen.clear();
		VLCConstants.colCompressed = 0;

		double lgst = 0.0; 
		double tmp = 0.0;
		boolean runopt = true;
		total = VLCConstants.QHistoryFreq.get("total");

		ArrayList<String> curList = new ArrayList<String>();
		
		
		while (runopt) {

			if (seenCol.isEmpty()) {  // first run is to pick the top column
				int q = 0;
				// getting the top column frequency
				
				for (int i=0; i < VLCConstants.numCols; i++) {
					if (VLCConstants.QHistoryFreq.containsKey(""+i)) {
						tmp = VLCConstants.QHistoryFreq.get(""+i);
						if (lgst < tmp) {
							lgst = tmp;
							q = i;
						}
					}
				}
				lgst = 0.0;
				tmp = 0.0;

				// optimize for this top column with its associated columns.
				doOpt(""+q);

			} else {  // optimize other disjoint column-sets of queries
				// for the next tops that are not associated with the previous optimized columns

				int cnt = 0;
				curList.clear();
				for (int i=0; i < VLCConstants.numCols; i++) {
					if (!seenCol.containsKey(""+i)) {
						cnt++;
						curList.add(""+i);
					}
				}

				if (cnt == 0) {
					runopt = false;
				} else {
					lgst = 0;
					String q = null;

					// find the next high frequency query
					for (String s: curList) {
						if (VLCConstants.QHistoryFreq.containsKey(s)) {
							tmp = VLCConstants.QHistoryFreq.get(s);
							if (lgst < tmp) {
								lgst = tmp;
								q = s;
							}
						}						
					}
					lgst = 0.0;
					tmp = 0.0;
					if (q != null) {
						doOpt(q);													
					} else {
						runopt = false;
					}
				}
			}

		}

		//System.out.println(" Update current active bitmaps and re compress columns with optimzed segment lenght...");

		WLDCompressor comp = new WLDCompressor();
		VLCConstants.totalCurFsize = 0;
		int curSeglen = 0;

		
		//loop through each col
		for (int i=0; i < VLCConstants.numCols; i++) {

			int preSeglen = VLCConstants.ColEncode.get(""+i);	//get current seglen

			if (seenCol.containsKey(""+i)) {
				//if been queried, then the key exists
				curSeglen = VLCConstants.optSeglen.get(""+i);	//optimized seglen
			}
			else {
				double max = 0.0;
				for (int s=4; s < VLCConstants.WORD_LEN; s++) {
					if (s % topSeglen == 0) {
						if (max < VLCConstants.Wo_Table.get(s+":"+i)) {
							max = VLCConstants.Wo_Table.get(s+":"+i);
							curSeglen = s;
						}
					}
				}
				if (curSeglen < 3) curSeglen = topSeglen;
			}

			// update active segment length table
			if (curSeglen != preSeglen)
			{
				ActiveBitCollection a = comp.compress(curSeglen,i);
				WLD_QueryMain.alreadyLoaded.put(i+"", a); //overwrite old key
				VLCConstants.ColEncode.put(""+i,curSeglen);
				VLCConstants.colCompressed += 1;
			} 
			
			// update history weight for next optimization period
			double h = 0;
			if (VLCConstants.QHistoryFreq.get(i+"") != null)
				h = VLCConstants.QHistoryFreq.get(i+"")/total;
			h = VLCConstants.ALPHA * h + (1-VLCConstants.ALPHA)*VLCConstants.colHistory.get(i);		
			VLCConstants.colHistory.put(i,h);
			
			VLCConstants.totalCurFsize += VLCConstants.colSizeInBytes.get(curSeglen+":"+i);

		}

		//System.out.println("...done");

		if (VLCConstants.resetQueryHistoryAtK) {
			VLCConstants.QHistoryFreq.clear();
			VLCConstants.QRel.clear();
		}

	}

	private static void doOpt(String q) {
		
		
		//System.out.println("processing query col:"+q);
		seenCol.put(q, true);

		// getting the query's list of partners
		ArrayList<String> list = VLCConstants.QRel.get(q);

		//System.out.println("top: "+q);
		int comSeglen = 1;
		double maxW = 0;
		double w = 0;



		if (!setTop) {
            double WQ = 0;
            double WCR = 0;
    		// generate new weight for optimization
    		for (int s=4; s < VLCConstants.WORD_LEN; s++) {                  
    			// loop through and calculate the associates
				double history = 0;
				double preH = 0;
				if (VLCConstants.colHistory.get(q) != null)
					preH = VLCConstants.colHistory.get(q);
				if (VLCConstants.QHistoryFreq.get(q) != null)
					history = VLCConstants.QHistoryFreq.get(q);
				history = VLCConstants.ALPHA * history /total + (1-VLCConstants.ALPHA)*preH;
				
    			double segRatio = (double)s/(double)VLCConstants.WORD_LEN;
   			    WQ = VLCConstants.ColEncode.get(q+"") * segRatio * history;
   			    WCR = VLCConstants.WCR_Table.get(s+":"+q) * (1 - history);
   			    int colCnt = 1;
    			for (String c: list) {
    				colCnt++;
    				// do it for the associate queries that not been done
    				// by higher frequency query (not been seen).
    				if (!seenCol.containsKey(c)) {
    					double h2 = VLCConstants.QHistoryFreq.get(c)/total; 
    					h2 = VLCConstants.ALPHA * h2 + (1-VLCConstants.ALPHA)*VLCConstants.colHistory.get(Integer.parseInt(c));
    					WQ +=  (VLCConstants.GCD_MATRIX[s][VLCConstants.orgSeglen.get(c)] * segRatio * h2);
    					WCR +=  (VLCConstants.WCR_Table.get(s+":"+c) * (1 - h2));
    					seenCol.put(c, true);
    				}
    			}
    			//WQ =  VLCConstants.LAMBDA * VLCConstants.WQnorm2WCR(WQ/(VLCConstants.WORD_LEN * colCnt));
    			WQ = (VLCConstants.Coeff * VLCConstants.LAMBDA * WQ) / (VLCConstants.WORD_LEN * colCnt);
    			WCR = (1 - VLCConstants.LAMBDA) * (WCR / colCnt);
    			w = Math.sqrt(Math.pow(WQ,2.0) + Math.pow((1-WCR),2.0));
    			//System.out.println("column: "+q+", seglen: "+s+", WCR: "+WCR+", WQ: "+WQ+", weight: "+tmp+", maxW: "+lgst);

    			if (w > maxW) {
					maxW = w;
					comSeglen = s;
    			}
    		}

		} else {
			for (int s=4; s < VLCConstants.WORD_LEN; s++) {
				double segRatio = (double)s/(double)VLCConstants.WORD_LEN;
				double history = 0;
				double preH = 0;
				if (VLCConstants.colHistory.get(q) != null)
					preH = VLCConstants.colHistory.get(q);
				if (VLCConstants.QHistoryFreq.get(q) != null)
					history = VLCConstants.QHistoryFreq.get(q);
				history = VLCConstants.ALPHA * history /total + (1-VLCConstants.ALPHA)*preH;
				
				// calculate WQ & WCR
				double WCR = VLCConstants.WCR_Table.get(s+":"+q) * (1- history);
				double WQ = s * segRatio;
				if (VLCConstants.ColEncode.get(q) != null)
					WQ = VLCConstants.ColEncode.get(q) * history * segRatio; 
				if (s % topSeglen == 0) {
				    int colCnt = 1;
					for(String c: list) {
                        colCnt++;
						double h2 = 0;
						if (VLCConstants.QHistoryFreq.get(c+"") != null) 
							h2 = VLCConstants.QHistoryFreq.get(c+"")/total;
						h2 = VLCConstants.ALPHA * h2 + (1-VLCConstants.ALPHA)*VLCConstants.colHistory.get(Integer.parseInt(c));
						
						WQ += (VLCConstants.GCD_MATRIX[s][VLCConstants.orgSeglen.get(c)] * segRatio * h2);
						WCR += (VLCConstants.WCR_Table.get(s+":"+c) * (1 - h2));
					}
        			//WQ =  VLCConstants.LAMBDA * VLCConstants.WQnorm2WCR(WQ/(VLCConstants.WORD_LEN * colCnt));
					WQ = (VLCConstants.Coeff * VLCConstants.LAMBDA * WQ) / (VLCConstants.WORD_LEN * colCnt);
        			WCR = (1 - VLCConstants.LAMBDA) * (WCR / colCnt);
					w = Math.sqrt(Math.pow(WQ,2.0) + Math.pow((1-WCR),2.0));

					if (w > maxW) {
						maxW = w;
						comSeglen = s;
					}
				}
			}
		}

		if (!setTop) {
			topSeglen = comSeglen;
			setTop = true;
		}
		VLCConstants.optSeglen.put(q,comSeglen);
		for (String c: list) {
			VLCConstants.optSeglen.put(c,comSeglen);
		}

		//System.out.println("comSeglen: "+comSeglen);
	}

}
