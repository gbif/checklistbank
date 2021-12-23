package org.gbif.checklistbank.service.mybatis.persistence.postgres;

import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Extension;

public class HstoreExtensionCountTypeHandler extends HstoreCountTypeHandler<Extension> {

  @Override
  protected Extension toKey(String key) throws IllegalArgumentException {
    return (Extension) VocabularyUtils.lookupEnum(key, Extension.class);
  }

}
