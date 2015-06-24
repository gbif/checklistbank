package org.gbif.checklistbank.cli.common;

/**
 * Created by markus on 17/06/15.
 */
public class Metrics {

    public static final String SYNC_METER = "clb-importer.sync";
    public static final String SYNC_FILES = "clb-importer.openfiles";
    public static final String HEAP_GAUGE = "heap.usage";
    public static final String INSERT_METER = "taxon.inserts";
    public static final String RELATION_METER = "taxon.relations";
    public static final String METRICS_METER = "taxon.metrics";
    public static final String DENORMED_METER = "taxon.denormed";

}
