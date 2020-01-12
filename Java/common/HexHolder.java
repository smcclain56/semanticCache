package bcf.common;



/**
 * Class used to hold the values used to identify different fill 
 * configurations. For example, if one of the other classes in the vlcmainctrl
 * package needs to deal with an encoding length of 14, this class can generate
 * and hold the value needed to identify a run of 1s (110000000000000).
 * 
 * @author Fabian
 *
 */
public class HexHolder {
	
	/**Initialize all possible Hexholders at the start*/
	static private HexHolder initHex[] = new HexHolder[VLCConstants.WORD_LEN];
	static{
		for(int i = 1; i < VLCConstants.WORD_LEN; i++){
			initHex[i]= new HexHolder(i);
		}
	}
	
	
	
	/**An empty run of 1s. e.g. 11000000*/
	private long onerun;

	/**An empty run of 0s. e.g. 10000000*/
	private long zerorun;

	/**A run of 1s, (variablelen+1) long. e.g. 11111111*/
	private long ones;

	/**A run of 1s, variablelen long. e.g. 1111111*/
	private long onesnoflag;

	/**A run of zeroes, representing 2 runs. e.g. 10000010*/
	private long zerorunwone;

	/**A run of ones, representing 2 runs. e.g. 11000010*/
	private long onerunwone;

	/**A run of zeroes with the maximum number of runs that can be held. e.g. 10111111 */
	private long zerorunfull;

	/**A run of 1s with a length of the size of an encoding lengths binary representation field. e.g. 111111 */
	private long runlen;


	
	/**
	 * Create an instance of the HexHolder class with
	 * a set encoding length.  Private to ensure that no
	 * one calls it.  Instead they should call
	 * the getHexHolder which returns one of the 
	 * statically created values.
	 * 
	 * @param varlen Encoding length to be used.
	 */
	private HexHolder(int varlen){
		this.setTrueHex(varlen);
	}
	
	static public HexHolder getHexHolder(int varLen){
		return HexHolder.initHex[varLen];
	}
	
	/**
	 * The appropriate number for representing an empty 
	 * run of 1s.
	 * @return onerun
	 */
	public long getOneRun(){
		return this.onerun;
	}
	
	/**
	 * The appropriate number for representing an empty
	 * run of 0s.
	 * @return zerorun
	 */
	public long getZeroRun(){
		return this.zerorun;
	}
	
	/**
	 * Returns the number of 1s needed
	 * to fully represent the encoding length.
	 * (e.g., 7 1s for an encoding length of 6,
	 * 29 1s for an encoding length of 28).
	 * @return ones
	 */
	public long getOnes(){
		return this.ones;
	}
	
	/**
	 * Returns the number of 1s equal
	 * to the variable length.
	 * (e.g., 6 bits for an encoding length
	 * of 6).
	 * 
	 * @return onesnoflag
	 */
	public long getOnesNoFlag(){
		return this.onesnoflag;
	}
	
	/**
	 * The appropriate number for a run of 0s
	 * with one run in it.
	 * 
	 * @return zerorunwone
	 */
	public long getZeroRunWOne(){
		return this.zerorunwone;
	}
	
	/**
	 * The appropriate number for a run of 1s
	 * with one run in it.
	 * @return onerunwone
	 */
	public long getOneRunWOne(){
		return this.onerunwone;
	}
	
	/**
	 * The appropriate number for a run of 0s
	 * at full capacity.
	 * (e.g., for an encoding length of 6,
	 * "1011111")
	 * @return zerorunfull
	 */
	public long getZeroRunFull(){
		return this.zerorunfull;
	}
	
	/**
	 * The number of bits that make
	 * up the run length. The number
	 * of bits used to represent the
	 * number of runs.
	 * @return runlen
	 */
	public long getRunLen(){
		return this.runlen;
	}
	
	
	
	/**
	 * Generates the appropriate values for the 
	 * varlen passed to this HexHolder.
	 * 
	 * @param seglen This HexHolder's current variable length (seglen or encoding length)
	 */
	private void setTrueHex(int seglen){
		long temp  = 0;
		//<= means that it will go seglen+1 catching the flag bit
		for(int i = 0;i<=seglen;i++){
			temp += VLCConstants.powers[i];
			
		}
		
		this.ones = temp;
		this.zerorunfull = (temp - (VLCConstants.powers[seglen-1]));
		this.onesnoflag = (temp - (VLCConstants.powers[seglen]));
		this.runlen = (temp - (VLCConstants.powers[seglen]) - (VLCConstants.powers[seglen-1]));
		this.zerorunwone = (VLCConstants.powers[seglen] + 2);
		this.onerunwone = (VLCConstants.powers[seglen]+VLCConstants.powers[seglen-1]+2);
		this.zerorun = (VLCConstants.powers[seglen]);
		this.onerun = (VLCConstants.powers[seglen]+VLCConstants.powers[seglen-1]);
		

	}
	

}
