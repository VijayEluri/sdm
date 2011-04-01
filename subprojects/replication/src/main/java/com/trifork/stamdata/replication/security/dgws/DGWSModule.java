// Stamdata - Copyright (C) 2011 National Board of e-Health (NSI)
// 
// All source code and information supplied as part of Stamdata is
// copyright to National Board of e-Health.
// 
// The source code has been released under a dual license - meaning you can
// use either licensed version of the library with your code.
// 
// It is released under the Common Public License 1.0, a copy of which can
// be found at the link below.
// http://www.opensource.org/licenses/cpl1.0.php
// 
// It is released under the LGPL (GNU Lesser General Public License), either
// version 2.1 of the License, or (at your option) any later version. A copy
// of which can be found at the link below.
// http://www.gnu.org/copyleft/lesser.html

package com.trifork.stamdata.replication.security.dgws;

import java.io.IOException;
import java.util.Properties;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import com.google.inject.Provides;
import com.trifork.stamdata.replication.security.SecurityManager;
import com.trifork.stamdata.replication.security.UnrestrictedSecurityManager;
import com.trifork.stamdata.replication.util.ConfiguredModule;
import dk.sosi.seal.SOSIFactory;
import dk.sosi.seal.model.SignatureUtil;
import dk.sosi.seal.pki.Federation;
import dk.sosi.seal.pki.SOSIFederation;
import dk.sosi.seal.vault.EmptyCredentialVault;


public class DGWSModule extends ConfiguredModule {

	private Properties encryptionSetting;
	private JAXBContext jaxbContext;
	public DGWSModule() throws IOException {

		super();
	}

	@Override
	protected void configureServlets() {

		// BIND THE SECURITY MANAGER
		//
		// The binding is required by the replication module.

		if (getProperty("security.enabled").equals("false")) {
			bind(SecurityManager.class).to(UnrestrictedSecurityManager.class);
		} else {
			bind(SecurityManager.class).to(DGWSSecurityManager.class);
		}

		// SETUP THE ENCRYPTION SETTINGS FOR SEAL
		//
		// Use SEAL's handy crypto provider selection algorithm
		// to figure out what crypto provider is best suited for
		// the current runtime environment.

		encryptionSetting = SignatureUtil.setupCryptoProviderForJVM();

		// SERVE THE SOAP AUTHENTICATION SERVICE
		//
		// This servlet takes DGWS requests, authenticates and authorizes
		// them, and returns a replication token if authorized.

		serve("/stamdata/authenticate").with(AuthorizationServlet.class);

		// XML MARSHALLING
		//
		// The marshallers are used to marshal the SOAP
		// bodies to and from XML.
		//
		// @see AuthorizationRequestStructure
		// @see AuthorizationResponseStructure

		try {
			jaxbContext = JAXBContext.newInstance(AuthorizationRequestStructure.class, AuthorizationResponseStructure.class);
		}
		catch (JAXBException e) {
			addError(e);
		}
	}

	@Provides
	protected Federation provideSOSIFederation() {

		return new SOSIFederation(encryptionSetting);
	}

	@Provides
	protected SOSIFactory provideSOSIFactory(Federation federation) {

		return new SOSIFactory(federation, new EmptyCredentialVault(), encryptionSetting);
	}

	@Provides
	protected Marshaller provideMarshaller() throws JAXBException {

		return jaxbContext.createMarshaller();
	}

	@Provides
	protected Unmarshaller provideUnmarshaller() throws JAXBException {

		return jaxbContext.createUnmarshaller();
	}
}
