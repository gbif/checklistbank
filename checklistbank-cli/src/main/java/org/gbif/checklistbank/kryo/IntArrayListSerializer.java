package org.gbif.checklistbank.kryo;

import com.carrotsearch.hppc.IntArrayList;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Created by markus on 30/09/15.
 */
public class IntArrayListSerializer extends Serializer<IntArrayList> {

    public IntArrayListSerializer() {
        super(true, true);
    }

    @Override
    public void write(Kryo kryo, Output output, IntArrayList object) {
        //output.writeInt(object.size(), true);
        kryo.writeObject(output, object.toArray());
    }

    @Override
    public IntArrayList read(Kryo kryo, Input input, Class<IntArrayList> type) {
        //final int size = input.readInt(true);
        int[] data = kryo.readObject(input, int[].class);
        return IntArrayList.from(data);
    }

}