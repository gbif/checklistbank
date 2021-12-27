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
package org.gbif.checklistbank.service.mybatis.service;

import org.gbif.api.model.checklistbank.*;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.checklistbank.model.RawUsage;
import org.gbif.checklistbank.service.mybatis.persistence.mapper.*;

import java.util.*;

import javax.annotation.Nullable;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Implements the NameUsageService using MyBatis. All PagingResponses will not have the count set as
 * it can be too costly sometimes.
 */
@Service
public class NameUsageServiceMyBatis implements NameUsageService {

  private static final Logger LOG = LoggerFactory.getLogger(NameUsageServiceMyBatis.class);

  private final NameUsageMapper mapper;
  private final NameUsageMetricsMapper metricsMapper;
  private final ParsedNameMapper parsedNameMapper;
  private final VernacularNameMapper vernacularNameMapper;
  private final RawUsageMapper rawUsageMapper;
  private final VerbatimNameUsageMapperJson verbatimParser = new VerbatimNameUsageMapperJson();
  private final DataSource ds;

  @Autowired
  NameUsageServiceMyBatis(
      NameUsageMapper mapper,
      VernacularNameMapper vernacularNameMapper,
      NameUsageMetricsMapper metricsMapper,
      RawUsageMapper rawUsageMapper,
      ParsedNameMapper parsedNameMapper,
      DataSource ds) {
    this.mapper = mapper;
    this.metricsMapper = metricsMapper;
    this.vernacularNameMapper = vernacularNameMapper;
    this.rawUsageMapper = rawUsageMapper;
    this.parsedNameMapper = parsedNameMapper;
    this.ds = ds;
  }

  @Override
  public NameUsage get(int usageKey, @Nullable Locale locale) {
    NameUsage usage = mapper.get(usageKey);
    addVernacularName(usage, getLanguage(locale));
    return usage;
  }

  @Override
  public VerbatimNameUsage getVerbatim(int usageKey) {
    VerbatimNameUsage v = null;
    RawUsage raw = rawUsageMapper.get(usageKey);
    if (raw != null) {
      v = verbatimParser.read(raw.getJson());
      if (v != null) {
        // we might not have crawled that record yet with the new crawling
        v.setKey(usageKey);
        v.setLastCrawled(raw.getLastCrawled());
      }
    }
    return v;
  }

  @VisibleForTesting
  protected void insertRaw(RawUsage raw) {
    rawUsageMapper.insert(raw);
  }

  @VisibleForTesting
  protected RawUsage getRaw(int usageKey) {
    return rawUsageMapper.get(usageKey);
  }

  @Override
  public ParsedName getParsedName(int usageKey) {
    return parsedNameMapper.getByUsageKey(usageKey);
  }

  @Nullable
  @Override
  public NameUsageMetrics getMetrics(int usageKey) {
    return metricsMapper.get(usageKey);
  }

  @Override
  public PagingResponse<NameUsage> list(
      Locale locale,
      @Nullable UUID datasetKey,
      @Nullable String sourceId,
      @Nullable Pageable page) {
    if (page == null) {
      page = new PagingRequest();
    }

    List<NameUsage> usages;
    // if a sourceID has been provided as a filter, there wont be too many results
    if (!Strings.isNullOrEmpty(sourceId)) {
      usages = mapper.listByTaxonId(datasetKey, sourceId, page);

    } else {
      usages = Lists.newArrayList();
      // we need to page over ids only and then lookup each full usage by its key
      // otherwise the db queries get too heavy for the database if offsets are high!
      List<Integer> usageIds = mapper.list(datasetKey, page);
      for (Integer uid : usageIds) {
        usages.add(get(uid, locale));
      }
    }
    return localizedPage(locale, usages, page);
  }

  @Override
  public PagingResponse<NameUsage> listByCanonicalName(
      Locale locale, String canonicalName, @Nullable Pageable page, @Nullable UUID... datasetKey) {
    if (page == null) {
      page = new PagingRequest();
    }

    List<NameUsage> usages = mapper.listByCanonicalName(canonicalName, datasetKey, page);
    return localizedPage(locale, usages, page);
  }

  @Override
  public List<NameUsage> listParents(int usageKey, @Nullable Locale locale) {
    LinkedList<NameUsage> parents = Lists.newLinkedList();
    NameUsage curr = get(usageKey, locale);
    if (curr != null) {
      // make sure we don't hit some circular structures which should not exist really, but who
      // knows...
      Set<Integer> visited = Sets.newHashSet();
      visited.add(usageKey);
      while (curr.getParentKey() != null) {
        int parentKey = curr.getParentKey();
        if (visited.contains(parentKey)) {
          LOG.warn("Taxonomic hierarchy for usage {} contains circular references!", parentKey);
          break;
        }
        visited.add(parentKey);
        curr = get(parentKey, locale);
        if (curr == null) {
          LOG.warn("Usage not found for parent key {}", parentKey);
          break;
        }
        parents.addFirst(curr);
      }
    }
    return parents;
  }

  @Override
  public PagingResponse<NameUsage> listChildren(
      int parentKey, @Nullable Locale locale, @Nullable Pageable page) {
    if (page == null) {
      page = new PagingRequest();
    }
    return localizedPage(locale, mapper.listChildren(parentKey, page), page);
  }

  private PagingResponse<NameUsage> localizedPage(
      Locale locale, List<NameUsage> usages, Pageable requestPage) {
    addVernacularNames(usages, getLanguage(locale));
    return new PagingResponse<NameUsage>(requestPage, null, usages);
  }

  @Override
  public PagingResponse<NameUsage> listSynonyms(
      int usageKey, @Nullable Locale locale, @Nullable Pageable page) {
    if (page == null) {
      page = new PagingRequest();
    }
    List<NameUsage> usages = mapper.listSynonyms(usageKey, page);
    return localizedPage(locale, usages, page);
  }

  @Override
  public PagingResponse<NameUsage> listRoot(
      UUID datasetKey, @Nullable Locale locale, @Nullable Pageable page) {
    if (page == null) {
      page = new PagingRequest();
    }
    List<NameUsage> usages = mapper.listRoot(datasetKey, page);
    return localizedPage(locale, usages, page);
  }

  @Override
  public PagingResponse<NameUsage> listRelated(
      int nubKey, @Nullable Locale locale, @Nullable Pageable page, @Nullable UUID... datasetKey) {
    if (page == null) {
      page = new PagingRequest();
    }
    List<NameUsage> usages = mapper.listRelated(nubKey, datasetKey, page);
    return localizedPage(locale, usages, page);
  }

  @Override
  public List<NameUsage> listCombinations(int basionymKey, Locale locale) {
    List<NameUsage> usages = mapper.listCombinations(basionymKey);
    addVernacularNames(usages, getLanguage(locale));
    return usages;
  }

  /** @return the lowercase 2 letter iso language code or null */
  protected static String getLanguage(@Nullable Locale locale) {
    if (locale == null || locale.getLanguage() == null || locale.getLanguage().length() != 2) {
      return null;
    }
    return locale.getLanguage().toLowerCase();
  }

  /**
   * Adds a matching vernacular name of a given language to a list of existing name usages. See
   * {@link #addVernacularName(org.gbif.api.model.checklistbank.NameUsage, String)} for logic used
   * in picking the common name.
   *
   * @param usages the checklist usages to add a vernacular name to
   * @param language the requested language
   */
  private void addVernacularNames(List<NameUsage> usages, @Nullable String language) {
    if (language == null) return;
    for (NameUsage u : usages) {
      addVernacularName(u, language);
    }
  }

  /**
   * Adds a vernacular name of a given language to an existing name usage. Nothing is added if there
   * is no vernacular name in the given language.
   *
   * @param u the checklist usage to add the vernacular name to
   * @param language the requested language
   */
  private void addVernacularName(NameUsage u, @Nullable String language) {
    if (u == null || language == null) {
      return;
    }

    // first try with given language
    VernacularName v = getVernacularNameInLanguage(u, language);

    if (v != null) {
      u.setVernacularName(v.getVernacularName());
    }
  }

  /**
   * Gets a single vernacular name for a given language and a name usage.
   *
   * @param u the name usage of the vernacular name
   * @param language the requested language, never null
   * @return vernacular name or null if none can be found
   */
  private VernacularName getVernacularNameInLanguage(NameUsage u, String language) {
    if (u.isNub()) {
      return vernacularNameMapper.getByNubUsage(u.getKey(), language);
    } else {
      return vernacularNameMapper.getByChecklistUsage(u.getKey(), language);
    }
  }
}
