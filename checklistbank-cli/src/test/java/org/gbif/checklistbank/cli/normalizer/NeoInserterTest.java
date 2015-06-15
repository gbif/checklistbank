package org.gbif.checklistbank.cli.normalizer;

import org.gbif.api.model.checklistbank.NameUsageContainer;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.neo.NeoMapper;
import org.gbif.dwca.record.RecordImpl;
import org.gbif.dwca.record.StarRecordImpl;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwca.io.ArchiveField;
import org.gbif.utils.file.FileUtils;

import java.io.File;
import java.net.URI;
import java.util.List;

import com.beust.jcommander.internal.Lists;
import com.yammer.metrics.Meter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NeoInserterTest {
  NeoInserter ins;
  File store;
  List<String> values;
  List<ArchiveField> fields;

  @Before
  public void init() throws Exception {
    values = Lists.newArrayList();
    fields = Lists.newArrayList();
    store = FileUtils.createTempDir();
    ins = new NeoInserter(store, 100, Mockito.mock(Meter.class));
  }

  @After
  public void cleanup() throws Exception {
    org.apache.commons.io.FileUtils.cleanDirectory(store);
  }

  private void addColumn(Term term, String value) {
    ArchiveField f = new ArchiveField(fields.size(), term, null, ArchiveField.DataType.string);
    fields.add(f);
    values.add(value);
  }

  @Test
  public void testNonDwcProperties() throws Exception {

    addColumn(DwcTerm.taxonID, "1");
    addColumn(DwcTerm.scientificName, "Abies alba Mill., 1982");
    addColumn(DwcTerm.scientificNameAuthorship, "Mill.");
    addColumn(DwcTerm.namePublishedIn, "Führ. Pilzk. (Zwickau) 136 (1871)");
    addColumn(DwcTerm.namePublishedInID, "12345678");
    addColumn(DwcTerm.namePublishedInYear, "1871");
    addColumn(DwcTerm.nameAccordingTo, "Miller");
    addColumn(DwcTerm.nameAccordingToID, "123456");
    addColumn(DwcTerm.taxonRemarks, "bugger off");
    addColumn(DcTerm.references, "http://gbif.org");

    StarRecordImpl star = new StarRecordImpl(Lists.<Term>newArrayList());
    RecordImpl rec = new RecordImpl(fields.get(0), fields, DwcTerm.Taxon, true, true);
    rec.setRow(values.toArray(new String[] {}));
    star.newCoreRecord(rec);
    ins.insertStarRecord(star);
    ins.close();

    NeoMapper mapper = NeoMapper.instance();
    GraphDatabaseService db = new GraphDatabaseFactory()
      .newEmbeddedDatabaseBuilder(store.getAbsolutePath())
      .newGraphDatabase();

    Transaction tx = db.beginTx();
    Node n = db.getNodeById(0);
    NameUsageContainer u = mapper.read(n);
    assertEquals("1", u.getTaxonID());
    assertEquals("Abies alba Mill., 1982", u.getScientificName());
    assertEquals("Mill.", u.getAuthorship());
    assertEquals("Führ. Pilzk. (Zwickau) 136 (1871)", u.getPublishedIn());
    assertEquals("Miller", u.getAccordingTo());
    assertEquals("bugger off", u.getRemarks());
    assertEquals(URI.create("http://gbif.org"), u.getReferences());
    tx.close();
  }



  @Test
  public void testSetScientificName() throws Exception {

    VerbatimNameUsage v = new VerbatimNameUsage();
    v.setCoreField(DwcTerm.scientificName, "Abies alba Mill., 1982");
    assertName(v, Rank.SPECIES, "Abies alba Mill., 1982", "Abies alba", NameType.WELLFORMED);

    v = new VerbatimNameUsage();
    v.setCoreField(DwcTerm.scientificName, "Abies alba");
    assertName(v, Rank.SPECIES, "Abies alba", "Abies alba", NameType.WELLFORMED);

    v = new VerbatimNameUsage();
    v.setCoreField(DwcTerm.scientificName, "? alba");
    assertName(v, Rank.SPECIES, "? alba", null, NameType.DOUBTFUL);

    v = new VerbatimNameUsage();
    v.setCoreField(DwcTerm.genus, "Abies");
    v.setCoreField(DwcTerm.specificEpithet, "alba");
    assertName(v, Rank.SPECIES, "Abies alba", "Abies alba", NameType.WELLFORMED);

    v = new VerbatimNameUsage();
    v.setCoreField(DwcTerm.genus, "Abies");
    v.setCoreField(DwcTerm.infraspecificEpithet, "alpina");
    assertName(v, Rank.SUBSPECIES, "Abies subsp.", "Abies subsp.", NameType.WELLFORMED);

    v = new VerbatimNameUsage();
    v.setCoreField(DwcTerm.genus, "Abies");
    v.setCoreField(DwcTerm.specificEpithet, "alba");
    v.setCoreField(DwcTerm.infraspecificEpithet, "alpina");
    assertName(v, Rank.SUBSPECIES, "Abies alba subsp. alpina", "Abies alba alpina", NameType.WELLFORMED);

    v.setCoreField(DwcTerm.scientificNameAuthorship, "Duméril & Bibron");
    assertName(v, Rank.SUBSPECIES, "Abies alba subsp. alpina Duméril & Bibron", "Abies alba alpina", NameType.WELLFORMED);

    v.setCoreField(DwcTerm.namePublishedInYear, "1937");
    assertName(v, Rank.SUBSPECIES, "Abies alba subsp. alpina Duméril & Bibron, 1937", "Abies alba alpina", NameType.WELLFORMED);

    v.setCoreField(DwcTerm.scientificNameAuthorship, "Duméril & Bibron 1937");
    assertName(v, Rank.SUBSPECIES, "Abies alba subsp. alpina Duméril & Bibron 1937", "Abies alba alpina", NameType.WELLFORMED);
  }

  @Test(expected = IgnoreNameUsageException.class)
  public void testSetScientificNameExc() throws Exception {
    VerbatimNameUsage v = new VerbatimNameUsage();
    assertName(v, Rank.SPECIES, null, null, NameType.BLACKLISTED);
  }

  private NameUsageContainer assertName(VerbatimNameUsage v, Rank rank, String sciname, String canonical, NameType ntype)
    throws IgnoreNameUsageException {
    NameUsageContainer u = new NameUsageContainer();
    ins.setScientificName(u, v, rank);
    if (sciname != null) {
      assertEquals(sciname, u.getScientificName());
    } else {
      assertNull(u.getScientificName());
    }
    if (canonical != null) {
      assertEquals(canonical, u.getCanonicalName());
    } else {
      assertNull(u.getCanonicalName());
    }
    if (ntype != null) {
      assertEquals(ntype, u.getNameType());
    } else {
      assertNull(u.getNameType());
    }
    return u;
  }

  @Test
  public void testClean() throws Exception {
    assertNull(NeoInserter.clean(null));
    assertNull(NeoInserter.clean(" "));
    assertNull(NeoInserter.clean("   "));
    assertNull(NeoInserter.clean("\\N"));
    assertNull(NeoInserter.clean("NULL"));
    assertNull(NeoInserter.clean("\t "));

    assertEquals("Abies", NeoInserter.clean("Abies"));
    assertEquals("öAbies", NeoInserter.clean("öAbies"));
    assertEquals("Abies  mille", NeoInserter.clean(" Abies  mille"));
  }
}