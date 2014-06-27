package org.gbif.checklistbank.service.mybatis.model;

import org.gbif.api.vocabulary.Language;

/**
 *
 */
public class TocEntry {
  int key;
  String topic;
  Language language;

  public int getKey() {
    return key;
  }

  public void setKey(int key) {
    this.key = key;
  }

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public Language getLanguage() {
    return language;
  }

  public void setLanguage(Language language) {
    this.language = language;
  }
}
