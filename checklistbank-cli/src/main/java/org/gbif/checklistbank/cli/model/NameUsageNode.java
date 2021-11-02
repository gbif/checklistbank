package org.gbif.checklistbank.cli.model;

import org.apache.commons.lang3.StringUtils;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.neo4j.graphdb.Node;

/**
 * Simple wrapper to hold a neo node together with a NameUsage.
 * The modified flag can be used to (manually) track if an instance has changed and needs to be persisted.
 */
public class NameUsageNode {
    public Node node;
    public NameUsage usage;
    public boolean modified;

    /**
     * Adds a string remark to the taxonRemarks property of a usage but does not flush the change into the storage.
     * You need to make sure the usage is stored afterwards to not lose it.
     * Existing remarks are left untouched and the new string is appended.
     */
    public static void addRemark(NameUsage usage, String remark) {
        if (StringUtils.isBlank(usage.getRemarks())) {
            usage.setRemarks(remark);
        } else {
            usage.setRemarks(usage.getRemarks() + "; " + remark);
        }
    }

    public NameUsageNode(Node node, NameUsage usage, boolean modified) {
        this.node = node;
        this.usage = usage;
        this.modified = modified;
    }

    /**
     * Adds a string remark to the taxonRemarks property of a usage but does not flush the change into the storage.
     * You need to make sure the usage is stored afterwards to not lose it.
     * Existing remarks are left untouched and the new string is appended.
     */
    public void addRemark(String remark) {
        addRemark(usage, remark);
        modified = true;
    }

    public void addIssue(NameUsageIssue ... issues) {
        for (NameUsageIssue issue : issues) {
            usage.addIssue(issue);
        }
        modified = true;
    }

}