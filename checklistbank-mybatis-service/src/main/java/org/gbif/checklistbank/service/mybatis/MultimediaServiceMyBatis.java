package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.NameUsageMediaObject;
import org.gbif.api.service.checklistbank.MultimediaService;

import java.util.List;
import java.util.Map;

import com.google.inject.Inject;

/**
 * Implements an ImageService using MyBatis.
 */
public class MultimediaServiceMyBatis extends NameUsageComponentServiceMyBatis<NameUsageMediaObject>
    implements MultimediaService {

  @Inject
  MultimediaServiceMyBatis(MultimediaMapper multimediaMapper) {
    super(multimediaMapper);
  }

  @Override
  public Map<Integer, List<NameUsageMediaObject>> listRange(int usageKeyStart, int usageKeyEnd) {
    throw new UnsupportedOperationException("listRange not supported");
  }
}
