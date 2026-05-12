/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.ws;

import org.apache.catalina.connector.Connector;
import org.junit.Test;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;

import static org.junit.Assert.assertEquals;

public class ChecklistBankWsApplicationTest {

  @Test
  public void shouldRelaxTomcatQueryAndPathCharacters() {
    ChecklistBankWsApplication app = new ChecklistBankWsApplication();
    TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();

    app.tomcatRequestCharCustomizer().customize(factory);

    Connector connector = new Connector();
    factory
        .getTomcatConnectorCustomizers()
        .forEach(customizer -> customizer.customize(connector));

    assertEquals(
        ChecklistBankWsApplication.RELAXED_REQUEST_CHARS,
        connector.getProperty("relaxedPathChars"));
    assertEquals(
        ChecklistBankWsApplication.RELAXED_REQUEST_CHARS,
        connector.getProperty("relaxedQueryChars"));
  }
}
