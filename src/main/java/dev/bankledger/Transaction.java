package dev.bankledger;

import java.util.UUID;

public class Transaction {
	private String userId;
	private String messageId;
	private Amount transactionAmount;
	private String status;
	private long serverTime;

	public Transaction(String userId, Amount transactionAmount, String status) {
		this.userId = userId;
		this.messageId = UUID.randomUUID().toString();
		this.transactionAmount = transactionAmount;
		this.status = status;
		this.serverTime = System.currentTimeMillis();
	}

	public String getUserId() {
		return userId;
	}

	public String getMessageId() {
		return messageId;
	}

	public Amount getTransactionAmount() {
		return transactionAmount;
	}

	public String getStatus() {
		return status;
	}
	
	public long getServerTime() {
		return serverTime;
	}
}
