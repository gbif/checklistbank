package org.gbif.checklistbank.ws.client;

import org.gbif.api.model.checklistbank.Description;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/** Client-side implementation to the DescriptionService. */
@RequestMapping("description")
public interface DescriptionClient {

  @RequestMapping(
      value = "{id}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  Description get(@PathVariable("id") int key);
}
