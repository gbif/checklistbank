package org.gbif.checklistbank.cli.common;

import com.google.common.base.Strings;
import com.yammer.metrics.Gauge;
import com.yammer.metrics.MetricRegistry;
import org.gbif.checklistbank.cli.normalizer.NormalizerService;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoMapper;
import org.gbif.checklistbank.neo.TaxonProperties;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.kernel.impl.util.StringLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 *
 */
public class NeoRunnable {
    private static final Logger LOG = LoggerFactory.getLogger(NeoRunnable.class);

    private final NeoConfiguration neoCfg;
    protected final UUID datasetKey;
    protected final int batchSize;
    private final Gauge memory;
    protected GraphDatabaseService db;
    protected NeoMapper mapper = NeoMapper.instance();
    protected ExecutionEngine engine;

    public NeoRunnable(UUID datasetKey, NeoConfiguration cfg, MetricRegistry registry) {
        batchSize = cfg.batchSize;
        this.datasetKey = datasetKey;
        this.memory = registry.getGauges().get(NormalizerService.HEAP_GAUGE);
        this.neoCfg= cfg;
    }

    protected void setupDb() {
        db = neoCfg.newEmbeddedDb(datasetKey);
        engine = new ExecutionEngine(db, StringLogger.SYSTEM);
    }

    protected void tearDownDb() {
        db.shutdown();
    }

    protected void logMemory() {
        LOG.debug("Heap usage: {}", memory.getValue());
    }

    protected String value(Node n, Term term) {
        if (n.hasProperty(term.simpleName())) {
            return Strings.emptyToNull(((String) n.getProperty(term.simpleName(), null)).trim());
        }
        return null;
    }

    protected String[] listValue(Node n, DwcTerm term) {
        if (n.hasProperty(term.simpleName())) {
            return (String[]) n.getProperty(term.simpleName(), new String[0]);
        }
        return new String[0];
    }

    /**
     * @return the single matching node with the taxonID or null
     */
    protected Node nodeByTaxonId(String taxonID) {
        return IteratorUtil.singleOrNull(db.findNodesByLabelAndProperty(Labels.TAXON, TaxonProperties.TAXON_ID, taxonID));
    }
}
