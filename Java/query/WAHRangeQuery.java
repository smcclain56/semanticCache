package bcf.query;

import bcf.common.ActiveBitCollection;
import bcf.query.WAHPointQuery;
import java.util.HashMap;

public class WAHRangeQuery implements RangeQuery {

    @Override
    public ActiveBitCollection OrQuery(HashMap<Integer, ActiveBitCollection> columns, int start, int end) {
        ActiveBitCollection ret = null;
        WAHPointQuery queryEng = new WAHPointQuery();

        // loop through executing point query
        ret = columns.get(start);
        for(int i = start+1; i <= end; i++){
            ret = queryEng.OrQuery(ret, columns.get(i));
        }
        return ret;
    }

    @Override
    public ActiveBitCollection AndQuery(HashMap<Integer, ActiveBitCollection> columns, int start, int end) {
        ActiveBitCollection ret = null;
        WAHPointQuery queryEng = new WAHPointQuery();

        // loop through executing point query
        ret = columns.get(start);
        for(int i = start+1; i <= end; i++){
            ret = queryEng.AndQuery(ret, columns.get(i));
        }
        return ret;
    }
}
