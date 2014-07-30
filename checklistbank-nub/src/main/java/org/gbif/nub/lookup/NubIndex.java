package org.gbif.nub.lookup;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.model.common.LinneanClassificationKeys;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.service.UsageService;
import org.gbif.checklistbank.model.Usage;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TrackingIndexWriter;
import org.apache.lucene.search.ControlledRealTimeReopenThread;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A memory based lucene index keeping the core attributes of a nub name usage.
 * The index exposes matching methods that allow to select usages based on their nub key or do fuzzy matches
 * on the canonical name alone.
 *
 * The index lies at the core of the nub matching service to preselect a list of potential good matches.
 *
 * For the entire nub with roughly 4.5 million usages this index requires 4GB of java memory.
 */
public class NubIndex implements ClassificationResolver {

  protected static final String FIELD_ID = "id";
  protected static final String FIELD_CANONICAL_NAME = "canonical";
  private static final String FIELD_SCIENTIFIC_NAME = "sciname";
  private static final String FIELD_RANK = "rank";
  private static final String FIELD_SYNONYM = "syn";
  private static final Map<Rank, String> HIGHER_RANK_ID_FIELD_MAP = ImmutableMap.<Rank, String>builder()
    .put(Rank.KINGDOM, "k")
    .put(Rank.PHYLUM, "p")
    .put(Rank.CLASS, "c")
    .put(Rank.ORDER, "o")
    .put(Rank.FAMILY, "f")
    .put(Rank.GENUS, "g")
    .put(Rank.SPECIES, "s")
    .build();
  private static final Map<Rank, String> HIGHER_RANK_FIELD_MAP = ImmutableMap.<Rank, String>builder()
    .put(Rank.KINGDOM, "kid")
    .put(Rank.PHYLUM, "pid")
    .put(Rank.CLASS, "cid")
    .put(Rank.ORDER, "oid")
    .put(Rank.FAMILY, "fid")
    .put(Rank.GENUS, "gid")
    .put(Rank.SPECIES, "sid")
    .build();


  private static final Logger LOG = LoggerFactory.getLogger(NubIndex.class);

  private final Directory index;
  private final IndexWriterConfig cfg;
  private final Analyzer analyzer;
  private final TrackingIndexWriter writer;
  private SearcherManager searchManager;
  private final ControlledRealTimeReopenThread<IndexSearcher> indexSearcherReopenThread;
  private long reopenToken;

  public NubIndex() throws IOException {
    index = new RAMDirectory();
    LOG.info("Init a new, empty nub index");
    analyzer = new ScientificNameAnalyzer();
    cfg = new IndexWriterConfig(org.gbif.nub.utils.Constants.LUCENE_VERSION, analyzer);
    writer = new TrackingIndexWriter(new IndexWriter(index, cfg));
    // creates initial index segments
    writer.getIndexWriter().commit();
    searchManager = new SearcherManager(writer.getIndexWriter(), false, null);
    // Create the ControlledRealTimeReopenThread that reopens the index periodically having into
    // account the changes made to the index and tracked by the TrackingIndexWriter instance
    // The index is refreshed every 10 minutes when nobody is waiting
    // and every 10 millis whenever is someone waiting (see search method)
    indexSearcherReopenThread = new ControlledRealTimeReopenThread<IndexSearcher>(writer, searchManager, 600, 0.1);
    indexSearcherReopenThread.start(); // start the refresher thread
  }

  public static NubIndex newNubIndex(UsageService usageService) throws IOException {
    NubIndex idx = new NubIndex();
    NubIndexBuilder nubLoader = new NubIndexBuilder(idx.writer, usageService, 4);
    nubLoader.run();
    idx.searchManager.maybeRefresh();
    return idx;
  }

  public void addNameUsage(NameUsage usage) throws IOException {
    reopenToken = writer.addDocument(toDoc(usage));
  }

  /**
   * Adds the minimal usage information to the index required for a lookup.
   * The returned match objects are lacking any classification information though, so make sure
   * to use an external classification resolver with the nub matching service.
   * Use the regular addNameUsage(usage) method for complete objects.
   * @param u minimal usage
   * @param canonical
   * @throws IOException
   */
  public void addNameUsage(Usage u, String canonical) throws IOException {
    reopenToken = writer.addDocument(toDoc(u.key, canonical, null, u.status.isSynonym(), u.rank, null, null));
  }

  public NameUsageMatch matchByUsageId(Integer usageID) {

    Term tUsageID = new Term(NubIndex.FIELD_ID, usageID.toString());
    Query q = new TermQuery(tUsageID);
    try {
      IndexSearcher searcher = obtainSearcher();
      TopDocs docs = searcher.search(q, 3);
      if (docs.totalHits > 0) {
        Document doc = searcher.doc(docs.scoreDocs[0].doc);
        NameUsageMatch match = fromDoc(doc);
        match.setConfidence(100);
        return match;

      } else {
        LOG.warn("No usage {} found in lucene index", usageID);
      }
    } catch (IOException e) {
      LOG.error("Cannot load usage {} from lucene index", usageID, e.getMessage());
    }

    return null;
  }

  public List<NameUsageMatch> matchByName(String name, boolean fuzzySearch, int maxMatches) {
    List<NameUsageMatch> results = Lists.newArrayList();

    // use lucene analyzer to normalize input without using the full query parser
    StringBuilder sb = new StringBuilder();
    try {
      TokenStream stream = new ScientificNameAnalyzer().tokenStream(NubIndex.FIELD_CANONICAL_NAME, new StringReader(name));
      CharTermAttribute termAtt = stream.getAttribute(CharTermAttribute.class);
      while (stream.incrementToken()) {
        sb.append(termAtt.toString());
      }
    } catch (IOException e) {
      LOG.error("An impossible error happened", e);
    }

    LOG.debug("Analyzed {} query \"{}\" becomes >>{}<<", fuzzySearch ? "fuzzy" : "straight", name, sb.toString());

    // query needs to have at least 2 letters to match a real name
    if (sb.length() < 2) {
      return results;
    }

    final String analyzedName = sb.toString();
    Term t = new Term(NubIndex.FIELD_CANONICAL_NAME, analyzedName);
    Query q;
    if (fuzzySearch) {
      // allow 2 edits for names longer than 10 chars
      q = new FuzzyQuery(t, analyzedName.length() > 10 ? 2 : 1, 1);
    } else {
      q = new TermQuery(t);
    }
    try {
      IndexSearcher searcher = obtainSearcher();
      TopDocs docs = searcher.search(q, maxMatches);
      if (docs.totalHits > 0) {
        for (ScoreDoc sdoc : docs.scoreDocs) {
          NameUsageMatch match = fromDoc(searcher.doc(sdoc.doc));
          if (name.equalsIgnoreCase(match.getCanonicalName())) {
            match.setMatchType(NameUsageMatch.MatchType.EXACT);
            results.add(match);
          } else if (fuzzySearch) {
            // even though we used a term query for straight matching the lucene analyzer has already normalized
            // the name drastically. So we include these matches here only in case of fuzzy queries
            match.setMatchType(NameUsageMatch.MatchType.FUZZY);
            results.add(match);
          }
        }

      } else {
        LOG.debug("No {} match for name {}", fuzzySearch ? "fuzzy" : "straight", name);
      }

    } catch (IOException e) {
      LOG.error("lucene search error", e);
    }

    return results;
  }

  private IndexSearcher obtainSearcher() {
    try {
      indexSearcherReopenThread.waitForGeneration(reopenToken);
      return searchManager.acquire();

    } catch (IOException e) {
      LOG.error("Could not obtain lucene searcher", e);
      throw new RuntimeException(e);

    } catch (InterruptedException e) {
      LOG.error("Could not obtain lucene searcher in time", e);
      throw new RuntimeException(e);
    }
  }
  /**
   * Builds a NameUsageMatch instance from a lucene Document and populates all fields but the matching specifics
   * i.e. confidence and matchType.
   */
  private static NameUsageMatch fromDoc(Document doc) {
    NameUsageMatch u = new NameUsageMatch();
    u.setUsageKey(toInt(doc.get(FIELD_ID)));

    u.setScientificName(doc.get(FIELD_SCIENTIFIC_NAME));
    u.setCanonicalName(doc.get(FIELD_CANONICAL_NAME));

    // higher ranks
    for (Rank r : HIGHER_RANK_FIELD_MAP.keySet()) {
      ClassificationUtils.setHigherRank(u, r, doc.get(HIGHER_RANK_FIELD_MAP.get(r)), toInt(doc.get(HIGHER_RANK_ID_FIELD_MAP.get(r))));
    }

    Integer rankOrdinal = toInt(doc.get(FIELD_RANK));
    if (rankOrdinal != null) {
      u.setRank(Rank.values()[rankOrdinal]);
    } else {
      u.setRank(Rank.UNRANKED);
    }

    u.setSynonym(doc.get(FIELD_SYNONYM).equals("0") ? false : true);

    return u;
  }

  protected static Document toDoc(NameUsage u) {
    return toDoc(u.getKey(), u.getCanonicalName(), u.getScientificName(), u.isSynonym(), u.getRank(), u, u);
  }

  protected static Document toDoc(int key, String canonical, String sciname, boolean synonym, Rank rank,
    LinneanClassification cl, LinneanClassificationKeys clKeys) {

    Document doc = new Document();

    doc.add(new StringField(FIELD_ID, String.valueOf(key), Field.Store.YES));

    // analyzed name field - this is what we search upon
    if (canonical != null) {
      doc.add(new TextField(FIELD_CANONICAL_NAME, canonical, Field.Store.YES));
    }

    // store full name and classification only to return a full match object for hits
    if (sciname != null) {
      doc.add(new StoredField(FIELD_SCIENTIFIC_NAME, sciname));
    }

    // store ids as doc int values, not searchable
    if (clKeys != null) {
      for (Rank r : HIGHER_RANK_ID_FIELD_MAP.keySet()) {
        Integer hrk = clKeys.getHigherRankKey(r);
        if (hrk != null) {
          doc.add(new StoredField(HIGHER_RANK_ID_FIELD_MAP.get(r), hrk));
        }
      }
    }

    // store higher ranks, not searchable
    if (cl!= null) {
      for (Rank r : HIGHER_RANK_FIELD_MAP.keySet()) {
        String hr = cl.getHigherRank(r);
        if (hr != null) {
          doc.add(new StoredField(HIGHER_RANK_FIELD_MAP.get(r), hr));
        }
      }
    }

    // store synonym boolean as simple int with 0=false
    doc.add(new StoredField(FIELD_SYNONYM, synonym ? 1 : 0));

    // store rank if existing as ordinal int
    // this lucene index is not persistent, so not risk in changing ordinal numbers
    if (rank != null) {
      doc.add(new StoredField(FIELD_RANK, rank.ordinal()));
    }

    return doc;
  }

  private static Integer toInt(String val) {
    return Strings.isNullOrEmpty(val) ? null : Integer.valueOf(val);
  }

  @Override
  public LinneanClassification getClassification(int usageKey) {
    return matchByUsageId(usageKey);
  }
}
