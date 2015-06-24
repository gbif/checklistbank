package org.gbif.checklistbank.kryo;

import java.util.UUID;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Serializer that produces a null for any value upon read and does not write anything.
 */
public class NullSerializer<T> extends Serializer<T> {

    @Override
    public void write(final Kryo kryo, final Output output, final T obj) {

    }

    @Override
    public T read(final Kryo kryo, final Input input, final Class<T> theClass) {
        return null;
    }
}