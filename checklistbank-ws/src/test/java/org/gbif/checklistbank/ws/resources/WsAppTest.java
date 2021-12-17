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
package org.gbif.checklistbank.ws.resources;

import org.gbif.ws.app.Application;

import java.security.Permission;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;



public class WsAppTest {
  private SecurityManager sm;

  @Before
  public void init(){
    sm = System.getSecurityManager();
    System.setSecurityManager(new NoExitSecurityManager());
  }

  @After
  public void shutdown(){
    System.setSecurityManager(sm);
  }

  /**
   * Test the startup of the webapp.
   * We expect a ExitException raised as the Application stops because of missing configs.
   * But no NoSuchMethodError thrown by incompatible jackson we see with a wrong version introduced by solr
   */
  @Test(expected = ExitException.class)
  public void testWsStartup() {
    Application.main(new String[]{});
  }

  public static class ExitException extends RuntimeException {
    public ExitException(int status) {
      super("Exit status " + status + " called");
    }
  }

  private static class NoExitSecurityManager extends SecurityManager {
    @Override
    public void checkPermission(Permission perm) {
      // allow anything.
    }
    @Override
    public void checkPermission(Permission perm, Object context) {
      // allow anything.
    }

    @Override
    public void checkExit(int status) {
      super.checkExit(status);
      throw new ExitException(status);
    }
  }
}
