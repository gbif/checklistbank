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