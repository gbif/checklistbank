package org.gbif.checklistbank.cli.common.paging;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.DatasetType;

import java.util.Iterator;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Iterator over datasets from paging responses that filters out deleted datasets and adds an optional type filter.
 */
abstract class DatasetBasePager implements Iterable<Dataset> {
    private static final Logger LOG = LoggerFactory.getLogger(DatasetBasePager.class);
    private final DatasetType type;

    /**
     * @param type the accepted dataset type, null for all
     */
    public DatasetBasePager(@Nullable DatasetType type) {
        this.type = type;
    }

    class ResponseIterator implements Iterator<Dataset>{
        private final PagingRequest page = new PagingRequest(0, 25);
        private PagingResponse<Dataset> resp = null;
        private Iterator<Dataset> iter;
        private Dataset next;

        public ResponseIterator() {
            loadPage();
            next = nextDataset();
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Dataset next() {
            Dataset d = next;
            next = nextDataset();
            return d;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private Dataset nextDataset() {
            // just to be sure we dont get stuck in this while loop forever
            int safeCounter = 0;
            while (true) {
                if (safeCounter++ > 100000) {
                    LOG.warn("We tried 100.000 times to get a new dataset but didnt. Sth is wrong and we stop.");
                    return null;
                }
                if (!iter.hasNext()) {
                    if (resp.isEndOfRecords()) {
                        // no more records to load, stop!
                        return null;
                    } else {
                        loadPage();
                    }
                }
                Dataset d = iter.next();
                if (d.getDeleted() != null) {
                    LOG.debug("Ignore deleted dataset {}: {}", d.getKey(), d.getTitle().replaceAll("\n", " "));
                } else if (type != null && d.getType() != type) {
                    LOG.debug("Ignore {} dataset {}: {}", d.getType(), d.getKey(), d.getTitle().replaceAll("\n", " "));
                } else {
                    return d;
                }
            }
        }

        private void loadPage() {
            LOG.info("Load dataset page {}-{}", page.getOffset(), page.getOffset()+page.getLimit());
            resp = nextPage(page);
            iter = resp.getResults().iterator();
            page.nextPage();
        }
    }

    abstract PagingResponse<Dataset> nextPage(PagingRequest page);

    @Override
    public Iterator<Dataset> iterator() {
        return new ResponseIterator();
    }

}
