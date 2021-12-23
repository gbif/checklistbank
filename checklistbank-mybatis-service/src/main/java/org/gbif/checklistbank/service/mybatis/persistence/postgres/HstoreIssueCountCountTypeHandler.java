package org.gbif.checklistbank.service.mybatis.persistence.postgres;

import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.NameUsageIssue;

public class HstoreIssueCountCountTypeHandler extends HstoreCountTypeHandler<NameUsageIssue> {

  @Override
  protected NameUsageIssue toKey(String key) throws IllegalArgumentException {
    return (NameUsageIssue) VocabularyUtils.lookupEnum(key, NameUsageIssue.class);
  }

}
