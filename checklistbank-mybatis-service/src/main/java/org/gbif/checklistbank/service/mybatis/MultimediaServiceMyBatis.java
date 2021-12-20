package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.NameUsageMediaObject;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.checklistbank.MultimediaService;
import org.gbif.checklistbank.service.mybatis.mapper.MultimediaMapper;
import org.gbif.checklistbank.utils.MediaTypeUtils;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements an ImageService using MyBatis.
 */
public class MultimediaServiceMyBatis extends NameUsageComponentServiceMyBatis<NameUsageMediaObject>
    implements MultimediaService {


  @Autowired
  public MultimediaServiceMyBatis(MultimediaMapper multimediaMapper) {
    super(multimediaMapper);
  }

  @Override
  public Map<Integer, List<NameUsageMediaObject>> listRange(int usageKeyStart, int usageKeyEnd) {
    throw new UnsupportedOperationException("listRange not supported");
  }

  @Override
  public PagingResponse<NameUsageMediaObject> listByUsage(int usageKey, @Nullable Pageable page) {
    PagingResponse<NameUsageMediaObject> result = super.listByUsage(usageKey, page);
    //TODO: avoid live interpretations until we store the type properly
    for (NameUsageMediaObject m : result.getResults()) {
      MediaTypeUtils.detectType(m);
    }
    return result;
  }

}
