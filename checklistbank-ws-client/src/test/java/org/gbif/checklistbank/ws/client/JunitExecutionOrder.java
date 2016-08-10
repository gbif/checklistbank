package org.gbif.checklistbank.ws.client;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies junit runs test in proper order
 */
public class JunitExecutionOrder {
  private static String staticFieldInit = JunitExecutionOrderSuite.execute("staticFieldInit");
  private String fieldInit = JunitExecutionOrderSuite.execute("fieldInit");

  public JunitExecutionOrder() {
    JunitExecutionOrderSuite.execute("Constructor");
  }

  @BeforeClass
  public static void beforeClass() {
    JunitExecutionOrderSuite.execute("BeforeClass");
  }

  @Before
  public void before() {
    JunitExecutionOrderSuite.execute("Before");
  }

  @Test
  public void test1() {
    JunitExecutionOrderSuite.execute("Test1");
  }

  @Test
  public void test2() {
    JunitExecutionOrderSuite.execute("Test2");
  }

  @After
  public void after() {
    JunitExecutionOrderSuite.execute("After");
  }

  @AfterClass
  public static void afterClass() {
    JunitExecutionOrderSuite.execute("AfterClass");
  }

}