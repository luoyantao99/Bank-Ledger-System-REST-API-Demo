package dev.bankledger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores and manages all transactions within the application.
 * Utilizes a ConcurrentHashMap to ensure thread-safe operations.
 */
public class EventStore {
	private final ConcurrentHashMap<String, List<Transaction>> transactions = new ConcurrentHashMap<>();

	/**
	 * Adds a transaction to the store.
	 * If no transactions exist for a given userId, it initializes a new list.
	 *
	 * @param transaction The transaction to add.
	 */
	public void addTransaction(Transaction transaction) {
		transactions.computeIfAbsent(transaction.getUserId(), k -> new ArrayList<>()).add(transaction);
	}

	/**
	 * Retrieves all transactions for a specified user.
	 *
	 * @param userId The ID of the user whose transactions are to be retrieved.
	 * @return A list of transactions for the user.
	 */
	public List<Transaction> getTransactionsForUser(String userId) {
		return transactions.getOrDefault(userId, new ArrayList<>());
	}

	/**
	 * Computes the current balance for a specified user based on their transactions.
	 * Only 'APPROVED' transactions affect the balance.
	 *
	 * @param userId The ID of the user whose balance is to be computed.
	 * @return The computed balance as a BigDecimal.
	 */
	public BigDecimal computeBalance(String userId) {
		BigDecimal balance = BigDecimal.ZERO;
		List<Transaction> userTransactions = transactions.get(userId);

		// Process each transaction to compute the balance.
		if (userTransactions != null) {
			for (Transaction transaction : userTransactions) {
				if ("APPROVED".equals(transaction.getStatus())) {
					BigDecimal amount = new BigDecimal(transaction.getTransactionAmount().getAmount());
					// Add or subtract the transaction amount based on the type (CREDIT or DEBIT).
					if (transaction.getTransactionAmount().getDebitOrCredit() == DebitCredit.CREDIT) {
						balance = balance.add(amount);
					} else if (transaction.getTransactionAmount().getDebitOrCredit() == DebitCredit.DEBIT) {
						balance = balance.subtract(amount);
					}
				}
			}
		}
		return balance;
	}
}
