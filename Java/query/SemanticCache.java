package bcf.query;

import bcf.common.ActiveBitCollection;

import java.awt.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;

public class SemanticCache {
    private HashMap<Integer,ArrayList<Node>> cache;
    private int numRanges;

    public SemanticCache(){
        cache = new HashMap<Integer, ArrayList<Node>>();
    }

    public int getNumRanges(){
       return numRanges;
    }

    public boolean keyExists(int start){
        if(cache.containsKey(start)){
            return true;
        }
        return false;
    }

    public int degree(int start){
        ArrayList<Node> list = cache.get(start);
        return list.size();
    }

    public ArrayList<Node> getList(int start){
        return cache.get(start);
    }

    public void addList(int start){
        ArrayList<Node> list = new ArrayList<Node>();
        cache.put(start,list);
    }

    public Node getNode(int start, int index){
        if(keyExists(start)){
            ArrayList<Node> list = cache.get(start);
            return list.get(index);
        }
        return null;
    }

    public void addNode(int start, Node node){
        //System.out.println("ADD NODE " + start + ", " + node.getEnd());
        ArrayList<Node> list = cache.get(start);
        list.add(node);
        node.setCached();
    }

    public boolean containsNode(int start, int end){
        List<Node> list = cache.get(start);
        for(Node node : list){
            if(node.getEnd() == end && node.getStart() == start){
                return true;
            }
        }
        return false;
    }

    public String toString(){
        String str ="";
        List<Node> list = new ArrayList<Node>();

        for(int key : cache.keySet()){
            str += key + ":";
            list = cache.get(key);
            for(int i=0; i<list.size(); i++){
                str += list.get(i).toString();
            }

            str += "\n";
        }
        return str;
    }

    public Node[] jarvisLookup(int start, int end) {
      //  System.out.println("\t\tSTART JARVIS LOOKUP ");
        int current_start = 1000;
        int current_end = -1000;
        int maxSubranges = (end-start) +1;
        WAHPointQuery ptQueryEng = new WAHPointQuery();
        //Node ret = new Node(current_start, current_end, null);
        Node[] subranges = new Node[maxSubranges];
        ArrayList<Node> list;
        int k = end-start;
        Node current;
        int currentRange = 0;
        int j = 0;

        NodeComparator compEng = new NodeComparator();
        // FIND ALL POSSIBLE SUBRANGES
        for(int i=start; i<=(start+k); i++){
            if(keyExists(i)){
                list = getList(i);
            }else{
                continue; //move on if list doesn't exist
            }

            Collections.sort(list, compEng); //sort list in descending order

            j=0;
            current = getNode(i,j);
            while(current != null){
                if(current.getEnd() > end){ //removes subranges with too much coverage
                    j++;
                    if(j < list.size()){
                        current = getNode(i,j);
                        continue;
                    }else{
                        break;
                    }

                }
                if(current_end < current.getEnd()){
                    if(current_start == 1000){ //update on first instance only
                        current_start = i;
                    }
                    current_end = current.getEnd();
                    subranges[currentRange] = current;
                    currentRange++;
                    break;
                }
                j++;
                if(j < list.size()){
                    current = list.get(j); //get next node 
                } else{
                    break;
                }

            }
        }

        int totalSubRanges = currentRange;
        Node[] ret = new Node[totalSubRanges];
        // END FIND SUBRANGES

        //if no subranges found then return the empty set
        if(subranges[0] == null){
            return null;
        }

        // first subrange must be included because it provides unique coverage
        Node tmp = subranges[0];
        ret[0] = tmp;

        if(tmp.getStart() == start  && tmp.getEnd() == end){ //full query was found
            ret[0].setCached();
            return ret;
        }

        // otherwise find sub-ranges with most coverage
        int currentSubrange = 1;
        int i = 1;
        int found;
        while(i < totalSubRanges){
            found = 0;
            while(i < totalSubRanges && subranges[i].getStart() <= current_end+1){
                i++;
                found = 1;
            }
            if(found == 1){
                i--;
            }
            // OR subranges[i] with query being made
            //ret.setVector(ptQueryEng.OrQuery(subranges[i].getVector(), ret.getVector()));
            ret[currentSubrange] = subranges[i];
            current_end = subranges[i].getEnd();
            currentSubrange++;
            i++;
        }

        numRanges = currentSubrange ; //TODO NOT SURE
        return ret;
    }


    //TRY TO WRITE NON-RECURSIVELY!!! -- USE A STACK (BEAT JARVIS IN TERMS OF TIME -- NOT IN TERMS OF COVERAGE)
    public Node findAndSplitLookup(int start, int end) {
        //System.out.println("\t\tSTART FS LOOKUP");
        ActiveBitCollection result = null;
        WAHPointQuery ptQueryEng = new WAHPointQuery();
        Node ret = null;
        numRanges =0;
        boolean full_query = false;
        int current_start = start;
        int current_end = start;
        Node mostComparable;
        int currentDiff=0;
        int bestDiff = 0;
        ArrayList<Node> list;
        int j;
        Node current;

        // keep going until pieces are past end of query
        while(current_start <= end){
            // Grab list if exists of current_start
            if(keyExists(current_start)){
                list = getList(current_start);
            }else{
                break; //want to end if we come upon an empty list
            }

            // Grab head of the list
            //System.out.println("\t"+list);
            j=0;
            current = getNode(current_start,j);
            //System.out.println("current node: "+current.toString());

            mostComparable = current;
            bestDiff = Math.abs(current_start-mostComparable.getStart()) + Math.abs(end-mostComparable.getEnd());
            while(current != null){
                currentDiff = Math.abs(current_start-current.getStart()) + Math.abs(end-current.getEnd());
                if(currentDiff < bestDiff){ //update the most comparable node
                    mostComparable = current;
                    bestDiff = currentDiff;
                }
                j++;
                if(j < list.size()){ //if there's another node
                    current = list.get(j); //get next node
                }else{ //no more nodes in this list
                    break;
                }
            }

            // Check to see if we found the complete query
            if(mostComparable.getStart() == start && mostComparable.getEnd() == end){
                full_query = true;
            }

            // Combine the resulting vectors together
            if(mostComparable.getStart() == start){ // the first result found
                result = mostComparable.getVector();
            }else{ // add vector to the combined results
                result = ptQueryEng.OrQuery(result, mostComparable.getVector());
            }

            // Update current start and end after getting most comparable node in list
              current_end = mostComparable.getEnd();

            // update current_start to start up where last node left off
            current_start = current_end+1;
            numRanges++;
        }

        ret = new Node(start, current_end, result);
        if(full_query == true){
            ret.setCached();
        }

        return ret;
    }

}
