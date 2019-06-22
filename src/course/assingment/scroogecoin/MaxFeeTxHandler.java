package course.assingment.scroogecoin;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class MaxFeeTxHandler {

	private UTXOPool utxoPool;
	
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public MaxFeeTxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
    	return  areInputsAvailable(tx)
    			&&  !anySigInvalid(tx)
    			&& !anyDoubleSpendInTx(tx) 
    			&& !anyNegativeOutput(tx)
    			&& inputGreaterThanOutput(tx);
    }

    //returns true if tx IS valid
    private boolean inputGreaterThanOutput(Transaction tx) {
    	return calculateTxFee(tx) >= 0;
	}

    //returns true if tx is NOT valid
	private boolean anyNegativeOutput(Transaction tx) {
		return tx.getOutputs().stream().anyMatch(o -> o.value < 0);
	}

	//returns true if tx is NOT valid
	private boolean anyDoubleSpendInTx(Transaction tx) {
		List<UTXO> txUtxosUsed = new ArrayList<>();
		for(Transaction.Input i : tx.getInputs()) {
			UTXO possibleUtxo = new UTXO(i.prevTxHash, i.outputIndex);
			if(txUtxosUsed.contains(possibleUtxo))
				return true;
			txUtxosUsed.add(possibleUtxo);
		}
		return false;
	}
	
	//returns true if tx is NOT valid
	private boolean anySigInvalid(Transaction tx) {
		return tx.getInputs()
				.stream()
				.anyMatch(
						i -> !Crypto.verifySignature(
							utxoPool.getTxOutput(new UTXO(i.prevTxHash, i.outputIndex)).address, 
							tx.getRawDataToSign(tx.getInputs().indexOf(i)), 
							i.signature)
						);
	}

	//returns true if tx IS valid
	private boolean areInputsAvailable(Transaction tx) {
		return tx.getInputs()
				.stream()
				.allMatch(i -> utxoPool.contains(new UTXO(i.prevTxHash, i.outputIndex)));
	}

	private List<UTXO> allUtxoUsedInTx(Transaction tx) {
		List<UTXO> txUtxosUsed = new ArrayList<>();
		for(Transaction.Input i : tx.getInputs()) {
			txUtxosUsed.add(new UTXO(i.prevTxHash, i.outputIndex));
		}
		return txUtxosUsed;
	}
	
	protected double calculateTxFee(Transaction tx) {
		return tx.getInputs().stream().mapToDouble(i -> utxoPool.getTxOutput(new UTXO(i.prevTxHash, i.outputIndex)).value).sum() -
				tx.getOutputs().stream().mapToDouble(o -> o.value).sum();
	}
	
	/**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
    	//accepting transactions as first come first served if there are double spends
    	List<Transaction> validTransactions = new ArrayList<>();
    	
    	Comparator<Transaction> feeCompare = new Comparator<Transaction>() {
			@Override
			public int compare(Transaction o1, Transaction o2) {
				
				double o1Fee = isValidTx(o1) ? calculateTxFee(o1) : 0;
				double o2Fee = isValidTx(o2) ? calculateTxFee(o2) : 0;
				
				return o1Fee - o2Fee > 0 ? 1 : -1;
			}
		};
    	
    	//sort
    	Arrays.sort(possibleTxs, feeCompare);

    	//while the utxopool is being updated, try to put in new txs
    	List<Transaction> newlyValidatedTxs = new ArrayList<>();
    	while(true) {
    		
    		for(Transaction tx : possibleTxs) {
    			if(isValidTx(tx)) {
    				acceptTransaction(tx);
    				validTransactions.add(tx);
    				newlyValidatedTxs.add(tx);
    				possibleTxs = Arrays.stream(possibleTxs).filter(t -> t != tx).toArray(Transaction[]::new);
    				Arrays.sort(possibleTxs, feeCompare);
    				break;
    			}
    		}
    		
    		if(newlyValidatedTxs.isEmpty())
    			break;
    		newlyValidatedTxs.clear();
    	}
    	return validTransactions.toArray(new Transaction[0]);
    }
    
    private void acceptTransaction(Transaction tx) {
    	//update utxoPool
		for(UTXO utxo : allUtxoUsedInTx(tx)) {
			utxoPool.removeUTXO(utxo);
		}
		for(int index = 0; index < tx.getOutputs().size(); index++) {
			utxoPool.addUTXO(new UTXO(tx.getHash(), index), tx.getOutput(index));
   		}
	}
}
