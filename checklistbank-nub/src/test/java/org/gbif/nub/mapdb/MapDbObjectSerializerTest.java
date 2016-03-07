package org.gbif.nub.mapdb;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.nub.lookup.straight.LookupKryoFactory;
import org.gbif.nub.lookup.straight.LookupUsage;

import java.io.File;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import static org.junit.Assert.assertNotNull;

public class MapDbObjectSerializerTest {

  final int repeat = 100000;

  @Test
  public void testObjs() throws Exception {
    final File dbf = File.createTempFile("clb", "x");
    final DBMaker.Maker dbm = DBMaker
        .fileDB(dbf)
        .fileMmapEnableIfSupported()
        .transactionDisable();
    // warmup db & kryo
    DB db = dbm.make();
    Map<Long, LookupUsage> kvp = getMap(db);
    kvp.put(1l, usage(1));
    kvp.clear();

    // start
    long start = System.currentTimeMillis();
    for (long x = 0l; x < repeat; x++) {
      kvp.put(x, usage((int) x));
    }
    db.commit();
    logRate("write", start);
    db.close();

    // open again and read
    db = dbm.make();
    kvp = getMap(db);
    start = System.currentTimeMillis();
    for (long x = 0; x < repeat; x++) {
      LookupUsage u2 = kvp.get(x);
      assertNotNull("Not in map", u2);
    }
    db.commit();
    logRate("read", start);
    db.close();

    FileUtils.deleteQuietly(dbf);
  }

  private void logRate(String name, long start) {
    System.out.print(name);
    System.out.print(": ");
    long rate = repeat / (System.currentTimeMillis() - start);
    System.out.println(rate + "/ms");
  }

  private Map<Long, LookupUsage> getMap(DB db) {
    return db.hashMapCreate("usages")
        .keySerializer(Serializer.LONG)
        .valueSerializer(new MapDbObjectSerializer<LookupUsage>(LookupUsage.class, new LookupKryoFactory()))
        .makeOrGet();
  }

  public static LookupUsage usage(int key) {
    return usage(key, Rank.SPECIES);
  }

  public static LookupUsage usage(int key, Rank rank) {
    LookupUsage u = new LookupUsage();
    u.setKey(key);
    u.setAuthorship("Mill.");
    u.setCanonical("Abies alba");
    u.setDeleted(false);
    u.setKingdom(Kingdom.PLANTAE);
    u.setRank(rank);
    u.setYear("1867");
    return u;
  }
}