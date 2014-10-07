package org.gbif.checklistbank.model;

import org.gbif.api.vocabulary.Language;

import java.util.Objects;

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

  @Override
  public int hashCode() {
    return Objects.hash(key, topic, language);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final TocEntry other = (TocEntry) obj;
    return Objects.equals(this.key, other.key) && Objects.equals(this.topic, other.topic) && Objects
      .equals(this.language, other.language);
  }
}
