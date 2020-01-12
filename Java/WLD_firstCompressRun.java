package bcf;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import bcf.common.*;
import bcf.compression.*;
import bcf.query.*;

public class WLD_firstCompressRun {
	
	/** statistically compute user-input seglen for algo-1 */
	private static HashMap<Integer,Integer> potentialUserInput = new HashMap<Integer,Integer>();
	private static WLDCompressor comp = new WLDCompressor();
	
	public static void main(String[] args) throws Exception {
		String [] str = new String[3];
		
        /* 
        str[0] = "kddcup";
        str[1] = "/Users/wsuvgradstudent/Documents/workspace/general/kddCup/kddcup_bitmap.txt";
        str[2] = "/Users/wsuvgradstudent/Documents/workspace/general/kddCup/queries_kddcup.txt";
        //*/
        
		/* 
        str[0] = "record_linkage";
        str[1] = "/Users/wsuvgradstudent/Documents/workspace/general/record_linkage/record_linkage_bitmap.txt";
        //str[2] = "/Users/wsuvgradstudent/Documents/workspace/general/record_linkage/queries_linkage.txt";
        str[2] = "/Users/wsuvgradstudent/Documents/workspace/general/record_linkage/skew2_queries_linkage.txt";
        //*/
        
        /* europe
        str[0] = "europe_horizontal";
        str[1] = "/Users/wsuvgradstudent/Documents/workspace/general/europe_horizontal/europe_horizontal.txt";
        str[2] = "/Users/wsuvgradstudent/Documents/workspace/general/europe_horizontal/queries_europe.txt";
        //*/

        /* histobig 
         str[0] = "histobig";
         str[1] = "/Users/wsuvgradstudent/Documents/workspace/general/bitmap_histobig/bitmap_histobig.txt";       
         str[2] = "/Users/wsuvgradstudent/Documents/workspace/general/bitmap_histobig/ZipfQuery_skew2_hep.txt";       
        /*/
        
        /* uniform 
        str[0] = "uniform";
        str[1] = "/Users/wsuvgradstudent/Documents/workspace/general/bitmap_uniform/bitmap_uniform.txt";
        str[2] = "/Users/wsuvgradstudent/Documents/workspace/general/bitmap_uniform/ZipfQuery_skew2_uniform.txt";
        //*/
        
        /* skysurvey48 
        str[0] = "skysurvey48";
        str[1] = "/Users/wsuvgradstudent/Documents/workspace/general/bitmap_skysurvey48/bitmap_skysurvey1Mrows.txt";
        str[2] = "/Users/wsuvgradstudent/Documents/workspace/general/bitmap_skysurvey48/skysurveyQ_48_2M.txt";
        //*/
        
          
        //* hep 
        str[0] = "hep";
        str[1] = "/Users/wsuvgradstudent/Documents/workspace/general/bitmap_hep/bitmap_hep.txt";
        str[2] = "/Users/wsuvgradstudent/Documents/workspace/general/bitmap_hep/ZipfQuery_skew0_hep.txt";
        //*/     
        
        /* landsat 
        str[0] = "landsat";
        str[1] = "/Users/wsuvgradstudent/Documents/workspace/general/bitmap_landsat/bitmap_landsat.txt";
        str[2] = "/Users/wsuvgradstudent/Documents/workspace/general/bitmap_landsat/ZipfQuery_skew2_landsat.txt";
        //*/
        
        /* stock 
        str[0] = "stock";
        str[1] = "/Users/wsuvgradstudent/Documents/workspace/general/bitmap_stock/bitmap_stock.txt";
        str[2] = "/Users/wsuvgradstudent/Documents/workspace/general/bitmap_stock/ZipfQuery_skew2_stock.txt";
        //*/
		
		
		VLCConstants.tableName = str[0];
		VLCConstants.outFileExtension = "bitmap_"+str[0];
		String inFile = str[1];
		VLCConstants.baseDir = inFile.replaceAll("(\\w+|\\w+[-]\\w+)\\.txt","");
		VLCConstants.outDir = VLCConstants.baseDir+"bitmap_output";		

		//BitStringRep w[]  = RawBitmapReader.readFileColumnFormat(input);
		VLCConstants.RAW_TABLE = RawBitmapReader.readFileColumnFormat(inFile);
		VLCConstants.WQ_Deno = VLCConstants.WORD_LEN * VLCConstants.numCols;
		
		for(int i = 0; i<VLCConstants.numCols; i++) 
		{
			computeOptSegLen(i);
		}

		double cnt = 1;
		double wqRatio = 0.0;
		int segCnt = 0;
		potentialUserInput.clear();
		for (int j=0; j < VLCConstants.numCols; j++) {
			for (int s=4; s < VLCConstants.WORD_LEN; s++) {
				double WCR = VLCConstants.WCR_Table.get(s+":"+j);
				double segRatio = (double)s/(double)VLCConstants.WORD_LEN;
				double WQ = s * segRatio;
				for (int k=j+1; k < VLCConstants.numCols; k++) {
					WQ += (VLCConstants.GCD_MATRIX[s][VLCConstants.orgSeglen.get(k+"")] * segRatio);
					WCR += VLCConstants.WCR_Table.get(s+":"+k);
				}
				WQ = WQ/VLCConstants.WQ_Deno;				
				cnt++;
				wqRatio += ((1-WCR) / WQ);
				if (VLCConstants.orgSeglen.get(""+s) != null) {
					segCnt = 0;
					if (potentialUserInput.get(s) != null) {
						segCnt = potentialUserInput.get(s) + 1;
					} else {
						segCnt++;
					}
					potentialUserInput.put(s,segCnt);						
				}
				
			}
			//VLCConstants.colCoeff.put(j,(wqRatio/cnt)*VLCConstants.LAMBDA);
			//System.out.println("coefficent factor: "+VLCConstants.Coeff+", factor: "+wqRatio+", sample count: "+cnt);
			
			//initialize column history for moving average computation
			VLCConstants.colHistory.put(j,0.5);			
		}	
				
		VLCConstants.Coeff = (wqRatio/cnt)* VLCConstants.LAMBDA;
		//* compute potential candidate user-input segment length for algo 1
		segCnt = 0;
		
		for (Integer k : potentialUserInput.keySet()) {
		    if (segCnt < potentialUserInput.get(k)) {
		    	segCnt = potentialUserInput.get(k);
		    	VLCConstants.userInputSegLen = k;
		    }			    
		}
		
		WriteCompressedColumn wcc = new VLCBinaryColumnWriter(VLCConstants.outDir,VLCConstants.outFileExtension);
		int sizeInBits = 0;

		if (VLCConstants.algoOption == 1) {
			
			int optSeglen = VLCConstants.userInputSegLen;
			
			for (int j=0; j < VLCConstants.numCols; j++) {
				double maxW = 0;
				for (int s=4; s < VLCConstants.WORD_LEN; s++) {	
					if (s % VLCConstants.userInputSegLen == 0) {
						double segRatio = (double)s/(double)VLCConstants.WORD_LEN;
						double WCR = VLCConstants.WCR_Table.get(s+":"+j);
						double WQ = s * segRatio;

						for (int k=j+1; k < VLCConstants.numCols; k++) {
							//WQ += VLCgcdmatrix.getGCD(s,VLCConstants.orgSeglen.get(k+"")) * segRatio * VLCConstants.colCoeff.get(k);
							WQ += (VLCConstants.GCD_MATRIX[s][VLCConstants.orgSeglen.get(k+"")] * segRatio);
							WCR += VLCConstants.WCR_Table.get(s+":"+k);
						}

						//WQ = VLCConstants.LAMBDA * VLCConstants.WQnorm2WCR(WQ /VLCConstants.WQ_Deno);
						WQ = VLCConstants.Coeff * VLCConstants.LAMBDA * WQ /VLCConstants.WQ_Deno;
						WCR = (1-VLCConstants.LAMBDA) * (WCR / VLCConstants.numCols);
						double w = Math.sqrt(Math.pow(WQ,2.0) + Math.pow((1-WCR),2.0));

						if (w > maxW) {
							maxW = w;
							optSeglen = s;								
						}
					}
				}
				try {
					ActiveBitCollection a = comp.compress(optSeglen,j);
					sizeInBits += a.getSize();
					wcc.writeColumn(a);
					VLCConstants.ColEncode.put(""+j, optSeglen);
					//System.out.println("Done compressing column " + j + " with seglen = "+optSeglen);		
				}catch(Exception e){
					e.printStackTrace();
				}

			}

		} else {	//algo 2 and 3 initialization

			System.out.println("Generating seglen ratio table, file ratio table, and initial weight table...");
			/** generating W_o Table for algorithms 2 & 3 */

			for (int s=4; s < VLCConstants.WORD_LEN; s++) {					
				for (int j=0; j < VLCConstants.numCols; j++) {
					double segRatio = (double)s/(double)VLCConstants.WORD_LEN;
					double WCR = VLCConstants.WCR_Table.get(s+":"+j);
					double WQ = s * segRatio;
					for (int k=j; k < VLCConstants.numCols; k++) {
						WQ += (VLCConstants.GCD_MATRIX[s][VLCConstants.orgSeglen.get(k+"")] * segRatio);
						WCR += VLCConstants.WCR_Table.get(s+":"+k);
					}
					//WQ = VLCConstants.LAMBDA * VLCConstants.WQnorm2WCR(WQ /VLCConstants.WQ_Deno);
					WQ = VLCConstants.Coeff * VLCConstants.LAMBDA * WQ/VLCConstants.WQ_Deno;
					WCR = (1-VLCConstants.LAMBDA) * (WCR / VLCConstants.numCols);
					double myWeight = Math.sqrt(Math.pow(WQ,2.0) + Math.pow((1-WCR),2.0));
					VLCConstants.Wo_Table.put(s+":"+j,myWeight);
				}
			}	


			int comSeglen = computeCommonSegLen();
			System.out.println(" Calculated common segment length: "+comSeglen);
			System.out.println(" Start compressing...");

			try {
				for (int i=0; i< VLCConstants.numCols; i++) 				{
					ActiveBitCollection a = comp.compress(comSeglen,i);
					sizeInBits += a.getSize();
					wcc.writeColumn(a);
					VLCConstants.ColEncode.put(""+i,comSeglen);
					//System.out.println("Done compressing column " + i);
				}
			} catch(Exception e) {
				e.printStackTrace();
			}


		}
		//Okay done compressing files
		//Start running queries
		System.out.println("The index initial compressed size: "+sizeInBits+" bits, or "+Math.ceil((double)sizeInBits/8)+" bytes");
		VLCConstants.totalCurFsize = (int) Math.ceil((double)sizeInBits/8);
		//System.out.println("Start running query files ");
		WLD_QueryMain.main(str);
		/*
		while (VLCConstants.loopQuery >= 0) {
			WLD_QueryMain.main(str);
			VLCConstants.loopQuery--;
			if (VLCConstants.loopQuery >= 0) {
				System.out.println("---> Start re running query file again after optimization...");
			}
		}
		//*/

	}

	/**
	 * Using brute force to find compression ratios for columns
	 * @param col The column number currently being compressed
	 * @throws Exception 
	 */

	public static void computeOptSegLen(int col) throws Exception {
		
		double smlst = 0, bgst = 0;
		int benc = 0;

		//Compress the column using all segment lengths and record the 
		//Basically, brute force
		for (int rSeglen = 4; rSeglen < VLCConstants.WORD_LEN; rSeglen++)
		{
			
			ActiveBitCollection a = comp.compress(rSeglen,col);
			double colSizeInBytes = Math.ceil((double) a.getSize()/8.0);

			//int numberSegPerWORD = VLCConstants.WORD_LEN / (rSeglen+1);

			VLCConstants.colSizeInBytes.put(rSeglen+":"+col, colSizeInBytes);
			
			//System.out.println("file size for col:"+c+" is "+fileSize+" w/ seglen:"+rSeglen);
			//double WQ = VLCConstants.LAMBDA * (rSeglen/VLCConstants.WORD_LEN);
			double WCR = colSizeInBytes/VLCConstants.USZ_Bytes;
			VLCConstants.WCR_Table.put(rSeglen+":"+col,WCR);

			if (colSizeInBytes < smlst || smlst == 0)
			{
				smlst = colSizeInBytes;
				benc = rSeglen;
			} 

			// choose larger segment length for the same file size
			if (colSizeInBytes == smlst && benc < rSeglen)
				benc = rSeglen;

			if (colSizeInBytes > bgst)
				bgst = colSizeInBytes;
		}

		VLCConstants.Wo_Table.put("smlst:"+col,smlst); // best weight
		VLCConstants.Wo_Table.put("bgst:"+col,bgst);   // worst weight
		VLCConstants.orgSeglen.put(""+col,benc);     // best encoding length for the column

		System.out.println("Possibly use "+benc+" to encode column: "+col);


	}
	
	/**
	 *  calculate common encoding length for initial run
	 *  
	 *  @return common encoding length
	 */
	static int computeCommonSegLen() { 

		int comSeglen = 1;
		double w = 0.0;
		double maxW = 0.0;

		for (int s=4; s < VLCConstants.WORD_LEN; s++) {
			w = VLCConstants.Wo_Table.get(s+":"+0);
			for (int i=1; i < VLCConstants.numCols; i++) {
				w *= VLCConstants.Wo_Table.get(s+":"+i);
			}

			if (w > maxW) {
				maxW= w;
				comSeglen = s;
			}
		}
		return comSeglen;
	}
	

}
