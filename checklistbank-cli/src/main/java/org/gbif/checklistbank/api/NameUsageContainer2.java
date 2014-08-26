package org.gbif.checklistbank.api;

import org.gbif.api.model.checklistbank.NameUsageContainer;

import java.util.Set;

import com.beust.jcommander.internal.Sets;

/**
 * proposed change to NameUsage(Container) to hold issues.
 */
public class NameUsageContainer2 extends NameUsageContainer {
  private Set<NameUsageIssue> issues = Sets.newHashSet();

  public Set<NameUsageIssue> getIssues() {
    return issues;
  }

  public void setIssues(Set<NameUsageIssue> issues) {
    this.issues = issues;
  }

  public void addIssue(NameUsageIssue issue) {
    this.issues.add(issue);
  }
}
