package org.gbif.checklistbank.service.mybatis.persistence.postgres;

import org.gbif.api.vocabulary.NameUsageIssue;

/**
 * This type handler is based on a text[] postgres type.
 */
public class ArraySetIssueTypeHandler extends ArraySetTypeHandler<NameUsageIssue> {

  public ArraySetIssueTypeHandler() {
    super("text");
  }

  @Override
  protected NameUsageIssue convert(String x) {
    return NameUsageIssue.valueOf(x);
  }
}
