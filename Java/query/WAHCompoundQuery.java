package bcf.query;

import bcf.common.ActiveBitCollection;
import bcf.query.WAHRangeQuery;

import javax.management.Query;
import java.util.ArrayList;
import java.util.HashMap;

//Query processing (extraneous stuff) and overhead (just for jarvis)
//how big is cache and how many entries are in there at the end??? mine should be more than jarvis?
    //knowing number of entries will give good approx on how cache is growing
//replacement policy
public class WAHCompoundQuery implements CompoundQuery {
    protected final int CACHE;
    protected final int CACHE_POLICY;
    protected SemanticCache cache;
    protected HashMap<Integer, ActiveBitCollection> columns;
    protected WAHPointQuery ptQueryEng;
    protected WAHRangeQuery rangeQueryEng;

    protected long lookupTime;
    protected long overheadTime;

    protected int numColumnsFound;
    protected int numColumnWanted;
    protected int numRangesFound;
    protected int numRangesWanted;
    protected boolean rangeQuery;



    public WAHCompoundQuery(SemanticCache cache, HashMap<Integer, ActiveBitCollection> columns){
        CACHE = 0; //0 means no cache
                    //1 means there is a cache
        CACHE_POLICY = 0; // 0 means find and split lookup algorithm
                                // 1 means jarvis lookup algorithm
        this.cache = cache;
        this.columns = columns;
        ptQueryEng = new WAHPointQuery();
        rangeQueryEng = new WAHRangeQuery();
        lookupTime =0;
        overheadTime =0;
        numColumnsFound =0;
        numColumnWanted =0;
        numRangesFound =0;
        numRangesWanted =0;
        rangeQuery = false;
    }
    @Override
    public ActiveBitCollection query(SemanticCache cache, HashMap<Integer, ActiveBitCollection> columns, String[] tokens) {

        Node result = null;
        ActiveBitCollection ret = null;
        ActiveBitCollection subQuery = null;

        // get the number of ranges in the compound query
        int numRanges = (int) Math.ceil(tokens.length / 3.0);
        numRangesWanted = numRanges;
        // Get start and end of first sub-query
        int start = Integer.parseInt(tokens[0]);
        int end = Integer.parseInt(tokens[1]);

        // check the cache for the sub-query
        if(CACHE == 1){
            ret = checkCache(start, end);
        }else{ //if no cache then execute query
            ret = executeQuery(start, end);
        }


        // if there is only one range return result of vector
        if (numRanges == 1) {
            return ret;
        }

        // otherwise combine with the other sub-queries
        int i = 3;
        int j = 2;
        while (i <= tokens.length) {
            // otherwise execute the range query
            start = Integer.parseInt(tokens[i]);
            end = Integer.parseInt(tokens[i + 1]);

            // find next sub-query in cache
            if(CACHE == 1){
                subQuery = checkCache(start, end);
            }else{ //or execute query if no cache
                subQuery = executeQuery(start,end);
            }

            // combine the sub-query to the result
            if (tokens[j].equals('&')) { // AND the sub-query to the current result
                ret = ptQueryEng.AndQuery(ret, subQuery);
            } else if (tokens[j].equals('|')) { // OR the sub-query to the current result
                ret = ptQueryEng.OrQuery(ret, subQuery);
            }
            i += 3;
            j += 3;
        }

        return ret;
    }

    /**
     * Returns the time needed to look through the cache for query (either for jarvis or FS algorithms)
     * @return returns the lookup time needed to search cache for query
     */
    public long getLookupTime(){
        return lookupTime;
    }

    /**
     * Returns the overhead time needed to use the cache. Consists the time needed to find the
     * remainder query (either for jarvis or FS algorithms)
     * @return returns the overhead time needed to search semantic cache for query
     */
    public long getOverheadTime(){
        return overheadTime;
    }

    /**
     * Returns the % of the total query, the algorithm found within the cache
     * @return returns the percentage of total query that was found within the cache
     */
    public double getCoverage(){
        double coverage = (numColumnsFound / (double) numColumnWanted) * 100.0;
        if(coverage > 100.0){
            return 100.0;
        }
        return coverage;
    }

    /**
     * Returns the number of ranges that were found in the cache using the lookup algorithm
     * @return returns the number of ranges that were found in cache
     */
    public int getNumRanges(){
        return numRangesFound;
    }

    /*********************************************************
     * Private Methods
     *********************************************************/

    private ActiveBitCollection executeQuery(int start, int end){
        return rangeQueryEng.OrQuery(columns,start, end);
    }

    private ActiveBitCollection checkCache(int start, int end) {
        Node FSresult = null;
        Node jarvisResult[] = null;
        ActiveBitCollection subQuery = null;
        long lookup_start =0;
        long lookup_end = 0;
        long overhead_start;
        long overhead_end;
        // Add a new list in the cache if it doesn't exist yet
        if (!cache.keyExists(start)) {
            cache.addList(start);
        } else { // Search the cache for the query
            if(CACHE_POLICY == 0){ //use find and split algorithm
                lookup_start = System.nanoTime();
                FSresult = cache.findAndSplitLookup(start, end);
                lookup_end = System.nanoTime();
            }else{ // use jarvis
                lookup_start = System.nanoTime();
                jarvisResult = cache.jarvisLookup(start,end);
                lookup_end = System.nanoTime();
            }
        }

        int columns_found;
        int columns_wanted = (end-start)+1;
        if(CACHE_POLICY == 0){
            overhead_start = System.nanoTime();
            columns_found = getRemainderFS(start, end, FSresult);
            overhead_end = System.nanoTime();

        }else{
            overhead_start = System.nanoTime();
            columns_found = getRemainderJarvis(start, end, jarvisResult);
            overhead_end = System.nanoTime();
        }

        //update variables
        lookupTime = lookup_end - lookup_start;
        overheadTime = overhead_end - overhead_start;

        if(start != end){
            rangeQuery = true;
            numColumnsFound += columns_found;
            numColumnWanted += columns_wanted;
            numRangesFound += cache.getNumRanges();
            //System.out.println(numRangesFound);
            //System.out.println(coverage  + "\t" + numRangesFound);
        }
        return subQuery;
    }

    /**
     * Finds the remaining columns needed that were not found from the jarvis search through the cache.
     * Combines vectors found from cache and those calculated and stores the new result in the cache.
     * Returns the number of columns found from cache.
     * @param start starting column being queried
     * @param end ending column being queried
     * @param found an array holding the nodes found within the cache from jarvis
     * @return
     */
    private int getRemainderJarvis(int start, int end, Node[] found){
        Node ret = null;
        ActiveBitCollection result = null;
        int current_start = 0;
        int current_end = 0;
        int columns_found =0;
        if(found == null){ //no subranges found
            //System.out.println("NONE CACHED -- EXECUTE QUERY [" + start + ", " + end +"]");
            result = executeQuery(start, end);
            ret = new Node(start, end, result);
            cache.addNode(start, ret);
            return 0;
        }
        if(found.length == 1 && found[0].getStart() == start && found[0].getEnd() == end){ //full query found in one piece
            //System.out.println("FULL QUERY CACHED");
            ret = new Node(start, end, found[0].getVector());
            columns_found = (found[0].getEnd() - found[0].getStart() +1);
            return columns_found;
        }

        //System.out.println("PIECES FOUND/EXECUTED:");
        // first combine the found vectors
        result = found[0].getVector();
        //System.out.print("FOUND IN CACHE ["+ found[0].getStart() + ", " + found[0].getEnd() + "]\t");
        for(int k =1; k< found.length; k++){
            if(found[k] != null){
                //System.out.print("FOUND IN CACHE ["+ found[k].getStart() + ", " + found[k].getEnd() + "]\t");
                columns_found = (found[k].getEnd() - found[k].getStart() +1);
                result = ptQueryEng.OrQuery(result, found[k].getVector());
            }
        }

        //Now piece together the parts to be executed
        int i = start;
        int j = 0; //index of node found
        while(i <= end && j < found.length) {
            if(found[j] == null){
                break;
            }
            current_start = found[j].getStart();
            current_end = found[j].getEnd();
            if (i < current_start ) { //need to execute between i and current_start -1
                result = ptQueryEng.OrQuery(result, executeQuery(i, current_start - 1)); //otherwise complete an OR between temp and new result
                //System.out.print("EXECUTED ["+ i + ", " + (current_start-1) + "]\t");
            }
            j++;
            i = current_end+1;
        }

        //are there any trailing vectors that need to be added to result
        if(i <= end){
            result = ptQueryEng.OrQuery(result, executeQuery(i, end));
            //System.out.print("EXECUTED ["+ i + ", " + end + "]\t");
        }

        // add result to the cache and return
        ret = new Node(start, end, result);
        ret.setCached();
        cache.addNode(start, ret);
        return columns_found;
    }


    /**
     * Gets the remainder query based on full query being searched and the node resulting from FS search.
     * Returns the number of columns found from cache
     * @param start starting column being queried
     * @param end ending column being queried
     * @param found node found from doing FS search in the cache
     * @return
     */
    private int getRemainderFS(int start, int end, Node found) {
        ActiveBitCollection results = null;

        if(found == null){
            results = executeQuery(start, end);
            Node newNode = new Node(start,end, results);
            //System.out.println("EXECUTE QUERY");
            cache.addNode(start, newNode);
            return 0;
        }

        // check if we found full query
        if (start == found.getStart() && end == found.getEnd()) { //full query was found
            if(found.getCached() == 0){ //query was fully formed using partial matched
                cache.addNode(start, found);
            }
            return (end-start)+1;
        }

        // Add or get rid of columns to satisfy query
        if (start < found.getStart() && end < found.getEnd()) {
            results = chop(start, end, found);
            results = ptQueryEng.OrQuery(results, add_startW_to_startS(start, found.getStart()));
        } else if (start == found.getStart() && end < found.getEnd()) {
            results = chop(start, end, found);
        } else if (found.getStart() < end && found.getEnd() == end) {
            results = chop(start, end, found);
            results = ptQueryEng.OrQuery(results, chop(start, end, found));
        } else if (found.getStart() < start && end < found.getEnd()) {
            results = chop(start, end, found);
        } else if (found.getStart() < start && end > found.getEnd()) {
            results = chop(start, end, found);
            results = ptQueryEng.OrQuery(results, add_endS_to_endW(found.getEnd(), end));
        }

        // all columns from cache are wanted, plus additional ones
        else if (start < found.getStart() && end > found.getEnd()) {
            results = add_startW_to_startS(start, found.getStart());
            results = ptQueryEng.OrQuery(results, add_endS_to_endW(found.getEnd(), end));
        } else if (found.getEnd() == end && start < found.getStart()) {
            results = add_startW_to_startS(start, found.getStart());
        } else if (start == found.getStart() && found.getEnd() < end) {
            results = add_endS_to_endW(found.getEnd(), end);
        }

        //insert new node into cache
        Node newNode = new Node(start, end, results);
        cache.addNode(start, newNode);

        int columns_found = (found.getEnd() - found.getStart())+1;
        return columns_found;
    }

    // TODO CHECK ALL OF THESE

    private ActiveBitCollection add_startW_to_startS(int start_wanted, int start_stored) {
        ActiveBitCollection ret = rangeQueryEng.OrQuery(columns, start_wanted, start_stored);
        return ret;
    }

    private ActiveBitCollection add_endS_to_endW( int end_stored, int end_wanted) {
        ActiveBitCollection ret = rangeQueryEng.OrQuery(columns, end_stored, end_wanted);
        return ret;
    }

    private ActiveBitCollection chop(int start_wanted, int end_wanted, Node found){
        ActiveBitCollection ret = null;
        int numColumnsWanted = end_wanted - start_wanted+1;
        ActiveBitCollection[] ptQueries = new ActiveBitCollection[numColumnsWanted];

        //Chop off pieces between end_wanted and end_stored
        int j=0;
        for(int i=start_wanted; i<=end_wanted; i++){
            ptQueries[j] = ptQueryEng.AndQuery(found.getVector(), columns.get(i));
            j++;
        }

        // Or all results back together
        ret = ptQueries[0];
        for(int i=1; i< numColumnsWanted; i++) {
            ret = ptQueryEng.OrQuery(ret, ptQueries[i]);
        }

        return ret;
    }

}


