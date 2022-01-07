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
package org.gbif.checklistbank.cli.shell;

import org.gbif.api.model.Constants;
import org.gbif.checklistbank.cli.common.NeoConfiguration;

import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

public class ShellConfiguration {

  @ParametersDelegate
  @NotNull
  @Valid
  public NeoConfiguration neo;

  @Parameter(names = {"-k", "--key"}, required = false)
  @NotNull
  public UUID key = Constants.NUB_DATASET_KEY;

}
