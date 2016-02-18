package org.gbif.checklistbank.utils;

import org.gbif.api.model.registry.eml.Collection;

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 *
 */
public class ObjectUtilsTest {

  @Test
  public void testCoalesce() throws Exception {
    assertNull(ObjectUtils.coalesce());
    assertNull(ObjectUtils.coalesce((Integer)null));
    assertNull(ObjectUtils.coalesce((Integer)null, (Integer)null));

    assertEquals((Integer)13, ObjectUtils.coalesce(null, 13));
    assertEquals((Integer)13, ObjectUtils.coalesce(null, 13, 14));
    assertEquals((Integer)13, ObjectUtils.coalesce(13, 15));
  }

  @Test
  public void testCoalesce1() throws Exception {
    assertNull(ObjectUtils.coalesce((Collection) null));
    assertNull(ObjectUtils.coalesce(Lists.newArrayList()));
    assertNull(ObjectUtils.coalesce(Lists.newArrayList(null, null)));

    assertEquals((Integer)13, ObjectUtils.coalesce(Lists.newArrayList(null, null, 13)));
    assertEquals((Integer)13, ObjectUtils.coalesce(Lists.newArrayList(null, null, 13, 14)));
    assertEquals((Integer)13, ObjectUtils.coalesce(Lists.newArrayList(13, null)));

  }
}