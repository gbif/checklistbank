/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
