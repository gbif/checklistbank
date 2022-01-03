package org.gbif.checklistbank.config;

import java.util.StringJoiner;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.ParametersDelegate;

/**
 * Configuration needed to connect to the registry webservices.
 */
@SuppressWarnings("PublicField")
public class RegistryServiceConfiguration {

  @ParametersDelegate
  @Valid
  @NotNull
  public String wsUrl = "http://api.gbif.org/v1";

  public String user;

  public String password;

  @Override
  public String toString() {
    return new StringJoiner(", ", RegistryServiceConfiguration.class.getSimpleName() + "[", "]")
        .add("wsUrl='" + wsUrl + "'")
        .add("user='" + user + "'")
        .toString();
  }
}
