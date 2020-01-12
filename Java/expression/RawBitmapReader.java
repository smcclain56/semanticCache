package bcf.compression;
import java.io.*;
import java.util.*;
import bcf.common.*;
/**
 * This class reads in the raw bitmap from a text file.
 * Raw files should be  of the form <rowID, bitstring> where 
 * row id is the integer id of each row and bitstring 
 * is the bit values of that row. e.g:
 * 1,1010100
 * 2,0011001
 * 3,1000100
 * */
public class RawBitmapReader {
	
	
	/**
	 * Reads in a Raw bitmap fill and returns an array of bit strings
	 * that represent the rows of the bitmap
	 * 
	 * @param fully qualified path to the raw bitmap file
	 * @return Bit strings that represent the rows of the bitmap
	 * */
	public static BitStringRep[] readFileRowFormat(String filePath){
		
		BitStringRep [] table = null;
	
		try{
			//Set up to read from the file.
			File file = new File( filePath );
			VLCConstants.USZ_Bytes = file.length();
			
			BufferedReader inFileReader = new BufferedReader(new FileReader(file));

			ArrayList<String> rows = new ArrayList<String>();

			String str ="";

			//Take the lines from the infile and add them to the ArrayList of Rows
			while((str = inFileReader.readLine()) != null)
			{
				rows.add(str);
			}
			
			//get the number of rows
			int numRows = rows.size();
			VLCConstants.numRows = numRows;
			
			//initialized the table array
			table = new BitStringRep[numRows];
			
			//Seperate the actual row from the rowID.
			//The rowID is the first item in the line read in 
			//from the infile.
			StringTokenizer t = new StringTokenizer(rows.get(0), ",");
			t.nextToken();
			
			//Use a row to find the number of columns
			int numCols = t.nextToken().length();
			VLCConstants.numCols = numCols;
			
			
			int id;
			String s;
			//For every row, create an instance of the RowVec
			//class and add it to the table array.
			for (int i=0; i<numRows; i++)
			{
				//Grab the string
				s= rows.get(i);

				//Get rowID
				id = Integer.parseInt((new StringTokenizer(s, ",")).nextToken());
				
				//Creat the RowVec
				BitStringRep my_row = new BitStringRep(numCols);
				my_row.setId(id);
				my_row.setBitString(s);

				//Enter it into our array 
				table[i] = my_row;
			}

			
			inFileReader.close();
		}catch(IOException ex){
		
			ex.printStackTrace();
		}
		return table;
	}
	
	
	
	/**
	 * Reads in a Raw bitmap fill and returns an array of bit strings
	 * that represent the columns of the bitmap
	 * 
	 * @param fully qualified path to the raw bitmap file
	 * @return Bit strings that represent the columns of the bitmap
	 * */
	public static BitStringRep[] readFileColumnFormat(String filePath){
		//cheat and read in the rows 
		BitStringRep[] rows = RawBitmapReader.readFileRowFormat(filePath);
		//just incase the file was empty
		if(rows.length==0){
			return new BitStringRep[0];
		}
		

		//get the number of columns
		int numCols = rows[0].getBitString().length();
		
		BitStringRep[] table = new BitStringRep[numCols];
		
		//for each column in the bitmap
		for(int i = 0; i<numCols; i++){
			//create a new bitstring
			table[i] = new BitStringRep(rows.length);
			//its id will be the column number
			table[i].setId(i);
			//for each row
			for(int k = 0;k<rows.length;k++){
				//the ith column set the kth bit
				table[i].setBit(k, rows[k].getBit(i));
			}
		}
		return table;
	}
	
}
