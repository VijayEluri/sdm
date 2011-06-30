
// The contents of this file are subject to the Mozilla Public
// License Version 1.1 (the "License"); you may not use this file
// except in compliance with the License. You may obtain a copy of
// the License at http://www.mozilla.org/MPL/
// 
// Software distributed under the License is distributed on an "AS
// IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
// implied. See the License for the specific language governing
// rights and limitations under the License.
// 
// Contributor(s): Contributors are attributed in the source code
// where applicable.
// 
// The Original Code is "Stamdata".
// 
// The Initial Developer of the Original Code is Trifork Public A/S.
// 
// Portions created for the Original Code are Copyright 2011,
// Lægemiddelstyrelsen. All Rights Reserved.
// 
// Portions created for the FMKi Project are Copyright 2011,
// National Board of e-Health (NSI). All Rights Reserved.

package com.trifork.stamdata.replication.webservice;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.ScrollableResults;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.inject.Provider;
import com.trifork.stamdata.replication.mocks.MockEntity;
import com.trifork.stamdata.replication.security.SecurityManager;
import com.trifork.stamdata.views.View;

@RunWith(MockitoJUnitRunner.class)
public class RegistryServletTest {

	private RegistryServlet servlet;

	private HttpServletRequest request;
	private HttpServletResponse response;

	private @Mock SecurityManager securityManager;
	private Map<String, Class<? extends View>> registry;
	private Map<String, Class<? extends View>> mappedClasses;
	private @Mock RecordDao recordDao;
	private @Mock AtomFeedWriter writer;
	private String requestPath;
	private int count;
	private String countParam;
	private String clientId = "CVR:12345678";
	private ScrollableResults records;
	private String acceptHeader;
	private boolean authorized;
	private String offsetParam;
	private String nextOffset;

	@Before
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void setUp() throws Exception {

		registry = new HashMap<String, Class<? extends View>>();

		Provider securityManagerProvider = mock(Provider.class);
		when(securityManagerProvider.get()).thenReturn(securityManager);

		Provider recordDaoProvider = mock(Provider.class);
		when(recordDaoProvider.get()).thenReturn(recordDao);

		Provider writerProvider = mock(Provider.class);
		when(writerProvider.get()).thenReturn(writer);

		servlet = new RegistryServlet(registry, securityManagerProvider, recordDaoProvider, writerProvider);

		setUpValidRequest();
	}

	@Test
	public void Should_accept_valid_request() throws Exception {

		get();

		verify(response).setStatus(200);
		verify(writer).write(eq(MockEntity.class), eq(records), any(OutputStream.class), eq(false));
	}
	
	@Test
	public void Should_give_no_link_if_there_are_no_updates() throws Exception {

		when(records.last()).thenReturn(false);
		
		get();
		
		verify(response).setStatus(200);
		verify(response, never()).setHeader(Matchers.eq("Link"), Matchers.anyString());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void Should_deny_access_if_the_client_is_not_authorized_for_the_requested_view() throws Exception {

		authorized = false;
		get();
		verify(response).setStatus(401);
		verify(writer, never()).write(any(Class.class), any(ScrollableResults.class), any(OutputStream.class), Mockito.anyBoolean());
	}

	@Test
	public void Should_not_return_a_web_link_if_there_are_no_more_records() throws Exception {
		records = mock(ScrollableResults.class);

		get();

		verify(response, never()).setHeader(eq("Link"), anyString());
	}

	@Test
	public void Should_return_fast_infoset_if_the_user_requests_it() throws Exception {

		acceptHeader = "application/atom+fastinfoset";

		get();

		verify(response).setContentType("application/atom+fastinfoset; charset=utf-8");
		verify(writer).write(eq(MockEntity.class), eq(records), any(OutputStream.class), eq(true));
	}

	@Test
	public void Should_return_xml_if_the_user_requests_it() throws Exception {

		acceptHeader = "application/atom+xml";
		
		get();
		
		verify(response).setContentType("application/atom+xml; charset=utf-8");
		verify(writer).write(eq(MockEntity.class), eq(records), any(OutputStream.class), eq(false));
	}

	// TODO: Make test for unaccepted content type.

	@Test
	public void Should_return_records_from_the_correct_offset() throws Exception {

		get();
		
		verify(recordDao).findPage(MockEntity.class, "2222222222", new Date(1111111111000L), 2);
	}
	
	@Test
	public void Should_return_error_when_count_param_is_invalid() throws Exception {
		
		countParam = "12A232P";
		
		get();
		
		verify(response).sendError(eq(HTTP_BAD_REQUEST), anyString());
	}
	
	@Test
	public void Should_return_error_when_count_param_is_0() throws Exception {
		
		countParam = "0";
		
		get();
		
		verify(response).sendError(eq(HTTP_BAD_REQUEST), anyString());
	}
	
	@Test
	public void Should_use_default_count_when_count_param_is_unspecified() throws Exception {
		countParam = null;
		count = RegistryServlet.DEFAULT_PAGE_SIZE;
		
		get();
		
		verify(recordDao).findPage(MockEntity.class, "2222222222", new Date(1111111111000L), RegistryServlet.DEFAULT_PAGE_SIZE);
	}
	
	@Test
	public void Should_return_error_when_offset_param_is_invalid() throws Exception {
		
		countParam = "A2312D21";
		
		get();
		
		verify(response).sendError(eq(HTTP_BAD_REQUEST), anyString());
	}

	// HELPER METHODS

	public void get() throws Exception {

		when(securityManager.isAuthorized(request)).thenReturn(authorized);
		when(securityManager.getClientId(request)).thenReturn(clientId);
		
		when(recordDao.findPage(MockEntity.class, "2222222222", new Date(1111111111000L), count)).thenReturn(records);
		when(request.getPathInfo()).thenReturn(requestPath);
		when(request.getHeader("Accept")).thenReturn(acceptHeader);
		when(request.getParameter("offset")).thenReturn(offsetParam);
		when(request.getParameter("count")).thenReturn(countParam);
		registry.putAll(mappedClasses);

		servlet.doGet(request, response);
	}

	public void setUpValidRequest() throws Exception {

		request = mock(HttpServletRequest.class);
		response = mock(HttpServletResponse.class);
		requestPath = "/foo/bar/v1";
		
		authorized = true;
		
		mappedClasses = new HashMap<String, Class<? extends View>>();
		mappedClasses.put("foo/bar/v1", MockEntity.class);

		count = 2;
		countParam = "2";
		offsetParam = "11111111112222222222";
		nextOffset = "11111111113333333333";

		records = mock(ScrollableResults.class);
		when(records.next()).thenReturn(true,false);
		
		MockEntity lastRecord = mock(MockEntity.class);
		when(records.get(0)).thenReturn(lastRecord);
		when(records.last()).thenReturn(true);
		when(lastRecord.getOffset()).thenReturn(nextOffset);
		
		ServletOutputStream outputStream = mock(ServletOutputStream.class);
		when(response.getOutputStream()).thenReturn(outputStream);

		acceptHeader = "application/atom+xml";
	}
}