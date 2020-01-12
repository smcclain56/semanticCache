package bcf.query;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

import bcf.common.ActiveBitCollection;
import bcf.common.VALActiveBitCollection;



public class VALReadBitmap implements CompressedBitmapReader {

	@Override
	public ActiveBitCollection readColumn(DataInputStream columnIn) {
	
		// TODO Auto-generated method stub
		VALActiveBitCollection bV=new VALActiveBitCollection(); 
		try{
         // System.out.println("key =  "+key+"   file:  "+fileName);
          while (true) {
          try {                    
              bV.appendWord(0xFFFFFFFFL & columnIn.readInt());
          } catch (EOFException eof) {
              //System.out.println ("End of File");
              break;
          }
          }
         // numberofWords = bV.getNumberOfWords(); //This is doing it for each bitmap that is read
                       
          columnIn.close ();
		
		}catch(Exception e){
			e.printStackTrace();
		}
		return bV;
	}

}
