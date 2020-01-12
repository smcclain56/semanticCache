package bcf.query;

import bcf.common.ActiveBitCollection;

public class Node {
    private int start;
    private int end;
    private ActiveBitCollection vector;
    private int cached; //holds whether the node is stored in cache or not (0= false, 1=true)

    public Node(int startCol, int endCol, ActiveBitCollection resultVector){
        start = startCol;
        end = endCol;
        vector = resultVector;
        cached = 0;  //start out not in cache
    }

    /**
        Returns the current value of cached. 1 means the node is held in the cache, and 0 means
        it is not
     */
    public int getCached(){
        return cached;
    }
    /**
    Sets cached to 1 (meaning it was added to the cache)
     */
    public void setCached(){
        cached = 1;
    }
    public int getStart(){
        return start;
    }

    public int getEnd(){
        return end;
    }

    public ActiveBitCollection getVector(){
        return vector;
    }

    public void setVector(ActiveBitCollection newVector){
        vector = newVector;
    }

    public void setStart(int newStart){
        start = newStart;
    }

    public void setEnd(int newEnd){
        end = newEnd;
    }

    public String toString(){
        String str = "";
        str += "(" + start + " ," + end + ")";
        return str;
    }
}
