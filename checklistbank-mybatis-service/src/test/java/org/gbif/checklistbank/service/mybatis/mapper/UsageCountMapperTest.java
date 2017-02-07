package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.Constants;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.model.UsageCount;
import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class UsageCountMapperTest extends MapperITBase<UsageCountMapper> {

  public UsageCountMapperTest() {
    super(UsageCountMapper.class, ClbDbTestRule.squirrels());
  }

  @Test
  public void testRoot() {
    List<UsageCount> root = mapper.root(Constants.NUB_DATASET_KEY);
    assertEquals(1, root.size());
    assertEquals(1, root.get(0).getKey());

    root = mapper.root(ClbDbTestRule.SQUIRRELS_DATASET_KEY);
    assertEquals(1, root.size());
    assertEquals(100000001, root.get(0).getKey());
    assertEquals(27, root.get(0).getSize());
    assertEquals("Animalia", root.get(0).getName());
    assertEquals(Rank.KINGDOM, root.get(0).getRank());
  }

  @Test
  public void testChildren() {
    List<UsageCount> children = mapper.children(1);
    assertEquals(1, children.size());
    assertEquals(10, children.get(0).getKey());

    children = mapper.children(100000001);
    assertEquals(1, children.size());
    assertEquals(100000002, children.get(0).getKey());

    children = mapper.children(100000005);
    assertEquals(2, children.size());
    assertEquals(100000042, children.get(0).getKey());
    assertEquals(100000043, children.get(1).getKey());
    assertEquals(10, children.get(1).getSize());

    children = mapper.children(100000025);
    assertEquals(9, children.size());
  }

  @Test
  public void testChildrenTilRank() {
    assertTrue(mapper.childrenUntilRank(1, Rank.KINGDOM).isEmpty());

    List<UsageCount> children = mapper.childrenUntilRank(1, Rank.FAMILY);
    assertEquals(1, children.size());
    assertEquals(10, children.get(0).getKey());

    children = mapper.childrenUntilRank(100000025, Rank.SUBSPECIES);
    assertEquals(7, children.size());
  }
}