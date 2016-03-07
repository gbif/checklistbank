package org.gbif.nub.lookup.fuzzy;

import org.gbif.api.model.checklistbank.NameUsage;

import java.io.IOException;

import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.lucene.index.IndexWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class IndexBuildHandler implements ResultHandler<NameUsage> {
  private static final Logger LOG = LoggerFactory.getLogger(IndexBuildHandler.class);

  private final IndexWriter writer;
  private int counter;

  public IndexBuildHandler(IndexWriter writer) {
    this.writer = writer;
  }

  @Override
  public void handleResult(ResultContext<? extends NameUsage> u) {
    try {
      writer.addDocument(NubIndex.toDoc(u.getResultObject()));
      counter++;
      if (counter % 100000 == 0) {
        LOG.info("{} usages added to nub index", counter);
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
