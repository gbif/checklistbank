package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.model.checklistbank.TableOfContents;
import org.gbif.api.service.checklistbank.DescriptionService;
import org.gbif.checklistbank.service.mybatis.mapper.DescriptionMapper;
import org.gbif.checklistbank.service.mybatis.model.TocEntry;

import java.util.List;

import javax.annotation.Nullable;

import com.google.inject.Inject;

/**
 * Implements a DescriptionService using MyBatis.
 */
public class DescriptionServiceMyBatis extends NameUsageComponentServiceMyBatis<Description>
  implements DescriptionService {

  protected static final String ENGLISH = "en";
  private final DescriptionMapper descriptionMapper;

  @Inject
  DescriptionServiceMyBatis(DescriptionMapper descriptionMapper) {
    super(descriptionMapper);
    this.descriptionMapper = descriptionMapper;
  }

  @Override
  public TableOfContents getToc(int usageKey) {
    TableOfContents toc = new TableOfContents();

    List<TocEntry> entries;
    if (isNub(usageKey)) {
      entries = descriptionMapper.listTocEntriesByNub(usageKey);
    } else {
      entries = descriptionMapper.listTocEntriesByUsage(usageKey);
    }

    for (TocEntry e : entries) {
      toc.addDescription(e.getKey(), e.getLanguage(), e.getTopic());
    }

    return toc;
  }

  @Nullable
  @Override
  public Description get(int key) {
    return descriptionMapper.get(key);
  }
}
