package bcf.common;

import java.util.ArrayList;
import java.util.Iterator;

import bcf.common.VALActiveBlock;
import bcf.common.VALActiveWord;

public class VALActiveBitCollection implements ActiveBitCollection{
	
	public String name = "";
    static long[] power2 = {1,2,4,8,16,32,64,128,256,512,1024,2048,4096,8192,16384,32768,
        65536,131072,262144,524288,1048576,2097152,4194304,8388608,16777216,33554432,
        67108864,134217728,268435456,536870912,1073741824,2147483648L};  
    //public Vector vec; // list of regular code words
    public ArrayList<Long> vec;
    
    public VALActiveBlock activeBlock = new VALActiveBlock(); // active block (7,14 or 28 bit)
    public VALActiveBlock lastBlock = new VALActiveBlock();
    public VALActiveBlock superBlock = new VALActiveBlock(); //this block has the length twice or 4 times greater than the activeBlock
    public VALActiveWord activeWrd=new VALActiveWord(); // the active word  (32 bit)
//    public long active;
    public int wordLen=32-4; // The length of the word... hardcoded... need to change
    public int pos=0;
    public long zero =0;
	private int seglen;
	//JS: this will be used to determine if 
	//the last activeword has been written or not;
	private boolean  needCleanUp = true; 
   // public int maxPos=0;
    static int [] countOnes = {0,1,1,2,1,2,2,3,1,2,2,3,2,3,3,4,1,2,2,3,2,3,3,4,2,3,3,4,3,4,4,5,
                               1,2,2,3,2,3,3,4,2,3,3,4,3,4,4,5,2,3,3,4,3,4,4,5,3,4,4,5,4,5,5,6,
                               1,2,2,3,2,3,3,4,2,3,3,4,3,4,4,5,2,3,3,4,3,4,4,5,3,4,4,5,4,5,5,6,
                               2,3,3,4,3,4,4,5,3,4,4,5,4,5,5,6,3,4,4,5,4,5,5,6,4,5,5,6,5,6,6,7,
                               1,2,2,3,2,3,3,4,2,3,3,4,3,4,4,5,2,3,3,4,3,4,4,5,3,4,4,5,4,5,5,6,
                               2,3,3,4,3,4,4,5,3,4,4,5,4,5,5,6,3,4,4,5,4,5,5,6,4,5,5,6,5,6,6,7,
                               2,3,3,4,3,4,4,5,3,4,4,5,4,5,5,6,3,4,4,5,4,5,5,6,4,5,5,6,5,6,6,7,
                               3,4,4,5,4,5,5,6,4,5,5,6,5,6,6,7,4,5,5,6,5,6,6,7,5,6,6,7,6,7,7,8};
   
    
    public VALActiveBitCollection() {
        this.vec = new ArrayList<Long>(); //active = new activeWord(); 
       // System.out.println("initial size= "+ vec.size());
        
    }
    
    
    public VALActiveBitCollection(String name){        
        this.name = name;
        //vec = new Vector(); //active = new activeWord(); 
        this.vec = new ArrayList<Long>();
    }
    public VALActiveBitCollection( int seglen, String name){        
        this.name = name;
        this.seglen = seglen;
        this.vec = new ArrayList<Long>();
    }
    
    public void decodeBlock(int numOfBlocks) {
   	 if (pos < vec.size()) {
   		
       	try{
       		activeBlock.value = (vec.get(pos) >>>(numOfBlocks-activeBlock.position)*activeBlock.length) & (power2[activeBlock.length]-1);
       	}
       	catch(Exception e){System.out.println(" len is : "+activeBlock.length);}
       	if ((vec.get(pos) & (1<<(32-activeBlock.position)))!=0) { // check if it is fill
       	//	System.out.println((vec.get(pos) & (1<<(32-activeBlock.position))));
       		if ((vec.get(pos) & (1<<(((numOfBlocks-activeBlock.position+1)*activeBlock.length)-1)))!=0) // if fill with ones ?????????????????????!!!!!!
       		{activeBlock.fill= power2[activeBlock.length]-1;}  // 
       		else {activeBlock.fill=0;}
       		activeBlock.nBlocks= activeBlock.value & (power2[activeBlock.length-1]-1); 
       		activeBlock.isFill=true;
        	}
       	else{
       		
       		activeBlock.nBlocks=1;
       		activeBlock.isFill=false;
       	}
       	
       	
   	 }
   	
   	 else { //If the program reaches here it means that the bitmaps do not have the same number of bits...
   		    activeBlock.nBlocks=0; activeBlock.isFill = false; activeBlock.value = 0;
   		    System.out.println ("WANT TO ACCESS A POSITION BEYOND THE SIZE OF THE BITMAP ("+name+") Pos: "+pos+" MaxPos: "+vec.size()+".");
   		}
   	 activeBlock.position++;   
       }
    
    public void appendFillBlock(long n, long fillBit, int blockLength,int numOfBlocks){
    	// n is the number of segments appended
    	//numOfBlocks is the number of blocks in a word
    	this.needCleanUp=true;
    	
    	if (lastBlock.position>0) {
    		if (fillBit == 0) {
    			if (lastBlock.isFill && (lastBlock.value & (1<<(lastBlock.length-1)))==0){ //if lastBlock is a fill with zeros
    			  if(((lastBlock.value&(power2[lastBlock.length-1]-1)) + n)>(power2[activeBlock.length-1]-1)){
    				appendLastBlock(numOfBlocks);
    				lastBlock.value=n; 
    				lastBlock.isFill=true;
    			}
    			else 
    				lastBlock.value  = lastBlock.value+n; // cbi = 3
    			}
                else if (lastBlock.value == 0){ //if lastBlock is all zeros
                	lastBlock.value = n+1;
    				lastBlock.isFill=true; }
    		 	else {			// if lastBlock is not all zeros and not a fill with zeros
    		 		appendLastBlock(numOfBlocks);
    		 		lastBlock.value =  n;
    		 		lastBlock.isFill=true;
    		          }
    		}
    	    else if (lastBlock.isFill &&(lastBlock.value & (1<<(lastBlock.length-1)))!=0) // if lastBlock is a fill with ones and fillBit!=0
    	    	if((lastBlock.value&(power2[lastBlock.length-1]-1)) + n>power2[activeBlock.length-1]-1){
    				appendLastBlock(numOfBlocks);
    				lastBlock.value=((1<<(lastBlock.length-1)))| n;    	// corrected... was n	
    				lastBlock.isFill=true;
    			}
    			else    	    	
    	    	lastBlock.value = lastBlock.value + n;
    	    else {
    	    	appendLastBlock(numOfBlocks);
    	    	lastBlock.value = (1<<(lastBlock.length-1)) + n;
    	    	lastBlock.isFill = true;
    		     }
    	}
    	else {
    	    	lastBlock.length = blockLength;
    	    	lastBlock.position =1;		
		 		lastBlock.value = (fillBit&(1<<(lastBlock.length-1)))+ n;
		 		lastBlock.isFill=true;
		 		
            }
        
    }
    
    
    
    public void appendLiteralBlock(int numOfBlocks){
    	this.needCleanUp = true;
    	if (lastBlock.position==0) {  // check if this is the very first block to be added 
    		lastBlock.value=activeBlock.value;
    		lastBlock.position=1;
    		lastBlock.length = activeBlock.length;
    		///System.out.println("entered zero ...");
    		//.out.println("activeBlock.len= "+ activeBlock.length);
    	   	 } 
    	else if (activeBlock.value == 0) { // currentBlock is all zeros
    			    if (lastBlock.value == 0) { // lastBlock is all zeros
    			    	lastBlock.value = 2;
    			    	lastBlock.isFill = true;
    			    	//System.out.println("lastBlock.len = "+ lastBlock.length);
    			    } 
    						else if (lastBlock.isFill && ((lastBlock.value & (1<<(lastBlock.length-1)))==0)) //if lastBlock is a fill with zeros
    							{
    							
    							//System.out.println((power2[lastBlock.length-1]));
    							if ((lastBlock.value&(power2[lastBlock.length-1]-1))  ==power2[lastBlock.length-1]-1){ 
    								//System.out.println("OVERSIZE Literal zeros");
    								appendLastBlock(numOfBlocks); 
    								lastBlock.value=activeBlock.value;
    								lastBlock.isFill=false;
    							}
    							else
    								lastBlock.value++;
    							} 
    	                    else {  // activeBlock is all zeros, and the lastBlock is not a fill with zeros and not all zeros
    	                    	appendLastBlock(numOfBlocks); 
    	                    	lastBlock.value=activeBlock.value;
    	                    	lastBlock.isFill=false;
    					    }    }
    	else if (activeBlock.value == power2[activeBlock.length]-1){ //activeBlock is all ones
    		
    		if ( lastBlock.isFill && ((lastBlock.value&(1<<(lastBlock.length-1)))!=0)) // if lastBlock is a fill with ones
		    {	
		    	if((lastBlock.value&(power2[lastBlock.length-1]-1))==power2[lastBlock.length-1]-1){
		    	appendLastBlock(numOfBlocks); 
				lastBlock.value=activeBlock.value;
				lastBlock.isFill=false;
				
		    }
		    	else
		    		
		    		lastBlock.value++;
		    }
    		
    			    			    
    			    else if (lastBlock.value == activeBlock.value){ //lastBlock is also all ones
     			       lastBlock.value = (1<<(lastBlock.length-1))|2; //|2
     			       lastBlock.isFill=true;
     			     }   
    			    else {    			    	// if lastBlock is not a fill with ones and not all ones
    			    	
    			    	appendLastBlock(numOfBlocks); 
                    	lastBlock.value=activeBlock.value;
                    	lastBlock.isFill=false;
    	                    } }
    	 else { //lastBlock is neither all ones/zeors or a fill
    		 appendLastBlock(numOfBlocks);
         	 lastBlock.value=activeBlock.value;
         	 lastBlock.isFill = false;
    	      }
   }
    
    
    
    

	@Override
	public void appendFill(long numRuns, byte fillBit) {
		this.needCleanUp = true;
		int blockLength=this.seglen;
		int numOfBlocks = wordLen/blockLength;

    	// n is the number of segments appended
    	//numOfBlocks is the number of blocks in a word
    	
    	if (lastBlock.position>0) {
    		if (fillBit == 0) {
    			if (lastBlock.isFill && (lastBlock.value & (1<<(lastBlock.length-1)))==0){ //if lastBlock is a fill with zeros
    			  if(((lastBlock.value&(power2[lastBlock.length-1]-1)) + numRuns)>(power2[activeBlock.length-1]-1)){
    				appendLastBlock(numOfBlocks);
    				lastBlock.value=numRuns; 
    				lastBlock.isFill=true;
    			}
    			else 
    				lastBlock.value  = lastBlock.value+numRuns; // cbi = 3
    			}
                else if (lastBlock.value == 0){ //if lastBlock is all zeros
                	lastBlock.value = numRuns+1;
    				lastBlock.isFill=true; }
    		 	else {			// if lastBlock is not all zeros and not a fill with zeros
    		 		appendLastBlock(numOfBlocks);
    		 		lastBlock.value =  numRuns;
    		 		lastBlock.isFill=true;
    		          }
    		}
    	    else if (lastBlock.isFill &&(lastBlock.value & (1<<(lastBlock.length-1)))!=0) // if lastBlock is a fill with ones and fillBit!=0
    	    	if((lastBlock.value&(power2[lastBlock.length-1]-1)) + numRuns>power2[activeBlock.length-1]-1){
    				appendLastBlock(numOfBlocks);
    				lastBlock.value=((1<<(lastBlock.length-1)))| numRuns;    	// corrected... was n	
    				lastBlock.isFill=true;
    			}
    			else    	    	
    	    	lastBlock.value = lastBlock.value + numRuns;
    	    else {
    	    	appendLastBlock(numOfBlocks);
    	    	lastBlock.value = (1<<(lastBlock.length-1)) + numRuns;
    	    	lastBlock.isFill = true;
    		     }
    	}
    	else {
    	    	lastBlock.length = blockLength;
    	    	lastBlock.position =1;		
		 		lastBlock.value = (fillBit&(1<<(lastBlock.length-1)))+ numRuns;
		 		lastBlock.isFill=true;
		 		
            }
        
    
		
	}

	
	public void appendLastBlock(int numOfBlocks) {
		// JS: What is going on with this?  I think this needs to set the first 
		//element to zero so it needs to be vec.size() = 0
		//and maybe there should be an else.  MADE CHANGE
		if (vec.size() == 0) {
			vec.add(zero);
		}

		//else {

			if (lastBlock.isFill) {
			
				vec.set((vec.size() - 1),0xFFFFFFFFL&(vec.get(vec.size() - 1) | (1 << (32 - lastBlock.position)) | (lastBlock.value << lastBlock.length
										* (numOfBlocks - lastBlock.position))));
				this.needCleanUp = false;
				
			} else {
				vec.set((vec.size() - 1),(vec.get(vec.size() - 1) | (lastBlock.value << (numOfBlocks - lastBlock.position)
										* lastBlock.length)));
				this.needCleanUp = false;
			}
			// System.out.println("appended a block  "+ vec.size());
			lastBlock.position++;
			if (lastBlock.position > numOfBlocks) {
				vec.add(zero);
				lastBlock.position = 1;
			}
		//}
	}
	
	
	/**
	 * IMPORTANT
	 * IMPORTANT
	 * IMPORTANT
	 * JS: This is a hacky helper function.  The last activeBlock
	 * of a column is not always getting written to the file.
	 * This method must be called at the end of every column read
	 * in the compressor.
	 * 
	 * */
	public void finishColumn(){
		if(this.needCleanUp){
			int numOfBlocks = wordLen / this.seglen;
			this.appendLastBlock(numOfBlocks);
		}
	}
	@Override
	public void appendLiteral(long value) {
		// JS: Make use of the parameter I think the activeBlock.value was
		// supposed to be set directly
		this.needCleanUp = true;
		this.activeBlock.value = value;
		this.activeBlock.length = this.seglen;
		int numOfBlocks = wordLen / this.seglen;
		if (lastBlock.position == 0) { // check if this is the very first block to be added
			lastBlock.value = activeBlock.value;
			lastBlock.position = 1;
			lastBlock.length = activeBlock.length;
			// .out.println("activeBlock.len= "+ activeBlock.length);
		} else if (activeBlock.value == 0) { // currentBlock is all zeros
			if (lastBlock.value == 0) { // lastBlock is all zeros
				lastBlock.value = 2;
				lastBlock.isFill = true;
				// System.out.println("lastBlock.len = "+ lastBlock.length);
			} else if (lastBlock.isFill && ((lastBlock.value & (1 << (lastBlock.length - 1))) == 0)) // if last block fill of zeros
			{

				// System.out.println((power2[lastBlock.length-1]));
				if ((lastBlock.value & (power2[lastBlock.length - 1] - 1)) == power2[lastBlock.length - 1] - 1) {
					//System.out.println("OVERSIZE Literal zeros");
					appendLastBlock(numOfBlocks);
					lastBlock.value = activeBlock.value;
					lastBlock.isFill = false;
				} else{
					lastBlock.value++;
				}
			} else { // activeBlock is all zeros, and the lastBlock is not a
						// fill with zeros and not all zeros
				
				appendLastBlock(numOfBlocks);
				lastBlock.value = activeBlock.value;
				lastBlock.isFill = false;
			}
		} else if (activeBlock.value == power2[activeBlock.length] - 1) { // activeBlock is all ones

			if (lastBlock.isFill && ((lastBlock.value & (1 << (lastBlock.length - 1))) != 0)) {// if lastblock is a fill of ones
				if ((lastBlock.value & (power2[lastBlock.length - 1] - 1)) == power2[lastBlock.length - 1] - 1) {
					appendLastBlock(numOfBlocks);
					lastBlock.value = activeBlock.value;
					lastBlock.isFill = false;

				} else {
					lastBlock.value++;
				}
			}

			else if (lastBlock.value == activeBlock.value) { // lastBlock is
																// also all ones
				lastBlock.value = (1 << (lastBlock.length - 1)) | 2; // |2
				lastBlock.isFill = true;
			} else { // if lastBlock is not a fill with ones and not all ones

				appendLastBlock(numOfBlocks);
				lastBlock.value = activeBlock.value;
				lastBlock.isFill = false;
			}
		} else { // lastBlock is neither all ones/zeors or a fill
			appendLastBlock(numOfBlocks);
			lastBlock.value = activeBlock.value;
			lastBlock.isFill = false;
		}

	}

	// method for decoding a block into a superBlock. numOfBlocks is the total
	// number of blocks in a word.
	public void decodeBlock2(int numOfBlocks) {
		if (pos < vec.size()) {
			if (activeBlock.value != 0) {
				if (activeBlock.isFill) {

					if (activeBlock.value == 1
							|| activeBlock.value == (power2[activeBlock.length - 1] | 1)) {
						superBlock.nBlocks = -1;
						superBlock.value = activeBlock.fill;
						activeBlock.position++;
						activeBlock.value = 0;
					} else {
						if (activeBlock.nBlocks % 2 == 0) {
							superBlock.nBlocks = activeBlock.nBlocks / 2;
							superBlock.isFill = true;
							superBlock.fill = (activeBlock.fill << activeBlock.length)
									| activeBlock.fill;
							activeBlock.position++;
							activeBlock.value = 0;
						} else {

							superBlock.nBlocks = (activeBlock.nBlocks - 1) / 2;
							superBlock.isFill = true;
							superBlock.fill = (activeBlock.fill << activeBlock.length)
									| activeBlock.fill;
							if ((activeBlock.value & power2[activeBlock.length - 1]) == 0) {
								activeBlock.isFill = true;
								activeBlock.value = 1;
							} else {
								activeBlock.isFill = false;
								activeBlock.value = power2[activeBlock.length] - 1;
							}
						}
					}
				} else {
					superBlock.nBlocks = -1;
					superBlock.value = activeBlock.value;
					superBlock.isFill = false;
					activeBlock.position++;
					activeBlock.value = 0;
				}
			} else {
				superBlock.value = ((vec.get(pos) >>> (numOfBlocks - activeBlock.position)
						* activeBlock.length) & (power2[activeBlock.length] - 1));
				if ((vec.get(pos) & (1 << (32 - activeBlock.position))) != 0) { // check
																				// if
																				// it
																				// is
																				// fill
					if ((vec.get(pos) & (1 << (((numOfBlocks
							- activeBlock.position + 1) * activeBlock.length) - 1))) != 0) // if
																							// fill
																							// with
																							// ones
						superBlock.fill = power2[activeBlock.length * 2] - 1; // 14
																				// or
																				// 28
																				// ones
					else
						superBlock.fill = 0;
					superBlock.isFill = true;

					if (superBlock.value % 2 == 0) { // check if nBlocks is even

						superBlock.nBlocks = ((superBlock.value) & (power2[activeBlock.length - 1] - 1)) / 2;
						activeBlock.position++;
						activeBlock.value = 0;
					} else {
						superBlock.nBlocks = ((superBlock.value - 1) & (power2[activeBlock.length - 1] - 1)) / 2; // corrected
						activeBlock.value = 1; // superBlock.fill &
												// (power2[activeBlock.length]-1);
						activeBlock.isFill = true;
						activeBlock.fill = superBlock.fill
								& (power2[activeBlock.length] - 1);
					}

				} else { // not fill
					superBlock.nBlocks = -1;
					superBlock.isFill = false;
					activeBlock.position++;
					activeBlock.value = 0;
				}

			}
		}

		else { // If the program reaches here it means that the bitmaps do not
				// have the same number of bits...
			superBlock.nBlocks = 0;
			superBlock.isFill = false;
			superBlock.value = 0;
			activeBlock.position++;
			activeBlock.value = 0;
			// System.out.println
			// ("WANT TO ACCESS A POSITION BEYOND THE SIZE OF THE BITMAP ("+name+") Pos: "+pos+" MaxPos: "+maxPos+".");
		}

	}

	public void addToSuperBlock(int numOfBlocks) {
		if (pos < vec.size()) {

			activeBlock.value = (vec.get(pos) >>> (numOfBlocks - activeBlock.position)
					* activeBlock.length)
					& (power2[activeBlock.length] - 1);

			if ((vec.get(pos) & (1 << (32 - activeBlock.position))) != 0) { // check
																			// if
																			// it
																			// is
																			// fill
				// System.out.println(pos+"'s word, position "+activeBlock.position+" is a fill");
				if ((vec.get(pos) & (1 << (((numOfBlocks - activeBlock.position + 1) * activeBlock.length) - 1))) != 0) { // if
																															// fill
																															// with
																															// ones

					superBlock.value = (superBlock.value << activeBlock.length)
							| (power2[activeBlock.length] - 1);
					superBlock.isFill = false;
					superBlock.nBlocks = 1;
					activeBlock.value--;
					// System.out.println(activeBlock.value);
					activeBlock.isFill = true;
					activeBlock.nBlocks = activeBlock.value
							& (power2[activeBlock.length - 1] - 1);
					activeBlock.fill = power2[activeBlock.length] - 1;

				} else { // if fill with zeros

					superBlock.value = superBlock.value << activeBlock.length;
					superBlock.isFill = false;
					superBlock.nBlocks = 1;
					activeBlock.value--;
					activeBlock.isFill = true;
					activeBlock.nBlocks = activeBlock.value
							& (power2[activeBlock.length - 1] - 1);
					activeBlock.fill = 0;
				}
			} else { // current block is not a fill

				superBlock.value = (superBlock.value << activeBlock.length)
						| activeBlock.value;
				superBlock.isFill = false;
				superBlock.nBlocks = 1;
				activeBlock.position++;
				activeBlock.value = 0;
			}
		} else { // If the program reaches here it means that the bitmaps do not
					// have the same number of bits...
			superBlock.nBlocks = 0;
			superBlock.isFill = false;
			superBlock.value = 0;
			activeBlock.position++;
			activeBlock.value = 0;
			// System.out.println
			// ("WANT TO ACCESS A POSITION BEYOND THE SIZE OF THE BITMAP ("+name+") Pos: "+pos+" MaxPos: "+maxPos+".");
		}
	}

	public void addToSuperBlock4(int numOfBlocks) { // adds blocks to a
													// superblock that is 4
													// times larger. The
													// superBlock contains some
													// segments here, but less
													// than 4.
		if (pos < vec.size()) {

			activeBlock.value = (vec.get(pos) >>> (numOfBlocks - activeBlock.position)
					* activeBlock.length)
					& (power2[activeBlock.length] - 1);

			if ((vec.get(pos) & (1 << (32 - activeBlock.position))) != 0) { // check if it is fill
				// System.out.println(pos+"'s word, position "+activeBlock.position+" is a fill");
				if (((activeBlock.value) & (power2[activeBlock.length - 1] - 1)) > (4 - superBlock.nBlocks)) {
					if ((vec.get(pos) & (1 << (((numOfBlocks- activeBlock.position + 1) * activeBlock.length) - 1))) != 0) { // if fill with ones

						superBlock.value = (superBlock.value << (activeBlock.length * (4 - superBlock.nBlocks)))
								| (power2[(int) (activeBlock.length * (4 - superBlock.nBlocks))] - 1);
						superBlock.isFill = false;
						activeBlock.value = activeBlock.value
								+ superBlock.nBlocks - 4;
						activeBlock.isFill = true;
						activeBlock.nBlocks = activeBlock.value
								& (power2[activeBlock.length - 1] - 1);
						activeBlock.fill = power2[activeBlock.length] - 1;
						superBlock.nBlocks = 4;

					} else { // if fill with zeros

						superBlock.value = superBlock.value << (activeBlock.length * (4 - superBlock.nBlocks));
						superBlock.isFill = false;
						activeBlock.value = activeBlock.value
								+ superBlock.nBlocks - 4;
						activeBlock.isFill = true;
						activeBlock.nBlocks = activeBlock.value
								& (power2[activeBlock.length - 1] - 1);
						activeBlock.fill = 0;
						superBlock.nBlocks = 4;

					}
				} else { // not enough to fill the superBlock

					if ((vec.get(pos) & (1 << (((numOfBlocks - activeBlock.position + 1) * activeBlock.length) - 1))) != 0) { // if fill withones
						superBlock.value = (superBlock.value << (activeBlock.length * ((activeBlock.value) & (power2[activeBlock.length - 1] - 1))))
								| (power2[(int) ((activeBlock.value) & (power2[activeBlock.length - 1] - 1))
										* activeBlock.length] - 1);
					} else { // fill with zeros
						superBlock.value = (superBlock.value << (activeBlock.length * ((activeBlock.value) & (power2[activeBlock.length - 1] - 1))));
					}
					superBlock.isFill = false;
					superBlock.nBlocks = superBlock.nBlocks
							+ ((activeBlock.value) & (power2[activeBlock.length - 1] - 1));
					activeBlock.position++;
					activeBlock.value = 0;
				}

			} else { // current block is not a fill

				// System.out.println("superBlock.value= "+superBlock.value+"  activeValue = "+activeBlock.value);
				superBlock.value = (superBlock.value << activeBlock.length)| activeBlock.value;
				superBlock.isFill = false;
				superBlock.nBlocks++;
				activeBlock.position++;
				activeBlock.value = 0;
			}
		} else { // If the program reaches here it means that the bitmaps do not
					// have the same number of bits...
			superBlock.nBlocks = 4;
			superBlock.isFill = false;
			superBlock.value = 0;
			activeBlock.position++;
			activeBlock.value = 0;
			// System.out.println
			// ("WANT TO ACCESS A POSITION BEYOND THE SIZE OF THE BITMAP ("+name+") Pos: "+pos+" MaxPos: "+maxPos+".");
		}
	}
    
    public void decodeBlock4(int numOfBlocks) {
     	 if (pos < vec.size()) {
     		if(activeBlock.value!=0){
     			if(activeBlock.isFill){
     				if((activeBlock.value & (power2[activeBlock.length-1]-1)) <4)  //if less than 4 blocks fill
     				{
     					
     					superBlock.nBlocks=(activeBlock.value & (power2[activeBlock.length-1]-1));
     					      					
     					if((activeBlock.value&power2[activeBlock.length-1])==0){ 
     						superBlock.value=0;}
     					else{
     						
     						superBlock.value=power2[(int) (superBlock.nBlocks*activeBlock.length)]-1;} // corrected
     					activeBlock.position++;
     					activeBlock.value=0;
     				}
     				else { //nblocks >=4
     					if (activeBlock.nBlocks%4==0){   //number of blocks is divisible by 4
     					superBlock.nBlocks=activeBlock.nBlocks;
     					superBlock.fill=(activeBlock.fill<<activeBlock.length)|activeBlock.fill;
     					superBlock.isFill=true;
     	   				superBlock.fill = (superBlock.fill<<(2*activeBlock.length))|superBlock.fill;
     	   				activeBlock.position++;
     	   				activeBlock.value=0;
     	   				
     				}
     				else{ //not divisible by 4
     					superBlock.fill=(activeBlock.fill<<activeBlock.length)|activeBlock.fill;
     					superBlock.nBlocks=(activeBlock.nBlocks-(activeBlock.nBlocks%4));
     					superBlock.isFill=true;
     					superBlock.fill = (superBlock.fill<<(2*activeBlock.length))|superBlock.fill;
     					if((activeBlock.value&power2[activeBlock.length-1])==0){ //if fill with zeros
     						activeBlock.isFill=true;
     						activeBlock.value=activeBlock.nBlocks%4;
     						activeBlock.nBlocks=activeBlock.nBlocks%4;
     					}
     					else if (activeBlock.nBlocks%4==1)
     					{
     					activeBlock.isFill=false;
     					activeBlock.value=power2[activeBlock.length]-1;
     					}
     					else{
     						activeBlock.isFill=true;
     						activeBlock.value=power2[activeBlock.length-1]|(activeBlock.nBlocks%4);
     						activeBlock.nBlocks=activeBlock.nBlocks%4;
     					}
     				}
     			}
     			}
     			else{
     			//	System.out.println("should enter here on next iter...");
     				superBlock.nBlocks=1;
     			superBlock.value=activeBlock.value;
     			superBlock.isFill=false;
     			activeBlock.position++;
     			activeBlock.value=0;}
     		}
     		else{  //if activeBlock is zero then it decodes next block and adds it to the superBlock
         	  	superBlock.value = ((vec.get(pos) >>>(numOfBlocks-activeBlock.position)*activeBlock.length) & (power2[activeBlock.length]-1));
         	if ((vec.get(pos) & (1<<(32-activeBlock.position)))!=0  ) { // check if it is fill
         		if (((superBlock.value)&(power2[activeBlock.length-1]-1))>3){  //check if more than 3 blocks
         		if (((vec.get(pos) & (1<<(((numOfBlocks-activeBlock.position+1)*activeBlock.length)-1)))!=0)) // if fill with ones !!!!!!
         		superBlock.fill= power2[activeBlock.length*4]-1;  //  28 ones
         		else superBlock.fill=0; 
         		superBlock.isFill=true;
        
         		 if ((superBlock.value& (power2[activeBlock.length-1]-1))%4==0){  // check if nBlocks is divisible by 4
         			
         		superBlock.nBlocks= ((superBlock.value) & (power2[activeBlock.length-1]-1)); 
         		activeBlock.position++;
         		activeBlock.value=0;
         		
         		}
         		else {
         		//	superBlock.isFill=true;
         			superBlock.nBlocks= ((superBlock.value-(superBlock.value&(power2[activeBlock.length-1]-1)%4)) & (power2[activeBlock.length-1]-1));  //corrected
         			activeBlock.value=(superBlock.value&(power2[activeBlock.length-1]-1)%4)|(power2[activeBlock.length-1]&superBlock.fill); //superBlock.fill & (power2[activeBlock.length]-1);
         			activeBlock.isFill= true;
         			activeBlock.fill=superBlock.fill & (power2[activeBlock.length]-1);
         		}
         	}
         		else {  //less than 3 fills
         			superBlock.nBlocks= superBlock.value&(power2[activeBlock.length-1]-1);
         			superBlock.isFill=false;
             		
             		if ((vec.get(pos) & (1<<(((numOfBlocks-activeBlock.position+1)*activeBlock.length)-1)))!=0) { //if fill with ones
             			
            			superBlock.value = power2[(int) (activeBlock.length*superBlock.nBlocks)]-1;}
            		
            		else {//fill with zeros 
            			superBlock.value = 0;}
             		
             		activeBlock.position++;
             		activeBlock.value=0;
         		}
         		
          	}
         	else{  //not fill
         		superBlock.nBlocks=1;
         		superBlock.isFill=false;
         		activeBlock.position++;
         		activeBlock.value=0;
         	}
         	
     	 }
     	 }
     	
     	 else { //If the program reaches here it means that the bitmaps do not have the same number of bits...
     		    superBlock.nBlocks=0; superBlock.isFill = false; superBlock.value = 0;
     		    activeBlock.position++;
     		    activeBlock.value=0;
     		   // System.out.println ("WANT TO ACCESS A POSITION BEYOND THE SIZE OF THE BITMAP ("+name+") Pos: "+pos+" MaxPos: "+maxPos+".");
     		}
     		     
         }
   
	

	@Override
	public void appendWord(long word) {
		
		this.vec.add (new Long(word));
		
	}

	@Override
	public void setSeglen(int seglen) {
		
		this.seglen=seglen;
		
	}

	@Override
	public int getSeglen() {
		
		return this.seglen;
	}

	@Override
	public int getNumberOfWords() {

		//return maxPos;        	
	    long l=new Long(0);
		int nWords=0;
		int size = 0;
		
		for (int i=0; i<vec.size()-1; i++ ) {            
		    l = vec.get(i);
		    if (l> 0x7FFFFFFFL) {
		    	nWords = (int)(l & 0x3FFFFFFFL);
		    } 
		    else {
		    	nWords = 1;
		    }
		    size+=nWords;
		}
		//aWords[0]--;
		//aWords[1]=l.intValue();
		return size;   
	    
	}

	@Override
	public boolean isEmpty() {
		
		return this.vec.isEmpty();
	}
	
	public VALActiveBitCollection getActiveBitCollectionVAL(){
		
		return this;
		
	}
	
public ArrayList<Long> getVector(){
		
		return this.vec;
		
	}

	@Override
	public Iterator<Long> getSegmentIterator() {
		return this.vec.iterator();
	}

	@Override
	public String getColName() {
		return this.name;
	}


	@Override
	public int getSize() {
		return this.vec.size()*32;
	}

	@Override
	public String toString(){
		String s = "";
		for(long l : this.vec){
			long flags = l&0xF0000000L;
			flags = flags>>>28;
            String f = Long.toBinaryString(flags);
            while(f.length()<4){
            	f="0"+f;
            }
            long rest = l&0xFFFFFFFL;
            String t = Long.toBinaryString(rest);
			while(t.length()<28){
				t="0"+t;
			}
			
			if(seglen == 7){
				t=t.substring(0, 7)+" "+t.substring(7, 14)+" "+t.substring(14, 21)+" "+t.substring(21, 28);
			}
			if(seglen==14){
				t=t.substring(0, 14)+" "+t.substring(14, 28);
			}
			
			s+=f+"|"+t+",  ";
		}
		return s;
	}
}
