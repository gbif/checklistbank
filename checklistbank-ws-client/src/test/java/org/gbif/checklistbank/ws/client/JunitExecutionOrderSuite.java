package org.gbif.checklistbank.ws.client;

import java.util.List;

import com.google.common.collect.Lists;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

/**
 *
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    JunitExecutionOrder.class,
})
public class JunitExecutionOrderSuite {
  private static List<String> executed = Lists.newArrayList();
  private static final Logger LOG = LoggerFactory.getLogger(JunitExecutionOrderSuite.class);

  @BeforeClass
  public static void setUp() {
    execute("suiteSetUp");
  }

  @AfterClass
  public static void tearDown() {
    execute("suiteTearDown");
    List<String> expected = Lists.newArrayList("suiteSetUp", "staticFieldInit", "BeforeClass",
        "fieldInit", "Constructor", "Before", "Test1", "After",
        "fieldInit", "Constructor", "Before", "Test2","After",
        "AfterClass", "suiteTearDown");
    assertEquals(expected, executed);
  }

  public static String execute(String method) {
    LOG.info(method);
    executed.add(method);
    return method;
  }

}
