package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.Description;
import org.gbif.api.service.checklistbank.DescriptionService;

import com.google.inject.Inject;

/**
 * Implements a DescriptionService using MyBatis.
 */
public class DescriptionServiceMyBatis extends NameUsageComponentServiceMyBatis<Description>
  implements DescriptionService {

  protected static final String ENGLISH = "en";
  private final DescriptionMapper descriptionMapper;

  @Inject
  DescriptionServiceMyBatis(DescriptionMapper descriptionMapper) {
    super(descriptionMapper);
    this.descriptionMapper = descriptionMapper;
  }

}
