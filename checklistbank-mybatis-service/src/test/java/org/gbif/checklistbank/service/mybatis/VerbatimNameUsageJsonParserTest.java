package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.vocabulary.Extension;
import org.gbif.dwc.terms.DwcTerm;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class VerbatimNameUsageJsonParserTest {
    private VerbatimNameUsageJsonParser parser = new VerbatimNameUsageJsonParser();

    @Test
    public void testJsonToVerbatim() throws Exception {
        final String json = "{\"id\":\"100\",\n" + " \"taxonomicStatus\":\"valid\",\n" + " \"taxonRank\":\"Species\",\n"
            + " \"scientificNameAuthorship\":null,\n" + " \"parentNameUsageID\":\"86\",\n"
            + " \"acceptedNameUsageID\":null,\n" + " \"scientificName\":\"Spirillum beijerinckii\",\n"
            + " \"extensions\": {\n"
            + "   \"VernacularName\" : [{\"vernacularName\":\"Strand\", \"language\":\"en\"}, {\"vernacularName\":\"Bönse\", \"language\":\"se\"}],\n"
            + "   \"Distribution\" : [{\"hi\":\"ho\"}]\n" + " }}";
        VerbatimNameUsageJsonParser parser = new VerbatimNameUsageJsonParser();
        VerbatimNameUsage v = parser.toVerbatimOld(json);

        assertEquals("valid", v.getFields().get(DwcTerm.taxonomicStatus));
        assertEquals("Spirillum beijerinckii", v.getFields().get(DwcTerm.scientificName));
        assertEquals("86", v.getFields().get(DwcTerm.parentNameUsageID));
        assertEquals(2, v.getExtensions().get(Extension.VERNACULAR_NAME).size());
        assertEquals(1, v.getExtensions().get(Extension.DISTRIBUTION).size());
    }

    @Test
    public void testToJson() throws Exception {
        VerbatimNameUsage v = new VerbatimNameUsage();
        v.setLastCrawled(new Date());
        v.setKey(132);
        v.setCoreField(DwcTerm.scientificName, "Abies");
        v.setCoreField(DwcTerm.taxonRank, "Gattung");
        v.setCoreField(DwcTerm.taxonID, "dqwd23");

        String json = parser.toJson(v);
        System.out.println(json);
    }

    @Test
    public void testToVerbatim() throws Exception {
        String json = "{\"extensions\":{},\"http://rs.tdwg.org/dwc/terms/taxonRank\":\"Gattung\",\"http://rs.tdwg.org/dwc/terms/scientificName\":\"Abies\",\"http://rs.tdwg.org/dwc/terms/taxonID\":\"dqwd23\"}";
        VerbatimNameUsage v = parser.toVerbatim(json);
        System.out.println(v);
        assertEquals("Abies", v.getCoreField(DwcTerm.scientificName));
        assertEquals("Gattung", v.getCoreField(DwcTerm.taxonRank));
        assertEquals("dqwd23", v.getCoreField(DwcTerm.taxonID));
    }

    @Test
    public void testRoundTripping() throws Exception {
        VerbatimNameUsage v = new VerbatimNameUsage();
        v.setCoreField(DwcTerm.scientificName, "Abies");
        v.setCoreField(DwcTerm.taxonRank, "Gattung");
        v.setCoreField(DwcTerm.taxonID, "dqwd23");

        String json = parser.toJson(v);

        VerbatimNameUsage v2 = parser.toVerbatim(json);
        System.out.println(v2);
        assertEquals(v, v2);
    }
}
