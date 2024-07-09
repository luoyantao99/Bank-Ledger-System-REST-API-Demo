package dev.bankledger;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

public class BankLedgerApp {
	private static final EventStore eventStore = new EventStore();
	private static final ConcurrentHashMap<String, Balance> balances = new ConcurrentHashMap<>();

	/**
	 * Main method to set up and start the server.
	 */
	public static void main(String[] args) {
		Javalin app = Javalin.create(config -> config.http.defaultContentType = "application/json").start(7000);
		defineRoutes(app);
	}
	
	/**
	 * Method to define all server routes for the application.
	 */
	public static void defineRoutes(Javalin app) {
		app.get("/ping", BankLedgerApp::pingHandler);
		app.put("/load", BankLedgerApp::loadHandler);
		app.put("/authorization", BankLedgerApp::authorizationHandler);
		app.get("/balance/{userId}", BankLedgerApp::balanceHandler);
		app.get("/verify/{userId}", BankLedgerApp::verifyHandler);
	}

	/**
	 * Handler for responding to ping requests.
	 */
	private static void pingHandler(Context ctx) {
		try {
			ctx.json(new Ping(System.currentTimeMillis()));
		} catch (Exception e) {
			ctx.status(500).json(new Error("An unexpected error occurred", "500"));
		}
	}
    
    /**
     * Handler for loading funds into a user's account.
     */
    public static void loadHandler(Context ctx) {
        try {
            String userId = ctx.queryParam("userId");
            String amount = ctx.queryParam("amount");

            if (userId == null || userId.trim().isEmpty()) {
            	ctx.status(400).json(new Error("User ID cannot be empty", "400"));
            	return;
            }

            if (amount == null || !amount.matches("\\d+(\\.\\d{1,2})?")) {
            	ctx.status(400).json(new Error("Amount must be a non-negative number with up to two decimal places", "400"));
            	return;
            }

            BigDecimal amountDecimal = new BigDecimal(amount);
            balances.computeIfAbsent(userId, k -> new Balance());
            Balance b = balances.get(userId);
            b.add(amountDecimal);
            
            Transaction transaction = new Transaction(
                userId, 
                new Amount(amount, "USD", DebitCredit.CREDIT),
                "APPROVED"
            );
            eventStore.addTransaction(transaction);

            LoadResponse lr = new LoadResponse(userId, transaction.getMessageId(), new Amount(b.getBalance().toString(), "USD", DebitCredit.CREDIT));
            System.out.println(lr);
            ctx.json(lr);
        }
        catch (Exception e) {
            ctx.status(500).json(new Error("An unexpected error occurred", "500"));
        }
    }
    
    /**
     * Handler for authorizing transactions from a user's account.
     */
    public static void authorizationHandler(Context ctx) {
    	try {
    		String userId = ctx.queryParam("userId");
    		String amount = ctx.queryParam("amount");

    		if (userId == null || userId.trim().isEmpty()) {
    			ctx.status(400).json(new Error("User ID cannot be empty", "400"));
    			return;
    		}

    		if (amount == null || !amount.matches("\\d+(\\.\\d{1,2})?")) {
    			ctx.status(400).json(new Error("Amount must be a non-negative number with up to two decimal places", "400"));
    			return;
    		}

    		BigDecimal transactionAmount = new BigDecimal(amount);
            balances.computeIfAbsent(userId, k -> new Balance());

            Balance balance = balances.get(userId);
            BigDecimal currentBalance = balance.getBalance();
            BigDecimal newBalance = balance.subtract(transactionAmount);
            
            if (newBalance.compareTo(currentBalance) < 0) {
                Transaction transaction = new Transaction(
                    userId, 
                    new Amount(amount, "USD", DebitCredit.DEBIT),
                    "APPROVED"
                );
                eventStore.addTransaction(transaction);
                
                AuthorizationResponse ar = new AuthorizationResponse(userId, transaction.getMessageId(), "APPROVED", 
                		new Amount(newBalance.toString(), "USD", DebitCredit.DEBIT));
                System.out.println(ar);
                ctx.json(ar);
            }
            else {
                Transaction transaction = new Transaction(
                    userId, 
                    new Amount(amount, "USD", DebitCredit.DEBIT),
                    "DENIED"
                );
                eventStore.addTransaction(transaction);

                AuthorizationResponse ar = new AuthorizationResponse(userId, transaction.getMessageId(), "DENIED", 
                		new Amount(currentBalance.toString(), "USD", DebitCredit.DEBIT));
                System.out.println(ar);
                ctx.json(ar);
            }
        }
        catch (Exception e) {
            ctx.status(500).json(new Error("An unexpected error occurred", "500"));
        }
    }
    
    /**
     * Handler for retrieving the current balance of a user's account.
     */
    public static void balanceHandler(Context ctx) {
    	try {
    		String userId = ctx.pathParam("userId");

    		if (userId == null || userId.trim().isEmpty()) {
    			ctx.status(400).json(new Error("User ID cannot be empty", "400"));
    			return;
    		}

    		balances.computeIfAbsent(userId, k -> new Balance());
    		BigDecimal currentBalance = balances.get(userId).getBalance();
    		String currency = balances.get(userId).getCurrency();
    		
    		BalanceResponse response = new BalanceResponse(userId, currentBalance.toString(), currency);
    		System.out.println("CHECK BALANCE: USER " + userId + ", BALANCE = " + currentBalance);
    		ctx.json(response);
    	}
    	catch (Exception e) {
    		ctx.status(500).json(new Error("An unexpected error occurred", "500"));
    	}
    }
    
    /**
     * Handler for verifying the consistency between the recorded balance and the event-log-reconstructed balance.
     */
    public static void verifyHandler(Context ctx) {
    	try {
    		String userId = ctx.pathParam("userId");

    		if (userId == null || userId.trim().isEmpty()) {
    			ctx.status(400).json(new Error("User ID cannot be empty", "400"));
    			return;
    		}

    		balances.computeIfAbsent(userId, k -> new Balance());
    		BigDecimal currentBalance = balances.get(userId).getBalance();
    		BigDecimal logBalance = eventStore.computeBalance(userId);
    		
    		if (currentBalance.compareTo(logBalance) == 0) {
                ctx.json(new VerificationResponse(userId, currentBalance, "Balances match"));
            }
    		else {
                ctx.status(409).json(new VerificationResponse(userId, currentBalance, "Balance discrepancy detected", logBalance));
            }
    	}
    	catch (Exception e) {
    		ctx.status(500).json(new Error("An unexpected error occurred", "500"));
    	}
    }
}


class Ping {
	private long serverTime;

	public Ping(long serverTime) {
		this.serverTime = serverTime;
	}

	public long getServerTime() {
		return serverTime;
	}
}

class LoadResponse {
	private String userId;
	private String messageId;
	private Amount balance;

	public LoadResponse(String userId, String messageId, Amount balance) {
		this.userId = userId;
		this.messageId = messageId;
		this.balance = balance;
	}

	public String getUserId() {
		return userId;
	}

	public String getMessageId() {
		return messageId;
	}

	public Amount getBalance() {
		return balance;
	}

	@Override
	public String toString() {
		return "LOAD: USER " + userId + ", BALANCE = " + balance.getAmount();
	}
}

class AuthorizationResponse {
	private String userId;
	private String messageId;
	private String responseCode;
	private Amount balance;

	public AuthorizationResponse(String userId, String messageId, String responseCode, Amount balance) {
		this.userId = userId;
		this.messageId = messageId;
		this.responseCode = responseCode;
		this.balance = balance;
	}

	public String getUserId() {
		return userId;
	}

	public String getMessageId() {
		return messageId;
	}

	public String getResponseCode() {
		return responseCode;
	}

	public Amount getBalance() {
		return balance;
	}

	@Override
	public String toString() {
		return "AUTHORIZATION " + responseCode + ": USER " + userId + ", BALANCE = " + balance.getAmount();
	}
}

class BalanceResponse {
	private String userId;
	private String balance;
	private String currency;

	public BalanceResponse(String userId, String balance, String currency) {
		this.userId = userId;
		this.balance = balance;
		this.currency = currency;
	}

	public String getUserId() {
		return userId;
	}

	public String getBalance() {
		return balance;
	}

	public String getCurrency() {
		return currency;
	}
}

class VerificationResponse {
	private String userId;
	private BigDecimal currentBalance;
	private String message;
	private BigDecimal logBalance;

	public VerificationResponse(String userId, BigDecimal currentBalance, String message) {
		this.userId = userId;
		this.currentBalance = currentBalance;
		this.message = message;
		this.logBalance = null;
	}

	public VerificationResponse(String userId, BigDecimal currentBalance, String message, BigDecimal logBalance) {
		this.userId = userId;
		this.currentBalance = currentBalance;
		this.message = message;
		this.logBalance = logBalance;
	}

	public String getUserId() {
		return userId;
	}

	public BigDecimal getCurrentBalance() {
		return currentBalance;
	}

	public String getMessage() {
		return message;
	}

	public BigDecimal getLogBalance() {
		return logBalance;
	}
}

class Error {
	private String message;
	private String code;

	public Error(String message, String code) {
		this.message = message;
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public String getCode() {
		return code;
	}
}
