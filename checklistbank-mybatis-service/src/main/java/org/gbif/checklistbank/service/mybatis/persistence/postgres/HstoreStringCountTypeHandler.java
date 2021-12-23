package org.gbif.checklistbank.service.mybatis.persistence.postgres;

public class HstoreStringCountTypeHandler extends HstoreCountTypeHandler<String> {

  @Override
  protected String toKey(String key) throws IllegalArgumentException {
    return key;
  }

}
