package dk.nsi.stamdata.cpr.pvit;

import com.google.inject.Inject;
import com.trifork.stamdata.Fetcher;
import com.trifork.stamdata.models.cpr.Person;
import dk.nsi.stamdata.cpr.DgwsHeadersUtils;
import dk.nsi.stamdata.cpr.SoapFaultUtil;
import dk.nsi.stamdata.cpr.WhitelistProvider.Whitelist;
import dk.nsi.stamdata.cpr.jaxws.GuiceInstanceResolver.GuiceWebservice;
import dk.nsi.stamdata.cpr.medcom.FaultMessages;
import dk.nsi.stamdata.cpr.medcom.PersonMapper;
import dk.nsi.stamdata.cpr.ws.*;
import dk.sosi.seal.model.SystemIDCard;
import dk.sosi.seal.model.constants.FaultCodeValues;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jws.WebService;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.Holder;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@GuiceWebservice
@WebService(serviceName = "StamdataPersonLookup", endpointInterface = "dk.nsi.stamdata.cpr.ws.StamdataPersonLookup")
public class StamdataPersonLookupImpl implements StamdataPersonLookup {
    private final static Logger logger = LoggerFactory.getLogger(StamdataPersonLookupImpl.class);

    private final static boolean CHECK_WHITELIST_SERVICE_TYPE = true; //TODO: Replace this field with the enum to use when calling PersonMapper.map(person, [ENUM])

    private final Set<String> whitelist;
    private final SystemIDCard idCard;
    private PersonMapper personMapper;
    private Fetcher fetcher;


    @Inject
    StamdataPersonLookupImpl(@Whitelist Set<String> whitelist, SystemIDCard idCard, Fetcher fetcher, PersonMapper personMapper) {
        this.whitelist = whitelist;
        this.idCard = idCard;
        this.personMapper = personMapper;
        this.fetcher = fetcher;
    }

    @Override
    public PersonLookupResponseType getPersonDetails(Holder<Security> wsseHeader, Holder<Header> medcomHeader,
                                                     PersonLookupRequestType request) throws DGWSFault {

        verifyExactlyOneQueryParameterIsNonNull(wsseHeader, medcomHeader, request);
        // TODO: This should be done in the filter
        // This has to be done according to the DGWS specifications
        DgwsHeadersUtils.setHeadersToOutgoing(wsseHeader, medcomHeader);

        try {
            if (request.getCivilRegistrationNumberPersonQuery() != null) {
                return answerCprRequest(request.getCivilRegistrationNumberPersonQuery());
            }
            if (request.getCivilRegistrationNumberListPersonQuery() != null) {
                return answerCivilRegistrationNumberListPersonRequest(request.getCivilRegistrationNumberListPersonQuery());
            }
            if (request.getBirthDatePersonQuery() != null) {
                return answerBirthDatePersonRequest(request.getBirthDatePersonQuery());
            }
            if (request.getNamePersonQuery() != null) {
                return answerNamePersonRequest(request.getNamePersonQuery());
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e); //TODO: Log medcom flow id
            throw SoapFaultUtil.newDGWSFault(wsseHeader, medcomHeader,
                                             FaultMessages.INTERNAL_SERVER_ERROR, FaultCodeValues.PROCESSING_PROBLEM);
        } catch (DatatypeConfigurationException e) {
            logger.error(e.getMessage(), e); //TODO: Log medcom flow id
            throw SoapFaultUtil.newDGWSFault(wsseHeader, medcomHeader,
                                             FaultMessages.INTERNAL_SERVER_ERROR, FaultCodeValues.PROCESSING_PROBLEM);
        }

        throw new AssertionError("Unreachable point: exactly one of the previous clauses is true");
    }

    private void printPersonLookupRequest(PersonLookupRequestType request) {
        System.out.println("Content of request: ");
        System.out.println(" - CPR: " + request.getCivilRegistrationNumberPersonQuery());
        System.out.println(" - Birth date person query: " + request.getBirthDatePersonQuery());
        System.out.println(" - Civil registration number list person: "
                                   + request.getCivilRegistrationNumberListPersonQuery());
        System.out.println(" - Name person query: " + request.getNamePersonQuery());
    }

    private void verifyExactlyOneQueryParameterIsNonNull(Holder<Security> securityHeaderHolder,
                                                         Holder<Header> medcomHeaderHolder, PersonLookupRequestType request) throws DGWSFault {
        Object[] queryParameters = new Object[4];

        queryParameters[0] = request.getBirthDatePersonQuery();
        queryParameters[1] = request.getCivilRegistrationNumberListPersonQuery();
        queryParameters[2] = request.getCivilRegistrationNumberPersonQuery();
        queryParameters[3] = request.getNamePersonQuery();

        int nonNullParameters = 0;
        for (Object parameter : queryParameters) {
            if (parameter != null) {
                nonNullParameters += 1;
            }
        }

        if (nonNullParameters != 1) {
            // TODO: This way of throwing faults was taken from DGCPROpslag and
            // doe not contain any meaningfull information for the caller
            throw SoapFaultUtil.newDGWSFault(securityHeaderHolder, medcomHeaderHolder,
                                             FaultMessages.INTERNAL_SERVER_ERROR, FaultCodeValues.PROCESSING_PROBLEM);
        }
    }

    private PersonLookupResponseType answerCprRequest(String cpr) throws SQLException, DatatypeConfigurationException {

        PersonLookupResponseType response = new PersonLookupResponseType();
        List<PersonInformationStructureType> personInformationStructure = response.getPersonInformationStructure();

        Person person = fetcher.fetch(Person.class, cpr);
        if (person != null) {
            personInformationStructure.add(personMapper.map(person, CHECK_WHITELIST_SERVICE_TYPE)); //TODO: Address protection or not?
        }
        return response;
    }

    private PersonLookupResponseType answerCivilRegistrationNumberListPersonRequest(
            CivilRegistrationNumberListPersonQueryType civilRegistrationNumberList) throws SQLException, DatatypeConfigurationException {
        PersonLookupResponseType response = new PersonLookupResponseType();
        List<PersonInformationStructureType> personInformationStructure = response.getPersonInformationStructure();
        for (String cpr : civilRegistrationNumberList.getCivilRegistrationNumber()) {
            Person person = fetcher.fetch(Person.class, cpr);
            if (person != null) {
                personInformationStructure.add(personMapper.map(person, CHECK_WHITELIST_SERVICE_TYPE)); //TODO: Address protection or not?
            }
        }

        return response;
    }

    private PersonLookupResponseType answerBirthDatePersonRequest(XMLGregorianCalendar birthDate) throws SQLException, DatatypeConfigurationException {
        PersonLookupResponseType response = new PersonLookupResponseType();
        List<PersonInformationStructureType> personInformationStructure = response.getPersonInformationStructure();

        List<Person> persons = fetcher.fetch(Person.class, "Foedselsdato", birthDate.toGregorianCalendar().getTime());
        for (Person person : persons) {
            personInformationStructure.add(personMapper.map(person, CHECK_WHITELIST_SERVICE_TYPE)); //TODO: Address protection or not?
        }

        return response;
    }

    private PersonLookupResponseType answerNamePersonRequest(NamePersonQueryType namePerson) throws SQLException, DatatypeConfigurationException {
        PersonLookupResponseType response = new PersonLookupResponseType();
        List<PersonInformationStructureType> personInformationStructure = response.getPersonInformationStructure();

        Map<String, Object> columnValuePairs = new HashMap<String, Object>();
        if (!StringUtils.isBlank(namePerson.getPersonGivenName())) {
            columnValuePairs.put("Fornavn", namePerson.getPersonGivenName());
        }
        if (!StringUtils.isBlank(namePerson.getPersonMiddleName())) {
            columnValuePairs.put("Mellemnavn", namePerson.getPersonMiddleName());
        }
        if (!StringUtils.isBlank(namePerson.getPersonSurnameName())) {
            columnValuePairs.put("Efternavn", namePerson.getPersonSurnameName());
        }

        List<Person> persons = fetcher.fetch(Person.class, columnValuePairs);
        for (Person person : persons) {
            personInformationStructure.add(personMapper.map(person, CHECK_WHITELIST_SERVICE_TYPE)); //TODO: Address protection or not?
        }

        return response;
    }
}
