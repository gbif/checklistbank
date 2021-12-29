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
package org.gbif.nub.mapdb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.NotImplementedException;
import org.mapdb.DataIO;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.serializer.GroupSerializerObjectArray;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;

/**
 * A mapDB serializer that uses kryo under the hood to quickly serialize objects into the mapdb data output/input.
 * @param <T> the class to serialize
 */
public class MapDbObjectSerializer<T> extends GroupSerializerObjectArray<T> {
    private final KryoPool pool;
    private final int bufferSize;
    private final Class<T> clazz;

  public MapDbObjectSerializer(Class<T> clazz, KryoFactory kryoFactory) {
    this(clazz, new KryoPool.Builder(kryoFactory).softReferences().build(), 256);
  }

  public MapDbObjectSerializer(Class<T> clazz, KryoPool pool, int bufferSize) {
        this.pool = pool;
        this.clazz = clazz;
        this.bufferSize = bufferSize;
    }

  @Override
  public void serialize(@NotNull DataOutput2 out, @NotNull T value) throws IOException {
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
  public T deserialize(@NotNull DataInput2 in, int available) throws IOException {
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

  @Override
  public int compare(T first, T second) {
    throw new NotImplementedException("compare should not be needed for our mapdb use");
  }

}
