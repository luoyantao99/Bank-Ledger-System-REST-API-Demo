package dev.bankledger;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runners.MethodSorters;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)

public class BankLedgerAppTests {

	@Mock
	private Context ctx;
	@Mock
	private EventStore eventStore;
	@Mock
	private Javalin app;

	@Before
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		BankLedgerApp.defineRoutes(app);
	}

	@Test
	@DisplayName("Test loading funds with a valid request")
	public void test1_load_funds_valid_request() {
		when(ctx.queryParam("userId")).thenReturn("user1");
		when(ctx.queryParam("amount")).thenReturn("100.00");
		BankLedgerApp.loadHandler(ctx);

		ArgumentCaptor<LoadResponse> captor = ArgumentCaptor.forClass(LoadResponse.class);
		verify(ctx).json(captor.capture());
		LoadResponse actualResponse = captor.getValue();

		assertEquals("user1", actualResponse.getUserId());
		assertEquals("100.00", actualResponse.getBalance().getAmount());
		assertEquals("USD", actualResponse.getBalance().getCurrency());
		assertEquals(DebitCredit.CREDIT, actualResponse.getBalance().getDebitOrCredit());
	}

	@Test
	@DisplayName("Test loading funds with an invalid user ID returns an error")
	public void test2_load_funds_invalid_userID() {
		when(ctx.queryParam("userId")).thenReturn("");
		when(ctx.queryParam("amount")).thenReturn("100.00");
		when(ctx.status(anyInt())).thenReturn(ctx);
		when(ctx.json(any())).thenReturn(ctx);
		BankLedgerApp.loadHandler(ctx);

		verify(ctx).status(400);

		ArgumentCaptor<Error> errorCaptor = ArgumentCaptor.forClass(Error.class);
		verify(ctx).json(errorCaptor.capture());

		Error capturedError = errorCaptor.getValue();

		assertEquals("User ID cannot be empty", capturedError.getMessage());
		assertEquals("400", capturedError.getCode());
	}

	@Test
	@DisplayName("Test loading funds with an invalid amount format returns an error")
	public void test3_load_funds_invalid_amount() {
		when(ctx.queryParam("userId")).thenReturn("user1");
		when(ctx.queryParam("amount")).thenReturn("abc.0");
		when(ctx.status(anyInt())).thenReturn(ctx);
		when(ctx.json(any())).thenReturn(ctx);
		BankLedgerApp.loadHandler(ctx);

		verify(ctx).status(400);

		ArgumentCaptor<Error> errorCaptor = ArgumentCaptor.forClass(Error.class);
		verify(ctx).json(errorCaptor.capture());

		Error capturedError = errorCaptor.getValue();

		assertEquals("Amount must be a non-negative number with up to two decimal places", capturedError.getMessage());
		assertEquals("400", capturedError.getCode());
	}

	@Test
	@DisplayName("Test authorizing funds with a valid request")
	public void test4_authorize_funds_valid_request() {
		when(ctx.queryParam("userId")).thenReturn("user1");
		when(ctx.queryParam("amount")).thenReturn("75.00");
		BankLedgerApp.authorizationHandler(ctx);

		ArgumentCaptor<AuthorizationResponse> captor = ArgumentCaptor.forClass(AuthorizationResponse.class);
		verify(ctx).json(captor.capture());
		AuthorizationResponse actualResponse = captor.getValue();

		assertEquals("user1", actualResponse.getUserId());
		assertEquals("APPROVED", actualResponse.getResponseCode());
		assertEquals("25.00", actualResponse.getBalance().getAmount());
		assertEquals("USD", actualResponse.getBalance().getCurrency());
		assertEquals(DebitCredit.DEBIT, actualResponse.getBalance().getDebitOrCredit());
	}
	
	@Test
	@DisplayName("Test authorizing funds with an invalid user ID returns an error")
	public void test5_authorize_funds_invalid_userID() {
		when(ctx.queryParam("userId")).thenReturn("");
		when(ctx.queryParam("amount")).thenReturn("75.00");
		when(ctx.status(anyInt())).thenReturn(ctx);
		when(ctx.json(any())).thenReturn(ctx);
		BankLedgerApp.authorizationHandler(ctx);

		verify(ctx).status(400);

		ArgumentCaptor<Error> errorCaptor = ArgumentCaptor.forClass(Error.class);
		verify(ctx).json(errorCaptor.capture());

		Error capturedError = errorCaptor.getValue();

		assertEquals("User ID cannot be empty", capturedError.getMessage());
		assertEquals("400", capturedError.getCode());
	}
	
	@Test
	@DisplayName("Test authorizing funds with an invalid amount format returns an error")
	public void test6_authorize_funds_invalid_amount() {
		when(ctx.queryParam("userId")).thenReturn("user1");
		when(ctx.queryParam("amount")).thenReturn("x75.00");
		when(ctx.status(anyInt())).thenReturn(ctx);
		when(ctx.json(any())).thenReturn(ctx);
		BankLedgerApp.authorizationHandler(ctx);

		verify(ctx).status(400);

		ArgumentCaptor<Error> errorCaptor = ArgumentCaptor.forClass(Error.class);
		verify(ctx).json(errorCaptor.capture());

		Error capturedError = errorCaptor.getValue();

		assertEquals("Amount must be a non-negative number with up to two decimal places", capturedError.getMessage());
		assertEquals("400", capturedError.getCode());
	}
	
	@Test
	@DisplayName("Test authorizing funds when insufficient returns a denied response")
	public void test7_authorize_funds_denied() {
		when(ctx.queryParam("userId")).thenReturn("user1");
		when(ctx.queryParam("amount")).thenReturn("75.00");
		BankLedgerApp.authorizationHandler(ctx);

		ArgumentCaptor<AuthorizationResponse> captor = ArgumentCaptor.forClass(AuthorizationResponse.class);
		verify(ctx).json(captor.capture());
		AuthorizationResponse actualResponse = captor.getValue();

		assertEquals("user1", actualResponse.getUserId());
		assertEquals("DENIED", actualResponse.getResponseCode());
		assertEquals("25.00", actualResponse.getBalance().getAmount());
		assertEquals("USD", actualResponse.getBalance().getCurrency());
		assertEquals(DebitCredit.DEBIT, actualResponse.getBalance().getDebitOrCredit());
	}
	
	@Test
	@DisplayName("Test checking balance returns the correct current balance")
	public void test8_check_balance() {
		when(ctx.pathParam("userId")).thenReturn("user1");
		BankLedgerApp.balanceHandler(ctx);

		ArgumentCaptor<BalanceResponse> captor = ArgumentCaptor.forClass(BalanceResponse.class);
		verify(ctx).json(captor.capture());
		BalanceResponse actualResponse = captor.getValue();

		assertEquals("user1", actualResponse.getUserId());
		assertEquals("25.00", actualResponse.getBalance());
		assertEquals("USD", actualResponse.getCurrency());
	}
}
