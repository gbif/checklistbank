package org.gbif.nub.lookup.fuzzy;

import org.gbif.api.model.Constants;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.model.common.LinneanClassificationKeys;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.checklistbank.service.mybatis.mapper.NameUsageMapper;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A read only lucene index keeping the core attributes of a nub name usage.
 * The index exposes matching methods that allow to select usages based on their nub key or do fuzzy matches
 * on the canonical name alone.
 *
 * The index lies at the core of the nub matching service to preselect a list of potential good matches.
 *
 * The index can either be purely memory based or on the filesystem using a memory mapped OS cache.
 * For the entire nub with roughly 4.5 million usages this index requires 4GB of heap memory if the RAMDirectory is used.
 * The memory mapped file index uses very little heap memory and instead all available memory should be given to the OS
 * to enabling caching on the file system level.
 */
public class NubIndex implements ClassificationResolver, AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(NubIndex.class);

  /**
   * Type for a stored IntField with max precision to minimize memory usage as we dont need range queries.
   */
  private static final FieldType INT_FIELD_MAX_PRECISION = new FieldType();
  static {
    INT_FIELD_MAX_PRECISION.setTokenized(false);
    INT_FIELD_MAX_PRECISION.setOmitNorms(true);
    INT_FIELD_MAX_PRECISION.setIndexOptions(IndexOptions.DOCS);
    INT_FIELD_MAX_PRECISION.setNumericType(FieldType.NumericType.INT);
    INT_FIELD_MAX_PRECISION.setNumericPrecisionStep(Integer.MAX_VALUE);
    INT_FIELD_MAX_PRECISION.setStored(true);
    INT_FIELD_MAX_PRECISION.freeze();
  }

  private static final String FIELD_ID = "id";
  private static final String FIELD_CANONICAL_NAME = "canonical";
  private static final String FIELD_SCIENTIFIC_NAME = "sciname";
  private static final String FIELD_RANK = "rank";
  private static final String FIELD_STATUS = "status";
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

  private static final Analyzer analyzer = new ScientificNameAnalyzer();
  private final Directory index;
  private final IndexSearcher searcher;

  private static void load(Directory d, NameUsageMapper mapper) throws IOException {
    LOG.info("Start building a new nub index");
    IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
    IndexWriter writer = new IndexWriter(d, cfg);
    // creates initial index segments
    writer.commit();

    IndexBuildHandler builder = new IndexBuildHandler(writer);
    mapper.processDataset(Constants.NUB_DATASET_KEY, builder);

    writer.close();
    LOG.info("Finished building nub index");
  }

  public static NubIndex newMemoryIndex(NameUsageMapper mapper) throws IOException {
    RAMDirectory dir = new RAMDirectory();
    load(dir, mapper);
    return new NubIndex(dir);
  }

  public static NubIndex newMemoryIndex(Iterable<NameUsageMatch> usages) throws IOException {
    LOG.info("Start building a new nub RAM index");
    RAMDirectory dir = new RAMDirectory();
    IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
    IndexWriter writer = new IndexWriter(dir, cfg);
    // creates initial index segments
    writer.commit();
    int counter = 0;
    for (NameUsageMatch u : usages) {
      if (u != null && u.getUsageKey() != null) {
        writer.addDocument(toDoc(u));
        counter++;
      }
    }
    writer.close();
    LOG.info("Finished building nub index with {} usages", counter);
    return new NubIndex(dir);
  }

  /**
   * Creates a nub index for the backbone by loading it from the lucene index dir if it exists.
   * If it is not existing a new index directory will be built using the UsageService with given threads.
   * If the indexDir is null the index is never written to the filesytem but just kept in memory.
   * @param indexDir directory to use as the lucence index directory. If null the index is only kept in memory.
   */
  public static NubIndex newFileIndex(File indexDir, NameUsageMapper mapper) throws IOException {
    MMapDirectory dir;
    if (indexDir.exists()) {
      Preconditions.checkArgument(indexDir.isDirectory(), "Given index directory exists but is not a directory");
      // load existing index from disk
      LOG.info("Loading existing nub index from disk: {}", indexDir.getAbsoluteFile());
      dir = new MMapDirectory(indexDir.toPath());

    } else {
      // persistent new memory mapped file based index and then populate it
      LOG.info("Creating new nub index directory at {}", indexDir.getAbsoluteFile());
      FileUtils.forceMkdir(indexDir);
      dir = new MMapDirectory(indexDir.toPath());
      load(dir, mapper);
    }
    return new NubIndex(dir);
  }

  public NubIndex(Directory d) throws IOException {
    index = d;
    DirectoryReader reader= DirectoryReader.open(index);
    searcher = new IndexSearcher(reader);
  }


  public NameUsageMatch matchByUsageId(Integer usageID) {

    Query q = NumericRangeQuery.newIntRange(NubIndex.FIELD_ID, Integer.MAX_VALUE, usageID, usageID, true, true);

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

    // use lucene analyzer to normalize input without using the full query parser
    StringBuilder sb = new StringBuilder();
    try (TokenStream stream = analyzer.tokenStream(NubIndex.FIELD_CANONICAL_NAME, new StringReader(name))) {
      CharTermAttribute termAtt = stream.getAttribute(CharTermAttribute.class);
      stream.reset();
      while (stream.incrementToken()) {
        sb.append(termAtt.toString());
      }
      stream.end();
    } catch (IOException e) {
      LOG.error("An impossible error happened", e);
    }

    LOG.debug("Analyzed {} query \"{}\" becomes >>{}<<", fuzzySearch ? "fuzzy" : "straight", name, sb.toString());

    // query needs to have at least 2 letters to match a real name
    if (sb.length() < 2) {
      return Lists.newArrayList();
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
      return search(q, name, fuzzySearch, maxMatches);
    } catch (RuntimeException e) {
      // for example TooComplexToDeterminizeException, see http://dev.gbif.org/issues/browse/POR-2725
      LOG.warn("Lucene failed to fuzzy search for name [{}]. Try a straight match instead", name);
      return search(new TermQuery(t), name, false, maxMatches);
    }
  }

  private List<NameUsageMatch> search(Query q, String name, boolean fuzzySearch, int maxMatches) {
    List<NameUsageMatch> results = Lists.newArrayList();
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
    return searcher;
  }

  /**
   * Builds a NameUsageMatch instance from a lucene Document and populates all fields but the matching specifics
   * i.e. confidence and matchType.
   */
  private static NameUsageMatch fromDoc(Document doc) {
    NameUsageMatch u = new NameUsageMatch();
    u.setUsageKey(toInt(doc, FIELD_ID));

    u.setScientificName(doc.get(FIELD_SCIENTIFIC_NAME));
    u.setCanonicalName(doc.get(FIELD_CANONICAL_NAME));

    // higher ranks
    for (Rank r : HIGHER_RANK_FIELD_MAP.keySet()) {
      ClassificationUtils.setHigherRank(u, r, doc.get(HIGHER_RANK_FIELD_MAP.get(r)), toInteger(doc,
        HIGHER_RANK_ID_FIELD_MAP.get(r)));
    }

    u.setRank(Rank.values()[toInt(doc, FIELD_RANK)]);
    u.setStatus(TaxonomicStatus.values()[toInt(doc, FIELD_STATUS)]);

    return u;
  }

  protected static Document toDoc(NameUsage u) {
    return toDoc(u.getKey(), u.getCanonicalName(), u.getScientificName(), u.getTaxonomicStatus(), u.getRank(), u, u);
  }

  private static Document toDoc(NameUsageMatch u) {
    return toDoc(u.getUsageKey(), u.getCanonicalName(), u.getScientificName(), u.getStatus(), u.getRank(), u, u);
  }

  /**
   * @param status any status incl null. Will be converted to just 3 accepted, synonym & doubtful
   */
  private static Document toDoc(int key, String canonical, String sciname, TaxonomicStatus status, Rank rank,
    LinneanClassification cl, LinneanClassificationKeys clKeys) {

    Document doc = new Document();

    // use custom precision step as we do not need range queries and prefer to save memory usage instead
    doc.add(new IntField(FIELD_ID, key, INT_FIELD_MAX_PRECISION));

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

    // store rank if existing as ordinal int
    // this lucene index is not persistent, so not risk in changing ordinal numbers
    doc.add(new StoredField(FIELD_RANK, rank == null ? Rank.UNRANKED.ordinal() : rank.ordinal()));

    // allow only 3 values for status, accepted, doubtful and synonym
    if (status == null) {
      status = TaxonomicStatus.DOUBTFUL;
    } else if (status.isSynonym()) {
      status = TaxonomicStatus.SYNONYM;
    }
    doc.add(new StoredField(FIELD_STATUS, status.ordinal()));


    return doc;
  }

  private static int toInt(Document doc, String field) {
    return (int) doc.getField(field).numericValue();
  }

  private static Integer toInteger(Document doc, String field) {
    IndexableField f = doc.getField(field);
    if ( f != null) {
      return (Integer) f.numericValue();
    }
    return null;
  }

  @Override
  public LinneanClassification getClassification(int usageKey) {
    return matchByUsageId(usageKey);
  }

  @Override
  public void close() throws Exception {
    index.close();
  }
}
