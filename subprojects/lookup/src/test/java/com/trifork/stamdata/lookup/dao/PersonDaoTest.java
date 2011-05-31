package com.trifork.stamdata.lookup.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.classic.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.trifork.stamdata.replication.DatabaseHelper;
import com.trifork.stamdata.util.DateUtils;
import com.trifork.stamdata.views.cpr.Foedselsregistreringsoplysninger;
import com.trifork.stamdata.views.cpr.Folkekirkeoplysninger;
import com.trifork.stamdata.views.cpr.Person;
import com.trifork.stamdata.views.cpr.Statsborgerskab;
import com.trifork.stamdata.views.cpr.Udrejseoplysninger;
import com.trifork.stamdata.views.cpr.UmyndiggoerelseVaergeRelation;

public class PersonDaoTest {
	private static DatabaseHelper db;
	private Session session;
	private PersonDao dao;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		db = new DatabaseHelper("lookup", Person.class, Folkekirkeoplysninger.class, Statsborgerskab.class, Foedselsregistreringsoplysninger.class,
				Udrejseoplysninger.class, UmyndiggoerelseVaergeRelation.class);
		Session session = db.openSession();
		session.createQuery("delete from Person").executeUpdate();
		session.close();
	}

	@Before
	public void before() {
		session = db.openSession();
		session.getTransaction().begin();
		dao = new PersonDao(session);
	}
	
	@After
	public void after() {
		session.getTransaction().rollback();
		session.close();
	}
	
	@Test
	public void givesCurrentPersonDataWithContentsFromUnderlyingView() {
		session.save(person(at(2005, Calendar.JANUARY, 5)));
		
		CurrentPersonData person = dao.get("1020304050");
		assertEquals("1020304050", person.getCprNumber());
		assertEquals(at(2005, Calendar.JANUARY, 5), person.getValidFrom());
	}

	@Test
	public void getsFolkekirkeOplysninger() {
		session.save(folkekirkeoplysninger("M"));
		CurrentPersonData person = dao.get("1020304050");
		assertTrue(person.getMedlemAfFolkekirken());
	}
	
	@Test
	public void getsStatsborgerskab() {
		session.save(statsborgerskab("1234"));
		CurrentPersonData person = dao.get("1020304050");
		assertEquals("1234", person.getStatsborgerskab());
	}
	
	@Test
	public void getsFoedselsregistreringsoplysninger() {
		session.save(foedselsregistreringsoplysninger("1234", "foedselsTekst"));
		CurrentPersonData person = dao.get("1020304050");
		assertEquals("1234", person.getFoedselsregistreringsstedkode());
		assertEquals("foedselsTekst", person.getFoedselsregistreringstekst());
	}
	
	@Test
	public void getsUdrejseoplysninger() {
		session.save(udrejseoplysninger());
		CurrentPersonData person = dao.get("1020304050");
		assertEquals("1234", person.getUdrejseoplysninger().udrejseLandekode);
		assertEquals("line1", person.getUdrejseoplysninger().udlandsadresse1);
	}

	@Test
	public void getsNewestPersonRecordIfNoRecordIsInTheFuture() {
		session.save(person(at(2005, Calendar.JANUARY, 5)));
		session.save(person(at(2010, Calendar.FEBRUARY, 10)));
		
		CurrentPersonData person = dao.get("1020304050");
		assertEquals(at(2010, Calendar.FEBRUARY, 10), person.getValidFrom());
	}
	
	@Test
	public void getsCurrentPersonRecordIfRecordsInTheFuture() {
		session.save(person(at(2005, Calendar.JANUARY, 5)));
		session.save(person(at(2010, Calendar.FEBRUARY, 10)));
		session.save(person(at(2110, Calendar.FEBRUARY, 10)));
		
		CurrentPersonData person = dao.get("1020304050");
		assertEquals(at(2010, Calendar.FEBRUARY, 10), person.getValidFrom());
	}
	
	@Test
	public void getsCurrentUmyndiggoerelseVaergeRelation() {
		session.save(umyndiggoerelseVaergeRelation("1020304050", "5040302010", DateUtils.PAST.getTime(), at(2005, Calendar.JUNE, 15)));
		session.save(umyndiggoerelseVaergeRelation("1020304050", "6050403020", at(2005, Calendar.JUNE, 15), DateUtils.FUTURE.getTime()));
		
		CurrentPersonData person = dao.get("1020304050");
		assertEquals("6050403020", person.getVaerge().relationCpr);
	}
	
	@Test
	public void getsCurrentVaergemaal() {
		session.save(umyndiggoerelseVaergeRelation("7060504030", "1020304050", DateUtils.PAST.getTime(), at(2005, Calendar.JUNE, 15)));
		session.save(umyndiggoerelseVaergeRelation("9080706050", "1020304050", at(2005, Calendar.JUNE, 15), DateUtils.FUTURE.getTime()));
		session.save(umyndiggoerelseVaergeRelation("7060504030", "1020304050", at(2005, Calendar.JUNE, 15), DateUtils.FUTURE.getTime()));
		session.save(umyndiggoerelseVaergeRelation("8070605040", "1020304050", at(2100, Calendar.JUNE, 15), DateUtils.FUTURE.getTime()));
		
		CurrentPersonData person = dao.get("1020304050");
		assertEquals(2, person.getVaergemaal().size());
		Set<String> vaergemaalscpr = new TreeSet<String>();
		for (UmyndiggoerelseVaergeRelation relation : person.getVaergemaal()) {
			vaergemaalscpr.add(relation.getCpr());
		}
		Set<String> expectedVaergemaalscpr = new TreeSet<String>();
		expectedVaergemaalscpr.add("9080706050");
		expectedVaergemaalscpr.add("7060504030");
		assertEquals(expectedVaergemaalscpr, vaergemaalscpr);
	}
	
	@Test
	public void pastAndFutureNotModified() {
		Folkekirkeoplysninger fo = folkekirkeoplysninger("M");
		fo.setValidFrom(DateUtils.PAST.getTime());
		fo.setValidTo(DateUtils.FUTURE.getTime());
		session.save(fo);
		session.flush();
		session.clear();
		Folkekirkeoplysninger retrieved = (Folkekirkeoplysninger) session.get(Folkekirkeoplysninger.class, fo.getRecordID());
		assertNull(retrieved.getValidFrom());
		
		assertNull(retrieved.getValidTo());
	}
	
	private UmyndiggoerelseVaergeRelation umyndiggoerelseVaergeRelation(String cpr, String vaergeCpr, Date validFrom, Date validTo) {
		UmyndiggoerelseVaergeRelation result = new UmyndiggoerelseVaergeRelation();
		result.typeKode = "001";
		result.typeTekst = "Hello world";
		result.id = cpr + "-" + result.typeKode;
		result.setCpr(cpr);
		result.relationCpr = vaergeCpr;
		result.setValidFrom(validFrom);
		result.setValidTo(validTo);
		result.setCreatedBy("AHJ");
		result.setModifiedBy("AHJ");
		result.setCreatedDate(new Date());
		result.setModifiedDate(new Date());
		return result;
	}

	private Person person(Date validFrom) {
		Person result = new Person();
		result.setCpr("1020304050");
		result.koen = "M";
		result.foedselsdato = at(1975, Calendar.MAY, 12);
		result.modifiedBy = "AHJ";
		result.createdBy = "AHJ";
		result.setValidFrom(validFrom);
		result.setModifiedDate(validFrom);
		result.setCreatedDate(at(2005, Calendar.JANUARY, 5));
		return result;
	}
	
	private Statsborgerskab statsborgerskab(String landekode) {
		Statsborgerskab result = new Statsborgerskab();
		result.setCpr("1020304050");
		result.landekode = landekode;
		result.modifiedBy = "AHJ";
		result.createdBy = "AHJ";
		result.setValidFrom(at(2005, Calendar.JANUARY, 5));
		result.setModifiedDate(result.getValidFrom());
		result.setCreatedDate(result.getValidFrom());
		result.statsborgerskabstartdatoUsikkerhedsmarkering = "";
		return result;
	}
	
	private Folkekirkeoplysninger folkekirkeoplysninger(String kode) {
		Folkekirkeoplysninger result = new Folkekirkeoplysninger();
		result.setCpr( "1020304050");
		result.forholdsKode = kode;
		result.modifiedBy = "AHJ";
		result.createdBy = "AHJ";
		result.setValidFrom(at(2005, Calendar.JANUARY, 5));
		result.setModifiedDate(result.getValidFrom());
		result.setCreatedDate(at(2005, Calendar.JANUARY, 5));
		return result;
	}
	
	private Foedselsregistreringsoplysninger foedselsregistreringsoplysninger(String kode, String tekst) {
		Foedselsregistreringsoplysninger result = new Foedselsregistreringsoplysninger();
		result.setCpr("1020304050");
		result.foedselsregistreringsstedkode = kode;
		result.foedselsregistreringstekst = tekst;
		result.modifiedBy = "AHJ";
		result.createdBy = "AHJ";
		result.setValidFrom(at(2005, Calendar.JANUARY, 5));
		result.setModifiedDate(result.getValidFrom());
		result.setCreatedDate(at(2005, Calendar.JANUARY, 5));
		return result;
	}
	
	private Udrejseoplysninger udrejseoplysninger() {
		Udrejseoplysninger result = new Udrejseoplysninger();
		result.setCpr("1020304050");
		result.udrejseLandekode = "1234";
		result.udrejsedatoUsikkerhedsmarkering = "";
		result.udrejsedato = new Date();
		result.udlandsadresse1 = "line1";
		result.udlandsadresse2 = "line2";
		result.udlandsadresse3 = "line3";
		result.udlandsadresse4 = "line4";
		result.udlandsadresse5 = "line5";
		result.modifiedBy = "AHJ";
		result.createdBy = "AHJ";
		result.setValidFrom(at(2005, Calendar.JANUARY, 5));
		result.setModifiedDate(result.getValidFrom());
		result.setCreatedDate(result.getValidFrom());
		return result;
	}

	private Date at(int year, int month, int day) {
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.set(Calendar.YEAR, year);
		calendar.set(Calendar.MONTH, month);
		calendar.set(Calendar.DAY_OF_MONTH, day);
		return calendar.getTime();
	}
}
