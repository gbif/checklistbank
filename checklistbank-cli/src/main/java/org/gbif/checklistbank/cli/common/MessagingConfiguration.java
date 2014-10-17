package org.gbif.checklistbank.cli.common;

import javax.validation.constraints.Min;

import com.beust.jcommander.Parameter;

/**
 *
 */
public class MessagingConfiguration extends org.gbif.common.messaging.config.MessagingConfiguration {

  @Parameter(names = "--messaging-pool-size")
  @Min(1)
  public int poolSize = 3;

}
