package org.gbif.checklistbank.kryo;

import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.Distribution;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMediaObject;
import org.gbif.api.model.checklistbank.NameUsageMetrics;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.checklistbank.Reference;
import org.gbif.api.model.checklistbank.SpeciesProfile;
import org.gbif.api.model.checklistbank.TypeSpecimen;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.model.checklistbank.VernacularName;
import org.gbif.api.model.common.Identifier;
import org.gbif.api.vocabulary.CitesAppendix;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.EstablishmentMeans;
import org.gbif.api.vocabulary.Extension;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.LifeStage;
import org.gbif.api.vocabulary.MediaType;
import org.gbif.api.vocabulary.NamePart;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.OccurrenceStatus;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.Sex;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.api.vocabulary.ThreatStatus;
import org.gbif.api.vocabulary.TypeDesignationType;
import org.gbif.api.vocabulary.TypeStatus;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.dwc.terms.AcTerm;
import org.gbif.dwc.terms.DcElement;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.EolReferenceTerm;
import org.gbif.dwc.terms.GbifInternalTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.IucnTerm;
import org.gbif.dwc.terms.UnknownTerm;
import org.gbif.dwc.terms.XmpRightsTerm;
import org.gbif.dwc.terms.XmpTerm;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoFactory;

/**
 * Creates a kryo factory usable for thread safe kryo pools that can deal with clb api classes.
 * We use Kryo for extremely fast byte serialization of objects:
 * 1) We store in postgres a byte array for the verbatim usage instances.
 * 2) We use kryo to serialize various information in kvp stores during checklist indexing and nub builds.
 *
 * The serialization format of kryo stayes the same over minor version changes, so we do not need to reindex checklists
 * just because we update the kryo library. Make sure to not update to an incompatible format change, see kryo changes logs:
 * https://github.com/EsotericSoftware/kryo/blob/master/CHANGES.md
 */
public class ClbKryoFactory implements KryoFactory {

    @Override
    public Kryo create() {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(true);

        kryo.register(NameUsage.class);
        kryo.register(VerbatimNameUsage.class);
        kryo.register(NameUsageMetrics.class);
        kryo.register(UsageExtensions.class);
        kryo.register(ParsedName.class);
        kryo.register(Description.class);
        kryo.register(Distribution.class);
        kryo.register(Identifier.class);
        kryo.register(NameUsageMediaObject.class);
        kryo.register(Reference.class);
        kryo.register(SpeciesProfile.class);
        kryo.register(NameUsage.class);
        kryo.register(TypeSpecimen.class);
        kryo.register(VernacularName.class);
        kryo.register(DatasetMetrics.class);

        // java & commons
        kryo.register(Date.class);
        kryo.register(HashMap.class);
        kryo.register(HashSet.class);
        kryo.register(ArrayList.class);
        kryo.register(UUID.class, new UUIDSerializer());
        kryo.register(URI.class, new URISerializer());
        ImmutableListSerializer.registerSerializers(kryo);
        // enums
        kryo.register(EnumSet.class, new EnumSetSerializer());
        kryo.register(NameUsageIssue.class);
        kryo.register(NomenclaturalStatus.class);
        kryo.register(NomenclaturalStatus[].class);
        kryo.register(TaxonomicStatus.class);
        kryo.register(Origin.class);
        kryo.register(Rank.class);
        kryo.register(Extension.class);
        kryo.register(Kingdom.class);
        kryo.register(NameType.class);
        kryo.register(NamePart.class);
        kryo.register(Language.class);
        kryo.register(Country.class);
        kryo.register(OccurrenceStatus.class);
        kryo.register(LifeStage.class);
        kryo.register(ThreatStatus.class);
        kryo.register(EstablishmentMeans.class);
        kryo.register(CitesAppendix.class);
        kryo.register(IdentifierType.class);
        kryo.register(MediaType.class);
        kryo.register(TypeStatus.class);
        kryo.register(TypeDesignationType.class);
        kryo.register(Sex.class);
        // term enums
        kryo.register(AcTerm.class);
        kryo.register(DcElement.class);
        kryo.register(DcTerm.class);
        kryo.register(DwcTerm.class);
        kryo.register(EolReferenceTerm.class);
        kryo.register(GbifInternalTerm.class);
        kryo.register(GbifTerm.class);
        kryo.register(IucnTerm.class);
        kryo.register(XmpRightsTerm.class);
        kryo.register(XmpTerm.class);
        TermSerializer ts = new TermSerializer();
        kryo.register(UnknownTerm.class, ts);

        return kryo;
    }
}