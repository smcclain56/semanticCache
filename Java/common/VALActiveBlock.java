package bcf.common;


public class VALActiveBlock implements ActiveSegment{
	//JS: making these all public since they are referenced directly
	
    public long value; // the literal value of the active block
    public boolean isFill; //tells if the block is a fill or not (starts with 1)
    public int length; //length of the block
    public int position; // position of the block within a word
    public long nBlocks; //number of blocks if it is a fill
    public long fill; // fill with zeros or ones
   // int[] shift; //shift number determined by block position
    public int nbits; // number of bits in the active word
    
   
    
    
    	

    	@Override
    	public long numOfSegments() {
    		// TODO Auto-generated method stub
    		
    		return this.nBlocks;
    	}

    	@Override
    	public byte getFillValue() {
    		if (this.fill ==0)
    		return 0;
    		else return 1;
    	}

    	@Override
    	public long getLiteralRepOfFill() {
    		// TODO Auto-generated method stub
    		return this.value;
    	}

    	@Override
    	public boolean isFill() {
    		// TODO Auto-generated method stub
    		return this.isFill;
    	}

    	@Override
    	public long getLiteralValue() {
    		// TODO Auto-generated method stub
    		return 0;
    	}

    	@Override
    	public void usedNumWords(long numUsed) {
    		// TODO Auto-generated method stub
    		
    	}

    
    
}

