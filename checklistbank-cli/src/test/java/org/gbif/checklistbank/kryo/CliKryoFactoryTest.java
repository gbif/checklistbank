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
package org.gbif.checklistbank.kryo;

import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMediaObject;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.checklistbank.Reference;
import org.gbif.api.model.checklistbank.SpeciesProfile;
import org.gbif.api.model.checklistbank.TypeSpecimen;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.common.Identifier;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.dwc.terms.*;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.Lists;

import static org.junit.Assert.assertEquals;

public class CliKryoFactoryTest {
    Kryo kryo = new CliKryoFactory().create();

    @Test
    public void testNubUsage() throws Exception {
        NubUsage u = new NubUsage();
        u.addNomStatus(NomenclaturalStatus.values());
        assertSerde(u);
    }

    @Test
    public void testSrcUsage() throws Exception {
        SrcUsage u = new SrcUsage();
        assertSerde(u);
    }

    @Test
    public void testTerms() throws Exception {
        List<Term> terms = Lists.newArrayList(
                DwcTerm.scientificName, DwcTerm.associatedOrganisms, DwcTerm.taxonID,
                DcTerm.title,
                GbifTerm.canonicalName,
                AcefTerm.Author,
                IucnTerm.threatStatus,
                new UnknownTerm(URI.create("http://gbif.org/abcdefg"), false)
        );
        assertSerde(terms);
    }

    @Test
    public void testEmptyModels() throws Exception {
        assertSerde(new NameUsage());
        assertSerde(new VerbatimNameUsage());
        assertSerde(new NameUsageMetrics());
        assertSerde(new UsageExtensions());
        assertSerde(new ParsedName());
        assertSerde(new Description());
        assertSerde(new Distribution());
        assertSerde(new Identifier());
        assertSerde(new NameUsageMediaObject());
        assertSerde(new Reference());
        assertSerde(new SpeciesProfile());
        assertSerde(new NameUsage());
        assertSerde(new TypeSpecimen());
        assertSerde(new VernacularName());
        assertSerde(new DatasetMetrics());
    }

    private void assertSerde(Object obj) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(128);
        Output output = new Output(buffer);
        kryo.writeObject(output, obj);
        output.close();
        byte[] bytes = buffer.toByteArray();

        final Input input = new Input(bytes);
        Object obj2 = kryo.readObject(input, obj.getClass());

        assertEquals(obj, obj2);
    }

}