package org.gbif.checklistbank.utils;

import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.vocabulary.Extension;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.IucnTerm;
import org.gbif.dwc.terms.Term;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VerbatimNameUsageMapperTest {

  @Test
  public void testSmile() throws Exception {
    VerbatimNameUsageMapper parser = new VerbatimNameUsageMapper();

    VerbatimNameUsage v = new VerbatimNameUsage();
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
    assertEquals(v, v2);

    long start = System.nanoTime();
    for (int x=1000; x>0; x--) {
      VerbatimNameUsage vx = parser.read(data);
    }
    long end = System.nanoTime();
    System.out.println(end - start);

    // mapper
    // 1259174000

    // reader & writer
    // 1247837000

    // ohne mixins
    // 1236704000

    // ohne smile
    // 1322153000
  }

  @Test
  public void testRoundTripping() throws Exception {
    VerbatimNameUsageMapper parser = new VerbatimNameUsageMapper();

    VerbatimNameUsage v = new VerbatimNameUsage();
    v.setCoreField(DwcTerm.scientificName, "Abies alba");
    v.setCoreField(DwcTerm.taxonRank, "Gattung");
    v.setCoreField(DwcTerm.taxonID, "dqwd23");

    List<Map<Term, String>> vernaculars = Lists.newArrayList();
    vernaculars.add((Map) ImmutableMap.of(DwcTerm.vernacularName, "Tanne", DcTerm.language, "de"));
    vernaculars.add((Map) ImmutableMap.of(DwcTerm.vernacularName, "Fir", DcTerm.language, "en"));
    v.getExtensions().put(Extension.VERNACULAR_NAME, vernaculars);

    List<Map<Term, String>> infos = Lists.newArrayList();
    infos.add((Map) ImmutableMap.of(GbifTerm.ageInDays, "750", IucnTerm.threatStatus, "extinct", GbifTerm.isExtinct, "true"));
    v.getExtensions().put(Extension.SPECIES_PROFILE, infos);

    byte[] data = parser.write(v);

    VerbatimNameUsage v2 = parser.read(data);
    assertEquals(v, v2);
  }
}
