package course.assingment.consensus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {

	private boolean[] followees;
	private Set<Transaction> confirmedTransactions;
	private Map<Integer, List<Transaction>> txRecivedFromFollowees;
	
    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
    	confirmedTransactions = new HashSet<>();
    	txRecivedFromFollowees = new HashMap<>();
    }

    public void setFollowees(boolean[] followees) {
    	this.followees = Arrays.copyOf(followees, followees.length);
    	for(int i=0; i<followees.length; i++) {
    		if(followees[i])
    			txRecivedFromFollowees.put(i, new ArrayList<>());
    	}
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
    	this.confirmedTransactions.addAll(pendingTransactions);
    }

    public Set<Transaction> sendToFollowers() {
        return confirmedTransactions;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        for(Candidate c : candidates) {
        	if(followees[c.sender]) {//ako je posiljatelj moj followee
        		txRecivedFromFollowees.get(c.sender).add(c.tx);
        		confirmedTransactions.add(c.tx);
        	}
        }
        for(Integer followe : txRecivedFromFollowees.keySet()) {
        	if(txRecivedFromFollowees.get(followe).isEmpty()) {//folowee has not broadcast any tx -> malicous
        		followees[followe] = false;
        	}
        }
        
    }
}
