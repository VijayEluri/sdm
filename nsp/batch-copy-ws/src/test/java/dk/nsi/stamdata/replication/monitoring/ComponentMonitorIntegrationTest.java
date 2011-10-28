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
package dk.nsi.stamdata.replication.monitoring;

import static com.jayway.restassured.RestAssured.expect;
import static org.junit.matchers.JUnitMatchers.containsString;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.jayway.restassured.RestAssured;

import dk.nsi.stamdata.replication.webservice.GuiceTestRunner;
import dk.nsi.stamdata.testing.TestServer;

@RunWith(GuiceTestRunner.class)
public class ComponentMonitorIntegrationTest {

    private TestServer server;

    @Before
    public void setUp() throws Exception {
        server = new TestServer().port(8986).contextPath("/").start();
        RestAssured.port = 8986;
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void testStatusIsOk()
    {
        expect().statusCode(200).body(containsString("200 OK")).when().get("/status");
    }
}
