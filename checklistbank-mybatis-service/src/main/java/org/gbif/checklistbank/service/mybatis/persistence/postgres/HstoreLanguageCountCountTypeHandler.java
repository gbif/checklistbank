package org.gbif.checklistbank.service.mybatis.persistence.postgres;

import org.gbif.api.vocabulary.Language;

public class HstoreLanguageCountCountTypeHandler extends HstoreCountTypeHandler<Language> {

  @Override
  protected Language toKey(String key) throws IllegalArgumentException {
    return Language.fromIsoCode(key);
  }

}
