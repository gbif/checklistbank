package org.gbif.checklistbank.service.mybatis.postgres;

import org.gbif.api.vocabulary.NameUsageIssue;

public class ArraySetIssueTypeHandler extends ArraySetTypeHandler<NameUsageIssue> {

  public ArraySetIssueTypeHandler() {
    super("text");
  }

  @Override
  protected NameUsageIssue convert(String x) {
    return NameUsageIssue.valueOf(x);
  }
}
