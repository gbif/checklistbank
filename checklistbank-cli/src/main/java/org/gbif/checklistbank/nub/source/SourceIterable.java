package org.gbif.checklistbank.nub.source;

import org.gbif.checklistbank.nub.model.SrcUsage;

public interface SourceIterable extends Iterable<SrcUsage>, AutoCloseable {

}
