package bcf.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import bcf.WLD_QueryMain;
import bcf.compression.VLCBinaryColumnWriter;
import bcf.compression.WLDCompressor;
import bcf.compression.WriteCompressedColumn;

public class WLDAlgo2 {
	private WLDAlgo2() { // static factory class, not to instantiate
	}

	/* temp table */
	private static HashMap<String,Boolean> seenCol = new HashMap<String,Boolean>();
	private static int comSeglen = 1;
	private static double total = 0.0;

	public static void runQoptimize() throws IOException
	{
		System.out.println("Running Algo 2 Optimization...");

		// clear old records in memory
		seenCol.clear();
		VLCConstants.optSeglen.clear();
		VLCConstants.colCompressed = 0;

		boolean runopt = true;
		total = VLCConstants.QHistoryFreq.get("total");

		ArrayList<Integer> remainCols = new ArrayList<Integer>();
		ArrayList<String> list;

		while (runopt) {

			if (seenCol.isEmpty()) {  // first run is to pick the top column
				int q = 0;
				double lgst = 0.0; 
				double tmp = 0.0;
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
				//System.out.println("processing query col:"+q);
				seenCol.put(q+"", true);
				list = VLCConstants.QRel.get(q+"");
				
				//*
                double WQ = 0;
                double WCR = 0;
				double history = VLCConstants.QHistoryFreq.get(q+"")/total + (1-VLCConstants.ALPHA)*VLCConstants.colHistory.get(q);
				
        		// generate new weight for optimization
        		for (int s=4; s < VLCConstants.WORD_LEN; s++) {                  
        			// loop through and calculate the associates
        			
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
        					WQ += (VLCConstants.GCD_MATRIX[s][VLCConstants.orgSeglen.get(c)] * segRatio * h2);
        					WCR += (VLCConstants.WCR_Table.get(s+":"+c) * (1 - h2));
        					seenCol.put(c, true);
        				}
        			}
        			//WQ =  VLCConstants.LAMBDA * VLCConstants.WQnorm2WCR(WQ/(VLCConstants.WORD_LEN * colCnt));
        			WQ = (VLCConstants.Coeff * VLCConstants.LAMBDA * WQ) / (VLCConstants.WORD_LEN * colCnt);
        			WCR = (1 - VLCConstants.LAMBDA) * (WCR / colCnt);

        			tmp = Math.sqrt(Math.pow(WQ,2.0) + Math.pow((1-WCR),2.0));
        			if (lgst < tmp) {
						lgst = tmp;
        				comSeglen = s;
        			}
        		}
				//System.out.println("-->> comSeglen: "+comSeglen);
				VLCConstants.optSeglen.put(q+"",comSeglen);
				for (String c: list) {
					VLCConstants.optSeglen.put(c,comSeglen);
				}

			} else {  // optimize other disjoint column-sets of queries
				// for the next tops that are not associated with the previous optimized columns

				remainCols.clear();
				for (int i=0; i < VLCConstants.numCols; i++) {
					if (!seenCol.containsKey(""+i)) {
						remainCols.add(i);
					}
				}
				runopt = false;
			}

		}

		// run the remaining columns with algo1 approximation

		for (int i: remainCols) {
			if (VLCConstants.QHistoryFreq.containsKey(i+"")) {

				int optSeglen = comSeglen;
				double maxW = 0;
				double w = 0;
				
				list = VLCConstants.QRel.get(i+"");	

				for (int s=4; s < VLCConstants.WORD_LEN; s++) {
					if (s % comSeglen == 0)  {
						
						double segRatio = (double)s/(double)VLCConstants.WORD_LEN;
						double history = 0;
						double preH = 0;
						if (VLCConstants.colHistory.get(i) != null)
							preH = VLCConstants.colHistory.get(i);
						if (VLCConstants.QHistoryFreq.get(i+"") != null)
							history = VLCConstants.QHistoryFreq.get(i+"");
						history = VLCConstants.ALPHA * history /total + (1-VLCConstants.ALPHA)*preH;

						double WCR = VLCConstants.WCR_Table.get(s+":"+i) * (1- history);

						// calculate WQ & WCR
						double WQ =  s * segRatio;
						if (VLCConstants.ColEncode.get(i+"") != null)
							WQ = VLCConstants.ColEncode.get(i+"") * segRatio * history; 
						int colCnt = 1;
						for(String c: list) {
							double h2 = 0;
							colCnt++;
							if (VLCConstants.QHistoryFreq.get(c+"") != null) 
								h2 = VLCConstants.QHistoryFreq.get(c+"")/total;
							h2 = VLCConstants.ALPHA * h2 + (1-VLCConstants.ALPHA)*VLCConstants.colHistory.get(Integer.parseInt(c));
							
							WQ += (VLCConstants.GCD_MATRIX[s][VLCConstants.orgSeglen.get(c)] * segRatio * h2);
							WCR += (VLCConstants.WCR_Table.get(s+":"+c) * (1 - h2));
						}
						//WQ = VLCConstants.LAMBDA * VLCConstants.WQnorm2WCR(WQ/(VLCConstants.WORD_LEN * colCnt));
						WQ = (VLCConstants.Coeff * VLCConstants.LAMBDA * WQ) / (VLCConstants.WORD_LEN * colCnt);
						WCR = (1 - VLCConstants.LAMBDA) * (WCR / colCnt);

						// calculate the distance
						w = Math.sqrt(Math.pow(WQ,2.0) + Math.pow((1-WCR),2.0));
						if (w > maxW) {
							maxW = w;
							optSeglen = s;
						}
					}
				}
				seenCol.put(i+"", true);
				VLCConstants.optSeglen.put(i+"",optSeglen);
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
					if (s % comSeglen == 0) {
				        if (max < VLCConstants.Wo_Table.get(s+":"+i)) {
				        	max = VLCConstants.Wo_Table.get(s+":"+i);
				        	curSeglen = s;
				        }
					}
				}
				if (curSeglen < 3) curSeglen = comSeglen;
			}

			// update active segment length table
			if (curSeglen != preSeglen)	{
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


}
