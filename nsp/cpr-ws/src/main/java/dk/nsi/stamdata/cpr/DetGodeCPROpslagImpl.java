package dk.nsi.stamdata.cpr;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.http.HttpServletRequest;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.soap.SOAPFaultException;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.trifork.stamdata.Fetcher;
import com.trifork.stamdata.Nullable;
import com.trifork.stamdata.models.cpr.Person;

import dk.nsi.dgws.DgwsIdcardFilter;
import dk.nsi.stamdata.cpr.annotations.Whitelist;
import dk.nsi.stamdata.cpr.ws.*;
import dk.sosi.seal.model.SystemIDCard;
import dk.sosi.seal.model.constants.FaultCodeValues;
import dk.sosi.seal.model.constants.MedComTags;
import dk.sosi.seal.model.constants.NameSpaces;
import org.w3c.dom.Element;

@WebService(serviceName = "DetGodeCprOpslag", endpointInterface = "dk.nsi.stamdata.cpr.ws.DetGodeCPROpslag")
public class DetGodeCPROpslagImpl implements DetGodeCPROpslag
{	
    @Inject
	@Whitelist
	private Set<String> whitelist;
	
	@Inject
	private Provider<Fetcher> fetcherPool;

    @Resource
    private WebServiceContext context;
	
	@PostConstruct
	protected void init()
	{
		// This is a bit of a hack allowing Guice to inject
		// the dependencies without having to jump through
		// hoops to get it to do so automatically.
		
		ApplicationController.injector.injectMembers(this);
	}

	@Override
    public GetPersonInformationOut getPersonInformation(
            @WebParam(name = "Security",
                      targetNamespace = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                      mode = WebParam.Mode.INOUT,
                      partName = "wsseHeader")
                      Holder<Security> wsseHeader,
            @WebParam(name = "Header",
                      targetNamespace = "http://www.medcom.dk/dgws/2006/04/dgws-1.0.xsd",
                      mode = WebParam.Mode.INOUT,
                      partName = "medcomHeader")
                      Holder<Header> medcomHeader,
            @WebParam(name = "getPersonInformationIn",
                      targetNamespace = "http://rep.oio.dk/medcom.sundcom.dk/xml/wsdl/2007/06/28/",
                      partName = "parameters")
                      GetPersonInformationIn input) {
		// 1. Check the white list to see if the client is authorized.

        String clientCVR = findIdcardInRequest().getSystemInfo().getCareProvider().getID();

		if (!whitelist.contains(clientCVR))
		{
			returnSOAPSenderFault(DetGodeCPROpslagFaultMessages.CALLER_NOT_AUTHORIZED, FaultCodeValues.NOT_AUTHORIZED);
		}
		
		
		// 2. Fetch the person from the database.
		//
		// NOTE: Unfortunately the specification is defined so that we have to return a
		// fault if no person is found. We cannot change this to return nil which would
		// be a nicer protocol.
		
	    if (input.getPersonCivilRegistrationIdentifier() == null) {
            returnSOAPSenderFault("PersonCivilRegistrationIdentifier was not set in request, but is required.", null);
        }


		String pnr = input.getPersonCivilRegistrationIdentifier();
		Person person = fetchPersonWithPnr(pnr);
		
		if (person == null)
		{
			returnSOAPSenderFault(DetGodeCPROpslagFaultMessages.NO_DATA_FOUND_FAULT_MSG, null);
		}

		// We now have the requested person. Use it to fill in
		// the response.
		
		GetPersonInformationOut output = new GetPersonInformationOut();
		
		PersonInformationStructureType personInformation = new PersonInformationStructureType();
		personInformation.setCurrentPersonCivilRegistrationIdentifier(pnr);

		output.setPersonInformationStructure(personInformation);
		
		return output;
	}

    private SystemIDCard findIdcardInRequest() {
        HttpServletRequest servletRequest = (HttpServletRequest)context.getMessageContext().get(MessageContext.SERVLET_REQUEST);
        SystemIDCard idcard = (SystemIDCard)servletRequest.getAttribute(DgwsIdcardFilter.IDCARD_REQUEST_ATTRIBUTE_KEY);

        return idcard;
    }

    @Override
    public GetPersonWithHealthCareInformationOut getPersonWithHealthCareInformation(@WebParam(name = "Security", targetNamespace = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", mode = WebParam.Mode.INOUT, partName = "wsseHeader") Holder<Security> wsseHeader, @WebParam(name = "Header", targetNamespace = "http://www.medcom.dk/dgws/2006/04/dgws-1.0.xsd", mode = WebParam.Mode.INOUT, partName = "medcomHeader") Holder<Header> medcomHeader, @WebParam(name = "getPersonWithHealthCareInformationIn", targetNamespace = "http://rep.oio.dk/medcom.sundcom.dk/xml/wsdl/2007/06/28/", partName = "parameters") GetPersonWithHealthCareInformationIn parameters) {
		return null;
        // TODO: Add sikrede information to the response
	}
	
	// HELPERS
	
	private Person fetchPersonWithPnr(String pnr)
	{
		try
		{
			Fetcher fetcher = fetcherPool.get();
			return fetcher.fetch(Person.class, pnr);
		}
		catch (Exception e)
		{
			throw new RuntimeException(DetGodeCPROpslagFaultMessages.INTERNAL_SERVER_ERROR, e);
		}
	}
	
	private void returnSOAPSenderFault(String message, @Nullable String medcomFaultcode)
	{		
		SOAPFault fault = null;
		
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

            if (medcomFaultcode != null) {
                Element detail = factory.createDetail();
                Element medcomFaultCode = detail.getOwnerDocument().createElementNS(NameSpaces.MEDCOM_SCHEMA, MedComTags.FAULT_CODE_PREFIXED);
                medcomFaultCode.appendChild(detail.getOwnerDocument().createTextNode(medcomFaultcode));
                detail.appendChild(medcomFaultCode);
                fault.appendChild(detail);
            }
		}
		catch (Exception e)
		{
			returnServerErrorFault(e);
		}
		
		throw new SOAPFaultException(fault);
	}
	
	private void returnServerErrorFault(Exception e)
	{
		throw new RuntimeException(DetGodeCPROpslagFaultMessages.INTERNAL_SERVER_ERROR, e);
	}
}
