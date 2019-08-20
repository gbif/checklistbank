package org.gbif.nub.lookup.straight;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.nub.mapdb.ImmutableListSerializer;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoFactory;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

/**
 * A kryo factory that knows how to serde the LookupUsage class
 */
public class LookupKryoFactory implements KryoFactory {

  @Override
  public Kryo create() {
    Kryo kryo = new Kryo();
    kryo.setRegistrationRequired(true);

    // model class(es)
    kryo.register(LookupUsage.class);

    // fastutils
    kryo.register(Int2IntArrayMap.class);
    kryo.register(Int2IntOpenHashMap.class);

    // java & commons
    kryo.register(Date.class);
    kryo.register(HashMap.class);
    kryo.register(HashSet.class);
    kryo.register(ArrayList.class);
    ImmutableListSerializer.registerSerializers(kryo);

    // enums
    kryo.register(Rank.class);
    kryo.register(TaxonomicStatus.class);
    kryo.register(Kingdom.class);

    return kryo;
  }
}
