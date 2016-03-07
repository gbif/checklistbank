package org.gbif.checklistbank.cli.nubbuild;

import org.gbif.checklistbank.cli.common.NeoConfiguration;
import org.gbif.checklistbank.cli.common.RegistryServiceConfiguration;
import org.gbif.checklistbank.config.ClbConfiguration;
import org.gbif.common.messaging.config.MessagingConfiguration;

import java.io.File;
import java.net.URI;
import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.ParametersDelegate;

/**
 *
 */
@SuppressWarnings("PublicField")
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

  /**
   * If true algorithmic detecting and grouping of basionyms is executed.
   */
  @Valid
  public boolean groupBasionyms = false;

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
  public boolean validate = false;

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
