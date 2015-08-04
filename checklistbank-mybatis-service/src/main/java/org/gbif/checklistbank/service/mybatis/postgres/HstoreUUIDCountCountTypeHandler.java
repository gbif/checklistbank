package org.gbif.checklistbank.service.mybatis.postgres;

import java.util.UUID;

public class HstoreUUIDCountCountTypeHandler extends HstoreCountTypeHandler<UUID> {

  @Override
  protected UUID toKey(String key) throws IllegalArgumentException {
      try {
          return UUID.fromString(key);
      } catch (IllegalArgumentException e) {
          return null;
      }
  }
}
