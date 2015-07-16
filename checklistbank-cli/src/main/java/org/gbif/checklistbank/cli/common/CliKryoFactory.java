package org.gbif.checklistbank.cli.common;

import org.gbif.checklistbank.kryo.ClbKryoFactory;
import org.gbif.checklistbank.kryo.NullSerializer;
import org.gbif.checklistbank.cli.model.ClassificationKeys;
import org.gbif.checklistbank.cli.model.UsageFacts;
import org.gbif.checklistbank.nub.lookup.LookupUsage;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.SrcUsage;

import com.esotericsoftware.kryo.Kryo;
import org.neo4j.kernel.impl.core.NodeProxy;


/**
 * Extended version of the clb one that also knows about cli specific classes.
 */
public class CliKryoFactory extends ClbKryoFactory {
    @Override
    public Kryo create() {
        Kryo kryo = super.create();
        kryo.register(LookupUsage.class);
        kryo.register(NubUsage.class);
        kryo.register(UsageFacts.class);
        kryo.register(ClassificationKeys.class);
        kryo.register(SrcUsage.class);
        // ignore those classes and set them to null upon read:
        NullSerializer devNull = new NullSerializer();
        kryo.register(NodeProxy.class, devNull);
        return kryo;
    }
}
