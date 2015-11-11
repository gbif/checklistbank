package org.gbif.checklistbank.kryo.migrate;

import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.vocabulary.Extension;
import org.gbif.checklistbank.kryo.TermSerializer;
import org.gbif.dwc.terms.AcTerm;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.UnknownTerm;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoFactory;

/**
 * Creates a kryo factory usable for thread safe kryo pools that can deal with clb api classes.
 * We use Kryo for extremely fast byte serialization of objects:
 * 1) We store in postgres a byte array for the verbatim usage instances.
 * 2) We use kryo to serialize various information in kvp stores during checklist indexing and nub builds.
 *
 * The serialization format of kryo stays the same over minor version changes, so we do not need to reindex checklists
 * just because we update the kryo library. Make sure to not update to an incompatible format change, see kryo changes logs:
 * https://github.com/EsotericSoftware/kryo/blob/master/CHANGES.md
 *
 * CAUTION! We require registration of all classes that kryo should be able to handle.
 * This registration reduces the resulting binary size and improves performance,
 * BUT the registered integers must stay the same over time or otherwise existing data in unreadable.
 */
public class ClbKryoFactory4 implements KryoFactory {

    @Override
    public Kryo create() {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        kryo.register(VerbatimNameUsage.class);
        kryo.register(Date.class);
        kryo.register(HashMap.class);
        kryo.register(ArrayList.class);
        kryo.register(Extension.class);
        kryo.register(DwcTerm.class);
        kryo.register(DcTerm.class);
        kryo.register(GbifTerm.class);
        kryo.register(AcTerm.class);
        TermSerializer ts = new TermSerializer();
        kryo.register(UnknownTerm.class, ts);
        kryo.register(Term.class, ts);
        return kryo;

    }
}