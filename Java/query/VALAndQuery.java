package bcf.query;

import bcf.common.ActiveBitCollection;
import bcf.common.VALActiveBitCollection;


public class VALAndQuery implements PointQuery {

	@Override
	public VALActiveBitCollection AndQuery(ActiveBitCollection col1A, ActiveBitCollection col2A) {
		// TODO Auto-generated method stub
		//JS: HACKY TO CONFORM TO INTERFACE
		VALActiveBitCollection col1 = (VALActiveBitCollection)col1A;
		VALActiveBitCollection col2 = (VALActiveBitCollection)col2A;
		
		//JS: apparently the first element in the arraylist holds the length
		//this should probably be changed to a field.
		//long col1_blockLength = col1.getVector().get(0);
    	//long col2_blockLength = col2.getVector().get(0);
		long col1_blockLength = col1.getSeglen();
    	long col2_blockLength = col2.getSeglen();
    	VALActiveBitCollection col_rez = null;
    	
    	if(col1_blockLength==col2_blockLength) {
    		//System.out.println("Equal block lengths...");
    		col_rez = AND_equal(col1.getActiveBitCollectionVAL(), col2.getActiveBitCollectionVAL(),col1_blockLength,col2_blockLength );
    		
    	}
    	
    	else if(col1_blockLength==7 & col2_blockLength==14) {
    		col_rez = AND_14_7(col2.getActiveBitCollectionVAL(), col1.getActiveBitCollectionVAL().getActiveBitCollectionVAL(), col2_blockLength,col1_blockLength);
    	}
    	
    	else if(col1_blockLength==14 & col2_blockLength==7) {
    		col_rez = AND_14_7(col1.getActiveBitCollectionVAL(), col2.getActiveBitCollectionVAL(), col1_blockLength,col2_blockLength);
    	}
    	else if(col1_blockLength==7 & col2_blockLength==28) {
    		col_rez = AND_28_7(col2.getActiveBitCollectionVAL(), col1.getActiveBitCollectionVAL(), col2_blockLength, col1_blockLength);
    	}
    	else if(col1_blockLength==28 & col2_blockLength==7) {
    		col_rez = AND_28_7(col1.getActiveBitCollectionVAL(), col2.getActiveBitCollectionVAL(), col1_blockLength,col2_blockLength);
    	}
    	
    	else if(col1_blockLength==14 & col2_blockLength==28) {
    		col_rez = AND_28_14(col2.getActiveBitCollectionVAL(), col1.getActiveBitCollectionVAL(), col2_blockLength,col1_blockLength);
    	}
    	
    	else if(col1_blockLength==28 & col2_blockLength==14) {
    		col_rez = AND_28_14(col1.getActiveBitCollectionVAL(), col2.getActiveBitCollectionVAL(), col1_blockLength,col2_blockLength);
    	}
    	
    	//}
    	//col_rez.printBitmap();
    	return col_rez;
    	
		
	}
	
	 public VALActiveBitCollection AND_equal(VALActiveBitCollection B1, VALActiveBitCollection B2, long B1_blockLength, long B2_blockLength) {
	     
	    	int numberOfblocks = (int) (28/B1_blockLength); // number of blocks that can be wrapped in a word
	    	//System.out.println("Number of blocks: "+numberOfblocks);
	    	long nBlocks;
	    	//int arraySize = Math.min(B1.vec.length, B2.vec.length);
	    	VALActiveBitCollection rez = new VALActiveBitCollection((int)B1_blockLength,"AndRes");  
		   
		     B1.pos=0; B2.pos=0; 
		        B1.activeBlock.nBlocks=0;
		        B2.activeBlock.nBlocks=0;
		        B1.activeBlock.length=(int)B1_blockLength;
		        B2.activeBlock.length=(int)B2_blockLength;
		        rez.activeBlock.length=B1.activeBlock.length;
		       // rez.appendWord(rez.activeBlock.length);
		        //System.out.println("rez pos = "+rez.pos);
		        rez.pos++;
		     		    
	    	B1.activeBlock.position=1;
	    	B2.activeBlock.position=1;
	    	
	    	
	    	while (B1.pos<B1.vec.size()-1 || B2.pos<B2.vec.size()-1){
	    	
	    		if (B1.activeBlock.nBlocks==0){
	    			B1.decodeBlock(numberOfblocks);
	    		}
	    		if (B1.activeBlock.position==numberOfblocks+1) {
	    			B1.pos++;  B1.activeBlock.position=1; 
	    		}// check if we need to switch to the next word
	    		
	    		if (B2.activeBlock.nBlocks==0){
	    			B2.decodeBlock(numberOfblocks);
	    		}
	    		if (B2.activeBlock.position==numberOfblocks+1) {
	    			B2.pos++;  B2.activeBlock.position=1;      }// check if we need to switch to the next word
	    		
	    		 if (B1.activeBlock.isFill) {
		                if (B2.activeBlock.isFill) {
		                	
		                    nBlocks = Math.min(B1.activeBlock.nBlocks, B2.activeBlock.nBlocks);
		                    rez.appendFillBlock(nBlocks, B1.activeBlock.fill & B2.activeBlock.fill, B1.activeBlock.length,numberOfblocks);  
		                    B1.activeBlock.nBlocks = B1.activeBlock.nBlocks-nBlocks;
		                    B2.activeBlock.nBlocks = B2.activeBlock.nBlocks-nBlocks;
		                } else { 		                    
		                	rez.activeBlock.value = B1.activeBlock.fill & B2.activeBlock.value;
		                	rez.appendLiteralBlock(numberOfblocks); 
		                	B1.activeBlock.nBlocks--;
		                	B2.activeBlock.nBlocks--;
		                } 
	    		    }else {
		  	            if (B2.activeBlock.isFill) { 
		  	            	rez.activeBlock.value = B2.activeBlock.fill & B1.activeBlock.value;
			                    rez.appendLiteralBlock(numberOfblocks);
			                    B1.activeBlock.nBlocks--;
			                	B2.activeBlock.nBlocks--;
			                    
			                } else {
			                	rez.activeBlock.value = B1.activeBlock.value & B2.activeBlock.value;
			                	rez.appendLiteralBlock(numberOfblocks);
			                    B1.activeBlock.nBlocks--;
			                	B2.activeBlock.nBlocks--;
			                }   
			            }
	            
	    	}
	        rez.finishColumn();
	        //rez.appendLastBlock(numberOfblocks);
	       	return rez;
	    }
	
	

	 
	 public VALActiveBitCollection AND_14_7(VALActiveBitCollection B1, VALActiveBitCollection B2, long B1_blockLength, long B2_blockLength) {
	    	//System.out.println("14_7");
	    //	int B1_numberOfblocks = (int) (28/B1_blockLength); // number of blocks that can be wrapped in a word
	    	
	    	//System.out.println("Number of blocks: "+numberOfblocks);
	    	long nBlocks;
	    	//int arraySize = Math.min(B1.vec.length, B2.vec.length);
	    	VALActiveBitCollection rez = new VALActiveBitCollection(14,"AndRes"); 
		     
		    		     
		     B1.pos=1; B2.pos=1; 
		        B1.activeBlock.nBlocks=0;
		        B2.activeBlock.nBlocks=0;
		        B2.superBlock.nBlocks=0;
		        B1.activeBlock.length=(int)B1_blockLength;
		        B2.activeBlock.length=(int)B2_blockLength;
		        rez.activeBlock.length=14;
		        //rez.appendWord(14);
		        //System.out.println("rez pos = "+rez.pos);
		        rez.pos++;
	
	    	B1.activeBlock.position=1;
	    	B2.activeBlock.position=1;
	    	
	    	
	    	while (B1.pos<B1.vec.size()-1 || B2.pos<B2.vec.size()-1){
	    		    		
	    		if (B1.activeBlock.nBlocks==0){
	    			B1.decodeBlock(2);
	    		}
	    		if (B1.activeBlock.position==3) {
	    			B1.pos++;  B1.activeBlock.position=1; 
	    		}// check if we need to switch to the next word
	    		
	    		if (B2.superBlock.nBlocks==0){
	    			B2.decodeBlock2(4);
	    				    		
	    		if (B2.activeBlock.position==5) {
	    			B2.pos++;  B2.activeBlock.position=1;      }// check if we need to switch to the next word
	    		
	    			if(B2.superBlock.nBlocks==-1){
	    //		System.out.println("superBlock nBlocks="+B2.superBlock.nBlocks+"  i= "+i+"  activeBlock.position is :  "+ B2.activeBlock.position+" value is: "+B2.activeBlock.value+" active isFill ="+B2.activeBlock.isFill+" actNBlocks= "+B2.activeBlock.nBlocks+" superValue= "+B2.superBlock.value+" superIsFill= "+B2.superBlock.isFill+" superFill= "+B2.superBlock.fill+"  B2.pos="+B2.pos+"  B1.pos="+B1.pos);
	    				B2.addToSuperBlock(4); 
	    				if (B2.activeBlock.position==5) {
	    	    			B2.pos++;  B2.activeBlock.position=1;      }// check if we need to switch to the next word
	    			}
	    		}
	    		
	    			
		 // System.out.println("superBlock nBlocks="+B2.superBlock.nBlocks+"  i= "+i+"  activeBlock.position is :  "+ B2.activeBlock.position+" value is: "+B2.activeBlock.value+" active isFill ="+B2.activeBlock.isFill+" actNBlocks= "+B2.activeBlock.nBlocks+" superValue= "+B2.superBlock.value+" superIsFill= "+B2.superBlock.isFill+" superFill= "+B2.superBlock.fill+"  B2.pos="+B2.pos+"  B1.pos="+B1.pos+" B1.blockPos= "+B1.activeBlock.position);
	  //  System.out.println("superBlock nBlocks="+B2.superBlock.nBlocks+"  i= "+i+"  B2.activeBlock.position is :  "+ B2.activeBlock.position+"  B2.pos="+B2.pos+"  B1.pos="+B1.pos+" B1.blockPos= "+B1.activeBlock.position);
	
	    		 if (B1.activeBlock.isFill) {
		                if (B2.superBlock.isFill) {
		                    nBlocks = Math.min(B1.activeBlock.nBlocks, B2.superBlock.nBlocks);
		                    rez.appendFillBlock(nBlocks, B1.activeBlock.fill & B2.superBlock.fill, B1.activeBlock.length,2);  
		                    B1.activeBlock.nBlocks = B1.activeBlock.nBlocks-nBlocks;
		                    B2.superBlock.nBlocks = B2.superBlock.nBlocks-nBlocks;
		                } else { 		                    
		                	rez.activeBlock.value = B1.activeBlock.fill & B2.superBlock.value;
		                	rez.appendLiteralBlock(2);  
		                	B1.activeBlock.nBlocks--;
		                	B2.superBlock.nBlocks--;
		                } 
	    		    }else {
		  	            if (B2.superBlock.isFill) { 
		  	            	rez.activeBlock.value = B2.superBlock.fill & B1.activeBlock.value;
			                    rez.appendLiteralBlock(2);
			                    B1.activeBlock.nBlocks--;
			                	B2.superBlock.nBlocks--;
			                    
			                } else {
			                	rez.activeBlock.value = B1.activeBlock.value & B2.superBlock.value;
			                	rez.appendLiteralBlock(2);
			                    B1.activeBlock.nBlocks--;
			                	B2.superBlock.nBlocks--;
			                }   
			            }
	    		 }
	    	rez.finishColumn();
	       //rez.appendLastBlock(2);
	      return rez;
	    }
	 
	 
	   public VALActiveBitCollection AND_28_7(VALActiveBitCollection B1, VALActiveBitCollection B2, long B1_blockLength, long B2_blockLength) {
	    	long nBlocks;
	    	//int arraySize = Math.max(B1.vec.length, B2.vec.length);
	    	VALActiveBitCollection rez = new VALActiveBitCollection(28,"AndRes");
		  
		    		     
		     B1.pos=1; B2.pos=1; 
		        B1.activeBlock.nBlocks=0;
		        B2.activeBlock.nBlocks=0;
		        B2.superBlock.nBlocks=0;
		        B1.activeBlock.length=(int)B1_blockLength;
		        B2.activeBlock.length=(int)B2_blockLength;
		        rez.activeBlock.length=28;
		       // rez.appendWord(28);
		        rez.pos++;
	
	    	B1.activeBlock.position=1;
	    	B2.activeBlock.position=1;
	    	
	    	
	    	while (B1.pos<B1.vec.size()-1 || B2.pos<B2.vec.size()-1){
	      		if (B1.activeBlock.nBlocks==0){
	    			B1.decodeBlock(1);
	    			B1.pos++; B1.activeBlock.position=1; 
	    		}
	    	
	    		if (B2.superBlock.nBlocks==0){
	    			B2.decodeBlock4(4);
	    				    		
	    			if (B2.activeBlock.position==5) {
	    			B2.pos++;  B2.activeBlock.position=1;      }// check if we need to switch to the next word

	    			while(B2.superBlock.nBlocks<4){
	    				B2.addToSuperBlock4(4); 
	    				
	    				if (B2.activeBlock.position==5) {
	    	    			B2.pos++;  B2.activeBlock.position=1;      }// check if we need to switch to the next word
	    			}
	    		}
	    	 if (B1.activeBlock.isFill) {
		                if (B2.superBlock.isFill) {
		                    nBlocks = Math.min(B1.activeBlock.nBlocks, B2.superBlock.nBlocks/4);
		                  //  System.out.println(nBlocks);
		                    rez.appendFillBlock(nBlocks, B1.activeBlock.fill & B2.superBlock.fill, B1.activeBlock.length,1);  
		                    B1.activeBlock.nBlocks = B1.activeBlock.nBlocks-nBlocks;
		                    B2.superBlock.nBlocks = B2.superBlock.nBlocks-(nBlocks*4);
		                } else { 		                    
		                	rez.activeBlock.value = B1.activeBlock.fill & B2.superBlock.value;
		                	rez.appendLiteralBlock(1);  
		                	B1.activeBlock.nBlocks--;
		                	B2.superBlock.nBlocks=B2.superBlock.nBlocks-4;
		                } 
	    		    }else {
		  	            if (B2.superBlock.isFill) { 
		  	            	rez.activeBlock.value = B2.superBlock.fill & B1.activeBlock.value;
			                    rez.appendLiteralBlock(1);
			                    B1.activeBlock.nBlocks--;
			                    B2.superBlock.nBlocks=B2.superBlock.nBlocks-4;
			                    
			                } else {
			                	rez.activeBlock.value = B1.activeBlock.value & B2.superBlock.value;
			                	rez.appendLiteralBlock(1);
			                    B1.activeBlock.nBlocks--;
			                    B2.superBlock.nBlocks=B2.superBlock.nBlocks-4;
			                }   
			            }
	    		
	    	}
	    	rez.finishColumn();
	       // rez.appendLastBlock(1);
	       	return rez;
	    }
	 
	   public VALActiveBitCollection AND_28_14(VALActiveBitCollection B1, VALActiveBitCollection B2, long B1_blockLength, long B2_blockLength) {
	    	
	    	long nBlocks;
	    	//int arraySize = Math.max(B1.vec.length, B2.vec.length);
	    	VALActiveBitCollection rez = new VALActiveBitCollection(28,"AndRes"); 
		     
		    		     
		     B1.pos=1; B2.pos=1; 
		        B1.activeBlock.nBlocks=0;
		        B2.activeBlock.nBlocks=0;
		        B2.superBlock.nBlocks=0;
		        B1.activeBlock.length=(int)B1_blockLength;
		        B2.activeBlock.length=(int)B2_blockLength;
		        rez.activeBlock.length=28;
		      //  rez.appendWord(28);
		        
		        rez.pos++;
	
	    	B1.activeBlock.position=1;
	    	B2.activeBlock.position=1;
	    	
	    	
	    	while (B1.pos<B1.vec.size()-1 || B2.pos<B2.vec.size()-1){
	    	
	    		if (B1.activeBlock.nBlocks==0){
	    			B1.decodeBlock(1);
	    			B1.pos++; B1.activeBlock.position=1; 
	    			
	    		}
	    	
	    		if (B2.superBlock.nBlocks==0){
	    			B2.decodeBlock2(2);
	    				    		
	    		if (B2.activeBlock.position==3) {
	    			B2.pos++;  B2.activeBlock.position=1;      }// check if we need to switch to the next word
	    		
	    			if(B2.superBlock.nBlocks==-1){
 	   
	    				B2.addToSuperBlock(2); 

	    				if (B2.activeBlock.position==3) {
	    	    			B2.pos++;  B2.activeBlock.position=1;      }// check if we need to switch to the next word
	    			}
	    		}
	    		
	    			
			 if (B1.activeBlock.isFill) {
		                if (B2.superBlock.isFill) {
		                    nBlocks = Math.min(B1.activeBlock.nBlocks, B2.superBlock.nBlocks);
		                    rez.appendFillBlock(nBlocks, B1.activeBlock.fill & B2.superBlock.fill, B1.activeBlock.length,1);  
		                    B1.activeBlock.nBlocks = B1.activeBlock.nBlocks-nBlocks;
		                    B2.superBlock.nBlocks = B2.superBlock.nBlocks-nBlocks;
		                } else { 		                    
		                	rez.activeBlock.value = B1.activeBlock.fill & B2.superBlock.value;
		                	rez.appendLiteralBlock(1);  
		                	B1.activeBlock.nBlocks--;
		                	B2.superBlock.nBlocks--;
		                } 
	    		    }else {
		  	            if (B2.superBlock.isFill) { 
		  	            	rez.activeBlock.value = B2.superBlock.fill & B1.activeBlock.value;
			                    rez.appendLiteralBlock(1);
			                    B1.activeBlock.nBlocks--;
			                	B2.superBlock.nBlocks--;
			                    
			                } else {
			                	rez.activeBlock.value = B1.activeBlock.value & B2.superBlock.value;
			                	rez.appendLiteralBlock(1);
			                    B1.activeBlock.nBlocks--;
			                	B2.superBlock.nBlocks--;
			                }   
			            }
	    	
	    	}
	       rez.finishColumn();
	     //   rez.appendLastBlock(1);
	      	return rez;
	    }


	@Override
	public ActiveBitCollection OrQuery(ActiveBitCollection col1, ActiveBitCollection col2) {
		 // TODO FILL THIS OUT??
		return null;
	}
}
