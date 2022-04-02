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
package org.gbif.checklistbank.index;

import java.time.Duration;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("occurrence/count")
public interface OccurrenceCountClient {

  class CachingOccurrenceCountClient implements OccurrenceCountClient {

    //Estimated capacity of the Backbone
    private static final int NUB_SIZE = 7_000_000;

    private final Cache<Integer,Long> cache;

    private CachingOccurrenceCountClient(OccurrenceCountClient wrappedClient, Duration duration) {
      cache = new Cache2kBuilder<Integer,Long>(){}
        .loader(wrappedClient::count)
        .expireAfterWrite(duration)
        .permitNullValues(true)
        .entryCapacity(NUB_SIZE)
        .build();
    }

    private CachingOccurrenceCountClient(OccurrenceCountClient wrappedClient) {
      cache = new Cache2kBuilder<Integer,Long>(){}
        .loader(wrappedClient::count)
        .eternal(true)
        .permitNullValues(true)
        .entryCapacity(NUB_SIZE)
        .build();
    }

    @Override
    public Long count(Integer taxonKey) {
      return cache.get(taxonKey);
    }
  }

  static OccurrenceCountClient cachingClient(OccurrenceCountClient wrappedClient) {
    return new CachingOccurrenceCountClient(wrappedClient);
  }

  static OccurrenceCountClient cachingClient(OccurrenceCountClient wrappedClient, Duration duration) {
    return new CachingOccurrenceCountClient(wrappedClient, duration);
  }

  @RequestMapping(
    method = RequestMethod.GET,
    produces = MediaType.APPLICATION_JSON_VALUE)
  Long count(@RequestParam("taxonKey") Integer taxonKey);
}
