package com.trifork.sdm.replication.saml;

import static com.trifork.sdm.replication.admin.models.RequestAttributes.*;
import static org.mockito.Mockito.*;

import javax.servlet.FilterChain;
import javax.servlet.http.*;

import org.junit.*;
import org.mockito.Matchers;

import com.google.inject.*;
import com.trifork.rid2cpr.RID2CPRFacade;
import com.trifork.sdm.replication.GuiceTest;
import com.trifork.sdm.replication.admin.models.IUserRepository;
import com.trifork.sdm.replication.admin.security.SamlFilter;

import dk.itst.oiosaml.sp.UserAssertion;


public class SamlFilterTest extends GuiceTest
{
	private static Injector injector;

	private IUserRepository userRepository;

	private String userRID;
	private String userCPR;

	private SamlFilter filter;
	private FilterChain filterChain;

	private HttpServletRequest request;
	private HttpServletResponse response;

	private RID2CPRFacade ridService;
	private Provider<UserAssertion> userAssertionProvider;


	@Override
	protected void configure()
	{
		bind(IUserRepository.class).toInstance(mock(IUserRepository.class));
		bind(RID2CPRFacade.class).toInstance(mock(RID2CPRFacade.class));
	}


	@Before
	public void setUp()
	{
		filter = injector.getInstance(SamlFilter.class);

		request = mock(HttpServletRequest.class);
		response = mock(HttpServletResponse.class);
		filterChain = mock(FilterChain.class);

		// TODO: There is not much use in injecting an instance if
		// we do not reset its behavior after each run. It does not
		// break the test, but it could in the future.

		ridService = injector.getInstance(RID2CPRFacade.class);
		userRepository = injector.getInstance(IUserRepository.class);
		userAssertionProvider = injector.getProvider(UserAssertion.class);
	}


	@Test
	public void should_continue_if_the_user_is_authorized() throws Exception
	{
		allowAccess();

		doFilter();

		assertAccessAllowed();
	}


	@Test
	public void should_not_continue_if_the_user_is_not_authorized() throws Exception
	{
		denyAccess();

		doFilter();

		assertAccessDenied();
	}


	//
	// Assertions
	//

	protected void assertAccessDenied() throws Exception
	{
		verify(request, times(0)).setAttribute(USER_CPR, Matchers.anyString());
		verify(filterChain, times(0)).doFilter(request, response);
	}


	protected void assertAccessAllowed() throws Exception
	{
		verify(request, times(1)).setAttribute(USER_CPR, userCPR);
		verify(filterChain, times(1)).doFilter(request, response);
	}


	//
	// Helper Methods
	//

	protected void doFilter() throws Exception
	{
		filter.doFilter(request, response, filterChain);
	}


	protected void denyAccess() throws Exception
	{
		when(userRepository.isAdmin(Matchers.anyString(), Matchers.anyString())).thenReturn(false);
	}


	protected void allowAccess() throws Exception
	{
		when(userRepository.isAdmin(Matchers.anyString(), Matchers.anyString())).thenReturn(true);
	}


	//
	// Test Providers
	//

	@Provides
	public UserAssertion provideUserAssertion() throws Exception
	{
		// Make the user assertion holder testable,
		// you can see the provider at the bottom of this class.

		UserAssertion userAssertion = mock(UserAssertion.class);

		when(userAssertion.getRIDNumber()).thenReturn(userRID);
		when(userAssertionProvider.get()).thenReturn(userAssertion);
		when(ridService.getCPR(userRID)).thenReturn(userCPR);

		return userAssertion;
	}
}
