package dk.nsi.stamdata.cpr;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.trifork.stamdata.Fetcher;
import com.trifork.stamdata.Preconditions;
import com.trifork.stamdata.models.cpr.Person;
import com.trifork.stamdata.models.sikrede.Sikrede;

import dk.nsi.dgws.DgwsIdcardFilter;
import dk.nsi.stamdata.cpr.annotations.Whitelist;
import dk.nsi.stamdata.cpr.ws.*;
import dk.sosi.seal.model.SystemIDCard;
import dk.sosi.seal.model.constants.FaultCodeValues;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.ServletRequest;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.soap.SOAPFaultException;
import java.util.Set;

import static com.trifork.stamdata.Preconditions.checkNotNull;
import static com.trifork.stamdata.Preconditions.checkState;

@WebService(serviceName = "DetGodeCprOpslag", endpointInterface = "dk.nsi.stamdata.cpr.ws.DetGodeCPROpslag")
public class DetGodeCPROpslagImpl implements DetGodeCPROpslag
{	
	private static Logger logger = LoggerFactory.getLogger(DetGodeCPROpslagImpl.class);
	
	private static final String NS_TNS = "http://rep.oio.dk/medcom.sundcom.dk/xml/wsdl/2007/06/28/";
	private static final String NS_DGWS_1_0 = "http://www.medcom.dk/dgws/2006/04/dgws-1.0.xsd"; // TODO: Shouldn't this be 1.0.1?
	private static final String NS_WS_SECURITY = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
	
    @Inject
	@Whitelist
	private Set<String> whitelist;
	
	@Inject
	private Provider<Fetcher> fetcherPool;

    @Inject
    private PersonMapper personMapper;
    
    @Inject
    private PersonWithHealthCareMapper personWithHealthCareMapper;

    @Resource
    private WebServiceContext context;
	
	@PostConstruct
	protected void init()
	{
		// This is a bit of a hack allowing Guice to inject
		// the dependencies without having to jump through
		// hoops to get it to do so automatically.
		
		checkState(ApplicationController.injector != null, "The application controller must be instanciated before jax-ws.");
		
		ApplicationController.injector.injectMembers(this);
	}

	@Override
    public GetPersonInformationOut getPersonInformation(
            @WebParam(name = "Security",
                      targetNamespace = NS_WS_SECURITY,
                      mode = WebParam.Mode.INOUT,
                      partName = "wsseHeader")
                      Holder<Security> wsseHeader,
            @WebParam(name = "Header",
                      targetNamespace = NS_DGWS_1_0,
                      mode = WebParam.Mode.INOUT,
                      partName = "medcomHeader")
                      Holder<Header> medcomHeader,
            @WebParam(name = "getPersonInformationIn",
                      targetNamespace = NS_TNS,
                      partName = "parameters")
                      GetPersonInformationIn input) throws DGWSFault {
		
		// 1. Check the white list to see if the client is authorized.

		String pnr = input.getPersonCivilRegistrationIdentifier();
		
		checkClientAuthorization(pnr, wsseHeader, medcomHeader);

		// 2. Validate the input parameters.

		checkInputParameters(pnr);
		
		// 2. Fetch the person from the database.
		//
		// NOTE: Unfortunately the specification is defined so that we have to return a
		// fault if no person is found. We cannot change this to return nil which would
		// be a nicer protocol.

		Person person = fetchPersonWithPnr(pnr);

		// We now have the requested person. Use it to fill in
		// the response.
		
		GetPersonInformationOut output = new GetPersonInformationOut();
        PersonInformationStructureType personInformation;
        
        try
        {
            personInformation = personMapper.map(person);
        }
        catch (DatatypeConfigurationException e)
        {
            throw newServerErrorFault(e);
        }
        
        output.setPersonInformationStructure(personInformation);
        
		return output;
	}


	@Override
	public GetPersonWithHealthCareInformationOut getPersonWithHealthCareInformation(
			@WebParam(	name = "Security",
						targetNamespace = NS_WS_SECURITY,
						mode = WebParam.Mode.INOUT,
						partName = "wsseHeader")
						Holder<Security> wsseHeader,
			@WebParam(	name = "Header",
						targetNamespace = NS_DGWS_1_0,
						mode = WebParam.Mode.INOUT,
						partName = "medcomHeader")
						Holder<Header> medcomHeader,
			@WebParam(	name = "getPersonWithHealthCareInformationIn",
						targetNamespace = NS_TNS,
						partName = "parameters")
						GetPersonWithHealthCareInformationIn parameters) throws DGWSFault {
		// 1. Check the white list to see if the client is authorized.

		String pnr = parameters.getPersonCivilRegistrationIdentifier();

		checkClientAuthorization(pnr, wsseHeader, medcomHeader);

		// 2. Validate the input parameters.

		checkInputParameters(pnr);

		// 2. Fetch the person from the database.
		//
		// NOTE: Unfortunately the specification is defined so that we have to
		// return a fault if no person is found. We cannot change this to return nil
		// which would be a nicer protocol.
		
		Person person = fetchPersonWithPnr(pnr);
		Sikrede sikrede = null; // TODO: Fetch the "sikrede" record for the pnr.
		
		GetPersonWithHealthCareInformationOut output = new GetPersonWithHealthCareInformationOut();
		
		try
		{
			output.setPersonWithHealthCareInformationStructure(personWithHealthCareMapper.map(person, sikrede));
		}
		catch (DatatypeConfigurationException e)
		{
			throw newServerErrorFault(e);
		}
		
		return output;
	}
	
	// HELPERS
    
    private SystemIDCard fetchIDCardFromRequestContext()
    {
        ServletRequest servletRequest = (ServletRequest)context.getMessageContext().get(MessageContext.SERVLET_REQUEST);
        SystemIDCard idcard = (SystemIDCard)servletRequest.getAttribute(DgwsIdcardFilter.IDCARD_REQUEST_ATTRIBUTE_KEY);
        
        // We are counting on the DGWS filter to inject the ID Card
        // into the request context. In fact we can never get to this
        // point if the request did not have a ID-card. Therefore if
        // the id card is null, the service is in an inconsistent state.
        
        Preconditions.checkState(idcard != null, "The SOSI ID Card was not injected to the request context.");
        
        return idcard;
    }
	
	private Person fetchPersonWithPnr(String pnr)
	{
		checkNotNull(pnr, "pnr");
		
		Person person;

		try
		{
			Fetcher fetcher = fetcherPool.get();
			person = fetcher.fetch(Person.class, pnr);
		}
		catch (Exception e)
		{
			throw newServerErrorFault(e);
		}

		if (person == null)
		{
			throw newSOAPSenderFault(DetGodeCPROpslagFaultMessages.NO_DATA_FOUND_FAULT_MSG);
		}

		return person;
	}

	private DGWSFault newDGWSFault(Holder<Security> securityHolder, Holder<Header> medcomHeaderHolder, String status, String errorMsg) throws DGWSFault
	{
		DGWSHeaderUtil.setHeadersToOutgoing(securityHolder, medcomHeaderHolder);
		medcomHeaderHolder.value.setFlowStatus(status);
		
		return new DGWSFault(errorMsg, DGWSHeaderUtil.DGWS_ERROR_MSG);
	}

	private SOAPFaultException newSOAPSenderFault(String message)
	{
		checkNotNull(message, "message");
		
		SOAPFault fault;
		
		try
		{
			// We have to make sure to use the same protocol version
			// as defined in the WSDL.
			
			SOAPFactory factory = SOAPFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
			
			fault = factory.createFault();
			fault.setFaultCode(SOAPConstants.SOAP_SENDER_FAULT);
			
			// TODO: For some reason the xml:lang att. is always "en"
			// even when the locale is set in this next call.
			
			fault.setFaultString(message);
		}
		catch (Exception e)
		{
			throw newServerErrorFault(e);
		}
		
		return new SOAPFaultException(fault);
	}
	

	private RuntimeException newServerErrorFault(Exception e)
	{
		checkNotNull(e, "e");
		
		return new RuntimeException(DetGodeCPROpslagFaultMessages.INTERNAL_SERVER_ERROR, e);
	}


	private void checkInputParameters(String pnr)
	{
		if (StringUtils.isBlank(pnr))
		{
			throw newSOAPSenderFault("PersonCivilRegistrationIdentifier was not set in request, but is required.");
		}
	}


	private void checkClientAuthorization(String requestedPNR, Holder<Security> wsseHeader, Holder<Header> medcomHeader) throws DGWSFault
    {
		String clientCVR = fetchIDCardFromRequestContext().getSystemInfo().getCareProvider().getID();

		if (!whitelist.contains(clientCVR))
		{
            logger.warn("Unauthorized access attempt. client_cvr={}, requested_pnr={}", clientCVR, requestedPNR);
            throw newDGWSFault(wsseHeader, medcomHeader, DetGodeCPROpslagFaultMessages.CALLER_NOT_AUTHORIZED, FaultCodeValues.NOT_AUTHORIZED);
        }
		else
		{
            logger.info("Access granted. client_cvr={}, requested_pnr={}", clientCVR, requestedPNR);
        }
	}
}