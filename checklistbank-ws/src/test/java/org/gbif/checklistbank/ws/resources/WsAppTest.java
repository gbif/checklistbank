package org.gbif.checklistbank.ws.resources;

import java.security.Permission;

import org.gbif.ws.app.Application;
import org.junit.Test;

import static org.junit.Assert.fail;


public class WsAppTest {

  /**
   * Test the startup of the webapp.
   * We expect a SecurityException raised as the Application stops because of missing configs.
   * But no NoSuchMethodError thrown by incompatible jackson we see with a wrong version introduced by solr
   */
  @Test(expected = SecurityException.class)
  public void testWsStartup() {
    // throw exception if Application exits
    System.setSecurityManager(new NoExitSecurityManager());
    Application.main(new String[]{});
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
      throw new SecurityException("Exit status " + status + " called");
    }
  }
}
