package org.gbif.checklistbank.nub;

import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.Kingdom;
import org.gbif.api.vocabulary.NamePart;
import org.gbif.api.vocabulary.Origin;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.neo.Labels;
import org.gbif.checklistbank.neo.NeoMapper;
import org.gbif.checklistbank.neo.RelType;
import org.gbif.checklistbank.neo.TaxonProperties;
import org.gbif.checklistbank.nub.authorship.AuthorComparator;
import org.gbif.checklistbank.nub.model.Equality;
import org.gbif.checklistbank.nub.model.NubUsage;
import org.gbif.checklistbank.nub.model.SrcUsage;

import java.io.Closeable;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NubDb implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(NubDb.class);
  public static final String PN_GENUS = "pnGenus";
  public static final String PN_SPECIES = "pnSpecies";
  public static final String PN_INFRASPECIES = "pnInfraspecies";
  public static final String PN_NOTHO = "pnNotho";
  public static final String PN_RANK = "pnRank";
  public static final String PN_AUTHOR = "pnAuthor";
  public static final String PN_YEAR = "pnYear";
  public static final String PN_BRACKET_AUTHOR = "pnBracketAuthor";
  public static final String PN_BRACKET_YEAR = "pnBracketYear";
  public static final String PN_SENSU = "pnSensu";
  private final AuthorComparator authComp = new AuthorComparator();
  private final int batchSize;
  protected GraphDatabaseService gds;
  private Transaction tx;
  private NeoMapper mapper = NeoMapper.instance();
  private TraversalDescription parentsTraversal;
  // activity counter to manage transaction commits
  private int counter = 0;
  // number of created nodes
  private int nodes = 0;

  public NubDb(GraphDatabaseService gds, int batchSize) {
    this.batchSize = batchSize;
    this.gds = gds;
    tx = gds.beginTx();
    // create indices
    Schema schema = gds.schema();
    IndexDefinition idx = schema.indexFor(Labels.TAXON).on(TaxonProperties.CANONICAL_NAME).create();
    renewTx();
  }

  public List<NubUsage> findNubUsages(String canonical) {
    List<NubUsage> usages = Lists.newArrayList();
    for (Node n : IteratorUtil.asIterable(gds.findNodes(Labels.TAXON, TaxonProperties.CANONICAL_NAME, canonical))) {
      usages.add(node2usage(n));
    }
    return usages;
  }

  /**
   * Returns the matching accepted nub usage for that canonical name at the given rank.
   */
  public NubUsage findNubUsage(String canonical, Rank rank) {
    List<NubUsage> usages = Lists.newArrayList();
    for (Node n : IteratorUtil.asIterable(gds.findNodes(Labels.TAXON, TaxonProperties.CANONICAL_NAME, canonical))) {
      NubUsage rn = node2usage(n);
      if (rank == rn.rank && !rn.status.isSynonym()) {
        usages.add(rn);
      }
    }
    if (usages.isEmpty()) {
      return null;
    } else if (usages.size() == 1) {
      return usages.get(0);
    } else {
      LOG.warn("{} homonyms encountered for {} {}", usages.size(), rank, canonical);
      throw new IllegalStateException("homonym "+ canonical);
    }
  }

  /**
   * Tries to find an already existing nub usage for the given source usage.
   * @return the existing usage or null if it does not exist yet.
   */
  public NubUsage findNubUsage(SrcUsage u, Kingdom uKingdom) {
    List<NubUsage> checked = Lists.newArrayList();
    for (Node n : IteratorUtil.asIterable(gds.findNodes(Labels.TAXON, TaxonProperties.CANONICAL_NAME, u.parsedName.canonicalName()))) {
      NubUsage rn = node2usage(n);
      if (matchesNub(u, uKingdom, rn)) {
        checked.add(rn);
      }
    }

    if (checked.isEmpty()) {
      return null;
    } else if (checked.size() == 1) {
      return checked.get(0);
    } else {
      LOG.warn("{} homonyms encountered for {}", checked.size(), u.scientificName);
      //TODO: implmement sth clever dealing with homonyms!!!
      throw new IllegalStateException("homonym "+ u.scientificName);
    }
  }

  private boolean matchesNub(SrcUsage u, Kingdom uKingdom, NubUsage match) {
    if (u.rank != match.rank) {
      return false;
    }
    // no homonmys above genus level!
    if (u.rank.isSuprageneric()) {
      return true;
    }
    Equality author = authComp.equals(u.parsedName, match.parsedName);
    Equality kingdom = compareClassification(uKingdom, match);
    return author != Equality.DIFFERENT && kingdom != Equality.DIFFERENT;
  }

  //TODO: improve classification comparison to more than just kingdom ???
  private Equality compareClassification(Kingdom uKingdom, NubUsage match) {
    if (uKingdom == null || match.kingdom_ == null) {
      return Equality.UNKNOWN;
    }
    return norm(uKingdom) == norm(match.kingdom_) ? Equality.EQUAL : Equality.DIFFERENT;
  }

  public long countTaxa() {
    Result res = gds.execute("start n=node(*) match n return count(n) as cnt");
    return (long) res.columnAs("cnt").next();
  }

  /**
   * distinct only 3 larger groups of kingdoms as others are often conflated.
   */
  private Kingdom norm(Kingdom k) {
    switch (k) {
      case ANIMALIA:
      case PROTOZOA:
        return Kingdom.ANIMALIA;
      case PLANTAE:
      case FUNGI:
      case CHROMISTA:
        return Kingdom.PLANTAE;
      default:
        return Kingdom.INCERTAE_SEDIS;
    }
  }

  private NubUsage node2usage(Node n) {
    if (n == null) return null;
    NubUsage nub = mapper.read(n ,new NubUsage());
    nub.node = n;

    Preconditions.checkNotNull(nub.kingdom_);
    return nub;
  }

  public void storeParsedName(ParsedName pn, Node n) {
    mapper.setProperty(n, PN_GENUS, pn.getGenusOrAbove());
    mapper.setProperty(n, PN_SPECIES, pn.getSpecificEpithet());
    mapper.setProperty(n, PN_INFRASPECIES, pn.getInfraSpecificEpithet());
    if (pn.getNotho() != null) {
      mapper.storeEnum(n, PN_NOTHO, pn.getNotho());
    }
    mapper.setProperty(n, PN_AUTHOR, pn.getAuthorship());
    mapper.setProperty(n, PN_YEAR, pn.getYear());
    mapper.setProperty(n, PN_BRACKET_AUTHOR, pn.getBracketAuthorship());
    mapper.setProperty(n, PN_BRACKET_YEAR, pn.getBracketYear());
    mapper.setProperty(n, PN_SENSU, pn.getSensu());
  }

  public ParsedName readParsedName(Node n) {
    ParsedName pn = new ParsedName();
    // name and rank is shared with NameUsage class
    pn.setScientificName((String) n.getProperty(TaxonProperties.SCIENTIFIC_NAME, null));
    pn.setRank(NeoMapper.readEnum(n, TaxonProperties.RANK, Rank.class, null));
    // parsed name specific
    pn.setGenusOrAbove((String) n.getProperty(PN_GENUS, null));
    pn.setSpecificEpithet((String) n.getProperty(PN_SPECIES, null));
    pn.setInfraSpecificEpithet((String) n.getProperty(PN_INFRASPECIES, null));
    pn.setNotho(NeoMapper.readEnum(n, PN_NOTHO, NamePart.class, null));
    pn.setAuthorship( (String) n.getProperty(PN_AUTHOR, null));
    pn.setYear((String) n.getProperty(PN_YEAR, null));
    pn.setBracketAuthorship( (String) n.getProperty(PN_BRACKET_AUTHOR, null));
    pn.setBracketYear( (String) n.getProperty(PN_BRACKET_YEAR, null));
    pn.setSensu( (String) n.getProperty(PN_SENSU, null));
    return pn;
  }

  public NubUsage addUsage(@Nullable NubUsage parent, SrcUsage src, Origin origin, UUID sourceDatasetKey) {
    LOG.debug("Creating {} {} with parent {}", origin, src.scientificName, parent == null ? "none" : parent.parsedName.getScientificName() );

    NubUsage nub = new NubUsage(src);
    nub.datasetKey = sourceDatasetKey;
    nub.origin = origin;
    if (src.key != null) {
      nub.sourceIds.add(src.key);
    }

    return addUsage(parent, nub);
  }

  /**
   * @param parent classification parent or accepted name in case the nub usage has a synonym status
   */
  public NubUsage addUsage(@Nullable NubUsage parent, NubUsage nub) {
    Node n = gds.createNode(Labels.TAXON);
    nodes++;
    if (parent == null) {
      n.addLabel(Labels.ROOT);
    } else {
      nub.kingdom_ = parent.kingdom_;
      if (nub.status != null && nub.status.isSynonym()) {
        n.addLabel(Labels.SYNONYM);
        n.createRelationshipTo(parent.node, RelType.SYNONYM_OF);
      } else {
        parent.node.createRelationshipTo(n, RelType.PARENT_OF);
      }
    }

    //TODO: map more data, basionym, fullname, author, ...
    mapper.store(n, nub, false);
    mapper.setProperty(n, TaxonProperties.CANONICAL_NAME, nub.parsedName.canonicalName());
    nub.node = n;

    countAndRenewTx();

    return nub;
  }

    protected static Integer toInt(String x) {
    return x == null ? null : Integer.valueOf(x);
  }

  private void countAndRenewTx() {
    if (counter++ > batchSize) {
      renewTx();
    }
  }

  protected void renewTx() {
    tx.success();
    tx.close();
    tx = gds.beginTx();
    counter = 0;
  }

  @Override
  public void close() {
    tx.success();
    tx.close();
    gds.shutdown();
  }
}
