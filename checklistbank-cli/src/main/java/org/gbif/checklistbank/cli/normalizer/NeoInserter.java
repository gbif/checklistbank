package org.gbif.checklistbank.cli.normalizer;

import com.google.common.base.Strings;
import com.yammer.metrics.Meter;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.model.checklistbank.VerbatimNameUsage;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoMapper;
import org.gbif.checklistbank.neo.TaxonProperties;
import org.gbif.checklistbank.service.mybatis.VerbatimNameUsageJsonParser;
import org.gbif.common.parsers.NomStatusParser;
import org.gbif.common.parsers.RankParser;
import org.gbif.common.parsers.TaxStatusParser;
import org.gbif.common.parsers.core.EnumParser;
import org.gbif.common.parsers.core.ParseResult;
import org.gbif.dwc.record.Record;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.text.Archive;
import org.gbif.dwc.text.ArchiveFactory;
import org.gbif.dwc.text.StarRecord;
import org.gbif.nameparser.NameParser;
import org.gbif.nameparser.UnparsableException;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.index.lucene.unsafe.batchinsert.LuceneBatchInserterIndexProvider;
import org.neo4j.unsafe.batchinsert.BatchInserterIndex;
import org.neo4j.unsafe.batchinsert.BatchInserterIndexProvider;
import org.neo4j.unsafe.batchinsert.BatchInserters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 *
 */
public class NeoInserter {
    private static final Logger LOG = LoggerFactory.getLogger(NeoInserter.class);
    private static final Pattern NULL_PATTERN = Pattern.compile("^\\s*(\\\\N|\\\\?NULL)\\s*$");
    private static final String PROP_RAW_JSON = "json";

    private boolean useCoreID;
    private VerbatimNameUsageJsonParser jsonMapper = new VerbatimNameUsageJsonParser();
    private NeoMapper mapper = NeoMapper.instance();
    private NameParser nameParser = new NameParser();
    private RankParser rankParser = RankParser.getInstance();
    private EnumParser<NomenclaturalStatus> nomStatusParser = NomStatusParser.getInstance();
    private EnumParser<TaxonomicStatus> taxStatusParser = TaxStatusParser.getInstance();

    public boolean insert(File storeDir, File dwca, NormalizerStats stats, int batchSize, Meter insertMeter) throws NormalizationFailedException {
        Archive arch = null;
        try {
            arch = ArchiveFactory.openArchive(dwca);
            if (!arch.getCore().hasTerm(DwcTerm.taxonID)) {
                LOG.warn("Using core ID for taxonID");
                useCoreID = true;
            }
        } catch (IOException e) {
            throw new NormalizationFailedException("IOException opening archive " + dwca.getAbsolutePath(), e);
        }

        final org.neo4j.unsafe.batchinsert.BatchInserter inserter = BatchInserters.inserter(storeDir.getAbsolutePath());

        final BatchInserterIndexProvider indexProvider = new LuceneBatchInserterIndexProvider(inserter);
        final BatchInserterIndex taxonIdx = indexProvider.nodeIndex(DwcTerm.taxonID.simpleName(), MapUtil.stringMap("type", "exact"));
        taxonIdx.setCacheCapacity(TaxonProperties.TAXON_ID, 10000);
        final BatchInserterIndex sciNameIdx = indexProvider.nodeIndex(DwcTerm.taxonID.simpleName(), MapUtil.stringMap("type", "exact"));
        sciNameIdx.setCacheCapacity(TaxonProperties.SCIENTIFIC_NAME, 10000);
        final BatchInserterIndex canonNameIdx = indexProvider.nodeIndex(DwcTerm.taxonID.simpleName(), MapUtil.stringMap("type", "exact"));
        canonNameIdx.setCacheCapacity(TaxonProperties.CANONICAL_NAME, 10000);

        final long startSort = System.currentTimeMillis();
        LOG.debug("Sorted archive in {} seconds", (System.currentTimeMillis() - startSort) / 1000);

        int counter = 0;
        for (StarRecord star : arch) {
            counter++;
            VerbatimNameUsage v = new VerbatimNameUsage();

            Record core = star.core();
            for (Term t : core.terms()) {
                String val = norm(core.value(t));
                if (val != null) {
                    v.setCoreField(t, val);
                }
            }
            // make sure this is last to override already put taxonID keys
            v.setCoreField(DwcTerm.taxonID, taxonID(core));

            NameUsage u = buildUsage(core.id(), v);

            // ... and into neo
            Map<String, Object> props = mapper.propertyMap(u, false);
            props.put(DcTerm.identifier.simpleName(), core.id());
            props.put(PROP_RAW_JSON, jsonMapper.toJson(v));
            putProp(props, DwcTerm.parentNameUsageID, v);
            putProp(props, DwcTerm.acceptedNameUsageID, v);
            putProp(props, DwcTerm.originalNameUsageID, v);

            long node = inserter.createNode(props, Labels.TAXON);
            taxonIdx.add(node, props);

            insertMeter.mark();
            if (counter % (batchSize *10) == 0) {
                LOG.debug("insert: {}", counter);
            }
        }
        stats.setRecords(counter);
        LOG.info("Data insert completed, {} nodes created", counter);
        LOG.info("Insert rate: {}", insertMeter.getMeanRate());

        indexProvider.shutdown();
        inserter.shutdown();
        LOG.info("Neo shutdown, data flushed to disk", counter);

        return useCoreID;
    }

    private void putProp(Map<String, Object> props, Term t, VerbatimNameUsage v) {
        String val = norm(v.getCoreField(t));
        if (val != null) {
            props.put(t.simpleName(), val);
        }
    }

    private NameUsage buildUsage(String coreID, VerbatimNameUsage v) {
        NameUsage u = new NameUsage();
        u.setTaxonID(useCoreID ? coreID : v.getCoreField(DwcTerm.taxonID));

        u.setParent(v.getCoreField(DwcTerm.parentNameUsage));
        u.setAccepted(v.getCoreField(DwcTerm.acceptedNameUsage));
        u.setBasionym(v.getCoreField(DwcTerm.originalNameUsage));

        // classification
        //TODO: interpret classification string if others are not given
        // DwcTerm.higherClassification;
        u.setKingdom(v.getCoreField(DwcTerm.kingdom));
        u.setPhylum(v.getCoreField(DwcTerm.phylum));
        u.setClazz(v.getCoreField(DwcTerm.class_));
        u.setOrder(v.getCoreField(DwcTerm.order));
        u.setFamily(v.getCoreField(DwcTerm.family));
        u.setGenus(v.getCoreField(DwcTerm.genus));
        u.setSubgenus(v.getCoreField(DwcTerm.subgenus));

        // rank
        Rank rank = rankParser.parse(v.getCoreField(DwcTerm.taxonRank)).getPayload();
        if (rank == null) {
            rank = rankParser.parse(v.getCoreField(DwcTerm.verbatimTaxonRank)).getPayload();
        }
        u.setRank(rank);

        // build best name
        String sciname;
        ParsedName pn;
        if (v.hasCoreField(DwcTerm.scientificName)) {
            sciname = v.getCoreField(DwcTerm.scientificName);
            try {
                pn = nameParser.parse(sciname);
                // append author if its not part of the name yet
                if (!pn.isAuthorsParsed()) {
                    String author = v.getCoreField(DwcTerm.scientificNameAuthorship);
                    //TODO: better use parsed name class respecting autonyms???
                    sciname = sciname + " " + author;
                }
            } catch (UnparsableException e) {
                LOG.debug("Unparsable {} name {}", e.type, e.name);
                pn = new ParsedName();
                pn.setType(e.type);
            }

        } else {
            pn = new ParsedName();
            if (v.hasCoreField(GbifTerm.genericName)) {
                pn.setGenusOrAbove(v.getCoreField(GbifTerm.genericName));
            } else {
                pn.setGenusOrAbove(v.getCoreField(DwcTerm.genus));
            }
            pn.setSpecificEpithet(v.getCoreField(DwcTerm.specificEpithet));
            pn.setInfraSpecificEpithet(v.getCoreField(DwcTerm.infraspecificEpithet));
            pn.setAuthorship(v.getCoreField(DwcTerm.scientificNameAuthorship));
            pn.setRank(rank);
            pn.setType(NameType.WELLFORMED);
            sciname = pn.fullName();
        }
        u.setScientificName(sciname);
        u.setCanonicalName(pn.canonicalName());
        //TODO: verify name parts and rank
        u.setNameType(pn.getType());

        // tax status
        ParseResult<TaxonomicStatus> taxParse = taxStatusParser.parse(v.getCoreField(DwcTerm.taxonomicStatus));
        if (taxParse.isSuccessful()) {
            u.setTaxonomicStatus(taxParse.getPayload());
        }

        // nom status
        ParseResult<NomenclaturalStatus> nsParse = nomStatusParser.parse(v.getCoreField(DwcTerm.nomenclaturalStatus));
        if (nsParse.isSuccessful()) {
            u.getNomenclaturalStatus().add(nsParse.getPayload());
        }
        if (!Strings.isNullOrEmpty(pn.getNomStatus())) {
            nsParse = nomStatusParser.parse(pn.getNomStatus());
            if (nsParse.isSuccessful()) {
                u.getNomenclaturalStatus().add(nsParse.getPayload());
            }
        }

        return u;
    }

    private String taxonID(Record core) {
        if (useCoreID) {
            return norm(core.id());
        } else {
            return norm(core.value(DwcTerm.taxonID));
        }
    }

    private String norm(String x) {
        if (Strings.isNullOrEmpty(x) || NULL_PATTERN.matcher(x).find()) {
            return null;
        }
        return x.trim();
    }

}
