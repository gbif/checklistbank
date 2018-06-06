package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.vocabulary.Extension;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.IucnTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.UnknownTerm;

import java.net.URI;
import java.util.HashMap;
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

public class VerbatimNameUsageMapperJsonTest {
  final URI qname1 = URI.create("http://it.was/not/me");
  final URI qname2 = URI.create("http://ditch.me/null");

  private final VerbatimNameUsageMapperJson mapper = new VerbatimNameUsageMapperJson();

  @Test
  public void testRoundTripping() throws Exception {
    VerbatimNameUsage v = new VerbatimNameUsage();
    v.setCoreField(DwcTerm.scientificName, "Abies alba");
    v.setCoreField(DwcTerm.taxonRank, "Gattung");
    v.setCoreField(DwcTerm.taxonID, "dqwd23");
    v.setCoreField(GbifTerm.depth, "1200");
    v.setCoreField(new UnknownTerm(qname1, false), "1200");
    v.setCoreField(new UnknownTerm(qname2, false), null);

    List<Map<Term, String>> vernaculars = Lists.newArrayList();
    vernaculars.add(map(DwcTerm.vernacularName, "Tanne", DcTerm.language, "de"));
    vernaculars.add(map(DwcTerm.vernacularName, "Fir", DcTerm.language, "en", new UnknownTerm(qname2, false), ""));
    v.getExtensions().put(Extension.VERNACULAR_NAME, vernaculars);

    List<Map<Term, String>> infos = Lists.newArrayList();
    infos.add(map(GbifTerm.ageInDays, "750", IucnTerm.threatStatus, "extinct", GbifTerm.isExtinct, "true"));
    v.getExtensions().put(Extension.SPECIES_PROFILE, infos);

    String json = mapper.write(v);

    VerbatimNameUsage v2 = mapper.read(json);
    assertEquals(v, v2);
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

    String json = mapper.write(v);
    assertEquals(mapper.read(json), v);
  }

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
      List<Pair<VerbatimNameUsage, String>> usages = Lists.newArrayList();
      for (int idx = 0; idx < max; idx++) {
        VerbatimNameUsage v = new VerbatimNameUsage();
        Pair<VerbatimNameUsage, String> pair = new Pair<VerbatimNameUsage, String>();
        pair.x = v;
        pair.y = mapper.write(v);
      }

      for (Pair<VerbatimNameUsage, String> pair : usages) {
        VerbatimNameUsage v2 = mapper.read(pair.y);
        assertEquals(pair.x, v2);
      }
      return true;
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

  private static Map<Term,String> map(Term key, String value, Term key2, String value2) {
    Map<Term,String> map = new HashMap<Term,String>();
    map.put(key, value);
    map.put(key2, value2);
    return map;
  }

  private static Map<Term,String> map(Term key, String value, Term key2, String value2, Term key3, String value3) {
    Map<Term,String> map = map(key, value, key2, value2);
    map.put(key3, value3);
    return map;
  }
}
