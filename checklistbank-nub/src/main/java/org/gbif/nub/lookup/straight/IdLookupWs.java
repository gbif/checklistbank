package org.gbif.nub.lookup.straight;

import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.service.checklistbank.NameUsageMatchingService;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.checklistbank.ws.client.guice.ChecklistBankWsClientModule;
import org.gbif.checklistbank.ws.client.guice.ChecklistBankWsMatchClientModule;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;

import static org.gbif.dwc.terms.GbifTerm.canonicalName;

/**
 * Implementation of an IdLookup using the nub matching webservice.
 */
public class IdLookupWs implements IdLookup {

  NameUsageMatchingService service;

  public static IdLookupWs create(String apiRootUrl) {
    Properties props = new Properties();
    props.setProperty(ChecklistBankWsMatchClientModule.PROPERTY_API_URL, apiRootUrl);
    return new IdLookupWs(props);
  }

  public IdLookupWs(Properties props) {
    ChecklistBankWsClientModule module = new ChecklistBankWsClientModule(props, false, true);
    Injector inj = Guice.createInjector(module);
    service = inj.getInstance(NameUsageMatchingService.class);
  }

  @Override
  public LookupUsage match(String canonicalName, Rank rank, Kingdom kingdom) {
    return match(canonicalName, null, null, rank, kingdom);
  }

  @Override
  public LookupUsage match(String canonicalName, @Nullable String authorship, @Nullable String year, Rank rank, Kingdom kingdom) {
    return usage(service.match(concatName(canonicalName, authorship, year), rank, classification(kingdom), true, false));
  }

  @Override
  public List<LookupUsage> match(String canonicalName) {
    List<LookupUsage> matches = Lists.newArrayList();
    NameUsageMatch match = service.match(canonicalName, null, null, true, true);
    if (match != null) {
      if (match.getUsageKey() != null) {
        matches.add(usage(match));
        // add alternatives
        for (NameUsageMatch alt : match.getAlternatives()) {
          matches.add(usage(alt));
        }
      }
    }
    return matches;
  }

  private static String concatName(String canonical, String authorship, String year) {
    StringBuilder name = new StringBuilder();
    name.append(canonical);
    if (authorship != null) {
      name.append(" ");
      name.append(authorship);
      if (year != null) {
        name.append(",");
      }
    }
    if (year != null) {
      name.append(" ");
      name.append(year);
    }
    return name.toString();
  }

  private static LinneanClassification classification(Kingdom kingdom) {
    LinneanClassification cl = new NameUsageMatch();
    cl.setKingdom(kingdom.scientificName());
    return cl;
  }

  private static LookupUsage usage(NameUsageMatch match) {
    LookupUsage u = null;
    if (match != null && match.getUsageKey() != null) {
      u = new LookupUsage();
      u.setKey(match.getUsageKey());
      u.setCanonical(match.getCanonicalName());
      u.setRank(match.getRank());
      u.setAuthorship(extractAuthor(match.getScientificName(), match.getCanonicalName()));
      u.setYear(extractYear(match.getScientificName(), match.getCanonicalName()));
      if (match.getKingdomKey() != null) {
        u.setKingdom(Kingdom.byNubUsageKey(match.getKingdomKey()));
      } else {
        u.setKingdom(Kingdom.INCERTAE_SEDIS);
      }
      u.setDeleted(false);
    }
    return u;
  }

  private static String extractAuthor(String sciname, String canonical) {
    // be simple, remove last canonical epithet from sciname
    String author = null;
    return author;
  }

  private static String extractYear(String sciname, String canonical) {
    // ignore year for now...
    return null;
  }

  @Override
  public int size() {
    throw new UnsupportedOperationException("The webservice lookup can only query backbone names");
  }

  @Override
  public int deletedIds() {
    throw new UnsupportedOperationException("The webservice lookup can only query backbone names");
  }

  @Override
  public Iterator<LookupUsage> iterator() {
    throw new UnsupportedOperationException("The webservice lookup can only query backbone names");
  }

  @Nullable
  @Override
  public AuthorComparator getAuthorComparator() {
    throw new UnsupportedOperationException("The webservice lookup can only query backbone names");
  }

  @Override
  public void close() throws Exception {
    // nothing to do
  }
}
