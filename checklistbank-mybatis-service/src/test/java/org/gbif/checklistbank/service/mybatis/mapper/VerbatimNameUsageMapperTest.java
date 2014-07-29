package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.vocabulary.Extension;
import org.gbif.checklistbank.service.VerbatimNameUsageMapper;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.IucnTerm;
import org.gbif.dwc.terms.Term;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VerbatimNameUsageMapperTest {

  @Test
  @Ignore
  public void testSmile() throws Exception {
    VerbatimNameUsageMapper parser = new VerbatimNameUsageMapper();

    VerbatimNameUsage v = new VerbatimNameUsage();
    v.setCoreField(DwcTerm.scientificName, "Abies alba");

    System.out.println( parser.write(v) );
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
