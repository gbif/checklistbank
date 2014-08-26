package org.gbif.checklistbank.neo;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageContainer;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.cli.normalizer.NeoTest;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Objects;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class NeoMapperTest extends NeoTest {

  final UUID dKey = UUID.randomUUID();
  final UUID cKey = UUID.randomUUID();

  @Test
  public void testPropertyMap() throws Exception {
    NeoMapper mapper = NeoMapper.instance();
    NameUsage u = usage();

    Map<String, Object> map = mapper.propertyMap(u, false);
    assertEquals(100, map.get("key"));
    assertNotNull(map.get("datasetKey"));
    assertNotNull(map.get("constituentKey"));
    assertEquals("Abies alba", map.get("scientificName"));
    assertEquals(20, map.get("basionymKey"));
    assertEquals(23, map.get("parentKey"));
    assertEquals("Abies", map.get("parent"));
    assertEquals(1, map.get("classKey"));
    assertEquals("Trees", map.get("clazz"));
    assertNotNull(map.get("lastCrawled"));
    assertNotNull(map.get("lastInterpreted"));
    assertEquals(Rank.SPECIES.ordinal(), map.get("rank"));
    assertEquals(TaxonomicStatus.ACCEPTED.ordinal(), map.get("taxonomicStatus"));
    assertEquals(NameType.SCINAME.ordinal(), map.get("nameType"));
    assertEquals(7, map.get("numDescendants"));

    assertNotNull(map.get("nomenclaturalStatus"));

    // isSynonym is set to false by default
    assertEquals(17, map.size());
  }

  private NameUsage usage() {
    return usage(true);
  }

  private NameUsage usage(boolean addKeys) {
    NameUsage u = new NameUsage();
    if (addKeys) {
      u.setKey(100);
      u.setBasionymKey(20);
      u.setParentKey(23);
      u.setClassKey(1);
    }
    u.setDatasetKey(dKey);
    u.setConstituentKey(cKey);
    u.setScientificName("Abies alba");
    u.setParent("Abies");
    u.setClazz("Trees");
    u.setLastCrawled(new Date());
    u.setLastInterpreted(new Date());
    u.setRank(Rank.SPECIES);
    u.setTaxonomicStatus(TaxonomicStatus.ACCEPTED);
    u.setNameType(NameType.SCINAME);
    u.setNumDescendants(7);
    u.getNomenclaturalStatus().add(NomenclaturalStatus.CONSERVED);
    u.getNomenclaturalStatus().add(NomenclaturalStatus.ILLEGITIMATE);
    return u;
  }

  @Test
  public void testNodeStore() throws Exception {
    NeoMapper mapper = NeoMapper.instance();
    initDb(UUID.randomUUID());

    try (Transaction tx = beginTx()) {
      NameUsage u = usage();
      Node n = db.createNode();

      mapper.store(n, u, true);
      assertEquals(100, n.getProperty("key"));
      assertNotNull(n.getProperty("datasetKey"));
      assertNotNull(n.getProperty("constituentKey"));
      assertEquals("Abies alba", n.getProperty("scientificName"));
      assertEquals(20, n.getProperty("basionymKey"));
      assertEquals(23, n.getProperty("parentKey"));
      assertEquals("Abies", n.getProperty("parent"));
      assertEquals(1, n.getProperty("classKey"));
      assertEquals("Trees", n.getProperty("clazz"));
      assertNotNull(n.getProperty("lastCrawled"));
      assertNotNull(n.getProperty("lastInterpreted"));
      assertEquals(Rank.SPECIES.ordinal(), n.getProperty("rank"));
      assertEquals(TaxonomicStatus.ACCEPTED.ordinal(), n.getProperty("taxonomicStatus"));
      assertEquals(NameType.SCINAME.ordinal(), n.getProperty("nameType"));
      assertEquals(7, n.getProperty("numDescendants"));

      assertNotNull(n.getProperty("nomenclaturalStatus"));

      // isSynonym is set to false by default
      assertEquals(17, IteratorUtil.count(n.getPropertyKeys()));

    }
  }

  @Test
  public void testRoundtrip() throws Exception {
    NeoMapper mapper = NeoMapper.instance();
    initDb(UUID.randomUUID());

    try (Transaction tx = beginTx()) {
      NameUsage u1 = usage();
      Node n = db.createNode();

      mapper.store(n, u1, true);
      NameUsage u2 = mapper.read(n, new NameUsage());

      assertEquals(u1, u2);
    }
  }

  @Test
  public void testClassification() throws Exception {
    NeoMapper mapper = NeoMapper.instance();
    initDb(UUID.randomUUID());
    try (Transaction tx = beginTx()) {
      NameUsage u = usage();
      u.setKingdom("Plant");
      u.setFamily("Asteraceae");
      u.setGenus("Aster");
      Node n = db.createNode();

      mapper.store(n, u, true);
      tx.success();

      LinneanClassification cl = mapper.readVerbatimClassification(n);

      assertEquals("Plant", cl.getKingdom());
      assertNull(cl.getPhylum());
      assertNull(cl.getClazz());
      assertNull(cl.getOrder());
      assertEquals("Asteraceae", cl.getFamily());
      assertEquals("Aster", cl.getGenus());
      assertNull(cl.getSubgenus());
    }
  }

  @Test
  public void testContainer() throws Exception {
    NeoMapper mapper = NeoMapper.instance();
    initDb(UUID.randomUUID());

    try (Transaction tx = beginTx()) {
      NameUsageContainer u = new NameUsageContainer(usage(false));
      u.setKey(0);

      VernacularName v1 = new VernacularName();
      v1.setVernacularName("Adler");
      v1.setLanguage(Language.GERMAN);
      u.getVernacularNames().add(v1);

      VernacularName v2 = new VernacularName();
      v2.setVernacularName("Fox");
      v2.setLanguage(Language.ENGLISH);
      v2.setCountry(Country.AFGHANISTAN);
      u.getVernacularNames().add(v2);

      Node n = db.createNode();
      mapper.store(n, u, true);

      NameUsageContainer u2 = mapper.read(n);

      assertEquals(u, u2);
    }
  }

  @Test
  public void testDataTypes() throws Exception {
    NeoMapper mapper = NeoMapper.instance();
    initDb(UUID.randomUUID());

    try (Transaction tx = beginTx()) {
      TestBean obj = new TestBean();
      obj.string = "Hello mr bean";
      obj.date = new Date();
      obj.integer = 123;
      obj.bool1 = true;
      obj.bool2 = null;
      obj.dub = 456.07d;
      obj.longer = Long.MAX_VALUE;
      obj.odd = Oddity.ODD;
      obj.uri = URI.create("Http://bean.org");
      obj.intList = Lists.newArrayList(1, 2, 4, 6, 8, 9);
      obj.boolList = Lists.newArrayList(true, true, false, null, true);
      obj.strList = Lists.newArrayList("hi", "du", "eiermann");
      obj.oddList = Lists.newArrayList(Oddity.EVEN, Oddity.ODD);
      TestBeanInc subbean = new TestBeanInc();
      subbean.integer = 9453;
      subbean.odd = Oddity.EVEN;
      subbean.string = "Mannomann";
      obj.beanList.add(subbean);

      Node n = db.createNode();
      mapper.store(n, obj, true);
      tx.success();

      TestBean bean2 = mapper.read(n, new TestBean());

      assertEquals(obj, bean2);
    }
  }

  @Test
  public void testContainerRoundtrip() throws Exception {
    NeoMapper mapper = NeoMapper.instance();
    initDb(UUID.randomUUID());

    try (Transaction tx = beginTx()) {
      NameUsageContainer u1 = new NameUsageContainer(usage(false));
      u1.setKey(0);
      Node n = db.createNode();

      mapper.store(n, u1);
      NameUsageContainer u2 = mapper.read(n);

      assertEquals(u1, u2);
    }
  }

  public static enum Oddity {
    EVEN, ODD
  }

  public static class TestBeanInc {

    public String string;
    public Integer integer;
    public Oddity odd;

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final TestBeanInc other = (TestBeanInc) obj;
      return Objects.equal(this.string, other.string) && Objects.equal(this.integer, other.integer) && Objects.equal(
        this.odd,
        other.odd);
    }
  }

  public static class TestBean {

    public String string;
    public Integer integer;
    public boolean bool1;
    public Boolean bool2;
    public double dub;
    public long longer;
    public Date date;
    public UUID uuid;
    public URI uri;
    public Oddity odd;
    public List<Integer> intList = Lists.newArrayList();
    public List<Boolean> boolList = Lists.newArrayList();
    public List<String> strList = Lists.newArrayList();
    public List<Date> dateList = Lists.newArrayList();
    public List<Oddity> oddList = Lists.newArrayList();
    public List<TestBeanInc> beanList = Lists.newArrayList();

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final TestBean other = (TestBean) obj;
      return Objects.equal(this.string, other.string)
             && Objects.equal(this.integer, other.integer)
             && Objects.equal(this.bool1,
                              other.bool1)
             && Objects.equal(this.bool2, other.bool2)
             && Objects.equal(this.dub, other.dub)
             && Objects.equal(this.longer,
                              other.longer)
             && Objects.equal(this.date, other.date)
             && Objects.equal(this.uuid, other.uuid)
             && Objects.equal(this.uri,
                              other.uri)
             && Objects.equal(this.odd, other.odd)
             && Objects.equal(this.intList, other.intList)
             && Objects.equal(this.boolList,
                              other.boolList)
             && Objects.equal(this.strList, other.strList)
             && Objects.equal(this.dateList, other.dateList)
             && Objects.equal(this.oddList, other.oddList)
             && Objects.equal(this.beanList, other.beanList);
    }

  }
}