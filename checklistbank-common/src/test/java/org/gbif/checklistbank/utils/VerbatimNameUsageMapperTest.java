package org.gbif.checklistbank.utils;

import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.vocabulary.Extension;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.IucnTerm;
import org.gbif.dwc.terms.Term;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VerbatimNameUsageMapperTest {

  @Test
  public void testSmile() throws Exception {
    VerbatimNameUsageMapper parser = new VerbatimNameUsageMapperJson();

    VerbatimNameUsage v = new VerbatimNameUsage();
    v.setKey(4712681);
    v.setLastCrawled(new Date());
    for (DwcTerm t : DwcTerm.values()) {
      v.setCoreField(t, RandomStringUtils.random(20));
    }
    v.getExtensions().put(Extension.DESCRIPTION, Lists.<Map<Term, String>>newArrayList());
    for (int i=0; i < 6; i++) {
      Map<Term, String> rec = Maps.newHashMap();
      rec.put(DcTerm.identifier, RandomStringUtils.random(10));
      rec.put(DcTerm.type, "description");
      rec.put(DcTerm.description, RandomStringUtils.random(1000));
      v.getExtensions().get(Extension.DESCRIPTION).add(rec);
    }

    byte[] data = parser.write(v);
    VerbatimNameUsage v2 = parser.read(data);
//    assertEquals(v, v2);

    long start = System.nanoTime();
    for (int x=10000; x>0; x--) {
      VerbatimNameUsage vx = parser.read(data);
    }
    long end = System.nanoTime();
    System.out.println(end - start);

    // json
    // 11746862000

    // smile
    // 11255459000

    // kryo
    // 1163882000
  }

  @Test
  public void testRoundTripping() throws Exception {
    VerbatimNameUsageMapper parser = new VerbatimNameUsageMapperKryo();

    VerbatimNameUsage v = new VerbatimNameUsage();
    v.setKey(4712681);
    v.setLastCrawled(new Date());
    v.setCoreField(DwcTerm.scientificName, "Abies alba");
    v.setCoreField(DwcTerm.taxonRank, "Gattung");
    v.setCoreField(DwcTerm.taxonID, "dqwd23");

    List<Map<Term, String>> vernaculars = Lists.newArrayList();
    vernaculars.add(map(DwcTerm.vernacularName, "Tanne", DcTerm.language, "de"));
    vernaculars.add(map(DwcTerm.vernacularName, "Fir", DcTerm.language, "en"));
    v.getExtensions().put(Extension.VERNACULAR_NAME, vernaculars);

    List<Map<Term, String>> infos = Lists.newArrayList();
    infos.add(map(GbifTerm.ageInDays, "750", IucnTerm.threatStatus, "extinct", GbifTerm.isExtinct, "true"));
    v.getExtensions().put(Extension.SPECIES_PROFILE, infos);

    byte[] data = parser.write(v);

    VerbatimNameUsage v2 = parser.read(data);
    assertEquals(v, v2);
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
