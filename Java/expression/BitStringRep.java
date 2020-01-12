package bcf.compression;
import java.util.StringTokenizer;

/**
 * Class used to represent a row OR a column in a bitmap.
 * 
 * @author Fabian
 *
 */

public class BitStringRep
{
	/** The bit string, represented by booleans. */
    private boolean[] bitString;
    /**The row id.*/
    private int id = 0;
    /**The number of columns in the row. */
    private int numBits;

    /**
     * Creates an instance of RowVec with 
     * a set number of columns. Will also initialize
     * the bitstring array to hold column amount of bits.
     * 
     * @param numBits The number of columns in the row.
     */
    public BitStringRep(int numBits) 
    {
        this.numBits = numBits;
        this.bitString = new boolean [numBits];
    }

    /**
     * Set this bit string  ID.
     * 
     * @param ident The number to set the ID to.
     */
    public void setId(int ident)
    {
        this.id = ident;
    }
    
    
    /**
     * Sets this RowVecs bitstring to the string passsed 
     * in. This method will remove the row id, which means it
     * can take information straight from the infile.
     * 
     * @param bitstring The bitstring to bet set.
     */
    public void setBitString(String bitstring)
    {
        StringTokenizer stk = new StringTokenizer(bitstring, ",");
        stk.nextToken();	//Ignore the row ID
        String temp = stk.nextToken();	//The bitstring
        
        if(temp.length() != this.getNumBits())
        {
            System.out.println("THERE IS AN ERROR for the number of columns");
        }
        
        for(int i=0; i< this.getNumBits(); i++)
        {
            
            this.bitString[i] = (Integer.parseInt(temp.substring(i,i+1)) == 1);
        }
    }
    
    /**
     * Sets this RowVecs bitstring to the string passed
     * in. This method will assume that the string does not
     * contain a row id.
     * 
     * @param bitstring The bitstring to be set.
     */
    public void setBitStringTrue(String bitstring){
    	if(bitstring.length() != this.getNumBits()){
    		System.out.println("THERE IS AN ERROR for the number of columns!");
    		System.out.println("Read Length: "+bitstring.length());
    		System.out.println("Expected Length: "+this.getNumBits());
    	}
    	
    	for(int i = 0; i< this.getNumBits(); i++){
    		this.bitString[i] = (Integer.parseInt(bitstring.substring(i,i+1)) == 1);
    	}
    }

    
    /**
     * Retrieve a bit from a specified location
     * in the row.
     * @param bit The place of the bit
     * @return The bit at the specified placement.
     */
    public boolean getBit(int bit) {
        if (bit< this.getNumBits())
        {
            return this.bitString[bit];
        }
        else
        {
        	return false;
       	}
    }
    
    /**
     *
     * @return Returns the RowVec ID.
     */
    public int getId()
    {
        return this.id;
    }
    
    /**
     * 
     * @return Returns this RowVecs bitstring as a String.
     */
    public String getBitString() {
        StringBuffer s = new StringBuffer();//= new String(bitString);
        for (int i=0; i< this.getNumBits(); i++) {
            if (this.bitString[i])
            {
                s.append(1);
            }
            else{
            	s.append(0);
            }
            	
        }
        return s.toString();
    }

    /**
     * @return number of bits in bitString
     * 
     * */
	public int getNumBits() {
		return numBits;
	}
	
	
	
	/**
	 * Sets a single bit 
	 * 
	 * @param index the position in the string that will be changed
	 * @param value the new value
	 * */
	public void setBit(int index, boolean value){
		this.bitString[index] = value;
		
	}
    
 
}