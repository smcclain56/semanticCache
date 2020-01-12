package bcf.query;

import java.util.Comparator;

public class NodeComparator implements Comparator<Node> {

    public NodeComparator(){
        super();
    }
    /**
     * Compares n1 and n2 by their ending column number. Assuming
     * the nodes have the same start column and we only comparing
     * their ending columns
     *
     * @param n1 the first node to compare
     * @param n2 the second node to compare
     * @return 0 if n1.end == n2.end
     *         -1 if n1.end < n2.end
     *         1 if n1.end > n2.end
     */
    @Override
    public int compare(Node n1, Node n2) {
        if(n1.getEnd() == n2.getEnd()){
            return 0;
        }else if(n1.getEnd() > n2.getEnd()){
            return -1;
        }else{ //n1.end < n2.end
            return 1;
        }
    }
}
