package org.gbif.checklistbank.model;

import org.gbif.api.model.registry.Dataset;

import java.util.Objects;
import java.util.UUID;

/**
 *
 */
public class DatasetCore {
  private UUID key;
  private String title;
  private UUID parent;
  private UUID publisher;

  public DatasetCore() {
  }

  public DatasetCore(Dataset d) {
    this.key = d.getKey();
    this.title = d.getTitle();
    this.parent = d.getParentDatasetKey();
    this.publisher = d.getPublishingOrganizationKey();
  }

  public DatasetCore(UUID key, String title) {
    this.key = key;
    this.title = title;
  }

  public DatasetCore(UUID key, String title, UUID parent, UUID publisher) {
    this.key = key;
    this.title = title;
    this.parent = parent;
    this.publisher = publisher;
  }

  public UUID getKey() {
    return key;
  }

  public void setKey(UUID key) {
    this.key = key;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public UUID getParent() {
    return parent;
  }

  public void setParent(UUID parent) {
    this.parent = parent;
  }

  public UUID getPublisher() {
    return publisher;
  }

  public void setPublisher(UUID publisher) {
    this.publisher = publisher;
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, title, parent, publisher);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final DatasetCore other = (DatasetCore) obj;
    return Objects.equals(this.key, other.key)
        && Objects.equals(this.title, other.title)
        && Objects.equals(this.parent, other.parent)
        && Objects.equals(this.publisher, other.publisher);
  }
}
