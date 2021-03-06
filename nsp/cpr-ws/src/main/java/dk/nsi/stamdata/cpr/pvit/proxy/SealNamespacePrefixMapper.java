/**
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Contributor(s): Contributors are attributed in the source code
 * where applicable.
 *
 * The Original Code is "Stamdata".
 *
 * The Initial Developer of the Original Code is Trifork Public A/S.
 *
 * Portions created for the Original Code are Copyright 2011,
 * Lægemiddelstyrelsen. All Rights Reserved.
 *
 * Portions created for the FMKi Project are Copyright 2011,
 * National Board of e-Health (NSI). All Rights Reserved.
 */
package dk.nsi.stamdata.cpr.pvit.proxy;

import java.util.HashMap;
import java.util.Map;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

/**
 * This class is used for deserializing DGWS headers. DGWS require that the
 * namespaces saml and ds be present.
 * 
 */
public class SealNamespacePrefixMapper extends NamespacePrefixMapper {

	private Map<String, String> prefixMap = new HashMap<String, String>();

	{
		// Essential for seal validation
		prefixMap.put("urn:oasis:names:tc:SAML:2.0:assertion", "saml");
		prefixMap.put("http://www.w3.org/2000/09/xmldsig#", "ds");

		// Non essential but pretty.
		prefixMap.put("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", "wsse");
		prefixMap.put("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd", "wsu");
		prefixMap.put("http://www.medcom.dk/dgws/2006/04/dgws-1.0.xsd", "medcom");
	}

	@Override
	public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requiredPrefix) {
		if (prefixMap.containsKey(namespaceUri)) {
			return prefixMap.get(namespaceUri);
		}
		return suggestion;
	}
}
