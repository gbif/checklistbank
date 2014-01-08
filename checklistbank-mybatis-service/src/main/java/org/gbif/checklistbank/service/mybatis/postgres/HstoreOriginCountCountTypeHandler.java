package org.gbif.checklistbank.service.mybatis.postgres;

import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Origin;

public class HstoreOriginCountCountTypeHandler extends HstoreCountTypeHandler<Origin> {

  @Override
  protected Origin toKey(String key) throws IllegalArgumentException {
    return (Origin) VocabularyUtils.lookupEnum(key, Origin.class);
  }

}
