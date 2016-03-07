package org.gbif.nub.mapdb;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import org.mapdb.DataIO;
import org.mapdb.Serializer;

/**
 * A mapDB serializer that uses kryo under the hood to quickly serialize objects into the mapdb data output/input.
 * @param <T> the class to serialize
 */
public class MapDbObjectSerializer<T> extends Serializer<T> {
    private final KryoPool pool;
    private final int bufferSize;
    private final Class<T> clazz;

  public MapDbObjectSerializer(Class<T> clazz, KryoFactory kryoFactory) {
    this.clazz = clazz;
    this.bufferSize = 256;
    this.pool = new KryoPool.Builder(kryoFactory)
        .softReferences()
        .build();
  }

  public MapDbObjectSerializer(Class<T> clazz, KryoPool pool, int bufferSize) {
        this.pool = pool;
        this.clazz = clazz;
        this.bufferSize = bufferSize;
    }

    @Override
    public void serialize(DataOutput out, T value) throws IOException {
        Kryo kryo = pool.borrow();
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(bufferSize);
            Output output = new Output(buffer, bufferSize);
            kryo.writeObject(output, value);
            output.close();
            byte[] bytes = buffer.toByteArray();
            DataIO.packInt(out, bytes.length);
            out.write(bytes);
        } finally {
            pool.release(kryo);
        }
    }

    @Override
    public T deserialize(DataInput in, int available) throws IOException {
        if(available==0) return null;
        Kryo kryo = pool.borrow();
        try {
            int size = DataIO.unpackInt(in);
            byte[] ret = new byte[size];
            in.readFully(ret);
            return kryo.readObject(new Input(ret), clazz);
        } finally {
            pool.release(kryo);
        }
    }

    @Override
    public boolean isTrusted() {
        return true;
    }

}
