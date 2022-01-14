/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.nub.lookup.fuzzy;


import org.gbif.checklistbank.model.ParsedNameUsage;

import java.io.IOException;
import java.util.function.Consumer;

import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.lucene.index.IndexWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class IndexBuildResultHandler implements Consumer<ParsedNameUsage> {
  private static final Logger LOG = LoggerFactory.getLogger(IndexBuildResultHandler.class);

  private final IndexWriter writer;
  private int counter;

  public IndexBuildResultHandler(IndexWriter writer) {
    this.writer = writer;
  }

  @Override
  public void accept(ParsedNameUsage u) {
    try {
      writer.addDocument(NubIndex.toDoc(u));
      counter++;
      if (counter % 100000 == 0) {
        LOG.info("{} usages added to nub index", counter);
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
