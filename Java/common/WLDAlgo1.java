package bcf.common;
import java.io.IOException;
import java.util.ArrayList;
import bcf.WLD_QueryMain;
import bcf.compression.*;

public class WLDAlgo1 {
	private WLDAlgo1() { // static factory class, not to instantiate
	}

	public static void runQoptimize() throws IOException
	{
		System.out.println("Running Algo1 Optimization...");

		// generate new weight for optimization
		VLCConstants.optSeglen.clear();
		VLCConstants.colCompressed = 0;
		double total = VLCConstants.QHistoryFreq.get("total");

		//System.out.println("top: "+q);
		int optSeglen = 1;
		ArrayList<String> list;
		
		for(int i = 0; i<VLCConstants.numCols; i++) {
			
			if (VLCConstants.QHistoryFreq.containsKey(i+"")) {
				double maxW = 0;
				double w = 0;			
				list = VLCConstants.QRel.get(i+"");
				
				for (int s=4; s < VLCConstants.WORD_LEN; s++) {
					if (s % VLCConstants.userInputSegLen == 0) {
						
						
						double history = 0.0;
						if (VLCConstants.QHistoryFreq.get(i+"") != null)
							history = VLCConstants.QHistoryFreq.get(i+"")/total;
						history = VLCConstants.ALPHA * history + (1-VLCConstants.ALPHA)*VLCConstants.colHistory.get(i);			
						
						double WCR = VLCConstants.WCR_Table.get(s+":"+i) * (1- history);	
						double WQ = 0;
						double segRatio = (double)s/(double)VLCConstants.WORD_LEN;
						if (VLCConstants.ColEncode.get(i+"") != null)
							//WQ = VLCConstants.colCoeff.get(i) * VLCConstants.ColEncode.get(i+"") * history * segRatio; 
							WQ = VLCConstants.ColEncode.get(i+"") * segRatio * history ;
						int colCnt = 1;
						if (!list.isEmpty()) {
							for(String c: list) {
								colCnt++;
								double h2 = VLCConstants.QHistoryFreq.get(c+"")/total;
								h2 = VLCConstants.ALPHA * h2 + (1-VLCConstants.ALPHA)*VLCConstants.colHistory.get(Integer.parseInt(c));
								WQ += (VLCConstants.GCD_MATRIX[s][VLCConstants.orgSeglen.get(c)] * segRatio * h2);
								WCR += (VLCConstants.WCR_Table.get(s+":"+c) * (1-h2));	
							}
						}
						
						WQ = (VLCConstants.Coeff * VLCConstants.LAMBDA * WQ) / (VLCConstants.WORD_LEN * colCnt);
	        			WCR = (1 - VLCConstants.LAMBDA) * (WCR / colCnt);
						
	        			w = Math.sqrt(Math.pow(WQ,2.0) + Math.pow((1-WCR),2.0));
						if (w > maxW) {
							maxW = w;
							optSeglen = s;
						}
					}
				}
				VLCConstants.optSeglen.put(i+"",optSeglen);
			}
			
		}

		//System.out.println(" Update current active bitmaps and re compress columns with optimzed segment lenght...");
		
		WLDCompressor comp = new WLDCompressor();
		VLCConstants.totalCurFsize = 0;
		int curSeglen = 0;

		//loop through each column and compress with new opt seglen
		for (int i=0; i < VLCConstants.numCols; i++) {

			int preSeglen = VLCConstants.ColEncode.get(""+i);	//get current seglen

			if (VLCConstants.optSeglen.containsKey(""+i)) {
				//if been queried, then the key exists
				curSeglen = VLCConstants.optSeglen.get(""+i);	//optimized seglen
			}
			else {
				double min = 1.0;
				for (int s=4; s < VLCConstants.WORD_LEN; s++) {
					if (s % VLCConstants.userInputSegLen == 0) {
						if (min > VLCConstants.WCR_Table.get(s+":"+i)) {
							min = VLCConstants.WCR_Table.get(s+":"+i);
							curSeglen = s;
						}
					}
				}
				if (curSeglen < 3) curSeglen = VLCConstants.userInputSegLen;
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
			
			// total bitmap size
			VLCConstants.totalCurFsize += VLCConstants.colSizeInBytes.get(curSeglen+":"+i);
		}


		if (VLCConstants.resetQueryHistoryAtK) {
			VLCConstants.QHistoryFreq.clear();
			VLCConstants.QRel.clear();
		}

	}

}