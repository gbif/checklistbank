package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.vocabulary.Extension;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.utils.text.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VerbatimNameUsageMapperKryoTest {

    private VerbatimNameUsageMapper mapper = new VerbatimNameUsageMapperKryo();

    class Pair<X, Y> {
        public X x;
        public Y y;
    }

    public class VerbSerde implements Callable<Boolean> {
        Random rnd = new Random();

        @Override
        public Boolean call() throws Exception {
            int max = 1000 + rnd.nextInt(1000);
            System.out.println("Run " + max + " serde tests in new thread");
            List<Pair<VerbatimNameUsage, byte[]>> usages = Lists.newArrayList();
            for (int idx = 0; idx < max; idx++) {
                VerbatimNameUsage v = new VerbatimNameUsage();
                Pair<VerbatimNameUsage, byte[]> pair = new Pair<VerbatimNameUsage, byte[]>();
                pair.x = v;
                pair.y = mapper.write(v);
            }

            for (Pair<VerbatimNameUsage, byte[]> pair : usages) {
                VerbatimNameUsage v2 = mapper.read(pair.y);
                assertEquals(pair.x, v2);
            }
            return true;
        }
    }

    @Test
    public void testFixedInstance() throws Exception {
        VerbatimNameUsage v = new VerbatimNameUsage();
        for (Term t : DwcTerm.values()) {
            v.setCoreField(t, "bla bla bla");
        }
        v.setCoreField(DwcTerm.taxonID, "12345678");
        for (Term t : DcTerm.values()) {
            v.setCoreField(t, "1234");
        }
        List<Map<Term, String>> edata = Lists.newArrayList();
        for (int x=0; x<10; x++) {
            Map<Term, String> erec = Maps.newHashMap();
            erec.put(DcTerm.identifier, "id:" + x);
            erec.put(DcTerm.title, "My life");
            erec.put(DcTerm.description, "I was born, went to school, got a job, married, got 3 kids and died");
            edata.add(erec);
        }
        v.getExtensions().put(Extension.DESCRIPTION, edata);

        byte[] bytes = mapper.write(v);
        assertEquals(1331, bytes.length);
        assertEquals(mapper.read(bytes), v);
    }

    @Test
    public void testExisting() throws Exception {
        byte[] data = new byte[]{1, 14, 1, 2, 1, 15, 15, 1, 1, 14, 1, 2, 18, 1, 50, 3, 1, 67, 97, 110, 97, 100, 105, 97, 110, 32, 97, 110, 100, 32, 65, 108, 97, 115, 107, 97, 110, 32, 115, 112, 101, 99, 105, 101, -13, 18, 1, 9, 3, 1, -50, 3, 76, 97, 114, 115, 111, 110, 32, 68, 74, 44, 32, 65, 108, 97, 114, 105, 101, 32, 89, 44, 32, 82, 111, 117, 103, 104, 108, 101, 121, 32, 82, 69, 32, 40, 50, 48, 48, 48, 41, 32, 80, 114, 101, 100, 97, 99, 101, 111, 117, 115, 32, 100, 105, 118, 105, 110, 103, 32, 98, 101, 101, 116, 108, 101, 115, 32, 40, 67, 111, 108, 101, 111, 112, 116, 101, 114, 97, 58, 32, 68, 121, 116, 105, 115, 99, 105, 100, 97, 101, 41, 32, 111, 102, 32, 116, 104, 101, 32, 78, 101, 97, 114, 99, 116, 105, 99, 32, 82, 101, 103, 105, 111, 110, 44, 32, 119, 105, 116, 104, 32, 101, 109, 112, 104, 97, 115, 105, 115, 32, 111, 110, 32, 116, 104, 101, 32, 102, 97, 117, 110, 97, 32, 111, 102, 32, 67, 97, 110, 97, 100, 97, 32, 97, 110, 100, 32, 65, 108, 97, 115, 107, 97, 46, 32, 78, 82, 67, 32, 82, 101, 115, 101, 97, 114, 99, 104, 32, 80, 114, 101, 115, 115, 44, 32, 79, 116, 116, 97, 119, 97, 46, 32, 120, 105, 118, 32, 43, 32, 57, 56, 50, 32, 112, 112, 46, 1, 3, 15, 1, 0, 14, 1, 15, 17, 1, -103, 1, 1, 65, 110, 105, 109, 97, 108, 105, 97, 59, 65, 114, 116, 104, 114, 111, 112, 111, 100, 97, 59, 73, 110, 115, 101, 99, 116, 97, 59, 67, 111, 108, 101, 111, 112, 116, 101, 114, 97, 59, 65, 100, 101, 112, 104, 97, 103, 97, -69, 18, 1, 35, 1, 101, -18, 17, 1, -99, 1, 1, 67, 111, 108, 101, 111, 112, 116, 101, 114, -31, 17, 1, 12, 1, 49, 48, 46, 53, 56, 56, 54, 47, 57, 57, 56, 100, 98, 115, 50, -31, 17, 1, -93, 1, 1, 102, 97, 109, 105, 108, -7, 17, 1, -91, 1, 1, 76, 101, 97, 99, 104, 44, 32, 49, 56, 49, -75, 17, 1, -89, 1, 1, 73, 67, 90, -50, 17, 1, -110, 1, 1, 68, 121, 116, 105, 115, 99, 105, 100, 97, 101, 32, 76, 101, 97, 99, 104, 44, 32, 49, 56, 49, -75, 17, 1, -118, 1, 1, 49, 54, 52, -77, 18, 1, 46, 1, 79, 71, 76, 67, 32, 50, 46, 48, 44, 32, 104, 116, 116, 112, 58, 47, 47, 100, 97, 116, 97, 46, 103, 99, 46, 99, 97, 47, 101, 110, 103, 47, 111, 112, 101, 110, 45, 103, 111, 118, 101, 114, 110, 109, 101, 110, 116, 45, 108, 105, 99, 101, 110, 99, 101, 45, 99, 97, 110, 97, 100, -31, 17, 1, -98, 1, 1, 68, 121, 116, 105, 115, 99, 105, 100, 97, -27, 17, 1, -102, 1, 1, 65, 110, 105, 109, 97, 108, 105, -31, 17, 1, -101, 1, 1, 65, 114, 116, 104, 114, 111, 112, 111, 100, -31, 17, 1, -100, 1, 1, 73, 110, 115, 101, 99, 116, -31, 17, 1, 15, 1, -56, 1, 67, 104, 101, 99, 107, 108, 105, 115, 116, 32, 111, 102, 32, 66, 101, 101, 116, 108, 101, 115, 32, 40, 67, 111, 108, 101, 111, 112, 116, 101, 114, 97, 41, 32, 111, 102, 32, 67, 97, 110, 97, 100, 97, 32, 97, 110, 100, 32, 65, 108, 97, 115, 107, 97, 46, 32, 83, 101, 99, 111, 110, 100, 32, 69, 100, 105, 116, 105, 111, 110, 46, 0, 0};
        VerbatimNameUsage v = mapper.read(data);
        assertEquals((Integer)100155447, v.getKey());
        //assertEquals(mapper.read(bytes), v);
    }

    @Test
    public void testLargeRoundtrip() throws Exception {
        Random rnd  = new Random();
        VerbatimNameUsage v = new VerbatimNameUsage();
        for (Term t : DwcTerm.values()) {
            v.setCoreField(t, StringUtils.randomString(2 + rnd.nextInt(500)));
        }
        for (Term t : DcTerm.values()) {
            v.setCoreField(t, String.valueOf(rnd.nextInt()));
        }
        for (Extension ext : Extension.values()) {
            List<Map<Term, String>> edata = Lists.newArrayList();
            for (int x=rnd.nextInt(20); x<21; x++) {
                Map<Term, String> erec = Maps.newHashMap();
                erec.put(DcTerm.title, StringUtils.randomString(500));
                erec.put(DcTerm.description, StringUtils.randomString(25000));
                for (Term t : GbifTerm.values()) {
                    erec.put(t, StringUtils.randomString(rnd.nextInt(250)));
                }
                edata.add(erec);
            }
            v.getExtensions().put(ext, edata);
        }

        final byte[] bytes = mapper.write(v);
        assertTrue(bytes.length > 4096);

        VerbatimNameUsage v2 = mapper.read(bytes);
        assertEquals(v2, v);
    }

    @Test
    public void testThreadSafety() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        ExecutorCompletionService<Boolean> ecs = new ExecutorCompletionService(executor);
        List<Future<Boolean>> futures = Lists.newArrayList();

        for (int i = 0; i < 500; i++) {
            futures.add(ecs.submit(new VerbSerde()));
        }

        for (Future<Boolean> f : futures) {
            assertTrue(f.get());
        }
        System.out.println("Finished all threads successfully");
    }

}