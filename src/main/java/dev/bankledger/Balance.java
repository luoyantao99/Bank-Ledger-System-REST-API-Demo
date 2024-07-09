package dev.bankledger;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import io.javalin.http.BadRequestResponse;

public class Balance {
	private BigDecimal balance;
	private String currency;
	private DebitCredit debitOrCredit;
	public final ReentrantLock lock = new ReentrantLock();

	public Balance() {
		this.balance = new BigDecimal("0");
		this.currency = "USD";
		this.debitOrCredit = DebitCredit.DEBIT;
	}

	public Balance(BigDecimal balance, String currency, DebitCredit debitOrCredit) {
		this.balance = balance;
		this.currency = currency;
		this.debitOrCredit = debitOrCredit;
	}

	public BigDecimal add(BigDecimal a) throws Exception
	{
		boolean lockAcquired = false;
		try {
			lockAcquired = lock.tryLock(100, TimeUnit.MILLISECONDS);
			if (lockAcquired) {
				balance = balance.add(a);
			}
			else {
				throw new BadRequestResponse("Server is busy. Please retry.");
			}
		}
		finally {
			if (lockAcquired) {
				lock.unlock();
			}
		}       
		return balance;
	}

	public BigDecimal subtract(BigDecimal s) throws Exception
	{
		boolean lockAcquired = false;
		try {
			lockAcquired = lock.tryLock(100, TimeUnit.MILLISECONDS);
			if (lockAcquired) {
				if (balance.compareTo(s) >= 0) {
					balance = balance.subtract(s);
				}
			}
			else {
				throw new BadRequestResponse("Server is busy. Please retry.");
			}
		}
		finally {
			if (lockAcquired) {
				lock.unlock();
			}
		}       
		return balance;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public String getCurrency() {
		return currency;
	}

	public DebitCredit getDebitOrCredit() {
		return debitOrCredit;
	}	
}
