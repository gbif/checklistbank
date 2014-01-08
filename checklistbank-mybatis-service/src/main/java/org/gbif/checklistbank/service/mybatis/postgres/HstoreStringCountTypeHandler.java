package org.gbif.checklistbank.service.mybatis.postgres;

public class HstoreStringCountTypeHandler extends HstoreCountTypeHandler<String> {

  @Override
  protected String toKey(String key) throws IllegalArgumentException {
    return key;
  }

}
