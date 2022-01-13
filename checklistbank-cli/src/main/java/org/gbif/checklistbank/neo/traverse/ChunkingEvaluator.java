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
package org.gbif.checklistbank.neo.traverse;

import org.gbif.checklistbank.cli.model.UsageFacts;
import org.gbif.checklistbank.neo.UsageDao;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;

import com.google.common.base.Preconditions;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * Marks appropriate points in the taxonomic tree where concurrent processing can start.
 * At present, families are marked. This could be improved based on the checklist,
 * or could take account of the current depth (path.length()), to avoid marking many
 * unplaced families.
 */
public class ChunkingEvaluator implements Evaluator {

  private UsageDao dao;
  private int chunkSize;
  private int minChunkSize;
  private LongSet chunkIds = new LongOpenHashSet();

  public ChunkingEvaluator(UsageDao dao, int minChunkSize, int chunkSize) {
    Preconditions.checkArgument(minChunkSize < chunkSize, "Minimum chunk size needs to be smaller then the chunk size");
    Preconditions.checkArgument(minChunkSize >= 0, "Minimum chunk size needs to be positive");
    Preconditions.checkArgument(chunkSize > 0, "Chunk size needs to be at least 1");
    this.chunkSize = chunkSize;
    this.dao = dao;
    this.minChunkSize = minChunkSize;
  }

  @Override
  public Evaluation evaluate(Path path) {
    Node n = path.endNode();
    UsageFacts facts = dao.readFacts(n.getId());
    int size = facts == null ? -1 : facts.metrics.getNumDescendants() + facts.metrics.getNumSynonyms();
    if (size > minChunkSize && (size < chunkSize || size - facts.metrics.getNumChildren() < minChunkSize)) {
      chunkIds.add(n.getId());
      return Evaluation.INCLUDE_AND_PRUNE;
    } else {
      return Evaluation.INCLUDE_AND_CONTINUE;
    }
  }

  public boolean isChunk(long nodeId) {
    return chunkIds.contains(nodeId);
  }
}
