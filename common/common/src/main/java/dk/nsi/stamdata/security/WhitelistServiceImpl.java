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
package dk.nsi.stamdata.security;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.type.StandardBasicTypes;

import java.util.List;

public class WhitelistServiceImpl implements WhitelistService {

    private static final String QUERY_IS_CVR_WHITELISTED = "SELECT cvr FROM whitelist_config WHERE component_name=? AND cvr=?";
    private static final String QUERY_GET_ALL_WHITELISTED = "SELECT cvr FROM whitelist_config WHERE component_name=?";
    private Session session;

    public WhitelistServiceImpl(Session session) {
        this.session = session;
    }

    @Override
    public List<String> getWhitelist(String serviceName) {
        SQLQuery query = session.createSQLQuery(QUERY_GET_ALL_WHITELISTED);
        query.setString(0, serviceName);
        query.addScalar("cvr", StandardBasicTypes.STRING);
        return query.list();
    }

    @Override
    public boolean isCvrWhitelisted(String cvr, String serviceName) {
        SQLQuery query = session.createSQLQuery(QUERY_IS_CVR_WHITELISTED);
        query.setString(0, serviceName);
        query.setString(1, cvr);
        query.addScalar("cvr", StandardBasicTypes.STRING);
        return query.list().size() >= 1;
    }
}
