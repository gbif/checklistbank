package org.gbif.checklistbank.ws.resources;

import java.security.Permission;

import org.gbif.ws.app.Application;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;


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
