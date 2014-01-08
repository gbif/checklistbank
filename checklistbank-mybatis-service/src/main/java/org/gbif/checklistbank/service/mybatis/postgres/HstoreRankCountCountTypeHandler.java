package org.gbif.checklistbank.service.mybatis.postgres;

import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Rank;

import java.util.HashMap;
import java.util.Map;

public class HstoreRankCountCountTypeHandler extends HstoreCountTypeHandler<Rank> {

  @Override
  protected Rank toKey(String key) throws IllegalArgumentException {
    return (Rank) VocabularyUtils.lookupEnum(key, Rank.class);
  }

  @Override
  protected Map<Rank, Integer> sortMap(HashMap<Rank, Integer> map) {
    return super.sortMap(map);
  }
}
