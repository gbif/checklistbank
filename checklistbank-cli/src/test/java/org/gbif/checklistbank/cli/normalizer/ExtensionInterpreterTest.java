/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.cli.normalizer;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.vocabulary.Extension;
import org.gbif.api.vocabulary.Language;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.dwc.terms.*;

import java.net.URI;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExtensionInterpreterTest {

    @Test
    public void testExpandGenus() throws Exception {
        assertEquals(null, ExtensionInterpreter.expandGenus(null, "Abies alba"));
        assertEquals("", ExtensionInterpreter.expandGenus("", "Abies"));

        assertEquals("Abies alba", ExtensionInterpreter.expandGenus("Abies alba", "Abies negra"));
        assertEquals("Abies alba", ExtensionInterpreter.expandGenus("Abies alba", null));
        assertEquals("Abies alba", ExtensionInterpreter.expandGenus("A alba", "Abies negra"));
        assertEquals("Abies alba", ExtensionInterpreter.expandGenus("A. alba", "Abies negra"));
        assertEquals("Abies alba", ExtensionInterpreter.expandGenus("A.  alba", "Abies negra"));
    }

    @Test
    public void testEolMedia() throws Exception {
        ExtensionInterpreter interp = new ExtensionInterpreter();
        NameUsage u = new NameUsage();
        VerbatimNameUsage v = new VerbatimNameUsage();

        Map<Term, String> doc1 = Maps.newHashMap();
        doc1.put(DcTerm.type, "http://purl.org/dc/dcmitype/StillImage");
        doc1.put(DcTerm.format, "image/jpeg");
        doc1.put(DcTerm.language, "en");
        doc1.put(DcTerm.identifier, "1013-sp1-Image-1");
        doc1.put(DwcTerm.taxonID, "1013-sp1");
        doc1.put(AcTerm.accessURI, "http://pwt.pensoft.net//showfigure.php?filename=big_4805.jpg");
        doc1.put(AcTerm.furtherInformationURL, "http://dx.doi.org/10.3897/BDJ.1.e1013");

        Map<Term, String> doc2 = Maps.newHashMap();
        doc2.put(DcTerm.type, "http://purl.org/dc/dcmitype/Text");
        doc2.put(DcTerm.format, "text/html");
        doc2.put(DcTerm.language, "en");
        doc2.put(DcTerm.identifier, "1013-sp1-text-3");
        doc2.put(DwcTerm.taxonID, "1013-sp1");
        doc2.put(DcTerm.description, "Eupolybothrus cavernicolus is so far known only from the caves Miljacka");
        doc2.put(DcTerm.title, "Description of the type locality");
        doc2.put(AcTerm.furtherInformationURL, "http://dx.doi.org/10.3897/BDJ.1.e1013");
        doc2.put(XmpRightsTerm.UsageTerms, "http://creativecommons.org/licenses/by/4.0/");

        v.getExtensions().put(Extension.EOL_MEDIA, Lists.newArrayList(doc1, doc2));

        UsageExtensions ext = interp.interpret(u, v);
        assertEquals(1, ext.media.size());
        assertEquals(URI.create("http://pwt.pensoft.net//showfigure.php?filename=big_4805.jpg"), ext.media.get(0).getIdentifier());
        assertEquals(URI.create("http://dx.doi.org/10.3897/BDJ.1.e1013"), ext.media.get(0).getReferences());

        assertEquals(1, ext.descriptions.size());
        assertEquals("Description of the type locality", ext.descriptions.get(0).getType());
        assertEquals("http://creativecommons.org/licenses/by/4.0/", ext.descriptions.get(0).getLicense());
        assertEquals("Eupolybothrus cavernicolus is so far known only from the caves Miljacka", ext.descriptions.get(0).getDescription());
    }

  @Test
  public void testVernacularNames() throws Exception {
    ExtensionInterpreter interp = new ExtensionInterpreter();
    NameUsage u = new NameUsage();
    VerbatimNameUsage v = new VerbatimNameUsage();

    Map<Term, String> v1 = Maps.newHashMap();
    v1.put(DwcTerm.vernacularName, "palmeirinha");
    v1.put(DcTerm.language, "PORTUGUES");
    v1.put(DwcTerm.locality, "Acre");

    Map<Term, String> v2 = Maps.newHashMap();
    v2.put(DwcTerm.vernacularName, "ubim");
    v2.put(DcTerm.language, "PORTUGUES");
    v2.put(DwcTerm.locality, "Acre");

    v.getExtensions().put(Extension.VERNACULAR_NAME, Lists.newArrayList(v1, v2));

    UsageExtensions ext = interp.interpret(u, v);
    assertEquals(2, ext.vernacularNames.size());
    assertTrue(u.getIssues().isEmpty());
    assertEquals("palmeirinha", ext.vernacularNames.get(0).getVernacularName());
    assertEquals(Language.PORTUGUESE, ext.vernacularNames.get(0).getLanguage());
    assertEquals("Acre", ext.vernacularNames.get(0).getArea());
  }
}