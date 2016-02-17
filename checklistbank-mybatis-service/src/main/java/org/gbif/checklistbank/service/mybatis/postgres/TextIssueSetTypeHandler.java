package org.gbif.checklistbank.service.mybatis.postgres;

import org.gbif.api.vocabulary.NameUsageIssue;

/**
 *
 */
public class TextIssueSetTypeHandler extends TextEnumSetTypeHandler<NameUsageIssue> {

  public TextIssueSetTypeHandler() {
    super(NameUsageIssue.class);
  }
}