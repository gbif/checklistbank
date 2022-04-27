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
package org.gbif.checklistbank.ws.resources;

import org.gbif.api.annotation.NullToNotFound;
import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.service.checklistbank.DescriptionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Description resource.
 */
@RestController
@RequestMapping(
  value = "/description",
  produces = {MediaType.APPLICATION_JSON_VALUE, "application/x-javascript"}
)
public class DescriptionResource {

  private final DescriptionService descriptionService;

  @Autowired
  public DescriptionResource(DescriptionService descriptionService) {
    this.descriptionService = descriptionService;
  }

  /**
   * This retrieves a Description by its key from ChecklistBank.
   *
   * @return requested Description or null if none could be found
   */
  @GetMapping("{id}")
  @NullToNotFound("/description/{key}")
  public Description get(@PathVariable("id") Integer key) {
    return descriptionService.get(key);
  }

}
