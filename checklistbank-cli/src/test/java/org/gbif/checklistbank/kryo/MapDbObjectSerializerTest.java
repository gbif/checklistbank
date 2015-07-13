package org.gbif.checklistbank.kryo;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;

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
        Map<Long, NameUsage> kvp = getMap(db);
        kvp.put(1l, usage(1));
        kvp.clear();

        // start
        long start = System.currentTimeMillis();
        for (long x=0l; x< repeat; x++) {
            kvp.put(x, usage((int) x));
        }
        db.commit();
        logRate("write", start);
        db.close();

        // open again and read
        db = dbm.make();
        kvp = getMap(db);
        start = System.currentTimeMillis();
        for (long x=0; x<repeat; x++) {
            NameUsage u2 = kvp.get(x);
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
        long rate = repeat / (System.currentTimeMillis()-start);
        System.out.println(rate + "/ms");
    }

    private Map<Long, NameUsage> getMap(DB db) {
        return db.hashMapCreate("usages")
                .keySerializer(Serializer.LONG)
                .valueSerializer(new MapDbObjectSerializer<NameUsage>(NameUsage.class))
                .makeOrGet();
    }

    public static NameUsage usage(int key) {
        return usage(key, Rank.SPECIES);
    }

    public static NameUsage usage(int key, Rank rank) {
        NameUsage u = new NameUsage();
        u.setKey(key);
        u.setKingdomKey(key);
        u.setParentKey(key);
        u.setAcceptedKey(key);
        u.setScientificName("Abies alba Mill.");
        u.setCanonicalName("Abies alba");
        u.setRank(rank);
        u.setTaxonomicStatus(TaxonomicStatus.ACCEPTED);
        return u;
    }
}