package org.gbif.checklistbank.index;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("occurrence/count")
public interface OccurrenceCountClient {

  @RequestMapping(
    method = RequestMethod.GET,
    produces = MediaType.APPLICATION_JSON_VALUE)
  Long count(@RequestParam("taxonKey") Integer taxonKey);
}
