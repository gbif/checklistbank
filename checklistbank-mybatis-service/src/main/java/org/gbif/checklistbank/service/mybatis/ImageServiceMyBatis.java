package org.gbif.checklistbank.service.mybatis;

import org.gbif.api.model.checklistbank.Image;
import org.gbif.api.service.checklistbank.ImageService;

import java.util.List;

import com.google.inject.Inject;

/**
 * Implements an ImageService using MyBatis.
 */
public class ImageServiceMyBatis extends NameUsageComponentServiceMyBatis<Image> implements ImageService {

  @Inject
  ImageServiceMyBatis(ImageMapper imageMapper) {
    super(imageMapper);
  }

  @Override
  public List<Image> listRange(int usageKeyStart, int usageKeyEnd) {
    throw new UnsupportedOperationException("listRange not supported");
  }
}
