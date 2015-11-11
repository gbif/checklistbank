package org.gbif.checklistbank.kryo;

import org.gbif.checklistbank.cli.model.ClassificationKeys;
import org.gbif.checklistbank.cli.model.UsageFacts;
import org.gbif.checklistbank.nub.lookup.LookupUsage;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.SrcUsage;

import com.carrotsearch.hppc.IntArrayList;
import com.esotericsoftware.kryo.Kryo;
import org.neo4j.kernel.impl.core.NodeProxy;


/**
 * Extended version of the clb one that also knows about cli specific classes.
 * Uses registered class id range from 100-127.
 */
public class CliKryoFactory extends ClbKryoFactory {
    @Override
    public Kryo create() {
        Kryo kryo = super.create();
        kryo.register(LookupUsage.class, 100);
        kryo.register(NubUsage.class, 101);
        kryo.register(UsageFacts.class, 102);
        kryo.register(ClassificationKeys.class, 103);
        kryo.register(SrcUsage.class, 104);
        kryo.register(IntArrayList.class, new IntArrayListSerializer(), 105);
        kryo.register(int[].class, 106);
        // ignore neo node proxies and set them to null upon read:
        kryo.register(NodeProxy.class, new NullSerializer(), 107);
        return kryo;
    }
}
