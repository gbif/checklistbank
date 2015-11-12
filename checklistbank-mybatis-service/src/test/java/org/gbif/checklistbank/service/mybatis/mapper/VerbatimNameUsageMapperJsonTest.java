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

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VerbatimNameUsageMapperJsonTest {
  final URI qname1 = URI.create("http://it.was/not/me");
  final URI qname2 = URI.create("http://ditch.me/null");

  @Test
  public void testRoundTripping() throws Exception {
    VerbatimNameUsageMapperJson parser = new VerbatimNameUsageMapperJson();

    VerbatimNameUsage v = new VerbatimNameUsage();
    v.setCoreField(DwcTerm.scientificName, "Abies alba");
    v.setCoreField(DwcTerm.taxonRank, "Gattung");
    v.setCoreField(DwcTerm.taxonID, "dqwd23");
    v.setCoreField(GbifTerm.depth, "1200");
    v.setCoreField(new UnknownTerm(qname1), "1200");
    v.setCoreField(new UnknownTerm(qname2), null);

    List<Map<Term, String>> vernaculars = Lists.newArrayList();
    vernaculars.add(map(DwcTerm.vernacularName, "Tanne", DcTerm.language, "de"));
    vernaculars.add(map(DwcTerm.vernacularName, "Fir", DcTerm.language, "en", new UnknownTerm(qname2), ""));
    v.getExtensions().put(Extension.VERNACULAR_NAME, vernaculars);

    List<Map<Term, String>> infos = Lists.newArrayList();
    infos.add(map(GbifTerm.ageInDays, "750", IucnTerm.threatStatus, "extinct", GbifTerm.isExtinct, "true"));
    v.getExtensions().put(Extension.SPECIES_PROFILE, infos);

    String json = parser.write(v);

    VerbatimNameUsage v2 = parser.read(json);
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
