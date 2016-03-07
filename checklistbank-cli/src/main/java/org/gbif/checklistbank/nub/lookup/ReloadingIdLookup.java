package org.gbif.checklistbank.nub.lookup;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.common.messaging.MessageListener;
import org.gbif.common.messaging.api.MessageCallback;
import org.gbif.common.messaging.api.messages.BackboneChangedMessage;
import org.gbif.nub.lookup.straight.IdLookup;
import org.gbif.nub.lookup.straight.IdLookupImpl;
import org.gbif.nub.lookup.straight.LookupUsage;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import javax.annotation.Nullable;

import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A IdLookup that uses the IdLookupImpl internally and registers itself as a listern for BackboneChangedMessage messages
 * that will trigger a reload of the lookup data.
 */
public class ReloadingIdLookup implements IdLookup, MessageCallback<BackboneChangedMessage> {
  private static final Logger LOG = LoggerFactory.getLogger(ReloadingIdLookup.class);
  private IdLookup lookup;
  private final ClbConfiguration cfg;

  public ReloadingIdLookup(ClbConfiguration cfg, MessageListener listener, String queuePrefix) {
    this.cfg = cfg;
    reload();
    try {
      String q = queuePrefix+"-nubchanged";
      LOG.info("Nub lookup starts listening to {} on queue {}", BackboneChangedMessage.class.getSimpleName(), q);
      listener.listen(q, 1, this);
    } catch (IOException e) {
      Throwables.propagate(e);
    }
  }

  /**
   * Reloads names from the backbone of the configured clb config.
   */
  @Override
  public void handleMessage(BackboneChangedMessage message) {
    reload();
  }

  private void reload() {
    try {
      lookup = IdLookupImpl.temp().load(cfg);
    } catch (Exception e) {
      LOG.error("Failed to reload backbone names", e);
      Throwables.propagate(e);
    }
  }

  @Override
  public Class<BackboneChangedMessage> getMessageClass() {
    return BackboneChangedMessage.class;
  }

  @Override
  public int deletedIds() {
    return lookup.deletedIds();
  }

  @Override
  @Nullable
  public AuthorComparator getAuthorComparator() {
    return lookup.getAuthorComparator();
  }

  @Override
  public Iterator<LookupUsage> iterator() {
    return lookup.iterator();
  }

  @Override
  public LookupUsage match(String canonicalName, @Nullable String authorship, @Nullable String year, Rank rank, Kingdom kingdom) {
    return lookup.match(canonicalName, authorship, year, rank, kingdom);
  }

  @Override
  public LookupUsage match(String canonicalName, Rank rank, Kingdom kingdom) {
    return lookup.match(canonicalName, rank, kingdom);
  }

  @Override
  public List<LookupUsage> match(String canonicalName) {
    return lookup.match(canonicalName);
  }

  @Override
  public int size() {
    return lookup.size();
  }

  @Override
  public void forEach(Consumer<? super LookupUsage> action) {
    lookup.forEach(action);
  }

  @Override
  public Spliterator<LookupUsage> spliterator() {
    return lookup.spliterator();
  }
}
