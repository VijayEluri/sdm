package com.trifork.stamdata.replication.replication;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.ScrollableResults;
import org.hibernate.StatelessSession;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.trifork.stamdata.replication.DatabaseHelper;
import com.trifork.stamdata.replication.replication.views.View;
import com.trifork.stamdata.replication.replication.views.cpr.Folkekirkeoplysninger;
import com.trifork.stamdata.replication.replication.views.usagelog.UsageLogEntry;

public class RecordDaoTest {
	private static StatelessSession session;
	private long nextTick = new Date().getTime();
	private RecordDao dao;

	@BeforeClass
	public static void init() throws Exception {
		DatabaseHelper db = new DatabaseHelper(Folkekirkeoplysninger.class, UsageLogEntry.class);
		session = db.openStatelessSession();
		session.createQuery("delete from Folkekirkeoplysninger").executeUpdate();
	}

	@Before
	public void setUp() {
		dao = new RecordDao(session);
		session.beginTransaction();
	}

	@After
	public void tearDown() {
		session.getTransaction().rollback();
	}

	@Test
	public void findsAllRecordsIfWithinLimit() {
		createFolkekirkeoplysninger();
		createFolkekirkeoplysninger();
		createFolkekirkeoplysninger();
		createFolkekirkeoplysninger();
		
		List<Folkekirkeoplysninger> result = findPage(Folkekirkeoplysninger.class, "0", yesterday(), null, 5);
		
		assertEquals(4, result.size());
	}
	
	@Test
	public void cutsOffAtGivenLimit() {
		for (int i=0; i<10; i++) {
			createFolkekirkeoplysninger();
		}
		
		List<Folkekirkeoplysninger> result = findPage(Folkekirkeoplysninger.class, "0", yesterday(), null, 5);
		
		assertEquals(5, result.size());
	}

	@Test
	public void canFilterForSpecificClientId() {
		createUsageLogEntry("client1");
		createUsageLogEntry("client4");
		createUsageLogEntry("client3");
		createUsageLogEntry("client1");
		
		List<UsageLogEntry> result = findPage(UsageLogEntry.class, "0", yesterday(), "client1", 5);
		
		assertEquals(2, result.size());
		assertEquals("client1", result.get(0).clientId);
		assertEquals("client1", result.get(1).clientId);
	}

	@SuppressWarnings("unchecked")
	public <T extends View> List<T> findPage(Class<T> type, String recordId, Date modifiedDate, String clientId, int limit) {
		ScrollableResults scrollableResults = dao.findPage(type, recordId, modifiedDate, clientId, limit);
		List<T> result = new ArrayList<T>();
		while (!scrollableResults.isLast()) {
			scrollableResults.next();
			result.add((T) scrollableResults.get(0));
		}
		return result;
	}

	private void createFolkekirkeoplysninger() {
		Folkekirkeoplysninger folkekirkeoplysninger = new Folkekirkeoplysninger();
		folkekirkeoplysninger.cpr = "1234567890";
		folkekirkeoplysninger.modifiedDate = nextDate();
		folkekirkeoplysninger.modifiedBy = "";
		folkekirkeoplysninger.createdDate = nextDate();
		folkekirkeoplysninger.createdBy = "";
		folkekirkeoplysninger.validFrom = nextDate();
		folkekirkeoplysninger.forholdsKode = "A";
		session.insert(folkekirkeoplysninger);
	}

	private void createUsageLogEntry(String clientId) {
		UsageLogEntry entry = new UsageLogEntry();
		entry.clientId = clientId;
		entry.modifiedDate = nextDate();
		entry.type = "/foo/bar/v1";
		entry.amount = 20;
		session.insert(entry);
	}
	
	private Date yesterday() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, -1);
		return calendar.getTime();
	}
	
	private Date nextDate() {
		nextTick += 1000;
		return new Date(nextTick);
	}
}
