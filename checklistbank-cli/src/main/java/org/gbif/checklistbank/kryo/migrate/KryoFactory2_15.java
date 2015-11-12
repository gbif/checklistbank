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
 * Kryo factory as used in VerbatimNameUsageMapperKryo in release 2.15
 */
public class KryoFactory2_15 implements KryoFactory {

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