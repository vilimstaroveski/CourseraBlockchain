package course.assingment.consensus;
import java.util.HashSet;
import java.util.Set;

public class MaliciousNode implements Node {

    public MaliciousNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
    }

    public void setFollowees(boolean[] followees) {
        return;
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        return;
    }

    public Set<Transaction> sendToFollowers() {
        return new HashSet<Transaction>();
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        return;
    }
}
