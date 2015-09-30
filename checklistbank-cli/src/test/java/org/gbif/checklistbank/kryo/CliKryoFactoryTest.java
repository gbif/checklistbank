package org.gbif.checklistbank.kryo;

import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.SrcUsage;

import java.io.ByteArrayOutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.junit.Test;

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

    private void assertSerde(Object obj) {

        ByteArrayOutputStream buffer = new ByteArrayOutputStream(64);
        Output output = new Output(buffer);
        kryo.writeObject(output, obj);
        output.close();
        byte[] bytes = buffer.toByteArray();

        final Input input = new Input(bytes);
        Object obj2 = kryo.readObject(input, obj.getClass());

        assertEquals(obj, obj2);
    }
}