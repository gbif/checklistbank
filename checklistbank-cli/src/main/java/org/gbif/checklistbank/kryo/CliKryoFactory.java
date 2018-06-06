package org.gbif.checklistbank.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoFactory;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.gbif.api.model.checklistbank.*;
import org.gbif.api.model.common.Identifier;
import org.gbif.api.vocabulary.*;
import org.gbif.checklistbank.cli.model.UsageFacts;
import org.gbif.checklistbank.model.Classification;
import org.gbif.checklistbank.model.UsageExtensions;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.SrcUsage;
import org.gbif.dwc.terms.*;
import org.neo4j.kernel.impl.core.NodeProxy;

import java.net.URI;
import java.util.*;


/**
 * Creates a kryo factory usable for thread safe kryo pools that can deal with clb api classes.
 * We use Kryo for extremely fast byte serialization of temporary objects.
 * It is used to serialize various information in kvp stores during checklist indexing and nub builds.
 */
public class CliKryoFactory implements KryoFactory {

  @Override
  public Kryo create() {
    Kryo kryo = new Kryo();
    kryo.setRegistrationRequired(true);

    kryo.register(NameUsage.class);
    kryo.register(VerbatimNameUsage.class);
    kryo.register(NameUsageMetrics.class);
    kryo.register(UsageExtensions.class);
    kryo.register(ParsedName.class);
    kryo.register(DatasetMetrics.class);
    kryo.register(Description.class);
    kryo.register(Distribution.class);
    kryo.register(Identifier.class);
    kryo.register(NameUsageMediaObject.class);
    kryo.register(Reference.class);
    kryo.register(SpeciesProfile.class);
    kryo.register(TypeSpecimen.class);
    kryo.register(VernacularName.class);
    // cli specifics
    kryo.register(NubUsage.class);
    kryo.register(UsageFacts.class);
    kryo.register(Classification.class);
    kryo.register(SrcUsage.class);

    // fastutil
    kryo.register(IntArrayList.class);

    // java & commons
    kryo.register(Date.class);
    kryo.register(HashMap.class);
    kryo.register(HashSet.class);
    kryo.register(ArrayList.class);
    kryo.register(UUID.class, new UUIDSerializer());
    kryo.register(URI.class, new URISerializer());
    kryo.register(int[].class);
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
    kryo.register(NamePart.class, 40);
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
    kryo.register(GbifInternalTerm.class);
    kryo.register(GbifTerm.class);
    kryo.register(IucnTerm.class);
    kryo.register(XmpRightsTerm.class);
    kryo.register(XmpTerm.class);
    kryo.register(UnknownTerm.class, new TermSerializer());

    // ignore neo node proxies and set them to null upon read:
    kryo.register(NodeProxy.class, new NullSerializer());

    return kryo;
  }
}
