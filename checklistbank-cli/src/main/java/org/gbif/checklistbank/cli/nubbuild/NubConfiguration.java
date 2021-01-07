package org.gbif.checklistbank.cli.nubbuild;

import com.beust.jcommander.ParametersDelegate;
import com.google.common.collect.Sets;
import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.checklistbank.config.RegistryServiceConfiguration;
import org.gbif.common.messaging.config.MessagingConfiguration;
import org.gbif.api.model.Constants;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.net.URI;
import java.util.Set;
import java.util.UUID;

/**
 *
 */
public class NubConfiguration {

  @ParametersDelegate
  @NotNull
  @Valid
  public MessagingConfiguration messaging = new MessagingConfiguration();

  @ParametersDelegate
  @Valid
  @NotNull
  public NeoConfiguration neo = new NeoConfiguration();

  @ParametersDelegate
  @Valid
  @NotNull
  public ClbConfiguration clb = new ClbConfiguration();

  @ParametersDelegate
  @NotNull
  @Valid
  public RegistryServiceConfiguration registry = new RegistryServiceConfiguration();

  @NotNull
  @Valid
  public URI sourceList = URI.create("https://raw.githubusercontent.com/gbif/checklistbank/master/checklistbank-nub/nub-sources.tsv");

  @NotNull
  @Valid
  public URI blacklist = URI.create("https://raw.githubusercontent.com/gbif/checklistbank/master/checklistbank-nub/blacklist.tsv");

  /**
   * Set of dataset keys from which suprageneric homonyms are allowed during nub builds.
   * Defaults to just the backbone patch and Catalogue of Life.
   */
  @NotNull
  @Valid
  public Set<UUID> homonymLists = Sets.newHashSet(
      UUID.fromString("daacce49-b206-469b-8dc2-2257719f3afa"), // backbone patch
      Constants.COL_DATASET_KEY
  );

  /**
   * Set of dataset, publisher or installation keys from which synonyms should be ignored during nub builds.
   * Defaults to the Plazi organization due to http://dev.gbif.org/issues/browse/POR-3151
   */
  @Valid
  public Set<UUID> ignoreSynonyms = Sets.newHashSet(UUID.fromString("7ce8aef0-9e92-11dc-8738-b8a03c50a862"));

  /**
   * If true algorithmic detecting and grouping of basionyms is executed.
   */
  @Valid
  public boolean groupBasionyms = false;

  @Valid
  public URI basionymExclusion = URI.create("https://raw.githubusercontent.com/gbif/checklistbank/master/checklistbank-nub/blacklist.tsv");

  /**
   * If false autonyms with no other sibling are removed.
   */
  @Valid
  public boolean keepLonelyAutonyms = false;

  /**
   * If false implicit names (genus, species) are removed in case they do not have any accepted children.
   */
  @Valid
  public boolean keepEmptyImplicitNames = false;

  /**
   * If true an import message is being send once the build is done and basic backbone assertions have passed successfully.
   */
  @Valid
  public boolean autoImport = false;

  /**
   * If true run validation routines at the end of the nub build, see NubTreeValidation.
   */
  @Valid
  public boolean validate = true;

  /**
   * If true run assertions for a complete production backbone, see NubAssertions.
   */
  @Valid
  public boolean runAssertions = false;

  /**
   * If true some explicit debugging and logging is enabled, e.g. the neo4j mapped memory stats and number of open files and threads.
   */
  @Valid
  @Nullable
  public File reportDir;

  /**
   * In milliseconds.
   */
  @Min(100)
  public int parserTimeout = 1000;

}
