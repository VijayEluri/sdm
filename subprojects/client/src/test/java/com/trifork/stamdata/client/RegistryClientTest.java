package com.trifork.stamdata.client;

import java.util.Iterator;

import org.apache.commons.lang.time.StopWatch;
import org.junit.Before;
import org.junit.Test;

import com.trifork.stamdata.replication.replication.views.cpr.Person;

public class RegistryClientTest {

	private RegistryClient client;

	@Before
	public void setUp() {
		client = new RegistryClient("http://localhost:8080/replication/stamdata/");
	}

	@Test
	public void should_extract_complete_dataset() throws Exception {

		Iterator<EntityRevision<Person>> revisions = client.update(Person.class, null, 5000);

		int recordCount = 0;

		StopWatch timer = new StopWatch();
		timer.start();

		while (revisions.hasNext()) {
			recordCount++;
			EntityRevision<Person> revision = revisions.next();
			printRevision(revision);
		}

		timer.stop();

		printStatistics(recordCount, timer);
	}

	@Test
	public void should_extract_delta_dataset() throws Exception {

		Iterator<EntityRevision<Person>> revisions = client.update(Person.class, "13002373210000092000225", 5000);

		int recordCount = 0;

		StopWatch timer = new StopWatch();
		timer.start();

		while (revisions.hasNext()) {
			recordCount++;
			EntityRevision<Person> revision = revisions.next();
			printRevision(revision);
		}

		timer.stop();

		printStatistics(recordCount, timer);
	}

	protected void printRevision(EntityRevision<?> revision) {
		System.out.println(revision.getId() + ": " + revision.getEntity());
	}

	protected void printStatistics(int i, StopWatch timer) {
		System.out.println();
		System.out.println("Time used: " + timer.getTime() / 1000. + " sec.");
		System.out.println("Record count: " + i);
	}
}