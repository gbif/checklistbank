package org.gbif.checklistbank.service.mybatis.postgres;

import org.gbif.api.vocabulary.NameUsageIssue;

/**
 * This type handler is based on a text[] postgres type.
 * It is deprecated as we discovered a bug in the postgres JDBC driver for this type with prepared statements.
 * Please use the TextEnumSetTypeHandler if possible for now.
 */
@Deprecated
public class ArraySetIssueTypeHandler extends ArraySetTypeHandler<NameUsageIssue> {

  public ArraySetIssueTypeHandler() {
    super("text");
  }

  @Override
  protected NameUsageIssue convert(String x) {
    return NameUsageIssue.valueOf(x);
  }
}
