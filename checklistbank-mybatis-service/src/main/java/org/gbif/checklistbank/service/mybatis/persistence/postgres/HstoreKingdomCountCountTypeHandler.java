package org.gbif.checklistbank.service.mybatis.persistence.postgres;

import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Kingdom;

public class HstoreKingdomCountCountTypeHandler extends HstoreCountTypeHandler<Kingdom> {

  @Override
  protected Kingdom toKey(String key) throws IllegalArgumentException {
    return (Kingdom) VocabularyUtils.lookupEnum(key, Kingdom.class);
  }
}
