package org.gbif.nub.lookup;

import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.model.common.LinneanClassificationKeys;
import org.gbif.api.util.ClassificationUtils;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
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
public abstract class NubIndex implements ClassificationResolver {

  /**
   * Type for a stored IntField with max precision to minimize memory usage as we dont need range queries.
   */
  private static final FieldType INT_FIELD_MAX_PRECISION = new FieldType();
  static {
    INT_FIELD_MAX_PRECISION.setIndexed(true);
    INT_FIELD_MAX_PRECISION.setTokenized(false);
    INT_FIELD_MAX_PRECISION.setOmitNorms(true);
    INT_FIELD_MAX_PRECISION.setIndexOptions(FieldInfo.IndexOptions.DOCS_ONLY);
    INT_FIELD_MAX_PRECISION.setNumericType(FieldType.NumericType.INT);
    INT_FIELD_MAX_PRECISION.setNumericPrecisionStep(Integer.MAX_VALUE);
    INT_FIELD_MAX_PRECISION.setStored(true);
    INT_FIELD_MAX_PRECISION.freeze();
  }

  protected static final String FIELD_ID = "id";
  protected static final String FIELD_CANONICAL_NAME = "canonical";
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


  private static final Logger LOG = LoggerFactory.getLogger(NubIndex.class);

  protected static final Analyzer analyzer = new ScientificNameAnalyzer();


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
    List<NameUsageMatch> results = Lists.newArrayList();

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

  abstract IndexSearcher obtainSearcher();

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

  protected static Document toDoc(NameUsageMatch u) {
    return toDoc(u.getUsageKey(), u.getCanonicalName(), u.getScientificName(), u.getStatus(), u.getRank(), u, u);
  }

  /**
   * @param status any status incl null. Will be converted to just 3 accepted, synonym & doubtful
   */
  protected static Document toDoc(int key, String canonical, String sciname, TaxonomicStatus status, Rank rank,
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
}
