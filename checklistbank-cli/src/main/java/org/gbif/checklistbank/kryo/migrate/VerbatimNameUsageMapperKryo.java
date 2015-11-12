package org.gbif.checklistbank.kryo.migrate;

import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.vocabulary.Extension;
import org.gbif.checklistbank.kryo.ImmutableListSerializer;
import org.gbif.checklistbank.kryo.TermSerializer;
import org.gbif.dwc.terms.AcTerm;
import org.gbif.dwc.terms.DcElement;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.EolReferenceTerm;
import org.gbif.dwc.terms.GbifInternalTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.IucnTerm;
import org.gbif.dwc.terms.UnknownTerm;
import org.gbif.dwc.terms.XmpRightsTerm;
import org.gbif.dwc.terms.XmpTerm;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.google.common.collect.ImmutableList;

/**
 * Serializing/Deserializing tool specifically for the term maps of a VerbatimNameUsage to be stored in postgres
 * or neo backends as a single binary column.
 */
public class VerbatimNameUsageMapperKryo {

    private static final int BUFFER_SIZE = 4096;
    // kryo is not thread safe, see https://github.com/EsotericSoftware/kryo#threading
    private final KryoPool pool;

    public VerbatimNameUsageMapperKryo() {
        this(new VerbatimKryoFactory());
    }

    public VerbatimNameUsageMapperKryo(KryoFactory factory) {
        // Build pool with SoftReferences enabled (optional)
        pool = new KryoPool.Builder(factory).softReferences().build();
    }

    public static class VerbatimKryoFactory implements KryoFactory {

        @Override
        public Kryo create() {
            Kryo kryo = new Kryo();
            kryo.setRegistrationRequired(true);
            // verbatim class
            kryo.register(VerbatimNameUsage.class, 1);
            // java
            kryo.register(Date.class, 2);
            kryo.register(HashMap.class, 3);
            kryo.register(ArrayList.class, 4);
            kryo.register(ImmutableList.class, new ImmutableListSerializer(), 5);
            // term enums
            kryo.register(AcTerm.class, 6);
            kryo.register(DcElement.class, 7);
            kryo.register(DcTerm.class, 8);
            kryo.register(DwcTerm.class, 9);
            kryo.register(EolReferenceTerm.class, 10);
            kryo.register(GbifInternalTerm.class, 11);
            kryo.register(GbifTerm.class, 12);
            kryo.register(IucnTerm.class, 13);
            kryo.register(XmpRightsTerm.class, 14);
            kryo.register(XmpTerm.class, 15);
            kryo.register(UnknownTerm.class, new TermSerializer(), 16);
            // vocabs
            kryo.register(Extension.class, 17);

            return kryo;
        }
    }

    public VerbatimNameUsage read(byte[] bytes) {
        if (bytes != null) {
            Kryo kryo = pool.borrow();
            try {
                final Input input = new Input(bytes);
                return kryo.readObject(input, VerbatimNameUsage.class);
            } finally {
                pool.release(kryo);
            }
        }
        return null;
    }

    public byte[] write(VerbatimNameUsage verbatim) {
        if (verbatim != null) {
            Kryo kryo = pool.borrow();
            try {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream(BUFFER_SIZE);
                final Output output = new Output(buffer, BUFFER_SIZE);
                kryo.writeObject(output, verbatim);
                output.flush();
                return buffer.toByteArray();
            } finally {
                pool.release(kryo);
            }
        }
        return null;
    }
}
