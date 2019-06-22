
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;

    private TransactionPool transactionPool;
    //recent blocks (relevant to CUT_OFF_AGE history)
    private Map<byte[], Block> blocks;
    //block to their utxopool
    private Map<byte[], UTXOPool> blockToUtxoPool;
    //heights?
    private Map<byte[], Integer> blockHeights;
    //block age
    private List<byte[]> blocksChronologically;
    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
    	transactionPool = new TransactionPool();
    	blocks = new HashMap<>();
    	blockToUtxoPool = new HashMap<>();
    	blockHeights = new HashMap<>();
    	blocksChronologically = new ArrayList<>();
    	
    	this.addBlock(genesisBlock);
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
    	Map.Entry<byte[], Integer> maxHeightBlock = null;
    	for(Map.Entry<byte[], Integer> entry : blockHeights.entrySet()) {
    		if (maxHeightBlock == null)
    			maxHeightBlock = entry;
    		else if (entry.getValue() >= maxHeightBlock.getValue()) {
    			if(maxHeightBlock.getValue() == entry.getValue()) {
    				maxHeightBlock = blocksChronologically.indexOf(maxHeightBlock.getKey()) < blocksChronologically.indexOf(entry.getKey()) ? maxHeightBlock : entry;
    			}
    			else {
    				maxHeightBlock = entry;
    			}
    	    }
    	}
		return blocks.get(maxHeightBlock.getKey());
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
		return new UTXOPool(blockToUtxoPool.get(getMaxHeightBlock().getHash()));
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
		return transactionPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
    	//CURRENT STATE OF THE BLOCK CHAIN WHERE WE WANT TO ADD THE BLOCK
    	UTXOPool utxopool;
    	Integer height;
    	
    	//CHECKING THE PREVIOUS HASH
    	if(block.getPrevBlockHash() == null) {
    		if(blocks.size() != 0) {
    			//if genesis block already exists, return false
    			return false;
    		}
    		utxopool = new UTXOPool();
    		height = -1;
    	}
    	else if(!blocks.containsKey(block.getPrevBlockHash())) {
			//if block wants to attach onto a non existing(or too old) block, return false
			return false;
    	}
    	else {
    		utxopool = blockToUtxoPool.get(block.getPrevBlockHash());
    		height = blockHeights.get(block.getPrevBlockHash());
    	}
    	
    	
    	//CHECKING TXS VALIDITY
    	TxHandler txh = new TxHandler(utxopool);
    	Transaction[] acceptedTxs = txh.handleTxs(block.getTransactions().toArray(new Transaction[0]));
    	if(acceptedTxs.length != block.getTransactions().size()) {
    		return false;
    	}
    	
		//ADD THE BLOCK TO THE BLOCK CHAIN
    	txh.getUTXOPool().addUTXO(new UTXO(block.getCoinbase().getHash(), 0), block.getCoinbase().getOutput(0));
		recordOnBlockChain(block, height, txh.getUTXOPool());
    	
		//cut off old blocks:
    	Block head = getMaxHeightBlock();
    	Integer headHeight = blockHeights.get(head.getHash());
    	// cutting all blocks that are UNDER headHeight-CUT_OFF_AGE
    	//finding old blocks
    	List<byte[]> blockHashesToRemove = new ArrayList<>();
    	for(Map.Entry<byte[], Integer> entry : blockHeights.entrySet()) {
    		if (entry.getValue() < headHeight-CUT_OFF_AGE) {
    			blockHashesToRemove.add(entry.getKey());
    	    }
    	}
    	//finally, remove them
    	for(byte[] hash : blockHashesToRemove) {
    		blocks.remove(hash);
    		blockHeights.remove(hash);
    		blockToUtxoPool.remove(hash);
    		blocksChronologically.remove(blocksChronologically.indexOf(hash));
    	}
		return true;
    }

	private void recordOnBlockChain(Block block, Integer prevHeight, UTXOPool newUtxopool) {
    	//add a block into recent blocks
    	blocks.put(block.getHash(), block);
    	//record the height it is being put on
    	blockHeights.put(block.getHash(), prevHeight+1);
    	//save new utxopool
    	blockToUtxoPool.put(block.getHash(), newUtxopool);
    	//UPDATE TXPOOL
    	for(Transaction t : block.getTransactions()) {
    		transactionPool.removeTransaction(t.getHash());
    	}
    	blocksChronologically.add(block.getHash());
    }
    
//    private List<UTXO> allUtxoUsedInTx(Transaction tx) {
//		List<UTXO> txUtxosUsed = new ArrayList<>();
//		for(Transaction.Input i : tx.getInputs()) {
//			txUtxosUsed.add(new UTXO(i.prevTxHash, i.outputIndex));
//		}
//		return txUtxosUsed;
//	}

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        transactionPool.addTransaction(tx);
    }
}