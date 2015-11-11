package org.gbif.checklistbank.service.mybatis.mapper;

import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.checklistbank.kryo.ClbKryoFactory;

import java.io.ByteArrayOutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serializing/Deserializing tool specifically for the term maps of a VerbatimNameUsage to be stored in postgres
 * or neo backends as a single binary column.
 */
public class VerbatimNameUsageMapperKryo implements VerbatimNameUsageMapper {

    private static final Logger LOG = LoggerFactory.getLogger(VerbatimNameUsageMapperKryo.class);
    private static final int BUFFER_SIZE = 4096;
    // kryo is not thread safe, see https://github.com/EsotericSoftware/kryo#threading
    private final KryoPool pool;

    public VerbatimNameUsageMapperKryo() {
        this(new ClbKryoFactory());
    }

    public VerbatimNameUsageMapperKryo(KryoFactory factory) {
        // Build pool with SoftReferences enabled (optional)
        pool = new KryoPool.Builder(factory).softReferences().build();
    }

    @Override
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

    @Override
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
