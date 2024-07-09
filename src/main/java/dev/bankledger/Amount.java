package dev.bankledger;

public class Amount {
	private String amount;
	private String currency;
	private DebitCredit debitOrCredit;

	public Amount(String amount, String currency, DebitCredit debitOrCredit) {
		this.amount = amount;
		this.currency = currency;
		this.debitOrCredit = debitOrCredit;
	}
	
	public String getAmount() {
		return amount;
	}
	
	public String getCurrency() {
		return currency;
	}
	
	public DebitCredit getDebitOrCredit() {
		return debitOrCredit;
	}
}
