
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

public class BlockChainBackup {
    public static final int CUT_OFF_AGE = 10;

    private TransactionPool transactionPool;
    //recent blocks (relevant to CUT_OFF_AGE history)
    private Map<byte[], Block> blocks;
    //block to their txhandler
    private Map<byte[], TxHandler> blockToTxHandler;
    //heights?
    private Map<byte[], Integer> blockHeights;
    
    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChainBackup(Block genesisBlock) {
    	transactionPool = new TransactionPool();
    	blocks = new HashMap<>();
    	blockToTxHandler = new HashMap<>();
    	blockHeights = new HashMap<>();
    	
    	this.addBlock(genesisBlock);
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
    	Map.Entry<byte[], Integer> maxHeightBlock = null;
    	for(Map.Entry<byte[], Integer> entry : blockHeights.entrySet()) {
    		if (maxHeightBlock == null || entry.getValue() > maxHeightBlock.getValue()) {
    			maxHeightBlock = entry;
    	    }
    	}
		return blocks.get(maxHeightBlock.getKey());
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
		return new UTXOPool(blockToTxHandler.get(getMaxHeightBlock().getHash()).getUTXOPool());
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
    	TxHandler currentTxHandler;
    	Integer currentHeight;
    	
    	//CHECKING THE PREVIOUS HASH
    	if(block.getPrevBlockHash() == null) {
    		if(blocks.size() != 0) {
    			//if genesis block already exists, return false
    			System.out.println("genesis block already exists");
    			return false;
    		}
    		currentTxHandler = new TxHandler(new UTXOPool());
    		currentHeight = 0;
    	}
    	else if(!blocks.containsKey(block.getPrevBlockHash())) {
			//if block wants to attach onto a non existing(or too old) block, return false
    		System.out.println("wanting to attach onto a non existing block");
			return false;
    	}
    	else {
    		currentTxHandler = blockToTxHandler.get(block.getPrevBlockHash());
    		currentHeight = blockHeights.get(block.getPrevBlockHash());
    	}
    	
    	System.out.println("CURRENT STATE OF UTXOS:");
    	for(UTXO u : currentTxHandler.getUTXOPool().getAllUTXO()) {
    		System.out.println("transaction:"+u.getTxHash()+" index:"+u.getIndex());
    	}
    	
    	//CHECKING TXS VALIDITY
    	TxHandler txhValidator = new TxHandler(new UTXOPool(currentTxHandler.getUTXOPool()));
    	Transaction[] acceptedTxs = txhValidator.handleTxs(block.getTransactions().toArray(new Transaction[0]));
    	if(acceptedTxs.length != block.getTransactions().size()) {
    		System.out.print("transactions invalid:");
    		System.out.println(acceptedTxs.length+"/"+block.getTransactions().size());
    		return false;
    	}
    	
    	//UPDATE UTXO POOL
    	currentTxHandler.handleTxs(block.getTransactions().toArray(new Transaction[0]));
		
		//ADD THE BLOCK TO THE BLOCK CHAIN
		recordOnBlockChain(block, currentHeight, currentTxHandler);
    	
		//cut off old blocks:
    	Block head = getMaxHeightBlock();
    	Integer headHeight = blockHeights.get(head.getHash());
    	// cutting all blocks that are UNDER headHeight-CUT_OFF_AGE
    	//finding old blocks
    	List<byte[]> blockHashesToRemove = new ArrayList<>();
    	for(Map.Entry<byte[], Integer> entry : blockHeights.entrySet()) {
    		if (entry.getValue() <= headHeight-CUT_OFF_AGE) {
    			blockHashesToRemove.add(entry.getKey());
    	    }
    	}
    	//finally, remove them
    	for(byte[] hash : blockHashesToRemove) {
    		System.out.println("REMOVING OLD BLOCK: "+hash);
    		blocks.remove(hash);
    		blockHeights.remove(hash);
    		blockToTxHandler.remove(hash);
    	}
    	System.out.println("Block "+block.hashCode()+" added.");
    	System.out.println("AFTER ADDING -> STATE OF UTXOS:");
    	for(UTXO u : currentTxHandler.getUTXOPool().getAllUTXO()) {
    		System.out.println("transaction:"+u.getTxHash()+" index:"+u.getIndex());
    	}
		return true;
    }
    
    private void recordOnBlockChain(Block block, Integer prevHeight, TxHandler newTxHandler) {
    	//add a block into recent blocks
    	blocks.put(block.getHash(), block);
    	//record the height it is being put on
    	blockHeights.put(block.getHash(), prevHeight+1);
    	//save new txhandler
    	blockToTxHandler.put(block.getHash(), newTxHandler);
    	//UPDATE TXPOOL
    	for(Transaction t : block.getTransactions()) {
    		transactionPool.removeTransaction(t.getHash());
    	}
    }
    

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        transactionPool.addTransaction(tx);
    }
}